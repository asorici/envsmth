package com.envsocial.android;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.R;

public class DetailsSearchableActivity extends SherlockFragmentActivity implements OnClickListener {
	private ListView mListView;
	
	
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
	protected void onNewIntent(Intent intent) {
	    setIntent(intent);
	    handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      String query = intent.getStringExtra(SearchManager.QUERY);
	      doMySearch(query);
	    }
	}
	
	private void doMySearch(String query) {
		Toast toast = Toast.makeText(this, "Pretend search in Feature.", Toast.LENGTH_LONG);
		toast.show();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}

}
