package com.envsocial.android.features.order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;

import com.envsocial.android.Envived;
import com.envsocial.android.api.EnvSocialResource;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;

public class OrderFeature extends Feature {
	private List<Map<String,String>> mCategories;
	private List<List<Map<String,String>>> mItems;
	private OrderDbHelper dbHelper;
	
	public static final String CATEGORY = "category";
	public static final String CATEGORY_ID = "id";
	public static final String CATEGORY_NAME = "name";
	public static final String CATEGORY_TYPE = "type";
	
	public static final String ITEM = "item";
	public static final String ITEM_ID = "id";
	public static final String ITEM_CATEGORY_ID = "category_id";
	public static final String ITEM_NAME = "name";
	public static final String ITEM_DESCRIPTION = "description";
	public static final String ITEM_PRICE = "price";
	
	public OrderFeature(String category, String resourceUri,
			String environmentUri, String areaUri, String data) throws EnvSocialContentException {
		
		super(category, resourceUri, environmentUri, areaUri, data);
	}

	@Override
	public void init() throws EnvSocialContentException {
		if (mCategories == null || mItems == null) {
			buildOrderMappings();
		}	
		
		if (dbHelper == null && (hasLocalQuerySupport() || hasLocalDatabaseSupport())) {
			dbHelper = new OrderDbHelper(Envived.getContext(), this);
		}
		
		if (dbHelper != null) {
			dbHelper.init();
		}
	}
	
	@Override
	public void cleanup() {
		if (dbHelper != null) {
			dbHelper.close();
			dbHelper = null;
		}
	}
	
	private void buildOrderMappings() throws EnvSocialContentException {
		// the JSON encoded data is in the data field
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
					map.put(ITEM_NAME, item.getString(ITEM_NAME));
					map.put(ITEM_DESCRIPTION, item.optString("description", "No description available"));
					map.put(ITEM_PRICE, item.getString(ITEM_PRICE));
					
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
	public Cursor localQuery(String query) {
		if (dbHelper != null) {
			return dbHelper.searchQuery(query);
		}
		
		return null;
	}

	
}
