package com.envsocial.android.features.program;

public interface ProgramUpdateObserver {
	public void registerListener(ProgramUpdateListener l);
	public void unregisterListener(ProgramUpdateListener l);
}
