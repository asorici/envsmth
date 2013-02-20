package com.envsocial.android.features.program;

import org.json.JSONObject;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.envsocial.android.R;

public class PresentationCommentsDialogFragment extends SherlockDialogFragment 
	implements Button.OnClickListener {
	
	private static final String TAG = "PresentationCommentsDialogFragment";
	
	private int mPresentationId;
	
	private Button mBtnSend;
	private Button mBtnCancel;
	private EditText mMsgContent;
	
	static PresentationCommentsDialogFragment newInstance() {
		PresentationCommentsDialogFragment f = new PresentationCommentsDialogFragment();
		
		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mPresentationId = getArguments().getInt("presentation_id");
		setStyle(STYLE_NO_TITLE, R.style.CommentsDialogTheme);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.program_presentation_comments_dialog, container, false);
		mMsgContent = (EditText) v.findViewById(R.id.msg_content);
		
		mBtnSend = (Button) v.findViewById(R.id.btn_send);
		mBtnCancel = (Button) v.findViewById(R.id.btn_cancel);
		mBtnSend.setOnClickListener(this);
		mBtnCancel.setOnClickListener(this);
		
		return v;
	}
	
	private String getCommentJSONString() {
		try {
			String messageContent = mMsgContent.getText().toString();
			
			if (messageContent.equals("")) {
				return null;
			}
			
			JSONObject commentJSON = new JSONObject();
			
			commentJSON.put("presentation_id", mPresentationId);
			commentJSON.put("text", messageContent);
			
			return commentJSON.toString();
		} catch (Exception e) {
			//Log.d(TAG, "Error building new comment JSON: ", e);
		}
		
		return null;
	}


	@Override
	public void onClick(View v) {
		if (v == mBtnSend) {
			PresentationCommentsActivity cm = (PresentationCommentsActivity) getActivity();
			String messageContent = getCommentJSONString();
			if (messageContent != null) {
				dismiss();
				cm.sendComment(messageContent);
			}
		} 
		else if (v == mBtnCancel) {
			dismiss();
		}
	}
}
