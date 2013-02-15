package com.envsocial.android.features.program;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragment;


public abstract class ProgramFragment extends SherlockFragment implements ProgramUpdateListener {
	protected static final int TIME_DISPLAY_TYPE = 0;
	protected static final int SESSION_DISPLAY_TYPE = 1; 
	
	protected int mProgramDisplayType = TIME_DISPLAY_TYPE;
	protected ProgramUpdateListener mProgramUpdateListener;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setProgramUpdateListener(this);
	    ((ProgramUpdateObserver)getActivity()).registerListener(mProgramUpdateListener);
	}
	
	
	@Override
	public void onDestroy() {
	    super.onDestroy();
	    
		((ProgramUpdateObserver)getActivity()).unregisterListener(mProgramUpdateListener);
	}
	
	
	protected void setProgramDisplayType(int displayType) {
		mProgramDisplayType = displayType;
	}
	
	protected int getProgramDisplayType(int displayType) {
		return mProgramDisplayType;
	}
	
	public void setProgramUpdateListener(ProgramUpdateListener l) {
		mProgramUpdateListener = l;
	}
	
	public void getProgramUpdateListener(ProgramUpdateListener l) {
		mProgramUpdateListener = l;
	}
	
	
	@Override
	public void onProgramUpdated(ProgramFeature updatedProgramFeature) {
		handleProgramUpdate(updatedProgramFeature);
	}
	
	protected abstract void handleProgramUpdate(ProgramFeature updatedProgramFeature);
}
