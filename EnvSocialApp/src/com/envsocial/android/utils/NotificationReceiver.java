package com.envsocial.android.utils;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import com.envsocial.android.GCMIntentService;

public class NotificationReceiver extends BroadcastReceiver {
	public static final String NEW_ORDER_NOTIFICATION = "new_order";
	public static final String RESOLVED_ORDER_NOTIFICATION = "resolved_order";
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		generateNotification(context, intent);
		abortBroadcast();
	}
	
	private static void generateNotification(Context context, Intent intent) {
		
		String locationUri = intent.getStringExtra(GCMIntentService.LOCATION_URI);
		String feature = intent.getStringExtra(GCMIntentService.FEATURE);
		String resourceUri = intent.getStringExtra(GCMIntentService.RESOURCE_URI);
		String params = intent.getStringExtra(GCMIntentService.PARAMS);
		
		JSONObject paramsJSON = null;
		NotificationDispatcher nd = null;
		
		try {
			if (params != null) {
				paramsJSON = new JSONObject(params);
			}
			
			nd = new NotificationDispatcher(context, locationUri, feature, resourceUri, paramsJSON);
		} catch(JSONException ex) {
			Log.e("Notification", "Envived notification params are not JSON parceable.", ex);
			nd = null;
		}
		
		if (nd != null) {
			String notificationType = paramsJSON.optString("type");
			
			// Create notification
			Notification notification = new Notification(nd.getNotificationIcon(), 
				nd.getNotificationTitle(), 
				nd.getNotificationWhen()
			);
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			
			if (notificationType.equals(NEW_ORDER_NOTIFICATION)) {
				// Create launcher intent
				Intent launcher = new Intent(Intent.ACTION_MAIN);
				launcher.setComponent(new ComponentName(context, 
						com.envsocial.android.EnvSocialAppActivity.class));
				launcher.addCategory(Intent.CATEGORY_LAUNCHER);
				launcher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_CLEAR_TOP);
				
				// Add extras
				launcher.putExtra(GCMIntentService.NOTIFICATION, true);
				launcher.putExtra(GCMIntentService.LOCATION_URI, locationUri);
				launcher.putExtra(GCMIntentService.FEATURE, feature);
				launcher.putExtra(GCMIntentService.RESOURCE_URI, resourceUri);
				launcher.putExtra(GCMIntentService.PARAMS, params);
				
				PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launcher, 0);
				notification.setLatestEventInfo(context, 
						nd.getNotificationTitle(), nd.getNotificationMessage(), pendingIntent);
			}
			else if (notificationType.equals(RESOLVED_ORDER_NOTIFICATION)) {
				PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, null, 0);
				notification.setLatestEventInfo(context, 
						nd.getNotificationTitle(), nd.getNotificationMessage(), pendingIntent);
			}
			
			NotificationManager nm = 
				(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
			nm.notify(nd.getNotificationId(), notification);
			playNotificationSound(context);
		}
	}
	
	public static void playNotificationSound(Context context) {
		Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		if (uri != null) {
			Ringtone rt = RingtoneManager.getRingtone(context, uri);
			if (rt != null) {
				rt.setStreamType(AudioManager.STREAM_NOTIFICATION);
				rt.play();
			}
		}
	}
}
