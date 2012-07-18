package com.envsocial.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.Url;
import com.envsocial.android.utils.Preferences;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class HomeActivity extends SherlockFragmentActivity implements OnClickListener {

	private static final String SIGN_OUT = "Sign out";
	
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
        
        displayCheckedInLocation();
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		
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
			Location currentLocation = Preferences.getCheckedInLocation(this);
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
		if (Preferences.isLoggedIn(this)) {
			menu.add(SIGN_OUT);
		}
    	return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		if (item.getTitle().toString().compareTo(SIGN_OUT) == 0) {
			ActionHandler.logout(this);
			startActivity(new Intent(this, EnvSocialAppActivity.class));
			finish();
		}
		
		return true;
	}

}
