package com.envsocial.android.utils;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.envsocial.android.R;

public class EnvivedCommentAlertDialog extends SherlockDialogFragment 
	implements Button.OnClickListener {
	
	private static final String TAG = "EnvivedCommentAlertDialog";
	
	private String mMessage;
	private TextView mMessageView;
	private Button mBtnOk;
	
	public static EnvivedCommentAlertDialog newInstance(String message) {
		EnvivedCommentAlertDialog f = new EnvivedCommentAlertDialog();
		
		Bundle args = new Bundle();
		args.putString("message", message);
		f.setArguments(args);
		
		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(STYLE_NO_TITLE, R.style.CommentsDialogTheme);
		
		mMessage = getArguments().getString("message");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.envived_comments_alert_dialog, container, false);
		
		mMessageView = (TextView) v.findViewById(R.id.msg_content);
		mMessageView.setText(mMessage);
		
		mBtnOk = (Button) v.findViewById(R.id.btn_ok);
		mBtnOk.setOnClickListener(this);
		
		return v;
	}
	
	@Override
	public void onClick(View v) {
		if (v == mBtnOk) {
			dismiss();
		} 
		
	}
}
