package com.envsocial.android.features.socialmedia;

import java.util.List;
import java.util.Locale;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentTabHost;

import com.envsocial.android.R;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.EnvivedFeatureActivity;
import com.envsocial.android.features.Feature;
import com.envsocial.android.features.socialmedia.SocialMediaFeature.SocialMediaLink;

public class SocialMediaActivity extends EnvivedFeatureActivity {
	
	private FragmentTabHost mTabHost;
	private SocialMediaFeature mSocialMediaFeature;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.social_media_tab_layout);
        mTabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);
    }
	
	private void bindSocialMediaViewData() {
		List<SocialMediaLink> socialMediaLinks = mSocialMediaFeature.getSocialMediaLinks();
		
		int len = socialMediaLinks.size();
		for (int i = 0; i < len; i++) {
			SocialMediaLink link = socialMediaLinks.get(i);
			
			Bundle args = new Bundle();
			args.putString("social_media_url", link.getUrl());
			
			String tabName = link.getName();
			String tabTag = tabName.toLowerCase(Locale.US);
			Drawable iconDrawable = getResources().getDrawable(link.getIconDrawable());
			
			mTabHost.addTab(mTabHost.newTabSpec(tabTag).setIndicator(tabName, iconDrawable),
	                SocialMediaFragment.class, args);
		}
	}

	
	
	@Override
	protected Feature getLocationFeature(Location location) throws EnvSocialContentException {
		return location.getFeature(Feature.SOCIAL_MEDIA);
	}

	@Override
	protected void onFeatureDataInitialized(Feature newFeature, boolean success) {
		if (success) {
			mSocialMediaFeature = (SocialMediaFeature) newFeature;
			bindSocialMediaViewData();
		}
	}

	

	@Override
	protected void onFeatureDataUpdated(Feature updatedFeature, boolean success) {
		if (success) {
			mSocialMediaFeature = (SocialMediaFeature) updatedFeature;
			bindSocialMediaViewData();
		}
	}

	@Override
	protected String getActiveUpdateDialogMessage() {
		return  "The URLs for the social media links have changed. This update will refresh your view. " +
				"Please select YES if you wish to perform the update. Select NO if, you " +
				"want to keep your current browsing session. The update will be performed next time you " +
				"start this activity.";
	}

}
