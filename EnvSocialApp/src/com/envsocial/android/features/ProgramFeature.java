package com.envsocial.android.features;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;

import com.envsocial.android.api.AppClient;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.Url;
import com.envsocial.android.features.program.ProgramDbHelper;
import com.envsocial.android.utils.ResponseHolder;

public class ProgramFeature extends Feature {
	public static final String ENTRY_QUERY_TYPE = "entry";
	
	public ProgramFeature(String category) {
		super(category);
	}
	
	static final String ENTRY = "program_entry";
	
	/**
	 * Gets the details of a single entry in the program. <br/>
	 * Performs a query to the FeatureResource.
	 * @param context
	 * @param location
	 * @param entryId
	 * @return a dictionary of the program entriy's details
	 */
	public static Map<String,String> getProgramEntryById(Context context, Location location, String entryId) {
		Map<String,String> entry = new HashMap<String,String>();
		
		AppClient client = new AppClient(context);
		
		// force a query on the environment
		String type = Location.ENVIRONMENT;
		String locationId = location.getId();
		if ( location.isArea() ) {
			// get the locationId from the URI of the parent environment
			locationId = Url.resourceIdFromUri(location.getParent());
		}
		
		Url url = new Url(Url.RESOURCE, Feature.TAG);
		url.setParameters(
			new String[] { type, "category", "querytype", "entry_id"}, 
			new String[] { locationId, Feature.PROGRAM, ProgramFeature.ENTRY_QUERY_TYPE, entryId}
		);
		
		System.out.println("[DEBUG] >> API query for entry: " + url.toString());
		
		try {
			HttpResponse response = client.makeGetRequest(url.toString());
			ResponseHolder holder = new ResponseHolder(response);
			
			if (holder.getCode() == HttpStatus.SC_OK) {
				// if all is Ok the response will be a list of program features with a single entry
				JSONObject featuresJSON = holder.getData();
				JSONArray featureList = featuresJSON.optJSONArray("objects");
				
				if (featureList != null) {
					JSONObject featureData = featureList.getJSONObject(0);
					JSONObject entryData = featureData.optJSONObject("data");
					
					if (entryData != null) {
						entry.put(ProgramDbHelper.COL_ENTRY_TITLE, entryData.optString("title", "Title not available."));
						entry.put(ProgramDbHelper.COL_ENTRY_SESSIONID, entryData.optString("sessionTitle", "Session name not available."));
						entry.put(ProgramDbHelper.COL_ENTRY_START_TIME, entryData.optString("startTime", "Start time not available."));
						entry.put(ProgramDbHelper.COL_ENTRY_SPEAKERS, entryData.optString("speakers", "Speaker data not available."));
						entry.put(ProgramDbHelper.COL_ENTRY_ABSTRACT, entryData.optString("abstract", "No abstract available."));
					
						return entry;
					}
				}
				
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch blocks
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public Map<String, String> getFeatureData(Context context, Location location, String category) {
		AppClient client = new AppClient(context);
		String type = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		
		Url url = new Url(Url.RESOURCE, Feature.TAG);
		url.setParameters(
			new String[] { type, "category" }, 
			new String[] { "" + location.getId(), Feature.PROGRAM }
		);
		
		try {
			HttpResponse response = client.makeGetRequest(url.toString());
			ResponseHolder holder = new ResponseHolder(response);
			
			if (holder.getCode() == HttpStatus.SC_OK) {
				// if all is Ok the response will be a list of program features with a single entry
				JSONObject featuresJSON = holder.getData();
				JSONArray featureList = featuresJSON.optJSONArray("objects");
				
				if (featureList != null) {
					Map<String, String> featureDataMap = new HashMap<String, String>();
					
					// there will be only one item in the list, so get it
					JSONObject featureData = featureList.getJSONObject(0);
					featureDataMap.put("category", featureData.optString("category", this.category));
					featureDataMap.put("environment_uri", featureData.optString("environment", null));
					featureDataMap.put("area_uri", featureData.optString("area", null));
					featureDataMap.put("resource_uri", featureData.optString("resource_uri", null));
					
					JSONObject programDataJSON = featureData.optJSONObject("data");
					if (programDataJSON != null) {
						featureDataMap.put("data", programDataJSON.toString());
					}
					else {
						featureDataMap.put("data", null);
					}
					
					return featureDataMap;
				}
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch blocks
			e.printStackTrace();
		}
		
		return null;
	}
}
