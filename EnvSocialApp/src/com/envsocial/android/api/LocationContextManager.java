package com.envsocial.android.api;

import java.io.IOException;
import java.io.Serializable;

import org.apache.http.HttpResponse;

import android.content.Context;

import com.envsocial.android.api.exceptions.EnvSocialComException;
import com.envsocial.android.api.exceptions.EnvSocialComException.HttpMethod;
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.ResponseHolder;


public class LocationContextManager implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private static final String TAG = "LocationContextManager";
	private static final String USER_COUNT_REQUEST = "peoplecount";
	
	public static final String CONTEXT_RESOURCE = "environmentcontext";
	
	private Location mLocation;
	private String mResourceUri;
	
	private void extractResourceUri() {
		String environmentId = null;
		if (mLocation.isEnvironment()) {
			environmentId = mLocation.getId();
		}
		else {
			// we have an area - so get the parent environment and then get its id 
			environmentId = Url.resourceIdFromUrl(mLocation.getParentUrl());
		}
		
		Url url = new Url(Url.RESOURCE, CONTEXT_RESOURCE);
		url.setItemId(environmentId);
		mResourceUri = url.toString();
	}
	
	public LocationContextManager(Location location) {
		mLocation = location;
		extractResourceUri();
	}

	public Location getLocation() {
		return mLocation;
	}

	public String getResourceUri() {
		return mResourceUri;
	}
	
	// ================================ the different possible context requests ================================= //
	
	public ResponseHolder getUserCount(Context context) {
		String userUri = Preferences.getUserUri(context);
		AppClient client = new AppClient(context);
		
		String requestUri = Url.appendOrReplaceParameter(mResourceUri, "request", USER_COUNT_REQUEST);
		HttpResponse response = null;
		
		try {
			response = client.makeGetRequest(requestUri);
		} catch (IOException e) {
			return new ResponseHolder(new EnvSocialComException(userUri, HttpMethod.GET, 
					EnvSocialResource.ENVIRONMENTCONTEXT, e));
		}
		
		return ResponseHolder.parseResponse(response);
	}
}
