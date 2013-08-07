package com.envsocial.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;

public class DetailsGridAdapter extends BaseAdapter {
	private static final String TAG = "DetailsGridAdapter";
	
	private Map<String, Feature> mFeatures;
	private Location mLocation;
	private Context mContext;
	
	private HashMap<String, Integer> mThumbnails;
	private ArrayList<Integer> mThumbIds;
	private ArrayList<String> mNames;
	private ArrayList<String> mFeatureCategories;
	
	public DetailsGridAdapter(Context context, Location location) {
		this.mContext = context;
		this.mLocation = location;
		this.mFeatures = mLocation.getFeatures();
		
		this.mThumbIds = new ArrayList<Integer>();
		this.mNames = new ArrayList<String>();
		this.mFeatureCategories = new ArrayList<String>();
		
		// first add the area browser if location is an environment
		if (location.isEnvironment()) {
			mFeatureCategories.add(Location.AREA);
			mThumbIds.add(R.drawable.details_icon_areas_white);
			mNames.add("Browse locations");
		}
		
		
		// then add the 
		for (String featureName : mFeatures.keySet()) {
			Feature currentFeature = mFeatures.get(featureName);
			
			mFeatureCategories.add(currentFeature.getCategory());
			mThumbIds.add(currentFeature.getDisplayThumbnail());		
			mNames.add(currentFeature.getDisplayName());
		}
		
		//Log.d(TAG, "environment feature categories: " + mFeatureCategories);
	}

	@Override
	public int getCount() {
		return mThumbIds.size();
	}

	@Override
	public Object getItem(int position) {
		return mFeatureCategories.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return 0;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View row=inflater.inflate(R.layout.details_grid_cell, parent, false);
		
		ImageView imageView = (ImageView)row.findViewById(R.id.details_grid_cell_image);
		imageView.setImageResource(mThumbIds.get(position));
        
        TextView textView = (TextView)row.findViewById(R.id.details_grid_cell_text);
        textView.setText(mNames.get(position));
        
        return row;
	}

}
