package com.envsocial.android.features.description;

import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.HomeActivity;
import com.envsocial.android.R;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.exceptions.EnvSocialComException;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.EnvivedCommentAlertDialog;
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.ResponseHolder;
import com.envsocial.android.utils.Utils;

public class CommentsActivity extends SherlockFragmentActivity implements OnClickListener {
	private static final String TAG = "CommentsActivity";
	private static final String TITLE_TAG = "Comments";
	
	private Location mLocation;
	
	private ActionBar mActionBar;
	
	private Calendar mNewestCommentTimestamp;
	private Calendar mOldestCommentTimestamp;
	
	private CommentsDialogFragment mCommentDialog;
	private CommentsListAdapter mCommentsListAdapter;
	private ListView mCommentsListView;
	private Button mMoreCommentsButton;
	private AlertDialog filterDialog;
	
	private RetrieveCommentsTask mRetrieveCommentsTask;
	private SendCommentTask mSendCommentTask;
	
	private String mSubject;
	
	@Override	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mActionBar = getSupportActionBar();
		mActionBar.setTitle(TITLE_TAG);
		mActionBar.setDisplayHomeAsUpEnabled(true);
		
		mLocation = (Location)getIntent().getSerializableExtra("location");
		mSubject = (String)getIntent().getStringExtra("productName");
		
		if (mSubject == null) {
			mSubject = mLocation.getName();
		}
		
		setContentView(R.layout.comments_view);
		TextView commentsLabelView = (TextView) findViewById(R.id.comments_label);
		commentsLabelView.setText(getResources().getString(R.string.lbl_comments, mLocation.getName()));
		
		TextView commentsListEmptyView = (TextView) findViewById(R.id.comments_empty);
		mCommentsListView = (ListView) findViewById(R.id.comments_list);
		mCommentsListAdapter = new CommentsListAdapter(this, new LinkedList<Comment>());
		
		mCommentsListView.setEmptyView(commentsListEmptyView);
		mCommentsListView.setAdapter(mCommentsListAdapter);
		
		mMoreCommentsButton = (Button) findViewById(R.id.comments_load_more_button);
		mMoreCommentsButton.setOnClickListener(this);
		
		getComments(null, false);
		
		initFilter();
		
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (mRetrieveCommentsTask != null) {
			mRetrieveCommentsTask.cancel(true);
			mRetrieveCommentsTask = null;
		}
		
		if (mSendCommentTask != null) {
			mSendCommentTask.cancel(false);
			mSendCommentTask = null;
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.description_comments_menu, menu);
     	
    	return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:	
			Intent i = new Intent(this, HomeActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return true;
		case R.id.comments_add:
			Location checkedInLocation = Preferences.getCheckedInLocation(this);
			if (checkedInLocation != null && checkedInLocation.getLocationUri().equals(mLocation.getLocationUri())) {
				mCommentDialog = CommentsDialogFragment.newInstance(mSubject);
    			mCommentDialog.show(getSupportFragmentManager(), "comment_dialog");
    				
    			return true;
			}
			
			String message = "You have to be checked in at this location (scan the QRcode) " +
							 "to be able to post messages.";
			EnvivedCommentAlertDialog alertDialog = EnvivedCommentAlertDialog.newInstance(message);
			alertDialog.show(getSupportFragmentManager(), "comment_alert_dialog");
			return true;
			
		case R.id.comments_refresh:
			mRetrieveCommentsTask = new RetrieveCommentsTask(this, this, mNewestCommentTimestamp, true);
			mRetrieveCommentsTask.execute();
			return true;
			
		case R.id.comments_filter:
			filterDialog.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
		
	}
	
