package com.envsocial.android.api;

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
import org.json.JSONStringer;

import android.content.Context;

public class Annotation {
	
	public static final String TAG = "annotation";
	
	private Context mContext;
	private Location mLocation;
	private String mCategory;
	private String mData;
	private String mUri;
	private String mUserUri;
	private Calendar mTimestamp;
	
	public Annotation(Context context, Location location, String category, Calendar timestamp, String annotation) {
		mContext = context;
		mLocation = location;
		mCategory = category;
		mTimestamp = timestamp;
		mData = annotation;
	}
	
	public Annotation(Context context, Location location, 
			String category, Calendar timestamp, String annotation, String uri) {
		this(context, location, category, timestamp, annotation);
		mUri = uri;
	}
	
	public Annotation(Context context, Location location, 
			String category, Calendar timestamp, String annotation, String uri, String userUri) {
		this(context, location, category, timestamp, annotation);
		mUri = uri;
		mUserUri = userUri;
	}
	
	public int post() throws Exception {
		AppClient client = new AppClient(mContext);
		//System.err.println("[DEBUG]>> annotation location: " + mLocation);
		//System.err.println("[DEBUG]>> annotation post: " + toJSON());
		
		HttpResponse response = client.makePostRequest(Url.resourceUrl(TAG),
				toJSON(),
				new String[] {"Content-Type", "Data-Type"},
				new String[] {"application/json", "json"}
		);
		
		//System.err.println("[DEBUG]>> Response for annotation post: " + response.toString());
		
		return response.getStatusLine().getStatusCode();
	}
	
	private String toJSON() throws JSONException {
		JSONStringer jsonData = new JSONStringer().object();
		String type = (mLocation.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		Url url = new Url(Url.RESOURCE, type, false, Url.HTTP);
		url.setItemId(mLocation.getId());
		String urlString = url.toString();
		
		Object data;
		try {
			data = new JSONObject(mData);
		} catch (JSONException e) {
			data = mData;
		}
		
		jsonData
			.key(type)
			.value(urlString)
			.key("category")
			.value(mCategory)
			.key("data")
			.value(data)
		.endObject();
		
		return jsonData.toString();
	}
	
	
	public static List<Annotation> getAnnotations(Context context, Location location) throws Exception {
		String type = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		Url url = new Url(Url.RESOURCE, TAG);
		url.setParameters(new String[] { type }, 
				new String[] { location.getId() }
		);
		
		return getAnnotationsList(context, url.toString(), location);
	}
	
	public static List<Annotation> getAnnotations(Context context, Location location, int offset, int limit) throws Exception {
		String type = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		Url url = new Url(Url.RESOURCE, TAG);
		url.setParameters(new String[] { type, "offset", "limit" }, 
				new String[] { location.getId(), offset + "", limit + ""}
		);
		
		return getAnnotationsList(context, url.toString(), location);
	}
	
	public static List<Annotation> getAnnotations(Context context, 
			Location location, String category) throws Exception {
		String type = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		Url url = new Url(Url.RESOURCE, TAG);
		url.setParameters(new String[] { type, "category" }, 
				new String[] { location.getId(), category }
		);
		
		return getAnnotationsList(context, url.toString(), location);
	}
	
	
	public static List<Annotation> getAnnotations(Context context, 
			Location location, String category, int offset, int limit) throws Exception {
		String type = (location.isEnvironment()) ? Location.ENVIRONMENT : Location.AREA;
		Url url = new Url(Url.RESOURCE, TAG);
		url.setParameters(new String[] { type, "category", "offset", "limit"}, 
				new String[] { location.getId(), category, offset + "", limit + "" }
		);
		
		return getAnnotationsList(context, url.toString(), location);
	}
	
	
	public static List<Annotation> getAnnotations(Context context, 
			Location location, String category, Map<String, String> extra) throws Exception {
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
		
		return getAnnotationsList(context, url.toString(), location);
	}
	
	
	public static List<Annotation> getAnnotations(Context context, 
			Location location, String category, Map<String, String> extra, int offset, int limit) throws Exception {
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
		
		return getAnnotationsList(context, url.toString(), location);
	}
	
	// this can be re-written in terms of getAnnotations + an extra
	public static List<Annotation> getAllAnnotationsForEnvironment(Context context, 
			String envId, String category) throws Exception {
		
		Url url = new Url(Url.RESOURCE, TAG);
		url.setParameters(
				new String[] { Location.ENVIRONMENT, "all", "category", "order_by" }, 
				new String[] { envId, "true", category, "-timestamp" }
		);
		
		return getAnnotationsList(context, url.toString());
	}
	
	private static List<Annotation> getAnnotationsList(Context context, String url) throws Exception {
		return getAnnotationsList(context, url, null);
	}
	
	private static List<Annotation> getAnnotationsList(Context context, 
			String url, Location location) throws Exception {
		AppClient client = new AppClient(context);
		HttpResponse response = client.makeGetRequest(url);
		
		// Check the status code
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			throw new Exception(Integer.toString(HttpStatus.SC_SERVICE_UNAVAILABLE));
		}
		
		// If SC_OK, parse response
		String responseData = EntityUtils.toString(response.getEntity());
		
		return parse(context, responseData, location);
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
				location = new Location(locationName, locationUri);
			}
			
			String data = annotation.getString("data");
			annotations.add(new Annotation(context, location, category, timestamp, data, uri, userUri));
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
}
