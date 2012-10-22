package com.envsocial.android.features.order;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ResourceCursorTreeAdapter;
import android.widget.TextView;

import com.envsocial.android.R;

public class OrderCatalogCursorAdapter extends ResourceCursorTreeAdapter 
		implements /*OnChildClickListener,*/ IOrderCatalogAdapter {
	
	private static final String TAG = "OrderCatalogCursorAdapter";
	
	private OrderFragment mParentFragment;
	private SparseArray<Map<String, Object>> mOrderSelections;
	private SparseArray<GroupViewHolder> mGroupViewMap;
	private SparseArray<ChildViewHolder> mChildViewMap;
	
	public OrderCatalogCursorAdapter(OrderFragment parentFragment, Context context, Cursor cursor,
			int groupLayout, int childLayout) {
		super(context, cursor, groupLayout, childLayout);
		
		mParentFragment = parentFragment;
		mOrderSelections = new SparseArray<Map<String,Object>>();
		mGroupViewMap = new SparseArray<OrderCatalogCursorAdapter.GroupViewHolder>();
		mChildViewMap = new SparseArray<OrderCatalogCursorAdapter.ChildViewHolder>();
	}	
	
	
	protected Fragment getParentFragment() {
		return mParentFragment;
	}
	
	
	@Override 
	public View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
		View view = super.newGroupView(context, cursor, isExpanded, parent);
		GroupViewHolder holder = new GroupViewHolder();
		
		int categoryIdColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_CATEGORY_ID);
		int categoryId = cursor.getInt(categoryIdColumnIdx);
		
		holder.orderCategoryNameView = (TextView) view.findViewById(R.id.orderGroup);
		
		mGroupViewMap.put(categoryId, holder);
		view.setTag(holder);
		
		//Log.d(TAG, "New Group View for categId: " + categoryId);
		
		return view;
	}
	
	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View v = super.getGroupView(groupPosition, isExpanded, convertView, parent);
		
		if (isExpanded) {
			v.setBackgroundResource(R.color.dark_green);
		}
		else {
			v.setBackgroundResource(R.color.white);
		}
		
		return v;
	}
	
	
	@Override
	protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
		// we know there is one because we put it there in the newGroupView call
		//Object tag = view.getTag();
		//Log.d(TAG, "Group view TAG has class: " + tag.getClass().getName());
		
		GroupViewHolder holder = (GroupViewHolder) view.getTag();
		
		int categoryNameColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_CATEGORY_NAME);
		String categoryName = cursor.getString(categoryNameColumnIdx);
		
		holder.orderCategoryName = categoryName;
		holder.orderCategoryNameView.setText(categoryName);
	}
	
	@Override
	public View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
		View view = super.newChildView(context, cursor, isLastChild, parent);
		ChildViewHolder holder = new ChildViewHolder();
		
		holder.itemView = (TextView) view.findViewById(R.id.orderItem);
		holder.itemView.setOnClickListener(new ItemNameClickListener(this, holder));
		
		holder.quantityView = (TextView) view.findViewById(R.id.quantity);
		holder.priceView = (TextView) view.findViewById(R.id.orderItemPrice);
		
		holder.btnLess = (ImageButton) view.findViewById(R.id.btn_less);
		holder.btnLess.setOnClickListener(new QuantityClickListener(this, holder));
		
		holder.btnMore = (ImageButton) view.findViewById(R.id.btn_more);
		holder.btnMore.setOnClickListener(new QuantityClickListener(this, holder));
		
		int itemIdColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_ITEM_ID);
		int itemId = cursor.getInt(itemIdColumnIdx);
		
		mChildViewMap.put(itemId, holder);
		
		view.setTag(holder);
		
		//Log.d(TAG, "New Child View for child: " + itemId);
		
		return view;
	}
	
	
	@Override
	protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
		// we know there is one because we put it there in the newGroupView call
		
		//Object tag = view.getTag();
		//Log.d(TAG, "Child view TAG has class: " + tag.getClass().getName());
		
		ChildViewHolder holder = (ChildViewHolder) view.getTag();

		int itemIdColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_ITEM_ID);
		int itemCategIdColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_ITEM_CATEGORY_ID);
		int itemNameColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_ITEM_NAME);
		int itemPriceColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_ITEM_PRICE);
		int itemUsageRankColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_ITEM_USAGE_RANK);
		int itemDescriptionColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_ITEM_DESCRIPTION);
		
		int itemId = cursor.getInt(itemIdColumnIdx);
		int itemCategId = cursor.getInt(itemCategIdColumnIdx);
		String itemName = cursor.getString(itemNameColumnIdx);
		double itemPrice = cursor.getDouble(itemPriceColumnIdx);
		String itemDescription = cursor.getString(itemDescriptionColumnIdx);
		int itemUsageRank = cursor.getInt(itemUsageRankColumnIdx);
		
		int quantityCt = 0;
		if (mOrderSelections.get(itemId) != null ) {
			quantityCt = (Integer)mOrderSelections.get(itemId).get("quantity");
		}
		
		// bind child view data
		holder.itemId = itemId;
		holder.itemCategId = itemCategId;
		holder.itemName = itemName;
		holder.itemPrice = itemPrice;
		holder.itemDescription = itemDescription;
		holder.itemUsageRank = itemUsageRank;
		
		holder.itemView.setText(itemName);
		holder.priceView.setText(new DecimalFormat("#.##").format(itemPrice) + " RON");
		holder.quantityView.setText("" + quantityCt);
	}
	
	
	@Override
	protected Cursor getChildrenCursor(Cursor groupCursor) {
		int categoryIdColumnIdx = groupCursor.getColumnIndex(OrderDbHelper.COL_CATEGORY_ID);
		int categoryId = groupCursor.getInt(categoryIdColumnIdx);
		
		return mParentFragment.getOrderFeature().getOrderItemCursor(categoryId);
	}

	
	private static class ChildViewHolder {
		int itemId;
		int itemCategId;
		String itemName;
		double itemPrice;
		
		int itemUsageRank;
		String itemDescription;
		
		TextView itemView;
		TextView priceView;
		TextView quantityView;
		
		ImageButton btnLess;
		ImageButton btnMore;
	}
	
	private static class GroupViewHolder {
		String orderCategoryName;
		TextView orderCategoryNameView;
	}
	
	
	private void updateChildQuantity(ChildViewHolder holder, int qDelta) {
		
		String categoryName = mGroupViewMap.get(holder.itemCategId).orderCategoryName;
		
		int itemId = holder.itemId;
		String itemName = holder.itemName;
		double itemPrice = holder.itemPrice;
		
		// Check if we have an entry for group
		Map<String, Object> itemMapping = mOrderSelections.get(itemId);
		if (itemMapping != null) {
			int quantity = (Integer)itemMapping.get("quantity");
			quantity += qDelta;
			
			if (quantity > 0) {
				itemMapping.put("quantity", quantity);
				
				// refresh quantity view
				holder.quantityView.setText("" + quantity);
			}
			else {
				mOrderSelections.remove(itemId);
				
				// set quantity view with 0
				holder.quantityView.setText("0");
			}
			
		}
		else if (qDelta > 0) {
			// create new item data
			itemMapping = new HashMap<String, Object>();

			itemMapping.put(OrderFeature.CATEGORY, categoryName);
			itemMapping.put(OrderFeature.ITEM, itemName);
			itemMapping.put(OrderFeature.ITEM_ID, itemId);
			itemMapping.put(OrderFeature.ITEM_PRICE, itemPrice);
			itemMapping.put("quantity", qDelta);
			
			// refresh quantity view
			holder.quantityView.setText("" + qDelta);
			
			mOrderSelections.put(itemId, itemMapping);
		}
	}

	
	// =================================== Click Listeners =================================== //
	
	static class QuantityClickListener implements OnClickListener {
		
		private ChildViewHolder mViewHolder;
		private OrderCatalogCursorAdapter mAdapter;
		
		QuantityClickListener(OrderCatalogCursorAdapter adapter, ChildViewHolder view) {
			mAdapter = adapter;
			mViewHolder = view;
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
			mAdapter.updateChildQuantity(mViewHolder, qDelta);
		}
	}
	
	
	private static class ItemNameClickListener implements OnClickListener {
		private ChildViewHolder mViewHolder;
		private OrderCatalogCursorAdapter mAdapter;
		
		ItemNameClickListener(OrderCatalogCursorAdapter adapter, ChildViewHolder view) {
			mAdapter = adapter;
			mViewHolder = view;
		}
		
		@Override
		public void onClick(View v) {
			String itemDescription = mViewHolder.itemDescription;
			float itemUsageRating = mViewHolder.itemUsageRank / (float)2.0;
			
			OrderCatalogItemDescriptionFragment newFragment = 
					OrderCatalogItemDescriptionFragment.newInstance(itemDescription, itemUsageRating);
			
			FragmentManager fm = mAdapter.getParentFragment().getActivity().getSupportFragmentManager(); 
			newFragment.show(fm, "description");
		}
		
	}
	
	
	// =============================== Selection, Update and Cleanup =============================== //
	
	public void updateFeature(Cursor groupCursor) {
		// this is where we need to update the feature
		clearOrderSelections();
		
		changeCursor(groupCursor);
		
		notifyDataSetChanged();
	}
	

	@Override
	public List<Map<String, Object>> getOrderSelections() {
		// linearize to list from SparseArray
		List<Map<String, Object>> orderList = new ArrayList<Map<String,Object>>();
		for (int i = 0; i < mOrderSelections.size(); i++) {
			orderList.add(mOrderSelections.valueAt(i));
		}
		
		return orderList;
	}


	@Override
	public void clearOrderSelections() {
		// first reset all quantity views in selection to 0
		for (int i = 0; i < mOrderSelections.size(); i++) {
			int itemId = mOrderSelections.keyAt(i);
			
			ChildViewHolder holder = mChildViewMap.get(itemId);
			if (holder != null) {
				holder.quantityView.setText("0");
			}
		}
		
		// the clear all order selections
		mOrderSelections.clear();
	}


	@Override
	public void doCleanup() {
		// cleanup all mappings
		mGroupViewMap.clear();
		mChildViewMap.clear();
		mOrderSelections.clear();
		
		mOrderSelections = null;
		mGroupViewMap = null;
		mChildViewMap = null;
		
		// we have to close all cursors here
		// the following will close the group cursor and all child cursors
		
		changeCursor(null);
	}
}
