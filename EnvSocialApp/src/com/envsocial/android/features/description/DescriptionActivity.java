package com.envsocial.android.features.description;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.envsocial.android.Envived;
import com.envsocial.android.R;
import com.envsocial.android.api.EnvSocialResource;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.LocationContextManager;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.EnvivedFeatureActivity;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.ResponseHolder;
import com.envsocial.android.utils.imagemanager.ImageCache;
import com.envsocial.android.utils.imagemanager.ImageFetcher;
import com.envsocial.android.utils.imagemanager.ImageWorker;

public class DescriptionActivity extends EnvivedFeatureActivity {
	private static final String TAG = "DescriptionActivity";
	
	private DescriptionFeature mDescriptionFeature;
	
	private TextView mDescriptionDetailsView;
	private TextView mDescriptionNewInfoView;
	private TextView mDescriptionPeopleCount;
	private ImageView mLogoImageView;
	
	private ImageFetcher mImageFetcher;
	private CheckedInCountTask mPeopleCountTask;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.description);
	    
 		mLogoImageView = (ImageView) findViewById(R.id.description_image);
 		mDescriptionDetailsView = (TextView) findViewById(R.id.description_details);
 		mDescriptionPeopleCount = (TextView) findViewById(R.id.description_people_count);
 		mDescriptionNewInfoView = (TextView) findViewById(R.id.description_new_info);
 		
 		initImageFetcher();
 		// bindDescriptionViewData();
	}

	
	private void bindDescriptionViewData() {
		mDescriptionDetailsView.setText(mDescriptionFeature.getDescriptionText());
		mDescriptionNewInfoView.setText(mDescriptionFeature.getNewestInfoText());
		
		String logoImageUrl = mDescriptionFeature.getLogoImageUri();
        
        if (logoImageUrl != null) {
        	if (mImageFetcher != null) {
        		mImageFetcher.loadImage(logoImageUrl, mLogoImageView);
        	}
        }
        else {
        	// TODO - smart loading to take into account image display sizes
        	Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.placeholder);
        	if (logoBitmap != null) {
        		mLogoImageView.setImageBitmap(logoBitmap);
        	}
        }    
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
	
	
	@Override
	public void onPause() {
		super.onPause();
		
		mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		mImageFetcher.setExitTasksEarly(false);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		//Log.d(TAG, "onStart in DescriptionFragment");
		
		mPeopleCountTask = new CheckedInCountTask(); 
		mPeopleCountTask.execute(mLocation);
		mPeopleCountTask = null;
		
		// check if due to delayed onDestroy (can happen from Notification relaunch) 
		// the image fetcher has closed the cache after it was opened again
		if (mImageFetcher == null || !mImageFetcher.cashOpen()) {
			initImageFetcher();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (mPeopleCountTask != null) {
			mPeopleCountTask.cancel(true);
			mPeopleCountTask = null;
		}
		
		// close image fetcher cache
		mImageFetcher.closeCache();
		
		if (mLogoImageView != null) {
            // Cancel any pending image work
            ImageWorker.cancelWork(mLogoImageView);
            mLogoImageView.setImageDrawable(null);
        }
	}
	
	
	private class CheckedInCountTask extends  AsyncTask<Location, Void, ResponseHolder> {
		
		@Override
		protected ResponseHolder doInBackground(Location... params) {
			// we only pass in one parameter
			Location currentLocation = params[0];
			LocationContextManager locationContextMgr = currentLocation.getContextManager();
			
			if (locationContextMgr != null) {
				return locationContextMgr.getUserCount(getApplicationContext());
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			if (holder != null && !holder.hasError()) {
				if (holder.getCode() == HttpStatus.SC_OK) {
					Context context = getApplicationContext();
					
					try {
						JSONObject dataJSON = holder.getJsonContent();
						
						// response is just the integer count
						int peopleCount = dataJSON.getInt("response");
						
						// Display a toast message and update the view
						mDescriptionPeopleCount.setText("Checked in with " + (peopleCount - 1) + " other users.");
						
						
					} catch (JSONException e) {
						//Log.d(TAG, holder.getResponseBody(), e);
					} 
				}
				// else fail silently - no update
			}
			else {
				//Log.d(TAG, holder.getResponseBody(), holder.getError());
			}
		}
	}
	


	@Override
	protected Feature getLocationFeature(Location location) throws EnvSocialContentException {
		// return the Description feature associated with the given location
		
		Feature descriptionFeature = location.getFeature(Feature.DESCRIPTION);
		if (descriptionFeature == null) {
			EnvSocialResource locationResource = 
					location.isEnvironment() ? EnvSocialResource.ENVIRONMENT : EnvSocialResource.AREA;
			throw new EnvSocialContentException(location.serialize(), locationResource, null);
		}
		
		return descriptionFeature;
	}

	@Override
	protected void onFeatureDataInitialized(Feature newFeature, boolean success) {
		if (success) {
			mDescriptionFeature = (DescriptionFeature) newFeature;
			bindDescriptionViewData();
		}
	}

	@Override
	protected void onFeatureDataUpdated(Feature updatedFeature, boolean success) {
		if (success) {
			mDescriptionFeature = (DescriptionFeature) updatedFeature;
			bindDescriptionViewData();
		}
	}

	@Override
	protected String getActiveUpdateDialogMessage() {
		return "The details of this location have been modified. This update will refresh your view " +
				"of the data. Please select YES if you wish to perform the update. Select NO if, you " +
				"want to keep your current browsing session. The update will be performed next time you " +
				"start this activity.";
	}
}
