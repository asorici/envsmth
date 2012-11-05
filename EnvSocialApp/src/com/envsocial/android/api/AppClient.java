package com.envsocial.android.api;

import java.io.IOException;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.util.Log;

import com.envsocial.android.utils.Preferences;

public class AppClient {
	
	public static final String SESSIONID = "sessionid";
	
	private DefaultHttpClient mHttpClient;
	private Context mContext;
	
	public AppClient(Context context) {
		 mHttpClient = new DefaultHttpClient();
		 mContext = context;
	}
	
	/** Make a post request given payload as list of name-value pairs */
	public HttpResponse makePostRequest(String url, List<NameValuePair> data, 
			String[] headerName, String[] headerValue) throws IOException {
		
		HttpPost postRequest = new HttpPost(url);
		postRequest = (HttpPost) addHeaders(postRequest, headerName, headerValue);
		postRequest.setEntity(new UrlEncodedFormEntity(data));
		
		return makeRequest(postRequest);
	}
	
	/** Make a post request given payload as JSON string */
	public HttpResponse makePostRequest(String url, String data, 
			String[] headerName, String[] headerValue) throws IOException {
		
		HttpPost postRequest = new HttpPost(url);
		postRequest = (HttpPost) addHeaders(postRequest, headerName, headerValue);
		postRequest.setEntity(new StringEntity(data));
		
		return makeRequest(postRequest);
	}
	
	/** Make a get request, no headers */
	public HttpResponse makeGetRequest(String url) throws IOException {
		HttpGet getRequest = new HttpGet(url);
		return makeRequest(getRequest);
	}
	
	/** Make a put request given payload as JSON string */
	public HttpResponse makePutRequest(String url, String data, 
			String[] headerName, String[] headerValue) throws IOException {
		
		HttpPut putRequest = new HttpPut(url);
		putRequest = (HttpPut) addHeaders(putRequest, headerName, headerValue);
		putRequest.setEntity(new StringEntity(data));
		
		return makeRequest(putRequest);
	}
	
	public HttpResponse makeDeleteRequest(String url) throws IOException {
		HttpDelete delRequest = new HttpDelete(url);
		return makeRequest(delRequest);
	}

	
	private HttpResponse makeRequest(HttpUriRequest r) throws IOException {
		// Check if we have a session
		String sessionCookie = Preferences.getStringPreference(mContext, SESSIONID);
		if (sessionCookie != null) {
			r.setHeader("Cookie", sessionCookie);
		}
		
		HttpResponse response = mHttpClient.execute(r);
		
		// Check if we received any cookies
		Header[] headers = response.getHeaders("Set-Cookie");
		if (headers.length == 0) {
			return response;
		}
		
		// Search for session id cookie
		for (Header h : headers) {
			if (h.getValue().indexOf(SESSIONID)>=0) {
				String value = h.getValue();
				sessionCookie = value.split(";")[0];
				Preferences.setStringPreference(mContext, SESSIONID, sessionCookie);
			}
		}
		
		return response;
	}
	
	private HttpUriRequest addHeaders(HttpUriRequest request, String[] headerName, String[] headerValue) {
		// Check if we have any specific headers to add
		if (headerName != null) {
			int len = headerName.length;
			for (int i = 0; i < len; ++ i) {
				request.setHeader(headerName[i], headerValue[i]);
			}
		}
		
		return request;
	}
}
