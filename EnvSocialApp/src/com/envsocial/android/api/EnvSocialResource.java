package com.envsocial.android.api;

public enum EnvSocialResource {
	USER("user"), ENVIRONMENT("environment"), AREA("area"), FEATURE("feature"), 
	ANNOTATION("annotation"), ANNOUNCEMENT("announcement"), 
	HISTORY("history"), PRIVACY("privacy"),
	REGISTER("register"), LOGIN("login"), LOGOUT("logout"), 
	CHECKIN("checkin"), CHECKOUT("checkout");
	
	private String name;
	
	private EnvSocialResource(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
