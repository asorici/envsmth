package com.envsocial.android.features.program;

import com.envsocial.android.features.order.OrderDbHelper;

import android.content.Context;
import android.database.Cursor;
import android.widget.SimpleCursorTreeAdapter;

public class ProgramBySessionAdapter extends SimpleCursorTreeAdapter {
	
	private ProgramFeature mProgramFeature;
	private String mSelectedDayString;
	
	public ProgramBySessionAdapter(ProgramFeature programFeature, String selectedDayString, 
			Context context, Cursor cursor,
			int groupLayout, String[] groupFrom, int[] groupTo,
			int childLayout, String[] childFrom, int[] childTo) {
		
		super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childFrom, childTo);
		
		mSelectedDayString = selectedDayString;
		mProgramFeature = programFeature;
	}

	@Override
	protected Cursor getChildrenCursor(Cursor groupCursor) {
		int sessionIdColumnIdx = groupCursor.getColumnIndex(ProgramDbHelper.COL_SESSION_ID);
		int sessionId = groupCursor.getInt(sessionIdColumnIdx);
		
		return mProgramFeature.getPresentationsByDay(mSelectedDayString, sessionId);
	}
	
	
	public void setSelectedDayString(String selectedDayString) {
		mSelectedDayString = selectedDayString;
		notifyDataSetChanged();
	}
	
	
	@Override
	public boolean isChildSelectable(int groupPosition, int  childPosition) {
		return true;
	}
}
