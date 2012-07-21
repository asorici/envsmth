package com.envsocial.android.api.exceptions;

import com.envsocial.android.api.EnvSocialResource;

public class EnvSocialCom405 extends EnvSocialComException {
	public EnvSocialCom405(String userUri, HttpMethod method, EnvSocialResource resource, Throwable cause) {
		super(userUri, method, resource, cause);
		this.prologErrorMessage = "Communication Method Not Allowed";
	}
}
