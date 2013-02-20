package com.envsocial.android.features.description;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.View;
import android.widget.TextView;

public class BoothDescriptionProductListAdapter extends SimpleCursorAdapter implements ViewBinder {
	private static final int SHORT_DESC_NUM_WORDS = 12;
	
	public BoothDescriptionProductListAdapter(Context context, int layout, Cursor c, 
			String[] from, int[] to, int flags) {
		
		super(context, layout, c, from, to, flags);
		setViewBinder(this);
	}
	
	
	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		// we only make a difference in the description text, where we want to just
		// take the first 12 words and show them
		if (view.getId() == com.envsocial.android.R.id.description_booth_product_description_short) {
			TextView shortDescView = (TextView) view;
			
			String text = cursor.getString(columnIndex);
			String[] textArray = text.split(" ");
			
			if (textArray.length <= SHORT_DESC_NUM_WORDS) {
				shortDescView.setText(text);
			}
			else {
				text = "";
				String separator = " ";
				for (int i = 0; i < SHORT_DESC_NUM_WORDS; i++) {
					text += textArray[i] + separator;
				}
				
				text += "...";
				
				shortDescView.setText(text);
			}
			
			return true;
		}
		
		// the other views will be handled by the implementation of the simple cursor adapter which
		// we are extending
		return false;
	}
	
}
