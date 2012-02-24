package com.envsocial.android.utils;

import com.envsocial.android.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

public class NotificationReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String locationUri = intent.getStringExtra("location_uri");
		generateNotification(context, locationUri, "New order", intent);
		abortBroadcast();
	}

	public static void generateNotification(Context context, String msg, String title, Intent intent) {
		int icon = R.drawable.ic_launcher;
		long when = System.currentTimeMillis();
		
		Notification notification = new Notification(icon, title, when);
		notification.setLatestEventInfo(context, title, msg, 
				PendingIntent.getActivity(context, 0, intent, 0));
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		NotificationManager nm = 
			(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(1, notification);
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
