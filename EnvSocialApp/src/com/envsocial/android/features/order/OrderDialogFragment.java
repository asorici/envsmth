package com.envsocial.android.features.order;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.envsocial.android.R;

public class OrderDialogFragment extends DialogFragment implements OnClickListener {
	
	private final String SUMMARY_TITLE = "Order Summary";
	
	private List<Map<String,String>> mCategories;
	private List<List<Map<String,String>>> mItems;
	private Map<Integer,Map<Integer,Integer>> mCounter;
	private List<Map<String,String>> mOrderSummary;
	
	private Button mBtnOrder;
	private Button mBtnCancel;
	
	static OrderDialogFragment newInstance(List<Map<String,String>> categories,
			List<List<Map<String,String>>> items, Map<Integer,Map<Integer,Integer>> counter) {
		OrderDialogFragment f = new OrderDialogFragment();
		
		Bundle args = new Bundle();
		args.putSerializable("categories", (Serializable) categories);
		args.putSerializable("items", (Serializable) items);
		args.putSerializable("counter", (Serializable) counter);
		f.setArguments(args);
		
		return f;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mCategories = (List<Map<String,String>>) getArguments().get("categories");
		mItems= (List<List<Map<String,String>>>) getArguments().get("items");
		mCounter= (Map<Integer,Map<Integer,Integer>>) getArguments().get("counter");
		mOrderSummary = new ArrayList<Map<String,String>>();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Set title
		getDialog().setTitle(SUMMARY_TITLE);
		
		View v = inflater.inflate(R.layout.order_dialog, container, false);
		
		ListView list = (ListView) v.findViewById(R.id.summary_list);
		mOrderSummary = getOrderSummary();
		
		// TODO empty order
//		if (!mOrderSummary.isEmpty()) {
			SimpleAdapter adapter = new SimpleAdapter(getActivity(),
					mOrderSummary,
		    		R.layout.order_dialog_row,
		    		new String[] { "category", "items" },
		    		new int[] { R.id.category, R.id.items }
		    		);
			View footer = inflater.inflate(R.layout.order_dialog_footer, null, false);
			list.addFooterView(footer);
			list.setAdapter(adapter);
			
			mBtnOrder = (Button) footer.findViewById(R.id.btn_order);
			mBtnOrder.setOnClickListener(this);
			mBtnCancel = (Button) footer.findViewById(R.id.btn_cancel);
			mBtnCancel.setOnClickListener(this);
//		}
		
		return v;
	}
	
	private List<Map<String,String>> getOrderSummary() {
		StringBuilder categorySummary = new StringBuilder();
		for (Integer groupIndex : mCounter.keySet()) {
			// Consider each category with an order counter
			Map<Integer,Integer> groupCounter = mCounter.get(groupIndex);
			for (Integer itemIndex : groupCounter.keySet()) {
				// Consider each item with a counter from that category
				// Get order quantity
				Integer quantity = groupCounter.get(itemIndex);
				if (quantity > 0) {
					// Add order to summary
					Map<String,String> itemData = mItems.get(groupIndex).get(itemIndex);
					categorySummary.append(buildSummaryRow(quantity, itemData));
				}
			}
			// Check that we have an order for this category
			if (categorySummary.length() > 0) {
				Map<String,String> catWrapper = new HashMap<String,String>();
				Map<String,String> catData = mCategories.get(groupIndex);
				catWrapper.put("category", catData.get(OrderFeature.CATEGORY_NAME));
				catWrapper.put("items", categorySummary.toString());
				mOrderSummary.add(catWrapper);
			}
			categorySummary.delete(0, categorySummary.length());
		}
		
		System.out.println("[DEBUG]>> order summary: " + mOrderSummary);
		
		return mOrderSummary;
	}
	
	public void onClick(View v) {
		if (v == mBtnOrder) {
			((OrderFragment) getTargetFragment()).sendOrder(this);
		} else if (v == mBtnCancel) {
			dismiss();
		}
	}
	
	private String buildSummaryRow(Integer quantity, Map<String,String> itemData) {
		String itemName = (String) itemData.get(OrderFeature.ITEM_NAME);
		return quantity.toString() + " " + itemName + "\n";
	}
	
	public String getOrderJSONString() {
		try {
			JSONObject allOrderJSON = new JSONObject();
			JSONArray orderListJSON = new JSONArray();
			for (Map<String,String> group : mOrderSummary) {
				JSONObject orderJSON = new JSONObject();
				orderJSON.put("category", group.get("category"));
				orderJSON.put("items", group.get("items"));
				orderListJSON.put(orderJSON);
			}
			allOrderJSON.put("order", orderListJSON);
			
			return allOrderJSON.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public boolean isEmpty() {
		return mOrderSummary.isEmpty();
	}
	
}
