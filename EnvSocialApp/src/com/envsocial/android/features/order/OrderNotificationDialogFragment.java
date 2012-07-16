package com.envsocial.android.features.order;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.utils.C2DMReceiver;
import com.google.android.c2dm.C2DMessaging;

public class OrderNotificationDialogFragment extends DialogFragment implements OnClickListener {
	private final String NOTIFICATION_TITLE = "Order Notifications";
	
	private Button mBtnRegister;
	private Button mBtnCancel;
	
	static OrderNotificationDialogFragment newInstance() {
		OrderNotificationDialogFragment f = new OrderNotificationDialogFragment();
		return f;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Set title
		getDialog().setTitle(NOTIFICATION_TITLE);
		
		View v = inflater.inflate(R.layout.order_notification_dialog, container, false);
		TextView info = (TextView) v.findViewById(R.id.notification_info_message);
		
		info.setText("You are not yet registered to receive new order notifications.\nPress the Register" +
				" button below to do so.\nYou can also do this later from the menu options.");
		
		mBtnRegister = (Button) v.findViewById(R.id.btn_order_notification_register);
		mBtnRegister.setOnClickListener(this);
		
		mBtnCancel = (Button) v.findViewById(R.id.btn_order_notification_cancel);
		mBtnCancel.setOnClickListener(this);
		
		return v;
	}
	
	@Override
	public void onClick(View v) {
		if (v == mBtnRegister) {
			C2DMessaging.register(this.getTargetFragment().getActivity(), C2DMReceiver.SENDER_ID);
			String regId = C2DMessaging.getRegistrationId(this.getTargetFragment().getActivity());
            if (regId != null && !"".equals(regId)) {
            	try {
            		ActionHandler.registerWithServer(this.getTargetFragment().getActivity(), regId);
            		Toast toast = Toast.makeText(this.getTargetFragment().getActivity(), 
    						"Registered for notifications", Toast.LENGTH_LONG);
    				toast.show();
            	} catch (Exception e) {
            		e.printStackTrace();
            		Toast toast = Toast.makeText(this.getTargetFragment().getActivity(), 
    						"Error registering for notifications", Toast.LENGTH_LONG);
    				toast.show();
            	}
            }
            
            dismiss();
		} else if (v == mBtnCancel) {
			dismiss();
		}
	}

}
