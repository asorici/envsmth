package com.envsocial.android.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.google.android.c2dm.C2DMBaseReceiver;

public class C2DMReceiver extends C2DMBaseReceiver {

	public final static String SENDER_ID = "aqua.envsocial@gmail.com";
	
	public static final String NOTIFICATION = "notification";
	public static final String FEATURE = "feature";
	public static final String LOCATION_URI = "location_uri";
	public static final String RESOURCE_URI = "resource_uri";
	public static final String PARAMS = "params";
	
	public C2DMReceiver() {
		super(SENDER_ID);
		System.out.println("[DEBUG]>> C2DMReceiver started.");
	}

	@Override
    public void onRegistered(Context context, String registrationId) {
		System.out.println("[DEBUG]>> C2DMReceiver got reg confirmation.");
		try {
			ActionHandler.registerWithServer(context, registrationId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
    public void onUnregistered(Context context) {
		System.out.println("[DEBUG]>> C2DMReceiver got unreg confirmation.");
		try {
			ActionHandler.unregisterWithServer(context);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onMessage(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Bundle extras = intent.getExtras();
		String locationUri = extras.getString(LOCATION_URI);
		String feature = extras.getString(FEATURE);
		String resourceUri = extras.getString(RESOURCE_URI);
		String params = extras.getString(PARAMS);
		
		System.out.println("[DEBUG]>> Got update message: [" 
				+ locationUri + ", " + feature + ", " + resourceUri + ", " + params + "]");
		
		Intent launcher = new Intent(Intent.ACTION_MAIN);
		launcher.setComponent(new ComponentName(context, 
				com.envsocial.android.EnvSocialAppActivity.class));
		launcher.addCategory(Intent.CATEGORY_LAUNCHER);
		launcher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
//		generateNotification(context, locationUri, "New order", launcher);
		dispatchNotification(context, locationUri, feature, resourceUri, params);
//		generateNotification(context, locationUri, "New order", new Intent(this, DetailsActivity.class));
		sendOrderedBroadcast(launcher, "com.envsocial.android.permission.NOTIFICATION");
	}

	@Override
	public void onError(Context context, String errorId) {
		// TODO Auto-generated method stub

	}
	
	private static void dispatchNotification(Context context, String locationUri, 
			String feature, String resourceUri, String params) {
		
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
		launcher.putExtra(NOTIFICATION, true);
		launcher.putExtra(LOCATION_URI, locationUri);
		launcher.putExtra(FEATURE, feature);
		launcher.putExtra(RESOURCE_URI, resourceUri);
		launcher.putExtra(PARAMS, params);
		
		// Create notification
		Notification notification = new Notification(nd.getNotificationIcon(), 
				nd.getNotificationTitle(), 
				nd.getNotificationWhen()
				);
		
		PendingIntent intent = PendingIntent.getActivity(context, 0, launcher, 0);
		notification.setLatestEventInfo(context, 
				nd.getNotificationTitle(), nd.getNotificationMessage(), intent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		NotificationManager nm = 
			(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(nd.getNotificationId(), notification);
		playNotificationSound(context);
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
