package com.envsocial.android;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.envsocial.android.api.Location;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.features.description.DescriptionFeature;
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
		Location currentLocation = Preferences.getCheckedInLocation(getApplicationContext());
		
		// if we have a checked in location; this is more of a safety check, we should not have
		// feature content updates issued to devices not checked in anywhere
		
		if (currentLocation != null) {
			// get the contents from the GCM message
			EnvivedNotificationContents notificationContents = 
					(EnvivedNotificationContents)intent.getSerializableExtra(DATA_RETRIEVE_SERVICE_INPUT);
			String featureCategory = notificationContents.getFeature();
			String featureResourceUrl = notificationContents.getResourceUrl();
			
			Feature updatedFeature = Feature.getFromServer(getApplicationContext(), 
												currentLocation, featureCategory, featureResourceUrl);
			
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
				Log.d(TAG, "Received NO feature update");
			}
		}
	}
}
