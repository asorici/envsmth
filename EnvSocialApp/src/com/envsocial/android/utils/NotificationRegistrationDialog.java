package com.envsocial.android.utils;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.envsocial.android.GCMIntentService;
import com.envsocial.android.R;
import com.google.android.gcm.GCMRegistrar;

public class NotificationRegistrationDialog extends DialogFragment implements OnClickListener {
	private final String NOTIFICATION_TITLE = "ENVIVED Notifications";
	
	private Button mBtnRegister;
	private Button mBtnCancel;
	
	public static NotificationRegistrationDialog newInstance() {
		NotificationRegistrationDialog f = new NotificationRegistrationDialog();
		return f;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Set title
		getDialog().setTitle(NOTIFICATION_TITLE);
		
		View v = inflater.inflate(R.layout.notification_registration_dialog, container, false);
		TextView info = (TextView) v.findViewById(R.id.notification_info_message);
		
		info.setText("You are not yet registered to receive ENVIVED notifications.\nENVIVED applications rely on" +
				" this feature for their normal functioning.\nPlease press the Register" +
				" button below to do so.\nYou can also do this later from the menu options.");
		
		mBtnRegister = (Button) v.findViewById(R.id.btn_notification_register);
		mBtnRegister.setOnClickListener(this);
		
		mBtnCancel = (Button) v.findViewById(R.id.btn_notification_cancel);
		mBtnCancel.setOnClickListener(this);
		
		return v;
	}
	
	
	@Override
	public void onClick(View v) {
		if (v == mBtnRegister) {
			GCMRegistrar.register(this.getActivity(), GCMIntentService.SENDER_ID);
			Toast toast = Toast.makeText(this.getActivity(), 
					"Request for notification registration sent.", Toast.LENGTH_LONG);
			toast.show();
            
            dismiss();
		} else if (v == mBtnCancel) {
			dismiss();
		}
	}

}
