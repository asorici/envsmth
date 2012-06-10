package com.envsocial.android.features.program;


import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;

public class ProgramFragment extends SherlockFragment implements OnClickListener {
	
	private Location mLocation;
	private ProgramListAdapter mAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		// Inflate layout for this fragment.
		View view = inflater.inflate(R.layout.program, container, false);
		
		try {
			String programJSON = mLocation.getFeatureData(Feature.PROGRAM);
			
			// Parse program's JSON
			JSONObject program = (JSONObject) new JSONObject(programJSON).getJSONObject("program");
			JSONArray sessionsArray = (JSONArray) program.getJSONArray("sessions");
			JSONArray entriesArray = (JSONArray) program.getJSONArray("entries");
			
			// Inflate the in-memory database
			ProgramDbHelper programDb = new ProgramDbHelper(getActivity());
			programDb.insertSessions(sessionsArray);
			programDb.insertEntries(entriesArray);
			
			LinearLayout dayScroll = (LinearLayout) view.findViewById(R.id.dayScroll);
			List<String> days = programDb.getDays();
			int k = 0;
			for (String d : days) {
				Button dayButton = new Button(getActivity());
				dayButton.setText(d);
				dayButton.setLayoutParams(
						new LayoutParams(
								ViewGroup.LayoutParams.WRAP_CONTENT,
								ViewGroup.LayoutParams.WRAP_CONTENT
						)
				);
				dayButton.setTag(k ++);
				dayButton.setOnClickListener(this);
				dayScroll.addView(dayButton);
			}
			
			ListView listView = (ListView) view.findViewById(R.id.program);
		    
		    // Create and set adapter
			mAdapter = new ProgramListAdapter(getActivity(), programDb);
		    listView.setAdapter(mAdapter);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return view;
	}

	@Override
	public void onClick(View v) {
		if (v instanceof Button) {
			mAdapter.setDay((Integer) v.getTag());
		}
	}
}
