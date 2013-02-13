package com.envsocial.android;

import org.apache.http.HttpStatus;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.Url;
import com.envsocial.android.api.exceptions.EnvSocialComException;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.order.OrderCustomAlertDialogFragment;
import com.envsocial.android.features.order.OrderCustomAlertDialogFragment.OrderNoticeAlertDialogListener;
import com.envsocial.android.utils.FeatureLRUTracker;
import com.envsocial.android.utils.LocationHistory;
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.RegisterEnvivedNotificationsTask;
import com.envsocial.android.utils.ResponseHolder;
import com.envsocial.android.utils.imagemanager.ImageCache;
import com.envsocial.android.utils.imagemanager.ImageFetcher;
import com.facebook.Session;
import com.google.android.gcm.GCMRegistrar;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class HomeActivity extends SherlockFragmentActivity 
	implements OnClickListener, OrderNoticeAlertDialogListener {
	
	private static final String TAG = "HomeActivity";
	private static final String SIGN_OUT = "Sign out";
	private static final String QUIT_ANONYMOUS = "Quit Anonymous";
	private static final String CHECK_OUT = "Check out";
	private static final String REGISTER_GCM_ITEM = "Notifications On";
	private static final String UNREGISTER_GCM_ITEM = "Notifications Off";
	
	private Button mBtnCheckin;
	private LinearLayout mFeaturedLocationsView;
	private LinearLayout mLocationHistoryView;
	
	private HomeFeaturedLocationsAdapter mFeaturedLocationsAdapter;
	private HomeLocationHistoryAdapter mLocationHistoryAdapter;
	
	//private ExpandableListView mLocationListView;
	//private HomeLocationListAdapter mLocationListAdapter;
	
	private ImageFetcher mImageFetcher;
	
	private RegisterEnvivedNotificationsTask mGCMRegisterTask;
	private LogoutTask mLogoutTask;
	private CheckoutTask mCheckoutTask;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "--- onCreate called in HomeActivity");
		super.onCreate(savedInstanceState);
        
		
		// -------------------------- feature LRU tracker loading ------------------------- //
		// look in the shared preferences to see if we have a FeatureLRUTracker
        FeatureLRUTracker featureLRUTracker = Preferences.getFeatureLRUTracker(getApplicationContext());
        if (featureLRUTracker == null) {
        	// Log.d(TAG, "CREATING NEW FEATURE LRU TRACKER");
        	
        	// if non exists create one with the default number of entries
        	featureLRUTracker = new FeatureLRUTracker();
        }
        else {
        	// Log.d(TAG, "FEATURE LRU TRACKER LOADED FROM SHARED PREF.");
        }
        
        // make it gloabally accessible in the application
        Envived.setFeatureLRUTracker(featureLRUTracker);
        
        
        // ---------------------------- location history loading ---------------------------- //
        LocationHistory locationHistory = Preferences.getLocationHistory(getApplicationContext());
        if (locationHistory == null) {
        	Log.d(TAG, "CREATING NEW LOCATION HISTORY");
        	
        	// if non exists create one with the default number of entries
        	locationHistory = new LocationHistory();
        }
        
        // make it gloabally accessible in the application
        Envived.setLocationHistory(locationHistory);
        
        
        // -------------------------- gcm notification registration ------------------------- //
        // register GCM status receiver
        registerReceiver(mHandleGCMMessageReceiver,
                new IntentFilter(GCMIntentService.ACTION_DISPLAY_GCM_MESSAGE));
		
        // setup GCM notification registration
        checkGCMRegistration();
        
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
        	// From notification, we forward to details
        	Intent intent = new Intent(this, DetailsActivity.class);
        	intent.putExtras(bundle);
        	startActivity(intent);
        }
        
        
        // ------------ setup image fetcher for asynchronous retrieval of images ------------ //
        initImageFetcher();
        
        mFeaturedLocationsAdapter = new HomeFeaturedLocationsAdapter(this, mImageFetcher);
        mLocationHistoryAdapter = new HomeLocationHistoryAdapter(this, mImageFetcher);
        
        // ------------ setup views for featured locations and location history ------------- //
        setContentView(R.layout.home);
        
        
        mFeaturedLocationsView = (LinearLayout) findViewById(R.id.home_featured_locations);
        mLocationHistoryView = (LinearLayout) findViewById(R.id.home_location_history);
        
        
        /*
        mFeaturedLocationsView.setAdapter(mFeaturedLocationsAdapter);
        mFeaturedLocationsView.setOnItemClickListener(this);
        
        mLocationHistoryView.setAdapter(mLocationHistoryAdapter);
        mLocationHistoryView.setOnItemClickListener(this);
        */
        
        
        /*
        mLocationListView = (ExpandableListView) findViewById(R.id.home_locations_list);
        mLocationListAdapter = new HomeLocationListAdapter(this, mImageFetcher);
        
        mLocationListView.setAdapter(mLocationListAdapter);
        mLocationListView.setOnChildClickListener(this);
        */
        
        // Set up action bar.
        getSupportActionBar().setTitle(R.string.app_name);

        mBtnCheckin = (Button) findViewById(R.id.btn_checkin);
        mBtnCheckin.setOnClickListener(this);
        
        fillFeaturedLocations();
        fillLocationHistory();
	}
	
	/*
	@Override
	public void onContentChanged() {
	    super.onContentChanged();
	    
	    LinearLayout featuredLocationsList = (LinearLayout) findViewById(R.id.home_featured_locations);
	    if (mFeaturedLocationsAdapter.getCount() == 0) {
	    	featuredLocationsList.removeAllViews();
	    	
	    	View v = 
	    		getLayoutInflater().inflate(R.layout.home_featured_locations_empty, featuredLocationsList, false);
	    	View emptyView = v.findViewById(R.id.home_featured_locations_empty);
	    	
	    	featuredLocationsList.addView(emptyView);
	    }
	    
	    
	    LinearLayout locationHistoryList = (LinearLayout) findViewById(R.id.home_location_history);
	    if (mLocationHistoryAdapter.getCount() == 0) {
	    	locationHistoryList.removeAllViews();
	    	
	    	View v = 
	    		getLayoutInflater().inflate(R.layout.home_location_history_empty, locationHistoryList, false);
	    	View emptyView = v.findViewById(R.id.home_location_history_empty);
	    	
	    	locationHistoryList.addView(emptyView);
	    }
	}
	*/
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "--- onDestroy called in HomeActivity");
		
		// --------------------------- stop asynchronous tasks ---------------------------- //
		if (mCheckoutTask != null) {
			mCheckoutTask.cancel(false);
		}
		
		if (mLogoutTask != null) {
			mLogoutTask.cancel(false);
		}
		
		// ------------------------ location cleanup if checked in ------------------------ //
		// if there is a current location
		Location currentLocation = Preferences.getCheckedInLocation(this);
		if (currentLocation != null) {
			// perform cleanup on exit
			currentLocation.doCleanup(getApplicationContext());
		}
		
		
		// --------------------------- feature LRU tracker saving ------------------------- //
		// save the feature lru tracker in the shared preferences
        FeatureLRUTracker featureLRUTracker = Envived.getFeatureLRUTracker();
        if (featureLRUTracker != null) {
        	Preferences.setFeatureLRUTracker(getApplicationContext(), featureLRUTracker);
        }
		
        // ------------------------ location history tracker saving ----------------------- //
        LocationHistory locationHistory = Envived.getLocationHistory();
        if (locationHistory != null) {
        	Preferences.setLocationHistory(getApplicationContext(), locationHistory);
        }
        
        // -------------------------- gcm notification deregister ------------------------- //
        // stop the GCM 3rd party server registration process if it is on the way
     	if (mGCMRegisterTask != null) {
     		mGCMRegisterTask.cancel(true);
     	}
        
     	// unregister the GCM broadcast receiver
     	GCMRegistrar.onDestroy(getApplicationContext());
     		
     	// unregister the GCM status receiver
     	unregisterReceiver(mHandleGCMMessageReceiver);
     	
     	// -------------------------------- other operations ------------------------------- //
        
     	// close image fetcher cache
     	mImageFetcher.closeCache();
     	
     	// close facebook session if existant
     	doFacebookLogout();
     	
     	super.onDestroy();
	}
	
	
	@Override
    public void onStart() {
    	super.onStart();
    	Log.d(TAG, "--- onStart in HomeActivity ");
    	
    	// update the location history list - to take into account the visits that were made
    	// to various locations
    	mLocationHistoryAdapter.notifyDataSetChanged();
    	fillLocationHistory();
    	//mLocationListAdapter.notifyDataSetChanged();
    }


	@Override
    public void onStop() {
    	super.onStop();
    	Log.d(TAG, "--- onStop in HomeActivity");
    }
	
	
	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "On Resume in Home Activity");
		
		mImageFetcher.setExitTasksEarly(false);
		
		// reset location if we have checked in at another activity in the meantime
		displayCheckedInLocation();
	}
	
	
	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, "On Pause in Home Activity");
		
		mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
	}
	
	
	private void initImageFetcher() {
        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(getApplicationContext(), ImageCache.IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(this, 0.0675f); 	// Set memory cache
        
        // The ImageFetcher takes care of loading images into ImageViews asynchronously
        mImageFetcher = Envived.getImageFetcherInstance(getSupportFragmentManager(), 
        		cacheParams, R.drawable.placeholder_small);
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
		
		// check if device is has an active registraionId and get one if not
		final String regId = GCMRegistrar.getRegistrationId(context);
        if (regId == null || "".equals(regId)) {
        	Log.d(TAG, "need to register for notifications");
        	GCMRegistrar.register(this, GCMIntentService.SENDER_ID);
        	//NotificationRegistrationDialog dialog = NotificationRegistrationDialog.newInstance();
        	//dialog.show(getSupportFragmentManager(), "dialog");
        }
        
        // check if we are also registered with the Envived server
        // this will only be done if a valid user URI exists. In anonymous mode one will only get
        // such an URI after checkin, so this action is deferred to DetailsActivity
        if (Preferences.getUserUri(context) != null && !GCMRegistrar.isRegisteredOnServer(context)) {
        	mGCMRegisterTask = new RegisterEnvivedNotificationsTask(this);
        	mGCMRegisterTask.execute(regId);
        }
	}
	
	
	private final BroadcastReceiver mHandleGCMMessageReceiver =
            new BroadcastReceiver() {
        
		@Override
        public void onReceive(Context context, Intent intent) {
            String newGCMMessage = intent.getExtras().getString(GCMIntentService.EXTRA_GCM_MESSAGE);
            
            Toast toast = Toast.makeText(HomeActivity.this, newGCMMessage, Toast.LENGTH_LONG);
			toast.show();
        }
    };
	
    
	private void doFacebookLogout() {
	    Session session = Session.getActiveSession();
	    if (session != null) {
	    	session.closeAndClearTokenInformation();
	    }
	}
	
	
	private void displayCheckedInLocation() {
		TextView v = (TextView)findViewById(R.id.checked_in_location_name);
        Location currentLocation = Preferences.getCheckedInLocation(this);
        //System.out.println("[DEBUG]>> Current checkin location: " + currentLocation);
        if (currentLocation != null) {
        	v.setText(currentLocation.getName());
        }
        else {
        	v.setText(R.string.lbl_no_checkin_location);
        }
	}
	
	private void fillLocationHistory() {
		mLocationHistoryView.removeAllViews();
		
		int len = mLocationHistoryAdapter.getCount();
		
		if (len == 0) {
			View v = 
				getLayoutInflater().inflate(R.layout.home_location_history_empty, mLocationHistoryView, false);
		    View emptyView = v.findViewById(R.id.home_location_history_empty);
		    	
		    mLocationHistoryView.addView(emptyView);
		}
		else {
			for (int i = 0; i < len; i++) {
				View item = mLocationHistoryAdapter.getView(i, null, mLocationHistoryView);
				item.setOnClickListener(this);
				item.setTag(i);
				mLocationHistoryView.addView(item);
				
				if (i != len - 1) {
					mLocationHistoryView.addView(getSeparatorView((ViewGroup)mLocationHistoryView));
				}
			}
		}
	}
	
	
	private void fillFeaturedLocations() {
		mFeaturedLocationsView.removeAllViews();
		
		int len = mFeaturedLocationsAdapter.getCount();
		
		if (len == 0) {
			View v = getLayoutInflater().inflate(
					R.layout.home_featured_locations_empty,
					mFeaturedLocationsView, false);
			View emptyView = v.findViewById(R.id.home_featured_locations_empty);

			mFeaturedLocationsView.addView(emptyView);
		}
		else {
			for (int i = 0; i < len; i++) {
				View item = mFeaturedLocationsAdapter.getView(i, null, mFeaturedLocationsView);
				item.setOnClickListener(this);
				item.setTag(i);
				mFeaturedLocationsView.addView(item);
				
				if (i != len - 1) {
					mFeaturedLocationsView.addView(getSeparatorView((ViewGroup)mFeaturedLocationsView));
				}
			}
		}
	}
	
	
	private View getSeparatorView(ViewGroup parent) {
		// instantiate speaker separator view
		View v = getLayoutInflater().inflate(R.layout.envived_layout_separator_default, parent, false);
		return v.findViewById(R.id.layout_separator);
	}
	
	/*
	@Override
	public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {
		
		if (parentView == mLocationHistoryView) {
			Location location = (Location) mLocationHistoryAdapter.getItem(position);
			
			Intent intent = new Intent(this, DetailsActivity.class);
			intent.putExtra("location", location);
			
			startActivity(intent);
		}
		else if (parentView == mFeaturedLocationsView) {
			String locationUrl = (String) mFeaturedLocationsAdapter.getItem(position);
			Url url = Url.fromResourceUrl(locationUrl);
			
			if (url != null && url.getItemId() != null) {
				String locationItem = url.getUrlItem();
				String locationId = url.getItemId();
			
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
	*/
	
	/*
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, 
			int childPosition, long id) {
		
		if (groupPosition == HomeLocationListAdapter.FEATURED_LOCATIONS_GROUP_ID) {
			String locationUrl = (String) mLocationListAdapter.getChild(groupPosition, childPosition);
			Url url = Url.fromResourceUrl(locationUrl);
			
			if (url != null && url.getItemId() != null) {
				String locationItem = url.getUrlItem();
				String locationId = url.getItemId();
			
				Url checkinUrl = new Url(Url.ACTION, ActionHandler.CHECKIN);
				checkinUrl.setParameters(new String [] {locationItem, "virtual"}, 
										 new String [] {locationId, Boolean.toString(true)});
				
				// create intent for new DetailsActivity
				Intent intent = new Intent(this, DetailsActivity.class);
				intent.putExtra(ActionHandler.CHECKIN, checkinUrl.toString());
				
				startActivity(intent);
				
				return true;
			}
		}
		else if (groupPosition == HomeLocationListAdapter.LOCATION_HISTORY_GROUP_ID) {
			Location location = (Location) mLocationListAdapter.getChild(groupPosition, childPosition);
			
			Intent intent = new Intent(this, DetailsActivity.class);
			intent.putExtra("location", location);
			
			startActivity(intent);
			
			return true;
		}
		
		return false;
	}
	*/
	
	public void onClick(View v) {
		if (v == mBtnCheckin) {
			final Location currentLocation = Preferences.getCheckedInLocation(this);
			if (currentLocation != null) {
				String dialogMessage = "Keep previous checkin location ("  + currentLocation.getName() + ") ?";
				
				OrderCustomAlertDialogFragment checkinDialog = 
						OrderCustomAlertDialogFragment.newInstance("Select Checkin Location", 
								dialogMessage, "Yes", "No");
				checkinDialog.setOrderNoticeAlertDialogListener(this);
				checkinDialog.show(getSupportFragmentManager(), "dialog");
			}
			else {
				IntentIntegrator integrator = new IntentIntegrator(this);
				integrator.initiateScan();
			}
		}
		else {
			View parent = (View)v.getParent();
			
			if (parent.getId() == R.id.home_featured_locations) {
				int position = (Integer)v.getTag();
				String locationUrl = (String) mFeaturedLocationsAdapter.getItem(position);
				
				Log.d(TAG, "TRYING TO GET ACCESS TO FEATURED LOCATION: " + position + " - " + locationUrl);
				Url url = Url.fromResourceUrl(locationUrl);
				
				if (url != null && url.getItemId() != null) {
					String locationItem = url.getUrlItem();
					String locationId = url.getItemId();
				
					Url checkinUrl = new Url(Url.ACTION, ActionHandler.CHECKIN);
					checkinUrl.setParameters(new String [] {locationItem, "virtual"}, 
											 new String [] {locationId, Boolean.toString(true)});
					
					Log.d(TAG, "TRYING TO GET ACCESS TO FEATURED LOCATION: " + position + " - " + checkinUrl.toString());
					
					// create intent for new DetailsActivity
					Intent intent = new Intent(this, DetailsActivity.class);
					intent.putExtra(ActionHandler.CHECKIN, checkinUrl.toString());
					
					startActivity(intent);
				}
			}
			else if (parent.getId() == R.id.home_location_history) {
				int position = (Integer)v.getTag();
				Location location = (Location) mLocationHistoryAdapter.getItem(position);
				
				Intent intent = new Intent(this, DetailsActivity.class);
				intent.putExtra("location", location);
				
				startActivity(intent);
			}
		}
	}
	

	@Override
	public void onDialogPositiveClick(DialogFragment dialog) {
		dialog.dismiss();
    	
    	Intent i = new Intent(HomeActivity.this, DetailsActivity.class);
		
    	// we can put null for the payload since we have a location saved in preferences
    	i.putExtra(ActionHandler.CHECKIN, (String)null);
		startActivity(i);
	}


	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		dialog.dismiss();
    	
    	// do a local checkout before checking in somewhere else
    	// for now this will also refresh any feature data that was renewed server-side
    	Preferences.checkout(getApplicationContext());
    	
    	IntentIntegrator integrator = new IntentIntegrator(HomeActivity.this);
		integrator.initiateScan();
	}
	
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK) {
			if (requestCode == IntentIntegrator.REQUEST_CODE) {
				// We have a checkin action, grab checkin url from scanned QR code
				IntentResult scanResult = 
					IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		    	if (scanResult != null) {
		    		String actionUrl = scanResult.getContents();
		    		Log.d(TAG, "CHECKIN URL: " + actionUrl);
		    		
		    		// Check if checkin url is proper
		    		if (actionUrl.startsWith(Url.actionUrl(ActionHandler.CHECKIN))) {
		    			Intent i = new Intent(this, DetailsActivity.class);
			    		i.putExtra(ActionHandler.CHECKIN, actionUrl);
			    		startActivity(i);
		    		} else {
		    			// If not, inform the user
		    			Toast toast = Toast.makeText(this, R.string.msg_malformed_checkin_url, Toast.LENGTH_LONG);
		    			toast.show();
		    		}
		    	}
			}
		}
		else {
			//System.err.println("WOUND UP HERE");
			Toast toast = Toast.makeText(this, "Action Canceled or Connection Error.", Toast.LENGTH_LONG);
			toast.show();
		}
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {		
		
		// add the search button
		MenuItem item = menu.add(R.string.menu_search);
        item.setIcon(R.drawable.ic_menu_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
        // add the register/unregister for notifications menu options
     	menu.add(REGISTER_GCM_ITEM);
     	menu.add(UNREGISTER_GCM_ITEM);
        
		
    	return true;
	}
	
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Context context = getApplicationContext();
		
		if (Preferences.getUserUri(context) != null) {
			
			boolean exists = false;
			for (int i = 0; i < menu.size(); i++) {
				if (menu.getItem(i).getTitle().equals(SIGN_OUT) 
					|| menu.getItem(i).getTitle().equals(QUIT_ANONYMOUS)) {
					exists = true;
				}
			}
			
			if (!exists) {
				if (Preferences.isLoggedIn(context)) {
					menu.add(SIGN_OUT);
				}
				else {
					menu.add(QUIT_ANONYMOUS);
				}
			}
		}
		
		if (Preferences.isCheckedIn(context)) {
			boolean exists = false;
			for (int i = 0; i < menu.size(); i++) {
				if (menu.getItem(i).getTitle().equals(CHECK_OUT) ) {
					exists = true;
				}
			}
			
			if (!exists) menu.add(CHECK_OUT);
		}
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final Context context = getApplicationContext();
		
		if (item.getTitle().toString().compareTo(SIGN_OUT) == 0) {
			new LogoutTask().execute(LogoutTask.REAL_LOGOUT);
		}
		else if (item.getTitle().toString().compareTo(QUIT_ANONYMOUS) == 0) {
			new LogoutTask().execute(LogoutTask.ANONYMOUS_LOGOUT);
		}
		else if (item.getTitle().toString().compareTo(CHECK_OUT) == 0) {
			new CheckoutTask().execute();
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
					mGCMRegisterTask = new RegisterEnvivedNotificationsTask(this);
	                mGCMRegisterTask.execute(regId);
					
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
		
		else if (item.getTitle().toString().compareTo(getString(R.string.menu_search)) == 0) {
			return onSearchRequested();
		}
		
		return true;
	}

	
	private class LogoutTask extends AsyncTask<Integer, Void, ResponseHolder> {
		public static final int ANONYMOUS_LOGOUT = 0;
		public static final int REAL_LOGOUT = 1;
		
		private ProgressDialog mLoadingDialog;
		
		@Override
		protected void onPreExecute() {
			mLoadingDialog = new ProgressDialog(new ContextThemeWrapper(HomeActivity.this, R.style.ProgressDialogWhiteText));
			mLoadingDialog.setMessage("Signing out ...");
			mLoadingDialog.setIndeterminate(true);
			mLoadingDialog.setCancelable(true);
			mLoadingDialog.setCanceledOnTouchOutside(false);
			
			mLoadingDialog.show();
		}
		
		@Override
		protected ResponseHolder doInBackground(Integer...args) {
			Context context = getApplicationContext();
			int action = args[0];
			
			ResponseHolder response = null;
			if (action == REAL_LOGOUT) {
				response = ActionHandler.logout(context);
			}
			else {
				response = ActionHandler.delete_anonymous(context);
			}
			
			if (!response.hasError() && response.getCode() == HttpStatus.SC_OK) {
				// unregister from our server notifications
				GCMRegistrar.setRegisteredOnServer(context, false);
				
				// also checkout before going back to the main activity
				Preferences.checkout(context);
			}
			
			return response;
		}
		
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			mLoadingDialog.cancel();
			
			if (!holder.hasError()) {
				if (holder.getCode() == HttpStatus.SC_OK) {
					// finish this activity and return to EnvSocialAppActivity or exit completely
					finish();
				}
				else {
					Log.d(TAG, "Error performing sign out. Error code: " + holder.getCode() + 
							". Response Body: " + holder.getResponseBody());
					
					Toast toast = Toast.makeText(HomeActivity.this, R.string.msg_logout_error, Toast.LENGTH_LONG);
					toast.show();
				}
			}
			else {
				// TODO: 	for now log the communication error and checkout anyway
				//			need to figure out a way to re-establish consistency 
				try {
					throw holder.getError();
				} catch (EnvSocialComException e) {
					Log.d(TAG, e.getMessage(), e);
				} catch (EnvSocialContentException e) {
					Log.d(TAG, e.getMessage(), e);
				} catch (Exception e) {
					Log.d(TAG, e.toString(), e);
				}
				
				Toast toast = Toast.makeText(HomeActivity.this, R.string.msg_logout_error, Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}
	
	
	private class CheckoutTask extends AsyncTask<Void, Void, ResponseHolder> {
		private ProgressDialog mLoadingDialog;
		private String mLocationName; 
		
		@Override
		protected void onPreExecute() {
			Location location = Preferences.getCheckedInLocation(getApplicationContext());
			if (location != null) {
				mLocationName = location.getName();
			}
			
			mLoadingDialog = new ProgressDialog(new ContextThemeWrapper(HomeActivity.this, R.style.ProgressDialogWhiteText));
			mLoadingDialog.setMessage("Checking out ...");
			mLoadingDialog.setIndeterminate(true);
			mLoadingDialog.setCancelable(true);
			mLoadingDialog.setCanceledOnTouchOutside(false);
			
			mLoadingDialog.show();
		}
		
		@Override
		protected ResponseHolder doInBackground(Void...args) {
			Context context = getApplicationContext();
			ResponseHolder response = ActionHandler.checkout(context);
			
			/*
			// THE TASK IS FOR PHYSICAL CHECKOUT; SO WE DON'T HAVE TO UNREGISTER FOR NOTIFICATIONS
			// THAT MIGHT ARRIVE FOR virtual CHECKINS
			
			if (!response.hasError() && !Preferences.isLoggedIn(context)) {
				// unregister from our server notifications
				GCMRegistrar.setRegisteredOnServer(context, false);
			}
			*/
			
			return response;
		}
		
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			mLoadingDialog.dismiss();
			
			if (!holder.hasError()) {
				
				if (holder.getCode() == HttpStatus.SC_OK) {
					String message = getResources().getString(R.string.msg_checkout_successful);
					if (mLocationName != null) {
						getResources().getString(R.string.msg_checkout_successful_location, mLocationName);
					}
					
					Toast toast = Toast.makeText(HomeActivity.this, message, Toast.LENGTH_LONG);
					toast.show();
					
					displayCheckedInLocation();
				}
				else {
					if (mLocationName != null) {
						Log.d(TAG, "ERROR performing checkout from " + mLocationName + 
							". Error code: " + holder.getCode() + ". Response body: " + holder.getResponseBody());
						
						String message = getResources().getString(R.string.msg_checkout_error);
						if (mLocationName != null) {
							getResources().getString(R.string.msg_checkout_error_location, mLocationName);
						}
						
						Toast toast = Toast.makeText(HomeActivity.this, message, Toast.LENGTH_LONG);
						toast.show();
					}
				}
			}
			else {
				// TODO: 	for now log the communication error and checkout anyway
				//			need to figure out a way to re-establish consistency 
				
				try {
					throw holder.getError();
				} catch (EnvSocialComException e) {
					Log.d(TAG, e.getMessage(), e);
				} catch (EnvSocialContentException e) {
					Log.d(TAG, e.getMessage(), e);
				} catch (Exception e) {
					Log.d(TAG, e.toString(), e);
				}
				
				String message = getResources().getString(R.string.msg_checkout_error);
				if (mLocationName != null) {
					getResources().getString(R.string.msg_checkout_error_location, mLocationName);
				}
				
				Toast toast = Toast.makeText(HomeActivity.this, message, Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}
}
