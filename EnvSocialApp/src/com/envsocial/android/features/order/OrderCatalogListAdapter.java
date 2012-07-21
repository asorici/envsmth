package com.envsocial.android.features.order;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.envsocial.android.R;

public class OrderCatalogListAdapter extends SimpleExpandableListAdapter {

	private List<? extends Map<String, ?>> mGroupData;
	private String[] mGroupFrom;
	private int[] mGroupTo;
	
	private List<? extends List<? extends Map<String,?>>> mChildData;
	private String[] mChildFrom;
	private int[] mChildTo;
	
	private Map<Integer,Map<Integer,Integer>> mQuantityData;
	
	private int[] alternatingColors;
	
	
	public OrderCatalogListAdapter(Context context,
			List<? extends Map<String, ?>> groupData, int groupLayout,
			String[] groupFrom, int[] groupTo,
			List<? extends List<? extends Map<String, ?>>> childData,
			int childLayout, String[] childFrom, int[] childTo,
			Map<Integer,Map<Integer,Integer>> counter) {
		super(context, groupData, groupLayout, groupFrom, groupTo, null,
				childLayout, null, null);
		
		mGroupData = groupData;
		mGroupFrom = groupFrom;
		mGroupTo = groupTo;
		
		mQuantityData = counter;
		mChildData = childData;
		mChildFrom = childFrom;
		mChildTo = childTo;
		
		// set the alternating colors of the list view
		alternatingColors = new int[2];
		alternatingColors[0] = R.color.white;
		alternatingColors[1] = R.color.light_green;
	}
	
	@Override
	public Map<String,?> getChild(int groupPosition, int childPosition) {
		return mChildData.get(groupPosition).get(childPosition);
	}
	
	@Override
	public int getChildrenCount(int groupPosition) {
		return mChildData.get(groupPosition).size();
	}
	
	public Integer updateChildQuantity(int groupPosition, int childPosition, int qDelta) {
		Integer q = 0;
		boolean newGroup = false;

		// Check if we have an entry for group
		Map<Integer,Integer> groupData = mQuantityData.get(groupPosition);
		if (groupData == null) {
			// Create new group data
			groupData = new HashMap<Integer,Integer>();
			newGroup = true;
		} else {
			// Check if we have an entry for child and compute quantity
			q = groupData.get(childPosition);
			if (q == null) {
				q = 0;
			}
		}
		// Update quantity
		q += qDelta;
		// TODO if q < 0 we can actually remove it
		q = (q < 0) ? 0 : q;
		groupData.put(childPosition, q);
		if (newGroup) {
			// We handle new group data
			mQuantityData.put(groupPosition, groupData);
		}
		
		return q;
	}
	
	public Integer getChildQuantity(int groupPosition, int childPosition) {
		Map<Integer,Integer> groupData = mQuantityData.get(groupPosition);
		if (groupData == null) {
			return 0;
		}
		Integer q = groupData.get(childPosition);

		return (q == null) ? 0 : q;
	}
	
	private void bindGroupData(GroupViewHolder holder, int groupPosition, 
			String[] groupFrom,
			int[] groupTo) {
		
		
		int len = groupFrom.length;
		for (int i = 0; i < len; ++ i) {
			if (groupTo[i] == R.id.orderGroup) {
				holder.orderGroup.setText((String)mGroupData.get(groupPosition).get(groupFrom[i]));
			}
		}
		
		//holder.orderGroup.setText((String)mGroupData.get(groupPosition).get(OrderMenu.CATEGORY));
	}
	
	private void bindChildData(ChildViewHolder holder, int groupPosition, 
			int childPosition, String from[], int to[]) {
		
		Map<String,?> childData = (Map<String,?>) getChild(groupPosition, childPosition);
		Integer quantity = getChildQuantity(groupPosition, childPosition);
		
		// Bind holder positions
		holder.groupPosition = groupPosition;
		holder.childPosition = childPosition;
		
		// Bind data
		int len = from.length;
		for (int i = 0; i < len; ++ i) {
			if (to[i] == R.id.orderItem) {
				holder.orderItem.setText((String) childData.get(from[i]));
				holder.quantity.setText(quantity.toString());
			}
		}
	}
	
	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		GroupViewHolder holder;
		
		if (convertView == null) {
			convertView = newGroupView(isExpanded, parent);
			holder = new GroupViewHolder();
			holder.orderGroup = (TextView) convertView.findViewById(R.id.orderGroup);
			
			convertView.setTag(holder);
		}
		else {
			holder = (GroupViewHolder) convertView.getTag();
		}
		
		bindGroupData(holder, groupPosition, mGroupFrom, mGroupTo);
		
		
		if (isExpanded) {
			convertView.setBackgroundResource(R.color.dark_green);
		}
		else {
			convertView.setBackgroundResource(R.color.white);
		}
		
		return convertView;
	}
	

	@Override
	public View getChildView(int groupPosition, int childPosition, 
			boolean isLastChild, View convertView, ViewGroup parent) {
		
		ChildViewHolder holder;
		
		if (convertView == null) {
			convertView = newChildView(isLastChild, parent);
			
			holder = new ChildViewHolder();
			holder.groupPosition = groupPosition;
			holder.childPosition = childPosition;
			holder.orderItem = (TextView) convertView.findViewById(R.id.orderItem);
			holder.quantity = (TextView) convertView.findViewById(R.id.quantity);
			holder.btnLess = (Button) convertView.findViewById(R.id.btn_less);
			holder.btnLess.setOnClickListener(new QuantityClickListener(this, holder));
			holder.btnMore = (Button) convertView.findViewById(R.id.btn_more);
			holder.btnMore.setOnClickListener(new QuantityClickListener(this, holder));
			
			convertView.setTag(holder);
		} else {
			holder = (ChildViewHolder) convertView.getTag();
		}
		
		bindChildData(holder, groupPosition, childPosition, mChildFrom, mChildTo);
		
		
		// set zebra style item list
		//int colorPos = childPosition % 2;
		//convertView.setBackgroundResource(alternatingColors[colorPos]);
		
		return convertView;
	}
	
	private static class ChildViewHolder {
		int groupPosition;
		int childPosition;
		TextView orderItem;
		TextView quantity;
		Button btnLess;
		Button btnMore;
	}
	
	private static class GroupViewHolder {
		TextView orderGroup;
	}
	
	static class QuantityClickListener implements OnClickListener {
		
		private ChildViewHolder mParentView;
		private OrderCatalogListAdapter mAdapter;
		
		QuantityClickListener(OrderCatalogListAdapter adapter, ChildViewHolder view) {
			mAdapter = adapter;
			mParentView = view;
		}
		
		public void onClick(View v) {
			int qDelta = 0;
			
			// Update quantity in adapter
			if (v.getId() == R.id.btn_less) {
				qDelta = -1;
			} else if ((v.getId() == R.id.btn_more)) {
				qDelta = 1;
			}
			
			// Update quantity in view holder 
			Integer q = mAdapter.updateChildQuantity(mParentView.groupPosition, 
					mParentView.childPosition, qDelta);
			mParentView.quantity.setText(q.toString());
		}
	}
}
