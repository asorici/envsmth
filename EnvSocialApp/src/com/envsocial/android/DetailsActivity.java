package com.envsocial.android;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.EnvSocialResource;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.exceptions.EnvSocialComException;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.features.description.DescriptionFragment;
import com.envsocial.android.features.order.OrderFragment;
import com.envsocial.android.features.order.OrderManagerFragment;
import com.envsocial.android.features.people.PeopleFragment;
import com.envsocial.android.features.program.ProgramFragment;
import com.envsocial.android.utils.EnvivedNotificationContents;
import com.envsocial.android.utils.NotificationRegistrationDialog;
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.ResponseHolder;
import com.google.android.gcm.GCMRegistrar;


public class DetailsActivity extends SherlockFragmentActivity {
	private static final String TAG = "DetailsActivity";
	
	public static final String ORDER_MANAGEMENT_FEATURE = "order_management";
	private static final String REGISTER_GCM_ITEM = "Notifications On";
	private static final String UNREGISTER_GCM_ITEM = "Notifications Off";
	
	public static final int LOCATION_LOADER = 0;
	public static final int FEATURE_LOADER = 1;
	
	public static List<String> searchableFragmentTags;
	 
	static {
		searchableFragmentTags = new ArrayList<String>();
		searchableFragmentTags.add(Feature.ORDER);
		searchableFragmentTags.add(Feature.PROGRAM);
	}
	
	private static String mActiveFragmentTag;
	private static boolean active;
	
	public static boolean isActive() {
		return active;
	}
	
	public static String getActiveFeatureTag() {
		return mActiveFragmentTag;
	}
	
	private Location mLocation;
	
	private ActionBar mActionBar;
	private Tab mDefaultTab;
	private Tab mProgramTab;
	private Tab mOrderTab;
	private Tab mOrderManagementTab;
	private Tab mPeopleTab;
	
	private AsyncTask<Void, Void, ResponseHolder> mGCMRegisterTask;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.i(TAG, "[INFO] running onCreate in DetailsActivity");
        setContentView(R.layout.details);
        
        mActionBar = getSupportActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        String checkinUrl = getIntent().getStringExtra(ActionHandler.CHECKIN);
        
        //Bundle loaderBundle = new Bundle();
        //loaderBundle.putString("CHECKIN_URL", checkinUrl);
        //getSupportLoaderManager().initLoader(0, loaderBundle, this);
        
        checkin(checkinUrl);
        
        // register GCM status receiver
        registerReceiver(mHandleGCMMessageReceiver,
                new IntentFilter(GCMIntentService.ACTION_DISPLAY_GCM_MESSAGE));
        
