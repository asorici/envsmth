package com.envsocial.android.features.description;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

import com.envsocial.android.features.Feature;

public class DescriptionFeature extends Feature {

	public DescriptionFeature(String category, String resourceUri,
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
	public SQLiteOpenHelper getLocalDatabaseSupport(Context context) {
		return null;
	}

}
