package com.envsocial.android.features.order;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.envsocial.android.R;
import com.envsocial.android.api.ActionHandler;
import com.envsocial.android.api.Annotation;
import com.envsocial.android.api.EnvSocialResource;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.exceptions.EnvSocialComException;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.ResponseHolder;

public class OrderFragment extends SherlockFragment implements OnClickListener {
	private static final String TAG = "OrderFragment";
	
	public static final int DIALOG_REQUEST = 0;
	
	private Location mLocation;
	
	private OrderMenu mOrderMenu;
	private Button mBtnOrder;
	
	// loader dialog for sending an order
	private ProgressDialog mSendOrderDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	}
	
	/*
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "[INFO] ------------- onActivityCreated called -----------------");
		super.onActivityCreated(savedInstanceState);
	}
	*/
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							Bundle savedInstanceState) {
		Log.i(TAG, "[INFO] onCreateView called.");
		
		// Inflate layout for this fragment.
		View v = inflater.inflate(R.layout.catalog, container, false);
		
		mLocation = (Location) getArguments().get(ActionHandler.CHECKIN);
		String menuJSON = mLocation.getFeatureData(Feature.ORDER).getSerializedData();
		
		try {
			mOrderMenu = new OrderMenu(menuJSON);
			
			// Create custom expandable list adapter
			OrderCatalogListAdapter adapter = new OrderCatalogListAdapter(getActivity(),
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
		} catch (EnvSocialContentException e) {
			Log.d(TAG, e.getMessage(), e);
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
			summaryDialog.show(getFragmentManager(), "dialog");
		}
	}
	
	public void sendOrder(OrderDialogFragment dialog) {
		String orderJSON = dialog.getOrderJSONString();
		dialog.dismiss();
		
		Annotation order = new Annotation(getActivity(), mLocation, 
				Feature.ORDER, Calendar.getInstance(), orderJSON);
		new SendOrderTask(order).execute();
		
	}
	
	private class SendOrderTask extends AsyncTask<Void, Void, ResponseHolder> {
		private Annotation order;
		
		public SendOrderTask(Annotation order) {
			this.order = order;
		}
		
		@Override
		protected void onPreExecute() {
			mSendOrderDialog = ProgressDialog.show(OrderFragment.this.getActivity(), 
					"", "Sending Order ...", true);
		}
		
		@Override
		protected ResponseHolder doInBackground(Void...args) {
			return order.post();
		}
		
		@Override
		protected void onPostExecute(ResponseHolder holder) {
			mSendOrderDialog.cancel();
			
			if (!holder.hasError()) {
				boolean error = false;
				int msgId = R.string.msg_send_order_ok;

				switch(holder.getCode()) {
				case HttpStatus.SC_CREATED: 					
					error = false;
					break;

				case HttpStatus.SC_BAD_REQUEST:
					msgId = R.string.msg_send_order_400;
					error = true;
					break;

				case HttpStatus.SC_UNAUTHORIZED:
					msgId = R.string.msg_send_order_401;
					error = true;
					break;

				case HttpStatus.SC_METHOD_NOT_ALLOWED:
					msgId = R.string.msg_send_order_405;
					error = true;
					break;

				default:
					msgId = R.string.msg_send_order_err;
					error = true;
					break;
				}

				if (error) {
					Log.d(TAG, "[DEBUG]>> Error sending order: " + msgId);
					Toast toast = Toast.makeText( OrderFragment.this.getActivity(),
							msgId, Toast.LENGTH_LONG);
					toast.show();
				}
				else {
					Toast toast = Toast.makeText( OrderFragment.this.getActivity(),
							msgId, Toast.LENGTH_LONG);
					toast.show();
				}
			}
			else {
				int msgId = R.string.msg_service_unavailable;

				try {
					throw holder.getError();
				} catch (EnvSocialComException e) {
					Log.d(TAG, e.getMessage(), e);
					msgId = R.string.msg_service_unavailable;
				} catch (EnvSocialContentException e) {
					Log.d(TAG, e.getMessage(), e);
					msgId = R.string.msg_service_error;
				} catch (Exception e) {
					Log.d(TAG, e.toString(), e);
					msgId = R.string.msg_service_error;
				}

				Toast toast = Toast.makeText(OrderFragment.this.getActivity(), msgId, Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}
	
	public static class OrderMenu {
		
		private List<Map<String,String>> mCategories;
		private List<List<Map<String,String>>> mItems;
		private Map<Integer,Map<Integer,Integer>> mCounter;
		
		public static final String CATEGORY = "category";
		public static final String ITEM_NAME = "name";
		public static final String ITEM_DESCRIPTION = "description";
		public static final String ITEM_PRICE = "price";
		
		
		OrderMenu(String jsonString) throws EnvSocialContentException {
			// Init counter
			mCounter = new HashMap<Integer,Map<Integer,Integer>>();
			
			try {
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
						map.put(ITEM_NAME, item.optString("name", "unknown"));
						map.put(ITEM_DESCRIPTION, item.optString("description", "No description available"));
						map.put(ITEM_PRICE, item.optString("price", "-- RON"));
						// Add item map to category
						catItems.add(map);
					}
					mItems.add(catItems);
				}
			} catch (JSONException e) {
				throw new EnvSocialContentException(jsonString, EnvSocialResource.FEATURE, e);
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
