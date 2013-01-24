package com.envsocial.android.features.order;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.EnvivedFeatureDataRetrievalService;
import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.viewpagerindicator.TitlePageIndicator;

public class OrderFragment extends SherlockFragment implements OnClickListener, ISendOrderRequest {
	private static final String TAG = "OrderFragment";
	private static boolean active = false;
	
	private static final String CALL_WAITER = "Call Waiter";
	private static final String CALL_CHECK = "Call Check";
	
	// mapping of selections by the item ID contained within them
	private static SparseArray<Map<String, Object>> mOrderTab;
	
	public static SparseArray<Map<String,Object>> getOrderTabInstance() {
		if (mOrderTab == null) {
			mOrderTab = new SparseArray<Map<String,Object>>();
		}
		
		return mOrderTab;
	}
	
	public static final int DIALOG_REQUEST = 0;
	
	private Location mLocation;
	private OrderFeature mOrderFeature;
	
	private Button mBtnOrder;
	private Button mBtnTab;
	
	private ViewPager mCatalogPager;
	private TitlePageIndicator mTitlePageIndicator;
	private OrderCatalogPagerAdapter mCatalogPagerAdapter;
	private Bundle mPagerStateBundle;
	private List<Map<String, Object>> mCurrentOrderSelections;
	
	
	private OrderFeatureDataReceiver mFeatureDataReceiver;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		
		mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
		mOrderFeature = (OrderFeature)mLocation.getFeature(Feature.ORDER);
		//mCatalogPagerAdapter = new OrderCatalogPagerAdapter(this);
		
		// register the order feature update receiver here
		mFeatureDataReceiver = new OrderFeatureDataReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(EnvivedFeatureDataRetrievalService.ACTION_FEATURE_RETRIEVE_DATA);
		getActivity().registerReceiver(mFeatureDataReceiver, filter, 
						EnvivedFeatureDataRetrievalService.FEATURE_RETRIEVE_DATA_PERMISSION, null);
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		
		Log.d(TAG, " --- onCreateView called in OrderFragment");
		
		// Inflate layout for this fragment.
		View v = inflater.inflate(R.layout.catalog, container, false);
		
		// find required views
		mTitlePageIndicator = (TitlePageIndicator) v.findViewById(R.id.catalog_page_titles);
		mCatalogPager = (ViewPager) v.findViewById(R.id.catalog_pager);
		
		
		mBtnOrder = (Button) v.findViewById(R.id.btn_order);
		mBtnOrder.setOnClickListener(this);
	    
		mBtnTab = (Button) v.findViewById(R.id.btn_tab);
		mBtnTab.setOnClickListener(this);
	    
