package com.envsocial.android.api.exceptions;

import com.envsocial.android.api.EnvSocialResource;

public class EnvSocialContentException extends EnvSocialException {
	protected String mJsonContent;
	protected EnvSocialResource mResource;
	
	public EnvSocialContentException(String jsonContent, EnvSocialResource resource, Throwable cause) {
		super(cause);
		
		mJsonContent = jsonContent;
		mResource = resource;
	}
	
	public String getContent() {
		return mJsonContent;
	}

	@Override
	public String getMessage() {
		String info = "Error parsing content :: " + mJsonContent + " for resource :: " + mResource.getName();
		return info;
	}

}
