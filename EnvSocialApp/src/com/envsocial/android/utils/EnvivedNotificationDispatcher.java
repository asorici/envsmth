package com.envsocial.android.utils;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.envsocial.android.EnvivedFeatureUpdateService;
import com.envsocial.android.features.Feature;
import com.envsocial.android.features.order.NewOrderRequestNotification;
import com.envsocial.android.features.order.OrderFeature;
import com.envsocial.android.features.order.ResolvedOrderRequestNotification;

/**
 * Acts like a factory class dispatching Envived GCM Notification messages
 * to the appropriate handler class.
 * @author alex
 *
 */
public class EnvivedNotificationDispatcher extends EnvivedReceiver {
	
	private static final String TAG = "EnvivedNotificationDispatcher";
	
	@Override
	public boolean handleNotification(Context context, Intent intent,
			EnvivedNotificationContents notificationContents) {
		
		String feature = notificationContents.getFeature();
		
		// big if-else statement to determine appropriate handler class
		if (feature.compareTo(Feature.ORDER) == 0) {
			JSONObject paramsJSON = notificationContents.getParams();
			
			// order notifications MUST have a type parameter 
			if (paramsJSON.optString("type", null) != null 
				&& paramsJSON.optString("type").compareTo(OrderFeature.NEW_REQUEST_NOTIFICATION) == 0) {
				new NewOrderRequestNotification(context, intent, notificationContents).sendNotification();
				
				return true;
			}
			else if (paramsJSON.optString("type", null) != null 
				&& paramsJSON.optString("type").compareTo(OrderFeature.RESOLVED_REQUEST_NOTIFICATION) == 0) {
				new ResolvedOrderRequestNotification(context, intent, notificationContents).sendNotification();
				
				return true;
			}
			else if (paramsJSON.optString("type", null) != null 
					&& paramsJSON.optString("type").compareTo(OrderFeature.UPDATE_CONTENT_NOTIFICATION) == 0) {
				// start the update service directly
				Intent updateService = new Intent(context, EnvivedFeatureUpdateService.class);
				updateService.putExtra(
						EnvivedFeatureUpdateService.UPDATE_SERVICE_INPUT, notificationContents);
				
				context.startService(updateService);
			}
			
			Log.d(TAG, "Order notification dispatch error: 'type` parameter missing or unknown in " 
						+ paramsJSON.toString());
		}
		
		return false;
	}
	
}
