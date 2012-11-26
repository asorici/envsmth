package com.envsocial.android.features.order;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpStatus;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.envsocial.android.R;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.api.Url;
import com.envsocial.android.features.IFeatureAdapter;
import com.envsocial.android.utils.Utils;
import com.envsocial.android.utils.SortedList;

public class OrderManagerListAdapter extends BaseExpandableListAdapter implements IFeatureAdapter {
	private static final String TAG = "OrderManagerListAdapter";
	
	private Context mContext;
	
	private Map<String, List<Map<String, String>>> mOrderRequestMap;
	private List<Map<String, String>> mOrderLocations;
	private List<Map<String, String>> mFilteredOrderLocations;
	
	private Map<String, Integer> mOrderRequestTypeImageMap;
	private Map<String, List<Map<String, String>>> mFilteredOrderRequestMap;
	private String mRequestFilter;
	
	//private Map<String, ChildViewHolder> mChildViewHolderMap;
	private Map<String, String> mTimestampGapMapper;
	private Timer mTimestampGapTimer;
	
	public OrderManagerListAdapter(Context context) {
		mContext = context;
		mOrderRequestMap = new HashMap<String, List<Map<String,String>>>();
		mOrderLocations = new SortedList<Map<String,String>>(new LocationDataComparator());
		
		//mChildViewHolderMap = new HashMap<String, OrderManagerListAdapter.ChildViewHolder>();
		mTimestampGapMapper = new HashMap<String,String>();
		mOrderRequestTypeImageMap = new HashMap<String, Integer>();
		mOrderRequestTypeImageMap.put(OrderFeature.NEW_ORDER_NOTIFICATION, R.drawable.order_request_new_order);
		mOrderRequestTypeImageMap.put(OrderFeature.CALL_CHECK_NOTIFICATION, R.drawable.order_request_call_check);
		mOrderRequestTypeImageMap.put(OrderFeature.CALL_WAITER_NOTIFICATION, R.drawable.order_request_call_waiter);
		
		// default filter is "all"
		filterOrderRequestMap("all");
		
		monitorTimegaps();
	}
	
