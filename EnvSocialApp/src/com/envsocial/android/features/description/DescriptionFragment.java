package com.envsocial.android.features.description;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.envsocial.android.DetailsActivity;
import com.envsocial.android.EnvivedFeatureDataRetrievalService;
import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.LocationContextManager;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.ResponseHolder;
import com.envsocial.android.utils.imagemanager.ImageFetcher;
import com.envsocial.android.utils.imagemanager.ImageWorker;

public class DescriptionFragment extends SherlockFragment {
	private static final String TAG = "DescriptionFragment";
	private static boolean active = false;
	
	private Location mLocation;
	private DescriptionFeature mDescriptionFeature;
	private DescriptionFeatureDataReceiver mFeatureDataReceiver;
	
	private TextView mDescriptionDetailsView;
	private TextView mDescriptionNewInfoView;
	private TextView mDescriptionPeopleCount;
	private ImageView mLogoImageView;
	
	private ImageFetcher mImageFetcher;
	private CheckedInCountTask mPeopleCountTask;
	
	private ProgressDialog mFeatureLoadingDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
	    mDescriptionFeature = (DescriptionFeature) mLocation.getFeature(Feature.DESCRIPTION);
	
	    // register the order feature update receiver here
	 	mFeatureDataReceiver = new DescriptionFeatureDataReceiver();
	 	IntentFilter filter = new IntentFilter();
 		filter.addAction(EnvivedFeatureDataRetrievalService.ACTION_FEATURE_RETRIEVE_DATA);
 		getActivity().registerReceiver(mFeatureDataReceiver, filter, 
 						EnvivedFeatureDataRetrievalService.FEATURE_RETRIEVE_DATA_PERMISSION, null);
 		//getActivity().registerReceiver(mFeatureDataReceiver, filter); 
 		
 		if (!mDescriptionFeature.isInitialized()) {
 			try {
				mDescriptionFeature.init();
			} catch (EnvSocialContentException e) {
				Log.d(TAG, "ERROR initializing description feature.", e);
			}
 		}
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		// Inflate layout for this fragment.
		View v = inflater.inflate(R.layout.description, container, false);
		mLogoImageView = (ImageView) v.findViewById(R.id.description_image);
		mDescriptionDetailsView = (TextView) v.findViewById(R.id.description_details);
		mDescriptionPeopleCount = (TextView) v.findViewById(R.id.description_people_count);
		mDescriptionNewInfoView = (TextView) v.findViewById(R.id.description_new_info);
		
