package com.envsocial.android.features.socialmedia;

import java.io.Serializable;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.envsocial.android.Envived;
import com.envsocial.android.R;
import com.envsocial.android.api.EnvSocialResource;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.FeatureDbHelper;
import com.envsocial.android.utils.Preferences;

public class SocialMediaFeature extends Feature {
	private static final long serialVersionUID = 1L;
	
	private static final String FACEBOOK_URL = "facebook_url";
	private static final String TWITTER_URL = "twitter_url";
	private static final String GOOGLE_PLUS_URL = "google_plus_url";
	private static final String INTERNAL_FORUM_URL = "internal_forum_url";
	
	private String mFacebookUrl;
	private String mTwitterUrl;
	private String mGooglePlusUrl;
	private String mInternalForumUrl;
	private List<SocialMediaLink> mSocialMediaLinks;
	
	public SocialMediaFeature(String category, int version, Calendar timestamp, boolean isGeneral, 
			String resourceUrl, String environmentUrl, String areaUrl, String data,
			boolean virtualAccess) {
		
		super(category, version, timestamp, isGeneral, resourceUrl, environmentUrl,
				areaUrl, data, virtualAccess);
		
		mSocialMediaLinks = new LinkedList<SocialMediaFeature.SocialMediaLink>();
	}
	
	
	public String getFacebookUrl() {
		return mFacebookUrl;
	}

	public String getTwitterUrl() {
		return mTwitterUrl;
	}

	public String getGooglePlusUrl() {
		return mGooglePlusUrl;
	}

	public String getInternalForumUrl() {
		return mInternalForumUrl;
	}
	
	public List<SocialMediaLink> getSocialMediaLinks() {
		return mSocialMediaLinks;
	}
	
	public int getNumLinks() {
		return mSocialMediaLinks.size();
	}
	
	@Override
	protected void featureInit(boolean insert) throws EnvSocialContentException {
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
			
			JSONObject socilaMediaUrlData = new JSONObject(serializedData);
			mFacebookUrl = socilaMediaUrlData.optString(FACEBOOK_URL, null);
			if (mFacebookUrl != null) {
				mSocialMediaLinks.add(new SocialMediaLink("Facebook", mFacebookUrl, R.drawable.icon_facebook));
			}
			
			mTwitterUrl = socilaMediaUrlData.optString(TWITTER_URL, null);
			if (mTwitterUrl != null) {
				mSocialMediaLinks.add(new SocialMediaLink("Twitter", mTwitterUrl, R.drawable.icon_twitter));
			}
			
			mGooglePlusUrl = socilaMediaUrlData.optString(GOOGLE_PLUS_URL, null);
			if (mGooglePlusUrl != null) {
				mSocialMediaLinks.add(new SocialMediaLink("Google Plus", mGooglePlusUrl, R.drawable.icon_google_plus));
			}
			
			mInternalForumUrl = socilaMediaUrlData.optString(INTERNAL_FORUM_URL, null);
			if (mInternalForumUrl != null) {
				mSocialMediaLinks.add(new SocialMediaLink("Forum", mInternalForumUrl, R.drawable.icon_internal_forum));
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
				JSONObject socilaMediaUrlData = new JSONObject(retrievedData);
				mFacebookUrl = socilaMediaUrlData.optString(FACEBOOK_URL, null);
				mTwitterUrl = socilaMediaUrlData.optString(TWITTER_URL, null);
				mGooglePlusUrl = socilaMediaUrlData.optString(GOOGLE_PLUS_URL, null);
				mInternalForumUrl = socilaMediaUrlData.optString(INTERNAL_FORUM_URL, null);
			}
		} catch (JSONException e) {
			throw new EnvSocialContentException(retrievedData, EnvSocialResource.FEATURE, e);
		}
	}

	@Override
	protected void featureCleanup(Context context) {}

	@Override
	protected void featureClose(Context context) {}

	@Override
	public boolean hasLocalQuerySupport() {
		return false;
	}

	@Override
	public int getDisplayThumbnail() {
		return R.drawable.details_icon_annotation_white;
	}

	@Override
	public String getDisplayName() {
		return "Social Media";
	}

	@Override
	public FeatureDbHelper getLocalDatabaseSupport() {
		return null;
	}

	@Override
	public boolean hasLocalDatabaseSupport() {
		return false;
	}
	
	
	static class SocialMediaLink implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private String mName;
		private String mUrl;
		private int mIconDrawable;
		
		public SocialMediaLink(String name, String url, int iconDrawable) {
			mName = name;
			mUrl = url;
			mIconDrawable = iconDrawable;
		}
		
		public String getName() {
			return mName;
		}
		
		public String getUrl() {
			return mUrl;
		}
		
		public int getIconDrawable() {
			return mIconDrawable;
		}
	}
}
