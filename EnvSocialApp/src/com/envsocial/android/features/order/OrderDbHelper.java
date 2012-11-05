package com.envsocial.android.features.order;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

import com.envsocial.android.api.EnvSocialResource;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.utils.FeatureDbHelper;

public class OrderDbHelper extends FeatureDbHelper {
	private static final long serialVersionUID = 1L;
	
	private static final String TAG = "OrderDbHelper";
	
	public static final String DATABASE_NAME = "db_order";
	
	protected static final String MENU_CATEGORY_TABLE = "category";
	protected static final String COL_CATEGORY_ID = BaseColumns._ID;
	protected static final String COL_CATEGORY_NAME = "name";
	protected static final String COL_CATEGORY_TYPE = "type";
	
	protected static final String MENU_ITEM_TABLE = "item";
	protected static final String COL_ITEM_ID = BaseColumns._ID;
	protected static final String COL_ITEM_CATEGORY_ID = "category_id";
	protected static final String COL_ITEM_NAME = "name";
	protected static final String COL_ITEM_DESCRIPTION = "description";
	protected static final String COL_ITEM_PRICE = "price";
	protected static final String COL_ITEM_USAGE_RANK = "usage_rank";
	
	protected static final String MENU_ORDER_TABLE_FTS = "item_category_fts";
	protected static final String COL_ORDER_FTS_ID = BaseColumns._ID;
	protected static final String COL_ORDER_FTS_ITEM = "item";
	protected static final String COL_ORDER_FTS_CATEGORY = "category";
	protected static final String COL_ORDER_FTS_PRICE = "price";
	protected static final String COL_ORDER_FTS_DESCRIPTION = "description";
	
	private static String[] searchableColumns = {COL_ORDER_FTS_ID, COL_ORDER_FTS_ITEM, 
												 COL_ORDER_FTS_CATEGORY, COL_ORDER_FTS_PRICE, 
												 COL_ORDER_FTS_DESCRIPTION };
	
	
	public OrderDbHelper(Context context, OrderFeature orderFeature, int version) throws EnvSocialContentException {
		super(context, DATABASE_NAME, orderFeature, version);
		
		this.database = this.getWritableDatabase();
	}
	

	@Override
	public void onDbCreate(SQLiteDatabase db) {
		Log.d(TAG, "[DEBUG] >> ----------- Database " + DATABASE_NAME + " is being created. ------------");
		
		// create menu category table
		db.execSQL("CREATE TABLE " + MENU_CATEGORY_TABLE + "(" + 
				COL_CATEGORY_ID + " INTEGER PRIMARY KEY, " + 
				COL_CATEGORY_NAME + " TEXT, " + COL_CATEGORY_TYPE + " TEXT);");
		
		// create menu item table
		db.execSQL("CREATE TABLE " + MENU_ITEM_TABLE + "(" + 
				COL_ITEM_ID + " INTEGER PRIMARY KEY, " + 
				COL_ITEM_NAME + " TEXT, " + COL_ITEM_DESCRIPTION + " TEXT, " + COL_ITEM_PRICE + " DOUBLE, " +
				COL_ITEM_USAGE_RANK + " INTEGER DEFAULT 0, " +
				COL_ITEM_CATEGORY_ID + " INTEGER NOT NULL, FOREIGN KEY (" + 
				COL_ITEM_CATEGORY_ID + ") REFERENCES " + MENU_CATEGORY_TABLE + "(" + COL_CATEGORY_ID + "));");
		
		// Add trigger to enforce foreign key constraints, as SQLite does not support them.
		// Checks that when inserting a new item, a category with the specified category_id exists.
		db.execSQL("CREATE TRIGGER fk_session_entryid " +
				" BEFORE INSERT "+
			    " ON "+ MENU_ITEM_TABLE +
			    " FOR EACH ROW BEGIN" +
			    " SELECT CASE WHEN ((SELECT " + COL_CATEGORY_ID + " FROM " + MENU_CATEGORY_TABLE + 
			    " WHERE "+ COL_CATEGORY_ID +"=new." + COL_ITEM_CATEGORY_ID + " ) IS NULL)" +
			    " THEN RAISE (ABORT,'Foreign Key Violation') END;"+
			    "  END;");
		
		// create FTS order table
		db.execSQL("CREATE VIRTUAL TABLE " + MENU_ORDER_TABLE_FTS + " " + "USING fts3(" + COL_ORDER_FTS_ID + 
				", " + COL_ORDER_FTS_ITEM + 
				", " + COL_ORDER_FTS_CATEGORY +
				", " + COL_ORDER_FTS_PRICE +
				", " + COL_ORDER_FTS_DESCRIPTION + " " +");");
		
		dbStatus = TABLES_CREATED;
	}

