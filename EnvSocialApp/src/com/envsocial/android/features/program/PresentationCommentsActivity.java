package com.envsocial.android.features.program;

import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
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
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.ResponseHolder;
import com.envsocial.android.utils.Utils;

public class PresentationCommentsActivity extends SherlockFragmentActivity implements OnClickListener {
	private static final String TAG = "PresentationCommentsActivity";
	private static final String TITLE_TAG = "Presentation Comments"; 
	
	private Location mLocation;
	private int mPresentationId;
	private String mPresentationTitle;
	
	private PresentationCommentListAdapter mCommentListAdapter;
	private RetrievePresentationCommentsTask mRetrieveCommentsTask;
	private SendPresentationCommentTask mSendCommentTask;
	
	private Calendar mNewestCommentTimestamp;
	private Calendar mOldestCommentTimestamp;
	
	
	private PresentationCommentsDialogFragment mCommentDialog;
	private ActionBar mActionBar;
	private ListView mCommentListView;
	private Button mMoreCommentsButton;
	
	@Override	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mLocation = (Location) getIntent().getSerializableExtra("location");
		mPresentationTitle = (String) getIntent().getStringExtra("presentation_title"); 
		mPresentationId = getIntent().getIntExtra("presentation_id", -1); 
		
		mActionBar = getSupportActionBar();
		mActionBar.setTitle(TITLE_TAG);
		mActionBar.setDisplayHomeAsUpEnabled(true);
		
		setContentView(R.layout.program_presentation_comments);
		TextView commentsLabelView = (TextView) findViewById(R.id.presentation_comments_label);
		TextView commentListEmptyView = (TextView) findViewById(R.id.presentation_comments_empty);
		mCommentListView = (ListView) findViewById(R.id.presentation_comments_list);
		mCommentListAdapter = new PresentationCommentListAdapter(this, new LinkedList<PresentationComment>());
		
		commentsLabelView.setText(getResources().getString(R.string.lbl_presentation_comments, mPresentationTitle));
		mCommentListView.setEmptyView(commentListEmptyView);
		mCommentListView.setAdapter(mCommentListAdapter);
		
		mMoreCommentsButton = (Button) findViewById(R.id.presentation_comments_load_more_button);
		mMoreCommentsButton.setOnClickListener(this);
		
		getPresentationComments(null, false);
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (mRetrieveCommentsTask != null) {
			mRetrieveCommentsTask.cancel(true);
			mRetrieveCommentsTask = null;
		}
		
