package com.envsocial.android.features.order;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.envsocial.android.utils.Utils;

public class OrderManagerFragment extends SherlockFragment {
	
	protected static final String RESOURCE_URI = "resource_uri";
	protected static final String LOCATION_NAME = "location_name";
	protected static final String ORDER_REQUEST_DETAILS = "order_details";
	protected static final String ORDER_REQUEST_TIMESTAMP = "timestamp";
	protected static final String ORDER_REQUEST_TYPE = OrderFeature.REQUEST_TYPE;
	
	private static final String REFRESH_ORDER_REQUESTS_MENU_ITEM = "Refresh";
	private static final String FILTER_ORDER_REQUESTS_MENU_ITEM = "Filter";
	
	private Location mLocation;
	private Calendar mLastRefreshTimestamp;

	private ExpandableListView mList;
	private TextView mOrderRequestFilterValue;
	private OrderManagerListAdapter mAdapter;
	
	private NewOrderReceiver mOrderReceiver;
	private ProgressDialog mOrderRetrievalDialog;
	private Map<String, String> mRequestFilterMap;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setHasOptionsMenu(true);
	    
	    mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
		

		// setup request filter options
		mRequestFilterMap = new HashMap<String, String>();
		mRequestFilterMap.put("All Requests", "all");
		mRequestFilterMap.put("Order Requests", OrderFeature.NEW_ORDER_NOTIFICATION);
		mRequestFilterMap.put("Check Requests", OrderFeature.CALL_CHECK_NOTIFICATION);
		mRequestFilterMap.put("Waiter Requests", OrderFeature.CALL_WAITER_NOTIFICATION);
	    
		// Create custom expandable list adapter
		mAdapter = new OrderManagerListAdapter(getActivity());
		
