package com.envsocial.android.features.program;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.envsocial.android.R;

public class PresentationCommentAlertDialog extends SherlockDialogFragment 
	implements Button.OnClickListener {
	
	private static final String TAG = "PresentationCommentAlertDialog";
	
	private Button mBtnOk;
	
	static PresentationCommentAlertDialog newInstance() {
		PresentationCommentAlertDialog f = new PresentationCommentAlertDialog();
		
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
		
		View v = inflater.inflate(R.layout.program_presentation_comments_alert_dialog, container, false);
		
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
