package com.envsocial.android.features.program;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.envsocial.android.R;
import com.envsocial.android.api.EnvSocialResource;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.EnvivedFeatureActivity;
import com.envsocial.android.features.Feature;

public class ProgramActivity extends EnvivedFeatureActivity implements ProgramUpdateObserver {
	private static final String TAG = "ProgramActivity";
	
	private static final String TIME_TAB_TAG = "time";
	private static final String SESSION_TAB_TAG = "session";
	
	private ProgramFeature mProgramFeature;
	
	private List<ProgramUpdateListener> mRegisteredUpdateReceivers; 
	
	private ActionBar mActionBar;
	private String mCurrentTabTag;
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.program);
        
        mActionBar = getSupportActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        // instantiate the registered program update receivers list
        mRegisteredUpdateReceivers = new LinkedList<ProgramUpdateListener>();
        
        if (savedInstanceState != null) {
            // get current tab tag
        	mCurrentTabTag = savedInstanceState.getString("program_tab");
        }
      	else {
      		mCurrentTabTag = TIME_TAB_TAG;
      	}
    }
    
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActionBar.getSelectedTab() != null) {
        	outState.putString("program_tab", (String)mActionBar.getSelectedTab().getTag());
        }
    }
    
        
    @Override
	public void onDestroy() {
		super.onDestroy();
	}
    
    
    private void initTabbedFragments() {
    	// Add tabs based on different program views
        
        // add the time-based view
        Tab timeBasedProgramTab = mActionBar.newTab()
        		.setText("By Time")
        		.setTag(TIME_TAB_TAG)
    			.setTabListener(new TabListener<ProgramByTimeFragment>(this, 
    					Feature.PROGRAM, ProgramByTimeFragment.class, mLocation));
        mActionBar.addTab(timeBasedProgramTab);	
        
        
        /*
        Tab sessionBasedProgramTab = actionBar.newTab()
        		.setText("By Session")
        		.setTag(SESSION_TAB_TAG)
    			.setTabListener(new TabListener<ProgramBySessionFragment>(this, 
    					Feature.DESCRIPTION, ProgramBySessionFragment.class, mLocation));
        actionBar.addTab(sessionBasedProgramTab);
        */
        
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
				
				ft.add(R.id.program_container, mFragment, mTag);
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
	public void registerListener(ProgramUpdateListener l) {
		if (!mRegisteredUpdateReceivers.contains(l)) {
			mRegisteredUpdateReceivers.add(l);
		}
	}


	@Override
	public void unregisterListener(ProgramUpdateListener l) {
		mRegisteredUpdateReceivers.remove(l);
	}
	
	
	private void notifyProgramUpdate(ProgramFeature updatedProgramFeature) {
		for (ProgramUpdateListener l : mRegisteredUpdateReceivers) {
			l.onProgramUpdated(updatedProgramFeature);
		}
	}


	@Override
	protected Feature getLocationFeature(Location location) throws EnvSocialContentException {
		// return the Program location associated to the specified location or throw an Exception
		// if no such feature exists
		Feature programFeature = location.getFeature(Feature.PROGRAM);
		if (programFeature == null) {
			EnvSocialResource locationResource = 
					location.isEnvironment() ? EnvSocialResource.ENVIRONMENT : EnvSocialResource.AREA;
			throw new EnvSocialContentException(location.serialize(), locationResource, null);
		}
		
		return programFeature;
	}


	@Override
	protected void onFeatureDataInitialized(Feature newFeature, boolean success) {
		if (success) {
			mProgramFeature = (ProgramFeature) newFeature;
			initTabbedFragments();
		}
	}
	

	@Override
	protected void onFeatureDataUpdated(Feature updatedFeature, boolean success) {
		if (success) {
			mProgramFeature = (ProgramFeature) updatedFeature;
			notifyProgramUpdate(mProgramFeature);
		}
	}


	@Override
	protected String getActiveUpdateDialogMessage() {
		return "The program of this event has been modified. This update will refresh your view " +
				"of the schedule. Please select YES if you wish to perform the update. Select NO if, you " +
				"want to keep your current browsing session. The update will be performed next time you " +
				"start this activity.";
	}
}
