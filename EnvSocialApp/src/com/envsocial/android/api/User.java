package com.envsocial.android.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class User {
	public static final String TAG = "user";
	
	private Context mContext;
	private Location mLocation;
	private UserProfileData mUserData;
	private String mUri;
	
	public User(Context context, Location location, UserProfileData userdata) {
		mContext = context;
		mLocation = location;
		mUserData = userdata;
	}
	
	public User(Context context, Location location, UserProfileData userdata, String uri) {
		this(context, location, userdata);
		mUri = uri;
	}
	
	
	public static List<User> getUsers(Context context, Location location) throws Exception {
		String type = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		Url url = new Url(Url.RESOURCE, TAG);
		url.setParameters(new String[] { type }, 
				new String[] { location.getId() }
		);
		
		return getUsersList(context, url.toString(), location);
	}
	
	
	public static List<User> getUsers(Context context, Location location, int offset, int limit) throws Exception {
		String type = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		Url url = new Url(Url.RESOURCE, TAG);
		url.setParameters(new String[] { type, "offset", "limit" }, 
				new String[] { location.getId(), offset + "", limit + "" }
		);
		
		return getUsersList(context, url.toString(), location);
	}
	
	private static List<User> getUsersList(Context context, String url, Location location) throws Exception {
		AppClient client = new AppClient(context);
		HttpResponse response = client.makeGetRequest(url);
		
		// Check the status code
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			throw new Exception(Integer.toString(HttpStatus.SC_SERVICE_UNAVAILABLE));
		}
		
		// If SC_OK, parse response
		String responseData = EntityUtils.toString(response.getEntity());
		
		
		// For now we will do several requests until we consume the entire user list
		List<User> users = parse(context, responseData, location);
		JSONObject meta = new JSONObject(responseData).getJSONObject("meta");
		String next = meta.getString("next");
		
		if (next != null) {
			users.addAll(getUsersList(context, next, location));
		}
		
		return users;
	}
	
	
	// Parse response to get a list of User objects
	private static List<User> parse(Context context, String jsonString, 
			Location location) throws JSONException {

		JSONArray array = new JSONObject(jsonString).getJSONArray("objects");
		int len = array.length();

		List<User> users = new ArrayList<User>();
		
		for (int i = 0; i < len; ++i) {
			JSONObject user = array.getJSONObject(i);
			
			String uri = user.getString("resource_uri");
			UserProfileData userdata = UserProfileData.parseProfileData(user);
			
			users.add(new User(context, location, userdata, uri));
		}

		return users;
	}
	
	public Location getLocation() {
		return mLocation;
	}
	
	public UserProfileData getData() {
		return mUserData;
	}
	
	public String getUri() {
		return mUri;
	}
	
	public static class UserProfileData {
		
		UserProfileData(String firstName, String lastName, String affiliation, String[] researchInterests) {
			this.firstName = firstName;
			this.lastName = lastName;
			this.affiliation = affiliation;
			this.researchInterests = researchInterests;
		}

		private String firstName;
		private String lastName;
		private String affiliation;
		private String[] researchInterests;
		
		public String getFirstName() {
			return firstName;
		}
		
		public String getLastName() {
			return lastName;
		}
		
		public String getAffiliation() {
			return affiliation;
		}
		
		public String[] getResearchInterests() {
			return researchInterests;
		}
		
		public static UserProfileData parseProfileData(JSONObject user) throws JSONException {
			String firstName = user.getString("first_name");
			String lastName = user.getString("last_name");
			
			JSONObject research_profile = user.getJSONObject("research_profile");
			String affiliation = research_profile.getString("affiliation");
			
			JSONArray research_interests = research_profile.getJSONArray("research_interests");
			int len = research_interests.length();
			String[] researchInterests = new String[len];
			 
			for (int i = 0; i < len; i++) {
				researchInterests[i] = research_interests.getString(i);
			}
			
			return new UserProfileData(firstName, lastName, affiliation, researchInterests);
		}
	}
}
