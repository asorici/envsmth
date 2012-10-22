package com.envsocial.android.features.description;

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

public class DescriptionFragment extends SherlockFragment {
	private static final String TAG = "DescriptionFragment";
	private Location mData;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mData = (Location) getArguments().get(ActionHandler.CHECKIN);
	}
	
	/*
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "[INFO] ------------- onActivityCreated called in Default Fragment -----------------");
		super.onActivityCreated(savedInstanceState);
	}
	*/
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		// Inflate layout for this fragment.
		
		View v = inflater.inflate(R.layout.description, container, false);
		TextView t = (TextView) v.findViewById(R.id.details);
		t.setText(mData.getFeature(Feature.DESCRIPTION).getSerializedData());
		
	    return v;
	}
}
