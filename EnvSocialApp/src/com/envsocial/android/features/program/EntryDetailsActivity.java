package com.envsocial.android.features.program;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.envsocial.android.R;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.api.EnvSocialResource;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.exceptions.EnvSocialComException;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.ResponseHolder;

public class EntryDetailsActivity extends SherlockFragmentActivity implements OnClickListener {
	private static final String TAG = "EntryDetailsActivity";
	
	private String mEntryId;
	private Location mLocation;
	
	private TextView mTitle;
	private TextView mSession;
	private TextView mDatetime;
	private TextView mSpeakers;
	private TextView mAbstract;
	
	// comment list parameters
	private int commentListOffset = 0;
	private static int COMMENT_LIMIT = 5;
	private LinearLayout mCommentsHolder;
	private Button mMoreCommentsBtn;
	
	private Button mBtnSend;
	private EditText mComment;
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		
		mEntryId = getIntent().getExtras().getString(ProgramDbHelper.COL_ENTRY_ID);
		mLocation = Preferences.getCheckedInLocation(this);
		
		setContentView(R.layout.entry_details);
		mTitle = (TextView) findViewById(R.id.title);
		mSession = (TextView) findViewById(R.id.session);
		mDatetime = (TextView) findViewById(R.id.datetime);
		mSpeakers = (TextView) findViewById(R.id.speakers);
		mAbstract = (TextView) findViewById(R.id.abs);
		
		
		// initialize comment holder layout
		mCommentsHolder = (LinearLayout) findViewById(R.id.entry_comments);
		
		mMoreCommentsBtn = (Button) findViewById(R.id.btn_more_comments);
		mMoreCommentsBtn.setOnClickListener(this);
		
		mBtnSend = (Button) findViewById(R.id.btn_send);
		mComment = (EditText) findViewById(R.id.comment);
		
		// set Listener for comment send button
		mBtnSend.setOnClickListener(this);
		
