package com.envsocial.android.features.description;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

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
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.Envived;
import com.envsocial.android.R;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.api.EnvSocialResource;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.Url;
import com.envsocial.android.api.exceptions.EnvSocialComException;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.features.program.PresentationCommentsDialogFragment;
import com.envsocial.android.utils.EnvivedCommentAlertDialog;
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.ResponseHolder;
import com.envsocial.android.utils.imagemanager.ImageCache;
import com.envsocial.android.utils.imagemanager.ImageFetcher;

public class BoothDescriptionProductDetailsActivity extends SherlockFragmentActivity 
	implements OnClickListener {
	
	private static final String TAG = "BoothDescriptionProductDetailsActivity";
	private static final String TITLE_TAG = "Project Description";
	
	private Location mLocation;
	private BoothDescriptionFeature mDescriptionFeature;
	private int mProductId;
	
	private String mBoothProductName;
	private String mBoothProductDescription;
	
	private LinearLayout mBoothProductLayout;
	private TextView mBoothNameView;
	private TextView mBoothProductNameView;
	private TextView mBoothProductVotesView;
	private ImageButton mBoothProductVoteButton;
	private WebView mBoothProductDescriptionView;
	
	
	//private ActionBar mActionBar;
	private ImageFetcher mImageFetcher;
	private ProgressDialog mFeatureLoadingDialog;
	private InitializeBoothProductTask mInitBoothProductTask;
	private UpdateProductVotesTask mUpdateVotesTask;
	private SendVoteTask mSendVoteTask;
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setTitle(TITLE_TAG);
		
		// retrieve the intent data
		mLocation = (Location) getIntent().getSerializableExtra("location");
		mDescriptionFeature = (BoothDescriptionFeature) getIntent().getSerializableExtra(Feature.BOOTH_DESCRIPTION);
		mProductId = getIntent().getIntExtra("product_id", -1);
		
		setContentView(R.layout.description_booth_product_detais);
		mBoothProductLayout = (LinearLayout) findViewById(R.id.description_product_details_layout);
		mBoothNameView = (TextView) findViewById(R.id.description_product_details_location);
		mBoothProductNameView = (TextView) findViewById(R.id.description_product_details_name);
		mBoothProductVotesView = (TextView) findViewById(R.id.description_product_details_votes);
		mBoothProductDescriptionView = (WebView) findViewById(R.id.description_product_details_description);
		mBoothProductDescriptionView.getSettings().setBuiltInZoomControls(true);
		
		mBoothProductVoteButton = (ImageButton) findViewById(R.id.description_product_details_vote_button);
		mBoothProductVoteButton.setOnClickListener(this);
		
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
		
		if (mInitBoothProductTask != null) {
			mInitBoothProductTask.cancel(false);
			mInitBoothProductTask = null;
		}
		
		if (mUpdateVotesTask != null) {
			mUpdateVotesTask.cancel(true);
			mUpdateVotesTask = null;
		}
		
		if (mSendVoteTask != null) {
			mSendVoteTask.cancel(true);
			mSendVoteTask = null;
		}
		
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
		
		return super.onOptionsItemSelected(item);
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
	
	
	@Override
	public void onClick(View v) {
		if (v == mBoothProductVoteButton) {
			Location checkedInLocation = Preferences.getCheckedInLocation(this);
			if (checkedInLocation == null || !checkedInLocation.getLocationUri().equals(mLocation.getLocationUri())) {
				String message = "You have to be checked in at this location (scan the QRcode) " +
    					"to be able to vote projects up.";
				EnvivedCommentAlertDialog alertDialog = EnvivedCommentAlertDialog.newInstance(message);
				alertDialog.show(getSupportFragmentManager(), "comment_alert_dialog");
        	}
			else {
				JSONObject voteContent = new JSONObject();
				try {
					voteContent.put("product_id", mProductId);
				} catch (JSONException e) {
					Log.d(TAG, "Error voting up product: " + mProductId, e);
					return;
				}
				
				Annotation voteRequest = new Annotation(mLocation, "booth_product_vote", Calendar.getInstance(), voteContent.toString());
				mSendVoteTask = new SendVoteTask(this, voteRequest);
				mSendVoteTask.execute();
			}
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
				
				mUpdateVotesTask = new UpdateProductVotesTask();
				mUpdateVotesTask.execute();
			}
		}
	}
	
	
	private class UpdateProductVotesTask extends AsyncTask<Void, Void, Integer> {
		@Override
		protected Integer doInBackground(Void... params) {
			try {
				Map<String, String> extra = new HashMap<String, String>();
				extra.put("product_id", String.valueOf(mProductId));
				
				List<Annotation> voteAnnotations = Annotation.getAnnotations(BoothDescriptionProductDetailsActivity.this, 
						mLocation, "booth_product_vote", extra, 0, 1); 
				
				
				if (!voteAnnotations.isEmpty()) {
					// there should be only one
					Annotation vote = voteAnnotations.get(0);
					try {
						JSONObject voteData = new JSONObject(vote.getData());
						int productVotes = voteData.getInt("product_votes");
						
						mDescriptionFeature.updateVotesValue(mProductId, productVotes);
						return productVotes;
					} catch (JSONException e) {
						throw new EnvSocialContentException(vote.getData(), EnvSocialResource.ANNOTATION, e);
					}
				}
				
				return 0;
			} catch (EnvSocialContentException e) {
				Log.d(TAG, "ERROR retrieving vote annotation for product id: " + mProductId, e);
				return null;
			} catch (EnvSocialComException e) {
				Log.d(TAG, "ERROR retrieving vote annotation for product id: " + mProductId, e);
				return null;
			}
		}
		
		
		@Override
		protected void onPostExecute(Integer productVotes) {
			if (productVotes != null) {
				mBoothProductVotesView.setText("" + productVotes);
			}
			else {
				Toast toast = Toast.makeText(BoothDescriptionProductDetailsActivity.this, 
						"Could not update project votes.", Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}
	
	
	private class SendVoteTask extends AsyncTask<Void, Void, ResponseHolder> {
		private static final String TAG = "SendVoteTask";
		
		private Context mContext;
		private boolean error = true;
		
		private Annotation mVoteRequest;
		
		public SendVoteTask(Context context, Annotation voteRequest) {
			mContext = context;
			mVoteRequest = voteRequest;
		}
		
		@Override
		protected ResponseHolder doInBackground(Void...args) {
			ResponseHolder holder = mVoteRequest.post(mContext);
			
			if (!holder.hasError() && 
				(holder.getCode() == HttpStatus.SC_CREATED || holder.getCode() == HttpStatus.SC_ACCEPTED )) {
				
				try {
					Annotation responseVote = Annotation.parseAnnotation(mContext, mLocation, holder.getJsonContent());
					int voteCount = new JSONObject(responseVote.getData()).getInt("product_votes");
					
					if (mDescriptionFeature.isInitialized()) {
						mDescriptionFeature.updateVotesValue(mProductId, voteCount);
					}
				} catch (JSONException e) {
					Log.d(TAG, "Error updating product vote count after vote-up.", e);
				} catch (ParseException e) {
					Log.d(TAG, "Error updating product vote count after vote-up.", e);
				}
			}
			
			return holder;
		}
		
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			if (!holder.hasError()) {
				error = false;
				int msgId = R.string.msg_send_vote_ok;
				
				switch(holder.getCode()) {
					case HttpStatus.SC_CREATED:
					case HttpStatus.SC_ACCEPTED:
						error = false;
						break;
					case HttpStatus.SC_NO_CONTENT:
					case HttpStatus.SC_FORBIDDEN:
						msgId = R.string.msg_send_vote_duplicate_err;
						error = true;
						break;
					default:
						msgId = R.string.msg_send_vote_err;
						error = true;
						break;
				}
				
				if (error) {
					Log.d(TAG, "response code: " + holder.getCode() + " response body: " + holder.getResponseBody());
					Toast toast = Toast.makeText( mContext, msgId, Toast.LENGTH_LONG);
					toast.show();
				}
				else {
					try {
						Annotation responseVote = Annotation.parseAnnotation(mContext, mLocation, holder.getJsonContent());
						int voteCount = new JSONObject(responseVote.getData()).getInt("product_votes");
						
						mBoothProductVotesView.setText(String.valueOf(voteCount));
					} catch (JSONException e) {
						Log.d(TAG, "Error updating product vote count after vote-up.", e);
					} catch (ParseException e) {
						Log.d(TAG, "Error updating product vote count after vote-up.", e);
					}
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
					msgId = R.string.msg_service_error;
				} catch (Exception e) {
					Log.d(TAG, e.toString(), e);
					msgId = R.string.msg_service_error;
				}

				Toast toast = Toast.makeText(mContext, msgId, Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}

}
