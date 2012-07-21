package com.envsocial.android.api.exceptions;

public abstract class EnvSocialException extends Exception {
	
	protected EnvSocialException(Throwable cause) {
		if (cause != null) {
			initCause(cause);
		}
	}
	
	@Override
	public abstract String getMessage();
}
