package com.envsocial.android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;

public class DefaultFragment extends SherlockFragment {
	
	private Location mData;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		System.out.println("[DEBUG] >> Created DEAFULT fragment");
	    super.onCreate(savedInstanceState);
	    mData = (Location) getArguments().get(ActionHandler.CHECKIN);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		// Inflate layout for this fragment.
		System.out.println("[DEBUG] >> Inflating layout for DEAFULT fragment");
		View v = inflater.inflate(R.layout.description, container, false);
		TextView t = (TextView) v.findViewById(R.id.details);
		System.out.println("[DEBUG] >> Setting text for for DEAFULT fragment");
		t.setText(mData.getFeatureData(Feature.DEFAULT));
		
	    return v;
	}
}