	@Override
	public void onDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + MENU_CATEGORY_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + MENU_ITEM_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + MENU_ORDER_TABLE_FTS);
		dbStatus = TABLES_INEXISTENT;
		
		onCreate(db);
	}
	
	
	@Override
	public void onOpen(SQLiteDatabase db) {
		Log.d(TAG, "[DEBUG] >> ----------- Database " + DATABASE_NAME + " is being opened. ------------");
	}
	
	@Override
	public void init() throws EnvSocialContentException {
		// calling insertMenu here
		Log.d(TAG, "[DEBUG] >> ----------- Init " + DATABASE_NAME + ". ------------");
		insertMenu();
	}
	
	@Override
	public void update() throws EnvSocialContentException {
		// since the update message does not yet specify individual entries do delete and insert
		// the update procedure is a simple DELETE TABLES followed by a new insertion of the program
		cleanupTables();
		
		// do update menu insertion here
		insertMenu();
	}
	
	public void insertMenu() throws EnvSocialContentException {
		if (dbStatus == TABLES_CREATED) {
			Log.d(TAG, "[DEBUG] >> ----------- INSERTING MENU DATA ------------");
			
			// the JSON encoded data is in the data field
			String encodedJsonData = feature.getSerializedData();
			try {
				// Grab menu
				JSONArray orderMenu = (JSONArray) new JSONObject(encodedJsonData).getJSONArray("order_menu");
				
				// define a values container to be used on insertion
				ContentValues values = new ContentValues();
				
				// Parse categories
				int nCategories = orderMenu.length();
				for (int i = 0; i < nCategories; ++ i) {
					JSONObject elem = orderMenu.getJSONObject(i);
	
					// Extract category data and insert it
					JSONObject categoryObject = elem.getJSONObject(OrderFeature.CATEGORY);
					
					int categoryId = categoryObject.getInt(OrderFeature.CATEGORY_ID);
					String categoryName = categoryObject.getString(OrderFeature.CATEGORY_NAME);
					String categoryType = categoryObject.getString(OrderFeature.CATEGORY_TYPE);
					
					// fill values container
					values.put(COL_CATEGORY_ID, categoryId);
					values.put(COL_CATEGORY_NAME, categoryName);
					values.put(COL_CATEGORY_TYPE, categoryType);
					
					database.insert(MENU_CATEGORY_TABLE, COL_CATEGORY_ID, values);
					values.clear();
	
					// insert items for current category
					JSONArray itemsArray = elem.getJSONArray("items");
					
					try {
						database.beginTransaction();
						
						int nItems = itemsArray.length();
						for (int j = 0; j < nItems; ++ j) {
							// extract item data to map
							JSONObject item = itemsArray.getJSONObject(j);
		
							int itemId = item.getInt(OrderFeature.ITEM_ID);
							int itemCategoryId = item.getInt(OrderFeature.ITEM_CATEGORY_ID);
							String itemName = item.getString(OrderFeature.ITEM_NAME);
							String itemDescription = item.optString(OrderFeature.ITEM_DESCRIPTION, 
									"No description available");
							//String itemPrice =  item.getString(OrderFeature.ITEM_PRICE);
							double itemPrice = item.getDouble(OrderFeature.ITEM_PRICE);
							int itemUsageRank = item.getInt(OrderFeature.ITEM_USAGE_RANK);
							
							// insert in the item table
							values.put(COL_ITEM_ID, itemId);
							values.put(COL_ITEM_CATEGORY_ID, itemCategoryId);
							values.put(COL_ITEM_NAME, itemName);
							values.put(COL_ITEM_DESCRIPTION, itemDescription);
							values.put(COL_ITEM_PRICE, itemPrice);
							values.put(COL_ITEM_USAGE_RANK, itemUsageRank);
							
							database.insert(MENU_ITEM_TABLE, COL_ITEM_ID, values);
							values.clear();
							
							// insert in FST table
							values.put(COL_ORDER_FTS_ID, "" + itemId);
							values.put(COL_ORDER_FTS_ITEM, itemName);
							values.put(COL_ORDER_FTS_CATEGORY, categoryName);
							values.put(COL_ORDER_FTS_PRICE, "" + itemPrice);
							values.put(COL_ORDER_FTS_DESCRIPTION, itemDescription);
							
							database.insert(MENU_ORDER_TABLE_FTS, COL_ORDER_FTS_ID, values);
							values.clear();
							
						}
						
						database.setTransactionSuccessful();
					} finally {
						database.endTransaction();
					}
					
				}
			} catch (JSONException e) {
				cleanupTables();
				e.printStackTrace();
				throw new EnvSocialContentException(encodedJsonData, EnvSocialResource.FEATURE, e);
			}
			
			dbStatus = TABLES_POPULATED;
		}
	}

	private void cleanupTables() {
		database.delete(MENU_CATEGORY_TABLE, null, null);
		database.delete(MENU_ITEM_TABLE, null, null);
		database.delete(MENU_ORDER_TABLE_FTS, null, null);
		
		dbStatus = TABLES_CREATED;
	}
	
	/**
	 * Searches the FTS Order table for item and category names matching the query string 
	 * @param query
	 */
	public Cursor searchQuery(String query) {
		String orderBy = COL_ORDER_FTS_CATEGORY + ", " + COL_ORDER_FTS_ITEM;
		
		String wildCardQuery = appendWildcard(query);
		String selection = MENU_ORDER_TABLE_FTS + " MATCH ?";
		
		//Log.d(TAG,"SEARCH WHERE CLAUSE " + "'" + COL_ORDER_FTS_ITEM + ":\"" + wildCardQuery + "\"" + " OR "
		//+ COL_ORDER_FTS_CATEGORY 	+ ":\"" + wildCardQuery + "\"" + "'");
		
		/*
		String[] selectionArgs = new String[] {
				COL_ORDER_FTS_ITEM + ":\"" + wildCardQuery + "\"" + " OR "
					+ COL_ORDER_FTS_CATEGORY 	+ ":\"" + wildCardQuery + "\"" };
		*/
		String[] selectionArgs = new String [] { "'" + wildCardQuery + "'" };
		
		return database.query(MENU_ORDER_TABLE_FTS, searchableColumns, 
				selection, selectionArgs, null, null, orderBy);
	}


	public Cursor getCategoryCursor(String type) {
		String orderBy = COL_CATEGORY_NAME;
		String selection = COL_CATEGORY_TYPE + " = ?" ;
		String[] selectionArgs = new String[] {type};
		
		return database.query(MENU_CATEGORY_TABLE, null, selection, selectionArgs, null, null, orderBy);
	}

	public Cursor getItemCursor(int categoryId) {
		String orderBy = COL_ITEM_USAGE_RANK + " DESC" + ", " + COL_ITEM_NAME;
		String selection = COL_ITEM_CATEGORY_ID + " = ?";
		String[] selectionArgs = new String[] {"" + categoryId};
		
		return database.query(MENU_ITEM_TABLE, null, selection, selectionArgs, null, null, orderBy);
	}

}
