package com.envsocial.android.features.order;

import java.util.ArrayList;
import java.util.Calendar;
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
	
	protected static final String RESOURCE_URI = "resource_uri";
	protected static final String LOCATION_NAME = "location_name";
	protected static final String ORDER_DETAILS = "order_details";
	protected static final String ORDER_TIMESTAMP = "timestamp";
	
	private static final String REFRESH_ORDERS_MENU_ITEM = "Refresh Orders";
	
	private Location mLocation;
	private Calendar lastRefreshTimestamp;

	private ExpandableListView mList;
	private OrderManagerListAdapter mAdapter;
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
		mAdapter = new OrderManagerListAdapter(getActivity(), 
				mOrderLocations,
				R.layout.order_group, 
				new String[] { LOCATION_NAME },
				new int[] { R.id.order_group }, 
				mOrders, R.layout.order_item,
				new String[] { ORDER_DETAILS },
				new int[] { R.id.order_details }
		);
		
		getOrders(null, false);
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
		// add the register/unregister for notifications menu options
     	menu.add(REFRESH_ORDERS_MENU_ITEM);
		
		super.onCreateOptionsMenu(menu, menuInflater);
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle().toString().compareTo(REFRESH_ORDERS_MENU_ITEM) == 0) {
			
			// get all orders since the last one handled
			getOrders(lastRefreshTimestamp, false);
			return true;
		}
		
		return false;
	}
	
	
	private void getOrders(Calendar timestamp, boolean reverseOrder) {
		RetrieveOrdersTask task = new RetrieveOrdersTask(reverseOrder);
		task.execute(timestamp);
	}
	
	
	private void parseOrders(List<Annotation> list, boolean reverseOrder) throws JSONException {
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
			
			// check timestamp
			if (lastRefreshTimestamp == null) {
				lastRefreshTimestamp = annotation.getTimestamp();
			}
			else {
				if (lastRefreshTimestamp.before(annotation.getTimestamp())) {
					lastRefreshTimestamp = annotation.getTimestamp();
				}
			}
			
			// Register order
			String locationName = annotation.getLocation().getName();
			Map<String, String> orderMap = new HashMap<String, String>();
			orderMap.put(RESOURCE_URI, annotation.getUri());
			orderMap.put(LOCATION_NAME, locationName);
			orderMap.put(ORDER_DETAILS, entry);
			addOrder(locationName, orderMap, reverseOrder);
		}
	}
	
	
	private void loadAllOrders() {
		mOrderLocations.clear();
		mOrders.clear();
		mIndex.clear();
		
		getOrders(null, false);
	}
	
	
	private void addOrder(String locationName, Map<String, String> order, boolean reverseOrder) {
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
			
			if (!reverseOrder) {
				orderList.add(order);
			}
			else {
				orderList.add(0, order);
			}
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
					
				//loadAllOrders();
				getOrders(lastRefreshTimestamp, false);
				return true;
			}
			
			return false;
		}
		
	}
	
	
	private class RetrieveOrdersTask extends AsyncTask<Calendar, Void, List<Annotation>> {
		private boolean mReverseOrder = false;
		
		RetrieveOrdersTask(boolean reverseOrder) {
			mReverseOrder = reverseOrder;
		}
		
		@Override
		protected void onPreExecute() {
			mOrderRetrievalDialog = ProgressDialog.show(OrderManagerFragment.this.getActivity(), 
					"", "Retrieving All Orders ...", true);
		}
		
		@Override
		protected List<Annotation> doInBackground(Calendar...cals) {
			Calendar cal = null;
			if (cals.length != 0) {
				cal = cals[0];
			}
			
			try {
				List<Annotation> orders = Annotation.getAllAnnotationsForEnvironment(getActivity(), 
						mLocation, 
						Feature.ORDER,
						cal
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
				// TODO order by smth: ordered automatically by timestamp descending
				try {
					parseOrders(orders, mReverseOrder);
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
