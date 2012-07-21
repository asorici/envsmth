package com.envsocial.android.api;

public class Url {

	public static final String HTTP = "http://";
	public static final String HTTPS = "https://";
	//public static final String HOSTNAME = "192.168.1.6:8000";
	//public static final String HOSTNAME = "192.168.100.108:8000";
	public static final String HOSTNAME = "192.168.1.105:8000";
	//public static final String HOSTNAME = "141.85.227.108";
	//public static final String HOSTNAME = "192.168.8.55:8000";
	//public static final String HOSTNAME = "172.16.2.181:8000";
	
	private static final String BASE_URL = "/envsocial/client/v1/";
	private static final String ACTION_RELATIVE_URL = BASE_URL + "actions/";
	private static final String RESOURCE_RELATIVE_URL = BASE_URL + "resources/";
	
	public static final int ACTION = 0;
	public static final int RESOURCE = 1;
	
	private String mProtocol;
	private int mType;
	private String mItem;
	private String mId;
	private boolean mAbsolute;
	
	private String[] mParams;
	private String[] mValues;
	
	
	public Url(int type, String item) {
		this(type, item, true, HTTP);
	}
	
	public Url(int type, String item, boolean absoluteUrl, String protocol) {
		mProtocol = protocol;
		mType = type;
		mItem = item;
		mAbsolute = absoluteUrl;
		mId = null;
	}
	
	public void setItemId(String id) {
		mId = id;
	}
	
	public void setParameters(String[] params, String[] values) {
		mParams = params;
		mValues = values;
	}
	
	@Override
	public String toString() {
		StringBuilder url = new StringBuilder();
		// Check if url is absolute
		if (mAbsolute) {
			url.append(mProtocol);
			url.append(HOSTNAME);
		}
		// Add relative url
		if (mType == ACTION) {
			url.append(ACTION_RELATIVE_URL);
		} else {
			url.append(RESOURCE_RELATIVE_URL);
		}
		// Add item and id if any
		if (mItem != null) {
			appendPathElement(url, mItem);
			if (mId != null) {
				appendPathElement(url, mId);
			}
		}
		// Add parameters
		appendParams(url);
		
		return url.toString();
	}
	
	private void appendPathElement(StringBuilder url, String elem) {
		url.append(elem + "/");
	}
	
	private void appendParams(StringBuilder url) {
		if (mParams == null || mValues == null) {
			return;
		}
		url.append("?");
		int len = mParams.length;
		int i;
		for (i = 0; i < len-1; ++ i) {
			url.append(mParams[i] + "=" + mValues[i] + "&");
		}
		url.append(mParams[i] + "=" + mValues[i]);
	}
	
	
	public static String fromUri(String uri) {
		return HTTP + HOSTNAME + uri;
	}
	
	public static String actionUrl(String action) {
		return HTTP + HOSTNAME + ACTION_RELATIVE_URL + action + "/";
	}
	
	public static String resourceUrl(String resource) {
		return HTTP + HOSTNAME + RESOURCE_RELATIVE_URL + resource + "/";
	}
	
	public static String appendParameter(String uri, String param, String value) {
		return uri + "&" + param + "=" + value;
	}
	
	public static String resourceIdFromUri(String uri) {
		try {
			// first remove last character (the slash) from the uri
			uri = uri.substring(0, uri.length() - 1);
			
			int slashIndex = uri.lastIndexOf('/');
			
			// now get the remaining string after the last slash - that will be the identifier
			String identifier = uri.substring(slashIndex + 1, uri.length());
			
			return identifier;
		} catch (Exception ex) {
			return null;
		}
	}
}
