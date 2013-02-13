package com.envsocial.android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.Location.AreaInfo;
import com.envsocial.android.api.Url;
import com.envsocial.android.utils.imagemanager.ImageCache;
import com.envsocial.android.utils.imagemanager.ImageFetcher;

public class BrowseLocationsActivity extends SherlockFragmentActivity implements OnItemClickListener {
	private static final String TAG = "BrowseLocationsActivity";
	
	private Location mLocation;
	private ImageFetcher mImageFetcher;
	
	private TextView mListHeaderView;
	private ListView mListView;
	private BrowseLocationsListAdapter mAdapter;
	
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (savedInstanceState != null) {
        	mLocation = (Location) savedInstanceState.get("location");
        }
        else {
        	mLocation = (Location) getIntent().getExtras().get("location");
        }
        
        // define a custom screen layout here
        setContentView(R.layout.browse_locations);
        
        // image cache initialization 
        initImageFetcher();
        
        // build the header view
        mListHeaderView = (TextView) findViewById(R.id.browse_locations_label);
        mListHeaderView.setText(getResources().getString(R.string.lbl_browse_locations, mLocation.getName()));
        
        
        // build list view and its adapter
        mListView = (ListView) findViewById(R.id.browse_locations_list);
        
        mAdapter = new BrowseLocationsListAdapter(this, mLocation, mImageFetcher);  
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
    }
	
	
	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("location", mLocation);
    }
	
	
	@Override
	public void onContentChanged() {
	    super.onContentChanged();
	    
	    ListView list = (ListView) findViewById(R.id.browse_locations_list);
	    View empty = findViewById(R.id.browse_locations_empty);
	    list.setEmptyView(empty);
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		mImageFetcher.setExitTasksEarly(false);
	}
	
	
	@Override
	public void onPause() {
		super.onPause();
		mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// close image fetcher cache
		mImageFetcher.closeCache();
	}
	
	
	private void initImageFetcher() {
        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(getApplicationContext(), ImageCache.IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(this, 0.0675f); // Set memory cache to 1/16 of mem class
        
        // The ImageFetcher takes care of loading images into ImageViews asynchronously
        mImageFetcher = Envived.getImageFetcherInstance(getSupportFragmentManager(), 
        		cacheParams, R.drawable.placeholder_small);
	}
	
	
	@Override
	public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {
		AreaInfo areaInfo = (AreaInfo) mAdapter.getItem(position);
		String areaUrlString = areaInfo.getResourceUrl();
		
		Log.d(TAG, "Accessing area with url: " + areaUrlString);
		
		Url areaResourceUrl = Url.fromResourceUrl(areaUrlString);
		
		if (areaResourceUrl != null && areaResourceUrl.getItemId() != null) {
			String locationItem = areaResourceUrl.getUrlItem();
			String locationId = areaResourceUrl.getItemId();
			
			Url checkinUrl = new Url(Url.ACTION, ActionHandler.CHECKIN);
			checkinUrl.setParameters(new String [] {locationItem, "virtual"}, 
									 new String [] {locationId, Boolean.toString(true)});
			
			// create intent for new DetailsActivity
			Intent intent = new Intent(this, DetailsActivity.class);
			intent.putExtra(ActionHandler.CHECKIN, checkinUrl.toString());
			
			startActivity(intent);
		}
	}
}
