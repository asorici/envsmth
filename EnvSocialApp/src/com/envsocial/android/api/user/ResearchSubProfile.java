package com.envsocial.android.api.user;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.envsocial.android.api.user.UserProfileConfig.UserSubProfileType;

public class ResearchSubProfile extends UserSubProfile {
	private String affiliation;
	private String[] researchInterests;
	
	public ResearchSubProfile(UserSubProfileType type) {
		super(type);
	}
	
	public ResearchSubProfile(UserSubProfileType type, String affiliation, String[] researchInterests) {
		super(type);
		this.affiliation = affiliation;
		this.researchInterests = researchInterests;
		
		setPopulated(true);
	}
	
	@Override
	protected UserSubProfile parseProfileData(JSONObject subProfiles) {
		String affiliation = "n.a.";
		String[] researchInterests = {"n.a."};
		
		//System.err.println("[DEBUG]>> user profile JSONObject: " + user.toString());
		
		JSONObject research_profile = (JSONObject)subProfiles.opt(profileType.name());
		
		if (research_profile != null) {
			affiliation = research_profile.optString("affiliation", "n.a.");
			
			JSONArray research_interests = research_profile.optJSONArray("research_interests");
			if (research_interests != null) {
				int len = research_interests.length();
				researchInterests = new String[len];
				 
				for (int i = 0; i < len; i++) {
					researchInterests[i] = research_interests.optString(i, "n.a.");
				}
			}
			
			return new ResearchSubProfile(UserSubProfileType.researchprofile, affiliation, researchInterests);
		}
		
		return null;
	}
	
	
	@Override
	protected JSONObject toJSON() throws JSONException {
		JSONObject research_profile = new JSONObject();
		
		// build research_interests list
		JSONArray research_interests = new JSONArray();
		for (int k = 0; k < researchInterests.length; k++) {
			research_interests.put(researchInterests[k]);
		}
		
		//System.err.println("[DEBUG]>> checked in people research_interests: " + research_interests);
		
		// build reseatrch_profile hash
		
		research_profile.put("affiliation", affiliation);
		research_profile.put("research_interests", research_interests);
		
		return research_profile;
	}
	
	
	public String getAffiliation() {
		return affiliation;
	}
	
	
	public String[] getResearchInterests() {
		return researchInterests;
	}
}
