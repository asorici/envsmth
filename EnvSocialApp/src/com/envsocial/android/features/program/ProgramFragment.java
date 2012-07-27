package com.envsocial.android.features.program;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;

public class ProgramFragment extends SherlockFragment implements OnClickListener {
	private static final String TAG = "ProgramFragment";
	
	private Location mLocation;
	private ProgramListAdapter mAdapter;
	private LinearLayout mDayScroll;
	
	private int currentDayIndex = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    //super.onCreate(savedInstanceState);
	    //mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		Log.i(TAG, "[INFO] onCreateView called.");
		
		// Inflate layout for this fragment.
		View view = inflater.inflate(R.layout.program, container, false);
		
		try {
			String programJSON = mLocation.getFeatureData(Feature.PROGRAM).getSerializedData();
			
			// Parse program's JSON
			JSONObject program = (JSONObject) new JSONObject(programJSON).getJSONObject("program");
			JSONArray sessionsArray = (JSONArray) program.getJSONArray("sessions");
			JSONArray entriesArray = (JSONArray) program.getJSONArray("entries");
			
			// Inflate the in-memory database
			ProgramDbHelper programDb = new ProgramDbHelper(getActivity());
			programDb.insertSessions(sessionsArray);
			programDb.insertEntries(entriesArray);
			
			mDayScroll = (LinearLayout) view.findViewById(R.id.dayScroll);
			List<String> days = programDb.getDays();
			int k = 0;
			for (String d : days) {
				TextView dayView = new TextView(getActivity());
				
				try {
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
					Date date = formatter.parse(d);
					formatter = new SimpleDateFormat("EEE, MMM d");
					dayView.setText(formatter.format(date));
				} catch (ParseException e) {
					e.printStackTrace();
					dayView.setText(d);
				}
				
				dayView.setLayoutParams(
						new LayoutParams(
								ViewGroup.LayoutParams.WRAP_CONTENT,
								ViewGroup.LayoutParams.WRAP_CONTENT
						)
				);
				dayView.setTag(k ++);
				dayView.setOnClickListener(this);
				dayView.setPadding(15, 15, 15, 15);
				dayView.setTextColor(getResources().getColor(R.color.white));
				dayView.setBackgroundColor(getResources().getColor(R.color.dark_green));
				mDayScroll.addView(dayView);
			}
			
			mDayScroll.getChildAt(currentDayIndex).setBackgroundColor(getResources().getColor(R.color.light_green));
			
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
		if (v instanceof TextView) {
			mDayScroll.getChildAt(currentDayIndex).setBackgroundColor(getResources().getColor(R.color.dark_green));
			v.setBackgroundColor(getResources().getColor(R.color.light_green));
			currentDayIndex = (Integer) v.getTag();
			mAdapter.setDay(currentDayIndex);
		}
	}
}
