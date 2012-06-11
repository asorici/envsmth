package com.envsocial.android.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONObject;

import android.content.Context;

import com.envsocial.android.features.Feature;
import com.envsocial.android.features.program.ProgramDbHelper;
import com.envsocial.android.utils.ResponseHolder;

public class ProgramEntry {
	
	static final String ENTRY = "program_entry";
	
	// TODO
	public static Map<String,String> getEntryById(Context context, Location location, String entryId) {
		Map<String,String> entry = new HashMap<String,String>();
		
		/*
		entry.put(ProgramDbHelper.COL_ENTRY_TITLE, "Designing Privacy-Aware Social Networks: A Multi-Agent Approach");
		entry.put(ProgramDbHelper.COL_ENTRY_SESSIONID, "Session 1: Social Networks");
		entry.put(ProgramDbHelper.COL_ENTRY_START_TIME, "11:20 - 11:40");
		entry.put(ProgramDbHelper.COL_ENTRY_SPEAKERS, "Andrei Ciortea, Yann Krupa, Laurent Vercouter");
		entry.put(ProgramDbHelper.COL_ENTRY_ABSTRACT, "People do not generally mind that some personal information is shared, " +
				"but that it is shared in the wrong ways and with inappropriate others. We introduce a model " +
				"for the automatic detection of incompatible relationships among agents in a virtual community. " +
				"We define this model to the aim of adding to the PrivaCIAS framework a new layer that follows " +
				"the theory of contextual integrity in more depth. We describe in detail the implementation of a " +
				"proof of concept privacy-aware photo-sharing social network that we had developed for Android " +
				"smartphones. The social network was built on the extended PrivaCIAS framework, and thus illustrates " +
				"its applicability in helping users to preserve privacy.");
		*/
		
		AppClient client = new AppClient(context);
		String type = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		
		Url url = new Url(Url.RESOURCE, type);
		url.setItemId(location.getId());
		url.setParameters(new String[] { "entryfeaturequery", "entry_id" }, 
				new String[] { "true", entryId }
		);
		
		System.out.println("[DEBUG] >> API query for entry: " + url.toString());
		
		try {
			HttpResponse response = client.makeGetRequest(url.toString());
			ResponseHolder holder = new ResponseHolder(response);
			
			if (holder.getCode() == HttpStatus.SC_OK) {
				Location loc = new Location(holder.getData());
				String dataJSON = loc.getFeatureData(Feature.PROGRAM);
				JSONObject entryData = new JSONObject(dataJSON);
				
				entry.put(ProgramDbHelper.COL_ENTRY_TITLE, entryData.optString("title", "Title not available."));
				entry.put(ProgramDbHelper.COL_ENTRY_SESSIONID, entryData.optString("sessionTitle", "Session name not available."));
				entry.put(ProgramDbHelper.COL_ENTRY_START_TIME, entryData.optString("startTime", "Start time not available."));
				entry.put(ProgramDbHelper.COL_ENTRY_SPEAKERS, entryData.optString("speakers", "Speaker data not available."));
				entry.put(ProgramDbHelper.COL_ENTRY_ABSTRACT, entryData.optString("abstract", "No abstract available."));
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return entry;
	}
}
