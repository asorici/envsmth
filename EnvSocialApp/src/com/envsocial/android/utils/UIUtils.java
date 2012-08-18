package com.envsocial.android.utils;

import android.content.Context;

public class UIUtils {
	
	public static int getDipsFromPixel(float pixels, Context context) {
	     // Get the screen's density scale
	     final float scale = context.getResources().getDisplayMetrics().density;
	     
	     // Convert the dps to pixels, based on density scale
	     return (int) (pixels * scale + 0.5f);
	}
	
}
