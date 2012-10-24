package com.envsocial.android.features.order;

import java.text.ParseException;
import java.util.Calendar;
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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.envsocial.android.R;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.features.IFeatureAdapter;
import com.envsocial.android.utils.Utils;

public class OrderManagerListAdapter extends SimpleExpandableListAdapter implements IFeatureAdapter {
	private static final String TAG = "OrderManagerListAdapter";
	private Context mContext;
	
	private List<List<Map<String,String>>> mChildData;
	private String[] mChildFrom;
	private int[] mChildTo;
	
	private Map<String, ChildViewHolder> mChildViewHolderMap;
	private Timer mTimestampGapTimer;
	
	public OrderManagerListAdapter(Context context,
			List<? extends Map<String, String>> groupData, int groupLayout,
			String[] groupFrom, int[] groupTo,
			List<List<Map<String, String>>> childData,
			int childLayout, String[] childFrom, int[] childTo) {
		super(context, groupData, groupLayout, groupFrom, groupTo, null,
				childLayout, null, null);
		
		mContext = context;
		mChildData = childData;
		mChildFrom = childFrom;
		mChildTo = childTo;
		
		mChildViewHolderMap = new HashMap<String, OrderManagerListAdapter.ChildViewHolder>();
		
		monitorTimegaps();
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
						if (mChildViewHolderMap != null) {
							synchronized(mChildViewHolderMap) {
								Log.d(TAG, "processing for " + mChildViewHolderMap.size() + " items");
								for (ChildViewHolder holder : mChildViewHolderMap.values()) {
									if (holder.orderTimestamp != null) {
										Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Europe/Bucharest"));
										
										String prettyTimeDiff = getPrettyTimeDiffInMinutes(holder.orderTimestamp, now);
										holder.orderTimegapView.setText(prettyTimeDiff);
									}
								}
							}
						}
					}
				});
			}
		};
		
		
		mTimestampGapTimer.schedule(updateTimegapTask, 10000, 5000);
	}
	

	public void setChildData(List<List<Map<String,String>>> mOrderLocations) {
		mChildData = mOrderLocations;
	}
	
	@Override
	public Map<String,String> getChild(int groupPosition, int childPosition) {
		return mChildData.get(groupPosition).get(childPosition);
	}
	
	@Override
	public int getChildrenCount(int groupPosition) {
		return mChildData.get(groupPosition).size();
	}
	
	public void removeItem(int groupPosition, int childPosition) {
		List<Map<String,String>> orders = mChildData.get(groupPosition);
		String orderAnnotationUri = orders.get(childPosition).get(OrderManagerFragment.RESOURCE_URI);
		
		// first remove from child view holder map
		synchronized(mChildViewHolderMap) {
			mChildViewHolderMap.remove(orderAnnotationUri);
		}
		
		// then remove from list
		orders.remove(childPosition);
		
		// TODO also remove the group if all orders are removed
		notifyDataSetChanged();
	}
	
	@Override
	public View getChildView(int groupPosition, int childPosition, 
			boolean isLastChild, View convertView, ViewGroup parent) {
		
		ChildViewHolder holder;
		
		if (convertView == null) {
			convertView = newChildView(isLastChild, parent);
			
			holder = new ChildViewHolder();
			holder.orderDetailsView = (TextView) convertView.findViewById(R.id.order_details);
			holder.orderTimegapView = (TextView) convertView.findViewById(R.id.order_timegap);
			holder.btnResolve = (Button) convertView.findViewById(R.id.btn_resolve);
			holder.btnResolve.setOnClickListener(new OrderClickListener(holder));
			
			convertView.setTag(holder);
			
			Log.d(TAG, "----------- calling get Child view");
			
		} else {
			holder = (ChildViewHolder) convertView.getTag();
		}
		
		bindChildData(holder, groupPosition, childPosition, mChildFrom, mChildTo);
		
		return convertView;
	}
	
	private void bindChildData(ChildViewHolder holder, 
			int groupPosition, int childPosition, String[] from, int[] to) {
		
		Map<String,String> childData = getChild(groupPosition, childPosition);
		
		holder.groupPosition = groupPosition;
		holder.childPosition = childPosition;
		holder.uri = childData.get(OrderManagerFragment.RESOURCE_URI);
		
		// this should not fail
		try {
			holder.orderTimestamp = Utils.stringToCalendar(childData.get(OrderManagerFragment.ORDER_TIMESTAMP), null);
			//holder.orderTimestamp.setTimeZone(TimeZone.getTimeZone("Europe/Bucharest"));
		} catch (ParseException e) {
			holder.orderTimestamp = null;
		}
		
		String orderDetails = childData.get(OrderManagerFragment.ORDER_DETAILS);
		holder.orderDetailsView.setText(Html.fromHtml(orderDetails));
		
		if (holder.orderTimestamp != null) {
			Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Europe/Bucharest"));
			
			String prettyTimeDiff = getPrettyTimeDiffInMinutes(holder.orderTimestamp, now);
			holder.orderTimegapView.setText(prettyTimeDiff);
		}
		else {
			holder.orderTimegapView.setText("n.a.");
		}
		
		synchronized(mChildViewHolderMap) {
			Log.d(TAG, "----------- calling append to map");
			mChildViewHolderMap.put(childData.get(OrderManagerFragment.RESOURCE_URI), holder);
		}
	}
	
	
	private static class ChildViewHolder {
		int groupPosition;
		int childPosition;
		
		Calendar orderTimestamp;
		String uri;
		
		TextView orderDetailsView;
		TextView orderTimegapView;
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
				removeItem(mHolder.groupPosition, mHolder.childPosition);
			}
		}
		
	}

	@Override
	public void doCleanup() {
		// cancel the timegap refresh timer 
		mTimestampGapTimer.cancel();
		
		// clear the view holder map
		synchronized(mChildViewHolderMap) {
			mChildViewHolderMap.clear();
		}
		
		mChildViewHolderMap = null;
	}
}
