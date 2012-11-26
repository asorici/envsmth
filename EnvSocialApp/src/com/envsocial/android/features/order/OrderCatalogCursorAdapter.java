package com.envsocial.android.features.order;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ResourceCursorTreeAdapter;
import android.widget.TextView;

import com.envsocial.android.R;
import com.envsocial.android.features.IFeatureAdapter;

public class OrderCatalogCursorAdapter extends ResourceCursorTreeAdapter 
		implements IOrderCatalogAdapter, IFeatureAdapter {
	
	private static final String TAG = "OrderCatalogCursorAdapter";
	private static final String LIST_DATA_KEY = "listData";
	
	private OrderFragment mParentFragment;
	private OrderCatalogPagerAdapter mPagerAdapter;
	private int mPagePosition;
	
	private HashMap<Integer, HashMap<String, Object>> mOrderSelections;
	private SparseArray<View> mOrderSelectionQuantityViews;
	private Context mContext;
	
	public OrderCatalogCursorAdapter(OrderCatalogPagerAdapter pagerAdapter, int pagePosition, 
			Cursor cursor, int groupLayout, int childLayout) {
		super(pagerAdapter.getParentFragment().getActivity(), cursor, groupLayout, childLayout);
		
		mPagerAdapter = pagerAdapter;
		mPagePosition = pagePosition;
		mParentFragment = pagerAdapter.getParentFragment();
		mContext = mParentFragment.getActivity();
		
		mOrderSelections = new HashMap<Integer, HashMap<String,Object>>();
		mOrderSelectionQuantityViews = new SparseArray<View>();
	}	
	
	
	protected Fragment getParentFragment() {
		return mParentFragment;
	}
	
	
	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		Cursor groupCursor = getGroup(groupPosition);
		if (groupCursor == null) {
			throw new IllegalStateException("this should only be called when the cursor is valid");
		}
		
		GroupViewHolder holder;
		
		if (convertView == null) {
			convertView = newGroupView(mContext, groupCursor, isExpanded, parent);
			
			holder = new GroupViewHolder();
			holder.orderCategoryNameView = (TextView) convertView.findViewById(R.id.orderGroup);
			convertView.setTag(holder);
		}
		else {
			holder = (GroupViewHolder)convertView.getTag();
		}
		
		if (isExpanded) {
			convertView.setBackgroundResource(R.color.dark_green);
		}
		else {
			convertView.setBackgroundResource(R.color.white);
		}
		
		bindGroupData(holder, mContext, groupPosition, groupCursor, isExpanded);
		
		return convertView;
	}
	
	@Override
	protected void bindGroupView(View view, Context context, Cursor cursor,
			boolean isExpanded) {
	}
	
	
	protected void bindGroupData(GroupViewHolder holder, Context context, int groupPosition, 
			Cursor cursor, boolean isExpanded) {
		
		int categoryIdColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_CATEGORY_ID);
		int categoryNameColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_CATEGORY_NAME);
		
		int categoryId = cursor.getInt(categoryIdColumnIdx);
		String categoryName = cursor.getString(categoryNameColumnIdx);
		
		holder.categoryId = categoryId;
		holder.groupPosition = groupPosition;
		holder.orderCategoryName = categoryName;
		holder.orderCategoryNameView.setText(categoryName);
	}
	
	
	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, 
			View convertView, ViewGroup parent) {
		
		Cursor childCursor = getChild(groupPosition, childPosition);
		if (childCursor == null) {
			throw new IllegalStateException("this should only be called when the cursor is valid");
		}
		
		ChildViewHolder holder;
		
		if (convertView == null) {
			convertView = newChildView(mContext, childCursor, isLastChild, parent);
			
			holder = new ChildViewHolder();
			
			holder.itemView = (TextView) convertView.findViewById(R.id.orderItem);
			holder.itemView.setOnClickListener(new ItemNameClickListener(this, holder));
			
			holder.quantityView = (TextView) convertView.findViewById(R.id.quantity);
			holder.priceView = (TextView) convertView.findViewById(R.id.orderItemPrice);
			
			holder.btnLess = (ImageButton) convertView.findViewById(R.id.btn_less);
			holder.btnLess.setOnClickListener(new QuantityClickListener(this, holder));
			
			holder.btnMore = (ImageButton) convertView.findViewById(R.id.btn_more);
			holder.btnMore.setOnClickListener(new QuantityClickListener(this, holder));
			
			convertView.setTag(holder);
		}
		else {
			holder = (ChildViewHolder)convertView.getTag();
		}
		
		bindChildData(holder, mContext, groupPosition, childPosition, childCursor, isLastChild);
		
		return convertView;
	}
	
	
	protected void bindChildData(ChildViewHolder holder, Context context, int groupPosition, 
			int childPosition, Cursor cursor, boolean isLastChild) {
		
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
		holder.groupPosition = groupPosition;
		holder.childPosition = childPosition;
		
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
	protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
		
	}
	
	
	@Override
	protected Cursor getChildrenCursor(Cursor groupCursor) {
		int categoryIdColumnIdx = groupCursor.getColumnIndex(OrderDbHelper.COL_CATEGORY_ID);
		int categoryId = groupCursor.getInt(categoryIdColumnIdx);
		
		return mParentFragment.getOrderFeature().getOrderItemCursor(categoryId);
	}

	
	private static class ChildViewHolder {
		public int childPosition;
		public int groupPosition;
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
		public int categoryId;
		public int groupPosition;
		String orderCategoryName;
		TextView orderCategoryNameView;
	}
	
	
	private void updateChildQuantity(ChildViewHolder holder, int qDelta) {
		
		int groupPosition = holder.groupPosition;
		Cursor categoryCursor = getGroup(groupPosition);
		
		int categoryIdColumnIdx = categoryCursor.getColumnIndex(OrderDbHelper.COL_CATEGORY_ID);
		int categoryNameColumnIdx = categoryCursor.getColumnIndex(OrderDbHelper.COL_CATEGORY_NAME);
		int categoryId = categoryCursor.getInt(categoryIdColumnIdx);
		String categoryName = categoryCursor.getString(categoryNameColumnIdx);
		
		int itemId = holder.itemId;
		String itemName = holder.itemName;
		double itemPrice = holder.itemPrice;
		
		// Check if we have an entry for group
		HashMap<String, Object> itemMapping = mOrderSelections.get(itemId);
		if (itemMapping != null) {
			int quantity = (Integer)itemMapping.get("quantity");
			quantity += qDelta;
			
			mOrderSelectionQuantityViews.put(itemId, holder.quantityView);
			
			if (quantity > 0) {
				itemMapping.put("quantity", quantity);
				
				// refresh quantity view
				holder.quantityView.setText("" + quantity);
			}
			else {
				mOrderSelections.remove(itemId);
				mOrderSelectionQuantityViews.remove(itemId);
				
				// set quantity view with 0
				holder.quantityView.setText("0");
			}
			
		}
		else if (qDelta > 0) {
			// create new item data
			itemMapping = new HashMap<String, Object>();

			itemMapping.put(OrderFeature.CATEGORY, categoryName);
			itemMapping.put(OrderFeature.ITEM_CATEGORY_ID, categoryId);
			itemMapping.put(OrderFeature.ITEM, itemName);
			itemMapping.put(OrderFeature.ITEM_ID, itemId);
			itemMapping.put(OrderFeature.ITEM_PRICE, itemPrice);
			itemMapping.put("quantity", qDelta);
			
			// refresh quantity view
			holder.quantityView.setText("" + qDelta);
			
			mOrderSelections.put(itemId, itemMapping);
			mOrderSelectionQuantityViews.put(itemId, holder.quantityView);
		}
		
		// notify pager adapter to save order selection data
		mPagerAdapter.saveListData(mPagePosition, onSaveInstanceState());
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
		//re-initialize orderSelections
		if (mOrderSelections == null) {
			//mOrderSelections = new SparseArray<Map<String,Object>>();
			mOrderSelections = new HashMap<Integer, HashMap<String,Object>>();
		}
		
		if (mOrderSelectionQuantityViews == null) {
			mOrderSelectionQuantityViews = new SparseArray<View>();
		}
		
		// this is where we need to update the feature
		clearOrderSelections();
		
		changeCursor(groupCursor);
		
		notifyDataSetChanged();
	}
	

	@Override
	public List<Map<String, Object>> getOrderSelections() {
		List<Map<String, Object>> orderList = new ArrayList<Map<String,Object>>();
		
		orderList.addAll(mOrderSelections.values());
		return orderList;
	}
	
	
	@Override
	public void clearOrderSelections() {
		if (mOrderSelections.size() > 0) {
			// the clear all order selections
			mOrderSelections.clear();
			mOrderSelectionQuantityViews.clear();
			
			notifyDataSetChanged(false);
		}
	}


	@Override
	public void doCleanup() {
		// cleanup all mappings
		mOrderSelections.clear();
		mOrderSelectionQuantityViews.clear();
		
		mOrderSelections = null;
		mOrderSelectionQuantityViews = null;
		
		// we have to close all cursors here
		// the following will close the group cursor and all child cursors		
		changeCursor(null);
	}
	
	
	protected Bundle onSaveInstanceState() {
		Bundle listDataBundle = new Bundle();
		listDataBundle.putSerializable(LIST_DATA_KEY, mOrderSelections);
		
		return listDataBundle;
	}
	
	
	protected void restoreState(Bundle listDataBundle) {
		if (listDataBundle != null) {
			HashMap<Integer, HashMap<String, Object>> selections = 
					(HashMap<Integer, HashMap<String, Object>>)listDataBundle.getSerializable(LIST_DATA_KEY);
			if (selections != null) {
				mOrderSelections = selections;
			}
		}
	}
}
