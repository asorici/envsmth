package com.envsocial.android.api;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.envsocial.android.api.exceptions.EnvSocialComException;
import com.envsocial.android.api.exceptions.EnvSocialComException.HttpMethod;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.ResponseHolder;

public class Annotation {
	
	public static final String TAG = "annotation";
	
	public static final int DEFAULT_OFFSET = 0;
	public static final int DEFAULT_LIMIT = 20;
	
	private Location mLocation;
	private String mCategory;
	private String mData;
	private String mUri;
	private String mUserUri;
	private Calendar mTimestamp;
	
	public Annotation(Location location, String category, Calendar timestamp, String annotation) {
		
		mLocation = location;
		mCategory = category;
		mTimestamp = timestamp;
		mData = annotation;
	}
	
	public Annotation(Location location, 
			String category, Calendar timestamp, String annotation, String uri) {
		this(location, category, timestamp, annotation);
		mUri = uri;
	}
	
	public Annotation(Context context, Location location, 
			String category, Calendar timestamp, String annotation, String uri, String userUri) {
		this(location, category, timestamp, annotation);
		mUri = uri;
		mUserUri = userUri;
	}
	
	public ResponseHolder post(Context context) {
		AppClient client = new AppClient(context);
		String userUri = Preferences.getLoggedInUserUri(context);
		
		String jsonContent = "";
		
		try {
			jsonContent = toJSON();
		}
		catch (JSONException e) {
			return new ResponseHolder (new EnvSocialContentException(jsonContent, 
											EnvSocialResource.ANNOTATION, e));
		}
		
		HttpResponse response = null;
		try {
			response = client.makePostRequest(Url.resourceUrl(TAG),
					jsonContent,
					new String[] {"Content-Type", "Data-Type"},
					new String[] {"application/json", "json"}
			);
		} catch (IOException e) {
			return new ResponseHolder(new EnvSocialComException(userUri, HttpMethod.POST, 
											EnvSocialResource.ANNOTATION, e));
		} catch (org.apache.http.ParseException e) {
			return new ResponseHolder(new EnvSocialComException(userUri, HttpMethod.POST, 
					EnvSocialResource.ANNOTATION, e));
		} 
		
		return ResponseHolder.parseResponse(response);
	}
	
	
	private String toJSON() throws JSONException {
		//JSONStringer jsonData = new JSONStringer().object();
		
		String type = (mLocation.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		Url url = new Url(Url.RESOURCE, type, false, Url.HTTP);
		url.setItemId(mLocation.getId());
		String urlString = url.toString();
		
		Object data = null;
		try {
			data = new JSONObject(mData);
		} catch (JSONException e) {
			data = mData;
		}
		
		JSONObject jsonData = new JSONObject();
		jsonData.put(type, urlString);
		jsonData.put("category", mCategory);
		jsonData.put("data", data);
		
		return jsonData.toString();
	}
	
	
	public static List<Annotation> getAnnotations(Context context, 
			Location location, String category, 
			boolean retrieveAll) throws EnvSocialComException, EnvSocialContentException {
		
		if (retrieveAll) {
			String type = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
			Url url = new Url(Url.RESOURCE, TAG);
			url.setParameters(new String[] { type, "category" }, 
					new String[] { location.getId(), category }
			);
			
			return getAnnotationsList(context, url.toString(), location, true);
		}
		else {
			return getAnnotations(context, location, category, DEFAULT_OFFSET, DEFAULT_LIMIT);
		}
	}
	
	
	public static List<Annotation> getAnnotations(Context context, 
			Location location, String category, 
			Map<String, String> extra, 
			boolean retrieveAll) throws EnvSocialComException, EnvSocialContentException {
		
		if (retrieveAll) {
			String type = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
			Url url = new Url(Url.RESOURCE, TAG);
			
			int extraSize = 0;
			if (extra != null) {
				extraSize = extra.size();
			}
			
			String[] keys = new String[2 + extraSize];
			String[] values = new String[2 + extraSize];
			
			keys[0] = type;
			values[0] = location.getId();
			keys[1] = "category";
			values[1] = category;
			
			if (extra != null) {
				int i = 2;
				for (String key : extra.keySet()) {
					keys[i] = key;
					values[i] = extra.get(key);
					i++;
				}
			}
			
			url.setParameters(keys, values);
			
			return getAnnotationsList(context, url.toString(), location, true);
		}
		else {
			return getAnnotations(context, location, category, extra, DEFAULT_OFFSET, DEFAULT_LIMIT);
		}
	}
	
	
	public static List<Annotation> getAnnotations(Context context, 
			Location location, String category, 
			int offset, int limit) throws EnvSocialComException, EnvSocialContentException {
		
		String type = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		Url url = new Url(Url.RESOURCE, TAG);
		url.setParameters(new String[] { type, "category", "offset", "limit"}, 
				new String[] { location.getId(), category, offset + "", limit + "" }
		);
		
		return getAnnotationsList(context, url.toString(), location, false);
	}
	
	
	public static List<Annotation> getAnnotations(Context context, Location location, 
			String category, Map<String, String> extra, 
			int offset, int limit) throws EnvSocialComException, EnvSocialContentException {
		
		String type = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		Url url = new Url(Url.RESOURCE, TAG);
		
		int extraSize = 0;
		if (extra != null) {
			extraSize = extra.size();
		}
		
		String[] keys = new String[4 + extraSize];
		String[] values = new String[4 + extraSize];
		
		keys[0] = type;
		values[0] = location.getId();
		keys[1] = "category";
		values[1] = category;
		
		keys[2] = "offset"; keys[3] = "limit";
		values[2] = offset + ""; values[3] = limit + "";
		
		if (extra != null) {
			int i = 4;
			for (String key : extra.keySet()) {
				keys[i] = key;
				values[i] = extra.get(key);
				i++;
			}
		}
		
		url.setParameters(keys, values);
		
		return getAnnotationsList(context, url.toString(), location, false);
	}
	
	// this can be re-written in terms of getAnnotations + an extra
	public static List<Annotation> getAllAnnotationsForEnvironment(Context context, 
			Location location, String category) throws EnvSocialComException, EnvSocialContentException {
		
		String envId = location.getId();
		if (location.isArea()) {
			envId = Url.resourceIdFromUri(location.getParent());
		}
		
		Url url = new Url(Url.RESOURCE, TAG);
		url.setParameters(
				new String[] { Location.ENVIRONMENT, "all", "category", "order_by" }, 
				new String[] { envId, "true", category, "-timestamp" }
		);
		
		// consume all annotations from server - no pagination needed afterwards
		return getAnnotationsList(context, url.toString(), null, true);
	}
	
	
	private static List<Annotation> getAnnotationsList(Context context, 
			String url, Location location, 
			boolean retrieveAll) throws EnvSocialComException, EnvSocialContentException {
		
		// get data of the user executing this action
		String userUri = Preferences.getLoggedInUserUri(context);
		AppClient client = new AppClient(context);
		
		HttpResponse response;
		String responseData;
		
		try {
			response = client.makeGetRequest(url);
			responseData = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			throw new EnvSocialComException(userUri, HttpMethod.GET, EnvSocialResource.ANNOTATION, e);
		} 
		
		// Check the status code
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			Log.d(TAG, "[DEBUG]>> Error response on annotations list: " + responseData);
			throw EnvSocialComException.newInstanceFrom(
					response.getStatusLine().getStatusCode(), 
					userUri, HttpMethod.GET, EnvSocialResource.ANNOTATION, null);
		}
		
		// If SC_OK, parse response
		try {
			List<Annotation> annotations = parse(context, responseData, location);
			
			// if we want to consume the entire annotations list
			if (retrieveAll) {
				JSONObject meta = new JSONObject(responseData).getJSONObject("meta");
				String next = meta.getString("next");

				if (next != null && !next.equalsIgnoreCase("null")) {
					next = Url.fromUri(next);
					annotations.addAll(getAnnotationsList(context, next, location, true));
				}
			}
			
			return annotations;
		
		} catch (JSONException e) {
			throw new EnvSocialContentException(responseData, EnvSocialResource.ANNOTATION, e);
		}
	}
	
	
	// Parse response to get a list of Annotation objects
	private static List<Annotation> parse(Context context, 
			String jsonString, Location location) throws JSONException {
		
		JSONArray array = new JSONObject(jsonString).getJSONArray("objects");
		int len = array.length();
		
		List<Annotation> annotations = new ArrayList<Annotation>();
		for (int i = 0; i < len; ++ i) {
			JSONObject annotation = array.getJSONObject(i);
			String uri = annotation.getString("resource_uri");
			String userUri = annotation.getString("user");
			String category = annotation.getString("category");
			
			String strTimestamp = annotation.getString("timestamp").replace('T', ' ');
			Calendar timestamp = Calendar.getInstance();
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				timestamp.setTime(sdf.parse(strTimestamp));
			} catch (ParseException e1) {
				
			}
			
			Location annLocation = null;
			
			if (location == null) {
				// We need to parse the location
				JSONObject locObj;
				try {
					locObj = annotation.getJSONObject("area");
				} catch (JSONException e) {
					locObj = annotation.getJSONObject("environment");
				}
				
				String locationName = locObj.getString("name");
				String locationUri = locObj.getString("resource_uri");
				
				annLocation = new Location(locationName, locationUri);
			}
			else {
				annLocation = location;
			}
			
			String data = annotation.getString("data");
			annotations.add(new Annotation(context, annLocation, category, timestamp, data, uri, userUri));
		}
		
		return annotations;
	}
	
	
	public static int deleteAnnotation(Context context, 
			String uri) throws Exception {
		
		String url = Url.fromUri(uri);
		AppClient client = new AppClient(context);
		
		System.out.println("[DEBUG]>> Sending delete request for: " + url);
		
		return client.makeDeleteRequest(url)
						.getStatusLine()
							.getStatusCode();
	}
	
	
	public Location getLocation() {
		return mLocation;
	}
	
	public String getCategory() {
		return mCategory;
	}
	
	public String getData() {
		return mData;
	}
	
	public String getUri() {
		return mUri;
	}
	
	public Calendar getTimestamp() {
		return mTimestamp;
	}
	
	public String getUserUri() {
		return mUserUri;
	}
	
	@Override
	public String toString() {
		String info = "";
		info += "Location::" + mLocation + ", ";
		info += "Category::" + mCategory + ", ";
		info += "Data::" + mData;
		
		return info;
	}
}
