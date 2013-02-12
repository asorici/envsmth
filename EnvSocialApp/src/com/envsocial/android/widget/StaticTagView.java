package com.envsocial.android.widget;

import org.codeandmagic.android.TagView;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;

public class StaticTagView extends TagView {

	public StaticTagView(Context context) {
		this(context, null, 0);
	}
	
	public StaticTagView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
    }
	
    public StaticTagView(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
    	
    	setInputType(InputType.TYPE_NULL);
    	setFocusableInTouchMode(false);
    	setFocusable(false);
    	setClickable(false);
    }
    
    @Override
	public void onClick(View v) {}
}