		// start feature loading progress dialog if feature data is not yet initialized
		if (!mDescriptionFeature.isInitialized()) {
			mFeatureLoadingDialog = createFeatureLoadingDialog(getActivity());
			
			// create TimerTask to cancel the wait... progress dialog after 5 seconds
			final Handler cancelLoadingDialogHandler = new Handler();
			final Timer cancelLoadingDialogTimer = new Timer();
			cancelLoadingDialogTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					cancelLoadingDialogHandler.post(new Runnable() {
						@Override
						public void run() {
							if (!mDescriptionFeature.isInitialized() && mFeatureLoadingDialog.isShowing()) {
								mFeatureLoadingDialog.dismiss();
								Toast toast = Toast.makeText(getActivity(), "Slow network connection.", Toast.LENGTH_LONG);
								toast.show();
							}
						}
					});
				}
			}, 5000);
			
			mFeatureLoadingDialog.show();
		}
		
	    return v;
	}
	

	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        if (DetailsActivity.class.isInstance(getActivity())) {
            mImageFetcher = ((DetailsActivity) getActivity()).getImageFetcher();
        }
        
        bindDescriptionViewData();
    }
	
	
	private ProgressDialog createFeatureLoadingDialog(Context context) {
		ProgressDialog pd = new ProgressDialog(new ContextThemeWrapper(context, R.style.ProgressDialogWhiteText));
		pd.setIndeterminate(true);
		pd.setMessage("Retrieving Data ...");
		pd.setCancelable(true);
		pd.setCanceledOnTouchOutside(false);
		
		return pd;
	}
	
	private void bindDescriptionViewData() {
		mDescriptionDetailsView.setText(mDescriptionFeature.getDescriptionText());
		mDescriptionNewInfoView.setText(mDescriptionFeature.getNewestInfoText());
		
		String logoImageUrl = mDescriptionFeature.getLogoImageUri();
        
        if (logoImageUrl != null) {
        	if (mImageFetcher != null) {
        		mImageFetcher.loadImage(logoImageUrl, mLogoImageView);
        	}
        }
        else {
        	// TODO - smart loading to take into account image display sizes
        	Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.user_group);
        	if (logoBitmap != null) {
        		mLogoImageView.setImageBitmap(logoBitmap);
        	}
        }
        
	}
	
	@Override
	public void onPause() {
		active = false;
		super.onPause();
	}
	
	
	@Override
	public void onResume() {
		active = true;
		super.onResume();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "onStart in DescriptionFragment");
		
		mPeopleCountTask = new CheckedInCountTask(); 
		mPeopleCountTask.execute(mLocation);
		mPeopleCountTask = null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (mPeopleCountTask != null) {
			mPeopleCountTask.cancel(true);
			mPeopleCountTask = null;
		}
		
		if (mLogoImageView != null) {
            // Cancel any pending image work
            ImageWorker.cancelWork(mLogoImageView);
            mLogoImageView.setImageDrawable(null);
        }
		
		getActivity().unregisterReceiver(mFeatureDataReceiver);
	}
	
	
	private class CheckedInCountTask extends  AsyncTask<Location, Void, ResponseHolder> {
		
		@Override
		protected ResponseHolder doInBackground(Location... params) {
			// we only pass in one parameter
			Location currentLocation = params[0];
			LocationContextManager locationContextMgr = currentLocation.getContextManager();
			
			if (locationContextMgr != null) {
				return locationContextMgr.getUserCount(getActivity());
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			if (holder != null && !holder.hasError()) {
				if (holder.getCode() == HttpStatus.SC_OK) {
					Context context = getActivity();
					
					try {
						JSONObject dataJSON = holder.getJsonContent();
						
						// response is just the integer count
						int peopleCount = dataJSON.getInt("response");
						
						// Display a toast message and update the view
						mDescriptionPeopleCount.setText("Checked in with " + (peopleCount - 1) + " other users.");
						
						
					} catch (JSONException e) {
						Log.d(TAG, holder.getResponseBody(), e);
					} 
				}
				// else fail silently - no update
			}
			else {
				Log.d(TAG, holder.getResponseBody(), holder.getError());
			}
		}
	}
	
	
	private class DescriptionFeatureDataReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// get the intent extras
			final Bundle extras = intent.getExtras();
			
			// get the feature category for which an update was performed
			String featureCategory = extras.getString("feature_category");
			
			Log.d(TAG, "Received update description notification with category: " + featureCategory);
			
			if (featureCategory.equals(Feature.DESCRIPTION)) {
				if (mDescriptionFeature.isInitialized()) {
					// the feature is already initialized, so update it with retrieved data 
					DescriptionFeature updatedDescriptionFeature = (DescriptionFeature) extras.getSerializable("feature_content");
					
					try {
						updatedDescriptionFeature.doUpdate();
						
						mDescriptionFeature = updatedDescriptionFeature;
						mLocation.setFeature(featureCategory, mDescriptionFeature);
						
						// update view
						bindDescriptionViewData();
					} catch (EnvSocialContentException e) {
						Log.d(TAG, "ERROR updating Description Feature", e);
					}
				}
				else {
					// the feature retrieved data for the first time, so initialize it
					DescriptionFeature newDescriptionFeature = (DescriptionFeature) extras.getSerializable("feature_content");
					
					try {
						newDescriptionFeature.init();
						
						mDescriptionFeature = newDescriptionFeature;
						mLocation.setFeature(featureCategory, mDescriptionFeature);
						
						// update view
						bindDescriptionViewData();
					} catch (EnvSocialContentException e) {
						Log.d(TAG, "ERROR initializing Description Feature", e);
					}
					
					// dismiss the waiting feature loader dialog if it is still running
					if (mFeatureLoadingDialog != null && mFeatureLoadingDialog.isShowing()) {
						mFeatureLoadingDialog.cancel();
					}
					
					Toast toast = Toast.makeText(getActivity(), "Feature data loading complete.", Toast.LENGTH_LONG);
					toast.show();
				}
				
				abortBroadcast();
			}
		}
		
	}
}
