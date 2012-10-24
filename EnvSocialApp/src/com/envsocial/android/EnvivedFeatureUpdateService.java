package com.envsocial.android;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.EnvivedNotificationContents;
import com.envsocial.android.utils.Preferences;

public class EnvivedFeatureUpdateService extends IntentService {
	public static String UPDATE_SERVICE_NAME = "EnvivedFeatureUpdateService";
	public static String UPDATE_SERVICE_INPUT = "com.envsocial.android.Input";
	public static String ACTION_UPDATE_FEATURE = "com.envsocial.android.intent.UPDATE_FEATURE";
	public static String UPDATE_PERMISSION = "com.envsocial.android.permission.UPDATE";
	
	public EnvivedFeatureUpdateService() {
		super(UPDATE_SERVICE_NAME);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Location currentLocation = Preferences.getCheckedInLocation(getApplicationContext());
		
		// if we have a checked in location; this is more of a safety check, we should not have
		// feature content updates issued to devices not checked in anywhere
		
		if (currentLocation != null) {
			// get the contents from the GCM message
			EnvivedNotificationContents notificationContents = 
					(EnvivedNotificationContents)intent.getSerializableExtra(UPDATE_SERVICE_INPUT);
			String featureCategory = notificationContents.getFeature();
			
			Feature updatedFeature = Feature.getFromServer(getApplicationContext(), 
												currentLocation, featureCategory);
			
			if (updatedFeature != null) {
				// we have successfully obtained the new feature contents
				// use an intent to signal to active listeners that they may refresh their feature contents
				Intent updateIntent = new Intent(ACTION_UPDATE_FEATURE);
				Bundle extras = new Bundle();
				extras.putString("feature_category", featureCategory);
				extras.putSerializable("feature_object", updatedFeature);
				
				updateIntent.putExtras(extras);
				sendOrderedBroadcast(updateIntent, UPDATE_PERMISSION);
			}
		}
	}
}
