package com.envsocial.android.features.order;

import java.util.Calendar;

import android.content.Context;
import android.database.Cursor;

import com.envsocial.android.Envived;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.EnvivedNotificationDispatcher;
import com.envsocial.android.utils.EnvivedNotificationHandler;
import com.envsocial.android.utils.FeatureDbHelper;

public class OrderFeature extends Feature {
	private static final long serialVersionUID = 1L;
	private static final String TAG = "OrderFeature";
	
	private EnvivedNotificationHandler notificationHandler;
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
	
	
	public OrderFeature(String category, int version, Calendar timestamp, String resourceUri,
			String environmentUri, String areaUri, String data, boolean virtualAccess) throws EnvSocialContentException {
		
		super(category, version, timestamp, resourceUri, environmentUri, areaUri, data, virtualAccess);
	}

	
	@Override
	protected void featureInit() throws EnvSocialContentException {
		// register order notification handler
		notificationHandler = new OrderFeatureNotificationHandler();
		EnvivedNotificationDispatcher.registerNotificationHandler(notificationHandler);

		// instantiate local database
		String databaseName = getLocalCacheFileName(category, 
				environmentUrl, areaUrl, version);

		if (dbHelper == null) {
			dbHelper = new OrderDbHelper(Envived.getContext(), databaseName, this, version);
		}
		
		// insert data if it was newly retrieved
		if (dbHelper != null && retrievedData != null) {
			dbHelper.init();
		}
	}

	@Override
	protected void featureUpdate() throws EnvSocialContentException {
		String databaseName = getLocalCacheFileName(category, 
				environmentUrl, areaUrl, version);

		if (dbHelper == null) {
			dbHelper = new OrderDbHelper(Envived.getContext(), databaseName, this, version);
		}
		
		dbHelper.update();		
	}

	@Override
	protected void featureCleanup(Context context) {
		if (dbHelper != null) {
			dbHelper.close();
			dbHelper = null;
		}
	}

	@Override
	protected void featureClose(Context context) {
		
		// first do cleanup
		featureCleanup(context);
		
		// unregister notification handler
		EnvivedNotificationDispatcher.unregisterNotificationHandler(notificationHandler);
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
	public FeatureDbHelper getLocalDatabaseSupport() {
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
