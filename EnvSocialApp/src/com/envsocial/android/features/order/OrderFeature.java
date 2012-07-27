package com.envsocial.android.features.order;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

import com.envsocial.android.features.Feature;

public class OrderFeature extends Feature {

	public OrderFeature(String category, String resourceUri,
			String environmentUri, String areaUri, String data) {
		
		super(category, resourceUri, environmentUri, areaUri, data);
	}

	@Override
	public boolean hasLocalDatabaseSupport() {
		return true;
	}

	@Override
	public boolean hasLocalQuerySupport() {
		return true;
	}

	@Override
	public SQLiteOpenHelper getLocalDatabaseSupport(Context context) {
		// TODO Auto-generated method stub
		return null;
	}

}
