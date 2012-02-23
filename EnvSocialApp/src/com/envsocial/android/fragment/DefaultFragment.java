package com.envsocial.android.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Location;

public class DefaultFragment extends Fragment {
	
	private Location mData;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mData = (Location) getArguments().get(ActionHandler.CHECKIN);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		// Inflate layout for this fragment.
		View v = inflater.inflate(R.layout.description, container, false);
		TextView t = (TextView) v.findViewById(R.id.details);
		t.setText(mData.getFeatureData(Location.FEATURE_DEFAULT));
		
	    return v;
	}
}
