package com.envsocial.android.features.order;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.envsocial.android.R;

public class OrderSearchCursorAdapter extends ResourceCursorAdapter implements IOrderCatalogAdapter {
	private static final String TAG = "OrderSearchCursorAdapter";
	
	/**
	 * Holds the mappings between position of quantity > 0 elements in list (cursor) and the data in the cursor.
	 * It is to be used when sending an order from the OrderSearch view.
	 */
	private Map<Integer, Map<String, Object>> searchOrderSelection;
	
	public OrderSearchCursorAdapter(Context context, int layout, Cursor c, int flags) {
		super(context, layout, c, flags);
		
		searchOrderSelection = new HashMap<Integer, Map<String, Object>>();
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		
		LayoutInflater inflater = LayoutInflater.from(context);
		View convertView = inflater.inflate(R.layout.catalog_search_result_item, null);
		
		ViewHolder holder = new ViewHolder();
		holder.categoryView = (TextView)convertView.findViewById(R.id.catalog_search_result_category);
		holder.itemView = (TextView)convertView.findViewById(R.id.catalog_search_result_item);
		holder.priceView = (TextView)convertView.findViewById(R.id.catalog_search_result_price);
		holder.quantityView = (TextView)convertView.findViewById(R.id.catalog_search_result_quantity);
		
		holder.btnLess = (ImageButton)convertView.findViewById(R.id.catalog_search_result_btn_less);
		holder.btnMore = (ImageButton)convertView.findViewById(R.id.catalog_search_result_btn_more);
		
		OnClickListener l = new OrderSearchQuantityListener(holder);
		holder.btnLess.setOnClickListener(l);
		holder.btnMore.setOnClickListener(l);
		
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
		holder.quantityView.setText("" + holder.quantityCt);
	}
	
	
	static class ViewHolder {
		int cursorPosition;
		
		int itemId;
		String itemName;
		double itemPrice;
		String categoryName;
		
		int quantityCt = 0;
		
		
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
				mHolder.quantityCt++;
				mHolder.quantityView.setText("" + mHolder.quantityCt);
				
				// add to searchOrderSelection mapping
				int position = mHolder.cursorPosition;
				Map<String, Object> selectionMapping = searchOrderSelection.get(position); 
				
				if (selectionMapping != null) {
					selectionMapping.put("quantity", mHolder.quantityCt);
				}
				else {
					selectionMapping = new HashMap<String, Object>();
					selectionMapping.put(OrderFeature.CATEGORY, mHolder.categoryName);
					selectionMapping.put(OrderFeature.ITEM_ID, mHolder.itemId);
					selectionMapping.put(OrderFeature.ITEM, mHolder.itemName);
					selectionMapping.put(OrderFeature.ITEM_PRICE, mHolder.itemPrice);
					selectionMapping.put("quantity", mHolder.quantityCt);
					
					searchOrderSelection.put(position, selectionMapping);
				}
				
			}
			else if (v.getId() == R.id.catalog_search_result_btn_less) {
				if (mHolder.quantityCt > 0) {
					mHolder.quantityCt--;
					mHolder.quantityView.setText("" + mHolder.quantityCt);
					
					
					// update searchOrderSelection mapping
					int position = mHolder.cursorPosition;
					if (mHolder.quantityCt == 0) {
						searchOrderSelection.remove(position);
					}
					else {
						Map<String, Object> selectionMapping = searchOrderSelection.get(position);
						
						// selectionMapping must be non-null since it is only created when tapping on the + button
						selectionMapping.put("quantity", mHolder.quantityCt);
					}
				}
			}
		}
		
	}
	
	@Override
	public List<Map<String, Object>> getOrderSelections() {
		List<Map<String, Object>> searchOrderList = 
				new ArrayList<Map<String,Object>>(searchOrderSelection.values());
		
		return searchOrderList;
	}

	@Override
	public void clearOrderSelections() {
		searchOrderSelection.clear();
	}

}
