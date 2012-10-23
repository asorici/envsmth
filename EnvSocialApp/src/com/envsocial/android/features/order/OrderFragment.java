package com.envsocial.android.features.order;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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
import com.envsocial.android.EnvivedFeatureUpdateService;
import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.viewpagerindicator.TitlePageIndicator;

public class OrderFragment extends SherlockFragment implements OnClickListener, ISendOrder {
	private static final String TAG = "OrderFragment";
	private static boolean active = false;
	
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
	private OrderCatalogPagerAdapter mCatalogPagerAdapter;
	
	private List<Map<String, Object>> mCurrentOrderSelections;
	
	private OrderFeatureUpdateReceiver mUpdateReceiver;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		
		mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
		mOrderFeature = (OrderFeature)mLocation.getFeature(Feature.ORDER);
		mCatalogPagerAdapter = new OrderCatalogPagerAdapter(this);
		
		// register the order feature update receiver here
		mUpdateReceiver = new OrderFeatureUpdateReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(EnvivedFeatureUpdateService.ACTION_UPDATE_FEATURE);
		getActivity().registerReceiver(mUpdateReceiver, filter, 
						EnvivedFeatureUpdateService.UPDATE_PERMISSION, null);
		
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		
		// Inflate layout for this fragment.
		View v = inflater.inflate(R.layout.catalog, container, false);
		
		mCatalogPager = (ViewPager) v.findViewById(R.id.catalog_pager);
		mCatalogPager.setAdapter(mCatalogPagerAdapter);
		
		//Bind the title indicator to the adapter
		TitlePageIndicator titleIndicator = (TitlePageIndicator) v.findViewById(R.id.catalog_page_titles);
		titleIndicator.setViewPager(mCatalogPager);
		mCatalogPagerAdapter.setTitlePageIndicator(titleIndicator);
		
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
	}
	
	@Override
	public void onResume() {
		Log.d(TAG, " --- onResume called in OrderFragment");
		active = true;
		super.onResume();
	}
	
	
	@Override
	public void onStart() {
		Log.d(TAG, " --- onStart called in OrderFragment");
		super.onStart();
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, " --- onDestroy called in OrderFragment");
		super.onDestroy();
		
		SparseArray<Map<String, Object>> orderTab = getOrderTabInstance();
		orderTab.clear();
		orderTab = null;
		
		mCatalogPagerAdapter.doCleanup();
		getActivity().unregisterReceiver(mUpdateReceiver);
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
			
			OrderTabDialogFragment orderTabDialog = OrderTabDialogFragment.newInstance(orderTabSelections);
			orderTabDialog.setTargetFragment(this, DIALOG_REQUEST);
			orderTabDialog.show(getFragmentManager(), "dialog");
		}
		
	}
	
	
	@Override
	public void sendOrder(OrderDialogFragment dialog) {
		String orderJSON = dialog.getOrderJSONString();
		
		// hold on to current order selections
		mCurrentOrderSelections = dialog.getOrderSelections();
		dialog.dismiss();
		
		Annotation order = new Annotation(mLocation, Feature.ORDER, Calendar.getInstance(), orderJSON);
		new SendOrderTask(getActivity(), this, order).execute();
	}
	
	
	@Override
	public void postSendOrder(boolean success) {
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
	
	
	Location getCurrentLocation() {
		return mLocation;
	}
	
	OrderFeature getOrderFeature() {
		return mOrderFeature;
	}
	
	
	private class OrderFeatureUpdateReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// get the intent extras
			Bundle extras = intent.getExtras();
			
			// get the feature category for which an update was performed
			String featureCategory = extras.getString("feature_category");
			
			if (featureCategory.equals(Feature.ORDER)) {
				// get the actual updated feature contents and re-initialize internal structures
				try {
					mOrderFeature = (OrderFeature)extras.getSerializable("feature_content");
					mOrderFeature.doUpdate();
					
					// check if the fragment is currently active
					if (active) {
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						LayoutInflater inflater = getActivity().getLayoutInflater();
						
						TextView titleDialogView = (TextView)inflater.inflate(R.layout.catalog_update_dialog_title, null, false);
						titleDialogView.setText("Allow menu content update?");
						
						String dialogMessage = "An update for the menu in " + mLocation.getName() + " " + 
								"has been issued. Press YES if you want to do the update. " +
								"If you want to keep your current activity, choose NO. " +
								"You can later update the menu by checking out and then checking in again.";
						
						TextView bodyDialogView = (TextView)inflater.inflate(R.layout.catalog_update_dialog_body, null, false);
						bodyDialogView.setText(dialogMessage);
						
						builder.setCustomTitle(titleDialogView);
						builder.setView(bodyDialogView);
						
						builder.setPositiveButton("Yes", new Dialog.OnClickListener() {
						    @Override
						    public void onClick(DialogInterface dialog, int which) { 
						    	dialog.cancel();
						    	
						    	// notify the adapter to do the update
								mCatalogPagerAdapter.updateFeature();
						    }
						});

						builder.setNegativeButton("No", new Dialog.OnClickListener() {
						    @Override
						    public void onClick(DialogInterface dialog, int which) {
						    	dialog.cancel();
						    }

						});

						builder.show();
						
					}
					else {
						// notify the adapter directly
						mCatalogPagerAdapter.updateFeature();
					}
				} catch (EnvSocialContentException ex) {
					Log.d(TAG, "[DEBUG] >> OrderFeature update failed. Content could not be parsed.", ex);
				}
				
				abortBroadcast();
			}
		}
		
	}
}