	    return v;
	}

	
	@Override
	public void onPause() {
		Log.d(TAG, " --- onPause called in OrderFragment");
		active = false;
		super.onPause();
	}
	
	
	@Override
	public void onStop() {
		Log.d(TAG, " --- onStop called in OrderFragment");
		super.onStop();
		
		// destroy all views in the catalog pager - they will be recreated onStart
		mPagerStateBundle = mCatalogPagerAdapter.onSaveInstanceState();
		mCatalogPager.removeAllViews();
	}
	
	
	@Override
	public void onDestroyView() {
		Log.d(TAG, " --- onDestroyView called in OrderFragment");
		super.onDestroyView();
	}
	
	
	@Override
	public void onDestroy() {
		Log.d(TAG, " --- onDestroy called in OrderFragment");
		super.onDestroy();
		
		SparseArray<Map<String, Object>> orderTab = getOrderTabInstance();
		orderTab.clear();
		orderTab = null;
		
		mOrderFeature.doCleanup(getActivity().getApplicationContext());
		mCatalogPagerAdapter.doCleanup();
		getActivity().unregisterReceiver(mFeatureDataReceiver);
	}
	
	
	@Override
	public void onResume() {
		Log.d(TAG, " --- onResume called in OrderFragment");
		active = true;
		super.onResume();
	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, " --- onStart called in OrderFragment");
		if (!mOrderFeature.isInitialized()) {
			try {
				mOrderFeature.init();
			} catch (EnvSocialContentException e) {
				Log.d(TAG, "[ERROR] >> Could not initialize order feature. ", e);
			}
		}
		
		// (re)initialize pager adapter
		mCatalogPagerAdapter = new OrderCatalogPagerAdapter(this);
		if (mPagerStateBundle != null) {
			mCatalogPagerAdapter.onRestoreInstanceState(mPagerStateBundle);
		}
		mCatalogPager.setAdapter(mCatalogPagerAdapter);
		
		//Bind the title indicator to the adapter
		mCatalogPagerAdapter.setTitlePageIndicator(mTitlePageIndicator);
		mTitlePageIndicator.setViewPager(mCatalogPager);
		
	}
	
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		// add the register/unregister for notifications menu options
     	menu.add(CALL_WAITER);
     	menu.add(CALL_CHECK);
		
		super.onCreateOptionsMenu(menu, menuInflater);
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle().toString().compareTo(CALL_WAITER) == 0) {
			sendCallWaiterRequest();
			return true;
		}
		else if (item.getTitle().toString().compareTo(CALL_CHECK) == 0) {
			sendCallCheckRequest();
			return true;
		}
		
		return false;
	}
	
	
	@Override
	public void onClick(View v) {
		
		if (v == mBtnOrder) {
			List<Map<String, Object>> orderSelections = mCatalogPagerAdapter.getOrderSelections();
			
			if (!orderSelections.isEmpty()) {
				OrderDialogFragment summaryDialog = OrderDialogFragment.newInstance(orderSelections);
				summaryDialog.setTargetFragment(this, DIALOG_REQUEST);
				summaryDialog.show(getFragmentManager(), "dialog");
			}
		}
		else if (v == mBtnTab) {
			
			SparseArray<Map<String,Object>> orderTab = getOrderTabInstance();
			List<Map<String, Object>> orderTabSelections = new ArrayList<Map<String,Object>>();
			for (int i = 0; i < orderTab.size(); i++) {
				orderTabSelections.add(orderTab.valueAt(i));
			}
			
			
			// switch to the order tab view fragment - replace current fragment in the details container
			// of DetailsActivity but use the same Feature.ORDER tag
			FragmentManager fm = getFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			
			OrderTabFragment tabFragment = OrderTabFragment.newInstance(mLocation, orderTabSelections);
			ft.replace(R.id.details_containter, tabFragment, Feature.ORDER);
			ft.addToBackStack(Feature.ORDER);
			
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
		}
	}
	
	
	private void sendCallWaiterRequest() {
		try {
			JSONObject orderMessageJSON = new JSONObject();
			orderMessageJSON.put(OrderFeature.REQUEST_TYPE, OrderFeature.CALL_WAITER_NOTIFICATION);
		
			Annotation orderRequest = new Annotation(mLocation, Feature.ORDER, 
											Calendar.getInstance(), orderMessageJSON.toString());
			new SendOrderRequestTask(getActivity(), 
					this, OrderFeature.CALL_WAITER_NOTIFICATION, orderRequest).execute();
			
		} catch (JSONException e) {
			Log.d(TAG, "Error building order message json: ", e);
		}
	}
	
	private void sendCallCheckRequest() {
		try {
			JSONObject orderMessageJSON = new JSONObject();
			orderMessageJSON.put(OrderFeature.REQUEST_TYPE, OrderFeature.CALL_CHECK_NOTIFICATION);
		
			Annotation orderRequest = new Annotation(mLocation, Feature.ORDER, 
											Calendar.getInstance(), orderMessageJSON.toString());
			new SendOrderRequestTask(getActivity(), 
					this, OrderFeature.CALL_CHECK_NOTIFICATION, orderRequest).execute();
			
		} catch (JSONException e) {
			Log.d(TAG, "Error building order message json: ", e);
		}
	}
	
	
	@Override
	public void sendOrder(OrderDialogFragment dialog) {
		String orderJSON = dialog.getOrderJSONString();
		
		// hold on to current order selections
		mCurrentOrderSelections = dialog.getOrderSelections();
		dialog.dismiss();
		
		Annotation order = new Annotation(mLocation, Feature.ORDER, Calendar.getInstance(), orderJSON);
		new SendOrderRequestTask(getActivity(), this, OrderFeature.NEW_ORDER_NOTIFICATION, order).execute();
	}
	
	
	@Override
	public void postSendOrderRequest(String orderRequestType, Annotation orderRequest, boolean success) {
		if (orderRequestType.compareTo(OrderFeature.NEW_ORDER_NOTIFICATION) == 0) {
			if (success) {
				SparseArray<Map<String, Object>> orderTab = getOrderTabInstance();
				
				// add current selections to tab then clear them
				for (Map<String, Object> itemData : mCurrentOrderSelections) {
					int itemId = (Integer) itemData.get(OrderFeature.ITEM_ID);
					
					Map<String, Object> itemTab = orderTab.get(itemId);
					if (itemTab == null) {
						itemTab = new HashMap<String, Object>();
						itemTab.putAll(itemData);
						
						orderTab.put(itemId, itemTab);
					}
					else {
						Integer tabQuantity = (Integer)itemTab.get("quantity");
						tabQuantity += (Integer)itemData.get("quantity");
						itemTab.put("quantity", tabQuantity);
					}
				}
			}
			
			// clear current temporary selections in OrderFragment
			mCurrentOrderSelections = null;
			
			// clear them in the pager adapter as well
			mCatalogPagerAdapter.clearOrderSelections();
		}
	}
	
	
	Location getCurrentLocation() {
		return mLocation;
	}
	
	OrderFeature getOrderFeature() {
		return mOrderFeature;
	}
	
	void setOrderFeature(OrderFeature feature) {
		mOrderFeature = feature;
	}
	
	private class OrderFeatureDataReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// get the intent extras
			final Bundle extras = intent.getExtras();
			
			// get the feature category for which an update was performed
			String featureCategory = extras.getString("feature_category");
			
			Log.d(TAG, "Received update order notification with category: " + featureCategory);
			
			if (featureCategory.equals(Feature.ORDER)) {
				// check if the fragment is currently active
				if (active && mOrderFeature.isInitialized()) {
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					LayoutInflater inflater = getActivity().getLayoutInflater();

					TextView titleDialogView = (TextView) inflater.inflate(
							R.layout.catalog_update_dialog_title, null, false);
					titleDialogView.setText("Allow menu content update?");

					String dialogMessage = "An update for the menu in "
							+ mLocation.getName()
							+ " "
							+ "has been issued. Press YES if you want to do the update. "
							+ "If you want to keep your current activity, choose NO. "
							+ "You can later update the menu by checking out and then checking in again.";

					TextView bodyDialogView = (TextView) inflater.inflate(
							R.layout.catalog_update_dialog_body, null, false);
					bodyDialogView.setText(dialogMessage);

					builder.setCustomTitle(titleDialogView);
					builder.setView(bodyDialogView);

					builder.setPositiveButton("Yes",
							new Dialog.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
									
									OrderFeature updatedOrderFeature = (OrderFeature) extras
											.getSerializable("feature_content");
									
									// notify the adapter to do the update
									mCatalogPagerAdapter.updateFeature(updatedOrderFeature);
								}
							});

					builder.setNegativeButton("No",
							new Dialog.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}

							});

					builder.show();

				} else {
					OrderFeature updatedOrderFeature = (OrderFeature) extras.getSerializable("feature_content");
					
					// notify the adapter directly
					mCatalogPagerAdapter.updateFeature(updatedOrderFeature);
				}
				
				abortBroadcast();
			}
		}
		
	}
}
