package com.envsocial.android.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.envsocial.android.api.exceptions.EnvSocialException;

public class ResponseHolder {
	
	private int mCode;
	private JSONObject mJsonContent;
	private String mResponseBody;
	private Object mTag;
	private Exception error;
	
	public ResponseHolder(Exception error) {
		this.error = error;
	}
	
	public ResponseHolder(int code, String responseBody, Object tag) {
		mCode = code;
		mResponseBody = responseBody;
		mTag = tag;
	}
	
	public Exception getError() {
		return error;
	}
	
	public void setError(EnvSocialException error) {
		this.error = error;
	}
	
	public boolean hasError() {
		if (error != null) {
			return true;
		}
		
		return false;
	}
	
	public int getCode() {
		return mCode;
	}
	
	public JSONObject getJsonContent() throws JSONException {
		if (mJsonContent == null) {
			mJsonContent = new JSONObject(mResponseBody);
		}
		
		return mJsonContent;
	}
	
	public String getResponseBody() {
		return mResponseBody;
	}
	
	public void setTag(Object tag) {
		mTag = tag;
	}
	
	public Object getTag() {
		return mTag;
	}
	
	public static Map<String, Object> getActionErrorMessages(JSONObject errorJson) throws JSONException {
		Map<String, Object> errorDict = new HashMap<String, Object>();
		List<String> errorList = new ArrayList<String>();
		
		JSONObject errorData = errorJson.getJSONObject("data");
		errorDict.put("msg", errorData.getString("msg"));
		
		Iterator errIt = errorData.keys();
		while(errIt.hasNext()) {
			String key = (String)errIt.next();
			if (!"msg".equals(key)) {
				JSONArray errors = errorData.getJSONArray(key);
				for (int i = 0; i < errors.length(); i++) {
					String errMessage = errors.getString(i);
					
					if (!"__all__".equalsIgnoreCase(key)) {
						errorList.add(key.toUpperCase() + ": " + errMessage);
					}
					else {
						errorList.add(errMessage);
					}
				}
			}
		}
		
		errorDict.put("errors", errorList);
		
		return errorDict;
	}
	
	public static ResponseHolder parseResponse(HttpResponse response) {
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			String responseBody = EntityUtils.toString(response.getEntity());
			
			return new ResponseHolder(statusCode, responseBody, null);
		} catch (ParseException e) {
			return new ResponseHolder(e);
		} catch (IOException e) {
			return new ResponseHolder(e);
		} catch (Exception e) {
			return new ResponseHolder(e);
		}
	}
	
}