package com.envsocial.android;

import android.app.Application;
import android.content.Context;

public class Envived extends Application {
	private static Context context;

	public void onCreate(){
        super.onCreate();
        Envived.context = getApplicationContext();
    }

    public static Context getContext() {
        return Envived.context;
    }
}
