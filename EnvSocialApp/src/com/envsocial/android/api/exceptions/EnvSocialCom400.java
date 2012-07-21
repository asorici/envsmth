package com.envsocial.android.api.exceptions;

import com.envsocial.android.api.EnvSocialResource;

public class EnvSocialCom400 extends EnvSocialComException {
	public EnvSocialCom400(String userUri, HttpMethod method, EnvSocialResource resource, Throwable cause) {
		super(userUri, method, resource, cause);
		this.prologErrorMessage = "Bad Content Message or URI params";
	}
}
