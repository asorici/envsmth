package com.envsocial.android.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import android.content.Context;

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
	
	
	public static int login(Context context, String email, String password) {
		
		AppClient client = new AppClient(context);
		
		// Add credentials to payload
		List<NameValuePair> data = new ArrayList<NameValuePair>(2);
		data.add(new BasicNameValuePair("email", email));
		data.add(new BasicNameValuePair("password", password));
		
		// Make request and handle login
		try {
			// TODO
			String url = Url.actionUrl(LOGIN);
//			String url = (new Url(Url.ACTION, LOGIN, true, Url.HTTPS)).toString();
			HttpResponse response = client.makePostRequest(url, data, null, null);
			ResponseHolder holder = new ResponseHolder(response);
			
			int statusCode = holder.getCode();
			if (statusCode == HttpStatus.SC_OK) {
				JSONObject dataJSON = holder.getData().getJSONObject("data");
				
				String user_uri = dataJSON.getString("resource_uri");
				String firstName = dataJSON.optString("first_name", "Anonymous");
				String lastName = dataJSON.optString("last_name", "Guest");
				Preferences.login(context, email, firstName, lastName, user_uri);
			}
			
			return statusCode;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return HttpStatus.SC_SERVICE_UNAVAILABLE;
	}
	
	public static int logout(Context context) {
		try {
			AppClient client = new AppClient(context);
			HttpResponse response = client.makeGetRequest(Url.actionUrl(LOGOUT));
			
//			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				Preferences.logout(context);
//			}
			
			return response.getStatusLine().getStatusCode();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return HttpStatus.SC_SERVICE_UNAVAILABLE;
	}
	
	
	public static ResponseHolder checkin(Context context, String url) {
		if (url == null) {
			// If url is null, try to grab a saved checked in location
			Location l = Preferences.getCheckedInLocation(context);
			return new ResponseHolder(HttpStatus.SC_OK, null, l);
		}
		
		// Sign url for client requests
		url = signUrl(url);
		try {
			AppClient client = new AppClient(context);
			HttpResponse response = client.makeGetRequest(url);
			
			ResponseHolder holder = new ResponseHolder(response);
			if (holder.getCode() == HttpStatus.SC_OK) {
				JSONObject checkinData = holder.getData();
				Location checkinLoc = new Location(checkinData.getJSONObject("data"));
				
				holder.setTag(checkinLoc);
				Preferences.checkin(context, checkinLoc);
			}
			
			return holder;
		} catch (HttpHostConnectException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static int checkout(Context context) {
		try {
			AppClient client = new AppClient(context);
			HttpResponse response = client.makeGetRequest(Url.actionUrl(CHECKOUT));
			
			int code = response.getStatusLine().getStatusCode();
			if (code == HttpStatus.SC_OK) {
				Preferences.checkout(context);
			}
			
			return code;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return HttpStatus.SC_SERVICE_UNAVAILABLE;
	}
	
	
	private static String signUrl(String url) {
		//TODO: proper url signing
		return Url.appendParameter(url, "clientrequest", "true");
	}

	public static int register(Context context, String email,
			String password, String firstName, String lastName, String affiliation, String interests) {
		
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
		String responseString = "nimic";
		try {
			// TODO
			String url = Url.actionUrl(REGISTER);
			response = client.makePostRequest(url, data, null, null);
			ResponseHolder holder = new ResponseHolder(response);
			
			int statusCode = holder.getCode();
			if (statusCode == HttpStatus.SC_OK) {
				JSONObject dataJSON = holder.getData().getJSONObject("data");
				
				String user_uri = dataJSON.getString("resource_uri");
				String userfirstName = dataJSON.optString("first_name", "Anonymous");
				String userlastName = dataJSON.optString("last_name", "Guest");
				Preferences.login(context, email, userfirstName, userlastName, user_uri);
			}
			
			return statusCode;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return HttpStatus.SC_SERVICE_UNAVAILABLE;
	}
	
}
