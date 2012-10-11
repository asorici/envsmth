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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.envsocial.android.R;

public class OrderDialogFragment extends SherlockDialogFragment implements OnClickListener {
	
	private final String SUMMARY_TITLE = "Order Summary";
	
	private Button mBtnOrder;
	private Button mBtnCancel;
	private TextView mTotalOrderPrice;
	private List<Map<String, Object>> mOrderSelections;
	private List<Map<String,String>> mOrderSummary;
	
	static OrderDialogFragment newInstance(List<Map<String, Object>> orderSelections) {
		OrderDialogFragment f = new OrderDialogFragment();
		
		Bundle args = new Bundle();
		args.putSerializable("selections", (Serializable)orderSelections);
		
		f.setArguments(args);
		return f;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mOrderSelections = (List<Map<String, Object>>) getArguments().get("selections");
		mOrderSummary = new ArrayList<Map<String,String>>();
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Set title
		getDialog().setTitle(SUMMARY_TITLE);
		View v = inflater.inflate(R.layout.order_dialog, container, false);
		
		ListView list = (ListView) v.findViewById(R.id.summary_list);
		double totalPrice = getOrderSummary();
		
		SimpleAdapter adapter = new SimpleAdapter(getActivity(),
				mOrderSummary,
				R.layout.order_dialog_row,
				new String[] { "category", "items" },
				new int[] { R.id.category, R.id.items }
				);
		View footer = inflater.inflate(R.layout.order_dialog_footer, null, false);
		list.addFooterView(footer);
		list.setAdapter(adapter);
		
		mTotalOrderPrice = (TextView) footer.findViewById(R.id.order_dialog_total_price);
		mTotalOrderPrice.setText("" + totalPrice + " RON");
		
		mBtnOrder = (Button) footer.findViewById(R.id.btn_order);
		mBtnOrder.setOnClickListener(this);
		mBtnCancel = (Button) footer.findViewById(R.id.btn_cancel);
		mBtnCancel.setOnClickListener(this);
		
		return v;
	}
	
	
	private double getOrderSummary() {
		double totalPrice = 0;
		Map<String, List<Map<String, Object>>> summaryCategoryGrouping = 
				new HashMap<String, List<Map<String, Object>>>();
		
		int orderLen = mOrderSelections.size();
		for (int idx = 0; idx < orderLen; idx++ ) {
			// the keys have no importance; we just want access to the elements
			Map<String, Object> selection = mOrderSelections.get(idx);
			String category = (String)selection.get(OrderFeature.CATEGORY);
			
			totalPrice += 
					(Integer) selection.get("quantity") * (Double) selection.get(OrderFeature.ITEM_PRICE);
			
			if (summaryCategoryGrouping.containsKey(category)) {
				summaryCategoryGrouping.get(category).add(selection);
			}
			else {
				List<Map<String, Object>> categorySelectedItemList = new ArrayList<Map<String,Object>>();
				categorySelectedItemList.add(selection);
				
				summaryCategoryGrouping.put(category, categorySelectedItemList);
			}
		}
		
		StringBuilder categorySummary = new StringBuilder();
		
		for (String category : summaryCategoryGrouping.keySet()) {
			List<Map<String, Object>> categorySelectedItemList = summaryCategoryGrouping.get(category);
			
			for (Map<String, Object> itemData : categorySelectedItemList) {
				categorySummary.append(buildSummaryRow(itemData));
			}
			
			Map<String,String> catWrapper = new HashMap<String,String>();
			catWrapper.put("category", category);
			catWrapper.put("items", categorySummary.toString());
			mOrderSummary.add(catWrapper);
			
			categorySummary.delete(0, categorySummary.length());
		}
		
		return totalPrice;
	}
	
	
	private String buildSummaryRow(Map<String, Object> itemData) {
		String itemName = (String) itemData.get(OrderFeature.ITEM);
		double price = (Double) itemData.get(OrderFeature.ITEM_PRICE);
		int quantity = (Integer) itemData.get("quantity");
		
		return quantity + " x " + itemName + " (" + price + " RON)" + "\n";
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
			
			// put the item IDs in a separate list
			JSONArray itemIdListJSON = new JSONArray();  
			int orderLen = mOrderSelections.size();
			for (int idx = 0; idx < orderLen; idx++ ) {
				Map<String, Object> selection = mOrderSelections.get(idx);
				
				JSONObject itemJSON = new JSONObject();
				itemJSON.put(OrderFeature.ITEM_ID, selection.get(OrderFeature.ITEM_ID));
				itemJSON.put("quantity", selection.get("quantity"));
				itemIdListJSON.put(itemJSON);
			}
			allOrderJSON.put("item_id_list", itemIdListJSON);
			
			return allOrderJSON.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	public boolean isEmpty() {
		return mOrderSummary.isEmpty();
	}
	
	public List<Map<String, Object>> getOrderSelections() {
		return mOrderSelections;
	}
	
	public void onClick(View v) {
		if (v == mBtnOrder) {
			((ISendOrder) getTargetFragment()).sendOrder(this);
		} else if (v == mBtnCancel) {
			dismiss();
		}
	}
}
