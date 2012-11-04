package com.envsocial.android.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.envsocial.android.GCMIntentService;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class Utils {
	
	public static long DAY_SCALE = 86400000;
	public static long HOUR_SCALE = 1440000;
	public static long MINUTE_SCALE = 60000;
	
	public static String calendarToString(Calendar c, String formatPattern) {
		// default is "yyyy-MM-dd HH:mm:ss"
		if (formatPattern == null) {
			formatPattern = "yyyy-MM-dd HH:mm:ss";
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat(formatPattern);
		return sdf.format(c.getTime());
	}
	
	public static Calendar stringToCalendar(String timestamp, String formatPattern) throws ParseException {
		// default is "yyyy-MM-dd'T'HH:mm:ssZ"
		if (formatPattern == null) {
			formatPattern = "yyyy-MM-dd'T'HH:mm:ssZ";
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat(formatPattern);
		Calendar c = Calendar.getInstance();
		c.setTime(sdf.parse(timestamp));
		
		return c;
	}
	
	public static void sendGCMStatusMessage(Context context, String message) {
		Intent intent = new Intent(GCMIntentService.ACTION_DISPLAY_GCM_MESSAGE);
        intent.putExtra(GCMIntentService.EXTRA_GCM_MESSAGE, message);
        context.sendBroadcast(intent);
	}
	
	
	// ============================== Version utilities ======================================= //
	
	/*
    @TargetApi(11)
    public static void enableStrictMode() {
        if (Utils.hasGingerbread()) {
            StrictMode.ThreadPolicy.Builder threadPolicyBuilder =
                    new StrictMode.ThreadPolicy.Builder()
                            .detectAll()
                            .penaltyLog();
            StrictMode.VmPolicy.Builder vmPolicyBuilder =
                    new StrictMode.VmPolicy.Builder()
                            .detectAll()
                            .penaltyLog();

            if (Utils.hasHoneycomb()) {
                threadPolicyBuilder.penaltyFlashScreen();
                vmPolicyBuilder
                        .setClassInstanceLimit(ImageGridActivity.class, 1)
                        .setClassInstanceLimit(ImageDetailActivity.class, 1);
            }
            StrictMode.setThreadPolicy(threadPolicyBuilder.build());
            StrictMode.setVmPolicy(vmPolicyBuilder.build());
        }
    }
	*/
    
    public static boolean hasFroyo() {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasJellyBean() {
        //return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    	return false;
    }
}
