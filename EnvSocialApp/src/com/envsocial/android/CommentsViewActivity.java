package com.envsocial.android;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.api.Location;

public class CommentsViewActivity extends SherlockFragmentActivity  {
	private static final String TAG = "CommentsViewActivity";
	private static boolean active;
	
	private Location mLocation;
	
	private ActionBar mActionBar;
	
	private LinearLayout mMainView;
	
	@Override	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//setContentView(R.layout.comments);
		
		mActionBar = getSupportActionBar();
		
		mLocation = (Location)getIntent().getSerializableExtra("location");
		if (mLocation != null) {
			mActionBar.setTitle(mLocation.getName());
		} 		
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		Log.d(TAG, " --- onPause called in CommentsViewActivity");
		active = false;		
	}
	
	@Override
	public void onStop() {
		Log.d(TAG, " --- onStop called in CommentsViewActivity");
		super.onStop();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Log.d(TAG, " --- onResume called in CommentsViewActivity");
		active = true;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		Log.d(TAG, " --- onStart called in CommentsViewActivity");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		Log.d(TAG, " --- onDestroy called in CommentsViewActivity");
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		// add the comment button
		MenuItem filterItem = menu.add(getText(R.string.menu_comment));
		filterItem.setTitle("FILTER");
		filterItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
     	
    	return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		final Context context = getApplicationContext();
		
		if (item.getTitle().toString().compareTo(getString(R.string.menu_filter)) == 0) {
			
			return true;
		}
		
		return false;
	}
}
