package com.envsocial.android.features.description;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codeandmagic.android.TagListView;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.Xml.Encoding;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.envsocial.android.R;
import com.envsocial.android.utils.SimpleCursorLoader;

public class BoothDescriptionDetailsFragment extends SherlockFragment 
	implements LoaderManager.LoaderCallbacks<Cursor> {
	
	private static final String TAG = "BoothDescriptionFragment";
	private static final int DESCRIPTION_LOADER = 0;
	private static boolean active = true;
	
	private String mBoothImageUrl;
	private String mBoothContactEmail;
	private String mBoothContactWebsite;
	private List<String> mBoothTags;
	private String mBoothDescription;
	
	private BoothDescriptionActivity mParentActivity;
	private BoothDescriptionFeature mDescriptionFeature;
	private ProgressDialog mDescriptionLoaderDialog;
	
	private TextView mBoothNameView;
	private ImageView mBoothImageView;
	private TextView mBoothContactEmailView;
	private TextView mBoothContactWebsiteView;
	private TagListView mBoothTagListView;
	private WebView mBoothDescriptionView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    mParentActivity = (BoothDescriptionActivity) getActivity();
	    mDescriptionFeature = (BoothDescriptionFeature) mParentActivity.getFeature();
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate layout for this fragment.
		View view = inflater.inflate(R.layout.description_booth_details, container, false);
		
		mBoothNameView = (TextView) view.findViewById(R.id.description_details_location);
		mBoothImageView = (ImageView) view.findViewById(R.id.description_details_image);
		mBoothContactEmailView = (TextView) view.findViewById(R.id.description_details_contact_email);
		mBoothContactWebsiteView = (TextView) view.findViewById(R.id.description_details_contact_website);
		mBoothTagListView = (TagListView) view.findViewById(R.id.description_details_tag_list);
		mBoothDescriptionView = (WebView) view.findViewById(R.id.description_details_description_content);
		mBoothDescriptionView.getSettings().setBuiltInZoomControls(true);
		
		return view;
	}
	

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    
	    // load data
	 	getLoaderManager().initLoader(DESCRIPTION_LOADER, null, this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		active = true;
	}
	
	@Override
	public void onPause() {
		super.onResume();
		active = false;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy(); 
		
		if (mDescriptionLoaderDialog != null) {
			mDescriptionLoaderDialog.cancel();
			mDescriptionLoaderDialog = null;
		}
	}
	
	
	
	private void bindData(Cursor cursor) {
		mBoothNameView.setText(mParentActivity.getFeatureLocation().getName());
		
		if (cursor != null && cursor.moveToFirst()) {
			int imageUrlIndex = cursor.getColumnIndex(BoothDescriptionDbHelper.COL_BOOTH_DESCRIPTION_IMAGE_URL);
			int contactEmailIndex = cursor.getColumnIndex(BoothDescriptionDbHelper.COL_BOOTH_DESCRIPTION_CONTACT_EMAIL);
			int contactWebsiteIndex = cursor.getColumnIndex(BoothDescriptionDbHelper.COL_BOOTH_DESCRIPTION_CONTACT_WEBSITE);
			int tagsIndex = cursor.getColumnIndex(BoothDescriptionDbHelper.COL_BOOTH_DESCRIPTION_TAGS);
			int descriptionIndex = cursor.getColumnIndex(BoothDescriptionDbHelper.COL_BOOTH_DESCRIPTION_DESCRIPTION);
			
			if (!cursor.isNull(imageUrlIndex)) {
				mBoothImageUrl = cursor.getString(imageUrlIndex);
				mParentActivity.getImageFetcher().loadImage(mBoothImageUrl, mBoothImageView);
			}
			
			if (!cursor.isNull(contactEmailIndex)) {
				mBoothContactEmail = cursor.getString(contactEmailIndex);
				mBoothContactEmailView.setText(mBoothContactEmail);
			}
			
			if (!cursor.isNull(contactWebsiteIndex)) {
				mBoothContactWebsite = cursor.getString(contactWebsiteIndex);
				mBoothContactWebsiteView.setText(mBoothContactWebsite);
			}
			
			if (!cursor.isNull(tagsIndex)) {
				String tagListString = cursor.getString(tagsIndex);
				String [] tagArray = tagListString.split(";");
				
				mBoothTags = Arrays.asList(tagListString.split(";"));
				mBoothTags = new ArrayList<String>();
				for (int i = 0; i < tagArray.length; i++) {
					String tag = tagArray[i].replace("\"", "");
					mBoothTags.add(tag);
				}
				
				mBoothTagListView.setTags(mBoothTags);
			}
			
			if (!cursor.isNull(descriptionIndex)) {
				mBoothDescription = cursor.getString(descriptionIndex);
				try {
					mBoothDescriptionView.loadData(URLEncoder.encode(mBoothDescription, "UTF-8").replaceAll("\\+", " "), "text/html", Encoding.UTF_8.toString());
				} catch (UnsupportedEncodingException e) {
					Log.d(TAG, "ERROR loading booth description in WebView.", e);
				}
			}
		}
	}
	
	
	private void resetData() {
		mBoothImageUrl = null;
		mBoothContactEmail = null;
		mBoothContactWebsite = null;
		mBoothTags = null;
		mBoothDescription = null;
	}
	
	
	private ProgressDialog getProgressDialogInstance(Context context) {
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setIndeterminate(true);
		dialog.setTitle("Retrieving data ...");
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
		
		return dialog;
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
		if (mDescriptionLoaderDialog == null && active) {
			Log.d(TAG, "CREATING LOADER AND PROGRESS DIALOG");
			
			mDescriptionLoaderDialog = getProgressDialogInstance(getActivity());
			mDescriptionLoaderDialog.show();
		}
		
		return new BoothDescriptionCursorLoader(getActivity(), mDescriptionFeature);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (mDescriptionLoaderDialog != null) {
			Log.d(TAG, "FINISHING LOADER AND PROGRESS DIALOG");
			mDescriptionLoaderDialog.cancel();
			mDescriptionLoaderDialog = null;
		}
		
		bindData(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (mDescriptionLoaderDialog != null) {
			mDescriptionLoaderDialog.cancel();
			mDescriptionLoaderDialog = null;
		}
		
		resetData();
	}
	
	
	// ##################################### Helper static classes #####################################
	
	static class BoothDescriptionCursorLoader extends SimpleCursorLoader {
		private BoothDescriptionFeature mDescriptionFeature;
		
		
		public BoothDescriptionCursorLoader(Context context, BoothDescriptionFeature descriptionFeature) {
			super(context);
			mDescriptionFeature = descriptionFeature;
		}

		@Override
		public Cursor loadInBackground() {
			Cursor cursor = mDescriptionFeature.getBoothData();
			return cursor;
		}
	}
}
