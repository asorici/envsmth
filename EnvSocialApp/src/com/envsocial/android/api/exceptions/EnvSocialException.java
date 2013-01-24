package com.envsocial.android.api.exceptions;

public abstract class EnvSocialException extends Exception {
	private static final long serialVersionUID = 1L;

	protected EnvSocialException(Throwable cause) {
		if (cause != null) {
			initCause(cause);
		}
	}
	
	@Override
	public abstract String getMessage();
}
