package com.envsocial.android.features.order;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
	
	private Map<Integer, Map<Integer, Map<String, Object>>> mOrderSelections;
	private Map<Integer, Map<Integer, ChildViewHolder>> mChildViewHolderMap;
	
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
		
		mOrderSelections = new HashMap<Integer, Map<Integer,Map<String,Object>>>();
		mChildViewHolderMap = new HashMap<Integer, Map<Integer,ChildViewHolder>>();
		
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
		
		Map<String, String> groupData = (Map<String, String>) getGroup(groupPosition);
		Map<String, String> childData = (Map<String, String>) getChild(groupPosition, childPosition);
		
		// Check if we have an entry for group
		Map<Integer, Map<String, Object>> categoryGroup = mOrderSelections.get(groupPosition);
		
		if (categoryGroup == null && qDelta > 0) {
			// if there is no category group and we really have smth to add 
			// create new group data
			categoryGroup = new HashMap<Integer, Map<String, Object>>();
			
			// create new item data
			Map<String, Object> itemMapping = new HashMap<String, Object>();

			itemMapping.put(OrderFeature.CATEGORY, groupData.get(OrderFeature.CATEGORY_NAME));
			itemMapping.put(OrderFeature.ITEM, childData.get(OrderFeature.ITEM_NAME));
			itemMapping.put(OrderFeature.ITEM_ID, Integer.parseInt(childData.get(OrderFeature.ITEM_ID)));
			itemMapping.put(OrderFeature.ITEM_PRICE, Double.parseDouble(childData.get(OrderFeature.ITEM_PRICE)));
			itemMapping.put("quantity", qDelta);

			categoryGroup.put(childPosition, itemMapping);
			mOrderSelections.put(groupPosition, categoryGroup);
			
			notifyDataSetChanged();
		}
		else if (categoryGroup != null) {
			Map<String, Object> itemMapping = categoryGroup.get(childPosition);
			if (itemMapping != null) {
				int quantity = (Integer)itemMapping.get("quantity");
				quantity += qDelta;
				
				if (quantity > 0) {
					itemMapping.put("quantity", quantity);
				}
				else {
					categoryGroup.remove(childPosition);
					
					// delete group too if it is empty
					if (groupData.isEmpty()) {
						mOrderSelections.remove(groupPosition);
					}
				}
				
				notifyDataSetChanged();
			}
			else if (qDelta > 0) {
				// create new item data
				itemMapping = new HashMap<String, Object>();

				itemMapping.put(OrderFeature.CATEGORY, groupData.get(OrderFeature.CATEGORY_NAME));
				itemMapping.put(OrderFeature.ITEM, childData.get(OrderFeature.ITEM_NAME));
				itemMapping.put(OrderFeature.ITEM_ID, Integer.parseInt(childData.get(OrderFeature.ITEM_ID)));
				itemMapping.put(OrderFeature.ITEM_PRICE, Double.parseDouble(childData.get(OrderFeature.ITEM_PRICE)));
				itemMapping.put("quantity", qDelta);

				categoryGroup.put(childPosition, itemMapping);
				
				notifyDataSetChanged();
			}
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
			int childPosition) {
		
		
		Map<String, String> childData = (Map<String, String>) getChild(groupPosition, childPosition);
		
		// Bind holder positions
		holder.groupPosition = groupPosition;
		holder.childPosition = childPosition;
		
		// Bind data
		String itemName = childData.get(OrderFeature.ITEM_NAME);
		Double itemPrice = Double.parseDouble(childData.get(OrderFeature.ITEM_PRICE));
		
		int quantityCt = 0;
		if (mOrderSelections.get(groupPosition) != null 
				&& mOrderSelections.get(groupPosition).get(childPosition) != null) {
			quantityCt = (Integer)mOrderSelections.get(groupPosition).get(childPosition).get("quantity");
		}
		
		//Log.d("OrderCatalogListAdapter", "binding child view data: " + 
		//			groupPosition + "::" + childPosition + "::" + itemName + "::" + quantityCt);
		
		// bind child view data
		holder.itemView.setText(itemName);
		holder.priceView.setText(new DecimalFormat("#.##").format(itemPrice) + " RON");
		holder.quantityView.setText("" + quantityCt);
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
			
			holder.btnLess = (ImageButton) convertView.findViewById(R.id.btn_less);
			holder.btnLess.setOnClickListener(new QuantityClickListener(this, holder));
			
			holder.btnMore = (ImageButton) convertView.findViewById(R.id.btn_more);
			holder.btnMore.setOnClickListener(new QuantityClickListener(this, holder));
			
			// store the child view holder in the map aswell
			Map<Integer, ChildViewHolder> storedViewHolder = mChildViewHolderMap.get(groupPosition);
			if (storedViewHolder != null) {
				storedViewHolder.put(childPosition, holder);
			}
			else {
				storedViewHolder = new HashMap<Integer, ChildViewHolder>();
				storedViewHolder.put(childPosition, holder);
				mChildViewHolderMap.put(groupPosition, storedViewHolder);
			}
			
			convertView.setTag(holder);
		} else {
			holder = (ChildViewHolder) convertView.getTag();
		}
		
		bindChildData(holder, groupPosition, childPosition);
		
		
		// set zebra style item list
		//int colorPos = childPosition % 2;
		//convertView.setBackgroundResource(alternatingColors[colorPos]);
		
		return convertView;
	}
	
	private static class ChildViewHolder {
		int groupPosition;
		int childPosition;
		
		TextView itemView;
		TextView priceView;
		TextView quantityView;
		
		ImageButton btnLess;
		ImageButton btnMore;
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
		
		for (Integer groupIdx : mOrderSelections.keySet()) {
			orderList.addAll(mOrderSelections.get(groupIdx).values());
		}
		
		return orderList;
	}

	@Override
	public void clearOrderSelections() {
		// clear views first
		/*
		for (Integer groupIdx : mOrderSelections.keySet()) {
			Set<Integer> childPositions = mOrderSelections.get(groupIdx).keySet();
			
			for (Integer childIdx : childPositions) {
				ChildViewHolder holder = mChildViewHolderMap.get(groupIdx).get(childIdx);
				holder.quantityCt = 0;
				holder.quantityView.setText("" + holder.quantityCt);
			}
		}
		*/
		
		// then clear all selections as well
		mOrderSelections.clear();
		notifyDataSetChanged();
	}
	
	
}