		new FetchEntryDetailTask(mLocation, mEntryId).execute();
	}
	
	
	
	private void addEntryComments(LinkedList<Map<String, String>> entryComments) {
		if (!entryComments.isEmpty()) {
			LayoutInflater inflater = getLayoutInflater();
			
			for (Map<String, String> comment : entryComments) {
				View commentView = makeCommentView(inflater, comment);
				mCommentsHolder.addView(commentView);
				commentListOffset++;
			}
		}
	}
	
	
	private View makeCommentView(LayoutInflater inflater, Map<String, String> comment) {
		View commentView = inflater.inflate(R.layout.entry_comment_row, mCommentsHolder, false);
		TextView authorView = (TextView)commentView.findViewById(R.id.entry_comment_author);
		TextView dateView = (TextView)commentView.findViewById(R.id.entry_comment_date);
		TextView textView = (TextView)commentView.findViewById(R.id.entry_comment_text);
		
		RelativeLayout authorDateHolder = (RelativeLayout)commentView.findViewById(R.id.entry_comment_author_date_holder);
		authorDateHolder.setBackgroundColor(getResources().getColor(R.color.light_green));
		
		authorView.setText(comment.get("author"));
		dateView.setText(comment.get("date"));
		textView.setText(comment.get("text"));
		
		return commentView;
	}
	
	
	@Override
	public void onClick(View v) {
		if (v == mBtnSend) {
			sendComment();
		}
		else {
			if (v == mMoreCommentsBtn) {
				new FetchEntryCommentTask().execute();
			}
		}
	}
	
	
	private LinkedList<Map<String, String>> getEntryComments() {
		try {
			Map<String, String> extra = new HashMap<String, String>();
			extra.put("entry_id", mEntryId);
			extra.put("order_by", "-timestamp");
			extra.put("userexplicit", "true");
			
			List<Annotation> entryAnnotations = Annotation.getAnnotations(this, mLocation, 
					Feature.PROGRAM, extra, commentListOffset, COMMENT_LIMIT);
			
			return parseEntryAnnotations(entryAnnotations);
			
		} catch (EnvSocialComException e) {
			Log.d(TAG, e.getMessage());
		} catch (EnvSocialContentException e) {
			Log.d(TAG, e.getMessage(), e);
		}
		
		return null;
	}

	private LinkedList<Map<String, String>> parseEntryAnnotations(
			List<Annotation> entryAnnotations) throws EnvSocialContentException {
		
		LinkedList<Map<String,String>> comments = new LinkedList<Map<String,String>>();
		for (Annotation ann : entryAnnotations) {
			Map<String, String> commentStringData = extractCommentData(ann);
			comments.add(commentStringData);
		}
		
		return comments;
	}
	
	private Map<String, String> extractCommentData(Annotation ann) throws EnvSocialContentException {
		String commentJSONString = ann.getData();
		
		try {
			JSONObject data = new JSONObject(commentJSONString);
			
			String text = data.optString("text", "--");
			JSONObject userNameInfo = data.optJSONObject("user");
			String firstName = "Anonymous";
			String lastName = "Guest";
			
			if (userNameInfo != null) {
				firstName = userNameInfo.optString("first_name", "Anonymous");
				lastName = userNameInfo.optString("last_name", "Guest");
			}
			
			Map<String, String> commentStringData = new HashMap<String, String>();
			commentStringData.put("text", text);
			commentStringData.put("author", firstName + " " + lastName);
			commentStringData.put("date", new SimpleDateFormat("dd MMM yyyy, HH:mm").format(ann.getTimestamp().getTime()));
			
			return commentStringData;
		} catch (JSONException ex) {
			throw new EnvSocialContentException(commentJSONString, EnvSocialResource.ANNOTATION, ex);
		}
	}
	
	
	private void bind(Map<String,String> entry) {
		mTitle.setText(entry.get(ProgramDbHelper.COL_ENTRY_TITLE));
		mSession.setText(entry.get(ProgramDbHelper.COL_ENTRY_SESSIONID));
		mDatetime.setText(entry.get(ProgramDbHelper.COL_ENTRY_START_TIME));
		mSpeakers.setText(entry.get(ProgramDbHelper.COL_ENTRY_SPEAKERS));
		mAbstract.setText(entry.get(ProgramDbHelper.COL_ENTRY_ABSTRACT));
	}

	
	private void sendComment() {
		try {
			JSONObject jsonComment = new JSONObject();
			jsonComment.put("text", mComment.getText().toString());
			jsonComment.put("entry_id", mEntryId);
			
			String firstName = Preferences.getLoggedInUserFirstName(this);
			String lastName = Preferences.getLoggedInUserLastName(this);
			
			JSONObject userNamingJSON = new JSONObject();
			userNamingJSON.put("first_name", firstName);
			userNamingJSON.put("last_name", lastName);
			jsonComment.put("user", userNamingJSON);
			
			String jsonCommentString = jsonComment.toString();
			Annotation entryComment = new Annotation(this, mLocation, Feature.PROGRAM, 
													Calendar.getInstance(), jsonCommentString);
			
			new SendEntryCommentTask(entryComment).execute();
			
		} catch (JSONException ex) {
			Log.d(TAG, "Error creating comment JSON string.", ex);
			ex.printStackTrace();
		} 
	}
	
	
	private void appendComment(Annotation entryAnnotation) {
		Map<String, String> commentStringData;
		try {
			commentStringData = extractCommentData(entryAnnotation);
			
			View commentView = makeCommentView(getLayoutInflater(), commentStringData);
			mCommentsHolder.addView(commentView, 0);
			commentListOffset++;
			
			// clear edit box at the end
			mComment.setText("");
		} catch (EnvSocialContentException ex) {
			Log.d(TAG, ex.getMessage() + "JSON Content :: " + ex.getContent(), ex);
		}
	}
	
	
	private class FetchEntryDetailTask extends AsyncTask<Void, Void, Void> {
		private String entryId;
		private Location location;
		private Map<String, String> entry;
		
		// loader dialog for async task of fetching entry data and comments
		private ProgressDialog mLoadingDialog;
		
		LinkedList<Map<String, String>> entryComments;
		
		public FetchEntryDetailTask(Location location, String entryId) {
			this.location = location;
			this.entryId = entryId;
		}
		
		@Override
		protected void onPreExecute() {
			mLoadingDialog = ProgressDialog.show(EntryDetailsActivity.this, 
					"", "Loading...", true);
		}
		
		@Override
		protected Void doInBackground(Void...args) {
			try {
				entry = ProgramFeature.getProgramEntryById(EntryDetailsActivity.this, location, entryId);
			} catch (Exception ex) {
				entry = null;
			}
			
			try {
				entryComments = getEntryComments();
			} catch (Exception ex) {
				entryComments = null;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			mLoadingDialog.cancel();
			if (entry != null) {
				bind(entry);
			}
			
			if (entryComments != null) {
				addEntryComments(entryComments);
			}
			
			if (entry == null && entryComments == null) {
				Toast toast = Toast.makeText(getApplicationContext(), 
						R.string.msg_get_entry_details_err, Toast.LENGTH_LONG);
				toast.show();
				EntryDetailsActivity.this.finish();
			}
		}
	}
	
	private class FetchEntryCommentTask extends AsyncTask<Void, Void, Void> {
		LinkedList<Map<String, String>> entryComments;
		ProgressDialog commentFetchDialog;
		
		@Override
		protected void onPreExecute() {
			commentFetchDialog = ProgressDialog.show(EntryDetailsActivity.this, 
					"", "Retrieving comments ...", true);
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			entryComments = getEntryComments();
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			commentFetchDialog.cancel();
			
			if (entryComments != null) {
				if (entryComments.isEmpty()) {
					Toast toast = Toast.makeText(EntryDetailsActivity.this, "No other comments.", Toast.LENGTH_LONG);
					toast.show();
				}
				else {
					addEntryComments(entryComments);
				}
			}
			else {
				Toast toast = Toast.makeText(EntryDetailsActivity.this, 
						R.string.msg_get_entry_comments_err, Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}
	
	
	private class SendEntryCommentTask extends AsyncTask<Void, Void, ResponseHolder> {
		Annotation comment;
		ProgressDialog commentSendDialog;
		
		public SendEntryCommentTask(Annotation comment) {
			this.comment = comment;
		}
		
		@Override
		protected void onPreExecute() {
			commentSendDialog = ProgressDialog.show(EntryDetailsActivity.this, 
					"", "Sending comment ...", true);
		}
		
		@Override
		protected ResponseHolder doInBackground(Void... params) {
			return comment.post();
		}
		
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			commentSendDialog.cancel();
			
			if (!holder.hasError()) {
				boolean error = false;
				int msgId = R.string.msg_send_entry_comment_ok;

				switch(holder.getCode()) {
				case HttpStatus.SC_CREATED: 					
					error = false;
					break;

				case HttpStatus.SC_BAD_REQUEST:
					error = true;
					msgId = R.string.msg_send_entry_comment_400;
					break;

				case HttpStatus.SC_UNAUTHORIZED:
					error = true;
					msgId = R.string.msg_send_entry_comment_401;
					break;

				case HttpStatus.SC_METHOD_NOT_ALLOWED:
					error = true;
					msgId = R.string.msg_send_entry_comment_405;
					break;

				default:
					error = true;
					msgId = R.string.msg_send_entry_comment_err;
					break;
				}

				if (!error) {
					appendComment(comment);
				}
				else {
					Log.d(TAG, "[DEBUG]>> Error sending comment: " + msgId);
				}

				Toast toast = Toast.makeText( EntryDetailsActivity.this,
						msgId, Toast.LENGTH_LONG);
				toast.show();
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

				Toast toast = Toast.makeText(EntryDetailsActivity.this, msgId, Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}
	
}
