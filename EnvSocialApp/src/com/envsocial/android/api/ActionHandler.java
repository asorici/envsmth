package com.envsocial.android.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import android.content.Context;

import com.envsocial.android.api.exceptions.EnvSocialComException;
import com.envsocial.android.api.exceptions.EnvSocialComException.HttpMethod;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.ResponseHolder;

public class ActionHandler {
	
	public final static String LOGIN = "login";
	public final static String LOGOUT = "logout";
	public final static String CHECKIN = "checkin";
	public final static String CHECKOUT = "checkout";
	public final static String REGISTER = "register";
	
	
	public static int registerWithServer(Context context, 
			String deviceRegistrationId) throws Exception {
		return makeRegistrationRequestToServer(context, 
				"c2dm_id", 
				deviceRegistrationId
				);
	}
	
	public static int unregisterWithServer(Context context) throws Exception {
		return makeRegistrationRequestToServer(context, 
				"unregister_c2dm", 
				true
				);
	}
	
	private static int makeRegistrationRequestToServer(Context context, 
			String param, Object value) throws Exception {
		
		String url = Url.fromUri(Preferences.getLoggedInUserUri(context));
		
		String data = new JSONStringer()
			.object()
				.key(param)
				.value(value)
			.endObject()
			.toString();
		
		// Perform registration request
		AppClient client = new AppClient(context);
		HttpResponse response = client.makePutRequest(url,
				data,
				new String[] {"Content-Type", "Data-Type"},
				new String[] {"application/json", "json"}
				);
		
		return response.getStatusLine().getStatusCode();
	}
	
	
	public static ResponseHolder login(Context context, String email, String password) {
		
		AppClient client = new AppClient(context);
		
		// Add credentials to payload
		List<NameValuePair> data = new ArrayList<NameValuePair>(2);
		data.add(new BasicNameValuePair("email", email));
		data.add(new BasicNameValuePair("password", password));
		
		HttpResponse response = null;
		try {
			String url = Url.actionUrl(LOGIN);
			response = client.makePostRequest(url, data, null, null);
		} catch (IOException e) {
			return new ResponseHolder(new EnvSocialComException(null, 
					HttpMethod.POST, EnvSocialResource.LOGIN, e));
		}
		
		// handle login response
		ResponseHolder holder = ResponseHolder.parseResponse(response);
		
		try {
			if (!holder.hasError()) {
				int statusCode = holder.getCode();
				
				if (statusCode == HttpStatus.SC_OK) {
					JSONObject dataJSON = holder.getJsonContent().getJSONObject("data");
					
					String user_uri = dataJSON.getString("resource_uri");
					String firstName = dataJSON.optString("first_name", "Anonymous");
					String lastName = dataJSON.optString("last_name", "Guest");
					Preferences.login(context, email, firstName, lastName, user_uri);
				}
			}
			
			return holder;
		} catch (JSONException e) {
			return new ResponseHolder(new EnvSocialContentException(holder.getResponseBody(), 
											EnvSocialResource.LOGIN, e));
		}
	}
	
	
	public static ResponseHolder logout(Context context) {
		String userUri = Preferences.getLoggedInUserUri(context);
		AppClient client = new AppClient(context);
		
		HttpResponse response = null;
		try {
			response = client.makeGetRequest(Url.actionUrl(LOGOUT));
		} catch (IOException e) {
			return new ResponseHolder(new EnvSocialComException(userUri, HttpMethod.GET, 
											EnvSocialResource.LOGOUT, e));
		}
		
		// handle logout response
		ResponseHolder holder = ResponseHolder.parseResponse(response);
		if (!holder.hasError()) {
			Preferences.logout(context);
		}
		
		return holder;
	}
	
	
	public static ResponseHolder checkin(Context context, String url) {
		
		String userUri = Preferences.getLoggedInUserUri(context);
		AppClient client = new AppClient(context);
		
		if (url == null) {
			// If url is null, try to grab a saved checked in location
			Location l = Preferences.getCheckedInLocation(context);
			return new ResponseHolder(HttpStatus.SC_OK, null, l);
		}
		
		// Sign url for client requests
		url = signUrl(url);
		HttpResponse response = null;
		try {
			response = client.makeGetRequest(url);
		} catch (IOException e) {
			return new ResponseHolder(new EnvSocialComException(userUri, HttpMethod.GET, 
											EnvSocialResource.CHECKIN, e));
		}
		
		ResponseHolder holder = ResponseHolder.parseResponse(response);
		try {
			if (!holder.hasError()) {
				if (holder.getCode() == HttpStatus.SC_OK) {
					JSONObject checkinData = holder.getJsonContent();
					Location checkinLoc = new Location(checkinData.getJSONObject("data"));
					
					holder.setTag(checkinLoc);
					Preferences.checkin(context, checkinLoc);
				}
			}
			
			return holder;
			
		} catch (JSONException e) {
			return new ResponseHolder(new EnvSocialContentException(holder.getResponseBody(), 
											EnvSocialResource.CHECKIN, e));
		} 
	}
	
