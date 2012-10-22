package com.envsocial.android.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.envsocial.android.GCMIntentService;

import android.content.Context;
import android.content.Intent;

public class Utils {
	
	public static String calendarToString(Calendar c, String formatPattern) {
		// default is "yyyy-MM-dd HH:mm:ss"
		if (formatPattern == null) {
			formatPattern = "yyyy-MM-dd HH:mm:ss";
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat(formatPattern);
		return sdf.format(c.getTime());
	}
	
	public static Calendar stringToCalendar(String timestamp, String formatPattern) throws ParseException {
		// default is "yyyy-MM-dd HH:mm:ss"
		if (formatPattern == null) {
			formatPattern = "yyyy-MM-dd HH:mm:ss";
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
}
