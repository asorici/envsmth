package com.envsocial.android.utils;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.support.v4.util.LruCache;

import com.envsocial.android.api.Location;

public class LocationHistory extends LruCache<String, Location> {
	private static final String MAX_ENTRIES_LABEL = "max_entries";
	private static final String LOCATIONS_LIST_LABEL = "locations";
	public static final int MAX_ENTRIES = 10;
	
	private int maxEntries;
	
	public LocationHistory() {
		super(MAX_ENTRIES);
		maxEntries = MAX_ENTRIES;
	}
	
	public LocationHistory(int maxSize) {
		super(maxSize);
		maxEntries = maxSize;
	}
	
	/**
	 * Method to serialize the contents of this LRU Location History tracker as a json string in the
	 * application's shared preferences.
	 * @return the contents of the LocationHistory as a JSON string
	 * @throws JSONException if an error occurs at JSON serialization 
	 */
	public String toJSON() throws JSONException {
		JSONObject serializedLocationHistory = new JSONObject();
		
		JSONArray trackedLocationsList = new JSONArray();
		for (Map.Entry<String, Location> trackedLocation : snapshot().entrySet()) {
			Location location = trackedLocation.getValue();
			
			// always save locations with virtual access set to true
			location.setVirtualAccess(true);
			trackedLocationsList.put(location.getAsJson());
		}
		
		serializedLocationHistory.put(MAX_ENTRIES_LABEL, maxEntries);
		serializedLocationHistory.put(LOCATIONS_LIST_LABEL, trackedLocationsList);
		
		return serializedLocationHistory.toString();
	}
	
	/**
	 * Method to retrieve JSON-serialized contents of this LocationHistory tracker from the
	 * application's shared preferences.
	 * @parameter jsonString the JSON-serialized string containing the entries 
	 * to be included in the LocationHistory
	 * @return the obtained LocationHistory
	 * @throws JSONException if an error occurs when reading from the json string 
	 * @throws ParseException 
	 */
	public static LocationHistory fromJSON(String jsonString) throws JSONException, ParseException {
		JSONObject serializedLocationHistory = new JSONObject(jsonString);
		
		int maxEntries = serializedLocationHistory.getInt(MAX_ENTRIES_LABEL);
		LocationHistory locationHistory = new LocationHistory(maxEntries);
		
		JSONArray trackedLocationsList = serializedLocationHistory.getJSONArray(LOCATIONS_LIST_LABEL);
		
		int len = trackedLocationsList.length();
		for (int i = 0; i < len; i++) {
			JSONObject locationObject = trackedLocationsList.getJSONObject(i);
			Location location = Location.fromSerialized(locationObject.toString());
			
			String locationType = location.isEnvironment() ? Location.ENVIRONMENT : Location.AREA;
			String locationId = location.getId();
			String locationKey = locationType + "_" + locationId;
			
			locationHistory.put(locationKey, location);
		}
		
		return locationHistory;
	}
}