		getRequests(null, false);
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
	public void onDestroy() {
		super.onDestroy();
		mAdapter.doCleanup();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		return inflater.inflate(R.layout.order_mgr, container, false);
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
        mList.setIndicatorBounds(width - UIUtils.getDipsFromPixel(28 + 10, appContext), width - UIUtils.getDipsFromPixel(10 + 10, appContext));
		mList.setAdapter(mAdapter);
		
		LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mList.setEmptyView(inflater.inflate(R.layout.order_mgr_empty, mList, false));
		
		// set filter value view
		mOrderRequestFilterValue = (TextView) view.findViewById(R.id.order_mgr_request_filter);
		Resources res = getActivity().getResources();
		String filterValue = String.format(res.getString(R.string.order_mgr_filter_value), "All Requests");
		mOrderRequestFilterValue.setText(filterValue);
	}
	
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mList = null;
	}
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
		// add the register/unregister for notifications menu options
     	menu.add(REFRESH_ORDER_REQUESTS_MENU_ITEM);
     	menu.add(FILTER_ORDER_REQUESTS_MENU_ITEM);
		
		super.onCreateOptionsMenu(menu, menuInflater);
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle().toString().compareTo(REFRESH_ORDER_REQUESTS_MENU_ITEM) == 0) {
			
			// get all orders since the last one handled
			getRequests(mLastRefreshTimestamp, false);
			return true;
		}
		else if (item.getTitle().toString().compareTo(FILTER_ORDER_REQUESTS_MENU_ITEM) == 0) {
			
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
			LayoutInflater inflater = 
					(LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.order_mgr_filter_dialog, null);
			final Spinner filterSpinner = (Spinner) layout.findViewById(R.id.order_mgr_filter_spinner);
			
			alertDialogBuilder
				.setTitle("Filter requests by:")
				.setCancelable(true)
				.setView(layout);
			
			alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			
			alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String filterSelection = String.valueOf(filterSpinner.getSelectedItem());
					
					Resources res = getActivity().getResources();
					String filterValue = 
							String.format(res.getString(R.string.order_mgr_filter_value), filterSelection);
					mOrderRequestFilterValue.setText(filterValue);
					
					mAdapter.filterOrderRequestMap(mRequestFilterMap.get(filterSelection));
					mAdapter.notifyDataSetChanged();
				}
			});
			
			// create alert dialog
			AlertDialog alertDialog = alertDialogBuilder.create();

			// show it
			alertDialog.show();
			
			return true;
		}
		
		return false;
	}
	
	
	private void getRequests(Calendar timestamp, boolean reverseOrder) {
		RetrieveOrdersTask task = new RetrieveOrdersTask(reverseOrder);
		task.execute(timestamp);
	}
	
	
	private void parseRequests(List<Annotation> list, boolean reverseOrder) throws JSONException {
		for (Annotation annotation : list) {
			if (annotation.getCategory().compareTo(Feature.ORDER) != 0) {
				continue;
			}
			
			// get location name and uri - construct location data map
			String locationName = annotation.getLocation().getName();
			String locationUri = annotation.getLocation().getLocationUri();
			Map<String, String> locationData = new HashMap<String, String>();
			locationData.put("location_name", locationName);
			locationData.put("location_uri", locationUri);
			
			
			// Get order
			String orderString = annotation.getData();
			JSONObject orderDataObject = new JSONObject(orderString);
			
			String orderRequestType = 
					orderDataObject.optString(OrderFeature.REQUEST_TYPE, OrderFeature.NEW_ORDER_NOTIFICATION);
			String entry = getResources().getString(R.string.lbl_order_request);
			
			if (orderRequestType.compareTo(OrderFeature.NEW_ORDER_NOTIFICATION) == 0) {
				String orderDataString = orderDataObject.getString("order");
				JSONArray orderData = new JSONArray(orderDataString);
				
				int orderLen = orderData.length();
				entry = "";
				for (int j = 0; j < orderLen; ++ j) {
					String category = orderData.getJSONObject(j).getString("category");
					String order = orderData.getJSONObject(j).getString("items");
					entry += "<b>" + category + ":</b><br />";
					entry += order.replace("\n", "<br />");
					entry += "<br />";
				}
			}
			else if (orderRequestType.compareTo(OrderFeature.CALL_CHECK_NOTIFICATION) == 0) {
				entry = "<b>" + "CHECK" + "</b>" + " requested at " + "<b>" + locationName + "</b>";
			}
			else if (orderRequestType.compareTo(OrderFeature.CALL_WAITER_NOTIFICATION) == 0) {
				entry = "<b>" + "WAITER" + "</b>" + " requested at " + "<b>" + locationName + "</b>";
			}
			
			// check timestamp
			if (mLastRefreshTimestamp == null) {
				mLastRefreshTimestamp = annotation.getTimestamp();
			}
			else {
				if (mLastRefreshTimestamp.before(annotation.getTimestamp())) {
					mLastRefreshTimestamp = annotation.getTimestamp();
				}
			}
			
			// Register order request
			Map<String, String> orderRequestMap = new HashMap<String, String>();
			orderRequestMap.put(RESOURCE_URI, annotation.getUri());
			orderRequestMap.put(LOCATION_NAME, locationName);
			orderRequestMap.put(ORDER_REQUEST_TYPE, orderRequestType);
			orderRequestMap.put(ORDER_REQUEST_DETAILS, entry);
			orderRequestMap.put(ORDER_REQUEST_TIMESTAMP, Utils.calendarToString(annotation.getTimestamp(), "yyyy-MM-dd'T'HH:mm:ssZ"));
			addOrder(locationData, orderRequestMap, reverseOrder);
		}
	}
	
	
	private void addOrder(Map<String, String> locationData, Map<String, String> order, boolean reverseOrder) {
		// add to adapter
		mAdapter.addOrderRequestData(locationData, order, reverseOrder);
	}
	
		
	private class NewOrderReceiver extends EnvivedReceiver {
		
		@Override
		public boolean handleNotification(Context context, Intent intent,
				EnvivedNotificationContents notificationContents) {
			
			JSONObject paramsJSON = notificationContents.getParams();
			
			if (notificationContents.getFeature().equals(Feature.ORDER) 
				&& paramsJSON.optString("type", null) != null 
				&& paramsJSON.optString("type").equalsIgnoreCase(OrderFeature.NEW_REQUEST_NOTIFICATION)) {
				
				getRequests(mLastRefreshTimestamp, false);
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
			mOrderRetrievalDialog.setCancelable(true);
			mOrderRetrievalDialog.setCanceledOnTouchOutside(false);
		}
		
		@Override
		protected List<Annotation> doInBackground(Calendar...cals) {
			Calendar cal = null;
			if (cals.length != 0) {
				cal = cals[0];
			}
			
			try {
				List<Annotation> orderRequests = Annotation.getAllAnnotationsForEnvironment(getActivity(), 
						mLocation, 
						Feature.ORDER,
						cal
				);
				
				return orderRequests;
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
					parseRequests(orders, mReverseOrder);
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
