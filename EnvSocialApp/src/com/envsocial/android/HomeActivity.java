package com.envsocial.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.Url;
import com.envsocial.android.api.exceptions.EnvSocialComException;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.ResponseHolder;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class HomeActivity extends SherlockFragmentActivity implements OnClickListener {
	
	private static final String TAG = "HomeActivity";
	private static final String SIGN_OUT = "Sign out";
	private static final String CHECK_OUT = "Check out";
	
	private Button mBtnCheckin;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
        	// From notification, we forward to details
        	Intent intent = new Intent(this, DetailsActivity.class);
        	intent.putExtras(bundle);
        	startActivity(intent);
        }
        
        setContentView(R.layout.home);
        
        // Set up action bar.
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mBtnCheckin = (Button) findViewById(R.id.btn_checkin);
        mBtnCheckin.setOnClickListener(this);
	}
	
	
	@Override
	public void onDestroy() {
		// if there is a current location
		Location currentLocation = Preferences.getCheckedInLocation(this);
		if (currentLocation != null) {
			// perform cleanup on exit
			currentLocation.doCleanup(getApplicationContext());
		}
		
		super.onDestroy();
	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		
		// reset location if we have checked in at another activity in the meantime
		displayCheckedInLocation();
		this.findViewById(R.id.checked_in_location_name).invalidate();
	}
	
	
	private void displayCheckedInLocation() {
		TextView v = (TextView)findViewById(R.id.checked_in_location_name);
        Location currentLocation = Preferences.getCheckedInLocation(this);
        //System.out.println("[DEBUG]>> Current checkin location: " + currentLocation);
        if (currentLocation != null) {
        	v.setText(currentLocation.getName());
        }
        else {
        	v.setText(R.string.lbl_no_checkin_location);
        }
	}


	public void onClick(View v) {
		if (v == mBtnCheckin) {
			final Location currentLocation = Preferences.getCheckedInLocation(this);
			if (currentLocation != null) {
				String dialogMessage = "Keep previous checkin location ("  
						+ currentLocation.getName() + ") ?";
				
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				
				LayoutInflater inflater = getLayoutInflater();
				//ViewGroup dialogViewGroup = (ViewGroup)findViewById(R.id.view_home);
				TextView titleDialogView = (TextView)inflater.inflate(R.layout.home_select_checkin_title, null, false);
				titleDialogView.setText("Select Checkin Location");
				
				TextView bodyDialogView = (TextView)inflater.inflate(R.layout.home_select_checkin_body, null, false);
				bodyDialogView.setText(dialogMessage);
				
				builder.setCustomTitle(titleDialogView);
				builder.setView(bodyDialogView);
				
				builder.setPositiveButton("Yes", new Dialog.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) { 
				    	dialog.cancel();
				    	
				    	Intent i = new Intent(HomeActivity.this, DetailsActivity.class);
			    		// we can put null for the payload since we have a location saved in preferences
				    	i.putExtra(ActionHandler.CHECKIN, (String)null);
			    		startActivity(i);
				    }
				});

				builder.setNegativeButton("No", new Dialog.OnClickListener() {

				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				    	dialog.cancel();
				    	
				    	// do a local checkout before checking in somewhere else
				    	// for now this will also refresh any feature data that was renewed server-side
				    	Preferences.checkout(getApplicationContext());
				    	
				    	IntentIntegrator integrator = new IntentIntegrator(HomeActivity.this);
						integrator.initiateScan();
				    }

				});

				builder.show();
			}
			else {
				IntentIntegrator integrator = new IntentIntegrator(this);
				integrator.initiateScan();
			}
		} 
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK) {
			if (requestCode == IntentIntegrator.REQUEST_CODE) {
				// We have a checkin action, grab checkin url from scanned QR code
				IntentResult scanResult = 
					IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		    	if (scanResult != null) {
		    		String actionUrl = scanResult.getContents();
		    		// Check if checkin url is proper
		    		if (actionUrl.startsWith(Url.actionUrl(ActionHandler.CHECKIN))) {
		    			Intent i = new Intent(this, DetailsActivity.class);
			    		i.putExtra(ActionHandler.CHECKIN, actionUrl);
			    		startActivity(i);
		    		} else {
		    			// If not, inform the user
		    			Toast toast = Toast.makeText(this, 
		    					R.string.msg_malformed_checkin_url, Toast.LENGTH_LONG);
		    			toast.show();
		    		}
		    	}
			}
		}
		else {
			//System.err.println("WOUND UP HERE");
			Toast toast = Toast.makeText(this, "Action Canceled or Connection Error.", Toast.LENGTH_LONG);
			toast.show();
		}
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// add the search button
		MenuItem item = menu.add(R.string.menu_search);
        item.setIcon(R.drawable.ic_menu_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		if (Preferences.isLoggedIn(this)) {
			menu.add(SIGN_OUT);
			menu.add(CHECK_OUT);
		}
		else {
			menu.add(CHECK_OUT);
		}
		
    	return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle().toString().compareTo(SIGN_OUT) == 0) {
			new LogoutTask().execute();
		}
		
		else if (item.getTitle().toString().compareTo(CHECK_OUT) == 0) {
			new CheckoutTask().execute();
		}
		
		else if (item.getTitle().toString().compareTo(getString(R.string.menu_search)) == 0) {
			return onSearchRequested();
		}
		
		return true;
	}

	
	private class LogoutTask extends AsyncTask<Void, Void, ResponseHolder> {
		private ProgressDialog mLoadingDialog;
		
		@Override
		protected void onPreExecute() {
			mLoadingDialog = ProgressDialog.show(HomeActivity.this, 
					"", "Signing out ...", true);
		}
		
		@Override
		protected ResponseHolder doInBackground(Void...args) {
			return ActionHandler.logout(getApplicationContext());
		}
		
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			mLoadingDialog.cancel();
			
			if (!holder.hasError()) {
				// also checkout before going back to the main activity
				Preferences.checkout(getApplicationContext());

				startActivity(new Intent(HomeActivity.this, EnvSocialAppActivity.class));
				finish();
			}
			else {
				// TODO: 	for now log the communication error and checkout anyway
				//			need to figure out a way to re-establish consistency 

				try {
					throw holder.getError();
				} catch (EnvSocialComException e) {
					Log.d(TAG, e.getMessage(), e);
				} catch (EnvSocialContentException e) {
					Log.d(TAG, e.getMessage(), e);
				} catch (Exception e) {
					Log.d(TAG, e.toString(), e);
				}
				
				// checkout anyway before going back to the main activity
				Preferences.checkout(getApplicationContext());
				
				startActivity(new Intent(HomeActivity.this, EnvSocialAppActivity.class));
				finish();
			}
		}
	}
	
	
	private class CheckoutTask extends AsyncTask<Void, Void, ResponseHolder> {
		private ProgressDialog mLoadingDialog;
		
		@Override
		protected void onPreExecute() {
			mLoadingDialog = ProgressDialog.show(HomeActivity.this, 
					"", "Checking out ...", true);
		}
		
		@Override
		protected ResponseHolder doInBackground(Void...args) {
			return ActionHandler.checkout(getApplicationContext());
		}
		
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			mLoadingDialog.cancel();
			
			if (!holder.hasError()) {
				startActivity(new Intent(HomeActivity.this, EnvSocialAppActivity.class));
				finish();
			}
			else {
				// TODO: 	for now log the communication error and checkout anyway
				//			need to figure out a way to re-establish consistency 
				
				try {
					throw holder.getError();
				} catch (EnvSocialComException e) {
					Log.d(TAG, e.getMessage(), e);
				} catch (EnvSocialContentException e) {
					Log.d(TAG, e.getMessage(), e);
				} catch (Exception e) {
					Log.d(TAG, e.toString(), e);
				}

				// checkout anyway before going back to the main activity
				Preferences.checkout(getApplicationContext());
				finish();
			}
		}
	}
}
