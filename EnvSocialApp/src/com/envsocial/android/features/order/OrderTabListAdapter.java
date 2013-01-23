package com.envsocial.android.features.order;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.DialogFragment;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.envsocial.android.R;
import com.envsocial.android.api.Location;
import com.envsocial.android.features.order.OrderCustomAlertDialogFragment.OrderNoticeAlertDialogListener;

public class OrderTabListAdapter extends BaseExpandableListAdapter {
	
	// internal structures
	private List<Map<String, Object>> mOrderSelections;
	private Map<String, List<Map<String, Object>>> mOrderCategoryGrouping;
	private SparseArray<String> mOrderCategories;
	
	// parent fragment
	private OrderTabFragment mParentFragment;
	
	
	public OrderTabListAdapter(OrderTabFragment parentFragment, List<Map<String, Object>> orderSelections) {
		mParentFragment = parentFragment;
		mOrderSelections = orderSelections;
		
		mOrderCategories = new SparseArray<String>();
		mOrderCategoryGrouping = getOrderSummary();
	}
	
	
	private Map<String, List<Map<String, Object>>> getOrderSummary() {
		Map<String, List<Map<String, Object>>> summaryCategoryGrouping = 
				new HashMap<String, List<Map<String, Object>>>();
		
		int orderLen = mOrderSelections.size();
		for (int idx = 0; idx < orderLen; idx++ ) {
			// the keys have no importance; we just want access to the elements
			Map<String, Object> selection = mOrderSelections.get(idx);
			String category = (String)selection.get(OrderFeature.CATEGORY);
			int categoryId = (Integer)selection.get(OrderFeature.ITEM_CATEGORY_ID);
			
			if (summaryCategoryGrouping.containsKey(category)) {
				summaryCategoryGrouping.get(category).add(selection);
			}
			else {
				// append to list of menu categories and start new list in
				// category grouping summary
				mOrderCategories.put(categoryId, category);
				List<Map<String, Object>> categorySelectedItemList = new ArrayList<Map<String,Object>>();
				categorySelectedItemList.add(selection);
				
				summaryCategoryGrouping.put(category, categorySelectedItemList);
			}
		}
		
		return summaryCategoryGrouping;
	}
	
	
	@Override
	public Map<String, Object> getChild(int groupPosition, int childPosition) {
		String category = mOrderCategories.valueAt(groupPosition);
		return mOrderCategoryGrouping.get(category).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		String category = mOrderCategories.valueAt(groupPosition);
		return (Integer)mOrderCategoryGrouping.get(category).get(childPosition)
						.get(OrderFeature.ITEM_ID);
		
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mParentFragment.getActivity()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.order_tab_item, null);
		}
		
		// solve item content part
		Map<String, Object> itemMapping = getChild(groupPosition, childPosition);
		TextView itemNameView = (TextView) convertView.findViewById(R.id.order_tab_item_name);
		TextView itemQuantityView = (TextView) convertView.findViewById(R.id.order_tab_item_quantity);
		TextView itemPriceView = (TextView) convertView.findViewById(R.id.order_tab_item_price);
		
		String itemName = (String) itemMapping.get(OrderFeature.ITEM);
		int itemQuantity = (Integer) itemMapping.get("quantity");
		double itemPrice = (Double) itemMapping.get(OrderFeature.ITEM_PRICE);
		double itemCumulatedPrice = itemPrice * itemQuantity;
		
		itemNameView.setText(itemName);
		itemQuantityView.setText("" + itemQuantity);
		itemPriceView.setText(new DecimalFormat("#.##").format(itemCumulatedPrice) + " RON");
		
		// solve facebook share part
		ImageView shareOrderItemView = (ImageView)convertView.findViewById(R.id.order_tab_item_fb_share);
		shareOrderItemView.setOnClickListener(new OrderItemShareClickListener(groupPosition, childPosition));
		
		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		String category = mOrderCategories.valueAt(groupPosition);
		return mOrderCategoryGrouping.get(category).size();
	}

	@Override
	public String getGroup(int groupPosition) {
		return mOrderCategories.valueAt(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return mOrderCategories.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return mOrderCategories.keyAt(groupPosition);
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mParentFragment.getActivity()
											.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.order_tab_group, null);
		}
		
		// solve category data part
		String categoryName = getGroup(groupPosition);
		TextView categoryNameView = (TextView) convertView.findViewById(R.id.order_tab_category_name);
		categoryNameView.setText(categoryName);
		
		// solve facebook share part
		ImageView shareCategoryItemView = (ImageView)convertView.findViewById(R.id.order_tab_category_fb_share);
		shareCategoryItemView.setOnClickListener(new OrderCategoryShareClickListener(groupPosition));
		
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}

	
	private class OrderCategoryShareClickListener implements OnClickListener {
		private int mGroupPosition;
		
		
		OrderCategoryShareClickListener(int groupPosition) {
			mGroupPosition = groupPosition;
		}
		
		
		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.order_tab_category_fb_share) {
				Location currentLocation = mParentFragment.getLocation();
				String locationName = "";
				if (currentLocation.isArea()) {
					locationName = currentLocation.getParentName();
				}
				else {
					locationName = currentLocation.getName();
				}
				
				String menuCategory = getGroup(mGroupPosition);
				String statusMessage = "is enjoying some " + menuCategory + " at " + locationName;
				
				showOrderPostDialog(statusMessage);
			}
		}
	}
	
	
	private class OrderItemShareClickListener implements OnClickListener {
		private int mGroupPosition;
		private int mChildPosition;
		
		OrderItemShareClickListener(int groupPosition, int childPosition) {
			mGroupPosition = groupPosition;
			mChildPosition = childPosition;
		}
		
		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.order_tab_item_fb_share) {
				Location currentLocation = mParentFragment.getLocation();
				String locationName = "";
				if (currentLocation.isArea()) {
					locationName = currentLocation.getParentName();
				}
				else {
					locationName = currentLocation.getName();
				}
				
				Map<String, Object> menuItem = getChild(mGroupPosition, mChildPosition);
				String menuItemName = (String)menuItem.get(OrderFeature.ITEM);
				String statusMessage = "";
				
				if (isVowel(menuItemName.toLowerCase().charAt(0))) {
					statusMessage = "is enjoying an " + menuItemName + " at " + locationName;
				}
				else {
					statusMessage = "is enjoying a " + menuItemName + " at " + locationName;
				}
				
				showOrderPostDialog(statusMessage);
			}
		}
	}
	
	
	private void showOrderPostDialog(String message) {
		final String statusMessage = message;
		
		// build the dialog for facebook message post request 
		/*
		Context context = mParentFragment.getActivity();
		ContextThemeWrapper ctw = new ContextThemeWrapper(context, R.style.EnvivedDialogTheme);
		
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctw);
		//AlertDialog.Builder alertDialogBuilder = new OrderCustomAlertDialogFragment(ctw);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.order_tab_fb_share_dialog, null);
		TextView messageView = (TextView)layout.findViewById(R.id.order_tab_fb_share_dialog_message);
		messageView.setText(statusMessage);
		
		// set title
		alertDialogBuilder.setTitle("Post status to Wall");
		
		// set content
		alertDialogBuilder.setView(layout);
		
		// set buttons
		alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				publishOrderOnFB(statusMessage);
			}
		});
		alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		
		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		
		// show it
		alertDialog.show();
		*/
		
		Context context = mParentFragment.getActivity();
		OrderCustomAlertDialogFragment orderPostDialog = 
				OrderCustomAlertDialogFragment.newInstance("Post Status to Wall", null, "OK", "Cancel");
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
		View messageView = inflater.inflate(R.layout.order_tab_fb_share_dialog, null);
		TextView messageTextView = (TextView)messageView.findViewById(R.id.order_tab_fb_share_dialog_message);
		messageTextView.setText(statusMessage);
		
		orderPostDialog.setMessageView(messageView);
		orderPostDialog.setOrderNoticeAlertDialogListener(new OrderNoticeAlertDialogListener() {
			
			@Override
			public void onDialogPositiveClick(DialogFragment dialog) {
				dialog.dismiss();
				publishOrderOnFB(statusMessage);
			}
			
			@Override
			public void onDialogNegativeClick(DialogFragment dialog) {
				dialog.dismiss();
			}
		});
		orderPostDialog.show(mParentFragment.getFragmentManager(), "dialog");
	}
		
	
	private void publishOrderOnFB(String message) {
		
		// set the order message to be published
		mParentFragment.setPublishOrderMessage(message);
		
		// publish it on wall
		mParentFragment.publishOrderOnFB();
	}
	
	private boolean isVowel(char c) {
		return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
	}
}
