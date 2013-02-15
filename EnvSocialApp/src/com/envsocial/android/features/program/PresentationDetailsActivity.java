package com.envsocial.android.features.program;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml.Encoding;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.Envived;
import com.envsocial.android.HomeActivity;
import com.envsocial.android.R;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.utils.Utils;
import com.envsocial.android.utils.imagemanager.ImageCache;
import com.envsocial.android.utils.imagemanager.ImageFetcher;

public class PresentationDetailsActivity extends SherlockFragmentActivity {
	private static final String TAG = "PresentationDetailsActivity";
	private static final String TITLE_TAG = "Presentation Details";
	
	private Location mLocation;
	private ProgramFeature mProgramFeature;
	private ImageFetcher mImageFetcher;
	
	private int mPresentationId;
	private String mLocationUrl;
	private String mLocationName;
	private String mTitle;
	private String mTags;
	private String mAbstract;
	private String mSessionTitle;
	private String mStartTime;
	private String mEndTime;
	private LinkedList<PresentationSpeakerInfo> mSpeakerInfoList;
	
	private TextView mTitleView;
	private TextView mDatetimeView;
	private TextView mSessionView;
	private TextView mLocationNameView;
	private TextView mTagsView;
	private WebView mAbstractView;
	
	private LinearLayout mSpeakersLayout;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setTitle(TITLE_TAG);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		
		// get intent parameters
		mLocation = (Location) getIntent().getExtras().getSerializable("location");
		mProgramFeature = (ProgramFeature)getIntent().getExtras().getSerializable("program_feature");
		mPresentationId = getIntent().getExtras().getInt(ProgramFeature.PRESENTATION_ID);
		
		
		// initialize feature
		try {
			mProgramFeature.init();
		} catch (EnvSocialContentException e) {
			Log.d(TAG, "Error initializing program feature.", e);
		}
		
		setContentView(R.layout.program_presentation_details);
		
		mTitleView = (TextView) findViewById(R.id.title);
		mSessionView = (TextView) findViewById(R.id.session);
		mLocationNameView = (TextView) findViewById(R.id.locationName);
		mDatetimeView = (TextView) findViewById(R.id.datetime);
		mTagsView = (TextView) findViewById(R.id.tags);
		mAbstractView = (WebView) findViewById(R.id.presentation_abstract);
		mAbstractView.getSettings().setBuiltInZoomControls(true);
		
		mSpeakersLayout = (LinearLayout) findViewById(R.id.presentation_speakers_layout);
		
		
		if (mProgramFeature != null && mPresentationId != -1) {
			// initialize url image fetcher
			initImageFetcher();
			
			Cursor presentationDetailsCursor = mProgramFeature.getPresentationDetails(mPresentationId);
			Cursor speakerInfoCursor = mProgramFeature.getPresentationSpeakerInfo(mPresentationId);
			bindData(presentationDetailsCursor, speakerInfoCursor);
		}
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
		
