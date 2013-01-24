package com.envsocial.android;

import android.app.Application;
import android.content.Context;

import com.envsocial.android.utils.FeatureLRUTracker;
import com.envsocial.android.utils.Preferences;


public class Envived extends Application {
	// fields that are globally accessible within the application
	private static Context context;
	private static FeatureLRUTracker featureLRUTracker;


	public void onCreate() {
        super.onCreate();
        Envived.context = getApplicationContext();
    }

    public static Context getContext() {
        return Envived.context;
    }
    
    public static FeatureLRUTracker getFeatureLRUTracker() {
    	return Envived.featureLRUTracker;
    }
    
    public static void setFeatureLRUTracker(FeatureLRUTracker featureLRUTracker) {
    	Envived.featureLRUTracker = featureLRUTracker;
    }
}
