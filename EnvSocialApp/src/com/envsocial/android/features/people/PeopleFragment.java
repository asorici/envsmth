package com.envsocial.android.features.people;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import com.envsocial.android.features.program.ProgramFragment.Program;

public class PeopleFragment extends Fragment {
	private Location mLocation;
	private UserData mUserData;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		// Inflate layout for this fragment.
		View v = inflater.inflate(R.layout.people, container, false);
		
		try {
			mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
			String peopleJSON = mLocation.getFeatureData(Feature.PEOPLE);
			mUserData = new UserData(peopleJSON);
			// TODO: Create custom list adapter
			
		    // Set adapter
		    ListView listView = (ListView) v.findViewById(R.id.people);
//		    listView.setAdapter(adapter);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return v;
	}
	
	public static class UserData {
		private List<Map<String, String>> userDetails = new ArrayList<Map<String,String>>();
		
		UserData(String jsonString) throws JSONException {
			JSONObject userDataObject = (JSONObject) new JSONObject(jsonString).getJSONObject("userdata");
			
		}
	}
}
