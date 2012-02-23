package com.envsocial.android.utils;

import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.envsocial.android.api.AppClient;
import com.envsocial.android.api.Location;


public final class Preferences {
	
	private static final String EMAIL = "email";
	private static final String USER_URI = "user_uri";
	private static final String CHECKED_IN_LOCATION = "checked_in_location";

	
	public static void login(Context context, String email, String uri) {
		setStringPreference(context, EMAIL, email);
		setStringPreference(context, USER_URI, uri);
	}
	
	public static void logout(Context context) {
		removeStringPreference(context, EMAIL);
		removeStringPreference(context, USER_URI);
		removeStringPreference(context, AppClient.SESSIONID);
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
	
	
	public static String getSessionId(Context context) {
		return getStringPreference(context, AppClient.SESSIONID);
	}
	
	
	public static void checkin(Context context, Location location) {
		setStringPreference(context, CHECKED_IN_LOCATION, location.toString());
	}
	
	public static void checkout(Context context) {
		removeStringPreference(context, CHECKED_IN_LOCATION);
	}
	
	public static Location getCheckedInLocation(Context context) {
		String jsonString = getStringPreference(context, CHECKED_IN_LOCATION);
		if (jsonString != null) {
			try {
				return new Location(jsonString);
			} catch (JSONException e) {
				e.printStackTrace();
				removeStringPreference(context, CHECKED_IN_LOCATION);
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
