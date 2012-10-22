package com.envsocial.android.features;

import java.io.IOException;
import java.io.Serializable;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.envsocial.android.api.AppClient;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.Url;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.description.DescriptionFeature;
import com.envsocial.android.features.order.OrderFeature;
import com.envsocial.android.features.people.PeopleFeature;
import com.envsocial.android.features.program.ProgramFeature;
import com.envsocial.android.utils.ResponseHolder;

public abstract class Feature implements Serializable {
	private static final long serialVersionUID = -4979711312694147738L;
	
	public static final String DESCRIPTION 	= 	"description";
	public static final String ORDER 		= 	"order";
	public static final String PEOPLE 		= 	"people";
	public static final String PROGRAM 		= 	"program";
	
	public static final String TAG 				= 	"feature";
	public static final String SEARCH_FEATURE 	= 	"search_feature";
	
	protected String category;
	protected int version;
	protected String resourceUri;
	protected String environmentUri;
	protected String areaUri;
	protected String data;
	
	protected Feature(String category, int version, String resourceUri, 
			String environmentUri, String areaUri, String data) {
		this.category = category;
		this.version = version;
		this.resourceUri = resourceUri;
		this.environmentUri = environmentUri;
		this.areaUri = areaUri;
		this.data = data;
	}
	
	public void init() throws EnvSocialContentException {
	}
	
	
	public void doUpdate() throws EnvSocialContentException {
	}
	
	
	public void doCleanup(Context context) {
	}

	
	public void doClose(Context context) {
	}
	
	
	public String getCategory() {
		return category;
	}
	
	public String getResourceUri() {
		return resourceUri;
	}

	public String getEnvironmentUri() {
		return environmentUri;
	}

	public String getAreaUri() {
		return areaUri;
	}

	public String getSerializedData() {
		return data;
	}
	
	
	public static Feature getFromServer(Context context, Location location, String category) {
		AppClient client = new AppClient(context);
		String type = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		
		Url url = new Url(Url.RESOURCE, Feature.TAG);
		url.setParameters(
			new String[] { type, "category" }, 
			new String[] { "" + location.getId(), Feature.PROGRAM }
		);
		
		try {
			HttpResponse response = client.makeGetRequest(url.toString());
			ResponseHolder holder = ResponseHolder.parseResponse(response);
			
			if (!holder.hasError() && holder.getCode() == HttpStatus.SC_OK) {
				// if all is Ok the response will be a list of program features with a single entry
				JSONObject featuresJSON = holder.getJsonContent();
				JSONArray featureList = featuresJSON.optJSONArray("objects");
				
				if (featureList != null) {
					// there will be only one item in the list, so get it
					JSONObject featureData = featureList.getJSONObject(0);
					
					String remoteCategory = featureData.optString("category", category);
					int remoteVersion = featureData.optInt("version", 1);
					String environmentUri = featureData.optString("environment", null);
					String areaUri = featureData.optString("area", null);
					String resourceUri = featureData.optString("resource_uri", null);
					
					String data = null;
					JSONObject programDataJSON = featureData.optJSONObject("data");
					if (programDataJSON != null) {
						data = programDataJSON.toString();
					}
					
					return getInstance(remoteCategory, remoteVersion, resourceUri, environmentUri, areaUri, data);
				}
			}
		} catch (IOException e) {
			Log.d(TAG, e.toString());
		} catch (Exception e) {
			Log.d(TAG, e.toString());
		}
		
		return null;
	}
	
	public static Feature getInstance(String category, int version, String resourceUri, 
			String environmentUri, String areaUri, String data) 
					throws IllegalArgumentException, EnvSocialContentException {
		
		if (category == null) {
			throw new IllegalArgumentException("No feature category specified.");
		}
		
		if (category.equals(PROGRAM)) {
			return new ProgramFeature(category, version, resourceUri, environmentUri, areaUri, data); 
		}
		else if (category.equals(DESCRIPTION)) {
			return new DescriptionFeature(category, version, resourceUri, environmentUri, areaUri, data);
		}
		else if (category.equals(ORDER)) {
			return new OrderFeature(category, version, resourceUri, environmentUri, areaUri, data);
		}
		else if (category.equals(PEOPLE)) {
			return new PeopleFeature(category, version, resourceUri, environmentUri, areaUri, data);
		}
		else {
			throw new IllegalArgumentException("No feature matching category (" + category + ").");
		}
	}
	
	public static Feature getFromSavedLocation(Location location, String category) {
		return location.getFeature(category);
	}
	
	
	public abstract boolean hasLocalQuerySupport();
	
	public abstract SQLiteOpenHelper getLocalDatabaseSupport();
	
	
	public boolean hasLocalDatabaseSupport() {
		return getLocalDatabaseSupport() != null;
	}
	
	public Cursor localSearchQuery(String query) {
		return null;
	}

}
