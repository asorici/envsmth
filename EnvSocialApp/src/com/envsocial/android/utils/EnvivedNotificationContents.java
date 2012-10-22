package com.envsocial.android.utils;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class EnvivedNotificationContents implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final String TAG = "EnvivedNotificationContents"; 
	
	public static final String FEATURE = "feature";
	public static final String LOCATION_URI = "location_uri";
	public static final String RESOURCE_URI = "resource_uri";
	public static final String PARAMS = "params";
	
	private String mLocationUri;
	private String mFeature;
	private String mResourceUri;
	private JSONObject mParams;
	
	private EnvivedNotificationContents(String locationUri, String feature, String resourceUri, JSONObject params) {
		mLocationUri = locationUri;
		mFeature = feature;
		mResourceUri = resourceUri;
		mParams = params;
	}
	
	public static EnvivedNotificationContents extractFromIntent(Context context, Intent intent) {
		String locationUri = intent.getStringExtra(LOCATION_URI);
		if (locationUri == null) {
			Log.d(TAG, "Location URI missing from GCM Envived Message. Notification aborted.");
			return null;
		}
		
		String feature = intent.getStringExtra(FEATURE);
		if (feature == null) {
			Log.d(TAG, "Feature type missing from GCM Envived Message. Notification aborted");
			return null;
		}
		
		String resourceUri = intent.getStringExtra(RESOURCE_URI);
		if (resourceUri == null) {
			Log.d(TAG, "Feature resourceUri missing from GCM Envived Message. Notification aborted");
			return null;
		}
		
		String params = intent.getStringExtra(PARAMS);
		if (params == null) {
			Log.d(TAG, "Notification parameters missing from GCM Envived Message. Notification aborted");
			return null;
		}
		
		JSONObject paramsJSON = null;
		try {
			paramsJSON = new JSONObject(params);
		} catch(JSONException ex) {
			Log.d(TAG, "Notification parameters (" + params + ") from GCM Envived Message could not be parsed. " +
					"Notification aborted");
			return null;
		}
		
		return new EnvivedNotificationContents(locationUri, feature, resourceUri, paramsJSON);
	}

	
	public String getLocationUri() {
		return mLocationUri;
	}

	public String getFeature() {
		return mFeature;
	}

	public String getResourceUri() {
		return mResourceUri;
	}

	public JSONObject getParams() {
		return mParams;
	}
	
	
}
