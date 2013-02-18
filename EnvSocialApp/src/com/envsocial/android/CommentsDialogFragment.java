package com.envsocial.android;

import org.json.JSONObject;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class CommentsDialogFragment extends SherlockDialogFragment implements Button.OnClickListener {

	private static final String TAG = "CommentsDialogFragment";
	private final String TITLE = "Add Comment";
	
	private Button mBtnSend;
	private Button mBtnCancel;
	private String mMsgSubject;
	private EditText mMsgContent;
	
	private CommentsDialogFragment (String subject) {
		mMsgSubject = subject;
	}
	
	static CommentsDialogFragment newInstance(String subject) {
		CommentsDialogFragment f = new CommentsDialogFragment(subject);
		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setStyle(STYLE_NO_TITLE, R.style.CommentsDialogTheme);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		getDialog().setTitle(TITLE);
		View v = inflater.inflate(R.layout.comments_dialog, container, false);
		
		//mMsgSubject = (EditText) v.findViewById(R.id.msg_subject);
		mMsgContent = (EditText) v.findViewById(R.id.msg_content);
		
		mBtnSend = (Button) v.findViewById(R.id.btn_send);
		mBtnCancel = (Button) v.findViewById(R.id.btn_cancel);
		mBtnSend.setOnClickListener(this);
		mBtnCancel.setOnClickListener(this);
		
		return v;
	}
	
	public String getCommentJSONString() {
		try {
			JSONObject commentJSON = new JSONObject();
			
			commentJSON.put("topic_type", "booth_description");
			//commentJSON.put("product_id", "test_product_id");
			commentJSON.put("text", mMsgContent.getText().toString());
			commentJSON.put("topic_title", mMsgSubject);
			
			return commentJSON.toString();
			
		} catch (Exception e) {
			Log.d(TAG, "Error building new comment JSON: ", e);
		}
		
		return null;
	}


	@Override
	public void onClick(View v) {
	    if(v==mBtnSend)
	       {
	        	   CommentsActivity cm = (CommentsActivity)getActivity();
	        	   cm.sendComment(this);
	       }
	           else if (v==mBtnCancel) {
	    	   dismiss();
	       }
	}
}
