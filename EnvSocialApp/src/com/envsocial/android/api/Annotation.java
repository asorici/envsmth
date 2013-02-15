package com.envsocial.android.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
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
import com.envsocial.android.utils.Utils;

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
		String userUri = Preferences.getUserUri(context);
		
		// build annotation request uri taking into account the type of access that is made
		boolean virtualAccess = mLocation.hasVirtualAccess();
		String annotationRequestUri = Url.appendOrReplaceParameter(Url.resourceUrl(TAG), "virtual", 
				Boolean.toString(virtualAccess));
		
		
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
			response = client.makePostRequest(annotationRequestUri,
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
		/*
		String locationType = (mLocation.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		Url url = new Url(Url.RESOURCE, locationType, false, Url.HTTP);
		url.setItemId(mLocation.getId());
		String locationUri = url.toString();
		*/
		String locationType = (mLocation.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		String locationUri = mLocation.getLocationUri();
		
		Object data = null;
		try {
			data = new JSONObject(mData);
		} catch (JSONException e) {
			data = mData;
		}
		
		JSONObject jsonData = new JSONObject();
		jsonData.put(locationType, locationUri);
		jsonData.put("category", mCategory);
		jsonData.put("data", data);
		
		return jsonData.toString();
	}
	
	
	public static List<Annotation> getAnnotations(Context context, Location location, String category, 
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
					
					try {
						values[i] = URLEncoder.encode(extra.get(key), "UTF-8");
					} catch (UnsupportedEncodingException e) {
						throw new EnvSocialContentException(null, EnvSocialResource.ANNOTATION, e);
					}
					
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
	
	
	public static List<Annotation> getAnnotations(Context context, Location location, String category, 
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
				
				try {
					values[i] = URLEncoder.encode(extra.get(key), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new EnvSocialContentException(null, EnvSocialResource.ANNOTATION, e);
				}
				
				i++;
			}
		}
		
		url.setParameters(keys, values);
		
		return getAnnotationsList(context, url.toString(), location, false);
	}
	
	// this can be re-written in terms of getAnnotations + an extra
	public static List<Annotation> getAllAnnotationsForEnvironment(Context context, 
			Location location, String category, Calendar timestamp) throws EnvSocialComException, EnvSocialContentException {
		
		String envId = location.getId();
		if (location.isArea()) {
			envId = Url.resourceIdFromUrl(location.getParentUrl());
		}
		
		Url url = new Url(Url.RESOURCE, TAG);
		url.setParameters(
				new String[] { "virtual", Location.ENVIRONMENT, "all", "category", "order_by" }, 
				new String[] { Boolean.toString(location.hasVirtualAccess()), 
						envId, "true", category, "timestamp" }
		);
		
		String annotationRequestUrl  = url.toString();
		if (timestamp != null) {
			String timeStr = Utils.calendarToString(timestamp, "yyyy-MM-dd HH:mm:ss");
			try {
				timeStr = URLEncoder.encode(timeStr, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new EnvSocialContentException(timeStr, EnvSocialResource.ANNOTATION, e);
			}
			annotationRequestUrl = Url.appendOrReplaceParameter(annotationRequestUrl, "timestamp__gt", timeStr);
		}
		
		// consume all annotations from server - no pagination needed afterwards
		return getAnnotationsList(context, annotationRequestUrl, null, true);
	}
	
	
	private static List<Annotation> getAnnotationsList(Context context, 
			String annotationRequestUrl, Location location, 
			boolean retrieveAll) throws EnvSocialComException, EnvSocialContentException {
		
		// get data of the user executing this action
		String userUri = Preferences.getUserUri(context);
		AppClient client = new AppClient(context);
		
		// append virtual access flag to the request url
		// if location is null, the flag will have been set earlier in the url
		if (location != null) {
			boolean virtualAccess = location.hasVirtualAccess();
			annotationRequestUrl = Url.appendOrReplaceParameter(annotationRequestUrl, "virtual", 
					Boolean.toString(virtualAccess));
		}
		
		
		HttpResponse response;
		String responseData;
		
		try {
			response = client.makeGetRequest(annotationRequestUrl);
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
					next = Url.getFullPath(next);
					annotations.addAll(getAnnotationsList(context, next, location, true));
				}
			}
			
			return annotations;
		
		} catch (JSONException e) {
			throw new EnvSocialContentException(responseData, EnvSocialResource.ANNOTATION, e);
		} catch (ParseException e) {
			throw new EnvSocialContentException(responseData, EnvSocialResource.ANNOTATION, e);
		}
	}
	
	
	// Parse response to get a list of Annotation objects
	private static List<Annotation> parse(Context context, 
			String jsonString, Location location) throws JSONException, ParseException {
		
		JSONArray array = new JSONObject(jsonString).getJSONArray("objects");
		int len = array.length();
		
		List<Annotation> annotations = new ArrayList<Annotation>();
		for (int i = 0; i < len; ++ i) {
			JSONObject annotationObject = array.getJSONObject(i);
			Annotation ann = parseAnnotation(context, location, annotationObject);
			annotations.add(ann);
		}
		
		return annotations;
	}
	
	
	public static Annotation parseAnnotation(Context context, Location location, JSONObject annotationObject) 
			throws JSONException, ParseException {
		String resourceUri = annotationObject.getString("resource_uri");
		String category = annotationObject.getString("category");
		String userUri = annotationObject.optString("user", null);
		
		//String strTimestamp = annotation.getString("timestamp").replace('T', ' ');
		String strTimestamp = annotationObject.getString("timestamp");
		Calendar timestamp = null;
		try {
			// try it without milliseconds
			timestamp = Utils.stringToCalendar(strTimestamp, "yyyy-MM-dd'T'HH:mm:ssZ");
		}
		catch(ParseException ex) {
			// try it wit milliseconds
			timestamp = Utils.stringToCalendar(strTimestamp, "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");
		}
		
		Location annLocation = null;
		
		if (location == null) {
			// We need to parse the location
			JSONObject locObj;
			try {
				locObj = annotationObject.getJSONObject("area");
			} catch (JSONException e) {
				locObj = annotationObject.getJSONObject("environment");
			}
			
			String locationName = locObj.getString("name");
			String locationUri = locObj.getString("resource_uri");
			
			annLocation = new Location(locationName, locationUri);
		}
		else {
			annLocation = location;
		}
		
		String data = annotationObject.getString("data");
		return new Annotation(context, annLocation, category, timestamp, data, resourceUri, userUri);
	}
	
	
	public static int deleteAnnotation(Context context, String uri) throws Exception {
		String url = Url.getFullPath(uri);
		AppClient client = new AppClient(context);
		
		return client.makeDeleteRequest(url).getStatusLine().getStatusCode();
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
