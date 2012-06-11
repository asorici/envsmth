package com.envsocial.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Url;
import com.envsocial.android.utils.Preferences;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class HomeActivity extends SherlockFragmentActivity implements OnClickListener {

	private static final String SIGN_OUT = "Sign out";
	
	private Button mBtnCheckin;
//	private Button mBtnRegister;
//	private Button mBtnUnregister;
	
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
        
        // TODO move everything related to c2dm
/*        mBtnRegister = (Button) findViewById(R.id.btn_c2dm_reg);
        mBtnRegister.setOnClickListener(this);
        
        mBtnUnregister = (Button) findViewById(R.id.btn_c2dm_unreg);
        mBtnUnregister.setOnClickListener(this);*/
	}
	
	public void onClick(View v) {
		if (v == mBtnCheckin) {
    		IntentIntegrator integrator = new IntentIntegrator(this);
    		integrator.initiateScan();
		} /*else if (v == mBtnRegister) {
			String regId = C2DMessaging.getRegistrationId(this);
            if (regId != null && !"".equals(regId)) {
            	try {
            		ActionHandler.registerWithServer(this, regId);
            	} catch (Exception e) {
            		e.printStackTrace();
            	}
            } else {
                C2DMessaging.register(this, C2DMReceiver.SENDER_ID);
            }
		} else if (v == mBtnUnregister) {
			C2DMessaging.unregister(this);
		}*/
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
