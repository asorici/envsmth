package com.envsocial.android.features.order;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.envsocial.android.R;

public class OrderTabDialogFragment extends SherlockDialogFragment implements OnClickListener {
private final String SUMMARY_TITLE = "Order Summary";
	
	private Button mBtnOk;
	
	private TextView mTotalOrderPrice;
	private List<Map<String, Object>> mOrderSelections;
	private List<Map<String,String>> mOrderSummary;
	
	static OrderTabDialogFragment newInstance(List<Map<String, Object>> searchOrderSelections) {
		OrderTabDialogFragment f = new OrderTabDialogFragment();
		
		Bundle args = new Bundle();
		args.putSerializable("selections", (Serializable)searchOrderSelections);
		
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
		View v = inflater.inflate(R.layout.order_tab_dialog, container, false);
		
		ListView list = (ListView) v.findViewById(R.id.order_tab_summary_list);
		double totalPrice = getOrderSummary();
		
		SimpleAdapter adapter = new SimpleAdapter(getActivity(),
				mOrderSummary,
				R.layout.order_dialog_row,
				new String[] { "category", "items" },
				new int[] { R.id.category, R.id.items }
				);
		View footer = inflater.inflate(R.layout.order_tab_dialog_footer, null, false);
		list.addFooterView(footer);
		list.setAdapter(adapter);
		
		mTotalOrderPrice = (TextView) footer.findViewById(R.id.order_tab_dialog_total_price);
		mTotalOrderPrice.setText("" + totalPrice + " RON");
		
		mBtnOk = (Button) footer.findViewById(R.id.btn_tab_ok);
		mBtnOk.setOnClickListener(this);
		
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

	
	@Override
	public void onClick(View v) {
		if (v == mBtnOk) {
			dismiss();
		}
	}
}
