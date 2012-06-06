package com.envsocial.android.features.program;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;

public class ProgramFragment extends Fragment {
	
	private Location mLocation;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		// Inflate layout for this fragment.
		View v = inflater.inflate(R.layout.program, container, false);
	
		try {
			mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
			String programJSON = mLocation.getFeatureData(Feature.PROGRAM);
			
			// Parse program's JSON
			JSONObject program = (JSONObject) new JSONObject(programJSON).getJSONObject("program");
			JSONArray sessionsArray = (JSONArray) program.getJSONArray("sessions");
			JSONArray entriesArray = (JSONArray) program.getJSONArray("entries");
			
			// Inflate the in-memory database
			ProgramDbHelper programDb = new ProgramDbHelper(getActivity());
			programDb.insertSessions(sessionsArray);
			programDb.insertEntries(entriesArray);
			
			// Create adapter
			ProgramListAdapter adapter = new ProgramListAdapter(getActivity(), programDb);
			
		    // Set adapter
		    ListView listView = (ListView) v.findViewById(R.id.program);
		    listView.setAdapter(adapter);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return v;
	}
}
