package com.envsocial.android;

import org.apache.http.HttpStatus;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.envsocial.android.api.ActionHandler;

public class RegisterActivity extends SherlockActivity implements OnClickListener {

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
        
        mBtnSubmit = (Button) findViewById(R.id.btn_submit);
        mBtnSubmit.setOnClickListener(this);
	}

	public void onClick(View v) {
		if (v == mBtnSubmit) {
			new LoginTask().execute();
		}
	}
	
	private class LoginTask extends AsyncTask<Void, Void, Integer> {

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
		protected Integer doInBackground(Void...args) {
			// TODO Register before login
			if (mFirst.isEmpty() || mLast.isEmpty()) {
				return HttpStatus.SC_NOT_ACCEPTABLE;
			}
			else {
				return ActionHandler.register(getApplicationContext(), mEmail, mPassword, mFirst, mLast, mAffiliation, mInterests);
			}
		}
		
		@Override
		protected void onPostExecute(Integer statusCode) {
			mLoadingDialog.cancel();
			if (statusCode == HttpStatus.SC_OK) {
				startActivity(new Intent(getApplicationContext(), HomeActivity.class));
				setResult(RESULT_OK);
				finish();
			} else if(statusCode == HttpStatus.SC_UNAUTHORIZED) {
				Toast toast = Toast.makeText(getApplicationContext(), 
						R.string.msg_unauthorized_login, Toast.LENGTH_LONG);
				toast.show();
			} else if (statusCode == HttpStatus.SC_NOT_ACCEPTABLE) {
				Toast toast = Toast.makeText(getApplicationContext(), 
						R.string.msg_registration_details_required, Toast.LENGTH_LONG);
				toast.show();
			}
			else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
				Toast toast = Toast.makeText(getApplicationContext(), 
						R.string.msg_registration_details_check, Toast.LENGTH_LONG);
				toast.show();
			}
			else {
				Toast toast = Toast.makeText(getApplicationContext(), 
						R.string.msg_service_unavailable, Toast.LENGTH_LONG);
				toast.show();
			}
		}
		
	}
}
