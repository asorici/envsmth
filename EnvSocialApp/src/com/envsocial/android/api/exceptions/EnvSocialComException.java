package com.envsocial.android.api.exceptions;

import org.apache.http.HttpStatus;

import com.envsocial.android.api.EnvSocialResource;


public class EnvSocialComException extends EnvSocialException {
	private static final long serialVersionUID = -7015287749039137733L;

	public static enum HttpMethod {GET, POST, PUT, DELETE}; 
	
	protected HttpMethod communicationMethod;
	protected EnvSocialResource communicationResource;
	protected String userUri;
	
	protected String prologErrorMessage = "Error";
	protected String responseContent;
	
	public EnvSocialComException(String userUri, HttpMethod method, EnvSocialResource resource, Throwable cause) {
		super(cause);
		
		this.communicationMethod = method;
		this.communicationResource = resource;
		this.userUri = userUri;
	}
	
	@Override
	public String getMessage() {
		String user = "ANONYMOUS";
		if (userUri != null) {
			user = userUri;
		}
		
		String info = prologErrorMessage + " DOING " + communicationMethod.name() + " request FOR EnvSocialResource::" + 
						communicationResource.getName() + " BY user::" + user;
		
		return info;
	}

	public String getResponseContent() {
		return responseContent;
	}

	public void setResponseContent(String responseContent) {
		this.responseContent = responseContent;
	}
	
	/**
	 * Convenience factory method to return an EnvSocialComException given a status code
	 * @param statusCode
	 * @param commMethod
	 * @param commResource
	 * @param userUri
	 * @return The EnvSocialComException corresponding to the encountered status code
	 */
	public static EnvSocialComException newInstanceFrom(int statusCode, 
			String userUri, 
			HttpMethod commMethod, 
			EnvSocialResource commResource,
			Throwable cause) {
		
		switch(statusCode) {
			case HttpStatus.SC_BAD_REQUEST:
				return new EnvSocialCom400(userUri, commMethod, commResource, cause);
			case HttpStatus.SC_UNAUTHORIZED:
				return new EnvSocialCom401(userUri, commMethod, commResource, cause);
			case HttpStatus.SC_METHOD_NOT_ALLOWED:
				return new EnvSocialCom405(userUri, commMethod, commResource, cause);
			default:
				return new EnvSocialComException(userUri, commMethod, commResource, cause);
		}
	}
}
