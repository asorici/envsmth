package com.envsocial.android.features.order;

import java.util.LinkedList;
import java.util.Map;

import org.apache.http.HttpStatus;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.envsocial.android.R;
import com.envsocial.android.api.Annotation;

public class OrderManagementListAdapter extends BaseAdapter {

	private Context mContext;
	private LayoutInflater mInflater;
	private LinkedList<Map<String,String>> mData;
	
	public OrderManagementListAdapter(Context context, LinkedList<Map<String,String>> data) {
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mData = data;
	}
	
	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public Map<String,String> getItem(int position) {
		return mData.get(position);
	}
	
	public void addItem(Map<String,String> data) {
		mData.addFirst(data);
		notifyDataSetChanged();
	}
	
	public void removeItem(int position) {
		mData.remove(position);
		notifyDataSetChanged();
	}
	
	public long getItemId(int position) {
		return position;
	}
	
	public void reloadList(LinkedList<Map<String,String>> list) {
		mData = list;
		notifyDataSetChanged();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ViewHolder holder;
		
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.m_order_row, parent, false);
			
			holder = new ViewHolder();
			holder.order = (LinearLayout) convertView.findViewById(R.id.order);
			holder.locationName = (TextView) convertView.findViewById(R.id.location_name);
			holder.orderDetails = (TextView) convertView.findViewById(R.id.order_details);
			holder.resolve = (Button) convertView.findViewById(R.id.btn_resolve);
			holder.resolve.setOnClickListener(new OrderManagementClickListener(holder));
			
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		bind(holder, position);
		
		return convertView;
	}
	
	private void bind(ViewHolder holder, int position) {
		Map<String,String> data = getItem(position);
		String annotationUri = data.get(OrderManagementListFragment.RESOURCE_URI);
		String locationName = data.get(OrderManagementListFragment.LOCATION_NAME);
		String orderDetails = data.get(OrderManagementListFragment.ORDER_DETAILS);
		holder.position = position;
		holder.annotationUri = annotationUri;
		holder.locationName.setText(locationName);
		holder.orderDetails.setText(Html.fromHtml(orderDetails));
	}
	
	
	private class OrderManagementClickListener implements OnClickListener {
		
		private ViewHolder mHolder;
		
		OrderManagementClickListener(ViewHolder holder) {
			mHolder = holder;
		}
		
		@Override
		public void onClick(View v) {
			if (v == mHolder.resolve) {
				new DeleteAnnotationTask(mHolder).execute();
			}
		}
	}
	
	
	static class ViewHolder {
		int position;
		String annotationUri;
		LinearLayout order;
		TextView locationName;
		TextView orderDetails;
		Button resolve;
	}
	
	
	private class DeleteAnnotationTask extends AsyncTask<Void, Void, Integer> {

		private ViewHolder mHolder;
		
		DeleteAnnotationTask(ViewHolder holder) {
			mHolder = holder;
		}
		
		@Override
		protected void onPreExecute() {
			mHolder.order.setBackgroundColor(Color.GRAY);
			mHolder.locationName.setTextColor(Color.BLACK);
			mHolder.locationName.setBackgroundColor(Color.GRAY);
			mHolder.orderDetails.setBackgroundColor(Color.GRAY);
			mHolder.resolve.setEnabled(false);
		}
		
		@Override
		protected Integer doInBackground(Void... args) {
			try {
				return Annotation.deleteAnnotation(mContext, mHolder.annotationUri);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Integer statusCode) {
			if (statusCode == HttpStatus.SC_NO_CONTENT) {
				System.out.println("[DEBUG]>> got status code: " + statusCode);
				Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.scale_out);
				mHolder.order.setAnimation(animation);
				mHolder.order.postDelayed(new Runnable() {
					public void run() {
						removeItem(mHolder.position);
					}
				}, animation.getDuration());
				
				mHolder.order.setBackgroundColor(Color.WHITE);
				mHolder.locationName.setTextColor(Color.WHITE);
				mHolder.locationName.setBackgroundColor(Color.BLACK);
				mHolder.orderDetails.setBackgroundColor(Color.WHITE);
				mHolder.resolve.setEnabled(true);
			}
		}
		
	}

}
