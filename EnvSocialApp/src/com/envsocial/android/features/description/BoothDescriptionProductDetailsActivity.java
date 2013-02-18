package com.envsocial.android.features.description;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml.Encoding;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.CommentsActivity;
import com.envsocial.android.Envived;
import com.envsocial.android.R;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.Url;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.imagemanager.ImageCache;
import com.envsocial.android.utils.imagemanager.ImageFetcher;

public class BoothDescriptionProductDetailsActivity extends SherlockFragmentActivity {
	private static final String TAG = "BoothDescriptionProductDetailsActivity";
	
	private Location mLocation;
	private BoothDescriptionFeature mDescriptionFeature;
	private int mProductId;
	
	private String mBoothProductName;
	private String mBoothProductDescription;
	
	private LinearLayout mBoothProductLayout;
	private TextView mBoothNameView;
	private TextView mBoothProductNameView;
	private WebView mBoothProductDescriptionView;
	
	
	//private ActionBar mActionBar;
	private ImageFetcher mImageFetcher;
	private ProgressDialog mFeatureLoadingDialog;
	private InitializeBoothProductTask mInitBoothProductTask;
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		
		// retrieve the intent data
		mLocation = (Location) getIntent().getSerializableExtra("location");
		mDescriptionFeature = (BoothDescriptionFeature) getIntent().getSerializableExtra(Feature.BOOTH_DESCRIPTION);
		mProductId = getIntent().getIntExtra("product_id", -1);
		
		setContentView(R.layout.description_booth_product_detais);
		mBoothProductLayout = (LinearLayout) findViewById(R.id.description_product_details_layout);
		mBoothNameView = (TextView) findViewById(R.id.description_product_details_location);
		mBoothProductNameView = (TextView) findViewById(R.id.description_product_details_name);
		mBoothProductDescriptionView = (WebView) findViewById(R.id.description_product_details_description);
		mBoothProductDescriptionView.getSettings().setBuiltInZoomControls(true);
		
		// call feature initialization task here
		mInitBoothProductTask = new InitializeBoothProductTask(mDescriptionFeature);
		mInitBoothProductTask.execute();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		if (mImageFetcher != null) {
			mImageFetcher.setExitTasksEarly(true);
			mImageFetcher.flushCache();
		}
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (mImageFetcher != null) {
			mImageFetcher.setExitTasksEarly(false);
		}
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// close image fetcher cache
		if (mImageFetcher != null) {
			mImageFetcher.closeCache();
		}
		
		if (mFeatureLoadingDialog != null) {
			mFeatureLoadingDialog.cancel();
			mFeatureLoadingDialog = null;
		}
		
		// cleanup the program feature
		mDescriptionFeature.doClose(getApplicationContext());
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
			intent.putExtra("productName", mBoothProductName);
			String boothId = Url.resourceIdFromUrl(mDescriptionFeature.getResourceUri());
			Cursor productsCursor = mDescriptionFeature.getAllProducts(Integer.parseInt(boothId));

			ArrayList<String> filterItemsList = new ArrayList<String>();

			while (productsCursor.moveToNext()) {
				int nameIndex = productsCursor.getColumnIndex(BoothDescriptionDbHelper.COL_BOOTH_PRODUCT_NAME);
				String productName = productsCursor.getString(nameIndex);
				filterItemsList.add(productName);
			}
			filterItemsList.add(mLocation.getName());

			String[] filterItems = filterItemsList.toArray(new String[filterItemsList.size()]);
			
			intent.putExtra("filterItems", filterItems);
			startActivity(intent);
			
