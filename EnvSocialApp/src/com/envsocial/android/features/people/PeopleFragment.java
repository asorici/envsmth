package com.envsocial.android.features.people;

import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.User;

public class PeopleFragment extends SherlockFragment {
	private Location mLocation;
	private List<User> mPeople;
	private int anonymousUserCount;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		// Inflate layout for this fragment.
		View v = inflater.inflate(R.layout.people, container, false);
		
		try {
			mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
			mPeople = getPeople(getActivity(), mLocation);
			anonymousUserCount = removeAnonymousUsers(mPeople);
			
			TextView anonymousUserCountView = (TextView) v.findViewById(R.id.people_anonymous_user_count);
			anonymousUserCountView.setText("There are " + anonymousUserCount + " anonymous users checked into " + mLocation.getName());
			
			// Create custom list adapter
			PeopleListAdapter adapter = new PeopleListAdapter(getActivity(), mPeople);
			
		    // Set adapter
		    ListView listView = (ListView) v.findViewById(R.id.people);
		    listView.setAdapter(adapter);
		    
		    listView.setTextFilterEnabled(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return v;
	}
	
	private int removeAnonymousUsers(List<User> checkedInPeople) {
		int anonymousUserCount = 0;
		
		// see how many of these users are anonymous - perform check by string comparing the name
		// with the default "Anonymous Guest" naming
		Iterator<User> it = checkedInPeople.iterator();
		while(it.hasNext()) {
			User u = it.next();
			if (u.getUserData().getFirstName().equalsIgnoreCase("anonymous")) {
				it.remove();
				anonymousUserCount++;
			}
		}
		
		return anonymousUserCount;
	}

	private List<User> getPeople(Context context, Location location) {
		List<User> checkedInPeople = null;
		
		// check if we already have a saved mUserData in the preferences for the current location
	    //checkedInPeople = Preferences.getPeopleInLocation(getActivity(), mLocation);
	    //if (checkedInPeople == null) {
	    	// try and retrieve from server
	    	
	    	try {
				checkedInPeople = User.getUsers(context, location, null);
				//String usersJsonString = User.toJSON(checkedInPeople);
		    	
		    	//if (usersJsonString != null) {
		    		//Preferences.setPeopleInLocation(context, usersJsonString);
		    	//}
	    	} catch (Exception e) {
				e.printStackTrace();
			}
	    //}
	    
	    return checkedInPeople;
	}
}
