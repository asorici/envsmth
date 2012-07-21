package com.envsocial.android.api.exceptions;

import com.envsocial.android.api.EnvSocialResource;

public class EnvSocialCom401 extends EnvSocialComException {
	public EnvSocialCom401(String userUri, HttpMethod method, EnvSocialResource resource, Throwable cause) {
		super(userUri, method, resource, cause);
		this.prologErrorMessage = "Unauthorized Access";
	}
}
