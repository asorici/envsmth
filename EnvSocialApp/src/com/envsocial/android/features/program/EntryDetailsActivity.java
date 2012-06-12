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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.envsocial.android.HomeActivity;
import com.envsocial.android.LoginActivity;
import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.ProgramEntry;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.Preferences;

public class EntryDetailsActivity extends SherlockFragmentActivity implements OnClickListener {
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
	
	// loader dialog for async task of fetching entry data and comments
	private ProgressDialog mLoadingDialog;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mEntryId = getIntent().getExtras().getString(ProgramDbHelper.COL_ENTRY_ID);
		mLocation = Preferences.getCheckedInLocation(this);
		
		setContentView(R.layout.entry_details);
		mTitle = (TextView) findViewById(R.id.title);
		mSession = (TextView) findViewById(R.id.session);
		mDatetime = (TextView) findViewById(R.id.datetime);
		mSpeakers = (TextView) findViewById(R.id.speakers);
		mAbstract = (TextView) findViewById(R.id.abs);
		
		//mCommentList = (ListView) findViewById(R.id.comment_list);
		//EntryCommentListAdapter commentListAdapter = new EntryCommentListAdapter(this, getEntryComments());
		//mCommentList.setAdapter(commentListAdapter);
		
		// initialize comment holder layout
		mCommentsHolder = (LinearLayout) findViewById(R.id.entry_comments);
		
		mMoreCommentsBtn = (Button) findViewById(R.id.btn_more_comments);
		mMoreCommentsBtn.setOnClickListener(this);
		
		mBtnSend = (Button) findViewById(R.id.btn_send);
		mComment = (EditText) findViewById(R.id.comment);
		
		// set Listener for comment send button
		mBtnSend.setOnClickListener(this);
		
		/*
		setupCommentViews();
		Map<String,String> entry = ProgramEntry.getEntryById(this, mLocation, mEntryId);
		bind(entry);
		*/
		new EntryFetchTask(mLocation, mEntryId).execute();
	}
	
	
	private class EntryFetchTask extends AsyncTask<Void, Void, Void> {
		private String entryId;
		private Location location;
		private Map<String, String> entry;
		LinkedList<Map<String, String>> entryComments;
		
		public EntryFetchTask(Location location, String entryId) {
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
				entry = ProgramEntry.getEntryById(EntryDetailsActivity.this, location, entryId);
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
						R.string.msg_service_error, Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}
	
	private void setupCommentViews() {
		LinkedList<Map<String, String>> entryComments = getEntryComments();
		addEntryComments(entryComments);
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
		
		authorView.setText(comment.get("author"));
		dateView.setText(comment.get("date"));
		textView.setText(comment.get("text"));
		
		return commentView;
	}


	private LinkedList<Map<String, String>> getEntryComments() {
		try {
			Map<String, String> extra = new HashMap<String, String>();
			extra.put("entry_id", mEntryId);
			extra.put("order_by", "-timestamp");
			extra.put("userexplicit", "true");
			
			List<Annotation> entryAnnotations = Annotation.getAnnotations(this, mLocation, 
					Feature.PROGRAM, extra, commentListOffset, COMMENT_LIMIT);
			// System.out.println("[DEBUG]>> received entryComments: " + entryComments);

			return parseEntryAnnotations(entryAnnotations);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private LinkedList<Map<String, String>> parseEntryAnnotations(
			List<Annotation> entryAnnotations) throws JSONException {
		
		LinkedList<Map<String,String>> comments = new LinkedList<Map<String,String>>();
		for (Annotation ann : entryAnnotations) {
			Map<String, String> commentStringData = extractCommentData(ann);
			comments.add(commentStringData);
		}
		
		return comments;
	}
	
	private Map<String, String> extractCommentData(Annotation ann) throws JSONException {
		String commentJSONString = ann.getData();
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
	}
	
	
	private void bind(Map<String,String> entry) {
		mTitle.setText(entry.get(ProgramDbHelper.COL_ENTRY_TITLE));
		mSession.setText(entry.get(ProgramDbHelper.COL_ENTRY_SESSIONID));
		mDatetime.setText(entry.get(ProgramDbHelper.COL_ENTRY_START_TIME));
		mSpeakers.setText(entry.get(ProgramDbHelper.COL_ENTRY_SPEAKERS));
		mAbstract.setText(entry.get(ProgramDbHelper.COL_ENTRY_ABSTRACT));
	}

	@Override
	public void onClick(View v) {
		if (v == mBtnSend) {
			sendComment();
		}
		else {
			if (v == mMoreCommentsBtn) {
				LinkedList<Map<String, String>> moreEntryComments = getEntryComments();
				if (moreEntryComments.isEmpty()) {
					Toast toast = Toast.makeText(this, "No other comments.", Toast.LENGTH_LONG);
					toast.show();
				}
				else {
					addEntryComments(moreEntryComments);
				}
			}
		}
		
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
			int statusCode = entryComment.post();
			
			if (statusCode == HttpStatus.SC_CREATED) {
				appendComment(entryComment);
				
				Toast toast = Toast.makeText(this, "Comment sent.", Toast.LENGTH_LONG);
				toast.show();
			}
			else {
				Toast toast = Toast.makeText(this, "Oops! Error sending comment.", Toast.LENGTH_LONG);
				toast.show();
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void appendComment(Annotation entryAnnotation) {
		Map<String, String> commentStringData;
		try {
			commentStringData = extractCommentData(entryAnnotation);
			//EntryCommentListAdapter adaptor = (EntryCommentListAdapter)mCommentList.getAdapter();
			//adaptor.addItem(commentStringData);
			
			View commentView = makeCommentView(getLayoutInflater(), commentStringData);
			mCommentsHolder.addView(commentView, 0);
			commentListOffset++;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
