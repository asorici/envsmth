package com.envsocial.android.features.description;

import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.envsocial.android.Envived;
import com.envsocial.android.api.EnvSocialResource;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.FeatureDbHelper;
import com.envsocial.android.utils.Preferences;

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

	public DescriptionFeature(String category, int version, Calendar timestamp, String resourceUri,
			String environmentUri, String areaUri, String data, boolean virtualAccess) {

		super(category, version, timestamp, resourceUri, environmentUri, areaUri, data, virtualAccess);
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
	public FeatureDbHelper getLocalDatabaseSupport() {
		return null;
	}

	@Override
	protected void featureInit() throws EnvSocialContentException {
		String serializedData = retrievedData;
		try {
			String localCacheFileName = getLocalCacheFileName(category, environmentUrl, areaUrl, version);
			
			// if no newly retrieved data, check for serialized cached data in shared preferences
			if (serializedData == null) {
				serializedData = Preferences.getSerializedFeatureData(Envived.getContext(), localCacheFileName);
			}
			else {
				Preferences.setSerializedFeatureData(Envived.getContext(), localCacheFileName, serializedData);
			}
			
			if (serializedData != null) {
				JSONObject descriptionData = new JSONObject(serializedData);
				mDescriptionText = descriptionData.optString(DESCRIPTION_TEXT, NO_DESCRIPTION);
				mNewestInfoText = descriptionData.optString(NEWEST_INFO_TEXT, NO_NEWEST_INFO);
				mLogoImageUri = descriptionData.optString(IMG_URL, null);
			}
		} 
		catch (JSONException e) {
			throw new EnvSocialContentException(serializedData, EnvSocialResource.FEATURE, e);
		}
	}

	@Override
	protected void featureUpdate() throws EnvSocialContentException {
		try {
			if (retrievedData != null) {
				JSONObject descriptionData = new JSONObject(retrievedData);
				mDescriptionText = descriptionData.optString(DESCRIPTION_TEXT, NO_DESCRIPTION);
				mNewestInfoText = descriptionData.optString(NEWEST_INFO_TEXT, NO_NEWEST_INFO);
				mLogoImageUri = descriptionData.optString(IMG_URL, null);
			}
		} catch (JSONException e) {
			throw new EnvSocialContentException(retrievedData, EnvSocialResource.FEATURE, e);
		}
	}

	@Override
	protected void featureCleanup(Context context) {}

	@Override
	protected void featureClose(Context context) {}
}
