package com.envsocial.android.features.description;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.CommentsActivity;
import com.envsocial.android.Envived;
import com.envsocial.android.R;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.EnvivedFeatureActivity;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.imagemanager.ImageCache;
import com.envsocial.android.utils.imagemanager.ImageFetcher;

public class BoothDescriptionActivity extends EnvivedFeatureActivity {
	private static final String DESCRIPTION_TAB_TAG = "Description";
	private static final String PRODUCT_TAB_TAG = "Projects";
	
	//private static final int BOOTH_DESCRIPTION_LOADER = 0;
	//private static final int BOOTH_PRODUCT_LOADER = 1;
	
	private BoothDescriptionFeature mDescriptionFeature;
	
	private ActionBar mActionBar;
	private String mCurrentTabTag;
	private ImageFetcher mImageFetcher;
	
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.description_booth);
        
        mActionBar = getSupportActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        initImageFetcher();
        
        if (savedInstanceState != null) {
            // get current tab tag
        	mCurrentTabTag = savedInstanceState.getString("booth_description_tab");
        }
      	else {
      		mCurrentTabTag = DESCRIPTION_TAB_TAG;
      	}
    }
    
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActionBar.getSelectedTab() != null) {
        	outState.putString("booth_description_tab", (String)mActionBar.getSelectedTab().getTag());
        }
    }
    
    
    @Override
	public void onStart() {
		super.onStart();
	
		// check if due to delayed onDestroy (can happen from Notification relaunch) 
		// the image fetcher has closed the cache after it was opened again
		if (mImageFetcher == null || !mImageFetcher.cashOpen()) {
			initImageFetcher();
		}
	}
    
        
    @Override
	public void onDestroy() {
		super.onDestroy();
		
		// close image fetcher cache
		mImageFetcher.closeCache();
	}
	
    
    private void initImageFetcher() {
		ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(
				getApplicationContext(), ImageCache.IMAGE_CACHE_DIR);
		
		cacheParams.setMemCacheSizePercent(this, 0.0675f); 	// Set memory cache
															// to 1/16 of memclass

		// The ImageFetcher takes care of loading images into ImageViews asynchronously
		mImageFetcher = Envived.getImageFetcherInstance(getSupportFragmentManager(),
				cacheParams, R.drawable.placeholder);
	}
    
    
    private void initTabbedFragments() {
    	// Add tabs based on different program views
        
        // add the time-based view
        Tab boothDescriptionTab = mActionBar.newTab()
        		.setText(DESCRIPTION_TAB_TAG)
        		.setTag(DESCRIPTION_TAB_TAG)
    			.setTabListener(new TabListener<BoothDescriptionDetailsFragment>(this, 
    					Feature.BOOTH_DESCRIPTION, BoothDescriptionDetailsFragment.class, mLocation));
        mActionBar.addTab(boothDescriptionTab);	
        
        
        Tab boothProductsTab = mActionBar.newTab()
        		.setText(PRODUCT_TAB_TAG)
        		.setTag(PRODUCT_TAB_TAG)
    			.setTabListener(new TabListener<BoothDescriptionProductsFragment>(this, 
    					Feature.BOOTH_DESCRIPTION, BoothDescriptionProductsFragment.class, mLocation));
        mActionBar.addTab(boothProductsTab);
        
        
        // set the selected tab
        
        if (mCurrentTabTag != null) {
	        int numTabs = mActionBar.getTabCount();
	        for (int i = 0; i < numTabs; i++) {
	        	Tab tab = mActionBar.getTabAt(i);
	        	
	        	if (tab.getTag().equals(mCurrentTabTag)) {
	        		tab.select();
	        		break;
	        	}
	        }
        }
	}
	
    
    private class TabListener<T extends SherlockFragment> implements ActionBar.TabListener {

		private SherlockFragment mFragment;
		private final Activity mActivity;
		private final String mTag;
		private final Class<T> mClass;
		private Location mLocation;
		
		public TabListener(Activity activity, String tag, Class<T> clz, Location location) {
			mActivity = activity;
			mTag = tag;
			mClass = clz;
			mLocation = location;
		}
		
		// Compatibility library sends null ft, so we simply ignore it and get our own
		public void onTabSelected(Tab tab, FragmentTransaction ingnoredFt) {
			FragmentManager fragmentManager = ((SherlockFragmentActivity) mActivity).getSupportFragmentManager();
	        FragmentTransaction ft = fragmentManager.beginTransaction();
	        
			// Check if the fragment is already initialized
			if (mFragment == null) {
				// If not, instantiate the fragment and add it to the activity
				mFragment = (SherlockFragment) SherlockFragment.instantiate(mActivity, mClass.getName());
				
				Bundle bundle = new Bundle();
				mFragment.setArguments(bundle);
				
				ft.add(R.id.booth_container, mFragment, mTag);
				//ft.add(mFragment, mTag);
			} else {
				// If the fragment exists, attach it in order to show it
				ft.attach(mFragment);
			}
			
			// set this fragment as the active one
			//mCurrentTabTag = mTag;
			
			ft.commit();
		}
		
		// Compatibility library sends null ft, so we simply ignore it and get our own
		public void onTabUnselected(Tab tab, FragmentTransaction ingnoredFt) {
			FragmentManager fragmentManager = ((SherlockFragmentActivity) mActivity).getSupportFragmentManager();
			
			// firstly pop the entire fragment backstack -- each feature may load it's different fragments
	        // but since we are swapping features here we basically want to clear everything
	        
	        if (fragmentManager.getBackStackEntryCount() > 0) {
	        	BackStackEntry bottomEntry = fragmentManager.getBackStackEntryAt(0);
	        	fragmentManager.popBackStackImmediate(
					bottomEntry.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
	        }
			
			
			FragmentTransaction ft = fragmentManager.beginTransaction();
			if (mFragment != null) {
				// Detach the fragment, another one is being attached
				ft.detach(mFragment);
			}
			
			//mCurrentTabTag = null;
			ft.commit();
		}
		
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// Do nothing.
		}		
	}
    
    
	@Override
	protected Feature getLocationFeature(Location location) throws EnvSocialContentException {
		return location.getFeature(Feature.BOOTH_DESCRIPTION);
	}

	@Override
	protected void onFeatureDataInitialized(Feature newFeature, boolean success) {
		if (success) {
			mDescriptionFeature = (BoothDescriptionFeature) newFeature;
			initTabbedFragments();
		}
	}

	@Override
	protected void onFeatureDataUpdated(Feature updatedFeature, boolean success) {
		if (success) {
			mDescriptionFeature = (BoothDescriptionFeature) updatedFeature;
			// TODO: update the fragments
		}
	}

	@Override
	protected String getActiveUpdateDialogMessage() {
		return "The data of this event exhibitor has been modified. This update will refresh your view. " +
				"Please select YES if you wish to perform the update. Select NO if, you " +
				"want to keep your current browsing session. The update will be performed next time you " +
				"start this activity.";
	}
	
	
	protected ImageFetcher getImageFetcher() {
		return mImageFetcher;
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem commentItem = menu.add(getText(R.string.menu_comments));
		commentItem.setIcon(R.drawable.ic_menu_comments);
		commentItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		final Context context = getApplicationContext();
		
		if (item.getTitle().toString().compareTo(getString(R.string.menu_comments)) == 0) {
			Intent intent = new Intent(getApplicationContext(), CommentsActivity.class);
			intent.putExtra("location", mLocation);
			startActivity(intent);
			
			return true;
		}
		
		return false;
	}
}
