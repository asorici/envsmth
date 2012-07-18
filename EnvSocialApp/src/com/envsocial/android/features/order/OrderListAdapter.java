package com.envsocial.android.features.order;

import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.envsocial.android.R;
import com.envsocial.android.api.Annotation;


public class OrderListAdapter extends SimpleExpandableListAdapter {

	private Context mContext;
	
	private List<List<Map<String,String>>> mChildData;
	private String[] mChildFrom;
	private int[] mChildTo;
	
	
	public OrderListAdapter(Context context,
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
			holder.orderDetails = (TextView) convertView.findViewById(R.id.order_details);
			holder.btnResolve = (Button) convertView.findViewById(R.id.btn_resolve);
			holder.btnResolve.setOnClickListener(new OrderClickListener(holder));
			
			convertView.setTag(holder);
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
		
		int len = from.length;
		for (int i = 0; i < len; ++ i) {
			if (to[i] == R.id.order_details) {
				String orderDetails = childData.get(from[i]);
				holder.orderDetails.setText(Html.fromHtml(orderDetails));
			}
		}
	}
	
	
	private static class ChildViewHolder {
		int groupPosition;
		int childPosition;
		TextView orderDetails;
		Button btnResolve;
		String uri;
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
}
