package com.envsocial.android.api;

public class Url {

	public static final String HTTP = "http://";
	public static final String HTTPS = "https://";
	public static final String HOSTNAME = "envived.com:8800";
	//public static final String HOSTNAME = "192.168.1.6:8000";
	//public static final String HOSTNAME = "192.168.100.102:8080";
	//public static final String HOSTNAME = "192.168.1.106:8000";
	//public static final String HOSTNAME = "192.168.1.107:8080";
	//public static final String HOSTNAME = "141.85.227.108";
	//public static final String HOSTNAME = "192.168.8.55:8000";
	//public static final String HOSTNAME = "172.16.2.181:8000";
	
	private static final String BASE_URL = "/envived/envsocial/client/v1/";
	//private static final String BASE_URL = "/envsocial/client/v1/";
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
		// if no parameters return
		if (mParams == null || mValues == null) {
			return;
		}
		
		int len = mParams.length;
		
		if (url.indexOf("?") >= 0) {
			// if a first parameter already exists just append the rest
			for (int i = 0; i < len; i++) {
				url.append("&" + mParams[i] + "=" + mValues[i]);
			}
		}
		else {
			url.append("?");
			for (int i = 0; i < len - 1; i++) {
				url.append(mParams[i] + "=" + mValues[i] + "&");
			}
			url.append(mParams[len - 1] + "=" + mValues[len - 1]);
		}
	}
	
	
	public static String fromRelativeUrl(String uri) {
		return HTTP + HOSTNAME + uri;
	}
	
	public static String actionUrl(String action) {
		return HTTP + HOSTNAME + ACTION_RELATIVE_URL + action + "/";
	} 
	
	public static String resourceUrl(String resource) {
		return HTTP + HOSTNAME + RESOURCE_RELATIVE_URL + resource + "/";
	}
	
	static String signUrl(String url) {
		//TODO: proper url signing
		return Url.appendOrReplaceParameter(url, "clientrequest", "true");
	}
	
	
	public static String appendOrReplaceParameter(String url, String param, String value) {
		if (url.contains("?")) {
			// url has parameters - so check if param is among them
			int paramIndex = url.indexOf(param);
			if (paramIndex == -1) {
				// param is not included in url, so append it
				return url + "&" + param + "=" + value;
			}
			else {
				// the value to replace is between the first `=' and `&' chars after 
				// the occurence of parameter
				int index1 = paramIndex + param.length();	// is the occurrence of =
				int index2 = url.indexOf('&', index1 + 1);
				
				if (index2 == -1) {
					// we want to replace the last parameter
					// get the substring up to the `=' sign and append the new value
					return url.substring(0, index1 + 1) + value;
				}
				else {
					// get the prefix and suffix substrings and put the new
					// value in the middle
					String urlPrefix = url.substring(0, index1 + 1);
					String urlSuffix = url.substring(index2);
					return urlPrefix + value + urlSuffix;
				}
			}
		}
		else {
			// url contains no parameters yet so append param as first one
			return url + "?" + param + "=" + value;
		}
	}
	
	
	public static String appendParameter(String url, String param, String value) {
		if (url.contains("?")) {
			// url has parameters - so append param to them
			return url + "&" + param + "=" + value;
		}
		else {
			// url contains no parameters yet so append param as first one
			return url + "?" + param + "=" + value;
		}
	}
	
	
	public static String resourceIdFromUrl(String url) {
		try {
			// first remove last character (the slash) from the uri
			url = url.substring(0, url.length() - 1);
			
			int slashIndex = url.lastIndexOf('/');
			
			// now get the remaining string after the last slash - that will be the identifier
			String identifier = url.substring(slashIndex + 1, url.length());
			
			return identifier;
		} catch (Exception ex) {
			return null;
		}
	}
}
