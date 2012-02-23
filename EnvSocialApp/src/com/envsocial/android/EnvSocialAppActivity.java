package com.envsocial.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;

import com.envsocial.android.utils.Preferences;
import com.envsocial.android.widget.Tour;

public class EnvSocialAppActivity extends FragmentActivity implements OnClickListener {
	
	private Button mBtnAnonymous;
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
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Tour tour = (Tour)findViewById(R.id.tour);
        tour.setAdapter(new ImageAdapter(this));
        
        mBtnAnonymous = (Button) findViewById(R.id.btn_anonymous);
        mBtnAnonymous.setOnClickListener(this);
        
        mBtnLogin = (Button) findViewById(R.id.btn_login);
        mBtnLogin.setOnClickListener(this);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main_activity, menu);
    	return true;
    }
    
    public void onClick(View v) {
    	if (v == mBtnAnonymous) {
    		// Use app as anonymous, a temporary user account will be created
    		startActivity(new Intent(this, HomeActivity.class));
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
    			R.drawable.tour0,
    			R.drawable.tour1,
    			R.drawable.tour2,
    			R.drawable.tour3,
    			R.drawable.tour4,
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
}
