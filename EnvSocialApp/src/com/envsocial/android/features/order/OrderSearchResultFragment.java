package com.envsocial.android.features.order;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.envsocial.android.R;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.Preferences;
import com.envsocial.android.utils.SimpleCursorLoader;

public class OrderSearchResultFragment extends SherlockFragment 
					implements LoaderManager.LoaderCallbacks<Cursor>, OnClickListener, ISendOrderRequest {
	
	private static final String TAG = "OrderSearchResultFragment";
	
	public static final int DIALOG_REQUEST = 0;
	
	// the feature query
	private String mFeatureQuery;
	private OrderFeature mOrderFeature;
	
	private TextView mResultsCountText;
	private Button mBtnOrder;
	private ListView mListView;
	private OrderSearchCursorAdapter mAdapter;
	
	private List<Map<String, Object>> mCurrentOrderSelections;
	
	private ProgressDialog mSearchLoaderDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			mFeatureQuery = args.getString("query");
		}
		
		Location location = Preferences.getCheckedInLocation(getActivity());
		mOrderFeature = (OrderFeature)location.getFeature(Feature.ORDER);
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.catalog_search_result, container, false);
		
		mListView = (ListView) v.findViewById(R.id.catalog_search_result_list);
		
		TextView noResultsView = (TextView)inflater.inflate(R.layout.catalog_search_no_results, null);
		noResultsView.setText(getString(R.string.search_no_results, mFeatureQuery));
		mListView.setEmptyView(noResultsView);
		
		
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
		
		
		mAdapter = new OrderSearchCursorAdapter(mOrderFeature, getActivity(),
				R.layout.catalog_search_result_item, null, 0);
		
		mListView.setAdapter(mAdapter);
		
		
		getLoaderManager().initLoader(0, null, this);
	}
	
	
	@Override
	public void onDestroy() {
		Log.d(TAG, " --- onDestroy called in OrderFragment");
		super.onDestroy();
		
		mAdapter.doCleanup();
	}
	
	public void newSearchQuery(String query) {
		// update internal query and restart loader
		mFeatureQuery = query;
		getLoaderManager().restartLoader(0, null, this);
	}
	
	
	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
		mSearchLoaderDialog.show();
		return new OrderSearchCursorLoader(getActivity(), mOrderFeature, mFeatureQuery);
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
			List<Map<String, Object>> searchOrderSelections = mAdapter.getOrderSelections();
			
			if (!searchOrderSelections.isEmpty()) {
				OrderDialogFragment summaryDialog = OrderDialogFragment.newInstance(searchOrderSelections);
				summaryDialog.setTargetFragment(this, DIALOG_REQUEST);
				summaryDialog.show(getFragmentManager(), "dialog");
			}
		}
		
	}
	
	@Override
	public void sendOrder(OrderDialogFragment dialog) {
		String orderJSON = dialog.getOrderJSONString();
		mCurrentOrderSelections = dialog.getOrderSelections();
		
		dialog.dismiss();
		
		Location location = Preferences.getCheckedInLocation(getActivity());
		Annotation order = new Annotation(location, Feature.ORDER, Calendar.getInstance(), orderJSON);
		new SendOrderRequestTask(getActivity(), this, OrderFeature.NEW_ORDER_NOTIFICATION, order).execute();
		
	}
	
	@Override
	public void postSendOrderRequest(String orderRequestType, Annotation orderRequest, boolean success) {
		if (orderRequestType.compareTo(OrderFeature.NEW_ORDER_NOTIFICATION) == 0) {
			if (success) {
				SparseArray<Map<String, Object>> orderTab = OrderFragment.getOrderTabInstance();
				
				// add current selections to tab then clear them
				for (Map<String, Object> itemData : mCurrentOrderSelections) {
					int itemId = (Integer) itemData.get(OrderFeature.ITEM_ID);
					
					Map<String, Object> itemTab = orderTab.get(itemId);
					if (itemTab == null) {
						itemTab = new HashMap<String, Object>();
						itemTab.putAll(itemData);
						
						orderTab.put(itemId, itemTab);
					}
					else {
						Integer tabQuantity = (Integer)itemTab.get("quantity");
						tabQuantity += (Integer)itemData.get("quantity");
						itemTab.put("quantity", tabQuantity);
					}
				}
			}
			
			// clear current temporary selections in OrderSearchResultFragment
			mCurrentOrderSelections = null;
			
			// clear them in the pager adapter as well
			mAdapter.clearOrderSelections();
		}
	}
	
	// ##################################### Helper static classes #####################################
	
	static class OrderSearchCursorLoader extends SimpleCursorLoader {
		private String mQuery;
		private OrderFeature mFeature;
		
		public OrderSearchCursorLoader(Context context, OrderFeature orderFeature, String query) {
			super(context);
			mFeature = orderFeature;
			mQuery = query;
		}

		@Override
		public Cursor loadInBackground() {
			Cursor cursor = mFeature.localSearchQuery(mQuery);
			
			return cursor;
		}
	}
	
}
