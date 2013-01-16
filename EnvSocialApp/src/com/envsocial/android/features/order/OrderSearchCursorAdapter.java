package com.envsocial.android.features.order;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.ResourceCursorAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.envsocial.android.R;
import com.envsocial.android.features.IFeatureAdapter;

public class OrderSearchCursorAdapter extends ResourceCursorAdapter 
					implements IOrderCatalogAdapter, IFeatureAdapter {
	private static final String TAG = "OrderSearchCursorAdapter";
	
	private OrderFeature mOrderFeature;
	private SparseArray<ViewHolder> mViewHolderMap;
	private Context mContext;
	
	/**
	 * Holds the mappings between position of quantity > 0 elements in list (cursor) and the data in the cursor.
	 * It is to be used when sending an order from the OrderSearch view.
	 */
	private SparseArray<Map<String, Object>> searchOrderSelection;
	
	public OrderSearchCursorAdapter(OrderFeature orderFeature, Context context, 
			int layout, Cursor c, int flags) {
		super(context, layout, c, flags);
		
		mContext = context;
		mOrderFeature = orderFeature;
		searchOrderSelection = new SparseArray<Map<String, Object>>();
		mViewHolderMap = new SparseArray<OrderSearchCursorAdapter.ViewHolder>();
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View convertView = inflater.inflate(R.layout.catalog_search_result_item, null);
		
		ViewHolder holder = new ViewHolder();
		holder.categoryView = (TextView)convertView.findViewById(R.id.catalog_search_result_category);
		holder.itemView = (TextView)convertView.findViewById(R.id.catalog_search_result_item);
		holder.itemView.setOnClickListener(new OrderSearchItemNameListener(holder));
		
		holder.priceView = (TextView)convertView.findViewById(R.id.catalog_search_result_price);
		holder.quantityView = (TextView)convertView.findViewById(R.id.catalog_search_result_quantity);
		
		holder.btnLess = (ImageButton)convertView.findViewById(R.id.catalog_search_result_btn_less);
		holder.btnMore = (ImageButton)convertView.findViewById(R.id.catalog_search_result_btn_more);
		
		OnClickListener l = new OrderSearchQuantityListener(holder);
		holder.btnLess.setOnClickListener(l);
		holder.btnMore.setOnClickListener(l);
		
		int itemIdColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_ORDER_FTS_ID);
		int itemId = cursor.getInt(itemIdColumnIdx);
		
		mViewHolderMap.put(itemId, holder);
		convertView.setTag(holder);
		
		return convertView;
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// we know there is one because we put it there in the newView call
		ViewHolder holder = (ViewHolder) view.getTag();
		
		// now bind data from the cursor to the holder - first get column indexes by name
		int categoryColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_ORDER_FTS_CATEGORY);
		int itemIdColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_ORDER_FTS_ID);
		int itemColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_ORDER_FTS_ITEM);
		int priceColumnIdx = cursor.getColumnIndex(OrderDbHelper.COL_ORDER_FTS_PRICE);
		
		// then get actual values from cursor
		int itemId = Integer.parseInt(cursor.getString(itemIdColumnIdx));
		String category = cursor.getString(categoryColumnIdx);
		String item = cursor.getString(itemColumnIdx);
		double price = Double.parseDouble(cursor.getString(priceColumnIdx));
		
		//Log.i(TAG, "==== Result: " + itemId + ", " + category + ", " + item + ", " + price + " RON.");
		
		holder.cursorPosition = cursor.getPosition();
		
		// first save data in the holder
		holder.itemId = itemId;
		holder.itemName = item;
		holder.itemPrice = price;
		holder.categoryName = category;
		
		// then bind data to the views
		holder.categoryView.setText(category);
		holder.itemView.setText(item);
		holder.priceView.setText(new DecimalFormat("#.##").format(price) + " RON");
		
		int quantityCt = 0;
		if (searchOrderSelection.get(itemId) != null ) {
			quantityCt = (Integer)searchOrderSelection.get(itemId).get("quantity");
		}
		
		holder.quantityView.setText("" + quantityCt);
	}
	
	
	static class ViewHolder {
		int cursorPosition;
		
		int itemId;
		String itemName;
		double itemPrice;
		String categoryName;
		
		TextView categoryView;
		TextView itemView;
		TextView priceView;
		
		TextView quantityView;
		ImageButton btnLess;
		ImageButton btnMore;
	}
	
	private class OrderSearchQuantityListener implements OnClickListener {
		private ViewHolder mHolder;
		
		OrderSearchQuantityListener(ViewHolder holder) {
			mHolder = holder;
		}
		
		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.catalog_search_result_btn_more) {
				int itemId = mHolder.itemId;
				
				// add to searchOrderSelection mapping
				Map<String, Object> selectionMapping = searchOrderSelection.get(itemId); 
				
				if (selectionMapping != null) {
					int quantityCt = (Integer)selectionMapping.get("quantity");
					quantityCt += 1;
					selectionMapping.put("quantity", quantityCt);
					mHolder.quantityView.setText("" + quantityCt);
				}
				else {
					int quantityCt = 1;
					
					selectionMapping = new HashMap<String, Object>();
					selectionMapping.put(OrderFeature.CATEGORY, mHolder.categoryName);
					selectionMapping.put(OrderFeature.ITEM_ID, mHolder.itemId);
					selectionMapping.put(OrderFeature.ITEM, mHolder.itemName);
					selectionMapping.put(OrderFeature.ITEM_PRICE, mHolder.itemPrice);
					selectionMapping.put("quantity", quantityCt);
					
					searchOrderSelection.put(itemId, selectionMapping);
					mHolder.quantityView.setText("" + quantityCt);
				}
				
				
			}
			else if (v.getId() == R.id.catalog_search_result_btn_less) {
				int itemId = mHolder.itemId;
				
				// subtract from searchOrderSelection mapping
				Map<String, Object> selectionMapping = searchOrderSelection.get(itemId); 
				
				if (selectionMapping != null) {
					int quantityCt = (Integer)selectionMapping.get("quantity");
					quantityCt -= 1;
					
					if (quantityCt > 0) {
						selectionMapping.put("quantity", quantityCt);
					}
					else {
						quantityCt = 0;
						searchOrderSelection.remove(itemId);
					}
					
					mHolder.quantityView.setText("" + quantityCt);
				}
			}
		}
		
	}
	
	
	private class OrderSearchItemNameListener implements OnClickListener {
		private ViewHolder mHolder;
		
		OrderSearchItemNameListener(ViewHolder holder) {
			mHolder = holder;
		}

		@Override
		public void onClick(View v) {
			int itemId = mHolder.itemId;
			Cursor catalogDetailCursor = null;
			
			try {
				catalogDetailCursor = mOrderFeature.getOrderItemDetailCursor(itemId);
				
				if (catalogDetailCursor != null) {
					// there should be only one entry so advance the cursor to it
					catalogDetailCursor.moveToFirst();
					
					int itemNameColumnId = catalogDetailCursor.getColumnIndex(OrderDbHelper.COL_ITEM_NAME);
					int itemDescriptionColumnId = catalogDetailCursor.getColumnIndex(OrderDbHelper.COL_ITEM_DESCRIPTION);
					int itemUsageRankColumnId = catalogDetailCursor.getColumnIndex(OrderDbHelper.COL_ITEM_USAGE_RANK);
					
					String itemName = catalogDetailCursor.getString(itemNameColumnId);
					String itemDescription = catalogDetailCursor.getString(itemDescriptionColumnId);
					float itemUsageRating = catalogDetailCursor.getInt(itemUsageRankColumnId) / (float)2.0;
					
					OrderCatalogItemDescriptionFragment newFragment = 
							OrderCatalogItemDescriptionFragment.newInstance(itemName, itemDescription, itemUsageRating);
					
					FragmentManager fm = ((FragmentActivity)mContext).getSupportFragmentManager(); 
					newFragment.show(fm, "description");
				}
			} 
			catch (Exception ex) {
				Log.d(TAG, "Error getting order item details for itemId: " + itemId, ex);
			} 
			finally {
				if (catalogDetailCursor != null) {
					catalogDetailCursor.close();
				}
			}
		}
	}
	
	
	@Override
	public List<Map<String, Object>> getOrderSelections() {
		List<Map<String, Object>> searchOrderList = new ArrayList<Map<String,Object>>();
		
		for (int i = 0; i < searchOrderSelection.size(); i++) {
			searchOrderList.add(searchOrderSelection.valueAt(i));
		}
		
		return searchOrderList;
	}

	@Override
	public void clearOrderSelections() {
		// clear quantity views
		int quantityCt = 0;
		for (int i = 0; i < searchOrderSelection.size(); i++) {
			int itemId = searchOrderSelection.keyAt(i);
			mViewHolderMap.get(itemId).quantityView.setText("" + quantityCt);
		}
		
		// clear selections
		searchOrderSelection.clear();
	}

	@Override
	public void doCleanup() {
		// we don't need to do anything here as the ResourceCursorAdapter will take
		// care of the cursor lifecycle
		//changeCursor(null);
	}

}
