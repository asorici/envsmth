package com.envsocial.android;

import android.app.Application;
import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;

import com.envsocial.android.utils.FeatureLRUTracker;
import com.envsocial.android.utils.LocationHistory;
import com.envsocial.android.utils.imagemanager.ImageCache.ImageCacheParams;
import com.envsocial.android.utils.imagemanager.ImageFetcher;


public class Envived extends Application {
	// fields that are globally accessible within the application
	private static Context context;
	private static FeatureLRUTracker featureLRUTracker;
	private static LocationHistory locationHistory;

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
    
    public static LocationHistory getLocationHistory() {
    	return Envived.locationHistory;
    }
    
    public static void setLocationHistory(LocationHistory locationHistory) {
    	Envived.locationHistory = locationHistory;
    }
    
    public static ImageFetcher getImageFetcherInstance(FragmentManager fragmentManager, 
    		ImageCacheParams cacheParams, int loadingImageDrawable) {
    	
    	// Fetch screen height and width, to use as our max size when loading images as this
        // activity runs full screen
		final DisplayMetrics displayMetrics = Envived.context.getResources().getDisplayMetrics();
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;
        final int longest = (height > width ? height : width);
        
        // The ImageFetcher takes care of loading images into ImageViews asynchronously
        ImageFetcher imageFetcher = new ImageFetcher(Envived.context, longest);
        
        if (cacheParams != null) {
        	imageFetcher.addImageCache(fragmentManager, cacheParams);
        }
        
        imageFetcher.setLoadingImage(loadingImageDrawable);
        imageFetcher.setImageFadeIn(false);
        
        return imageFetcher;
    }
}
