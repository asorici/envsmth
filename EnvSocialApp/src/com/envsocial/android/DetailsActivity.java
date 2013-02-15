package com.envsocial.android;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpStatus;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.exceptions.EnvSocialComException;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.features.description.BoothDescriptionActivity;
import com.envsocial.android.features.description.DescriptionActivity;
import com.envsocial.android.features.program.ProgramActivity;
import com.envsocial.android.utils.EnvivedNotificationContents;
import com.envsocial.android.utils.LocationHistory;
import com.envsocial.android.utils.ResponseHolder;
import com.envsocial.android.utils.imagemanager.ImageCache;
import com.envsocial.android.utils.imagemanager.ImageFetcher;


public class DetailsActivity extends SherlockFragmentActivity {
	private static final String TAG = "DetailsActivity";
	
	public static final String ORDER_MANAGEMENT_FEATURE = "order_management";
	
	public static final int LOCATION_LOADER = 0;
	public static final int FEATURE_LOADER = 1;
	
	public static List<String> searchableFragmentTags;
	 
	static {
		searchableFragmentTags = new ArrayList<String>();
		searchableFragmentTags.add(Feature.ORDER);
		searchableFragmentTags.add(Feature.PROGRAM);
	}
	
	// private RegisterEnvivedNotificationsTask mGCMRegisterTask;
	
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
	
	private LinearLayout mMainView;
	private TextView mLabelView;
	private GridView mGridView;
	private DetailsGridAdapter mGridAdapter;
	
