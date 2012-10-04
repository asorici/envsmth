package com.envsocial.android;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;
import com.envsocial.android.fragment.SearchResultFragmentFactory;
import com.envsocial.android.utils.Preferences;

public class DetailsSearchableActivity extends SherlockFragmentActivity implements OnClickListener {
	private static final String TAG = "DetailsSearchableActivity";
	private static final String SEARCH_FRAGMENT_TAG = "search_fragment_tag";
	private static final int SEARCH_FRAGMENT_ID = 1;
	
	private String mFeatureCategoryTag;
	private Fragment mSearchFragment;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.feature_search);
	    
	    handleIntent(getIntent());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// add the search button
		MenuItem item = menu.add(R.string.menu_search);
        item.setIcon(R.drawable.ic_menu_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
    	return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle().toString().compareTo(getString(R.string.menu_search)) == 0) {
			return onSearchRequested();
		}
		
		return true;
	}
	
	@Override
	public boolean onSearchRequested() {
		// first check to see which fragment initiated the search
		if (mFeatureCategoryTag != null) {
			Bundle appData = new Bundle();
		    appData.putString(Feature.SEARCH_FEATURE, mFeatureCategoryTag);
			
		    startSearch(null, false, appData, false);
		    return true;
		}
		
		return false;
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
	    setIntent(intent);
	    handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      String query = intent.getStringExtra(SearchManager.QUERY);
	      
	      Bundle appData = getIntent().getBundleExtra(SearchManager.APP_DATA);
	      if (appData != null) {
	    	  mFeatureCategoryTag = appData.getString(Feature.SEARCH_FEATURE);
	    	  Log.i(TAG, "Search from feature: " + mFeatureCategoryTag);
	      }
	      
	      // get the corresponding fragment
	      mSearchFragment = SearchResultFragmentFactory.newInstance(mFeatureCategoryTag, query);
	      
	      // set the query
	      Bundle queryBundle = new Bundle();
	      queryBundle.putString("query", query);
	      mSearchFragment.setArguments(queryBundle);
	      
	      FragmentManager fm = getSupportFragmentManager();
	      Fragment oldFrag = fm.findFragmentByTag(SEARCH_FRAGMENT_TAG);
	      
	      if (oldFrag == null) {
	    	  FragmentTransaction ft = fm.beginTransaction();
	    	  
	    	  ft.add(R.id.feature_search_result_container, mSearchFragment, SEARCH_FRAGMENT_TAG);
	    	  
	    	  ft.commit();
	      }
	      else {
	    	  FragmentTransaction ft = fm.beginTransaction();
	    	  //ft.remove(oldFrag);
	    	  //ft.add(mSearchFragment, SEARCH_FRAGMENT_TAG);
	    	  ft.replace(R.id.feature_search_result_container, mSearchFragment, SEARCH_FRAGMENT_TAG);
	    	  
	    	  ft.commit();
	      }
	      
	    }
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}

}
