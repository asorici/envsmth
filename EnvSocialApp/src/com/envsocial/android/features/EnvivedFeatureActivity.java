package com.envsocial.android.features;

import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.envsocial.android.EnvivedFeatureDataRetrievalService;
import com.envsocial.android.R;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.exceptions.EnvSocialContentException;

public abstract class EnvivedFeatureActivity extends SherlockFragmentActivity {
	private static final String TAG = "EnvivedFeatureActivity";
	private static final int NO_DATA = 0;
	private static final int DATA_RETRIEVED = 1;
	
	protected static boolean active = true;
	
	protected Location mLocation;
	protected Feature mFeature;
	protected int mFeatureDataStatus;
	
	private FeatureDataReceiver mFeatureDataReceiver;
	protected ProgressDialog mFeatureLoadingDialog;
	protected InitializeFeatureTask mInitFeatureTask;
	protected Timer mCancelLoadingDialogTimer;
	
	
	private ProgressDialog createFeatureLoadingDialog(Context context, String message) {
		ProgressDialog pd = new ProgressDialog(new ContextThemeWrapper(context, R.style.ProgressDialogWhiteText));
		pd.setIndeterminate(true);
		pd.setMessage(message);
		pd.setCancelable(true);
		pd.setCanceledOnTouchOutside(false);
		
		return pd;
	}
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	
        // retrieve Location argument
        if (savedInstanceState != null) {
        	mLocation = (Location) savedInstanceState.get("location");
        }
        else {
        	mLocation = (Location)getIntent().getExtras().get("location");
        }
        
        try {
			mFeature = getLocationFeature(mLocation);
		} catch (EnvSocialContentException e) {
			Log.d(TAG, "Error retrieving feature from location: " + mLocation.getName(), e.getCause());
			Toast toast = Toast.makeText(this, "Error starting feature activity.", Toast.LENGTH_LONG);
			toast.show();
			
			finish();
			return;
		}
        
        // register the order feature update receiver here
     	mFeatureDataReceiver = new FeatureDataReceiver();
     	IntentFilter filter = new IntentFilter();
      	filter.addAction(EnvivedFeatureDataRetrievalService.ACTION_FEATURE_RETRIEVE_DATA);
      	this.registerReceiver(mFeatureDataReceiver, filter, 
      					EnvivedFeatureDataRetrievalService.FEATURE_RETRIEVE_DATA_PERMISSION, null);
      	
      	// call feature initialization task here, start the loading dialog and the timer that cancels
      	// the dialog if things move to slow
      	mInitFeatureTask = new InitializeFeatureTask(mFeature, false);
      	mInitFeatureTask.execute();
      	
		//mFeatureLoadingDialog = createFeatureLoadingDialog(this, "Loading data ...");
		mCancelLoadingDialogTimer = new Timer();
		final Handler handler = new Handler();
		
		mCancelLoadingDialogTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (!mFeature.isInitialized() && mFeatureLoadingDialog != null
						&& mFeatureLoadingDialog.isShowing()) {
					mFeatureLoadingDialog.dismiss();
					
					if (active) {
						handler.post(new Runnable() {
							@Override
							public void run() {
								Toast toast = Toast.makeText(getApplicationContext(), "Slow network connection / disk access.", Toast.LENGTH_LONG);
								toast.show();
							}
						});
					}
				}
			}
		}, 10000);
		
		//mFeatureLoadingDialog.show();
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		active = true;
	}
	
	
	@Override
	public void onPause() {
    	super.onPause();
    	active = false;	
	}
	
	
	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("location", mLocation);
    }
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// cancel init task if currently running
		if (mInitFeatureTask != null) {
			mInitFeatureTask.cancel(false);
			mInitFeatureTask = null;
		}
		
		// cancel loading dialog timer
		if (mCancelLoadingDialogTimer != null) {
			mCancelLoadingDialogTimer.cancel();
			mCancelLoadingDialogTimer = null;
		}
		
		// cancel loading dialog
		if (mFeatureLoadingDialog != null) {
			mFeatureLoadingDialog.cancel();
			mFeatureLoadingDialog = null;
		}
		
		this.unregisterReceiver(mFeatureDataReceiver);
		
		// finally close the feature
		mFeature.doClose(getApplicationContext());
	}
	
	
	protected abstract Feature getLocationFeature(Location location) throws EnvSocialContentException;
	
	protected abstract void onFeatureDataInitialized(Feature newFeature, boolean success);

	protected abstract void onFeatureDataUpdated(Feature updatedFeature, boolean success);
	
	protected abstract String getActiveUpdateDialogMessage();
		
	
	protected class FeatureDataReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// get the intent extras
			final Bundle extras = intent.getExtras();
			
			// get the feature category for which an update was performed
			String featureCategory = extras.getString("feature_category");
			Log.d(TAG, "Received update description notification with category: " + featureCategory);
			
			if (featureCategory.equals(mFeature.getCategory())) {
				if (mFeature.isInitialized()) {
					
					// the feature is already initialized, so update it with retrieved data 
					final Feature updatedFeature = (Feature) extras.getSerializable("feature_content");
					
					// check for active  notification here to tell the user the program is changing
					if (active) {
						AlertDialog.Builder builder = new AlertDialog.Builder(EnvivedFeatureActivity.this);
						LayoutInflater inflater = getLayoutInflater();
	
						TextView titleDialogView = (TextView) inflater.inflate(
								R.layout.catalog_update_dialog_title, null, false);
						titleDialogView.setText("Allow content update?");
						
						
						String dialogMessage = getActiveUpdateDialogMessage();
						
						TextView bodyDialogView = (TextView) inflater.inflate(
								R.layout.catalog_update_dialog_body, null, false);
						bodyDialogView.setText(dialogMessage);
						
						builder.setCustomTitle(titleDialogView);
						builder.setView(bodyDialogView);
	
						builder.setPositiveButton("Yes",
								new Dialog.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.cancel();
										
										mInitFeatureTask = new InitializeFeatureTask(updatedFeature, true);
										mInitFeatureTask.execute();
									}
								});
	
						builder.setNegativeButton("No",
								new Dialog.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.cancel();
									}
								});
	
						builder.show();
					}
					else {
						// start the update directly
						mInitFeatureTask = new InitializeFeatureTask(updatedFeature, true);
						mInitFeatureTask.execute();
					}
				}
				else {
					// the feature retrieved data for the first time, so initialize it
					Feature newFeature = (Feature) extras.getSerializable("feature_content");
					
					// start the initialization task
					mInitFeatureTask = new InitializeFeatureTask(newFeature, false);
					mInitFeatureTask.execute();
					
					// dismiss the waiting feature loader dialog if it is still running
					if (mFeatureLoadingDialog != null && mFeatureLoadingDialog.isShowing()) {
						mFeatureLoadingDialog.cancel();
					}
				}
				
				abortBroadcast();
			}
		}
	}
	
	
	protected class InitializeFeatureTask extends AsyncTask<Void, Void, Boolean> {
		private Feature mNewFeature;
		private boolean mUpdate;
		
		InitializeFeatureTask(Feature feature, boolean update) {
			mNewFeature = feature;
			mUpdate = update;
		}
		
		@Override
		protected void onPreExecute() {
			String message = "Initializing Data ...";
			
			if (mFeatureLoadingDialog != null) {
				mFeatureLoadingDialog.setMessage(message);
				if (!mFeatureLoadingDialog.isShowing()) {
					mFeatureLoadingDialog.show();
				}
			}
			else {
				mFeatureLoadingDialog = createFeatureLoadingDialog(EnvivedFeatureActivity.this, message);
				mFeatureLoadingDialog.show();
			}
		}
		
		@Override
		protected Boolean doInBackground(Void... params) {
			if (!mUpdate) {
				try {
					mNewFeature.init();
					return true;
				} catch (EnvSocialContentException e) {
					Log.d(TAG, "ERROR initializing feature " + mNewFeature.getCategory(), e);
					return false;
				}
			}
			else {
				try {
					mNewFeature.doUpdate();
					return true;
				} catch (EnvSocialContentException e) {
					Log.d(TAG, "ERROR initializing feature " + mNewFeature.getCategory(), e);
					return false;
				}
			}
		}
		
		
		@Override
		protected void onPostExecute(Boolean success) {
			if (mNewFeature.hasData()) {
				// stop the feature loading dialog
				if (mFeatureLoadingDialog != null) {
					mFeatureLoadingDialog.cancel();
					mFeatureLoadingDialog = null;
				}
				
				// on successful initialization / update set newly initialized feature as current one
				if (success) {
					mFeature = mNewFeature;
					mLocation.setFeature(mFeature.getCategory(), mFeature);
				}
				
				// do the post init / update logic in the activities inheriting from EnvivedFeatureActivity 
				if (!mUpdate) {
					onFeatureDataInitialized(mFeature, success);
				}
				else {
					onFeatureDataUpdated(mFeature, success);
				}
			}
		}
	}
	
	
	public Location getFeatureLocation() {
		return mLocation;
	}
	
	
	public Feature getFeature() {
		return mFeature;
	}
}