	private ImageFetcher mImageFetcher;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.details);
        
        mMainView = (LinearLayout) findViewById(R.id.details_container);
        mLabelView = (TextView) findViewById(R.id.details_label_features);
        mGridView = new GridView(this);
        
        mActionBar = getSupportActionBar();
        
        
        // --------------------------- image cache initialization -------------------------- //
        initImageFetcher();
        
        // ------------------------------------- checkin ------------------------------------ //
        // first check to see if we have a location parameter sent directly
        mLocation = (Location) getIntent().getSerializableExtra("location");
        if (mLocation != null) {
        	displayOrRedirect();
        }
        else {
        	// we must at least have received a checkin URL
        	String checkinUrl = getIntent().getStringExtra(ActionHandler.CHECKIN);
            checkin(checkinUrl);
        }
	}
	
	
	private void initImageFetcher() {
        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(getApplicationContext(), ImageCache.IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(this, 0.0675f); // Set memory cache to 1/16 of mem class
        
        // The ImageFetcher takes care of loading images into ImageViews asynchronously
        mImageFetcher = Envived.getImageFetcherInstance(getSupportFragmentManager(), 
        		cacheParams, R.drawable.placeholder_medium);
	}

	
	@Override
	public void onPause() {
		super.onPause();
		
		Log.d(TAG, " --- onPause called in DetailsActivity");
		active = false;
		
		mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
	}
	
	
	@Override
	public void onStop() {
		Log.d(TAG, " --- onStop called in DetailsActivity");
		super.onStop();
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		
		Log.d(TAG, " --- onResume called in DetailsActivity");
		active = true;
		mImageFetcher.setExitTasksEarly(false);
	}
	
	
	@Override
	public void onStart() {
		Log.d(TAG, " --- onStart called in DetailsActivity");
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
		Log.d(TAG, " --- onDestroy called in DetailsActivity");
		
		// close image fetcher cache
		mImageFetcher.closeCache();
	}
	
	

	public boolean onCreateOptionsMenu(Menu menu) {
		/*
		// add the search button
		MenuItem item = menu.add(getText(R.string.menu_search));
        item.setIcon(R.drawable.ic_menu_search_holder);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

     	*/
		
     	// add temporary test for feature update notification
     	//menu.add("Test Update Feature");
     	
    	return true;
	}
	
	
	public boolean onOptionsItemSelected(MenuItem item) {
		final Context context = getApplicationContext();
		
		if (item.getTitle().toString().compareTo(getString(R.string.menu_search)) == 0) {
			return onSearchRequested();
		}
		
		else if (item.getTitle().toString().compareTo("Test Update Feature") == 0) {
			Intent updateService = new Intent(context, EnvivedFeatureDataRetrievalService.class);
			
			String locationUri = mLocation.getLocationUri();
			EnvivedNotificationContents notificationContents = 
					new EnvivedNotificationContents(locationUri, Feature.ORDER, null, null);
			updateService.putExtra(EnvivedFeatureDataRetrievalService.DATA_RETRIEVE_SERVICE_INPUT, 
					notificationContents);
			
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
	
	
	private void displayOrRedirect() {
		// save/update this location in the LocationHistory cache
		LocationHistory locationHistory = Envived.getLocationHistory();
		String locationType = mLocation.isEnvironment() ? Location.ENVIRONMENT : Location.AREA;
		String locationId = mLocation.getId();
		String locationKey = locationType + "_" + locationId;
		
		// touch the location
		if (locationHistory != null && locationHistory.get(locationKey) == null) {
			locationHistory.put(locationType + "_" + locationId, mLocation);
		}
		
		
		int localFeatures = 0;
		boolean hasBoothDescription = false;
		
		// if the location is an area, see if there is only a booth description feature and/or general features
		// attached to it. If so, redirect to that description activity. 
		if (mLocation.isArea()) {
			for (Entry<String, Feature> featureEntry : mLocation.getFeatures().entrySet()) {
				Feature feature = featureEntry.getValue();
				
				if (!feature.isGeneral()) {
					localFeatures ++;
					if ( feature.getCategory().equals(Feature.BOOTH_DESCRIPTION)) {
						hasBoothDescription = true;
					}
				}
			}
			
			if (localFeatures == 1 && hasBoothDescription) {
				
				// redirect to booth description feature and remove this activity from the backstack
				Intent intent = new Intent(this, BoothDescriptionActivity.class);
				intent.putExtra("location", mLocation);
				startActivity(intent);
				
				finish();
				return;
			}
		}
		
		mActionBar.setTitle(mLocation.getName());
		
		String featureLabelMessage = getResources().getString(R.string.lbl_details_features, mLocation.getName());
		mLabelView.setText(featureLabelMessage);
		
		initializeGrid();
	}
	
	
	private void initializeGrid() {
		// initialize grid
		
		final Map<String, Feature> features = mLocation.getFeatures();
		
		if (features.size() <= 4)
			mGridView.setNumColumns(2);
		else 
			mGridView.setNumColumns(3);
		
		mGridView.setId(0);
		
		LinearLayout.LayoutParams gridViewLayoutParams = 
				new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 
						LinearLayout.LayoutParams.WRAP_CONTENT);
		gridViewLayoutParams.setMargins(5, 5, 5, 5);
		mGridView.setLayoutParams(gridViewLayoutParams);
		mGridView.setColumnWidth(90);
		mGridView.setHorizontalSpacing(10);
		mGridView.setVerticalSpacing(100);
		mGridView.setGravity(Gravity.CENTER);
		mGridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		
		mGridAdapter = new DetailsGridAdapter(this, mLocation);
		mGridView.setAdapter(mGridAdapter);
		
		mMainView.addView(mGridView);
		
		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				//List<String> featureNameList = new ArrayList<String>(features.keySet());
				//Log.d(TAG, "Starting activity for feature: " + featureNameList.get(position));
				String featureCategory = (String) mGridAdapter.getItem(position);
				
				Intent i = null;
				
				if (featureCategory.equals(Location.AREA)) {
					i = new Intent(getApplicationContext(), BrowseLocationsActivity.class);
				}
				else if (featureCategory.equals(Feature.DESCRIPTION)) {
					i = new Intent(getApplicationContext(), DescriptionActivity.class);
				}
				else if (featureCategory.equals(Feature.BOOTH_DESCRIPTION)) {
					i = new Intent(getApplicationContext(), BoothDescriptionActivity.class);
				}
				else if (featureCategory.equals(Feature.PROGRAM)) {
					i = new Intent(getApplicationContext(), ProgramActivity.class);
				}
				
				if (i != null) {
					i.putExtra("location", mLocation);
					startActivity(i);
				}
			}
		});
	}
	
	
	private class CheckinTask extends AsyncTask<Void, Void, ResponseHolder> {
		private ProgressDialog mLoadingDialog;
		private String checkinUrl;
		
		public CheckinTask(String checkinUrl) {
			this.checkinUrl = checkinUrl;
		}

		@Override
		protected void onPreExecute() {
			mLoadingDialog = new ProgressDialog(new ContextThemeWrapper(DetailsActivity.this, R.style.ProgressDialogWhiteText));
			mLoadingDialog.setIndeterminate(true);
			mLoadingDialog.setMessage("Checking in ...");
			mLoadingDialog.setCancelable(true);
			mLoadingDialog.setCanceledOnTouchOutside(false);
			
			mLoadingDialog.show();
		}
		
		@Override
		protected ResponseHolder doInBackground(Void...args) {
			Log.d(TAG, "Checkin URL: " + checkinUrl);
			
			ResponseHolder holder = ActionHandler.checkin(DetailsActivity.this, checkinUrl);
			return holder;
		}
		
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			mLoadingDialog.cancel();
			
			if (!holder.hasError()) {
				if (holder.getCode() == HttpStatus.SC_OK) {
					mLocation = (Location) holder.getTag();
					
					// check to see the type of features for this location and redirect to proper activity
					displayOrRedirect();
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
