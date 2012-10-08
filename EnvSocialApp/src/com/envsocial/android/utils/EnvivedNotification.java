package com.envsocial.android.utils;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

public abstract class EnvivedNotification {
	
	protected EnvivedNotificationContents mNotificationContents;
	protected Context mContext;
	protected Intent mIntent;
	
	protected EnvivedNotification(Context context, Intent intent, 
		EnvivedNotificationContents notificationContents) {
		mContext = context;
		mIntent = intent;
		mNotificationContents = notificationContents;
		
	}
	
	public abstract int getNotificationId();
		
	public abstract int getNotificationIcon();
	
	public abstract String getNotificationTitle();
	
	public abstract long getNotificationWhen();
	
	public abstract String getNotificationMessage();
	
	public abstract void sendNotification();
	
	
	public Intent[] getIntentStack() {
		// the default does nothing
		
		return null;
	}
	
	public void playNotificationSound(Context context) {
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