	protected void filterOrderRequestMap(String requestFilter) {
		mRequestFilter = requestFilter;
		mFilteredOrderRequestMap = new HashMap<String, List<Map<String,String>>>();
		mFilteredOrderLocations = new SortedList<Map<String,String>>(new LocationDataComparator());
		
		if (!mRequestFilter.equals("all")) {
			int numLocations = mOrderLocations.size();
			for (int i = 0; i < numLocations; i++) {
				Map<String, String> locationData = mOrderLocations.get(i);
				String locationName = locationData.get("location_name");
				
				List<Map<String, String>> locationOrderRequests = mOrderRequestMap.get(locationName);
				List<Map<String, String>> filteredOrderRequests = new ArrayList<Map<String,String>>();
				
				int numRequests = locationOrderRequests.size();
				for (int r = 0; r < numRequests; r++) {
					Map<String, String> orderRequestData = locationOrderRequests.get(r);
					String orderRequestType = orderRequestData.get(OrderManagerFragment.ORDER_REQUEST_TYPE); 
					if (orderRequestType.equalsIgnoreCase(requestFilter)) {
						filteredOrderRequests.add(orderRequestData);
					}
				}
				
				if (!filteredOrderRequests.isEmpty()) {
					mFilteredOrderLocations.add(locationData);
					mFilteredOrderRequestMap.put(locationName, filteredOrderRequests);
				}
			}
		}
		else {
			mFilteredOrderLocations.addAll(mOrderLocations);
			mFilteredOrderRequestMap.putAll(mOrderRequestMap);
		}
	}

	
	private void monitorTimegaps() {
		mTimestampGapTimer = new Timer();
		final Handler handler = new Handler();
		
		TimerTask updateTimegapTask = new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						if (mTimestampGapMapper != null) {
							synchronized(mTimestampGapMapper) {
								for (String locationName : mOrderRequestMap.keySet()) {
									List<Map<String, String>> locationOrderRequests = 
											mOrderRequestMap.get(locationName);
									
									int nrLocationOrders = locationOrderRequests.size(); 
									for (int j = 0; j < nrLocationOrders; j++) {
										Map<String, String> orderData = locationOrderRequests.get(j);
										String resourceUri = orderData.get(OrderManagerFragment.RESOURCE_URI);
										
										try {
											Calendar orderCal = Utils.stringToCalendar(
													orderData.get(OrderManagerFragment.ORDER_REQUEST_TIMESTAMP), null);
											Calendar now = Calendar.getInstance(
													TimeZone.getTimeZone("Europe/Bucharest"));
											
											String prettyTimeDiff = getPrettyTimeDiffInMinutes(orderCal, now);
											mTimestampGapMapper.put(resourceUri, prettyTimeDiff);
										} catch (ParseException e) {
											mTimestampGapMapper.put(resourceUri, "N.A.");
										}
									}
								}
							}
							
							notifyDataSetChanged();
						}
					}
				});
			}
		};
		
		mTimestampGapTimer.schedule(updateTimegapTask, 10000, 60000);
	}
	
	public void addOrderRequestData(Map<String, String> locationData, Map<String, String> orderRequestData, boolean reverseOrder) {
		String resourceUri = orderRequestData.get(OrderManagerFragment.RESOURCE_URI);
		String orderRequestType = orderRequestData.get(OrderManagerFragment.ORDER_REQUEST_TYPE);
		String locationName = locationData.get("location_name");
		
		// --------------- do unfiltered check first ---------------
		if (mOrderLocations.contains(locationData)) {
			List<Map<String, String>> locationOrderRequests = mOrderRequestMap.get(locationName);
			
			if (reverseOrder) {
				locationOrderRequests.add(0, orderRequestData);
			}
			else {
				locationOrderRequests.add(orderRequestData);
			}
		}
		else {
			mOrderLocations.add(locationData);
			
			List<Map<String, String>> locationOrderRequest = new ArrayList<Map<String, String>>();
			locationOrderRequest.add(orderRequestData);
			mOrderRequestMap.put(locationName, locationOrderRequest);
		}
		
		// --------------- do filtered check next ---------------
		if (mRequestFilter.equals("all")) {
			mFilteredOrderLocations.clear();
			mFilteredOrderLocations.addAll(mOrderLocations);
			
			mFilteredOrderRequestMap.clear();
			mFilteredOrderRequestMap.putAll(mOrderRequestMap);
		}
		else if (mRequestFilter.equals(orderRequestType)) {
			if (mFilteredOrderLocations.contains(locationData)) {
				List<Map<String, String>> filteredlocationOrderRequests = 
						mFilteredOrderRequestMap.get(locationName);
				
				if (reverseOrder) {
					filteredlocationOrderRequests.add(0, orderRequestData);
				}
				else {
					filteredlocationOrderRequests.add(orderRequestData);
				}
			}
			else {
				mFilteredOrderLocations.add(locationData);
				
				List<Map<String, String>> filteredLocationOrderRequests = new ArrayList<Map<String, String>>();
				filteredLocationOrderRequests.add(orderRequestData);
				mFilteredOrderRequestMap.put(locationName, filteredLocationOrderRequests);
			}
		}
		
		
		// --------------- then add to timestamp gap mapper ---------------
		Calendar orderCal;
		try {
			orderCal = Utils.stringToCalendar(orderRequestData.get(OrderManagerFragment.ORDER_REQUEST_TIMESTAMP), null);
			Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Europe/Bucharest"));
			
			String prettyTimeDiff = getPrettyTimeDiffInMinutes(orderCal, now);
			synchronized(mTimestampGapMapper) {
				mTimestampGapMapper.put(resourceUri, prettyTimeDiff);
			}
		} catch (ParseException e) {
			orderCal = null;
			
			synchronized(mTimestampGapMapper) {
				mTimestampGapMapper.put(resourceUri, "N.A.");
			}
		}
	}
	
	
	public void removeOrderRequestData(int groupPosition, int childPosition) {
		Map<String, String> filteredLocationData = mFilteredOrderLocations.get(groupPosition);
		String locationName = filteredLocationData.get("location_name");
		
		// remove from filtered map
		List<Map<String, String>> locationOrderRequests = mFilteredOrderRequestMap.get(locationName); 
		Map<String, String> orderRequestData = locationOrderRequests.remove(childPosition);
		
		// remove from unfiltered map as well - remove by object reference this time
		List<Map<String, String>> unfilteredLocationOrderRequests = mOrderRequestMap.get(locationName); 
		unfilteredLocationOrderRequests.remove(orderRequestData);
		
		// also remove the group if all orders are removed
		if (locationOrderRequests.isEmpty()) {
			mFilteredOrderRequestMap.remove(locationName);
			Map<String, String> locationData = mFilteredOrderLocations.remove(groupPosition);
		
			if (unfilteredLocationOrderRequests.isEmpty()) {
				mOrderRequestMap.remove(locationName);
				mOrderLocations.remove(locationData);
			}
		}
		
		
		// remove from timegap mapper as well
		String orderAnnotationUri = orderRequestData.get(OrderManagerFragment.RESOURCE_URI);
		synchronized(mTimestampGapMapper) {
			mTimestampGapMapper.remove(orderAnnotationUri);
		}
		
		notifyDataSetChanged();
	}

	
	@Override
	public Map<String, String> getGroup(int groupPosition) {
		return mFilteredOrderLocations.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return mFilteredOrderLocations.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		Map<String, String> locationData = mFilteredOrderLocations.get(groupPosition);
		String locationUri = locationData.get("location_uri");
		
		return Long.parseLong(Url.resourceIdFromUri(locationUri));
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = 
					(LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.order_mgr_group, parent, false);
		}
		
		TextView locationNameTextView = (TextView)convertView.findViewById(R.id.order_group);
		Map<String, String> locationData = getGroup(groupPosition);
		locationNameTextView.setText(locationData.get("location_name"));
		
		return convertView;
	}
	
	
	@Override
	public Map<String, String> getChild(int groupPosition, int childPosition) {
		Map<String, String> locationData = mFilteredOrderLocations.get(groupPosition);
		String locationName = locationData.get("location_name");
		
		return mFilteredOrderRequestMap.get(locationName).get(childPosition);
	}
	
	@Override
	public int getChildrenCount(int groupPosition) {
		Map<String, String> locationData = mFilteredOrderLocations.get(groupPosition);
		String locationName = locationData.get("location_name");
		
		return mFilteredOrderRequestMap.get(locationName).size();
	}
	
	
	@Override
	public long getChildId(int groupPosition, int childPosition) {
		Map<String, String> locationData = mFilteredOrderLocations.get(groupPosition);
		String locationName = locationData.get("location_name");
		Map<String, String> orderRequestData = mFilteredOrderRequestMap.get(locationName).get(childPosition);
		
		String resourceUri = orderRequestData.get(OrderManagerFragment.RESOURCE_URI);
		
		return Long.parseLong(Url.resourceIdFromUri(resourceUri));
	}

	
	
	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}
	
	@Override
	public View getChildView(int groupPosition, int childPosition, 
			boolean isLastChild, View convertView, ViewGroup parent) {
		
		ChildViewHolder holder;
		
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.order_mgr_item, parent, false);
			
			holder = new ChildViewHolder();
			holder.orderDetailsView = (TextView) convertView.findViewById(R.id.order_details);
			holder.orderTimegapView = (TextView) convertView.findViewById(R.id.order_timegap);
			holder.orderRequestTypeImageView = 
					(ImageView) convertView.findViewById(R.id.order_request_type_image);
			
			holder.btnResolve = (Button) convertView.findViewById(R.id.btn_resolve);
			holder.btnResolve.setOnClickListener(new OrderClickListener(holder));
			
			convertView.setTag(holder);
		} else {
			holder = (ChildViewHolder) convertView.getTag();
		}
		
		bindChildData(holder, groupPosition, childPosition);
		
		return convertView;
	}
	
	private void bindChildData(ChildViewHolder holder, int groupPosition, int childPosition) {
		
		Map<String,String> childData = getChild(groupPosition, childPosition);
		
		holder.groupPosition = groupPosition;
		holder.childPosition = childPosition;
		holder.uri = childData.get(OrderManagerFragment.RESOURCE_URI);
		
		String orderRequestType = childData.get(OrderManagerFragment.ORDER_REQUEST_TYPE);
		holder.orderRequestTypeImageView.setImageDrawable(
				mContext.getResources().getDrawable(mOrderRequestTypeImageMap.get(orderRequestType)));
		
		String orderDetails = childData.get(OrderManagerFragment.ORDER_REQUEST_DETAILS);
		holder.orderDetailsView.setText(Html.fromHtml(orderDetails));
		
		String orderTimegap = mTimestampGapMapper.get(holder.uri);
		if (orderTimegap != null) {
			holder.orderTimegapView.setText(orderTimegap);
		}
		else {
			holder.orderTimegapView.setText("n.a.");
		}
	}
	
	
	private static class ChildViewHolder {
		int groupPosition;
		int childPosition;
		
		String uri;
		
		TextView orderDetailsView;
		TextView orderTimegapView;
		ImageView orderRequestTypeImageView;
		Button btnResolve;
	}
	
	/**
	 * Returns a pretty formated time difference between two calendar dates in the same TimeZone
	 * in the format ``x min(s) ago'' if x < 60 and ``many hours ago'' or ``many days ago'' otherwise  
	 * @param c1 the before Calendar value
	 * @param c2 the after Calendar value
	 * @return a String of the explained format or the value ``n.a.'' if the calendars are not in the same timezone 
	 */
	private static String getPrettyTimeDiffInMinutes(Calendar c1, Calendar c2) {
		if (c1.getTimeZone() == null || c2.getTimeZone() == null 
				|| c1.getTimeZone().getRawOffset() != c2.getTimeZone().getRawOffset()) {
			return "N.A.";
		}
		else {
			//Log.d(TAG, "c2: " + c2 + ",  c1: " + c1);
			long diff = c2.getTimeInMillis() - c1.getTimeInMillis();
			
			if (diff / Utils.DAY_SCALE != 0) {
				return "many days ago";
			}
			else if (diff / Utils.HOUR_SCALE != 0) {
				return "many hours ago";
			}
			else {
				return (diff / Utils.MINUTE_SCALE) + " min(s) ago";
			}
		}
	}
	
	
	private class OrderClickListener implements OnClickListener {
		
		private ChildViewHolder mHolder;
		
		OrderClickListener(ChildViewHolder holder) {
			mHolder = holder;
		}
		
		public void onClick(View v) {
			if (v == mHolder.btnResolve) {
				new DeleteAnnotationTask(mHolder).execute();
			}
		}
	}
	
	
	private class DeleteAnnotationTask extends AsyncTask<Void, Void, Integer> {

		private ChildViewHolder mHolder;
		
		DeleteAnnotationTask(ChildViewHolder holder) {
			mHolder = holder;
		}
		
		@Override
		protected Integer doInBackground(Void... args) {
			try {
				return Annotation.deleteAnnotation(mContext, mHolder.uri);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Integer statusCode) {
			if (statusCode == HttpStatus.SC_NO_CONTENT) {
				removeOrderRequestData(mHolder.groupPosition, mHolder.childPosition);
			}
		}
		
	}

	@Override
	public void doCleanup() {
		// cancel the timegap refresh timer 
		mTimestampGapTimer.cancel();
		
		// clear the view holder map
		synchronized(mTimestampGapMapper) {
			mTimestampGapMapper.clear();
		}
		
		mTimestampGapMapper = null;
	}
	
	
	private class LocationDataComparator implements Comparator<Map<String, String>> {

		@Override
		public int compare(Map<String, String> data1, Map<String, String> data2) {
			String locationName1 = data1.get("location_name");
			String locationUri1 = data1.get("location_uri");
			
			String locationName2 = data2.get("location_name");
			String locationUri2 = data2.get("location_uri");
			
			if (locationUri1.compareTo(locationUri2) == 0) {
				return 0;
			}
			else return locationName1.compareToIgnoreCase(locationName2);
		}
	}
}
