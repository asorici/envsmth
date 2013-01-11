package com.envsocial.android.features.order;

<<<<<<< HEAD
import java.util.List;
import java.util.Map;

=======
>>>>>>> master
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;

import com.envsocial.android.Envived;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;

public class OrderFeature extends Feature {
	private static final long serialVersionUID = 1L;
	private static final String TAG = "OrderFeature";
	
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
	
	public static final String REQUEST_TYPE = "order_request_type";
	public static final String NEW_REQUEST_NOTIFICATION = "new_request";
	public static final String RESOLVED_REQUEST_NOTIFICATION = "resolved_request";
	
	public static final String NEW_ORDER_NOTIFICATION = "new_order";
	public static final String CALL_WAITER_NOTIFICATION = "call_waiter";
	public static final String CALL_CHECK_NOTIFICATION = "call_check";
	public static final String UPDATE_CONTENT_NOTIFICATION = "update_content";
	public static final String UPDATE_STRUCTURE_NOTIFICATION = "update_structure";
	
	public OrderFeature(String category, int version, String resourceUri,
			String environmentUri, String areaUri, String data) throws EnvSocialContentException {
		
		super(category, version, resourceUri, environmentUri, areaUri, data);
	}

	@Override
	public void init() throws EnvSocialContentException {
		super.init();
<<<<<<< HEAD
=======
				
		String databaseName = getLocalDatabaseName(OrderDbHelper.DATABASE_PREFIX, 
								environmentUri, areaUri, version);
>>>>>>> master
		
		if (dbHelper == null) {
			dbHelper = new OrderDbHelper(Envived.getContext(), databaseName, this, version);
		}
		
		if (dbHelper != null) {
			dbHelper.init();
		}
	}
	
	
	@Override
	public void doUpdate() throws EnvSocialContentException {
		super.doUpdate();
		
<<<<<<< HEAD
=======
		String databaseName = getLocalDatabaseName(OrderDbHelper.DATABASE_PREFIX, 
				environmentUri, areaUri, version);
	
>>>>>>> master
		if (dbHelper == null) {
			dbHelper = new OrderDbHelper(Envived.getContext(), databaseName, this, version);
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
		String databaseName = dbHelper.getDatabaseName();
		super.doClose(context);
		
		// first do cleanup
		doCleanup(context);
		
		// then remove the database file entirely
<<<<<<< HEAD
		context.deleteDatabase(OrderDbHelper.DATABASE_NAME);
	}

=======
		context.deleteDatabase(databaseName);
	}
	
>>>>>>> master
	
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
	
	
	public Cursor getOrderItemDetailCursor(int itemId) {
		if (dbHelper != null) {
			return dbHelper.getItemDetailCursor(itemId);
		}
		
		return null;
	}
}
