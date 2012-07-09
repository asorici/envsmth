package com.envsocial.android.api.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.envsocial.android.api.user.UserProfileConfig.UserSubProfileType;

public class UserProfileData {
	private String firstName;
	private String lastName;
	private Map<UserSubProfileType, UserSubProfile> subProfileMap;
	
	UserProfileData(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
		
		initSubProfileMap();
	}
	
	
	private void initSubProfileMap() {
		for (UserSubProfileType type : UserProfileConfig.subProfileClassMap.keySet()) {
			try {
				Class<?> subProfileClass = Class.forName(UserProfileConfig.subProfileClassMap.get(type));
				UserSubProfile subProfile = (UserSubProfile)subProfileClass.
						getConstructor(UserSubProfileType.class).newInstance(type);
				
				subProfileMap.put(type, subProfile);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	public String getFirstName() {
		return firstName;
	}
	
	
	public String getLastName() {
		return lastName;
	}
	
	public static UserProfileData parseProfileData(JSONObject user) throws JSONException {
		String firstName = user.optString("first_name", "Anonymous");
		String lastName = user.optString("last_name", "Guest");
		if (firstName.isEmpty())
			firstName = "Anonymous";
		if (lastName.isEmpty())  
			lastName = "Guest";
		
		UserProfileData profileData = new UserProfileData(firstName, lastName);
		
		JSONObject subProfiles = user.optJSONObject("subprofiles");
		if (subProfiles != null) {
			profileData.parseSubProfiles(profileData, subProfiles);
		}
		
		return profileData;
	}
	
	void parseSubProfiles(UserProfileData profileData, JSONObject subProfiles) {
		for (UserSubProfileType type : subProfileMap.keySet()) {
			UserSubProfile subProfile = subProfileMap.get(type);
			
			UserSubProfile parsedProfile = subProfile.parseProfileData(subProfiles);
			if (parsedProfile != null) {
				subProfileMap.put(type, parsedProfile);
			}
		}
	}


	public List<Map<String, JSONObject>> subProfilesToJson() {
		List<Map<String, JSONObject>> subProfileJsonList = new ArrayList<Map<String,JSONObject>>();
		for (UserSubProfileType type : subProfileMap.keySet()) {
			UserSubProfile subProfile = subProfileMap.get(type);
			
			try {
				JSONObject subProfileJson = subProfile.toJSON();
				Map<String, JSONObject> m = new HashMap<String, JSONObject>();
				m.put(type.name(), subProfileJson);
				
				subProfileJsonList.add(m);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		return subProfileJsonList;
	}
	
	
	public Map<UserSubProfileType, UserSubProfile> getSubProfileMap() {
		return subProfileMap;
	}
	
	public UserSubProfile getSubProfile(UserSubProfileType type) {
		return subProfileMap.get(type);
	}
}
