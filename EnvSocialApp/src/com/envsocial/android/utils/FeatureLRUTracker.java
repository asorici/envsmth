package com.envsocial.android.utils;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.support.v4.util.LruCache;

public class FeatureLRUTracker extends LruCache<String, FeatureLRUEntry> {
	
	private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final String MAX_ENTRIES_LABEL = "max_entries";
	private static final String FEAUTURE_LIST_LABEL = "entries";
	
	public static final String FEATURE_CATEGORY = "feature_category";
	public static final String FEATURE_DATABASE_NAME = "feature_db_name";
	public static final String FEATURE_TIMESTAMP = "feature_timestamp";
	public static final String FEATURE_LOCATION_URL = "feature_location_url";
	public static final String FEATURE_VIRTUAL_ACCESS = "feature_virtual_access";
	
	public static final int MAX_ENTRIES = 20;
	
	private int maxEntries;
	
	public FeatureLRUTracker() {
		super(MAX_ENTRIES);
		maxEntries = MAX_ENTRIES;
	}
	
	public FeatureLRUTracker(int maxEntries) {
		super(maxEntries);
		this.maxEntries = maxEntries;
	}
	
	/**
	 * Method to serialize the contents of this FeatureLRUTracker as a json string in the
	 * application's shared preferences.
	 * @return the contents of the FeatureLRUTracker as a JSON string
	 * @throws JSONException if an error occurs at JSON serialization 
	 */
	public String toJSON() throws JSONException {
		JSONObject serializedFeatureTracker = new JSONObject();
		
		JSONArray trackedFeatureList = new JSONArray();
		for (Map.Entry<String, FeatureLRUEntry> trackedFeatureEntry : snapshot().entrySet()) {
			JSONObject trackedFeatureObject = new JSONObject();
			trackedFeatureObject.put(FEATURE_CATEGORY, 
				trackedFeatureEntry.getValue().getFeatureCategory());
			trackedFeatureObject.put(FEATURE_DATABASE_NAME, 
				trackedFeatureEntry.getValue().getFeatureCacheFileName());
			trackedFeatureObject.put(FEATURE_LOCATION_URL, 
					trackedFeatureEntry.getValue().getFeatureLocationUrl());
			trackedFeatureObject.put(FEATURE_VIRTUAL_ACCESS, 
					trackedFeatureEntry.getValue().hasFeatureVirtualAccess());
			trackedFeatureObject.put(FEATURE_TIMESTAMP, 
				Utils.calendarToString(trackedFeatureEntry.getValue().getFeatureTimestamp(), 
						TIMESTAMP_FORMAT));
			
			trackedFeatureList.put(trackedFeatureObject);
		}
		
		serializedFeatureTracker.put(MAX_ENTRIES_LABEL, maxEntries);
		serializedFeatureTracker.put(FEAUTURE_LIST_LABEL, trackedFeatureList);
		
		return serializedFeatureTracker.toString();
	}
	
	
	/**
	 * Method to retrieve JSON-serialized contents of this FeatureLRUTracker from the
	 * application's shared preferences.
	 * @parameter jsonString the JSON-serialized string containing the entries 
	 * to be included in the FeatureLRUTracker 
	 * @return the obtained FeatureLRUTracker
	 * @throws JSONException if an error occurs when reading from the json string 
	 * @throws ParseException 
	 */
	public static FeatureLRUTracker fromJSON(String jsonString) throws JSONException, ParseException {
		JSONObject serializedFeatureTracker = new JSONObject(jsonString);
		
		int maxEntries = serializedFeatureTracker.getInt(MAX_ENTRIES_LABEL);
		FeatureLRUTracker featureLruTracker = new FeatureLRUTracker(maxEntries);
		
		JSONArray trackedFeatureList = serializedFeatureTracker.getJSONArray(FEAUTURE_LIST_LABEL);
		for (int i = 0; i < trackedFeatureList.length(); i++) {
			JSONObject trackedFeatureObject = trackedFeatureList.getJSONObject(i);
			String featureCategory = trackedFeatureObject.getString(FEATURE_CATEGORY);
			String featureDatabaseName = trackedFeatureObject.getString(FEATURE_DATABASE_NAME);
			String featureLocationUrl = trackedFeatureObject.getString(FEATURE_LOCATION_URL);
			boolean featureVirtualAccess = trackedFeatureObject.getBoolean(FEATURE_VIRTUAL_ACCESS);
			
			Calendar featureTimestamp = 
				Utils.stringToCalendar(trackedFeatureObject.getString(FEATURE_TIMESTAMP), TIMESTAMP_FORMAT);
			
			FeatureLRUEntry featureLRUEntry = new FeatureLRUEntry(featureCategory, featureDatabaseName, 
							featureLocationUrl, featureVirtualAccess, featureTimestamp);
			featureLruTracker.put(featureDatabaseName, featureLRUEntry);
		}
		
		return featureLruTracker;
	}
}
