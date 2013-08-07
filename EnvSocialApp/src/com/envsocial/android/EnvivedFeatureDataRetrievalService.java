package com.envsocial.android;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.EnvivedNotificationContents;
import com.envsocial.android.utils.Preferences;

public class EnvivedFeatureDataRetrievalService extends IntentService {
	private static final String TAG = "EnvivedFeatureDataRetrievalService";
	
	public static String DATA_RETRIEVE_SERVICE_NAME = "EnvivedFeatureDataRetrievalService";
	public static String DATA_RETRIEVE_SERVICE_INPUT = "com.envsocial.android.Input";
	public static String ACTION_FEATURE_RETRIEVE_DATA = "com.envsocial.android.intent.FEATURE_RETRIEVE_DATA";
	public static String FEATURE_RETRIEVE_DATA_PERMISSION = "com.envsocial.android.permission.FEATURE_RETRIEVE_DATA";
	
	public EnvivedFeatureDataRetrievalService() {
		super(DATA_RETRIEVE_SERVICE_NAME);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String userUrl = Preferences.getUserUri(getApplicationContext());
		Location location = Preferences.getCheckedInLocation(getApplicationContext());
		
		EnvivedNotificationContents notificationContents = 
			(EnvivedNotificationContents)intent.getSerializableExtra(DATA_RETRIEVE_SERVICE_INPUT);
		String featureLocationUrl = notificationContents.getLocationUrl();
		
		/* 
		 * if we are updating a feature that is attached to the physical 
		 * checked in location, then set the virtualAccess to false
		 */
		boolean virtualAccess = true;
		if (location != null && location.getLocationUri().equals(featureLocationUrl)) {
			virtualAccess = false;
		}
		
		// if we have a user uri; this safety check is passed by both anonymous and real users, 
		// since both get a userUri; it is also passed by virtual checkin since a userUri is retained 
		// then as well
		if (userUrl != null) {
			// get the contents from the GCM message
			
			String featureCategory = notificationContents.getFeature();
			String featureResourceUrl = notificationContents.getResourceUrl();
			
			Feature updatedFeature = Feature.getFromServer(getApplicationContext(), 
												featureCategory, featureResourceUrl, virtualAccess);
			
			if (updatedFeature != null) {
				// we have successfully obtained the new feature contents
				// use an intent to signal to active listeners that they may refresh their feature contents
				
				Intent updateIntent = new Intent(ACTION_FEATURE_RETRIEVE_DATA);
				Bundle extras = new Bundle();
				extras.putString("feature_category", featureCategory);
				extras.putSerializable("feature_content", updatedFeature);
				
				updateIntent.putExtras(extras);
				//sendBroadcast(updateIntent);
				sendOrderedBroadcast(updateIntent, FEATURE_RETRIEVE_DATA_PERMISSION);
			}
			else {
				//Log.d(TAG, "Received NO feature update");
			}
		}
	}
}
