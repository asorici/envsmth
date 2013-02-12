package com.envsocial.android.utils;

import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.google.android.gcm.GCMRegistrar;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class RegisterEnvivedNotificationsTask extends AsyncTask<String, Void, ResponseHolder> {
	private static final String TAG = "RegisterEnvivedNotificationsTask";
	

	private Context mContext;
	
	public RegisterEnvivedNotificationsTask(Context context) {
		mContext = context;
	}
	
	@Override
    protected ResponseHolder doInBackground(String... params) {
		String regId = params[0];
		
    	ResponseHolder holder = ActionHandler.registerWithServer(mContext, regId);
    	
        // At this point all attempts to register with the app
        // server failed, so we need to unregister the device
        // from GCM - the app will try to register again when
        // it is restarted. Note that GCM will send an
        // unregistered callback upon completion, but
        // GCMIntentService.onUnregistered() will ignore it.
        if (holder.hasError()) {
        	Log.d(TAG, "Registration error: " + holder.getError().getMessage(), holder.getError());
        	GCMRegistrar.unregister(mContext);
        }
        
        return holder;
    }

    @Override
    protected void onPostExecute(ResponseHolder holder) {
    	if (holder.hasError()) {
        	Toast toast = Toast.makeText(mContext, 
        			R.string.msg_gcm_registration_error, Toast.LENGTH_LONG);
			toast.show();
    	}
    	else {
    		GCMRegistrar.setRegisteredOnServer(mContext, true);
    		
    		Toast toast = Toast.makeText(mContext, R.string.msg_gcm_registered, Toast.LENGTH_LONG);
			toast.show();
    	}
    }

}
