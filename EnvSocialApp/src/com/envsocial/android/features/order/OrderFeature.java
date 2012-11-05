package com.envsocial.android.features.order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.envsocial.android.Envived;
import com.envsocial.android.api.EnvSocialResource;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;

public class OrderFeature extends Feature {
	private static final long serialVersionUID = 1L;
	private static final String TAG = "OrderFeature";
	
	private List<Map<String,String>> mCategories;
	private List<List<Map<String,String>>> mItems;
	private OrderDbHelper dbHelper;
	
	public static final String TYPE_DRINKS = "drinks";
	public static final String TYPE_FOOD = "food";
	public static final String TYPE_DESERT = "desert";
	
	public static final String CATEGORY = "category";
	public static final String CATEGORY_ID = "id";
	public static final String CATEGORY_NAME = "name";
	public static final String CATEGORY_TYPE = "type";
	
	public static final String ITEM = "item";
	public static final String ITEM_ID = "id";
	public static final String ITEM_CATEGORY_ID = "category_id";
	//public static final String ITEM_TYPE = "item_type";
	public static final String ITEM_NAME = "name";
	public static final String ITEM_DESCRIPTION = "description";
	public static final String ITEM_PRICE = "price";
	public static final String ITEM_USAGE_RANK = "usage_rank";
	
	public static final String NEW_ORDER_NOTIFICATION = "new_order";
	public static final String RESOLVED_ORDER_NOTIFICATION = "resolved_order";
	public static final String UPDATE_CONTENT_NOTIFICATION = "update_content";
	public static final String UPDATE_STRUCTURE_NOTIFICATION = "update_structure";
	
	public OrderFeature(String category, int version, String resourceUri,
			String environmentUri, String areaUri, String data) throws EnvSocialContentException {
		
		super(category, version, resourceUri, environmentUri, areaUri, data);
	}

	@Override
	public void init() throws EnvSocialContentException {
		super.init();
		/*
		if (mCategories == null || mItems == null) {
			buildOrderMappings();
		}	
		*/
		
		if (dbHelper == null) {
			dbHelper = new OrderDbHelper(Envived.getContext(), this, version);
		}
		
		if (dbHelper != null) {
			dbHelper.init();
		}
	}
	
	
	@Override
	public void doUpdate() throws EnvSocialContentException {
		super.doUpdate();
		
		// rebuild order mappings and update the dbHelper
		buildOrderMappings();
		
		if (dbHelper == null) {
			dbHelper = new OrderDbHelper(Envived.getContext(), this, version);
		}
		
		dbHelper.update();
	}
	
	@Override
	public void doCleanup(Context context) {
		super.doCleanup(context);
		
		if (dbHelper != null) {
			dbHelper.close();
			dbHelper = null;
		}
	}
	
	@Override
	public void doClose(Context context) {
		super.doClose(context);
		
		// first do cleanup
		doCleanup(context);
		
		// then remove the database file entirely
		context.deleteDatabase(OrderDbHelper.DATABASE_NAME);
	}

	
	private void buildOrderMappings() throws EnvSocialContentException {
		// the JSON encoded data is in the data field
		Log.d(TAG, "[DEBUG] >> Initializing order mappings.");
		
		try {
			// Grab menu
			JSONArray orderMenu = (JSONArray) new JSONObject(data).getJSONArray("order_menu");
			
			// Init data structures
			mCategories = new ArrayList<Map<String,String>>();
			mItems = new ArrayList<List<Map<String,String>>>();
			
			// Parse categories
			int nCategories = orderMenu.length();
			for (int i = 0; i < nCategories; ++ i) {
				JSONObject elem = orderMenu.getJSONObject(i);
				
				// Bind and add category
				JSONObject categoryObject = elem.getJSONObject(CATEGORY);
				Map<String,String> map = new HashMap<String,String>();
				
				map.put(CATEGORY_ID, categoryObject.getString(CATEGORY_ID));
				map.put(CATEGORY_NAME, categoryObject.getString(CATEGORY_NAME));
				map.put(CATEGORY_TYPE, categoryObject.getString(CATEGORY_TYPE));
				mCategories.add(map);
				
				
				// Add items
				JSONArray itemsArray = elem.getJSONArray("items");
				List<Map<String,String>> catItems = new ArrayList<Map<String,String>>();
				int nItems = itemsArray.length();
				for (int j = 0; j < nItems; ++ j) {
					// Bind item data to map
					map = new HashMap<String,String>();
					JSONObject item = itemsArray.getJSONObject(j);
					
					map.put(ITEM_ID, item.getString(ITEM_ID));
					//map.put(ITEM_TYPE, categoryObject.getString(CATEGORY_TYPE));
					map.put(ITEM_NAME, item.getString(ITEM_NAME));
					map.put(ITEM_DESCRIPTION, item.optString("description", "No description available"));
					map.put(ITEM_PRICE, item.getString(ITEM_PRICE));
					map.put(ITEM_USAGE_RANK, item.getString(ITEM_USAGE_RANK));
					
					// Add item map to category
					catItems.add(map);
				}
				mItems.add(catItems);
			}
		} catch (JSONException e) {
			throw new EnvSocialContentException(data, EnvSocialResource.FEATURE, e);
		}
	}

	public List<Map<String,String>> getOrderCategories() {
		return mCategories;
	}
	
	public List<Map<String,String>> getOrderCategories(String type) {
		List<Map<String,String>> categories = new ArrayList<Map<String,String>>();
		for (Map<String, String> cat : mCategories) {
			if (cat.get(CATEGORY_TYPE).equalsIgnoreCase(type)) {
				categories.add(cat);
			}
		}
		
		return categories;
	}
	
	public List<List<Map<String,String>>> getOrderItems() {
		return mItems;
	}
	
	public List<List<Map<String,String>>> getOrderItems(String type) {
		List<List<Map<String,String>>> items = new ArrayList<List<Map<String,String>>>();
		
		// this is sort of a hack; I don't like it - the fault lies with the requirements of 
		// SimpleExpandibleListAdapter
		// the trick is based on the fact that mCategories and mItems match exactly in length and
		// position of elements
		for (int i = 0; i < mCategories.size(); i++) {
			Map<String, String> cat = mCategories.get(i);
			if (cat.get(CATEGORY_TYPE).equalsIgnoreCase(type)) {
				items.add(mItems.get(i));
			}
		}
		
		return items;
	}
	
	@Override
	public boolean hasLocalDatabaseSupport() {
		return true;
	}

	@Override
	public boolean hasLocalQuerySupport() {
		return true;
	}

	@Override
	public SQLiteOpenHelper getLocalDatabaseSupport() {
		return dbHelper;
	}

	@Override
	public Cursor localSearchQuery(String query) {
		if (dbHelper != null) {
			return dbHelper.searchQuery(query);
		}
		
		return null;
	}
	
	
	public Cursor getOrderCategoryCursor(String type) {
		if (dbHelper != null) {
			return dbHelper.getCategoryCursor(type);
		}
		
		return null;
	}
	
	
	public Cursor getOrderItemCursor(int categoryId) {
		if (dbHelper != null) {
			return dbHelper.getItemCursor(categoryId);
		}
		
		return null;
	}
}
