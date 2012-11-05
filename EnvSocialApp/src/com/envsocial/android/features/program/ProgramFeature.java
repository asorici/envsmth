package com.envsocial.android.features.program;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.envsocial.android.Envived;
import com.envsocial.android.api.AppClient;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.Url;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.ResponseHolder;

public class ProgramFeature extends Feature {
	private static final long serialVersionUID = 1L;

	private static final String TAG = "ProgramFeature";
	
	public static final String ENTRY_QUERY_TYPE = "entry";
	public static final String ENTRY = "program_entry";
	
	private ProgramDbHelper dbHelper;
	
	public ProgramFeature(String category, int version, String resourceUri, 
			String environmentUri, String areaUri, String data) throws EnvSocialContentException {
		super(category, version, resourceUri, environmentUri, areaUri, data);
	
		
	}
	
	@Override
	public void init() throws EnvSocialContentException {
		super.init();
		
		if (dbHelper == null) {
			dbHelper = new ProgramDbHelper(Envived.getContext(), this, version);
		}
		
		if (dbHelper != null) {
			dbHelper.init();
		}
	}
	
	
	@Override
	public void doUpdate() throws EnvSocialContentException {
		super.doUpdate();
		
		if (dbHelper == null) {
			dbHelper = new ProgramDbHelper(Envived.getContext(), this, version);
		}
		
		dbHelper.update();
	}
	
	
	@Override
	public void doCleanup(Context context) {
		super.doCleanup(context);
		
		if (dbHelper != null) {
			dbHelper.close();
			dbHelper = null;
		}
	}
	
	@Override
	public void doClose(Context context) {
		super.doClose(context);
		
		// first do cleanup
		doCleanup(context);
				
		context.deleteDatabase(ProgramDbHelper.DATABASE_NAME);
	}
	
	
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
			locationId = Url.resourceIdFromUri(location.getParentUri());
		}
		
		Url url = new Url(Url.RESOURCE, Feature.TAG);
		url.setParameters(
			new String[] { type, "category", "querytype", "entry_id"}, 
			new String[] { locationId, Feature.PROGRAM, ProgramFeature.ENTRY_QUERY_TYPE, entryId}
		);
		
		//System.out.println("[DEBUG] >> API query for entry: " + url.toString());
		
		
		try {
			HttpResponse response = client.makeGetRequest(url.toString());
			ResponseHolder holder = ResponseHolder.parseResponse(response);
			
			if (!holder.hasError() && holder.getCode() == HttpStatus.SC_OK) {
				// if all is Ok the response will be a list of program features with a single entry
				JSONObject featuresJSON = holder.getJsonContent();
				JSONArray featureList = featuresJSON.optJSONArray("objects");
				
				if (featureList != null) {
					JSONObject featureData = featureList.getJSONObject(0);
					JSONObject entryData = featureData.optJSONObject("data");
					
					if (entryData != null) {
						entry.put(ProgramDbHelper.COL_ENTRY_TITLE, 
								entryData.optString(ProgramDbHelper.COL_ENTRY_TITLE, "Title not available."));
						entry.put(ProgramDbHelper.COL_ENTRY_SESSIONID, 
								entryData.optString(ProgramDbHelper.COL_ENTRY_SESSIONID, "Session name not available."));
						entry.put(ProgramDbHelper.COL_ENTRY_START_TIME, 
								entryData.optString(ProgramDbHelper.COL_ENTRY_START_TIME, "Start time not available."));
						entry.put(ProgramDbHelper.COL_ENTRY_SPEAKERS, 
								entryData.optString(ProgramDbHelper.COL_ENTRY_SPEAKERS, "Speaker data not available."));
						entry.put(ProgramDbHelper.COL_ENTRY_ABSTRACT, 
								entryData.optString(ProgramDbHelper.COL_ENTRY_ABSTRACT, "No abstract available."));
					
						return entry;
					}
				}
			}
		} catch (IOException e) {
			Log.d(TAG, e.toString());
		} catch (JSONException e) {
			Log.d(TAG, e.toString());
		}
		
		return null;
	}


	@Override
	public boolean hasLocalDatabaseSupport() {
		return true;
	}

	@Override
	public boolean hasLocalQuerySupport() {
		return true;
	}

	@Override
	public SQLiteOpenHelper getLocalDatabaseSupport() {
		return dbHelper;
	}

	@Override
	public Cursor localSearchQuery(String query) {
		if (dbHelper != null) {
			return dbHelper.searchQuery(query);
		}
		
		return null;
	}
}
