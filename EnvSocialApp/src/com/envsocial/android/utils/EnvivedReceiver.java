package com.envsocial.android.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public abstract class EnvivedReceiver extends BroadcastReceiver {
	private static final String TAG = "EnvivedReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		EnvivedNotificationContents notificationContents = 
				EnvivedNotificationContents.extractFromIntent(context, intent);
		
		if (notificationContents != null && handleNotification(context, intent, notificationContents)) {
			abortBroadcast();
		}
		else if(notificationContents == null) {
			Log.d(TAG, "Error receiving ENVIVED notification. Notification contents are missing " +
					"or unparseable in intent: " + intent.getDataString());
		}
	}
	
	public abstract boolean handleNotification(Context context, 
			Intent intent, EnvivedNotificationContents notificationContents);
}
