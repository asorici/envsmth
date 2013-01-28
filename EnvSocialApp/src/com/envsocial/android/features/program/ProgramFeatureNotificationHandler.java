package com.envsocial.android.features.program;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.envsocial.android.EnvivedFeatureDataRetrievalService;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.EnvivedNotificationContents;
import com.envsocial.android.utils.EnvivedNotificationHandler;

public class ProgramFeatureNotificationHandler extends EnvivedNotificationHandler {
	private static final long serialVersionUID = 1L;
	private static final String TAG = "ProgramFeatureNotificationHandler";
	
	@Override
	public boolean handleNotification(Context context, Intent intent,
			EnvivedNotificationContents notificationContents) {
		
		String feature = notificationContents.getFeature();
		
		// big if-else statement to determine appropriate handler class
		if (feature.compareTo(Feature.PROGRAM) == 0) {
			JSONObject paramsJSON = notificationContents.getParams();
			
			if (paramsJSON.optString("type", null) != null 
					&& paramsJSON.optString("type").compareTo(Feature.RETRIEVE_CONTENT_NOTIFICATION) == 0) {
				
				// start the update service directly
				Intent updateService = new Intent(context, EnvivedFeatureDataRetrievalService.class);
				updateService.putExtra(
						EnvivedFeatureDataRetrievalService.DATA_RETRIEVE_SERVICE_INPUT, notificationContents);
				
				context.startService(updateService);
			}
			
			Log.d(TAG, "Program notification dispatch error: 'type` parameter missing or unknown in " 
						+ paramsJSON.toString());
		}
		
		return false;
		
	}

}
