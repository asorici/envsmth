package com.envsocial.android.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

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
}
