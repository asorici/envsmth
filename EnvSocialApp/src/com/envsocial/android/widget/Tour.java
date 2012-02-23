package com.envsocial.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Gallery;

public class Tour extends Gallery {

	public Tour(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public Tour(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public Tour(Context context) {
		super(context);
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, 
			float velocityX, float velocityY) {

		if (velocityX > 0) {
			onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, null);
		} else {
			onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, null);
		}
		
		return false;
	}
}
