package com.envsocial.android.features.people;

import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;

import com.envsocial.android.features.Feature;

public class PeopleFeature extends Feature {

	public PeopleFeature(String category, String resourceUri,
			String environmentUri, String areaUri, String data) {
		
		super(category, resourceUri, environmentUri, areaUri, data);
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