        // setup GCM notification registration
        checkGCMRegistration();
	}
	
	
	private void checkGCMRegistration() {
		// look for the GCM registrationId stored in the GCMRegistrar
		// if not found, pop-up a dialog to invite the user to register in order to receive notifications
		final Context context = getApplicationContext();
		
		try {
			GCMRegistrar.checkManifest(context);
			GCMRegistrar.checkDevice(context);
		}
		catch (UnsupportedOperationException ex) {
			// TODO : see how to handle non-existent GSF package
			// this easy solution just catches an exception and presents a toast message
			Log.d(TAG, ex.getMessage());
			Toast toast = Toast.makeText(this, 
					"GSF package missing. Will not receive notifications. " +
					"Consider installing the Google Play App.", Toast.LENGTH_LONG);
			toast.show();
		}
		
		final String regId = GCMRegistrar.getRegistrationId(context);
        if (regId == null || "".equals(regId)) {
        	Log.d(TAG, "need to register for notifications");
        	NotificationRegistrationDialog dialog = NotificationRegistrationDialog.newInstance();
        	dialog.show(getSupportFragmentManager(), "dialog");
        }
	}
	
	
	private final BroadcastReceiver mHandleGCMMessageReceiver =
            new BroadcastReceiver() {
        
		@Override
        public void onReceive(Context context, Intent intent) {
            String newGCMMessage = intent.getExtras().getString(GCMIntentService.EXTRA_GCM_MESSAGE);
            
            Toast toast = Toast.makeText(DetailsActivity.this, 
					newGCMMessage, Toast.LENGTH_LONG);
			toast.show();
        }
    };
	
	
	@Override
	public void onPause() {
		Log.d(TAG, " --- onPause called in DetailsActivity");
		active = false;
		super.onPause();
	}
	
	
	@Override
	public void onStop() {
		Log.d(TAG, " --- onStop called in DetailsActivity");
		super.onStop();
	}
	
	@Override
	public void onResume() {
		Log.d(TAG, " --- onResume called in DetailsActivity");
		active = true;
		super.onResume();
	}
	
	
	@Override
	public void onStart() {
		Log.d(TAG, " --- onStart called in DetailsActivity");
		super.onStart();
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, " --- onDestroy called in DetailsActivity");
		
		// stop the GCM 3rd party server registration process if it is on the way
		if (mGCMRegisterTask != null) {
			mGCMRegisterTask.cancel(true);
		}
		
		// unregister the GCM broadcast receiver
		GCMRegistrar.onDestroy(getApplicationContext());
		
		// unregister the GCM status receiver
		unregisterReceiver(mHandleGCMMessageReceiver);
		
		/*
		if (mLocation != null) {
			mLocation.doCleanup(getApplicationContext());
		}
		*/
		
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// add the search button
		MenuItem item = menu.add(getText(R.string.menu_search));
        item.setIcon(R.drawable.ic_menu_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
        // add the register/unregister for notifications menu options
     	menu.add(REGISTER_GCM_ITEM);
     	menu.add(UNREGISTER_GCM_ITEM);
     	
     	// add temporary test for feature update notification
     	menu.add("Test Update Feature");
     	
    	return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final Context context = getApplicationContext();
		
		if (item.getTitle().toString().compareTo(getString(R.string.menu_search)) == 0) {
			return onSearchRequested();
		}
		
		else if (item.getTitle().toString().compareTo(REGISTER_GCM_ITEM) == 0) {
            final String regId = GCMRegistrar.getRegistrationId(context);
			
			if (regId != null && !"".equals(regId)) {
				
				// device is already registered on GCM, check server
				if (GCMRegistrar.isRegisteredOnServer(context)) {
					Toast toast = Toast.makeText(this, R.string.msg_gcm_already_registered, Toast.LENGTH_LONG);
					toast.show();
				}
				else {
					Log.d(TAG, "---- WE HAVE TO REGISTER WITH OUR SERVER FIRST.");
					
					// need to register with the server first
					// Try to register again, but not in the UI thread.
	                // It's also necessary to cancel the thread onDestroy(),
	                // hence the use of AsyncTask instead of a raw thread.
					mGCMRegisterTask = new AsyncTask<Void, Void, ResponseHolder>() {

	                    @Override
	                    protected ResponseHolder doInBackground(Void... params) {
	                        
	                    	ResponseHolder holder = ActionHandler.registerWithServer(context, regId);
	                    	
	                        // At this point all attempts to register with the app
	                        // server failed, so we need to unregister the device
	                        // from GCM - the app will try to register again when
	                        // it is restarted. Note that GCM will send an
	                        // unregistered callback upon completion, but
	                        // GCMIntentService.onUnregistered() will ignore it.
	                        if (holder.hasError()) {
	                        	Log.d(TAG, "Registration error: " + holder.getError().getMessage(), holder.getError());
	                        	GCMRegistrar.unregister(context);
	                        }
	                        
	                        return holder;
	                    }

	                    @Override
	                    protected void onPostExecute(ResponseHolder holder) {
	                    	if (holder.hasError()) {
		                    	Toast toast = Toast.makeText(DetailsActivity.this, 
		                    			R.string.msg_gcm_registration_error, Toast.LENGTH_LONG);
		    					toast.show();
	                    	}
	                    	else {
	                    		GCMRegistrar.setRegisteredOnServer(context, true);
	                    		
	                    		Toast toast = Toast.makeText(DetailsActivity.this, 
		                    			R.string.msg_gcm_already_registered, Toast.LENGTH_LONG);
		    					toast.show();
	                    	}
	                    	mGCMRegisterTask = null;
	                    }

	                };
	                mGCMRegisterTask.execute(null, null, null);
					
				}
            } else {
            	Log.d(TAG, "---- WE HAVE TO REGISTER WITH GCM.");
                GCMRegistrar.register(context, GCMIntentService.SENDER_ID);
            }
			
			return true;
		}
		
		else if (item.getTitle().toString().compareTo(UNREGISTER_GCM_ITEM) == 0) {
			GCMRegistrar.unregister(context);
			GCMRegistrar.setRegisteredOnServer(context, false);
			
			// for now we are not interested that much in the response for unregistration from our 3rd party server
			ActionHandler.unregisterWithServer(context);
			
			return true;
		}
		
		else if (item.getTitle().toString().compareTo("Test Update Feature") == 0) {
			Intent updateService = new Intent(context, EnvivedFeatureUpdateService.class);
			
			String locationUri = mLocation.getUri();
			EnvivedNotificationContents notificationContents = 
					new EnvivedNotificationContents(locationUri, Feature.ORDER, null, null);
			updateService.putExtra(EnvivedFeatureUpdateService.UPDATE_SERVICE_INPUT, notificationContents);
			
			context.startService(updateService);
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean onSearchRequested() {
		// first check to see which fragment initiated the search
		if (searchableFragmentTags.contains(mActiveFragmentTag)) {
			Bundle appData = new Bundle();
		    appData.putString(Feature.SEARCH_FEATURE, mActiveFragmentTag);
			
		    startSearch(null, false, appData, false);
		    return true;
		}
		
		return false;
	}
	
	
	private void checkin(String checkinUrl) {
		// Perform check in
		new CheckinTask(checkinUrl).execute();
	}
	
	
	private void addFeatureTabs() {
        // Add tabs based on features
        ActionBar actionBar = getSupportActionBar();
        
        if (mLocation.hasFeature(Feature.DESCRIPTION)) {
        	mDefaultTab = actionBar.newTab()
			.setText(R.string.tab_description)
			.setTabListener(new TabListener<DescriptionFragment>(
					this, Feature.DESCRIPTION, DescriptionFragment.class, mLocation));
        	actionBar.addTab(mDefaultTab);	
        }
         
        if (mLocation.hasFeature(Feature.PROGRAM)) {
        	mProgramTab = actionBar.newTab()
			.setText(R.string.tab_program)
			.setTabListener(new TabListener<ProgramFragment>(
					this, Feature.PROGRAM, ProgramFragment.class, mLocation));
        	actionBar.addTab(mProgramTab);	
        }
        
        if (mLocation.hasFeature(Feature.ORDER)) {
        	mOrderTab = actionBar.newTab()
			.setText(R.string.tab_order)
			.setTabListener(new TabListener<OrderFragment>(
					this, Feature.ORDER, OrderFragment.class, mLocation));
	        actionBar.addTab(mOrderTab);
	        
	        String loggedIn = Preferences.getLoggedInUserEmail(this);
	        
	        if (loggedIn != null && mLocation.isOwnerByEmail(loggedIn)) {
	        	mOrderManagementTab = actionBar.newTab()
				.setText(R.string.tab_order_manager)
				.setTabListener(new TabListener<OrderManagerFragment>(
						this, ORDER_MANAGEMENT_FEATURE, OrderManagerFragment.class, mLocation));
		        actionBar.addTab(mOrderManagementTab);
	        }
        }
        
        if (mLocation.hasFeature(Feature.PEOPLE)) {
        	mPeopleTab = actionBar.newTab()
        			.setText(R.string.tab_people)
        			.setTabListener(new TabListener<PeopleFragment>(
        					this, Feature.PEOPLE, PeopleFragment.class, mLocation));
        	actionBar.addTab(mPeopleTab);
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
				bundle.putSerializable(ActionHandler.CHECKIN, mLocation);
				mFragment.setArguments(bundle);
				
				ft.add(R.id.details_containter, mFragment, mTag);
			} else {
				// If the fragment exists, attach it in order to show it
				ft.attach(mFragment);
			}
			
			// set this fragment as the active one
			mActiveFragmentTag = mTag;
			
			ft.commit();
		}
		
		// Compatibility library sends null ft, so we simply ignore it and get our own
		public void onTabUnselected(Tab tab, FragmentTransaction ingnoredFt) {
			FragmentManager fragmentManager = ((SherlockFragmentActivity) mActivity).getSupportFragmentManager();
	        FragmentTransaction ft = fragmentManager.beginTransaction();
	        
			if (mFragment != null) {
				// Detach the fragment, another one is being attached
				ft.detach(mFragment);
			}
			
			mActiveFragmentTag = null;
			ft.commit();
		}
		
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// Do nothing.
		}		
	}
	
	private class CheckinTask extends AsyncTask<Void, Void, ResponseHolder> {
		private ProgressDialog mLoadingDialog;
		private String checkinUrl;
		
		public CheckinTask(String checkinUrl) {
			this.checkinUrl = checkinUrl;
		}

		@Override
		protected void onPreExecute() {
			mLoadingDialog = ProgressDialog.show(DetailsActivity.this, 
					"", "Check in ...", true);
		}
		
		@Override
		protected ResponseHolder doInBackground(Void...args) {
			ResponseHolder holder = ActionHandler.checkin(DetailsActivity.this, checkinUrl);
			if (!holder.hasError() && holder.getCode() == HttpStatus.SC_OK) {
				Location location = (Location) holder.getTag();
				
				try {
					location.initFeatures();
				} catch (EnvSocialContentException e) {
					holder = new ResponseHolder(new EnvSocialContentException(location.serialize(), 
							EnvSocialResource.FEATURE, e));
				}
			}
			
			return holder;
		}
		
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			mLoadingDialog.cancel();
			
			if (!holder.hasError()) {
				if (holder.getCode() == HttpStatus.SC_OK) {
					mLocation = (Location) holder.getTag();

					// TODO: fix padding issue in action bar style xml
					mActionBar.setTitle("     " + mLocation.getName());

					// We have location by now, so add tabs
					addFeatureTabs();
					String feature = getIntent().getStringExtra(EnvivedNotificationContents.FEATURE);
					if (feature != null) {
						mActionBar.selectTab(mOrderManagementTab);
					}
				}
				else if (holder.getCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
					setResult(RESULT_CANCELED);
					Toast toast = Toast.makeText(getApplicationContext(), R.string.msg_bad_checkin_response, Toast.LENGTH_LONG);
					toast.show();
					finish();
				}
				else {
					setResult(RESULT_CANCELED);
					Toast toast = Toast.makeText(getApplicationContext(), R.string.msg_malformed_checkin_url, Toast.LENGTH_LONG);
					toast.show();
					finish();
				}
			}
			else {
				int msgId = R.string.msg_service_unavailable;
				
				try {
					throw holder.getError();
				} catch (EnvSocialComException e) {
					Log.d(TAG, e.getMessage(), e);
					msgId = R.string.msg_service_unavailable;
				} catch (EnvSocialContentException e) {
					Log.d(TAG, e.getMessage(), e);
					msgId = R.string.msg_bad_checkin_response;
				} catch (Exception e) {
					Log.d(TAG, e.toString(), e);
					msgId = R.string.msg_service_error;
				}

				setResult(RESULT_CANCELED);
				Toast toast = Toast.makeText(getApplicationContext(), msgId, Toast.LENGTH_LONG);
				toast.show();
				finish();
			}
		}
	}
	
}
