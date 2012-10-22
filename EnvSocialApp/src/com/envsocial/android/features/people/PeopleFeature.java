package com.envsocial.android.features.people;

import android.database.sqlite.SQLiteOpenHelper;

import com.envsocial.android.features.Feature;

public class PeopleFeature extends Feature {
	private static final long serialVersionUID = 1L;

	public PeopleFeature(String category, int version, String resourceUri,
			String environmentUri, String areaUri, String data) {
		
		super(category, version, resourceUri, environmentUri, areaUri, data);
	}
	
	
	@Override
	public boolean hasLocalDatabaseSupport() {
		return false;
	}

	@Override
	public boolean hasLocalQuerySupport() {
		return false;
	}

	@Override
	public SQLiteOpenHelper getLocalDatabaseSupport() {
		return null;
	}

}
