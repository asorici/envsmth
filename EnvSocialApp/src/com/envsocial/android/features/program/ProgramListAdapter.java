package com.envsocial.android.features.program;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class ProgramListAdapter extends BaseAdapter {

	private Context mContext;
	private ProgramDbHelper mProgramDb;
	
	public ProgramListAdapter(Context context, ProgramDbHelper programDb) {
		mContext = context;
		mProgramDb = programDb;
		List<String> days = programDb.getDays();
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int arg0, View arg1, ViewGroup arg2) {
		// TODO Auto-generated method stub
		return null;
	}

}
