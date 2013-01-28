package com.envsocial.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.envsocial.android.features.Feature;

public class DetailsGridAdapter extends BaseAdapter {
	
	private static final String TAG = "DetailsGridAdapter";
	private HashMap<String, Integer> mThumbnails;
	Map<String, Feature> mFeatures;
	private ArrayList<Integer> mThumbIds;
	private ArrayList<String> mNames;
	private Context mContext;
	
	public DetailsGridAdapter(Context c, Map<String, Feature> features) {
		this.mContext = c;
		this.mFeatures = features;
		this.mThumbIds = new ArrayList<Integer>();
		this.mNames = new ArrayList<String>();
		for (String featureName : features.keySet()) {
			Feature currentFeature = mFeatures.get(featureName);
			
			mThumbIds.add(currentFeature.getDisplayThumbnail());		
			mNames.add(currentFeature.getDisplayName());
		}
	}

	@Override
	public int getCount() {
		return mThumbIds.size();
	}

	@Override
	public Object getItem(int position) {
		return mThumbIds.get(position);
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
