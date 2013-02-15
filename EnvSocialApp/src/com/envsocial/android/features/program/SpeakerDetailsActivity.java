package com.envsocial.android.features.program;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
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
import com.envsocial.android.Envived;
import com.envsocial.android.R;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.utils.Utils;
import com.envsocial.android.utils.imagemanager.ImageCache;
import com.envsocial.android.utils.imagemanager.ImageFetcher;

public class SpeakerDetailsActivity extends SherlockFragmentActivity {
	private static final String TAG = "SpeakerDetailsActivity";
	
	private ProgramFeature mProgramFeature;
	private ImageFetcher mImageFetcher;
	
	private int mSpeakerId;
	private LinkedList<SpeakerPresentationInfo> mPresentationInfoList;
	
	private TextView mNameView;
	private TextView mPositionView;
	private TextView mAffiliationView;
	private ImageView mSpeakerImageView;
	private WebView mBiographyView;
	private TextView mEmailView;
	private TextView mOnlineProfileView;
	
	private LinearLayout mPresentationsLayout;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//getSupportActionBar().setDisplayShowTitleEnabled(false);
		
		if (savedInstanceState != null) {
			mProgramFeature = (ProgramFeature)savedInstanceState.getSerializable("program_feature");
			mSpeakerId = savedInstanceState.getInt(ProgramFeature.SPEAKER_ID);
		}
		else {
			mProgramFeature = (ProgramFeature)getIntent().getExtras().getSerializable("program_feature");
			mSpeakerId = getIntent().getExtras().getInt(ProgramFeature.SPEAKER_ID);
		}
		
		// initialize feature
		try {
			mProgramFeature.init();
		} catch (EnvSocialContentException e) {
			Log.d(TAG, "Error initializing program feature.", e);
		}
		
		setContentView(R.layout.program_speaker_details);
		
		mNameView = (TextView) findViewById(R.id.speaker_name);
		mPositionView = (TextView) findViewById(R.id.speaker_position);
		mAffiliationView = (TextView) findViewById(R.id.speaker_affiliation);
		mBiographyView = (WebView) findViewById(R.id.speaker_biography);
		mBiographyView.getSettings().setBuiltInZoomControls(true);
		
		mSpeakerImageView = (ImageView) findViewById(R.id.speaker_image);
		mEmailView = (TextView) findViewById(R.id.speaker_email);
		mOnlineProfileView = (TextView) findViewById(R.id.speaker_online_profile_link);
		
		mPresentationsLayout = (LinearLayout) findViewById(R.id.speaker_presentations_layout);
		
		
		// initialize url image fetcher
		initImageFetcher();
		
