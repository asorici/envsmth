package com.envsocial.android;

import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.JSONException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.exceptions.EnvSocialComException;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.program.EntryDetailsActivity;
import com.envsocial.android.utils.ResponseHolder;

public class RegisterActivity extends SherlockActivity implements OnClickListener {
	private static final String TAG = "RegisterActivity";
	
	private EditText mTxtEmail;
	private EditText mTxtPassword;
	private EditText mTxtFirst;
	private EditText mTxtLast;
	private EditText mTxtAffiliation;
	private EditText mTxtInterests;
	private Button mBtnSubmit;
	
	private ProgressDialog mLoadingDialog;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        
        mTxtEmail = (EditText) findViewById(R.id.txt_email);
        mTxtPassword = (EditText) findViewById(R.id.txt_password);
        
        mTxtFirst = (EditText) findViewById(R.id.txt_first);
        mTxtLast = (EditText) findViewById(R.id.txt_last);
        mTxtAffiliation = (EditText) findViewById(R.id.txt_affiliation);
        mTxtInterests = (EditText) findViewById(R.id.txt_interests);
        
        /*
        mTxtEmail.setText("unmail@email.com");
        mTxtPassword.setText("unpass");
        mTxtFirst.setText("Andrei");
        mTxtLast.setText("Ciortea");
        mTxtAffiliation.setText("Univ. Politehnica Bucuresti");
        mTxtInterests.setText("multi-agent systems, ambient intelligence, internet of things");
        */
        
        mBtnSubmit = (Button) findViewById(R.id.btn_submit);
        mBtnSubmit.setOnClickListener(this);
	}

	public void onClick(View v) {
		if (v == mBtnSubmit) {
			new LoginTask().execute();
		}
	}
	
	private class LoginTask extends AsyncTask<Void, Void, ResponseHolder> {

		private String mEmail;
		private String mPassword;
		private String mFirst;
		private String mLast;
		private String mAffiliation;
		private String mInterests;
		
		@Override
		protected void onPreExecute() {
			mLoadingDialog = ProgressDialog.show(RegisterActivity.this, 
					"", "Loading...", true);
			mEmail = mTxtEmail.getText().toString();
			mPassword = mTxtPassword.getText().toString();
			mFirst = mTxtFirst.getText().toString();
			mLast = mTxtLast.getText().toString();
			mAffiliation = mTxtAffiliation.getText().toString();
			mInterests = mTxtInterests.getText().toString();
		}
		
		@Override
		protected ResponseHolder doInBackground(Void...args) {
			
			if (mFirst.isEmpty() || mLast.isEmpty()) {
				return new ResponseHolder(
						HttpStatus.SC_NOT_ACCEPTABLE,
						getResources().getString(R.string.msg_registration_details_required),
						null);
			} else {
				return ActionHandler.register(RegisterActivity.this,
						mEmail, mPassword, mFirst, mLast, mAffiliation, mInterests);
			}
		}
		
		@SuppressWarnings("unchecked")
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			mLoadingDialog.cancel();
			
			if (!holder.hasError()) {
				int statusCode = holder.getCode();
			
				if (statusCode == HttpStatus.SC_OK) {
					startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
					setResult(RESULT_OK);
					finish();
				} else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
					Toast toast = Toast.makeText(RegisterActivity.this, 
							R.string.msg_unauthorized_login, Toast.LENGTH_LONG);
					toast.show();
				} else if (statusCode == HttpStatus.SC_NOT_ACCEPTABLE) {
					Toast toast = Toast.makeText(RegisterActivity.this, 
							R.string.msg_registration_details_required, Toast.LENGTH_LONG);
					toast.show();
				}
				else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
					try {
						Map<String, Object> errorDict = ResponseHolder.getActionErrorMessages(holder.getJsonContent());
						String errorTitle = (String)errorDict.get("msg");
						List<String> errorList = (List<String>)errorDict.get("errors");
						
						AlertDialog infoDialog = buildInfoDialog(errorTitle, errorList);
						infoDialog.show();
					} catch (JSONException ex) {
						Log.d(TAG, ex.getMessage());
						Toast toast = Toast.makeText(RegisterActivity.this, 
								R.string.msg_registration_details_check, Toast.LENGTH_LONG);
						toast.show();
					}
				}
				else {
					Toast toast = Toast.makeText(RegisterActivity.this, 
							R.string.msg_service_error, Toast.LENGTH_LONG);
					toast.show();
				}
			}
			else {
				int msgId = R.string.msg_service_unavailable;
				
				try {
					throw holder.getError();
				} catch (EnvSocialComException e) {
					Log.d(TAG, e.getMessage(), e);
					msgId = R.string.msg_service_unavailable;
				} catch (EnvSocialContentException e) {
					Log.d(TAG, e.getMessage(), e);
					msgId = R.string.msg_bad_register_response;
				} catch (Exception e) {
					Log.d(TAG, e.toString(), e);
					msgId = R.string.msg_service_error;
				}
				
				Toast toast = Toast.makeText(RegisterActivity.this, msgId, Toast.LENGTH_LONG);
				toast.show();
			}
		}
		
	}
	
	private AlertDialog buildInfoDialog(String title, List<String> messages) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		LayoutInflater inflater = getLayoutInflater();
		TextView titleDialogView = (TextView)inflater.inflate(R.layout.dialog_generic_title, null, false);
		titleDialogView.setText(title);
		
		
		TextView bodyDialogView = (TextView)inflater.inflate(R.layout.dialog_generic_body, null, false);
		String dialogMessage = "Errors: \n\n";
		
		for (String message : messages) {
			dialogMessage += message + "\n\n";
		}
		
		bodyDialogView.setText(dialogMessage);
		
		builder.setCustomTitle(titleDialogView);
		builder.setView(bodyDialogView);
		
		builder.setPositiveButton("OK", new Dialog.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) { 
		    	dialog.cancel();
		    }
		});
		
		return builder.create();
	}
}
