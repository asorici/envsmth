package com.envsocial.android.features.description;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.sqlite.SQLiteOpenHelper;

import com.envsocial.android.api.EnvSocialResource;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;

public class DescriptionFeature extends Feature {
	private static final long serialVersionUID = 1L;

	private static final String DESCRIPTION_TEXT = "description";
	private static final String NEWEST_INFO_TEXT = "newest_info";
	private static final String IMG_URL = "img_url";

	private static final String NO_DESCRIPTION = "No description available";
	private static final String NO_NEWEST_INFO = "No new information available at the moment";
	private static final String NO_PEOPLE_COUNT = "Indeterminate number of checked in users.";

	private String mDescriptionText = NO_DESCRIPTION;
	private String mNewestInfoText = NO_NEWEST_INFO;
	private String peopleCountText = NO_PEOPLE_COUNT;
	private String mLogoImageUri = null;

	public DescriptionFeature(String category, int version, String resourceUri,
			String environmentUri, String areaUri, String data) {

		super(category, version, resourceUri, environmentUri, areaUri, data);
	}

	@Override
	public void init() throws EnvSocialContentException {
		super.init();
		
		try {
			JSONObject descriptionData = new JSONObject(data);
			mDescriptionText = descriptionData.optString(DESCRIPTION_TEXT, NO_DESCRIPTION);
			mNewestInfoText = descriptionData.optString(NEWEST_INFO_TEXT, NO_NEWEST_INFO);
			mLogoImageUri = descriptionData.optString(IMG_URL, null);
		} catch (JSONException e) {
			throw new EnvSocialContentException(data,
					EnvSocialResource.FEATURE, e);
		}
	}

	public String getDescriptionText() {
		return mDescriptionText;
	}

	public String getNewestInfoText() {
		return mNewestInfoText;
	}

	public String getPeopleCountText() {
		return peopleCountText;
	}

	public String getLogoImageUri() {
		return mLogoImageUri;
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
