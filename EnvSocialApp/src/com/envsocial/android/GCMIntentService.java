package com.envsocial.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.ResponseHolder;
import com.envsocial.android.utils.Utils;
import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

public class GCMIntentService extends GCMBaseIntentService {
	private final static String TAG = "GCMIntentService";
	public final static String SENDER_ID = "127142975039";
	
	public static final String ACTION_DISPLAY_GCM_MESSAGE = "com.envsocial.android.intent.DISPLAY_GCM_MESSAGE";
	public static final String EXTRA_GCM_MESSAGE = "com.envsocial.android.gcm_message";
	
	public final static String NOTIFICATION_PERMISSION = "com.envsocial.android.permission.NOTIFICATION";
	public final static String ACTION_RECEIVE_NOTIFICATION = "com.envsocial.android.intent.RECEIVE_NOTIFICATION";
	public static final String NOTIFICATION = "notification";
	
	public GCMIntentService() {
		super(SENDER_ID);
	}
	
	@Override
	protected void onError(Context context, String errorId) {
		Log.d(TAG, "Error in GCM Notification: " + errorId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Intent launcher = new Intent(ACTION_RECEIVE_NOTIFICATION);
		Bundle extras = intent.getExtras();
		
		launcher.putExtras(extras);
		sendOrderedBroadcast(launcher, NOTIFICATION_PERMISSION);
	}

	@Override
	protected void onRegistered(Context context, String regId) {
		Log.d(TAG, "Received registration id: " + regId);
		
		if (Preferences.getUserUri(context) != null) {
			ResponseHolder holder = ActionHandler.registerWithServer(context, regId);
			if (!holder.hasError()) {
				// mark the device as registered with the server as well
				GCMRegistrar.setRegisteredOnServer(context, true);
				Log.d(TAG, "---- REGISTERED WITH OUR SERVER");
				
				Utils.sendGCMStatusMessage(context, context.getString(R.string.gcm_registered));
			}
			else {
				Log.d(TAG, "---- THERE WAS AN ERROR IN REG WITH OUR SERVER: " + holder.getError().getMessage());
				Utils.sendGCMStatusMessage(context, context.getString(R.string.gcm_register_error));
			}
		}
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		Log.d(TAG, "Unregistered regId: " + regId + " from GCM.");
		
		ActionHandler.unregisterWithServer(context);
		GCMRegistrar.setRegisteredOnServer(context, false);
		
		Utils.sendGCMStatusMessage(context, context.getString(R.string.gcm_unregistered));
	}

}
