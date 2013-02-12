package com.envsocial.android.api;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.Utils;

public class Location implements Serializable {
	
	private static final long serialVersionUID = -1700484963112613638L;
	
	public static final String ENVIRONMENT = "environment";
	public static final String AREA = "area";
	
	private static final int TYPE_ENVIRONMENT = 0;
	private static final int TYPE_AREA = 1;
	
	private String mJSONString;
	private boolean mVirtualAccess = true;
	
	private String mId;
	private String mResourceUri;
	private int mType;
	private String mName;
	private Map<String, Feature> mFeatures;
	private List<AreaInfo> mAreaInfoList;
	private List<String> mTags;
	
	private String mParentUri;
	private String mParentName;
	
	private String mOwnerFirstName;
	private String mOwnerLastName;
	private String mOwnerEmail;
	private String mOwnerUri;
	
	private String mImageThumbnailUrl;
	
	/** Environment specific fields */
	private String mLatitude;
	private String mLongitude;
	private String mLayoutUrl;
	
	/** Area specific fields */
	private String mAreaType;
	private int mLevel;
	private String mAdminFirstName;
	private String mAdminLastName;
	private String mAdminEmail;
	private String mAdminUri;
	
	/** Location context specific fields */
	private LocationContextManager mContextManager;
	
	
	private static Location mFullInstance = null;
	private static String prevLocationString = null;
	
	
	public Location(String name, String uri) {
		mName = name;
		mResourceUri = uri;
	}
	
	
	private Location (String jsonString) throws JSONException {
		mJSONString = jsonString;
		JSONObject data = new JSONObject(jsonString);
		
		// Get location access flag: virtual or physical
		mVirtualAccess = data.optBoolean("virtual", true);
		
		// get location Area info list
		mAreaInfoList = new LinkedList<Location.AreaInfo>();
		JSONArray areaInfoArray = data.optJSONArray("area_list");
		if (areaInfoArray != null) {
			int len = areaInfoArray.length();
			for (int i = 0; i < len; i++) {
				JSONObject areaInfoObject = areaInfoArray.getJSONObject(i);
				AreaInfo info = AreaInfo.fromSerialized(areaInfoObject);

				mAreaInfoList.add(info);
			}
		}
		
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
		mResourceUri = locationData.getString("resource_uri");
		mName = locationData.getString("name");
		
		JSONObject owner = locationData.getJSONObject("owner");
		mOwnerUri = owner.getString("resource_uri");
		mOwnerFirstName = owner.getString("first_name");
		mOwnerLastName = owner.getString("last_name");
		mOwnerEmail = owner.optString("email", null);
		
		// Get features
		mFeatures = new HashMap<String, Feature>();
		JSONArray array = locationData.getJSONArray("features");
		int len = array.length();
		for (int i = 0; i < len; ++ i) {
			JSONObject item = array.getJSONObject(i);
			String category = item.getString("category");
			int version = item.optInt("version", 1);
			
			String timestampString = item.optString("timestamp", null);
			Calendar timestamp = null;
			if (timestampString != null) {
				try {
					timestamp = Utils.stringToCalendar(timestampString, Feature.TIMESTAMP_FORMAT);
				} catch (ParseException e) {
					Log.d("Location", "Feature timestamp parsing error.", e);
				}
			}
			
			String environmentUri = item.optString("environment", null);
			String areaUri = item.optString("area", null);
			String resourceUri = item.optString("resource_uri", null);
			String featureData = item.optString("data", null);
			
			try {
				Feature feat = Feature.getInstance(category, version, timestamp, resourceUri, environmentUri, areaUri, featureData, mVirtualAccess);
				mFeatures.put(category, feat);
			} catch (IllegalArgumentException ex) {
				Log.d("Location", ex.getMessage());
			} catch (EnvSocialContentException ex) {
				Log.d("Location", "Failed to add feature of category :: " + category.toUpperCase(Locale.US) + ". Reason: " + ex.getMessage());
			} catch (SQLiteException ex) {
				Log.d("Location", "Failed to add feature of category ::" + category.toUpperCase(Locale.US) + ". Feature local DB error: " + ex.getMessage());
			}
		}
		
		// Get tags
		mTags = new ArrayList<String>();
		array = locationData.getJSONArray("tags");
		len = array.length();
		for (int i = 0; i < len; ++ i) {
			mTags.add(array.getString(i));
		}
		
		// Get thumbnail image if available
		mImageThumbnailUrl = locationData.optString("img_thumbnail_url", null);
		
		// Get parent data
		JSONObject parentData = locationData.optJSONObject("parent");
		if (parentData != null) {
			mParentUri = parentData.optString("uri");
			mParentName = parentData.optString("name");
		}
		
		// Grab environment/area specific fields
		if (isEnvironment()) {
			mLatitude = locationData.getString("latitude");
			mLongitude = locationData.getString("longitude");
			mLayoutUrl = locationData.optString("layout_url", null);
		} 
		else if (isArea()) {
			mAreaType = locationData.getString("areaType");
			mLevel = locationData.getInt("level");
			
			JSONObject admin = locationData.optJSONObject("admin");
			if (admin != null) {
				mAdminUri = admin.getString("resource_uri");
				mAdminFirstName = admin.getString("first_name");
				mAdminLastName = admin.getString("last_name");
				mAdminEmail = admin.optString("email", null);
			}
		}
		
		// lastly initialize the context manager
		mContextManager = new LocationContextManager(this);
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
	
	
	public void doCleanup(Context context) {
		// call cleanup on all the features of this location doing things like
		// closing all handlers to local support databases
		for (String category : mFeatures.keySet()) {
			Feature feat = mFeatures.get(category);
			if (feat != null ) {
				feat.doCleanup(context);
			}
		}
	}
	
	
	public void doClose(Context context) {
		/* called only on checkout when we want to remove all data associated with this location
		 TODO:	in the future we may want to CACHE the data for FAVORITE locations and implement
		 		stronger consistency checks between server and client application
		*/
		
		for (String category : mFeatures.keySet()) {
			Feature feat = mFeatures.get(category);
			if (feat != null ) {
				feat.doClose(context);
			}
		}
	}
	
	public boolean hasVirtualAccess() {
		return mVirtualAccess;
	}
	
	public void setVirtualAccess(boolean access) {
		mVirtualAccess = access;
	}
	
	public String getId() {
		return mId;
	}
	
	public String getLocationUri() {
		return mResourceUri;
	}
	
	public int getType() {
		return mType;
	}
	
	public String getName() {
		return mName;
	}
	
	public String getOwnerName() {
		return mOwnerFirstName + " " + mOwnerLastName;
	}
	
	public String getOwnerEmail() {
		return mOwnerEmail;
	}
	
	public String getOwnerUri() {
		return mOwnerUri;
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
	
	public void setFeature(String category, Feature feature) {
		mFeatures.put(category, feature);
	}
	
	public boolean hasFeature(String category) {
		return mFeatures.get(category) != null;
	}
	
	public Map<String, Feature> getFeatures() {
		return mFeatures;
	}
	
	public List<AreaInfo> getAreaInfoList() {
		return mAreaInfoList;
	}
	
	public List<String> getTags() {
		return mTags;
	}
	
	public String getImageThumbnailUrl() {
		return mImageThumbnailUrl;
	}
	
	public String getParentUrl() {
		return mParentUri;
	}
	
	public String getParentName() {
		return mParentName;
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
	
	public String getAdminName() {
		return mAdminFirstName + " " + mAdminLastName;
	}
	
	public String getAdminEmail() {
		return mAdminEmail;
	}
	
	public String getAdminUri() {
		return mAdminUri;
	}
	
	public String getAreaType() {
		return mAreaType;
	}
	
	public int getLevel() {
		return mLevel;
	}
	
	public String serialize() {
		return mJSONString;
	}
	
	public JSONObject getAsJson() throws JSONException {
		return new JSONObject(mJSONString);
	}
	
	@Override
	public String toString() {
		String info = "";
		info += "name::" + mName + ", ";
		info += "uri::" + mResourceUri + ", ";
		info += "jsonString::" + mJSONString;
		
		return info;
	}
	
	/** Location context specific methods */
	public LocationContextManager getContextManager() {
		return mContextManager;
	}
	
	public static class AreaInfo implements Serializable {
		private String mResourceUrl;
		private String mName;
		
		private List<String> mTags;
		private int mPersonCount;
		private String mImageUrl;
		
		private AreaInfo(String resourceUrl, String name, List<String> tags, int personCount, String imageUrl) {
			this.mResourceUrl = resourceUrl;
			this.mName = name;
			this.mTags = tags;
			this.mPersonCount = personCount;
			this.mImageUrl = imageUrl;
		}

		
		public String getResourceUrl() {
			return mResourceUrl;
		}
		
		public String getName() {
			return mName;
		}
		
		public List<String> getTags() {
			return mTags;
		}
		
		public int getPersonCount() {
			return mPersonCount;
		}
		
		public String getImageUrl() {
			return mImageUrl;
		}
		
		public static AreaInfo fromSerialized(JSONObject areaInfo) throws JSONException {
			String resourceUrl = areaInfo.getString("resource_uri");
			String name = areaInfo.getString("name");
			JSONArray tags = areaInfo.optJSONArray("tags");
			List<String> tagList = null;
			if (tags != null) {
				tagList = new LinkedList<String>();
				int len = tags.length();
				
				for (int i = 0; i < len; i++) {
					tagList.add(tags.getString(i));
				}
			}
			
			int personCount = areaInfo.getInt("person_count");
			String imageUrl = areaInfo.optString("image_url", null);
			
			return new AreaInfo(resourceUrl, name, tagList, personCount, imageUrl);
		}
	}
}
