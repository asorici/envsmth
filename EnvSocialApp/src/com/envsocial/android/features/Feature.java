package com.envsocial.android.features;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.envsocial.android.Envived;
import com.envsocial.android.EnvivedFeatureDataRetrievalService;
import com.envsocial.android.api.AppClient;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.Url;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.description.DescriptionFeature;
import com.envsocial.android.features.order.OrderDbHelper;
import com.envsocial.android.features.order.OrderFeature;
import com.envsocial.android.features.people.PeopleFeature;
import com.envsocial.android.features.program.ProgramFeature;
import com.envsocial.android.utils.EnvivedNotificationContents;
import com.envsocial.android.utils.FeatureDbHelper;
import com.envsocial.android.utils.FeatureLRUEntry;
import com.envsocial.android.utils.FeatureLRUTracker;
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.ResponseHolder;
import com.envsocial.android.utils.Utils;

public abstract class Feature implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	
	public static final String ENVIRONMENT_TAG = "_environment_";
	public static final String AREA_TAG = "_area_";
	
	public static final String DESCRIPTION 	= 	"description";
	public static final String ORDER 		= 	"order";
	public static final String PEOPLE 		= 	"people";
	public static final String PROGRAM 		= 	"program";
	
	public static final String TAG 				= 	"Feature";
	public static final String SEARCH_FEATURE 	= 	"Search_Feature";
	
	public static final String RETRIEVE_CONTENT_NOTIFICATION = "retrieve_content";
	public static final String RETRIEVE_STRUCTURE_NOTIFICATION = "retrieve_structure";
	
	protected String category;
	protected int version;
	protected Calendar timestamp;
	protected String resourceUrl;
	protected String environmentUrl;
	protected String areaUrl;
	protected boolean virtualAccess;
	
	/**
	 * This field will only be populated after a Data Retrieval request is made to the server. The retrieved data is then either
	 * cached locally in a SQLite database, or stored in the specific fields of classes that extend this base class.
	 */
	protected String retrievedData;
	
	private boolean initialized = false;
	
	protected Feature(String category, int version, Calendar timestamp, String resourceUrl, 
			String environmentUrl, String areaUrl, String data, boolean virtualAccess) {
		this.category = category;
		this.version = version;
		this.timestamp = timestamp;
		this.resourceUrl = resourceUrl;
		this.environmentUrl = environmentUrl;
		this.areaUrl = areaUrl;
		this.retrievedData = data;
		this.virtualAccess = virtualAccess;
	}
	
	public void init() throws EnvSocialContentException {
		if (hasData()) {
			featureInit();
			
			Log.i(TAG, "USING CACHED DATA");
			
			// if feature data has been parsed correctly mark it as an entry in the feature lru tracker
			String featureCacheFileName = getLocalCacheFileName(category, environmentUrl, areaUrl, version);
			String locationUrl = environmentUrl != null ? environmentUrl : areaUrl;
			FeatureLRUEntry featureLruEntry = 
					new FeatureLRUEntry(category, featureCacheFileName, locationUrl, virtualAccess, timestamp);
			
			FeatureLRUTracker featureLruTracker = Envived.getFeatureLRUTracker();
			featureLruTracker.put(featureCacheFileName, featureLruEntry);
			
			initialized = true;
		}
		else {
			Log.i(TAG, "RETRIEVING DATA ANEW.");
			
			// start data retrieval service
			Context context = Envived.getContext();
			String locationUri = (environmentUrl != null) ? environmentUrl : areaUrl;
			JSONObject paramsJSON = new JSONObject();
			
			try {
				paramsJSON.put("type", Feature.RETRIEVE_CONTENT_NOTIFICATION);
			} catch (JSONException e) {
				Log.d(TAG, "ERROR constructing paramsJSON object for feature data retrieval request", e);
				
				// should never actually happen, but if it does, abort the process
				return;
			}
			
			EnvivedNotificationContents notificationContents = 
				new EnvivedNotificationContents(locationUri, category, resourceUrl, paramsJSON.toString());
			
			Intent updateService = new Intent(context, EnvivedFeatureDataRetrievalService.class);
			updateService.putExtra(EnvivedFeatureDataRetrievalService.DATA_RETRIEVE_SERVICE_INPUT, 
					notificationContents);
			
			context.startService(updateService);
		}
	}
	
	
	public void doUpdate() throws EnvSocialContentException {
		if (hasData()) {
			featureUpdate();
		}
		else {
			// TODO start data retrieval process
		}
		initialized = true;
	}
	
	
	public void doCleanup(Context context) {
		featureCleanup(context);
		initialized = false;
	}


	public void doClose(Context context) {
		featureClose(context);
		
		// check to see if our feature data is still in the feature lru tracker
		FeatureLRUTracker featureLruTracker = Envived.getFeatureLRUTracker();
		String localCacheFileName = getLocalCacheFileName(category, 
				environmentUrl, areaUrl, version);
		
		if (featureLruTracker.get(localCacheFileName) == null) {
			Log.i(TAG, "CACHING FEATURE DATA: NO DB OR PREFERENCE DELETE");
			
			// if it is no longer in the cache then remove the database file entirely
			if (hasLocalDatabaseSupport()) {
				context.deleteDatabase(localCacheFileName);
			}
			else {
				Preferences.removeSerializedFeatureData(context, localCacheFileName);
			}
		}
		
		initialized = false;
	}
	
	
	public boolean hasVirtualAccess() {
		return virtualAccess;
	}
	
	public String getCategory() {
		return category;
	}
	
	public String getResourceUri() {
		return resourceUrl;
	}

	public String getEnvironmentUri() {
		return environmentUrl;
	}

	public String getAreaUri() {
		return areaUrl;
	}

	public String getSerializedData() {
		return retrievedData;
	}
	
	/**
	 * Specifies if this feature has a local cache of it's relevant data.
	 * If the data is not present, it has to be retrieved from the server.
	 * @return
	 */
	public boolean hasData() {
		if (retrievedData != null) {
			return true;
		}
		else {
			// search for an existing local cache (database) of this feature's data.
			String localCacheFileName = getLocalCacheFileName(category, environmentUrl, areaUrl, version);
			if (hasLocalDatabaseSupport()) {
				return databaseExists(Envived.getContext(), localCacheFileName);
			}
			else {
				return Preferences.featureDataCacheExists(Envived.getContext(), localCacheFileName);
			}
		}
	}
	
	private static boolean databaseExists(Context context, String databaseName) {
	    File dbFile = context.getDatabasePath(databaseName);
	    return dbFile.exists();
	}
	
	/**
	 * Specifies if the cached data for this feature has been initialized, i.e. if the local database
	 * is open and all feature relevant fields have been given their value 
	 * @return true if the cached data has been initialized, false otherwise
	 */
	public boolean isInitialized() {
		return initialized;
	}
	
	@Override
	public String toString() {
		String info = "Feature (" + category + ", " + resourceUrl + ")\n";
		info += "Feature Data: " + retrievedData;
		info += "\n";
		return info;
	}
	
	public static Feature getFromServer(Context context, Location location, String category, 
			String featureResourceUrl) {
		AppClient client = new AppClient(context);
		String locationType = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		
		Url url = new Url(Url.RESOURCE, "feature");
		url.setItemId(Url.resourceIdFromUrl(featureResourceUrl));
		
		url.setParameters(
			new String[] { locationType, "virtual", "category" }, 
			new String[] { "" + location.getId(), Boolean.toString(location.hasVirtualAccess()), category }
		);
		
		
		try {
			HttpResponse response = client.makeGetRequest(url.toString());
			ResponseHolder holder = ResponseHolder.parseResponse(response);
			
			//Log.i(TAG, "Feature RETRIEVE FROM SERVER response body:" + holder.getResponseBody());
			
			if (!holder.hasError() && holder.getCode() == HttpStatus.SC_OK) {
				// if all is Ok the response will be an object with the feature attributes including
				// its serialized 'data' attribute
				JSONObject featureObject = holder.getJsonContent();
				
				if (featureObject != null) {
					String remoteCategory = featureObject.optString("category", category);
					int remoteVersion = featureObject.optInt("version", 1);
					String remoteTimestampString = featureObject.optString("timestamp", null);
					Calendar remoteTimestamp = null;
					if (remoteTimestampString != null) {
						remoteTimestamp = Utils.stringToCalendar(remoteTimestampString, TIMESTAMP_FORMAT);
					}
					
					String environmentUri = featureObject.optString("environment", null);
					String areaUri = featureObject.optString("area", null);
					String resourceUri = featureObject.optString("resource_uri", null);
					
					String data = null;
					JSONObject featureSerializedDataObject = featureObject.optJSONObject("data");
					if (featureSerializedDataObject != null) {
						data = featureSerializedDataObject.toString();
					}
					
					return getInstance(remoteCategory, remoteVersion, remoteTimestamp, resourceUri, 
							environmentUri, areaUri, data, location.hasVirtualAccess());
				}
			}
			else {
				Log.d(TAG, holder.getResponseBody(), holder.getError());
			}
		} catch (IOException e) {
			Log.d(TAG, e.toString());
		} catch (Exception e) {
			Log.d(TAG, e.toString());
		}
		
		return null;
	}
	
	public static Feature getInstance(String category, int version, Calendar timestamp, String resourceUri, 
			String environmentUri, String areaUri, String data, boolean virtualAccess) 
					throws IllegalArgumentException, EnvSocialContentException {
		
		if (category == null) {
			throw new IllegalArgumentException("No feature category specified.");
		}
		
		if (category.equals(PROGRAM)) {
			return new ProgramFeature(category, version, timestamp, resourceUri, environmentUri, areaUri, data, virtualAccess); 
		}
		else if (category.equals(DESCRIPTION)) {
			return new DescriptionFeature(category, version, timestamp, resourceUri, environmentUri, areaUri, data, virtualAccess);
		}
		else if (category.equals(ORDER)) {
			return new OrderFeature(category, version, timestamp, resourceUri, environmentUri, areaUri, data, virtualAccess);
		}
		else if (category.equals(PEOPLE)) {
			return new PeopleFeature(category, version, timestamp, resourceUri, environmentUri, areaUri, data, virtualAccess);
		}
		else {
			throw new IllegalArgumentException("No feature matching category (" + category + ").");
		}
	}
	
	
	public static Feature getFromSavedLocation(Location location, String category) {
		return location.getFeature(category);
	}
	
	
	public static String getLocalCacheFileName(String prefix, String environmentUrl, String areaUrl, int version) {
		
		if (environmentUrl != null) {
			return prefix + ENVIRONMENT_TAG + Url.resourceIdFromUrl(environmentUrl) + "_" + version;
		}
		else {
			return prefix + AREA_TAG + Url.resourceIdFromUrl(areaUrl) + "_" + version;
		}
	}
	
	
	protected abstract void featureInit() throws EnvSocialContentException;
	
	protected abstract void featureUpdate() throws EnvSocialContentException;
	
	protected abstract void featureCleanup(Context context);
	
	protected abstract void featureClose(Context context);
	
	public abstract boolean hasLocalQuerySupport();
	
	public abstract FeatureDbHelper getLocalDatabaseSupport();
	
	
	public boolean hasLocalDatabaseSupport() {
		return getLocalDatabaseSupport() != null;
	}
	
	public Cursor localSearchQuery(String query) {
		return null;
	}

}
