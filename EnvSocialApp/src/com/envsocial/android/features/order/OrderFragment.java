package com.envsocial.android.features.order;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.api.Location;
import com.envsocial.android.features.Feature;

public class OrderFragment extends Fragment implements OnClickListener {

	public static final int DIALOG_REQUEST = 0;
	
	private Location mLocation;
	
	private OrderMenu mOrderMenu;
	private Button mBtnOrder;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		// Inflate layout for this fragment.
		View v = inflater.inflate(R.layout.catalog, container, false);
		
		try {
			mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
			String menuJSON = mLocation.getFeatureData(Feature.ORDER);
			mOrderMenu = new OrderMenu(menuJSON);
			
			// Create custom expandable list adapter
			CatalogListAdapter adapter = new CatalogListAdapter(getActivity(),
		    		mOrderMenu.getCategoryData(),
		    		R.layout.catalog_group,
		    		new String[] { OrderMenu.CATEGORY },
		    		new int[] { R.id.orderGroup },
		    		mOrderMenu.getItemData(),
		    		R.layout.catalog_item,
		    		new String[] { OrderMenu.ITEM_NAME },
		    		new int[] { R.id.orderItem },
		    		mOrderMenu.getCounter()
		    		);
			
		    // Set adapter
		    ExpandableListView listView = (ExpandableListView) v.findViewById(R.id.catalog);
		    listView.setAdapter(adapter);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		mBtnOrder = (Button) v.findViewById(R.id.btn_order);
		mBtnOrder.setOnClickListener(this);
	    
	    return v;
	}
	
	public void onClick(View v) {
		if (v == mBtnOrder) {
			OrderDialogFragment summaryDialog = 
				OrderDialogFragment.newInstance(mOrderMenu.getCategoryData(), 
						mOrderMenu.getItemData(), mOrderMenu.getCounter());
			summaryDialog.setTargetFragment(this, DIALOG_REQUEST);
			summaryDialog.show(getSupportFragmentManager(), "dialog");
		}
	}
	
	public void sendOrder(OrderDialogFragment dialog) {
		String orderJSON = dialog.getOrderJSONString();
		dialog.dismiss();
		try {
			Annotation order = new Annotation(getActivity(), mLocation, 
					Feature.ORDER, Calendar.getInstance(), orderJSON);
			order.post();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Toast toast = Toast.makeText(getActivity(), "Sending order: " + orderJSON, Toast.LENGTH_LONG);
		toast.show();
	}
	
	
	public static class OrderMenu {
		
		private List<Map<String,String>> mCategories;
		private List<List<Map<String,String>>> mItems;
		private Map<Integer,Map<Integer,Integer>> mCounter;
		
		public static final String CATEGORY = "category";
		public static final String ITEM_NAME = "name";
		public static final String ITEM_DESCRIPTION = "description";
		public static final String ITEM_PRICE = "price";
		
		
		OrderMenu(String jsonString) throws JSONException {
			// Init counter
			mCounter = new HashMap<Integer,Map<Integer,Integer>>();
			// Grab menu
			JSONArray orderMenu = (JSONArray) new JSONObject(jsonString).getJSONArray("order_menu");
			
			// Init data structures
			mCategories = new ArrayList<Map<String,String>>();
			mItems = new ArrayList<List<Map<String,String>>>();
			
			// Parse categories
			int nCategories = orderMenu.length();
			for (int i = 0; i < nCategories; ++ i) {
				JSONObject elem = orderMenu.getJSONObject(i);
				// Bind and add category
				Map<String,String> map = new HashMap<String,String>();
				map.put(CATEGORY, elem.getString("category"));
				mCategories.add(map);
				// Add items
				JSONArray itemsArray = elem.getJSONArray("items");
				List<Map<String,String>> catItems = new ArrayList<Map<String,String>>();
				int nItems = itemsArray.length();
				for (int j = 0; j < nItems; ++ j) {
					// Bind item data to map
					map = new HashMap<String,String>();
					JSONObject item = itemsArray.getJSONObject(j);
					map.put(ITEM_NAME, item.getString("name"));
//					map.put(ITEM_DESCRIPTION, item.getString("description"));
//					map.put(ITEM_PRICE, item.getString("price"));
					// Add item map to category
					catItems.add(map);
				}
				mItems.add(catItems);
			}
		}
		
		public List<Map<String,String>> getCategoryData() {
			return mCategories;
		}
		
		public List<List<Map<String,String>>> getItemData() {
			return mItems;
		}
		
		public Map<Integer,Map<Integer,Integer>> getCounter() {
			return mCounter;
		}
		
	}
	
}
