package com.envsocial.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;

import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.exceptions.EnvSocialComException;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.ResponseHolder;
import com.envsocial.android.widget.Tour;
import com.google.android.gcm.GCMRegistrar;

public class EnvSocialAppActivity extends Activity implements OnClickListener {
	private static final String TAG = "EnvSocialAppActivity";
	
	private Button mBtnAnonymous;
	private Button mBtnRegister;
	private Button mBtnLogin;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (Preferences.getSessionId(this) != null) {
        	// If we already have a session, forward to HomeActivity
        	Intent intent = new Intent(this, HomeActivity.class);
        	Bundle bundle = getIntent().getExtras();
        	if (bundle != null) {
        		intent.putExtras(bundle);
        	}
        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	startActivity(intent);
        	if (Preferences.isLoggedIn(this)) {
        		// If the user is logged in, finish to remove from stack
        		finish();
        		return;
        	}
        }
        
    	// Set up activity
        setContentView(R.layout.main);

        Tour tour = (Tour)findViewById(R.id.tour);
        tour.setAdapter(new ImageAdapter(this));
        
        mBtnAnonymous = (Button) findViewById(R.id.btn_anonymous);
        mBtnAnonymous.setOnClickListener(this);
        
        mBtnRegister = (Button) findViewById(R.id.btn_register);
        mBtnRegister.setOnClickListener(this);
        
        mBtnLogin = (Button) findViewById(R.id.btn_login);
        mBtnLogin.setOnClickListener(this);
    }
    
    @Override
    public void onAttachedToWindow() {
    	super.onAttachedToWindow();
    	Window window = getWindow();
    	window.setFormat(PixelFormat.RGBA_8888);
    }
    
    
    @Override
    public void onRestart() {
    	super.onRestart();
    	Log.d(TAG, "--- onRestart in EnvSocialAppActivity !!!!!!!!!!!!!!");
    	
    	/*
    	if (Preferences.isCheckedIn(getApplicationContext())) {
    		// the flow is such that this method should be called when ending the
    		// HomeActivity as an anonymous user without checking out. Since we don't
    		// like that we're going to perform checkout here.
    		new CheckoutTask().execute();
    	}
    	*/
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	Log.d(TAG, "--- onStart in EnvSocialAppActivity !!!!!!!!!!!!!!");
    }
    
    
    @Override
    public void onStop() {
    	super.onStop();
    	Log.d(TAG, "--- onStop in EnvSocialAppActivity !!!!!!!!!!!!!!");
    }
    
    
    @Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, "--- onPause in EnvSocialAppActivity !!!!!!!!!!!!!");
	}
    
    public void onClick(View v) {
    	if (v == mBtnAnonymous) {
    		// Use app as anonymous, a temporary user account will be created
    		startActivity(new Intent(this, HomeActivity.class));
    	} else if (v == mBtnRegister) {
    		// Register
    		startActivityForResult(new Intent(this, RegisterActivity.class), 0);
    	} else if (v == mBtnLogin) {
    		// Log in
    		startActivityForResult(new Intent(this, LoginActivity.class), 0);
    	}
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode == RESULT_OK) {
    		finish();
    	}
    }
    
    /** Image adapter for Tour widget (service presentation). */
    public class ImageAdapter extends BaseAdapter {
    	
    	private Context mContext;
    	private Integer[] mSlides = {
    			R.drawable.slide1,
    			R.drawable.slide2,
    			R.drawable.slide3,
    			R.drawable.slide4,
    			R.drawable.slide5
    	};
    	
    	public ImageAdapter(Context c) {
    		mContext = c;
    	}

		public int getCount() {
			return mSlides.length;
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imgView = new ImageView(mContext);
			
			imgView.setImageResource(mSlides[position]);
			imgView.setScaleType(ImageView.ScaleType.FIT_XY);
			
			return imgView;
		}
    }
    
    
    private class CheckoutTask extends AsyncTask<Void, Void, ResponseHolder> {
		private ProgressDialog mLoadingDialog;
		
		@Override
		protected void onPreExecute() {
			mLoadingDialog = ProgressDialog.show(EnvSocialAppActivity.this, 
					"", "Checking out ...", true);
		}
		
		@Override
		protected ResponseHolder doInBackground(Void...args) {
			Context context = getApplicationContext();
			
			ResponseHolder response = ActionHandler.checkout(context);
			if (!response.hasError() && !Preferences.isLoggedIn(context)) {
				// unregister from our server notifications
				GCMRegistrar.setRegisteredOnServer(context, false);
			}
			
			return response;
		}
		
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			mLoadingDialog.cancel();
			
			if (holder.hasError()) {
				// TODO: 	for now log the communication error and checkout anyway
				//			need to figure out a way to re-establish consistency 
				
				try {
					throw holder.getError();
				} catch (EnvSocialComException e) {
					Log.d(TAG, e.getMessage(), e);
				} catch (EnvSocialContentException e) {
					Log.d(TAG, e.getMessage(), e);
				} catch (Exception e) {
					Log.d(TAG, e.toString(), e);
				}

				// checkout anyway before going back to the main activity
				Preferences.checkout(getApplicationContext());
			}
		}
	}
}
