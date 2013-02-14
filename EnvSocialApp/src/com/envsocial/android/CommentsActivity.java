package com.envsocial.android;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.Utils;

public class CommentsActivity extends SherlockFragmentActivity {
	private static final String TAG = "CommentsActivity";
	protected static final String RESOURCE_URI = "resource_uri";
	protected static final String LOCATION_NAME = "location_name";
	protected static final String COMMENT_OWNER = "comment_owner";
	protected static final String COMMENT_SUBJECT = "comment_subject";
	protected static final String COMMENT_CONTENT = "comment_content";
	protected static final String COMMENT_TIMESTAMP = "comment_timestamp";
	private static boolean active;
	
	private Location mLocation;
	
	private ActionBar mActionBar;
	
	private LinearLayout mMainView;
	
	private CommentsDialogFragment d;
	
	private LinkedList<Comment> mCommentList;
	
	@Override	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.comments_view);
		
		mActionBar = getSupportActionBar();
		
		mLocation = (Location)getIntent().getSerializableExtra("location");
		if (mLocation != null) {
			mActionBar.setTitle(mLocation.getName());
		}
		
		getComments(null);
		
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		Log.d(TAG, " --- onPause called in CommentsViewActivity");
		active = false;		
	}
	
	@Override
	public void onStop() {
		Log.d(TAG, " --- onStop called in CommentsViewActivity");
		super.onStop();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Log.d(TAG, " --- onResume called in CommentsViewActivity");
		active = true;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		Log.d(TAG, " --- onStart called in CommentsViewActivity");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		Log.d(TAG, " --- onDestroy called in CommentsViewActivity");
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		// add the filter button
		MenuItem filterItem = menu.add(getText(R.string.menu_filter));
		filterItem.setTitle("FILTER");
		filterItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		// add the add button
		MenuItem addItem = menu.add(getText(R.string.menu_add));
		addItem.setTitle("ADD");
		addItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
     	
    	return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		
		if (item.getTitle().toString().compareTo(getString(R.string.menu_add)) == 0) {
			d = CommentsDialogFragment.newInstance();
			FragmentManager fm = getSupportFragmentManager();
			d.show(fm, "comment_dialog");
			
			return true;
		}
		
		return false;
	}
	
	public void sendComment(CommentsDialogFragment dialog) {
		String commentJSON = dialog.getCommentJSONString();
		
		Annotation comment = new Annotation(mLocation, Feature.BOOTH_DESCRIPTION, Calendar.getInstance(), commentJSON);
		new SendCommentTask(this, this, comment).execute();
	}
	
	public void postSendComment() {
		d.dismiss();
	}
	
	private void getComments(Calendar timestamp) {
		RetrieveCommentsTask task = new RetrieveCommentsTask(this);
		task.execute(timestamp);
	}
	
	private void parseRequests(List<Annotation> list) throws JSONException {
		for (Annotation annotation : list) {
			if (annotation.getCategory().compareTo(Feature.BOOTH_DESCRIPTION) != 0) {
				continue;
			}
			
			String locationName = annotation.getLocation().getName();
			String locationUri = annotation.getLocation().getLocationUri();
			Map<String, String> locationData = new HashMap<String, String>();
			locationData.put("location_name", locationName);
			locationData.put("location_uri", locationUri);
			
			
			String commentString = annotation.getData();
			JSONObject commentDataObject = new JSONObject(commentString);
			
			JSONArray commentData = new JSONArray(commentString);
			
			Map<String, String> commentRequestMap = new HashMap<String, String>();
			commentRequestMap.put(RESOURCE_URI, annotation.getUri());
			commentRequestMap.put(LOCATION_NAME, locationName);
			commentRequestMap.put(COMMENT_OWNER, annotation.getUserUri());
			commentRequestMap.put(COMMENT_TIMESTAMP, Utils.calendarToString(annotation.getTimestamp(), "yyyy-MM-dd'T'HH:mm:ssZ"));
			commentRequestMap.put(COMMENT_CONTENT, annotation.getData());
			Log.d(TAG, commentRequestMap.toString());
		}
	}
	
	
	private class Comment {
		private String mCommentSubject;
		private String mCommentContent;
		private String mCommentOwner;
	}
	
	private class RetrieveCommentsTask extends AsyncTask<Calendar, Void, List<Annotation>> {
		private ProgressDialog mCommentRetrievalDialog;
		private CommentsActivity mCommentsActivity;
		
		RetrieveCommentsTask(CommentsActivity commentsActivity) {
			mCommentsActivity = commentsActivity;
		}
		
		@Override
		protected void onPreExecute() {
			mCommentRetrievalDialog = new ProgressDialog( new ContextThemeWrapper(mCommentsActivity, R.style.ProgressDialogWhiteText));
			mCommentRetrievalDialog.setMessage("Retrieving comments ...");
			mCommentRetrievalDialog.setIndeterminate(true);
			mCommentRetrievalDialog.setCancelable(true);
			mCommentRetrievalDialog.setCanceledOnTouchOutside(true);
			
			mCommentRetrievalDialog.show();
		}

		@Override
		protected List<Annotation> doInBackground(Calendar... cals) {
			Log.d(TAG, "doInBackground() started");
			Calendar cal = null;
			
			if (cals.length != 0) {
				cal = cals[0];
			}
			
			try {
				List<Annotation> commentRequests = Annotation.getAnnotations(mCommentsActivity, 
						mLocation, 
						Feature.BOOTH_DESCRIPTION,
						true
				);
				
				Log.d(TAG, " " + commentRequests.size());
				
				return commentRequests;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(List<Annotation> comments) {
			mCommentRetrievalDialog.cancel();
			
			if (comments != null) {
				try {
					parseRequests(comments);
				} catch (JSONException e) {
					e.printStackTrace();
					
					Toast toast = Toast.makeText(mCommentsActivity, R.string.msg_get_entry_comments_err, Toast.LENGTH_LONG);
					toast.show();
				}
			} else {
				Toast toast = Toast.makeText(mCommentsActivity, R.string.msg_get_entry_comments_err, Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}
}
