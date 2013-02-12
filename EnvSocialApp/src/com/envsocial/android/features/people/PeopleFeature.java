package com.envsocial.android.features.people;

import java.util.Calendar;

import android.content.Context;

import com.envsocial.android.R;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.FeatureDbHelper;

public class PeopleFeature extends Feature {
	private static final long serialVersionUID = 1L;

	public PeopleFeature(String category, int version, Calendar timestamp, String resourceUri,
			String environmentUri, String areaUri, String data, boolean virtualAccess) {
		
		super(category, version, timestamp, resourceUri, environmentUri, areaUri, data, virtualAccess);
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
	public FeatureDbHelper getLocalDatabaseSupport() {
		return null;
	}


	@Override
	protected void featureInit(boolean insert) throws EnvSocialContentException {}


	@Override
	protected void featureUpdate() throws EnvSocialContentException {}


	@Override
	protected void featureCleanup(Context context) {}


	@Override
	protected void featureClose(Context context) {}


	@Override
	public int getDisplayThumbnail() {
		return R.drawable.ic_envived_white; // no people thumbnail yet
	}


	@Override
	public String getDisplayName() {
		return "People";
	}

}