		// cleanup the program feature
		mProgramFeature.doClose(getApplicationContext());
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.presentation_details_menu, menu);
	    return true;
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            Intent intent = new Intent(this, HomeActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
	            return true;
	        case R.id.view_presentation_comments:
	        	// start the presentation comments activity
	        	Intent i = new Intent(this, PresentationCommentsActivity.class);
	        	i.putExtra("presentation_id", mPresentationId);
	        	i.putExtra("presentation_title", mTitle);
	        	i.putExtra("location", mLocation);
	        	startActivity(i);
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	
	private View getSeparatorView() {
		// instantiate speaker separator view
		View v = getLayoutInflater().inflate(R.layout.envived_layout_separator_default, mSpeakersLayout, false);
		return v.findViewById(R.id.layout_separator);
	}
	
	private void bindData(Cursor presentationDetailsCursor, Cursor speakerInfoCursor) {
		if (presentationDetailsCursor != null) {
			
			int idIndex = presentationDetailsCursor.getColumnIndex(ProgramDbHelper.COL_PRESENTATION_ID);
			int titleIndex = presentationDetailsCursor.getColumnIndex(ProgramDbHelper.COL_PRESENTATION_TITLE);
			int tagsIndex = presentationDetailsCursor.getColumnIndex(ProgramDbHelper.COL_PRESENTATION_TAGS);
			int startTimeIndex = presentationDetailsCursor.getColumnIndex(ProgramDbHelper.COL_PRESENTATION_START_TIME);
			int endTimeIndex = presentationDetailsCursor.getColumnIndex(ProgramDbHelper.COL_PRESENTATION_END_TIME);
			int abstractIndex = presentationDetailsCursor.getColumnIndex(ProgramDbHelper.COL_PRESENTATION_ABSTRACT);
			
			int sessionTitleIndex = presentationDetailsCursor.getColumnIndex(ProgramFeature.SESSION);
			int locationUrlIndex = presentationDetailsCursor.getColumnIndex(ProgramDbHelper.COL_SESSION_LOCATION_URL);
			int locationNameIndex = presentationDetailsCursor.getColumnIndex(ProgramDbHelper.COL_SESSION_LOCATION_NAME);
			
			
			// move cursor to first and only entry
			presentationDetailsCursor.moveToFirst();
			
			mPresentationId = presentationDetailsCursor.getInt(idIndex);
			mLocationUrl = presentationDetailsCursor.getString(locationUrlIndex);
			mLocationName = presentationDetailsCursor.getString(locationNameIndex);
			mTitle = presentationDetailsCursor.getString(titleIndex);
			mSessionTitle = presentationDetailsCursor.getString(sessionTitleIndex);
			mStartTime = presentationDetailsCursor.getString(startTimeIndex);
			mEndTime = presentationDetailsCursor.getString(endTimeIndex);
			
			if (!presentationDetailsCursor.isNull(tagsIndex)) {
				mTags = presentationDetailsCursor.getString(tagsIndex);
			}
			else {
				mTags = null;
			}
			
			if (!presentationDetailsCursor.isNull(abstractIndex)) {
				mAbstract = presentationDetailsCursor.getString(abstractIndex);
			}
			else {
				mAbstract = null;
			}
			
			// ======================= binding ========================
			mTitleView.setText(mTitle);
			
			Calendar startDate = null;
			try {
				startDate = Utils.stringToCalendar(mStartTime, "yyyy-MM-dd'T'HH:mm:ss");
			} catch (ParseException e) {
				Log.d(TAG, "Error parsing presentation start time string: " + mStartTime);
			}
			
			if (startDate != null) {
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		        String startHour = sdf.format(startDate.getTime());
		        
		        sdf.applyPattern("dd MMM yyyy");
		        String startDay = sdf.format(startDate.getTime());
				mDatetimeView.setText(startHour + ",  " + startDay);
			}
			else {
				mDatetimeView.setText("Unknown");
			}
			
			mSessionView.setText(mSessionTitle);
			mLocationNameView.setText(mLocationName);
			
			if (mTags != null) {
				mTagsView.setText(mTags.replace(";", ", "));
			}
			
			if (mAbstract != null) {
				try {
					mAbstractView.loadData(URLEncoder.encode(mAbstract, "UTF-8").replaceAll("\\+", " "), "text/html", Encoding.UTF_8.toString());
				} catch (UnsupportedEncodingException e) {
					Log.d(TAG, "ERROR loading presentation abstract in WebView.", e);
				}
			}
			else {
				String message = "No abstract available";
				mAbstractView.loadData(message, "text/html", Encoding.UTF_8.toString());
			}
		}
		
		if (speakerInfoCursor != null) {
			// consume speaker info cursor
			mSpeakerInfoList = new LinkedList<PresentationDetailsActivity.PresentationSpeakerInfo>();
			int speakerIdIndex = speakerInfoCursor.getColumnIndex(ProgramDbHelper.COL_SPEAKER_ID);
			int speakerFirstNameIndex = speakerInfoCursor.getColumnIndex(ProgramDbHelper.COL_SPEAKER_FIRST_NAME);
			int speakerLastNameIndex = speakerInfoCursor.getColumnIndex(ProgramDbHelper.COL_SPEAKER_LAST_NAME);
			int speakerImageUrlIndex = speakerInfoCursor.getColumnIndex(ProgramDbHelper.COL_SPEAKER_IMAGE_URL);
			
			while(speakerInfoCursor.moveToNext()) {
				int speakerId = speakerInfoCursor.getInt(speakerIdIndex);
				String speakerFirstName = speakerInfoCursor.getString(speakerFirstNameIndex);
				String speakerLastName = speakerInfoCursor.getString(speakerLastNameIndex);
				String speakerImageUrl = null;
				
				if (!speakerInfoCursor.isNull(speakerImageUrlIndex)) {
					speakerImageUrl = speakerInfoCursor.getString(speakerImageUrlIndex);
				}
				
				mSpeakerInfoList.add(
						new PresentationSpeakerInfo(speakerId, speakerImageUrl, speakerFirstName, speakerLastName));
			}
			
			setupSpeakerViews();
		}
	}
	
	private void setupSpeakerViews() {
		
		int len = mSpeakerInfoList.size();
		for (int i = 0; i < len; i++) {
			PresentationSpeakerInfo speakerInfo = mSpeakerInfoList.get(i); 
					
			View speakerInfoRowView = getLayoutInflater().
					inflate(R.layout.program_presentation_details_speaker_row, mSpeakersLayout, false);
			
			ImageView speakerImageView = (ImageView) speakerInfoRowView.findViewById(R.id.presentation_details_speaker_row_image);
			TextView speakerImageName = (TextView) speakerInfoRowView.findViewById(R.id.presentation_details_speaker_row_name);
		
			if (mImageFetcher != null && speakerInfo.getImageUrl() != null) {
				mImageFetcher.loadImage(speakerInfo.getImageUrl(), speakerImageView);
			}
			
			speakerImageName.setText(speakerInfo.getFirstName() + " " + speakerInfo.getLastName());
			speakerImageName.setOnClickListener(new PresentationSpeakerClickListener(speakerInfo.getSpeakerId()));
		
			// add speaker row view speaker layout
			mSpeakersLayout.addView(speakerInfoRowView);
			
			if (i != len - 1) {
				mSpeakersLayout.addView(getSeparatorView());
			}
		}
	}
	
	
	private void initImageFetcher() {
		ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(getApplicationContext(), ImageCache.IMAGE_CACHE_DIR);
        cacheParams.memoryCacheEnabled = false;
        
        // The ImageFetcher takes care of loading images into ImageViews asynchronously
        mImageFetcher = Envived.getImageFetcherInstance(getSupportFragmentManager(), 
        		cacheParams, R.drawable.placeholder_medium);
	}
	
	
	private class PresentationSpeakerClickListener implements OnClickListener {
		private int mSpeakerId;
		
		PresentationSpeakerClickListener(int speakerId) {
			mSpeakerId = speakerId;
		}
		
		@Override
		public void onClick(View v) {
			Log.d(TAG, "Launching speaker details activity for speakerId: " + mSpeakerId);
			Intent i = new Intent(PresentationDetailsActivity.this, SpeakerDetailsActivity.class);
			Bundle extras = new Bundle();
			extras.putInt(ProgramFeature.SPEAKER_ID, mSpeakerId);
			extras.putSerializable("program_feature", mProgramFeature);
			
			i.putExtras(extras);
			startActivity(i);
		}
	}
	
	
	static class PresentationSpeakerInfo implements Serializable {
		int mSpeakerId;

		String mImageUrl;
		String mFirstName;
		String mLastName;
		
		public PresentationSpeakerInfo(int speakerId, String imageUrl, String firstName, String lastName) {
			mSpeakerId = speakerId;
			mImageUrl = imageUrl;
			mFirstName = firstName;
			mLastName = lastName;
		}
		
		public int getSpeakerId() {
			return mSpeakerId;
		}


		public String getImageUrl() {
			return mImageUrl;
		}

		public String getFirstName() {
			return mFirstName;
		}

		public String getLastName() {
			return mLastName;
		}
	}
	
}
