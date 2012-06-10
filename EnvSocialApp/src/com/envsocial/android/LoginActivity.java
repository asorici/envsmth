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

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.envsocial.android.api.ActionHandler;

public class LoginActivity extends SherlockFragmentActivity implements OnClickListener {

	private EditText mTxtEmail;
	private EditText mTxtPassword;
	private Button mBtnLogin;
	
	private ProgressDialog mLoadingDialog;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        
        mTxtEmail = (EditText) findViewById(R.id.txt_email);
        mTxtPassword = (EditText) findViewById(R.id.txt_password);
        // TODO dummy data for testing
        mTxtEmail.setText("user_1@email.com");
        mTxtPassword.setText("pass_1");
        mBtnLogin = (Button) findViewById(R.id.btn_login);
        mBtnLogin.setOnClickListener(this);
	}

	public void onClick(View v) {
		if (v == mBtnLogin) {
			new LoginTask().execute();
		}
	}
	
	private class LoginTask extends AsyncTask<Void, Void, Integer> {

		private String mEmail;
		private String mPassword;
		
		@Override
		protected void onPreExecute() {
			mLoadingDialog = ProgressDialog.show(LoginActivity.this, 
					"", "Loading...", true);
			mEmail = mTxtEmail.getText().toString();
			mPassword = mTxtPassword.getText().toString();
		}
		
		@Override
		protected Integer doInBackground(Void...args) {
			return ActionHandler.login(getApplicationContext(), mEmail, mPassword);
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
			} else {
				Toast toast = Toast.makeText(getApplicationContext(), 
						R.string.msg_service_unavailable, Toast.LENGTH_LONG);
				toast.show();
			}
		}
		
	}
}
