package com.envsocial.android.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;

public class Location implements Serializable {
	
	private static final long serialVersionUID = -1700484963112613638L;
	
	public static final String ENVIRONMENT = "environment";
	public static final String AREA = "area";
	
	private static final int TYPE_ENVIRONMENT = 0;
	private static final int TYPE_AREA = 1;
	
	private String mJSONString;
	
	private String mId;
	private String mUri;
	private int mType;
	private String mName;
	private Map<String, Feature> mFeatures;
	private List<String> mTags;
	private String mParent;
	
	private String mOwnerFirst;
	private String mOwnerLast;
	private String mOwnerEmail;
	
	/** Environment specific fields */
	private String mLatitude;
	private String mLongitude;
	private String mLayoutUrl;
	
	/** Area specific fields */
	private String mAreaType;
	private int mLevel;
	
	
	private static Location mFullInstance = null;
	private static String prevLocationString = null;
	
	public Location(String name, String uri) {
		mName = name;
		mUri = uri;
	}
	
	// TODO
	/*
	public Location(String jsonString) throws JSONException {
		this(new JSONObject(jsonString));
	}
	*/
	
	//public Location(JSONObject data) throws JSONException {
	private Location (String jsonString) throws JSONException {
		//mJSONString = data.toString();
		mJSONString = jsonString;
		JSONObject data = new JSONObject(jsonString);

		// Get location type
		String type = data.getString("location_type");
		if (type.compareTo(ENVIRONMENT) == 0) {
			mType = TYPE_ENVIRONMENT;
		} else if (type.compareTo(AREA) == 0) {
			mType = TYPE_AREA;
		}
		
		// Get location data
		JSONObject locationData = data.getJSONObject("location_data");
		
		mId = locationData.getString("id");
		mUri = locationData.getString("resource_uri");
		mName = locationData.getString("name");
		
		JSONObject owner = locationData.getJSONObject("owner");
		mOwnerFirst = owner.getString("first_name");
		mOwnerLast = owner.getString("last_name");
		try {
			mOwnerEmail = owner.getString("email");
		} catch (JSONException e) {
			mOwnerEmail = null;
		}
		
		// Get features
		mFeatures = new HashMap<String, Feature>();
		JSONArray array = locationData.getJSONArray("features");
		int len = array.length();
		for (int i = 0; i < len; ++ i) {
			JSONObject item = array.getJSONObject(i);
			String category = item.getString("category");
			String environmentUri = item.optString("environment", null);
			String areaUri = item.optString("area", null);
			String resourceUri = item.optString("resource_uri", null);
			String featureData = item.optString("data", null);
			
			
			try {
				Feature feat = Feature.getInstance(category, resourceUri, environmentUri, areaUri, featureData);
				mFeatures.put(category, feat);
			} catch (IllegalArgumentException ex) {
				Log.d("Location", ex.getMessage());
			} catch (EnvSocialContentException ex) {
				Log.d("Location", "Failed to add feature of category :: " + category.toUpperCase() + ". Reason: " + ex.getMessage());
			} catch (SQLiteException ex) {
				Log.d("Location", "Failed to add feature of category ::" + category.toUpperCase() + ". Feature local DB error: " + ex.getMessage());
			}
		}
		
		// Get tags
		mTags = new ArrayList<String>();
		array = locationData.getJSONArray("tags");
		len = array.length();
		for (int i = 0; i < len; ++ i) {
			mTags.add(array.getString(i));
		}
		
		mParent = locationData.getString("parent");
		
		// Grab environment/area specific fields
		if (isEnvironment()) {
			mLatitude = locationData.getString("latitude");
			mLongitude = locationData.getString("longitude");
			try {
				mLayoutUrl = locationData.getString("layout_url");
			} catch (JSONException e) {
				// Do nothing, layout was not requested
			}
		} else if (isArea()) {
			mAreaType = locationData.getString("areaType");
			mLevel = locationData.getInt("level");
		}
	}
	
	
	public static Location fromSerialized(String jsonString) throws JSONException {
		if (mFullInstance == null) {
			prevLocationString = jsonString;
			mFullInstance = new Location(jsonString);
		}
		else {
			// compare the hashcodes, it is much faster than big string comparison
			if (prevLocationString.hashCode() != jsonString.hashCode()) {
				prevLocationString = jsonString;
				mFullInstance = new Location(jsonString);
			}
		}
		
		return mFullInstance;
	}
	
	public void initFeatures() throws EnvSocialContentException {
		for (String category : mFeatures.keySet()) {
			Feature feat = mFeatures.get(category);
			if (feat != null ) {
				feat.init();
			}
		}
	}
	
	public void doCleanup() {
		// close all local support databases for the current location
		for (String category : mFeatures.keySet()) {
			Feature feat = mFeatures.get(category);
			if (feat != null ) {
				feat.cleanup();
			}
		}
	}
	
	public String getId() {
		return mId;
	}
	
	public String getUri() {
		return mUri;
	}
	
	public int getType() {
		return mType;
	}
	
	public String getName() {
		return mName;
	}
	
	public String getOwnerName() {
		return mOwnerFirst + " " + mOwnerLast;
	}
	
	public String getOwnerEmail() {
		return mOwnerEmail;
	}
	
	public boolean isOwnerByEmail(String email) {
		if (mOwnerEmail == null) {
			return false;
		}
		return mOwnerEmail.compareToIgnoreCase(email) == 0;
	}
	
	public Feature getFeature(String category) {
		return mFeatures.get(category);
	}
	
	public boolean hasFeature(String category) {
		return mFeatures.get(category) != null;
	}
	
	public Map<String, Feature> getFeatures() {
		return mFeatures;
	}
	
	public List<String> getTags() {
		return mTags;
	}
	
	public String getParent() {
		return mParent;
	}
	
	public String getLayoutUrl() {
		return mLayoutUrl;
	}
	
	public boolean isEnvironment() {
		return mType == TYPE_ENVIRONMENT;
	}
	
	public boolean isArea() {
		return mType == TYPE_AREA;
	}
	
	
	/** Environment specific methods */
	
	public String getLatitude() {
		return mLatitude;
	}
	
	public String getLongitude() {
		return mLongitude;
	}
	
	
	/** Area specific methods */
	
	public String getAreaType() {
		return mAreaType;
	}
	
	public int getLevel() {
		return mLevel;
	}
	
	public String serialize() {
		return mJSONString;
	}
	
	@Override
	public String toString() {
		String info = "";
		info += "name::" + mName + ", ";
		info += "uri::" + mUri + ", ";
		info += "jsonString::" + mJSONString;
		
		return info;
	}
}
