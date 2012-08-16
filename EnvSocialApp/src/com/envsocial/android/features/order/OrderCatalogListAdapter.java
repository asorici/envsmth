package com.envsocial.android.features.order;

import java.text.DecimalFormat;
import java.util.ArrayList;
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

public class OrderCatalogListAdapter extends SimpleExpandableListAdapter implements IOrderCatalogAdapter {

	private List<? extends Map<String, String>> mGroupData;
	private String[] mGroupFrom;
	private int[] mGroupTo;
	
	private List<? extends List<? extends Map<String,String>>> mChildData;
	private String[] mChildFrom;
	private int[] mChildTo;
	
	private Map<Integer,Map<Integer,Map<String, Object>>> mQuantityData;
	
	private int[] alternatingColors;
	
	
	public OrderCatalogListAdapter(Context context,
			List<? extends Map<String, String>> groupData, int groupLayout,
			String[] groupFrom, int[] groupTo,
			List<? extends List<? extends Map<String, String>>> childData,
			int childLayout, String[] childFrom, int[] childTo
			) {
		super(context, groupData, groupLayout, groupFrom, groupTo, null,
				childLayout, null, null);
		
		mGroupData = groupData;
		mGroupFrom = groupFrom;
		mGroupTo = groupTo;
		
		mQuantityData = new HashMap<Integer, Map<Integer,Map<String,Object>>>();
		mChildData = childData;
		mChildFrom = childFrom;
		mChildTo = childTo;
		
		// set the alternating colors of the list view
		alternatingColors = new int[2];
		alternatingColors[0] = R.color.white;
		alternatingColors[1] = R.color.light_green;
	}
	
	@Override 
	public Map<String, String> getGroup(int groupPosition) {
		return mGroupData.get(groupPosition);
	}
	
	@Override 
	public int getGroupCount() {
		return mGroupData.size();
	}
	
	@Override
	public Map<String, String> getChild(int groupPosition, int childPosition) {
		return mChildData.get(groupPosition).get(childPosition);
	}
	
	@Override
	public int getChildrenCount(int groupPosition) {
		return mChildData.get(groupPosition).size();
	}
	
	public void updateChildQuantity(ChildViewHolder holder, int qDelta) {
		int groupPosition = holder.groupPosition;
		int childPosition = holder.childPosition;
		
		// update holder quantity data
		holder.quantityCt += qDelta;
		if (holder.quantityCt > 0) {
			boolean newGroup = false;
			
			// Check if we have an entry for group
			Map<Integer, Map<String, Object>> groupData = mQuantityData.get(groupPosition);
			if (groupData == null) {
				// Create new group data
				groupData = new HashMap<Integer, Map<String, Object>>();
				newGroup = true;
			}
			
			// Check if we have an entry for child and compute quantity
			Map<String, Object> itemMapping = groupData.get(childPosition);
			if (itemMapping != null) {
				itemMapping.put("quantity", holder.quantityCt);
			}
			else {
				// set the data for this mapping
				itemMapping = new HashMap<String, Object>();

				itemMapping.put(OrderFeature.CATEGORY, holder.categoryName);
				itemMapping.put(OrderFeature.ITEM, holder.itemName);
				itemMapping.put(OrderFeature.ITEM_ID, holder.itemId);
				itemMapping.put(OrderFeature.ITEM_PRICE, holder.itemPrice);
				itemMapping.put("quantity", holder.quantityCt);

				groupData.put(childPosition, itemMapping);
			}
			
			if (newGroup) {
				mQuantityData.put(groupPosition, groupData);
			}
			
			// finally update the text view too
			holder.quantityView.setText("" + holder.quantityCt);
		}
		else {
			// delete the item selection if it exists
			Map<Integer, Map<String, Object>> groupData = mQuantityData.get(groupPosition);
			if (groupData != null) {
				groupData.remove(childPosition);
				
				// delete group too if it is empty
				if (groupData.isEmpty()) {
					mQuantityData.remove(groupPosition);
				}
			}
			
			// finally reset counter and update text view
			holder.quantityCt = 0;
			holder.quantityView.setText("" + holder.quantityCt);
		}
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
		
		Map<String, String> groupData = (Map<String, String>) getGroup(groupPosition);
		Map<String, String> childData = (Map<String, String>) getChild(groupPosition, childPosition);
		
		// Bind holder positions
		holder.groupPosition = groupPosition;
		holder.childPosition = childPosition;
		
		// Bind data
		holder.itemId = Integer.parseInt(childData.get(OrderFeature.ITEM_ID));
		holder.categoryName = groupData.get(OrderFeature.CATEGORY_NAME);
		holder.itemName = childData.get(OrderFeature.ITEM_NAME);
		holder.itemPrice = Double.parseDouble(childData.get(OrderFeature.ITEM_PRICE));
		
		
		holder.itemView.setText(holder.itemName);
		holder.priceView.setText(new DecimalFormat("#.##").format(holder.itemPrice) + " RON");
		holder.quantityView.setText("" + holder.quantityCt);
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
		
			holder.itemView = (TextView) convertView.findViewById(R.id.orderItem);
			holder.quantityView = (TextView) convertView.findViewById(R.id.quantity);
			holder.priceView = (TextView) convertView.findViewById(R.id.orderItemPrice);
			
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
		int itemId;
		String itemName;
		double itemPrice;
		String categoryName;
		int quantityCt = 0;
		
		int groupPosition;
		int childPosition;
		
		TextView itemView;
		TextView priceView;
		TextView quantityView;
		
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
			mAdapter.updateChildQuantity(mParentView, qDelta);
		}
	}

	@Override
	public List<Map<String, Object>> getOrderSelections() {
		List<Map<String, Object>> orderList = 
				new ArrayList<Map<String,Object>>();
		
		for (Integer groupIdx : mQuantityData.keySet()) {
			orderList.addAll(mQuantityData.get(groupIdx).values());
		}
		
		return orderList;
	}

	@Override
	public void clearOrderSelections() {
		mQuantityData.clear();
	}
}
