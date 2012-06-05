package com.envsocial.android.features.program;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.service.textservice.SpellCheckerService.Session;
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
	private Program mProgram;
	
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
			mProgram= new Program(getActivity(), programJSON);
			
			// TODO: Create custom list adapter
			
		    // Set adapter
		    ListView listView = (ListView) v.findViewById(R.id.program);
//		    listView.setAdapter(adapter);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return v;
	}
	
	
	public static class Program {
		
		Map<Integer, Session> sessions = new HashMap<Integer, Session>();
		Map<Integer, Session> entries = new HashMap<Integer, Session>();
		
		Program(Context context, String jsonString) throws JSONException {
			JSONObject program = (JSONObject) new JSONObject(jsonString).getJSONObject("program");
			JSONArray sessionsArray = (JSONArray) program.getJSONArray("sessions");
			JSONArray entriesArray = (JSONArray) program.getJSONArray("entries");

			System.out.println("[DEBUG] >>" + sessionsArray);
			System.out.println("[DEBUG] >>" + entriesArray);
			
			ProgramDbHelper programDb = new ProgramDbHelper(context);
			programDb.insertSessions(sessionsArray);
			programDb.insertEntries(entriesArray);
			
			List<String> days = programDb.getDays();
			List<Map<String,String>> sessions = programDb.getAllSessions();
//			System.out.println("[DEBUG] >> Sessions: " + sessions);
			
			programDb.close();
		}
	}
}
