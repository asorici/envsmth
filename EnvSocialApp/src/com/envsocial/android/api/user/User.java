package com.envsocial.android.api.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.envsocial.android.api.AppClient;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.Url;

public class User {
	public static final String TAG = "user";
	
	private Location mLocation;
	private UserProfileData mUserData;
	
	private String mUri;
	
	public User(Location location, UserProfileData userdata) {
		
		mLocation = location;
		mUserData = userdata;
	}
	
	public User(Location location, UserProfileData userdata, String uri) {
		this(location, userdata);
		mUri = uri;
	}
	
	
	public static List<User> getUsers(Context context, Location location, String showprofile, String jsonString) throws Exception {
		if (jsonString == null) { 
			String type = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
			Url url = new Url(Url.RESOURCE, TAG);
			
			if (showprofile != null) {
				url.setParameters(
					new String[] { type, "showprofile" }, 
					new String[] { location.getId(), showprofile }
				);
			}
			else {
				url.setParameters(
					new String[] { type }, 
					new String[] { location.getId() }
				);
			}
			
			//System.err.println("[DEBUG]>> Url for getUSERS: " + url);
			// retrieve all users until exhaustion
			return getUsersList(context, url.toString(), location, true);
		}
		else {
			return parse(context, jsonString, location);
		}
	}
	
	
	public static List<User> getUsers(Context context, Location location, String showprofile, int offset, int limit) throws Exception {
		String type = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		Url url = new Url(Url.RESOURCE, TAG);
		if (showprofile != null) {
			url.setParameters(
				new String[] { type, "showprofile", "offset", "limit" }, 
				new String[] { location.getId(), showprofile, offset + "", limit + "" }
			);
		}
		else {
			url.setParameters(
				new String[] { type, "offset", "limit" }, 
				new String[] { location.getId(), offset + "", limit + "" }
			);
		}
		
		return getUsersList(context, url.toString(), location, false);
	}
	
	
	private static List<User> getUsersList(Context context, String url, 
			Location location, boolean retrieveAll) throws Exception {
		
		AppClient client = new AppClient(context);
		HttpResponse response = client.makeGetRequest(url);
		
		// Check the status code
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			throw new Exception(Integer.toString(HttpStatus.SC_SERVICE_UNAVAILABLE));
		}
		
		// If SC_OK, parse response
		String responseData = EntityUtils.toString(response.getEntity());
		List<User> users = parse(context, responseData, location);
		
		// if we want to consume the entire user list
		if (retrieveAll) {
			JSONObject meta = new JSONObject(responseData).getJSONObject("meta");
			String next = meta.getString("next");
			
			if (next != null && !next.equalsIgnoreCase("null")) {
				next = Url.getFullPath(next);
				users.addAll(getUsersList(context, next, location, true));
			}
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
			
			users.add(new User(location, userdata, uri));
		}
		
		return users;
	}
	
	
	public static String toJSON(List<User> users) {
		try {
			if (users == null) {
				return null;
			}
			
			JSONArray userListJSON = new JSONArray();
			int len = users.size();
			
			for (int i = 0; i < len; i++) {
				User userdata = users.get(i);
				
				
				
				//System.err.println("[DEBUG]>> checked in people research_profile: " + research_profile);
				
				// build user object hash
				JSONObject userJSON = new JSONObject();
				userJSON.put("resource_uri", userdata.getUri());
				userJSON.put("first_name", userdata.getUserData().getFirstName());
				userJSON.put("last_name", userdata.getUserData().getLastName());
				
				// add subprofiles if existent
				List<Map<String, JSONObject>> subProfiles = userdata.getUserData().subProfilesToJson();
				if (subProfiles != null && !subProfiles.isEmpty()) {
					JSONObject subProfileJSON = new JSONObject();
					
					for (Map<String, JSONObject> subProfile : subProfiles) {
						for (String key : subProfile.keySet()) {
							subProfileJSON.put(key, subProfile.get(key));
						}
					}
					
					userJSON.put("subprofiles", subProfileJSON);
				}
				
				userListJSON.put(userJSON);
			}
			
			JSONObject list = new JSONObject();
			list.put("objects", userListJSON);
			String jsonString = list.toString();
			
			//System.err.println("[DEBUG]>> created checked in people JSON string: " + jsonString);
			return jsonString;
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	public Location getLocation() {
		return mLocation;
	}
	
	public UserProfileData getUserData() {
		return mUserData;
	}
	
	public String getUri() {
		return mUri;
	}
	
}
