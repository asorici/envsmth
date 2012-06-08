package com.envsocial.android.features.program;

import java.util.Map;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Scroller;
import android.widget.TextView;

import com.envsocial.android.R;
import com.envsocial.android.api.ProgramEntry;

public class EntryDetailsActivity extends FragmentActivity {
	private String mEntryId;
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
		
		setContentView(R.layout.entry_details);
		mTitle = (TextView) findViewById(R.id.title);
		mSession = (TextView) findViewById(R.id.session);
		mDatetime = (TextView) findViewById(R.id.datetime);
		mSpeakers = (TextView) findViewById(R.id.speakers);
		mAbstract = (TextView) findViewById(R.id.abs);
		mCommentList = (ListView) findViewById(R.id.comment_list);
		mBtnSend = (Button) findViewById(R.id.btn_send);
		mComment = (EditText) findViewById(R.id.comment);
		
		Map<String,String> entry = ProgramEntry.getEntryById(this, mEntryId);
		bind(entry);
	}
	
	// TODO
	private void bind(Map<String,String> entry) {
		mTitle.setText(entry.get(ProgramDbHelper.COL_ENTRY_TITLE));
		mSession.setText(entry.get(ProgramDbHelper.COL_ENTRY_SESSIONID));
		mDatetime.setText(entry.get(ProgramDbHelper.COL_ENTRY_START_TIME));
		mSpeakers.setText(entry.get(ProgramDbHelper.COL_ENTRY_SPEAKERS));
		mAbstract.setText(entry.get(ProgramDbHelper.COL_ENTRY_ABSTRACT));
	}
}
