package com.envsocial.android.utils;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.envsocial.android.GCMIntentService;
import com.envsocial.android.R;
import com.envsocial.android.features.Feature;

public class NotificationDispatcher {
	
	// TODO
	private Context mContext;
	private String mLocationUri;
	private String mFeature;
	private String mResourceUri;
	private JSONObject mParams;
	
	private int mId;
	private int mIconId;
	private String mTitle;
	private long mWhen;
	private String mMessage;
	
	public NotificationDispatcher(Context context, String locationUri, 
			String feature, String resourceUri, JSONObject params) {
		
		mContext = context;
		mLocationUri = locationUri;
		mFeature = feature;
		mResourceUri = resourceUri;
		mParams = params;
		
		if (mFeature.compareTo(Feature.ORDER) == 0) {
			
			if (mParams != null && mParams.optString("type", null) != null 
				&& mParams.optString("type").equalsIgnoreCase(NotificationReceiver.RESOLVED_ORDER_NOTIFICATION)) {
				
				mId = R.string.resolved_order;
				mIconId = R.drawable.ic_launcher;
				mTitle = mContext.getResources().getString(R.string.resolved_order);
				mWhen = System.currentTimeMillis();
				mMessage = "Your order is on the way!";
				
			}
			else if (mParams != null && mParams.optString("type", null) != null 
					&& mParams.optString("type").equalsIgnoreCase(NotificationReceiver.NEW_ORDER_NOTIFICATION)) {
				mId = R.string.incoming_order;
				mIconId = R.drawable.ic_launcher;
				mTitle = mContext.getResources().getString(R.string.incoming_order);
				mWhen = System.currentTimeMillis();
				
				mMessage = "You have new orders!";
			}
		}
	}
	
	
	public int getNotificationId() {
		return mId;
	}
	
	public int getNotificationIcon() {
		return mIconId;
	}
	
	public String getNotificationTitle() {
		return mTitle;
	}
	
	public long getNotificationWhen() {
		return mWhen;
	}
	
	public String getNotificationMessage() {
		return mMessage;
	}
	
	
	public static Intent[] getIntentStack(Context context, 
			String locationUri, String feature, String resourceUri, String params) {
		
		Intent[] intents = new Intent[2];
		
		intents[0] = new Intent(context, com.envsocial.android.HomeActivity.class);
		intents[1] = new Intent(context, com.envsocial.android.DetailsActivity.class);
		intents[1].putExtra(GCMIntentService.NOTIFICATION, true);
		
		return null;
	}
	
}
