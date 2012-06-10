package com.envsocial.android.api;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.envsocial.android.features.program.ProgramDbHelper;

public class ProgramEntry {
	
	static final String ENTRY = "program_entry";
	
	// TODO
	public static Map<String,String> getEntryById(Context context, String id) {
		Map<String,String> entry = new HashMap<String,String>();
		
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
		
		AppClient client = new AppClient(context);
		Url url = new Url(Url.RESOURCE, ENTRY);
		url.setItemId(id);
		System.out.println("[DEBUG] >> API query for entry: " + url.toString());
		
/*		try {
			client.makeGetRequest(url.toString());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		
		return entry;
	}
}