	public void initFilter() {
		final String[] filterItems = getIntent().getStringArrayExtra("filterItems");

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Filter by topic");
		
		boolean[] checked = new boolean[filterItems.length];
		for (int i = 0; i < filterItems.length; i++) {
			if (mSubject.equals(filterItems[i])) {
				checked[i] = true;
				mCommentsListAdapter.addFilter(mSubject);
				mCommentsListAdapter.applyFilter();
			} else {
				checked[i] = false;
			}
		}
		
		builder.setMultiChoiceItems(filterItems, checked, new DialogInterface.OnMultiChoiceClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				if (isChecked) {
					mCommentsListAdapter.addFilter(filterItems[which]);
				} else {
					mCommentsListAdapter.removeFilter(filterItems[which]);
				}
				mCommentsListAdapter.applyFilter();
			}
		});
		filterDialog = builder.create();
	}
	
	public void sendComment(CommentsDialogFragment dialog) {
		String commentJSON = dialog.getCommentJSONString();
		
		Annotation comment = new Annotation(mLocation, Feature.BOOTH_DESCRIPTION, Calendar.getInstance(), commentJSON);
		
		mSendCommentTask = new SendCommentTask(this, this, comment);
		mSendCommentTask.execute();
	}
	
	public void postSendComment() {
		mCommentDialog.dismiss();
	}
	
	private void getComments(Calendar timestamp, boolean getNew) {
		mRetrieveCommentsTask = new RetrieveCommentsTask(this, this, timestamp, getNew);
		mRetrieveCommentsTask.execute(timestamp);
	}
	
	private Comment parseComment(Annotation commentObj) throws JSONException {
		String commentString = commentObj.getData();
		JSONObject commentDataObject = new JSONObject(commentString);
		
		String userFirstName = "Anonymous";
		String userLastName = "Guest";
		
		String commentText = commentDataObject.optString("text", "Empty comment.");
		String commentSubject = commentDataObject.optString("topic_title", "Empty Subject");
		JSONObject userJSONObject = commentDataObject.optJSONObject("user");
		if (userJSONObject != null) {
			userFirstName = userJSONObject.optString("first_name", "Anonymous");
			userLastName = userJSONObject.optString("last_name", "Guest");
		}
		 
		return	new Comment(commentObj.getUri(), commentText, userFirstName + " " + userLastName, 
						commentObj.getUserUri(), commentObj.getTimestamp(), commentSubject);
	}
	
	private List<Comment> parseComments(List<Annotation> list) throws JSONException {
		List<Comment> commentsList = new LinkedList<Comment>();
		int len = list.size(), i = 0;
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
			
			String userFirstName = "Anonymous";
			String userLastName = "Guest";
			
			String commentText = commentDataObject.optString("text", "Empty String");
			String commentSubject = commentDataObject.optString("topic_title", "Empty Subject");
			JSONObject userJSONObject = commentDataObject.optJSONObject("user");
			if (userJSONObject != null) {
				userFirstName = userJSONObject.optString("first_name", "Anonymous");
				userLastName = userJSONObject.optString("last_name", "Guest");
			}
			
			Comment parsedComment = new Comment(annotation.getUri(), commentText, userFirstName + " " + userLastName, 
					annotation.getUserUri(), annotation.getTimestamp(), commentSubject);
			
			if (i == 0) {
				if (mNewestCommentTimestamp == null) {
					mNewestCommentTimestamp = annotation.getTimestamp();
				} else {
					if (annotation.getTimestamp().after(mNewestCommentTimestamp)) {
						mNewestCommentTimestamp = annotation.getTimestamp();
					}
				}
			} else if (i == len - 1) {
				if (mOldestCommentTimestamp == null) {
					mOldestCommentTimestamp = annotation.getTimestamp();
				}
				else {
					if (annotation.getTimestamp().before(mOldestCommentTimestamp)) {
						mOldestCommentTimestamp = annotation.getTimestamp();
					}
				}
			}
			
			commentsList.add(parsedComment);
			i++;
		}
		
		return commentsList;
	}
	
	static class Comment {
		private String mCommentUrl;
		private String mCommentContent;
		private String mCommentOwner;
		private String mCommentOwnerUrl;
		private Calendar mCommentTimestamp; 
		private String mCommentSubject;
		
		public Comment(String commentUrl, String commentContent, 
				String commentOwner, String commentOwnerUrl, 
				Calendar commentTimestamp, String commentSubject) {
			
			this.mCommentUrl = commentUrl;
			this.mCommentContent = commentContent;
			this.mCommentOwner = commentOwner;
			this.mCommentOwnerUrl = commentOwnerUrl;
			this.mCommentTimestamp = commentTimestamp;
			this.mCommentSubject = commentSubject;
		}

		public String getCommentUrl() {
			return mCommentUrl;
		}

		public String getCommentContent() {
			return mCommentContent;
		}

		public String getCommentOwner() {
			return mCommentOwner;
		}
		
		public String getCommentOwnerUrl() {
			return mCommentOwnerUrl;
		}
		
		public Calendar getCommentTimestamp() {
			return mCommentTimestamp;
		}
		
		public String getCommentSubject() {
			return mCommentSubject;
		}
		
		public String toString() {
			return mCommentOwner + "\n" + mCommentSubject + "\n" + mCommentContent;
		}
	}
	
	private class RetrieveCommentsTask extends AsyncTask<Calendar, Void, List<Annotation>> {
		private ProgressDialog mCommentRetrievalDialog;
		private CommentsActivity mCommentsActivity;
		private Context mContext;
		private Calendar mTimestamp;
		private boolean mGetNew;
		
		RetrieveCommentsTask(CommentsActivity commentsActivity, Context context, Calendar timestamp, boolean getNew) {
			mCommentsActivity = commentsActivity;
			mContext = context;
			mTimestamp = timestamp;
			mGetNew = getNew;
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
			
			Map<String, String> extra = new HashMap<String, String>();
			
			// add presentation_id and order_by parameters 
			extra.put("order_by", "-timestamp");
			
			if (mTimestamp != null) {
				String timeStr = Utils.calendarToString(mTimestamp, "yyyy-MM-dd HH:mm:ss");
				if (mGetNew) {
					extra.put("timestamp__gt", timeStr);
				}
				else {
					extra.put("timestamp__lt", timeStr);
				}
			}
			
			try {
				List<Annotation> commentRequests = Annotation.getAnnotations(mCommentsActivity, 
						mLocation, 
						Feature.BOOTH_DESCRIPTION, 
						extra,
						false
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
				if (comments.isEmpty()) {
					Toast toast = Toast.makeText(mContext, "No other comments.", Toast.LENGTH_LONG);
					toast.show();
					
					return;
				}
				try {
					List<Comment> parsedComments = parseComments(comments);
					mCommentsListAdapter.addAllItems(parsedComments, false);
					mCommentsListAdapter.applyFilter();
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

	@Override
	public void onClick(View v) {
		if (v == mMoreCommentsButton) {
			getComments(mOldestCommentTimestamp, false);
		}
		
	}
	
	
	
	private class SendCommentTask extends AsyncTask<Void, Void, ResponseHolder> {
		private static final String TAG = "SendCommentTask";
		
		// loader dialog for sending the comment
		private ProgressDialog mSendCommentDialog;
		private Context mContext;
		private boolean error = true;
		
		//private String mCommentSubject;
		private Annotation mCommentRequest;
		
		private CommentsActivity mCommentsActivity;
		
		public SendCommentTask(Context context, CommentsActivity commentsActivity, Annotation commentRequest) {
			mContext = context;
			mCommentRequest = commentRequest;
			mCommentsActivity = commentsActivity;
		}
		
		@Override
		protected void onPreExecute() {
			mSendCommentDialog = new ProgressDialog(new ContextThemeWrapper(mContext, R.style.ProgressDialogWhiteText));
			mSendCommentDialog.setMessage("Sending comment ...");
			mSendCommentDialog.setIndeterminate(true);
			mSendCommentDialog.setCanceledOnTouchOutside(true);
			
			mSendCommentDialog.show();
		}
		
		@Override
		protected ResponseHolder doInBackground(Void...args) {
			return mCommentRequest.post(mContext);
		}
		
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			mSendCommentDialog.cancel();
			
			if (!holder.hasError()) {
				error = false;
				int msgId = R.string.msg_send_comment_ok;
				
				switch(holder.getCode()) {
				case HttpStatus.SC_CREATED:
				case HttpStatus.SC_ACCEPTED:
					error = false;
					break;
				default:
					msgId = R.string.msg_send_comment_err;
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
						Annotation createdComment = Annotation.parseAnnotation(mContext, mLocation, holder.getJsonContent());
						
						Comment newComment = parseComment(createdComment);
						mCommentsListAdapter.addItem(newComment, false);
						
						msgId = R.string.msg_send_comment_ok;
					} 
					catch (JSONException e) {
						Log.d(TAG, "Error parsing new Booth Description comment. ", e);
						msgId = R.string.msg_send_comment_err;
					} catch (ParseException e) {
						Log.d(TAG, "Error parsing new Booth Description comment. ", e);
						msgId = R.string.msg_send_comment_err;
					}
					
					Toast toast = Toast.makeText( mContext, msgId, Toast.LENGTH_LONG);
					toast.show();
				}
			} else {
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
			
			mCommentsActivity.postSendComment();
		}
	}
}
