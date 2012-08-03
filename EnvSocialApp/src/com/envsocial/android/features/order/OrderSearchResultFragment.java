package com.envsocial.android.features.order;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragment;
import com.envsocial.android.R;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.SimpleCursorLoader;

public class OrderSearchResultFragment extends SherlockFragment 
					implements LoaderManager.LoaderCallbacks<Cursor>,
								OnClickListener {
	
	private static final String TAG = "OrderSearchResultFragment";
	
	public static final int DIALOG_REQUEST = 0;
	
	// the feature query
	private String mFeatureQuery;
	
	private TextView mResultsCountText;
	private Button mBtnOrder;
	private ListView mListView;
	private OrderSearchCursorAdapter mAdapter;
	
	private ProgressDialog mSearchLoaderDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			mFeatureQuery = args.getString("query");
		}
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.catalog_search_result, container, false);
		
		mListView = (ListView) v.findViewById(R.id.catalog_search_result_list);
		
		TextView noResultsView = (TextView)inflater.inflate(R.layout.catalog_search_no_results, null);
		noResultsView.setText(getString(R.string.search_no_results, mFeatureQuery));
		mListView.setEmptyView(noResultsView);
		
		int[] colors = {0, getResources().getColor(R.color.light_green), 0};
		mListView.setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, colors));
		mListView.setDividerHeight(2);
		
		mResultsCountText = (TextView) v.findViewById(R.id.catalog_search_result_header_text);
		
		mBtnOrder = (Button) v.findViewById(R.id.catalog_search_result_btn_order);
		mBtnOrder.setOnClickListener(this);
		
	    return v;
	}
	
	
	@Override 
	public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        mSearchLoaderDialog = new ProgressDialog(getActivity());
		mSearchLoaderDialog.setIndeterminate(true);
		mSearchLoaderDialog.setTitle("Loading search results ...");
		
		
		mAdapter = new OrderSearchCursorAdapter(getActivity(), 
				R.layout.catalog_search_result_item, null, 
				/*CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER*/ 0);
		
		mListView.setAdapter(mAdapter);
		
		
		getLoaderManager().initLoader(0, null, this);
	}
	
	
	public void newSearchQuery(String query) {
		// update internal query and restart loader
		mFeatureQuery = query;
		getLoaderManager().restartLoader(0, null, this);
	}
	
	
	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
		mSearchLoaderDialog.show();
		return new OrderSearchCursorLoader(getActivity(), mFeatureQuery);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter.swapCursor(cursor);
		mSearchLoaderDialog.cancel();
		
		int rowCt = cursor.getCount();
		Log.i(TAG, "#### There should be: " + rowCt + " results. ####");
		mResultsCountText.setText(getResources().getQuantityString(R.plurals.search_results, 
													rowCt, rowCt, mFeatureQuery));
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}


	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.catalog_search_result_btn_order) {
			Map<Integer, Map<String, Object>> searchOrderSelections = mAdapter.getSearchOrderSelections();
			
			if (!searchOrderSelections.isEmpty()) {
				OrderSearchDialogFragment summaryDialog = 
						OrderSearchDialogFragment.newInstance(searchOrderSelections);
					summaryDialog.setTargetFragment(this, DIALOG_REQUEST);
					summaryDialog.show(getFragmentManager(), "dialog");
			}
		}
		
	}
	
	
	void sendOrder(OrderSearchDialogFragment dialog) {
		String orderJSON = dialog.getOrderJSONString();
		dialog.dismiss();
		
		Location location = Preferences.getCheckedInLocation(getActivity());
		
		Annotation order = new Annotation(location, 
				Feature.ORDER, Calendar.getInstance(), orderJSON);
		new SendOrderTask(getActivity(), order).execute();
		
	}
	
	// ##################################### Helper static classes #####################################
	
	static class OrderSearchCursorLoader extends SimpleCursorLoader {
		private String mQuery;
		
		public OrderSearchCursorLoader(Context context, String query) {
			super(context);
			mQuery = query;
		}

		@Override
		public Cursor loadInBackground() {
			Location location = Preferences.getCheckedInLocation(this.getContext());
			Feature feat = location.getFeature(Feature.ORDER);
			
			Cursor cursor = feat.localQuery(mQuery);
			
			return cursor;
		}
	}
	
	static class OrderSearchDialogFragment extends SherlockDialogFragment implements OnClickListener {
		private final String SUMMARY_TITLE = "Order Summary";
		
		private Button mBtnOrder;
		private Button mBtnCancel;
		private TextView mTotalOrderPrice;
		private Map<Integer, Map<String, Object>> mSearchOrderSelections;
		private List<Map<String,String>> mOrderSummary;
		
		static OrderSearchDialogFragment newInstance(Map<Integer, Map<String, Object>> searchOrderSelections) {
			OrderSearchDialogFragment f = new OrderSearchDialogFragment();
			
			Bundle args = new Bundle();
			args.putSerializable("selections", (Serializable)searchOrderSelections);
			
			f.setArguments(args);
			return f;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			mSearchOrderSelections = (Map<Integer, Map<String, Object>>) getArguments().get("selections");
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
			
			for (Integer idx : mSearchOrderSelections.keySet()) {
				// the keys have no importance; we just want access to the elements
				Map<String, Object> selection = mSearchOrderSelections.get(idx);
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
			
			return quantity + " " + itemName + " (" + price + " RON)" + "\n";
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
		
		
		public void onClick(View v) {
			if (v == mBtnOrder) {
				((OrderSearchResultFragment) getTargetFragment()).sendOrder(this);
			} else if (v == mBtnCancel) {
				dismiss();
			}
		}
	}

}