			return true;
		}
		
		return false;
	}
	
	
	private void initImageFetcher() {
		ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(getApplicationContext(), ImageCache.IMAGE_CACHE_DIR);
        cacheParams.memoryCacheEnabled = false;
        
        // The ImageFetcher takes care of loading images into ImageViews asynchronously
        mImageFetcher = Envived.getImageFetcherInstance(getSupportFragmentManager(), 
        		cacheParams, R.drawable.placeholder_medium);
	}
	
	
	private ProgressDialog createFeatureLoadingDialog(Context context, String message) {
		ProgressDialog pd = new ProgressDialog(new ContextThemeWrapper(context, R.style.ProgressDialogWhiteText));
		pd.setIndeterminate(true);
		pd.setMessage(message);
		pd.setCancelable(true);
		pd.setCanceledOnTouchOutside(false);
		
		return pd;
	}
	
	
	private void bindData(Cursor cursor) {
		int productNameIndex = cursor.getColumnIndex(BoothDescriptionDbHelper.COL_BOOTH_PRODUCT_NAME);
		int productDescriptionIndex = cursor.getColumnIndex(BoothDescriptionDbHelper.COL_BOOTH_PRODUCT_DESCRIPTION);
		int productImageIndex = cursor.getColumnIndex(BoothDescriptionDbHelper.COL_BOOTH_PRODUCT_IMAGE_URL);
		int productWebsiteIndex = cursor.getColumnIndex(BoothDescriptionDbHelper.COL_BOOTH_PRODUCT_WEBSITE_URL);
		
		mBoothNameView.setText(mLocation.getName());
		
		mBoothProductName = cursor.getString(productNameIndex);
		mBoothProductNameView.setText(mBoothProductName);
		
		if (!cursor.isNull(productDescriptionIndex)) {
			mBoothProductDescription = cursor.getString(productDescriptionIndex);
			
			try {
				mBoothProductDescriptionView.loadData(URLEncoder.encode(mBoothProductDescription, "UTF-8").replaceAll("\\+", " "), "text/html", Encoding.UTF_8.toString());
			} catch (UnsupportedEncodingException e) {
				Log.d(TAG, "ERROR loading booth description in WebView.", e);
			}
		}
		else {
			String message = "No project description available";
			mBoothProductDescriptionView.loadData(message, "text/html", Encoding.UTF_8.toString());
		}
		
		if (!cursor.isNull(productImageIndex)) {
			String boothProductImageUrl = cursor.getString(productImageIndex);
			
			View v = getLayoutInflater().inflate(R.layout.description_booth_product_details_image, mBoothProductLayout, false);
			LinearLayout imageLayout = (LinearLayout) v.findViewById(R.id.description_product_details_image_layout);
			ImageView imageView = (ImageView) v.findViewById(R.id.description_product_details_image);
			
			initImageFetcher();
			mImageFetcher.loadImage(boothProductImageUrl, imageView);
			
			mBoothProductLayout.addView(imageLayout);
		}
		
		if (!cursor.isNull(productWebsiteIndex)) {
			String boothProductWebsiteUrl = cursor.getString(productWebsiteIndex);
			
			View v = getLayoutInflater().inflate(R.layout.description_booth_product_details_website, mBoothProductLayout, false);
			LinearLayout websiteLayout = (LinearLayout) v.findViewById(R.id.description_product_details_website_layout);
			TextView websiteUrlView = (TextView) v.findViewById(R.id.description_product_details_website);
			websiteUrlView.setText(boothProductWebsiteUrl);
			
			mBoothProductLayout.addView(websiteLayout);
		}
	}
	
	private class InitializeBoothProductTask extends AsyncTask<Void, Void, Cursor> {
		private BoothDescriptionFeature mNewFeature;
		
		InitializeBoothProductTask(BoothDescriptionFeature feature) {
			mNewFeature = feature;
		}
		
		@Override
		protected void onPreExecute() {
			String message = "Initializing Data ...";
			
			mFeatureLoadingDialog = createFeatureLoadingDialog(BoothDescriptionProductDetailsActivity.this, message);
			mFeatureLoadingDialog.show();
		}
		
		@Override
		protected Cursor doInBackground(Void... params) {
			try {
				mNewFeature.init();
				
				if (mNewFeature.hasData() && mNewFeature.isInitialized() && mProductId != -1) {
					return mNewFeature.getProductData(mProductId);
				}
				
				return null;
			} catch (EnvSocialContentException e) {
				Log.d(TAG, "ERROR initializing feature " + mNewFeature.getCategory(), e);
				return null;
			}
		}
		
		
		@Override
		protected void onPostExecute(Cursor cursor) {
			if (mFeatureLoadingDialog != null) {
				mFeatureLoadingDialog.cancel();
				mFeatureLoadingDialog = null;
			}
			
			// on successful initialization / update set newly initialized feature as current one
			if (cursor != null) {
				mDescriptionFeature = mNewFeature;
				cursor.moveToFirst();
				
				bindData(cursor);
			}
		}
	}
}
