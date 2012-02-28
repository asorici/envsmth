package com.envsocial.android.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.envsocial.android.api.ActionHandler;
import com.google.android.c2dm.C2DMBaseReceiver;

public class C2DMReceiver extends C2DMBaseReceiver {

	public final static String SENDER_ID = "aqua.envsocial@gmail.com";
	public final static String NOTIFICATION_PERMISSION = "com.envsocial.android.permission.NOTIFICATION";
	public final static String ACTION_RECEIVE_NOTIFICATION = "com.envsocial.android.intent.RECEIVE_NOTIFICATION";
	
	public static final String NOTIFICATION = "notification";
	public static final String FEATURE = "feature";
	public static final String LOCATION_URI = "location_uri";
	public static final String RESOURCE_URI = "resource_uri";
	public static final String PARAMS = "params";
	
	public C2DMReceiver() {
		super(SENDER_ID);
	}

	@Override
    public void onRegistered(Context context, String registrationId) {
		try {
			ActionHandler.registerWithServer(context, registrationId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
    public void onUnregistered(Context context) {
		try {
			ActionHandler.unregisterWithServer(context);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onMessage(Context context, Intent intent) {
		Intent launcher = new Intent(ACTION_RECEIVE_NOTIFICATION);
		Bundle extras = intent.getExtras();
		launcher.putExtras(extras);
		sendOrderedBroadcast(launcher, NOTIFICATION_PERMISSION);
	}

	@Override
	public void onError(Context context, String errorId) {
		// TODO Auto-generated method stub

	}
	
}
