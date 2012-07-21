package com.envsocial.android;

import org.apache.http.HttpStatus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.exceptions.EnvSocialComException;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.features.order.OrderFragment;
import com.envsocial.android.features.order.OrderManagerFragment;
import com.envsocial.android.features.people.PeopleFragment;
import com.envsocial.android.features.program.ProgramFragment;
import com.envsocial.android.fragment.DefaultFragment;
import com.envsocial.android.utils.C2DMReceiver;
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.ResponseHolder;


public class DetailsActivity extends SherlockFragmentActivity {
	private static final String TAG = "DetailsActivity";
	
	public static final String ORDER_MANAGEMENT_FEATURE = "order_management";
	public static final String REGISTER_CD2M_ITEM = "Notifications On";
	public static final String UNREGISTER_CD2M_ITEM = "Notifications Off";
	
	private Location mLocation;
	
	private ActionBar mActionBar;
	private Tab mDefaultTab;
	private Tab mProgramTab;
	private Tab mOrderTab;
	private Tab mOrderManagementTab;
	private Tab mPeopleTab;
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details);
        
        mActionBar = getSupportActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        String checkinUrl = getIntent().getStringExtra(ActionHandler.CHECKIN);
        checkin(checkinUrl);
	}
	
	
	private void checkin(String checkinUrl) {
		// Perform check in
		new CheckinTask(checkinUrl).execute();
	}
	
	private void addFeatureTabs() {
        // Add tabs based on features
        ActionBar actionBar = getSupportActionBar();
        
        if (mLocation.hasFeature(Feature.DESCRIPTION)) {
        	System.out.println("[DEBUG] >> Creating DESCRIPTION tab");
        	mDefaultTab = actionBar.newTab()
			.setText(R.string.tab_description)
			.setTabListener(new TabListener<DefaultFragment>(
					this, Feature.DESCRIPTION, DefaultFragment.class, mLocation));
        	actionBar.addTab(mDefaultTab);	
        }
         
        if (mLocation.hasFeature(Feature.PROGRAM)) {
        	mProgramTab = actionBar.newTab()
			.setText(R.string.tab_program)
			.setTabListener(new TabListener<ProgramFragment>(
					this, Feature.PROGRAM, ProgramFragment.class, mLocation));
        	actionBar.addTab(mProgramTab);	
        }
        
        if (mLocation.hasFeature(Feature.ORDER)) {
        	mOrderTab = actionBar.newTab()
			.setText(R.string.tab_order)
			.setTabListener(new TabListener<OrderFragment>(
					this, Feature.ORDER, OrderFragment.class, mLocation));
	        actionBar.addTab(mOrderTab);
	        
	        System.out.println("[DEBUG]>> owner email: " + mLocation.getOwnerEmail());
	        System.out.println("[DEBUG]>> loggedin email: " + Preferences.getLoggedInUserEmail(this));
	        String loggedIn = Preferences.getLoggedInUserEmail(this);
	        if (loggedIn != null && mLocation.isOwnerByEmail(loggedIn)) {
	        	mOrderManagementTab = actionBar.newTab()
				.setText(R.string.tab_order_manager)
				.setTabListener(new TabListener<OrderManagerFragment>(
						this, ORDER_MANAGEMENT_FEATURE, OrderManagerFragment.class, mLocation));
		        actionBar.addTab(mOrderManagementTab);
	        }
        }
        
        if (mLocation.hasFeature(Feature.PEOPLE)) {
        	mPeopleTab = actionBar.newTab()
        			.setText(R.string.tab_people)
        			.setTabListener(new TabListener<PeopleFragment>(
        					this, Feature.PEOPLE, PeopleFragment.class, mLocation));
        	actionBar.addTab(mPeopleTab);
        }
	}
	
	
	public static class TabListener<T extends SherlockFragment> implements ActionBar.TabListener {

		private SherlockFragment mFragment;
		private final Activity mActivity;
		private final String mTag;
		private final Class<T> mClass;
		private Location mLocation;
		
		public TabListener(Activity activity, String tag, Class<T> clz, Location location) {
			mActivity = activity;
			mTag = tag;
			mClass = clz;
			mLocation = location;
		}
		
		// Compatibility library sends null ft, so we simply ignore it and get our own
		public void onTabSelected(Tab tab, FragmentTransaction ingnoredFt) {
			FragmentManager fragmentManager = ((SherlockFragmentActivity) mActivity).getSupportFragmentManager();
	        FragmentTransaction ft = fragmentManager.beginTransaction();
	        
			// Check if the fragment is already initialized
			if (mFragment == null) {
				// If not, instantiate the fragment and add it to the activity
				mFragment = (SherlockFragment) SherlockFragment.instantiate(mActivity, mClass.getName());
				
				Bundle bundle = new Bundle();
				bundle.putSerializable(ActionHandler.CHECKIN, mLocation);
				mFragment.setArguments(bundle);
				
				System.out.println("[DEBUG]>> Adding fragment " + mClass.getName());
				ft.add(R.id.details_containter, mFragment, mTag);
				System.out.println("[DEBUG]>> Adding ok!");
			} else {
				// If the fragment exists, attach it in order to show it
				System.out.println("[DEBUG]>> Attaching fragment.");
				ft.attach(mFragment);
			}
			
			ft.commit();
		}
		
		// Compatibility library sends null ft, so we simply ignore it and get our own
		public void onTabUnselected(Tab tab, FragmentTransaction ingnoredFt) {
			FragmentManager fragmentManager = ((SherlockFragmentActivity) mActivity).getSupportFragmentManager();
	        FragmentTransaction ft = fragmentManager.beginTransaction();
	        
			if (mFragment != null) {
				// Detach the fragment, another one is being attached
				ft.detach(mFragment);
			}
			
			ft.commit();
		}
		
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// Do nothing.
		}		
	}

	private class CheckinTask extends AsyncTask<Void, Void, ResponseHolder> {
		private ProgressDialog mLoadingDialog;
		private String checkinUrl;
		
		public CheckinTask(String checkinUrl) {
			this.checkinUrl = checkinUrl;
		}

		@Override
		protected void onPreExecute() {
			mLoadingDialog = ProgressDialog.show(DetailsActivity.this, 
					"", "Check in ...", true);
		}
		
		@Override
		protected ResponseHolder doInBackground(Void...args) {
			return ActionHandler.checkin(DetailsActivity.this, checkinUrl);
		}
		
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			mLoadingDialog.cancel();
			
			if (!holder.hasError()) {
				if (holder.getCode() == HttpStatus.SC_OK) {
					mLocation = (Location) holder.getTag();

					// TODO: fix padding issue in action bar style xml
					mActionBar.setTitle("     " + mLocation.getName());

					// We have location by now, so add tabs
					addFeatureTabs();
					String feature = getIntent().getStringExtra(C2DMReceiver.FEATURE);
					if (feature != null) {
						// TODO
						mActionBar.selectTab(mOrderManagementTab);
					}
				}
				else if (holder.getCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
					setResult(RESULT_CANCELED);
					Toast toast = Toast.makeText(getApplicationContext(), R.string.msg_bad_checkin_response, Toast.LENGTH_LONG);
					toast.show();
					finish();
				}
				else {
					setResult(RESULT_CANCELED);
					Toast toast = Toast.makeText(getApplicationContext(), R.string.msg_malformed_checkin_url, Toast.LENGTH_LONG);
					toast.show();
					finish();
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
					msgId = R.string.msg_bad_checkin_response;
				} catch (Exception e) {
					Log.d(TAG, e.toString(), e);
					msgId = R.string.msg_service_error;
				}

				setResult(RESULT_CANCELED);
				Toast toast = Toast.makeText(getApplicationContext(), msgId, Toast.LENGTH_LONG);
				toast.show();
				finish();
			}
		}
	}
}
