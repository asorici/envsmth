package com.envsocial.android.api;

public enum EnvSocialResource {
	USER("user"), ENVIRONMENT("environment"), AREA("area"), FEATURE("feature"), 
	ANNOTATION("annotation"), ANNOUNCEMENT("announcement"), 
	HISTORY("history"), PRIVACY("privacy"), ENVIRONMENTCONTEXT("environmentcontext"),
	REGISTER("register"), LOGIN("login"), LOGOUT("logout"), 
	CHECKIN("checkin"), CHECKOUT("checkout"),
	CREATE_ANONYMOUS("create_anonymous"), DELETE_ANONYMOUS("delete_anonymous"),
	GCM("cloud_messaging");
	
	private String name;
	
	private EnvSocialResource(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