	public static ResponseHolder checkout(Context context) {
		String userUri = Preferences.getLoggedInUserUri(context);
		AppClient client = new AppClient(context);
		
		HttpResponse response = null;
		try {
			response = client.makeGetRequest(Url.actionUrl(CHECKOUT));
		} catch (IOException e) {
			return new ResponseHolder(new EnvSocialComException(userUri, HttpMethod.GET, 
											EnvSocialResource.CHECKOUT, e));
		}
		
		ResponseHolder holder = ResponseHolder.parseResponse(response);		
		if (!holder.hasError()) {
			if (holder.getCode() == HttpStatus.SC_OK) {
				Preferences.checkout(context);
			}
		}
		
		return holder;
	}
	
	
	private static String signUrl(String url) {
		//TODO: proper url signing
		return Url.appendParameter(url, "clientrequest", "true");
	}

	public static ResponseHolder register(Context context, String email, String password, 
			String firstName, String lastName, 
			String affiliation, String interests) {
		
		AppClient client = new AppClient(context);
		
		// Add credentials to payload
		List<NameValuePair> data = new ArrayList<NameValuePair>(6);
		data.add(new BasicNameValuePair("email", email));
		data.add(new BasicNameValuePair("password1", password));
		data.add(new BasicNameValuePair("password2", password));
		data.add(new BasicNameValuePair("first_name", firstName));
		data.add(new BasicNameValuePair("last_name", lastName));
		
		JSONObject researchProfileData = new JSONObject();
		try {
			researchProfileData.put("affiliation", affiliation);
			researchProfileData.put("research_interests", interests);
			data.add(new BasicNameValuePair("research_profile", researchProfileData.toString()));
		} catch (JSONException e1) {
			
		}
		
		
		HttpResponse response = null;
		try {
			String url = Url.actionUrl(REGISTER);
			response = client.makePostRequest(url, data, null, null);
		} catch (IOException e) {
			return new ResponseHolder(new EnvSocialComException(null, HttpMethod.POST, 
											EnvSocialResource.REGISTER, e));
		}
		
		ResponseHolder holder = ResponseHolder.parseResponse(response);
		
		try {
			if (!holder.hasError()) {
				if (holder.getCode() == HttpStatus.SC_OK) {
					JSONObject dataJSON = holder.getJsonContent().getJSONObject("data");
					
					String user_uri = dataJSON.getString("resource_uri");
					String userfirstName = dataJSON.optString("first_name", "Anonymous");
					String userlastName = dataJSON.optString("last_name", "Guest");
					Preferences.login(context, email, userfirstName, userlastName, user_uri);
				}
			}
			
			return holder;
		} catch (JSONException e) {
			return new ResponseHolder(new EnvSocialContentException(holder.getResponseBody(), 
											EnvSocialResource.REGISTER, e));
		}
	}
	
}
