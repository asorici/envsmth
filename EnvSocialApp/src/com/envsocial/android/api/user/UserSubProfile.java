package com.envsocial.android.api.user;

import org.json.JSONException;
import org.json.JSONObject;

import com.envsocial.android.api.user.UserProfileConfig.UserSubProfileType;

public abstract class UserSubProfile {
	protected UserSubProfileType profileType;
	protected boolean populated = false;
	
	public UserSubProfile(UserSubProfileType type) {
		profileType = type;
	}
	
	public UserSubProfileType getProfileType() {
		return profileType;
	}
	
	protected boolean isPopulated() {
		return populated;
	}

	protected void setPopulated(boolean populated) {
		this.populated = populated;
	}
	
	protected abstract UserSubProfile parseProfileData(JSONObject user);
	protected abstract JSONObject toJSON() throws JSONException;
}
