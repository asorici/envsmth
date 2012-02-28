package com.envsocial.android.utils;

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

public class NotificationReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		generateNotification(context, intent);
		abortBroadcast();
	}

	private static void generateNotification(Context context, Intent intent) {
		
		String locationUri = intent.getStringExtra(C2DMReceiver.LOCATION_URI);
		String feature = intent.getStringExtra(C2DMReceiver.FEATURE);
		String resourceUri = intent.getStringExtra(C2DMReceiver.RESOURCE_URI);
		String params = intent.getStringExtra(C2DMReceiver.PARAMS);
		
		NotificationDispatcher nd = 
			new NotificationDispatcher(context, locationUri, feature, resourceUri, params);
		
		// Create launcher intent
		Intent launcher = new Intent(Intent.ACTION_MAIN);
		launcher.setComponent(new ComponentName(context, 
				com.envsocial.android.EnvSocialAppActivity.class));
		launcher.addCategory(Intent.CATEGORY_LAUNCHER);
		launcher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		// Add extras
		launcher.putExtra(C2DMReceiver.NOTIFICATION, true);
		launcher.putExtra(C2DMReceiver.LOCATION_URI, locationUri);
		launcher.putExtra(C2DMReceiver.FEATURE, feature);
		launcher.putExtra(C2DMReceiver.RESOURCE_URI, resourceUri);
		launcher.putExtra(C2DMReceiver.PARAMS, params);
		
		// Create notification
		Notification notification = new Notification(nd.getNotificationIcon(), 
				nd.getNotificationTitle(), 
				nd.getNotificationWhen()
				);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launcher, 0);
		notification.setLatestEventInfo(context, 
				nd.getNotificationTitle(), nd.getNotificationMessage(), pendingIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		NotificationManager nm = 
			(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(nd.getNotificationId(), notification);
		playNotificationSound(context);
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
