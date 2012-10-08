package com.envsocial.android.features.order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.envsocial.android.Envived;
import com.envsocial.android.GCMIntentService;
import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.EnvivedNotificationContents;
import com.envsocial.android.utils.EnvivedReceiver;
import com.envsocial.android.utils.UIUtils;

public class OrderManagerFragment extends SherlockFragment {
	
	public static final String RESOURCE_URI = "resource_uri";
	public static final String LOCATION_NAME = "location_name";
	public static final String ORDER_DETAILS = "order_details";
	
	private Location mLocation;

	private ExpandableListView mList;
	private OrderListAdapter mAdapter;
	private NewOrderReceiver mOrderReceiver;
	private ProgressDialog mOrderRetrievalDialog;
	
	private List<Map<String,String>> mOrderLocations;
	private List<List<Map<String,String>>> mOrders;
	private Map<String,Integer> mIndex;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setHasOptionsMenu(true);
	    
	    mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
	    mOrderLocations = new ArrayList<Map<String,String>>();
		mOrders = new ArrayList<List<Map<String,String>>>();
		mIndex = new HashMap<String,Integer>();
		
		// Create custom expandable list adapter
		mAdapter = new OrderListAdapter(getActivity(), 
				mOrderLocations,
				R.layout.order_group, 
				new String[] { LOCATION_NAME },
				new int[] { R.id.order_group }, 
				mOrders, R.layout.order_item,
				new String[] { ORDER_DETAILS },
				new int[] { R.id.order_details }
		);
		
		getOrders();
	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		mOrderReceiver = new NewOrderReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(GCMIntentService.ACTION_RECEIVE_NOTIFICATION);
		filter.setPriority(1);
		getActivity().registerReceiver(mOrderReceiver, filter);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		getActivity().unregisterReceiver(mOrderReceiver);
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		//System.out.println("[DEBUG]>> onCreateView: " + mOrderLocations);
		//System.out.println("[DEBUG]>> onCreateView: " + mOrders);
		return inflater.inflate(R.layout.order, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		if (mList != null) {
			return;
		}
		
		View root = getView();
		if (root == null) {
			throw new IllegalStateException("Content view not yet created");
		}
		if (root instanceof ExpandableListView) {
			mList = (ExpandableListView) root;
		} else {
			mList = (ExpandableListView) root.findViewById(R.id.order_list);
		}
		
		Context appContext = Envived.getContext();
        DisplayMetrics metrics = appContext.getResources().getDisplayMetrics();
        
        int width = metrics.widthPixels;
        mList.setIndicatorBounds(width - UIUtils.getDipsFromPixel(28, appContext), width - UIUtils.getDipsFromPixel(10, appContext));
		mList.setAdapter(mAdapter);
		
		int len = mAdapter.getGroupCount();
		for (int i = 0; i < len; ++ i) {
			mList.expandGroup(i);
		}
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mList = null;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		//System.err.println("[DEBUG]>> In tab order manager menu creator");
		
		super.onCreateOptionsMenu(menu, menuInflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return true;
	}
	
	
	private void getOrders() {
		RetrieveOrdersTask task = new RetrieveOrdersTask();
		task.execute();
	}
	
	// TODO paginatie
	private void parseOrders(List<Annotation> list) throws JSONException {
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
			
			// Register order
			String locationName = annotation.getLocation().getName();
			Map<String,String> orderMap = new HashMap<String,String>();
			orderMap.put(RESOURCE_URI, annotation.getUri());
			orderMap.put(LOCATION_NAME, locationName);
			orderMap.put(ORDER_DETAILS, entry);
			addOrder(locationName, orderMap);
		}
	}
	
	private void loadOrders() {
		mOrderLocations.clear();
		mOrders.clear();
		mIndex.clear();
		
		getOrders();
		
		// TODO retrieve only the orders since the last update
	}
	
	private void addOrder(String locationName, Map<String,String> order) {
		Integer index = mIndex.get(locationName);
		if (index == null) {
			// Received order from a new location
			mIndex.put(locationName, mOrderLocations.size());
			
			// Add location
			Map<String,String> locationMap = new HashMap<String,String>();
			locationMap.put(LOCATION_NAME, locationName);
			mOrderLocations.add(locationMap);
			
			// Add order
			List<Map<String,String>> orderList = new LinkedList<Map<String,String>>();
			orderList.add(order);
			mOrders.add(orderList);
		} else {
			// Else we just add the order
			List<Map<String,String>> orderList = mOrders.get(index);
			orderList.add(order);
		}
	}
	
		
	private class NewOrderReceiver extends EnvivedReceiver {
		
		@Override
		public boolean handleNotification(Context context, Intent intent,
				EnvivedNotificationContents notificationContents) {
			
			JSONObject paramsJSON = notificationContents.getParams();
			
			if (notificationContents.getFeature().equals(Feature.ORDER) 
				&& paramsJSON.optString("type", null) != null 
				&& paramsJSON.optString("type").equalsIgnoreCase(OrderFeature.NEW_ORDER_NOTIFICATION)) {
					
				loadOrders();
				return true;
			}
			
			return false;
		}
		
	}
	
	private class RetrieveOrdersTask extends AsyncTask<Void, Void, List<Annotation>> {
		
		@Override
		protected void onPreExecute() {
			mOrderRetrievalDialog = ProgressDialog.show(OrderManagerFragment.this.getActivity(), 
					"", "Retrieving All Orders ...", true);
		}
		
		@Override
		protected List<Annotation> doInBackground(Void...args) {
			try {
				List<Annotation> orders = Annotation.getAllAnnotationsForEnvironment(getActivity(), 
						mLocation, 
						Feature.ORDER
				);
				
				return orders;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(List<Annotation> orders) {
			mOrderRetrievalDialog.cancel();
			
			if (orders != null) {
				// TODO: handle multiple order pages - currently all annotations are consumed
				// TODO: order by smthg?
				try {
					parseOrders(orders);
					mAdapter.notifyDataSetChanged();
					
				} catch (JSONException e) {
					e.printStackTrace();
					
					Toast toast = Toast.makeText(OrderManagerFragment.this.getActivity(), 
							R.string.msg_get_orders_err, Toast.LENGTH_LONG);
					toast.show();
				}
			}
			else {
				Toast toast = Toast.makeText(OrderManagerFragment.this.getActivity(), 
						R.string.msg_get_orders_err, Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}

}
