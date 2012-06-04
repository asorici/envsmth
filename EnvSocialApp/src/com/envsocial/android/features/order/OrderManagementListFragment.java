package com.envsocial.android.features.order;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.C2DMReceiver;

public class OrderManagementListFragment extends ListFragment {

	public static final String RESOURCE_URI = "resource_uri";
	public static final String LOCATION_NAME = "location_name";
	public static final String ORDER_DETAILS = "order_details";
	
	private Location mLocation;
	
	private OrderManagementListAdapter mAdapter;
	private OrderReceiver mOrderReceiver;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
	    mAdapter = new OrderManagementListAdapter(getActivity(), getOrders());
	    setListAdapter(mAdapter);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		mOrderReceiver = new OrderReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(C2DMReceiver.ACTION_RECEIVE_NOTIFICATION);
		filter.setPriority(1);
		getActivity().registerReceiver(mOrderReceiver, filter);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		getActivity().unregisterReceiver(mOrderReceiver);
	}
	
	
	private LinkedList<Map<String,String>> getOrders() {
		
		try {
			List<Annotation> orders = Annotation.getAllAnnotationsForEnvironment(getActivity(), 
					mLocation.getId(), 
					Feature.ORDER
					);
			System.out.println("[DEBUG]>> received orders: " + orders);
			
			// TODO: handle multiple order pages
			// TODO: order by smthg?
			return parseOrders(orders);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	// TODO paginatie
	private LinkedList<Map<String,String>> parseOrders(List<Annotation> list) throws JSONException {
		LinkedList<Map<String,String>> orders = new LinkedList<Map<String,String>>();
		
		for (Annotation annotation : list) {
			if (annotation.getCategory().compareTo(Feature.ORDER) != 0) {
				continue;
			}
			
			// Get order
			String orderString = annotation.getData();
			String orderDataString = new JSONObject(orderString).getString("order");
			JSONArray orderData = new JSONArray(orderDataString);
			
			int orderLen = orderData.length();
			String entry = "";
			for (int j = 0; j < orderLen; ++ j) {
				String category = orderData.getJSONObject(j).getString("category");
				String order = orderData.getJSONObject(j).getString("items");
				entry += "<b>" + category + ":</b><br />";
				entry += order.replace("\n", "<br />");
				entry += "<br />";
			}
			
			// Add order to list
			Map<String,String> orderMap = new HashMap<String,String>();
			orderMap.put(RESOURCE_URI, annotation.getUri());
			orderMap.put(LOCATION_NAME, annotation.getLocation().getName());
			orderMap.put(ORDER_DETAILS, entry);
			orders.addLast(orderMap);
		}
		
		return orders;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		// Inflate layout for this fragment.
		return inflater.inflate(R.layout.m_order, container, false);
	}
	
	
	private class OrderReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			System.out.println("[DEBUG]>> App on receive");
			mAdapter.reloadList(getOrders());
			abortBroadcast();
		}
		
	}
	
}
