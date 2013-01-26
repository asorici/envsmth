package com.envsocial.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.envsocial.android.features.Feature;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class DetailsGridAdapter extends BaseAdapter {
	
	private static final String TAG = "DetailsGridAdapter";
	private HashMap<String, Integer> mThumbnails;
	Map<String, Feature> mFeatures;
	private ArrayList<Integer> mThumbIds;
	private Context mContext;
	
	public DetailsGridAdapter(Context c, Map<String, Feature> features) {
		this.mContext = c;
		this.mFeatures = features;
		this.mThumbIds = new ArrayList<Integer>();
		for (String featureName : features.keySet()) {

			if (featureName.equals("description")) {
				mThumbIds.add(R.drawable.details_icon_description);
			} else if (featureName.equals("order")) {
				mThumbIds.add(R.drawable.details_icon_order);
			} else if (featureName.equals("program")) {
				mThumbIds.add(R.drawable.details_icon_schedule);
			}
			
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
		ImageView imageView = new ImageView(mContext);
		imageView.setImageResource(mThumbIds.get(position));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setLayoutParams(new GridView.LayoutParams(70, 70));
		return imageView;
	}

}
