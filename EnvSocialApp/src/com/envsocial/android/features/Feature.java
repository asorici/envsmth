package com.envsocial.android.features;

import java.util.Map;

import android.content.Context;

import com.envsocial.android.api.Location;

public abstract class Feature {
	public static final String DESCRIPTION = "description";
	public static final String ORDER= "order";
	public static final String PEOPLE = "people";
	public static final String PROGRAM = "program";
	
	public static final String TAG = "feature";
	
	protected String category;
	
	public Feature(String category) {
		this.category = category;
	}
	
	public String getCategory() {
		return category;
	}
	
	public abstract Map<String, String> getFeatureData(Context context, Location location, String category);
}
