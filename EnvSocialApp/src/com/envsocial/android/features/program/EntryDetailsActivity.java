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

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.envsocial.android.R;
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
	private ListView mCommentList;
	private Button mBtnSend;
	private EditText mComment;
	
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
		
		mCommentList = (ListView) findViewById(R.id.comment_list);
		EntryCommentListAdapter commentListAdapter = new EntryCommentListAdapter(this, getEntryComments());
		mCommentList.setAdapter(commentListAdapter);
		
		mBtnSend = (Button) findViewById(R.id.btn_send);
		mComment = (EditText) findViewById(R.id.comment);
		
		// set Listener for comment send button
		mBtnSend.setOnClickListener(this);
		
		Map<String,String> entry = ProgramEntry.getEntryById(this, mLocation, mEntryId);
		bind(entry);
	}
	
	private LinkedList<Map<String, String>> getEntryComments() {
		try {
			Map<String, String> extra = new HashMap<String, String>();
			extra.put("entry_id", mEntryId);
			extra.put("order_by", "-timestamp");
			extra.put("userexplicit", "true");
			
			List<Annotation> entryAnnotations = Annotation.getAnnotations(this, mLocation, Feature.PROGRAM, extra, 0, 5);
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

	// TODO
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
			EntryCommentListAdapter adaptor = (EntryCommentListAdapter)mCommentList.getAdapter();
			adaptor.addItem(commentStringData);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
