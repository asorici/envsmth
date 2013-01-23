package com.envsocial.android.features.order;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.envsocial.android.R;

public class OrderCustomAlertDialogFragment extends SherlockDialogFragment implements OnClickListener {
	private static final String TAG = "OrderCustomAlertDialogFragment";
	private static final String ALERT_DIALOG_TITLE = "alert_dialog_title";
	private static final String ALERT_DIALOG_MESSAGE_TEXT = "alert_dialog_message_text";
	private static final String ALERT_DIALOG_POSITIVE_BUTTON_TEXT = "alert_dialog_positive_button_text";
	private static final String ALERT_DIALOG_NEGATIVE_BUTTON_TEXT = "alert_dialog_negative_button_text";
	
//	private final Context mContext;
    private LinearLayout mLayoutWrapper;
	private TextView mTitle;
    private View mMessage;
    private Button mPositiveButton;
    private Button mNegativeButton;
    private OrderNoticeAlertDialogListener mAlertDialogListener;

    public interface OrderNoticeAlertDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }
    
    public static OrderCustomAlertDialogFragment newInstance(String titleText, String messageText, 
    		String positiveButtonText, String negativeButtonText) {
    	OrderCustomAlertDialogFragment f = new OrderCustomAlertDialogFragment();
		
		Bundle args = new Bundle();
		args.putString(ALERT_DIALOG_TITLE, titleText);
		args.putString(ALERT_DIALOG_MESSAGE_TEXT, messageText);
		args.putString(ALERT_DIALOG_POSITIVE_BUTTON_TEXT, positiveButtonText);
		args.putString(ALERT_DIALOG_NEGATIVE_BUTTON_TEXT, negativeButtonText);
		
		f.setArguments(args);
		return f;
	}
    
    
    public void setOrderNoticeAlertDialogListener(OrderNoticeAlertDialogListener listener) {
    	mAlertDialogListener = listener;
    }
    
    
    public void setMessageView(View messageView) {
    	mMessage = messageView;
    }
    
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(STYLE_NO_TITLE, R.style.FeatureOrderDialogTheme);
	}
    
    
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View v = inflater.inflate(R.layout.catalog_custom_alert_dialog, container, false);
    	
    	mLayoutWrapper = (LinearLayout) v.findViewById(R.id.alert_dialog_wrapper);
    	mTitle = (TextView) v.findViewById(R.id.alert_dialog_title);
    	
    	mPositiveButton = (Button) v.findViewById(R.id.alert_dialog_btn_positive);
    	mPositiveButton.setOnClickListener(this);
    	
    	mNegativeButton = (Button) v.findViewById(R.id.alert_dialog_btn_negative);
    	mNegativeButton.setOnClickListener(this);
    	
    	// retrieve data from args
    	Bundle args = getArguments();
    	String titleText = args.getString(ALERT_DIALOG_TITLE);
    	mTitle.setText(titleText);
    	
    	String positiveButtonString = args.getString(ALERT_DIALOG_POSITIVE_BUTTON_TEXT);
    	mPositiveButton.setText(positiveButtonString);
    	
    	String negativeButtonString = args.getString(ALERT_DIALOG_NEGATIVE_BUTTON_TEXT);
    	mNegativeButton.setText(negativeButtonString);
    	
    	// if it hasn't been set inflate the default text view
    	if (mMessage == null) {
	    	mMessage = inflater.inflate(R.layout.catalog_custom_alert_dialog_message, (ViewGroup) mLayoutWrapper, false);
			TextView messageView = (TextView) mMessage.findViewById(R.id.message);
			
			String messageText = args.getString(ALERT_DIALOG_MESSAGE_TEXT);
	    	if (messageText != null) {
	    		messageView.setText(messageText);
	    	}
    	}
    	
		// add message view after the title - so index 1
		mLayoutWrapper.addView(mMessage, 1);
    	
    	return v;
    }


	@Override
	public void onClick(View v) {
		if (v == mPositiveButton) {
			mAlertDialogListener.onDialogPositiveClick(this);
		}
		else if (v == mNegativeButton) {
			mAlertDialogListener.onDialogNegativeClick(this);
		}
	}
}