		if (mSendCommentTask != null) {
			mRetrieveCommentsTask.cancel(false);
			mRetrieveCommentsTask = null;
		}
	}
	
	

	@Override
	public void onClick(View v) {
		if (v == mMoreCommentsButton) {
			getPresentationComments(mOldestCommentTimestamp, false);
		}
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.presentation_comments_menu, menu);
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
	        case R.id.presentation_comments_add:
	        	Location checkedInLocation = Preferences.getCheckedInLocation(this);
	        	if (checkedInLocation != null) {
	        		if (checkedInLocation.isArea()) {
	        			if (checkedInLocation.getLocationUri().equals(mLocation.getLocationUri())) {
	        				mCommentDialog = PresentationCommentsDialogFragment.newInstance();
	        	        	
	        	        	Bundle args = new Bundle();
	        	        	args.putInt("presentation_id", mPresentationId);
	        	        	
	        	        	mCommentDialog.setArguments(args);
	        				mCommentDialog.show(getSupportFragmentManager(), "comment_dialog");
	        				
	        				return true;
	        			}
	        		}
	        		else {
	        			if (checkedInLocation.getFeature(Feature.PROGRAM).isGeneral()) {
	        				mCommentDialog = PresentationCommentsDialogFragment.newInstance();
	        	        	
	        	        	Bundle args = new Bundle();
	        	        	args.putInt("presentation_id", mPresentationId);
	        	        	
	        	        	mCommentDialog.setArguments(args);
	        				mCommentDialog.show(getSupportFragmentManager(), "comment_dialog");
	        				
	        				return true;
	        			}
	        		}
	        	}
	        	
	        	PresentationCommentAlertDialog alertDialog = PresentationCommentAlertDialog.newInstance();
	        	alertDialog.show(getSupportFragmentManager(), "comment_alert_dialog");
	        	
	        	return true;
	        case R.id.presentation_comments_refresh:
	        	mRetrieveCommentsTask = 
	        		new RetrievePresentationCommentsTask(mPresentationId, this, mNewestCommentTimestamp, true);
	        	mRetrieveCommentsTask.execute();
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	
	public void sendComment(String commentJSON) {
		Annotation presentationComment = new Annotation(mLocation, Feature.PROGRAM, Calendar.getInstance(), commentJSON);
		
		mSendCommentTask = new SendPresentationCommentTask(this, presentationComment);
		mSendCommentTask.execute();
	}
	
	
	private void getPresentationComments(Calendar timestamp, boolean getNew) {
		mRetrieveCommentsTask = new RetrievePresentationCommentsTask(mPresentationId, this, timestamp, getNew);
		mRetrieveCommentsTask.execute();
	}
	
	
	private List<PresentationComment> parsePresentationComments(List<Annotation> presentationComments) throws JSONException {
		int len = presentationComments.size();
		
		List<PresentationComment> parsedComments = new LinkedList<PresentationComment>();
		
		for (int i = 0; i < len; i++) {
			Annotation comment = presentationComments.get(i);
			
			if (comment.getCategory().compareTo(Feature.PROGRAM) != 0) {
				continue;
			}
			
			PresentationComment presentationComment = parsePresentationComment(comment);
			
			if (i == 0) {
				if (mNewestCommentTimestamp == null) {
					mNewestCommentTimestamp = comment.getTimestamp();
				}
				else {
					if (comment.getTimestamp().after(mNewestCommentTimestamp)) {
						mNewestCommentTimestamp = comment.getTimestamp();
					}
				}
			}
			else if (i == len - 1) {
				if (mOldestCommentTimestamp == null) {
					mOldestCommentTimestamp = comment.getTimestamp();
				}
				else {
					if (comment.getTimestamp().before(mOldestCommentTimestamp)) {
						mOldestCommentTimestamp = comment.getTimestamp();
					}
				}
			}
			
			
			parsedComments.add(presentationComment);
		}
		
		return parsedComments;
	}
	
	private PresentationComment parsePresentationComment(Annotation commentObj) throws JSONException {
		String commentString = commentObj.getData();
		JSONObject commentDataObject = new JSONObject(commentString);
		
		String userFirstName = "Anonymous";
		String userLastName = "Guest";
		
		String commentText = commentDataObject.optString("text", "Empty comment.");
		JSONObject userJSONObject = commentDataObject.optJSONObject("user");
		if (userJSONObject != null) {
			userFirstName = userJSONObject.optString("first_name", "Anonymous");
			userLastName = userJSONObject.optString("last_name", "Guest");
		}
		 
		return	new PresentationComment(commentObj.getUri(), commentText, userFirstName + " " + userLastName, 
						commentObj.getUserUri(), commentObj.getTimestamp());
	}
	
	
	static class PresentationComment {		
		private String mCommentUrl;
		private String mCommentContent;
		private String mCommentOwner;
		private String mCommentOwnerUrl;
		private Calendar mCommentTimestamp; 
		
		public PresentationComment(String commentUrl, String commentContent, 
				String commentOwner, String commentOwnerUrl, 
				Calendar commentTimestamp) {
			
			this.mCommentUrl = commentUrl;
			this.mCommentContent = commentContent;
			this.mCommentOwner = commentOwner;
			this.mCommentOwnerUrl = commentOwnerUrl;
			this.mCommentTimestamp = commentTimestamp;
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
	}
	
	
	private class RetrievePresentationCommentsTask extends AsyncTask<Void, Void, List<Annotation>> {
		private ProgressDialog mCommentRetrievalDialog;
		private Context mContext;
		private Calendar mTimestamp;
		private boolean mGetNew;
		private int mPresentationId;
		
		RetrievePresentationCommentsTask(int presentationId, Activity commentsActivity, Calendar timestamp, boolean getNew) {
			mPresentationId = presentationId;
			mContext = commentsActivity;
			mTimestamp = timestamp;
			mGetNew = getNew;
		}
		
		@Override
		protected void onPreExecute() {
			mCommentRetrievalDialog = new ProgressDialog( new ContextThemeWrapper(mContext, R.style.ProgressDialogWhiteText));
			mCommentRetrievalDialog.setMessage("Retrieving comments ...");
			mCommentRetrievalDialog.setIndeterminate(true);
			mCommentRetrievalDialog.setCancelable(true);
			mCommentRetrievalDialog.setCanceledOnTouchOutside(true);
			
			mCommentRetrievalDialog.show();
		}

		@Override
		protected List<Annotation> doInBackground(Void... args) {
			if (mPresentationId != -1) {
				Map<String, String> extra = new HashMap<String, String>();
				
				// add presentation_id and order_by parameters 
				extra.put("presentation_id", "" + mPresentationId);
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
					List<Annotation> presentationComments = Annotation.getAnnotations(mContext, mLocation, 
							Feature.PROGRAM, extra, false);
					
					//Log.d(TAG, " " + presentationComments.size());
					
					return presentationComments;
				} catch (Exception e) {
					Log.d(TAG, "Error retrieving presentation annotations." , e);
					return null;
				}
			}
			else {
				Log.d(TAG, "Error retrieving presentation annotations. No presentation ID supplied.");
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(List<Annotation> presentationComments) {
			mCommentRetrievalDialog.cancel();
			
			if (presentationComments != null) {
				if (presentationComments.isEmpty()) {
					Toast toast = Toast.makeText(mContext, "No other comments.", Toast.LENGTH_LONG);
					toast.show();
					
					return;
				}
				
				try {
					List<PresentationComment> parsedComments = parsePresentationComments(presentationComments);
					mCommentListAdapter.addAllItems(parsedComments, !mGetNew);
				} catch (JSONException e) {
					e.printStackTrace();
					
					Toast toast = Toast.makeText(mContext, R.string.msg_get_entry_comments_err, Toast.LENGTH_LONG);
					toast.show();
				}
			} 
			else {
				Toast toast = Toast.makeText(mContext, R.string.msg_get_entry_comments_err, Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}
	
	
	class SendPresentationCommentTask extends AsyncTask<Void, Void, ResponseHolder> {
		private static final String TAG = "SendPresentationCommentTask";
		
		// loader dialog for sending the comment
		private ProgressDialog mSendCommentDialog;
		private Context mContext;
		private boolean error = true;
		private Annotation mCommentRequest;
		
		public SendPresentationCommentTask(Context context, Annotation commentRequest) {
			mContext = context;
			mCommentRequest = commentRequest;
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
				case HttpStatus.SC_NO_CONTENT:
					// parse the response content to see the newly created annotation as given by the server
					try {
						JSONObject annObj = holder.getJsonContent();
						Annotation ann = Annotation.parseAnnotation(getApplicationContext(), mLocation, annObj);
						
						// set newest timestamp as the one just created
						mNewestCommentTimestamp = ann.getTimestamp();
						
						// add new comment to the list
						PresentationComment presentationComment = parsePresentationComment(ann);
						mCommentListAdapter.addItem(presentationComment, false);
						
						error = false;
					} catch (JSONException e) {
						Log.d(TAG, "Error parsing annotation json as retrieved from server after posting. ", e);
						msgId = R.string.msg_send_comment_err;
						error = true;
					} catch (ParseException e) {
						Log.d(TAG, "Error parsing annotation calendar as retrieved from server after posting.", e);
						msgId = R.string.msg_send_comment_err;
						error = true;
					}
					
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
					Toast toast = Toast.makeText( mContext, msgId, Toast.LENGTH_LONG);
					toast.show();
				}
			} 
			else {
				int msgId = R.string.msg_service_unavailable;

				try {
					throw holder.getError();
				} 
				catch (EnvSocialComException e) {
					Log.d(TAG, e.getMessage(), e);
					msgId = R.string.msg_service_unavailable;
				} 
				catch (EnvSocialContentException e) {
					Log.d(TAG, e.getMessage(), e);
					msgId = R.string.msg_service_error;
				} 
				catch (Exception e) {
					Log.d(TAG, e.toString(), e);
					msgId = R.string.msg_service_error;
				}
				
				Toast toast = Toast.makeText(mContext, msgId, Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}
}
