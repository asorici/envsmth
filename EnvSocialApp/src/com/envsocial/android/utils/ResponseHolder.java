package com.envsocial.android.utils;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class ResponseHolder {
	
	int mCode;
	JSONObject mData;
	Object mTag;
	
	public ResponseHolder(int code, JSONObject data, Object tag) {
		mCode = code;
		mData = data;
		mTag = tag;
	}
	
	public ResponseHolder(HttpResponse response) throws Exception {
		String responseBody = EntityUtils.toString(response.getEntity());
		JSONObject jsonObject = new JSONObject(responseBody);
		mCode = jsonObject.getInt("code");
		mData = jsonObject.getJSONObject("data");
		mTag = null;
	}
	
	public int getCode() {
		return mCode;
	}
	
	public JSONObject getData() {
		return mData;
	}
	
	public void setTag(Object tag) {
		mTag = tag;
	}
	
	public Object getTag() {
		return mTag;
	}
	
	
	public int getInt(String key) throws JSONException {
		return mData.getInt(key);
	}
	
	public long getLong(String key) throws JSONException {
		return mData.getLong(key);
	}
	
	public boolean getBoolean(String key) throws JSONException {
		return mData.getBoolean(key);
	}
	
	public String getString(String key) throws JSONException {
		return mData.getString(key);
	}
	
	public Object get(String key) throws JSONException {
		return mData.get(key);
	}
}