package com.envsocial.android.utils;

import java.text.ParseException;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.envsocial.android.api.AppClient;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.user.User;


public final class Preferences {
	private static final String TAG = "Preferences";
	
	private static final String EMAIL = "email";
	private static final String FIRST_NAME = "first_name";
	private static final String LAST_NAME = "last_name";
	private static final String USER_URI = "user_uri";
	private static final String CHECKED_IN_LOCATION = "checked_in_location";
	private static final String PEOPLE_IN_LOCATION = "people_in_location";
	private static final String FEATURE_LRU_TRACKER = "feature_lru_tracker";
	
	public static void login(Context context, String email, String firstName, String lastName, String uri) {
		setStringPreference(context, EMAIL, email);
		setStringPreference(context, FIRST_NAME, firstName);
		setStringPreference(context, LAST_NAME, lastName);
		setStringPreference(context, USER_URI, uri);
	}
	
	public static void logout(Context context) {
		removeStringPreference(context, EMAIL);
		removeStringPreference(context, USER_URI);
		removeStringPreference(context, AppClient.SESSIONID);
		removeStringPreference(context, PEOPLE_IN_LOCATION);
		removeStringPreference(context, FIRST_NAME);
		removeStringPreference(context, LAST_NAME);
	}
	
	public static boolean isLoggedIn(Context context) {
		return !(getLoggedInUserEmail(context) == null);
	}
	
	public static String getLoggedInUserEmail(Context context) {
		return getStringPreference(context, EMAIL);
	}
	
	public static String getLoggedInUserUri(Context context) {
		return getStringPreference(context, USER_URI);
	}
	
	public static String getLoggedInUserFirstName(Context context) { 
		return getStringPreference(context, FIRST_NAME);
	}
	
	public static String getLoggedInUserLastName(Context context) { 
		return getStringPreference(context, LAST_NAME);
	}
	
	public static String getSessionId(Context context) {
		return getStringPreference(context, AppClient.SESSIONID);
	}
	
	
	public static void checkin(Context context, String userUri, Location location) {
		setStringPreference(context, CHECKED_IN_LOCATION, location.serialize());
		setStringPreference(context, USER_URI, userUri);
	}
	
	public static void checkout(Context context) {
		Location currentLocation = getCheckedInLocation(context);
		
		if (currentLocation != null) {
			Log.d("CHECKOUT", "doing checkout from " + currentLocation.getName());
			currentLocation.doClose(context);
		}
		
		removeStringPreference(context, CHECKED_IN_LOCATION);
		removeStringPreference(context, PEOPLE_IN_LOCATION);
		removeStringPreference(context, USER_URI);
		
		// if we didn't log in
		if (!isLoggedIn(context)) {
			// remove the client SESSION ID
			removeStringPreference(context, AppClient.SESSIONID);
		}
	}
	
	public static Location getCheckedInLocation(Context context) {
		String jsonString = getStringPreference(context, CHECKED_IN_LOCATION);
		if (jsonString != null) {
			try {
				//return new Location(jsonString);
				return Location.fromSerialized(jsonString);
			} catch (JSONException e) {
				e.printStackTrace();
				removeStringPreference(context, CHECKED_IN_LOCATION);
			}
		}
		
		return null;
	}
	
	public static boolean isCheckedIn(Context context) {
		return getCheckedInLocation(context) != null;
	}
	
	
	public static String getSerializedFeatureData(Context context, String localCacheFileName) {
		return getStringPreference(context, localCacheFileName);
	}
	
	public static void setSerializedFeatureData(Context context, String localCacheFileName, String serializedFeatureData) {
		setStringPreference(context, localCacheFileName, serializedFeatureData);
	}
	
	public static void removeSerializedFeatureData(Context context, String localCacheFileName) {
		removeStringPreference(context, localCacheFileName);
	}
	
	public static boolean featureDataCacheExists(Context context, String localCacheFileName) {
		return getStringPreference(context, localCacheFileName) != null;
	}
	
	public static FeatureLRUTracker getFeatureLRUTracker(Context context) {
		String jsonString = getStringPreference(context, FEATURE_LRU_TRACKER);
		
		try {
			if (jsonString != null) {
				return FeatureLRUTracker.fromJSON(jsonString);
			}
			
			return null;
		}
		catch (JSONException ex) {
			Log.d(TAG, "ERROR reading feature lru tracker from JSON serialized string.", ex);
			return null;
		} catch (ParseException ex) {
			Log.d(TAG, "ERROR reading feature lru tracker from JSON serialized string.", ex);
			return null;
		}
	}
	
	
	public static void setFeatureLRUTracker(Context context, FeatureLRUTracker featureLRUTracker) {
		try {
			String jsonString = featureLRUTracker.toJSON();
			setStringPreference(context, FEATURE_LRU_TRACKER, jsonString);
		}
		catch (JSONException ex) {
			Log.d(TAG, "ERROR json serializing feature lru tracker", ex);
		}
	}
	
	
	public static void setPeopleInLocation(Context context, String peopleString) {
		setStringPreference(context, PEOPLE_IN_LOCATION, peopleString);
	}
	
	public static List<User> getPeopleInLocation(Context context, Location location) {
		if (location == null) {
			location = getCheckedInLocation(context);
		}
		
		String jsonString = getStringPreference(context, PEOPLE_IN_LOCATION);
		if (jsonString != null) {
			try {
				return User.getUsers(context, location, null, jsonString);
			} catch (JSONException e) {
				e.printStackTrace();
				removeStringPreference(context, CHECKED_IN_LOCATION);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	public static void setStringPreference(Context context, String name, String value) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		
		editor.putString(name, value);
		editor.commit();
	}
	
	public static String getStringPreference(Context context, String name) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		return settings.getString(name, null);
	}
	
	public static void removeStringPreference(Context context, String name) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove(name);
		editor.commit();
	}
}
