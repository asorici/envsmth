package com.envsocial.android.features.description;

import android.database.sqlite.SQLiteOpenHelper;

import com.envsocial.android.features.Feature;

public class DescriptionFeature extends Feature {
	private static final long serialVersionUID = 1L;

	public DescriptionFeature(String category, int version, String resourceUri,
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
