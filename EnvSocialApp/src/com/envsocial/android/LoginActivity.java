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

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.exceptions.EnvSocialComException;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.program.EntryDetailsActivity;
import com.envsocial.android.utils.ResponseHolder;

public class LoginActivity extends SherlockFragmentActivity implements OnClickListener {
	private static final String TAG = "LoginActivity";
	
	private EditText mTxtEmail;
	private EditText mTxtPassword;
	private Button mBtnLogin;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        
        mTxtEmail = (EditText) findViewById(R.id.txt_email);
        mTxtPassword = (EditText) findViewById(R.id.txt_password);
       
        
        mBtnLogin = (Button) findViewById(R.id.btn_login);
        mBtnLogin.setOnClickListener(this);
	}

	public void onClick(View v) {
		if (v == mBtnLogin) {
			new LoginTask().execute();
		}
	}
	
	private class LoginTask extends AsyncTask<Void, Void, ResponseHolder> {
		private ProgressDialog mLoadingDialog;
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
		protected ResponseHolder doInBackground(Void...args) {
			return ActionHandler.login(LoginActivity.this, mEmail, mPassword);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			mLoadingDialog.cancel();
			if (!holder.hasError()) {
				if (holder.getCode() == HttpStatus.SC_OK) {
					startActivity(new Intent(LoginActivity.this, HomeActivity.class));
					setResult(RESULT_OK);
					finish();
				} else if(holder.getCode() == HttpStatus.SC_UNAUTHORIZED) {
					// handle errors
					try {
						Map<String, Object> errorDict = ResponseHolder.getActionErrorMessages(holder.getJsonContent());
						String errorTitle = (String)errorDict.get("msg");
						List<String> errorList = (List<String>)errorDict.get("errors");

						AlertDialog infoDialog = buildInfoDialog(errorTitle, errorList);
						infoDialog.show();
					} catch (JSONException ex) {
						Log.d(TAG, ex.getMessage());
						Toast toast = Toast.makeText(LoginActivity.this, 
								R.string.msg_unauthorized_login, Toast.LENGTH_LONG);
						toast.show();
					}
				} else {
					Toast toast = Toast.makeText(LoginActivity.this, 
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
					msgId = R.string.msg_bad_login_response;
				} catch (Exception e) {
					Log.d(TAG, e.toString(), e);
					msgId = R.string.msg_service_error;
				}

				Toast toast = Toast.makeText(LoginActivity.this, msgId, Toast.LENGTH_LONG);
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