		Cursor speakerDetailsCursor = mProgramFeature.getSpeakerDetails(mSpeakerId);
		Cursor speakerPresentationsCursor = mProgramFeature.getSpeakerPresentationsInfo(mSpeakerId);
		bindData(speakerDetailsCursor, speakerPresentationsCursor);
	}
	
	private View getSeparatorView() {
		// instantiate speaker separator view
		View v = getLayoutInflater().inflate(R.layout.envived_layout_separator_default, mPresentationsLayout, false);
		return v.findViewById(R.id.layout_separator);
	}
	
	private void bindData(Cursor speakerDetailsCursor, Cursor speakerPresentationsCursor) {
		if (speakerDetailsCursor != null) {
			
			int firstNameIndex = speakerDetailsCursor.getColumnIndex(ProgramDbHelper.COL_SPEAKER_FIRST_NAME);
			int lastNameIndex = speakerDetailsCursor.getColumnIndex(ProgramDbHelper.COL_SPEAKER_LAST_NAME);
			int affiliationIndex = speakerDetailsCursor.getColumnIndex(ProgramDbHelper.COL_SPEAKER_AFFILIATION);
			int positionIndex = speakerDetailsCursor.getColumnIndex(ProgramDbHelper.COL_SPEAKER_POSITION);
			int biographyIndex = speakerDetailsCursor.getColumnIndex(ProgramDbHelper.COL_SPEAKER_BIOGRAPHY);
			int emailIndex = speakerDetailsCursor.getColumnIndex(ProgramDbHelper.COL_SPEAKER_EMAIL);
			int onlineProfileIndex = speakerDetailsCursor.getColumnIndex(ProgramDbHelper.COL_SPEAKER_ONLINE_PROFILE_LINK);
			int imageUrlIndex = speakerDetailsCursor.getColumnIndex(ProgramDbHelper.COL_SPEAKER_IMAGE_URL);
			
			
			// move cursor to first and only entry
			speakerDetailsCursor.moveToFirst();
			
			String firstName = speakerDetailsCursor.getString(firstNameIndex);
			String lastName = speakerDetailsCursor.getString(lastNameIndex);
			String affiliation = speakerDetailsCursor.getString(affiliationIndex);
			String position = speakerDetailsCursor.getString(positionIndex);
			
			String biography = null;
			if (!speakerDetailsCursor.isNull(biographyIndex)) {
				biography = speakerDetailsCursor.getString(biographyIndex);
			}
			
			String email = null;
			if (!speakerDetailsCursor.isNull(emailIndex)) {
				email = speakerDetailsCursor.getString(emailIndex);
			}
			
			String onlineProfileLink = null;
			if (!speakerDetailsCursor.isNull(onlineProfileIndex)) {
				onlineProfileLink = speakerDetailsCursor.getString(onlineProfileIndex);
			}
			
			String imageUrl = null;
			if (!speakerDetailsCursor.isNull(imageUrlIndex)) {
				imageUrl = speakerDetailsCursor.getString(imageUrlIndex);
			}
			
			
			// ======================= binding ========================
			mNameView.setText(firstName + " " + lastName);
			mPositionView.setText(position);
			mAffiliationView.setText(affiliation);
			
			if (biography != null) {
				try {
					mBiographyView.loadData(URLEncoder.encode(biography, "UTF-8").replaceAll("\\+", " "), "text/html", Encoding.UTF_8.toString());
				} catch (UnsupportedEncodingException e) {
					Log.d(TAG, "ERROR loading speaker biography in WebView.", e);
				}
			}
			else {
				String message = "No biography available";
				mBiographyView.loadData(message, "text/html", Encoding.UTF_8.toString());
			}
			
			if (email != null) {
				mEmailView.setText(email);
			}
			
			if (onlineProfileLink != null) {
				mOnlineProfileView.setText(onlineProfileLink);
			}
			
			if (imageUrl != null) {
				mImageFetcher.loadImage(imageUrl, mSpeakerImageView);
			}
		}
		
		if (speakerPresentationsCursor != null) {
			// consume speaker info cursor
			mPresentationInfoList = new LinkedList<SpeakerPresentationInfo>();
			
			int presentationIdIndex = speakerPresentationsCursor.getColumnIndex(ProgramDbHelper.COL_PRESENTATION_ID);
			int presentationTitleIndex = speakerPresentationsCursor.getColumnIndex(ProgramDbHelper.COL_PRESENTATION_TITLE);
			int presentationStartTimeIndex = speakerPresentationsCursor.getColumnIndex(ProgramDbHelper.COL_PRESENTATION_START_TIME);
			int presentationEndTimeIndex = speakerPresentationsCursor.getColumnIndex(ProgramDbHelper.COL_PRESENTATION_END_TIME);
			int presentationLocationNameIndex = speakerPresentationsCursor.getColumnIndex(ProgramDbHelper.COL_SESSION_LOCATION_NAME);
			
			while(speakerPresentationsCursor.moveToNext()) {
				int presentationId = speakerPresentationsCursor.getInt(presentationIdIndex);
				String title = speakerPresentationsCursor.getString(presentationTitleIndex);
				String startTime = speakerPresentationsCursor.getString(presentationStartTimeIndex);
				String endTime = speakerPresentationsCursor.getString(presentationEndTimeIndex);
				String locationName = speakerPresentationsCursor.getString(presentationLocationNameIndex);
				
				mPresentationInfoList.add(
						new SpeakerPresentationInfo(presentationId, title, startTime, endTime, locationName));
			}
			
			setupPresentationViews();
		}
	}
	
	
	private void setupPresentationViews() {
		int len = mPresentationInfoList.size();
		
		for (int i = 0; i < len; i++) {
			SpeakerPresentationInfo presentationInfo = mPresentationInfoList.get(i);
			View presentationInfoRowView = getLayoutInflater().
					inflate(R.layout.program_speaker_details_presentation_row, mPresentationsLayout, false);
			
			TextView titleView = (TextView) presentationInfoRowView.findViewById(R.id.speaker_details_presentation_row_title);
			TextView timeView = (TextView) presentationInfoRowView.findViewById(R.id.speaker_details_presentation_row_time);
			TextView locationView = (TextView) presentationInfoRowView.findViewById(R.id.speaker_details_presentation_row_locationName);
			
			
			titleView.setText(presentationInfo.getTitle());
			titleView.setOnClickListener(new SpeakerPresentationClickListener(presentationInfo.getPresentationId()));
			
			locationView.setText(presentationInfo.getLocationName());
			
			String startTime = presentationInfo.getStartTime();
			String endTime = presentationInfo.getEndTime();
			
			try {
				Calendar start = Utils.stringToCalendar(startTime, "yyyy-MM-dd'T'HH:mm:ss");
				Calendar end = Utils.stringToCalendar(endTime, "yyyy-MM-dd'T'HH:mm:ss");
				
				String startHour = Utils.calendarToString(start, "HH:mm");
				String endHour = Utils.calendarToString(end, "HH:mm");
				String day = Utils.calendarToString(start, "dd MMM");
				
				timeView.setText(startHour + " - " + endHour +", " + day); 
			} catch (ParseException e) {
				Log.d(TAG, "Error parsing presentation start/end time: " + startTime + " / " + endTime, e);
			}
			
			// add speaker row view speaker layout
			mPresentationsLayout.addView(presentationInfoRowView);
			
			if (i != len - 1) {
				mPresentationsLayout.addView(getSeparatorView());
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
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putInt(ProgramFeature.PRESENTATION_ID, mSpeakerId);
		outState.putSerializable("program_feature", mProgramFeature);
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
	
	
	private class SpeakerPresentationClickListener implements OnClickListener {
		private int mPresentationId;
		
		SpeakerPresentationClickListener(int presentationId) {
			mPresentationId = presentationId;
		}
		
		@Override
		public void onClick(View v) {
			Log.d(TAG, "Launching speaker details activity for speakerId: " + mPresentationId);
			Intent i = new Intent(SpeakerDetailsActivity.this, PresentationDetailsActivity.class);
			Bundle extras = new Bundle();
			extras.putInt(ProgramFeature.SPEAKER_ID, mPresentationId);
			extras.putSerializable("program_feature", mProgramFeature);
			
			i.putExtras(extras);
			startActivity(i);
		}
	}
	
	static class SpeakerPresentationInfo implements Serializable {
		int mPresentationId;
		String mTitle;
		String mStartTime;
		String mEndTime;
		String mLocationName;
		
		public SpeakerPresentationInfo(int presentationId, String title, 
				String startTime, String endTime, String locationName) {
			
			this.mPresentationId = presentationId;
			this.mTitle = title;
			this.mStartTime = startTime;
			this.mEndTime = endTime;
			this.mLocationName = locationName;
		}

		public int getPresentationId() {
			return mPresentationId;
		}
		
		public String getTitle() {
			return mTitle;
		}
		
		public String getStartTime() {
			return mStartTime;
		}
		
		public String getEndTime() {
			return mStartTime;
		}
		
		public String getLocationName() {
			return mLocationName;
		}
	}
}
