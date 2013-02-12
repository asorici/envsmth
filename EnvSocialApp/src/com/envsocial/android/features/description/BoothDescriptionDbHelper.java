package com.envsocial.android.features.description;

import java.util.Locale;

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
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.FeatureDbHelper;

public class BoothDescriptionDbHelper extends FeatureDbHelper {
	private static final long serialVersionUID = 1L;
	private static final String TAG = "BoothDescriptionDbHelper";
	
	protected static final String BOOTH_DESCRIPTION_TABLE = "booth_description";
	protected static final String COL_BOOTH_DESCRIPTION_ID = BaseColumns._ID;
	protected static final String COL_BOOTH_DESCRIPTION_DESCRIPTION = "booth_description";
	protected static final String COL_BOOTH_DESCRIPTION_TAGS = "booth_tags";
	protected static final String COL_BOOTH_DESCRIPTION_IMAGE_URL = "booth_image_url";
	protected static final String COL_BOOTH_DESCRIPTION_CONTACT_EMAIL = "booth_contact_email";
	protected static final String COL_BOOTH_DESCRIPTION_CONTACT_WEBSITE = "booth_contact_website";
	
	protected static final String BOOTH_PRODUCT_TABLE = "booth_product";
	protected static final String COL_BOOTH_PRODUCT_ID = BaseColumns._ID;
	protected static final String COL_BOOTH_PRODUCT_BOOTH_ID = "booth_product_booth_id";
	protected static final String COL_BOOTH_PRODUCT_NAME = "booth_product_name";
	protected static final String COL_BOOTH_PRODUCT_DESCRIPTION = "booth_product_description";
	protected static final String COL_BOOTH_PRODUCT_IMAGE_URL = "booth_product_image_url";
	protected static final String COL_BOOTH_PRODUCT_WEBSITE_URL = "booth_product_website_url";
	
	protected static final String BOOTH_DESCRIPTION_FTS_TABLE = "booth_description_fts";
	protected static final String COL_BOOTH_DESCRIPTION_FTS_ID = BaseColumns._ID;
	protected static final String COL_BOOTH_DESCRIPTION_FTS_DESCRIPTION = "booth_description";
	protected static final String COL_BOOTH_DESCRIPTION_FTS_TAGS = "booth_tags";
	
	protected static final String BOOTH_PRODUCT_FTS_TABLE = "booth_product_fts";
	protected static final String COL_BOOTH_PRODUCT_FTS_ID = BaseColumns._ID;
	protected static final String COL_BOOTH_PRODUCT_FTS_NAME = "booth_product_name";
	protected static final String COL_BOOTH_PRODUCT_FTS_DESCRIPTION = "booth_product_description";
	
	
	
	public BoothDescriptionDbHelper(Context context, String databaseName, Feature feature, int version) {
		super(context, databaseName, feature, version);
	}

	@Override
	protected void onDbCreate(SQLiteDatabase db) {
		Log.d(TAG, "[DEBUG] >> ----------- Database " + getDBName() + " is being created. ------------");
		
		db.execSQL("CREATE TABLE " + BOOTH_DESCRIPTION_TABLE + "(" + 
				COL_BOOTH_DESCRIPTION_ID + " INTEGER PRIMARY KEY, " + 
				COL_BOOTH_DESCRIPTION_DESCRIPTION + " TEXT, " + 
				COL_BOOTH_DESCRIPTION_TAGS + " TEXT, " + 
				COL_BOOTH_DESCRIPTION_IMAGE_URL + " TEXT, " +
				COL_BOOTH_DESCRIPTION_CONTACT_WEBSITE + " TEXT, " +
				COL_BOOTH_DESCRIPTION_CONTACT_EMAIL + " TEXT" +
				");");
		
		
		db.execSQL("CREATE TABLE " + BOOTH_PRODUCT_TABLE + 
				"(" + COL_BOOTH_PRODUCT_ID + " INTEGER PRIMARY KEY, " + 
				COL_BOOTH_PRODUCT_NAME + " TEXT, " +
				COL_BOOTH_PRODUCT_DESCRIPTION + " TEXT, " +
				COL_BOOTH_PRODUCT_IMAGE_URL + " TEXT, " +
				COL_BOOTH_PRODUCT_WEBSITE_URL + " TEXT, " +
				COL_BOOTH_PRODUCT_BOOTH_ID + " INTEGER NOT NULL, " + 
				"FOREIGN KEY (" + COL_BOOTH_PRODUCT_BOOTH_ID + ") REFERENCES " 
				+ BOOTH_DESCRIPTION_TABLE + "(" + COL_BOOTH_DESCRIPTION_ID + "));");
		
		// Add trigger to enforce foreign key constraints, as SQLite does not support them.
		// Checks that when inserting a new product, a booth with the specified booth_id exists.
		db.execSQL("CREATE TRIGGER fk_session_presentation_id " +
				" BEFORE INSERT " +
			    " ON "+ BOOTH_PRODUCT_TABLE +
			    " FOR EACH ROW BEGIN" +
			    " SELECT CASE WHEN ((SELECT " + COL_BOOTH_DESCRIPTION_ID + " FROM " + BOOTH_DESCRIPTION_TABLE + 
			    " WHERE "+ COL_BOOTH_DESCRIPTION_ID +"=new." + COL_BOOTH_PRODUCT_BOOTH_ID + " ) IS NULL)" +
			    " THEN RAISE (ABORT,'Foreign Key Violation') END;"+
			    "  END;");
		
		// create the full text search tables for booth descriptions and products
		db.execSQL("CREATE VIRTUAL TABLE " + BOOTH_DESCRIPTION_FTS_TABLE + " " + 
					"USING fts3(" + 
						COL_BOOTH_DESCRIPTION_FTS_ID + ", " + 
						COL_BOOTH_DESCRIPTION_FTS_DESCRIPTION + ", " + 
						COL_BOOTH_DESCRIPTION_FTS_TAGS +
					");");
		
		db.execSQL("CREATE VIRTUAL TABLE " + BOOTH_PRODUCT_FTS_TABLE + " " + 
				"USING fts3(" + 
					COL_BOOTH_PRODUCT_FTS_ID + ", " + 
					COL_BOOTH_PRODUCT_FTS_NAME + ", " + 
					COL_BOOTH_PRODUCT_FTS_DESCRIPTION +
				");");
	}

	@Override
	protected void onDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + BOOTH_DESCRIPTION_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + BOOTH_PRODUCT_TABLE);
		
		db.execSQL("DROP TABLE IF EXISTS " + BOOTH_DESCRIPTION_FTS_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + BOOTH_PRODUCT_FTS_TABLE);
	}

	@Override
	protected void onDbOpen(SQLiteDatabase db) {
		Log.d(TAG, "[DEBUG] >> ----------- Database " + getDBName() + " already created. Now opening ------------");
	}
	
	
	@Override
	public void init(boolean insert) throws EnvSocialContentException {
		// do initial description insertion here if new data available
		if (insert) {
			insertDescriptionData();
		}
	}
	
	@Override
	public void update() throws EnvSocialContentException {
		// since the update message does not yet specify individual entries do delete and insert
		// the update procedure is a simple DELETE TABLES followed by a new insertion of the program
		cleanupTables();
		
		// do update description insertion here
		insertDescriptionData();
	}
	

	private void cleanupTables() {
		database.delete(BOOTH_PRODUCT_TABLE, null, null);
		database.delete(BOOTH_DESCRIPTION_TABLE, null, null);
		
		database.delete(BOOTH_PRODUCT_FTS_TABLE, null, null);
		database.delete(BOOTH_DESCRIPTION_FTS_TABLE, null, null);
	}
	
	
	private void insertDescriptionData() throws EnvSocialContentException {
		// perform initial insertion of the program if and only if the database is created
		Log.d(TAG, "Inserting booth description");
			
		String descriptionJSON = feature.getSerializedData();
		
		try {
			// Parse description JSON
			JSONObject description = (JSONObject) new JSONObject(descriptionJSON);
			ContentValues values = new ContentValues();
			
			int boothId = description.getInt(BoothDescriptionFeature.BOOTH_DESCRIPTION_ID);
			String boothDescription = description.getString(BoothDescriptionFeature.BOOTH_DESCRIPTION_DESCRIPTION);
			
			String boothTags = null;
			JSONArray boothTagsArray = description.optJSONArray(BoothDescriptionFeature.BOOTH_DESCRIPTION_TAGS);
			if (boothTagsArray != null) {
				boothTags = boothTagsArray.join(";");
			}
			
			String boothImageUrl = description.optString(BoothDescriptionFeature.BOOTH_DESCRIPTION_IMAGE_URL, null);
			String boothContactEmail = description.optString(BoothDescriptionFeature.BOOTH_DESCRIPTION_CONTACT_EMAIL, null);
			String boothContactWebsite = description.optString(BoothDescriptionFeature.BOOTH_DESCRIPTION_CONTACT_WEBSITE, null);
			
			// ---- insert into booth_description table ---- //
			values.put(COL_BOOTH_DESCRIPTION_ID, boothId);
			values.put(COL_BOOTH_DESCRIPTION_DESCRIPTION, boothDescription);
			
			if (boothTags != null) {
				values.put(COL_BOOTH_DESCRIPTION_TAGS, boothTags);
			}
			
			if (boothImageUrl != null) {
				values.put(COL_BOOTH_DESCRIPTION_IMAGE_URL, boothImageUrl);
			}
			
			if (boothContactEmail != null) {
				values.put(COL_BOOTH_DESCRIPTION_CONTACT_EMAIL, boothContactEmail);
			}
			
			if (boothContactWebsite != null) {
				values.put(COL_BOOTH_DESCRIPTION_CONTACT_WEBSITE, boothContactWebsite);
			}
			
			database.insert(BOOTH_DESCRIPTION_TABLE, null, values);
			values.clear();
			
			// ---- insert into booth_description_fts table ---- //
			values.put(COL_BOOTH_DESCRIPTION_FTS_ID, String.valueOf(boothId));
			values.put(COL_BOOTH_DESCRIPTION_FTS_DESCRIPTION, boothDescription.toLowerCase(Locale.US));
			if (boothTags != null) {
				String boothTagsFlat = boothTags.replace(';', ' ').toLowerCase(Locale.US);
				values.put(COL_BOOTH_DESCRIPTION_FTS_TAGS, boothTagsFlat);
			}
			
			database.insert(BOOTH_DESCRIPTION_FTS_TABLE, null, values);
			values.clear();
			
			// insert product list
			JSONArray boothProductList = description.optJSONArray(BoothDescriptionFeature.BOOTH_DESCRIPTION_PRODUCTS);
			if (boothProductList != null) {
				try {
					database.beginTransaction();
					
					int len = boothProductList.length();
					for (int i = 0; i < len; i++) {
						
						// ---- insert product in booth_product table ---- //
						JSONObject boothProduct = boothProductList.getJSONObject(i);
						int productId = boothProduct.getInt(BoothDescriptionFeature.BOOTH_PRODUCT_ID);
						String productName = boothProduct.getString(BoothDescriptionFeature.BOOTH_PRODUCT_NAME);
						String productDescription = boothProduct.getString(BoothDescriptionFeature.BOOTH_PRODUCT_DESCRIPTION);
						String productImageUrl = boothProduct.optString(BoothDescriptionFeature.BOOTH_PRODUCT_IMAGE_URL, null);
						String productWebsiteUrl = boothProduct.optString(BoothDescriptionFeature.BOOTH_PRODUCT_WEBSITE_URL, null);
						
						values.put(COL_BOOTH_PRODUCT_ID, productId);
						values.put(COL_BOOTH_PRODUCT_BOOTH_ID, boothId);
						values.put(COL_BOOTH_PRODUCT_NAME, productName);
						values.put(COL_BOOTH_PRODUCT_DESCRIPTION, productDescription);
						
						if (productImageUrl != null) {
							values.put(COL_BOOTH_PRODUCT_IMAGE_URL, productImageUrl);
						}
						
						if (productWebsiteUrl != null) {
							values.put(COL_BOOTH_PRODUCT_WEBSITE_URL, productWebsiteUrl);
						}
						
						database.insert(BOOTH_PRODUCT_TABLE, null, values);
						values.clear();
						
						// ---- insert product in booth_product_fts table ---- //
						values.put(COL_BOOTH_PRODUCT_FTS_ID, String.valueOf(productId));
						values.put(COL_BOOTH_PRODUCT_FTS_NAME, productName);
						values.put(COL_BOOTH_PRODUCT_FTS_DESCRIPTION, productDescription);
						
						database.insert(BOOTH_PRODUCT_FTS_TABLE, null, values);
						values.clear();
					}
					
					database.setTransactionSuccessful();
				}
				finally {
					database.endTransaction();
				}
			}
			
		} catch (JSONException ex) {
			cleanupTables();
			throw new EnvSocialContentException(descriptionJSON, EnvSocialResource.FEATURE, ex);
		}
		
		dbStatus = DB_POPULATED;
	}
	
	public Cursor getBoothData() {
		Cursor c = database.query(BOOTH_DESCRIPTION_TABLE, null, null, null, null, null, null);
		
		return c;
	}
	
	public Cursor getAllProducts(int boothId) {
		Log.d(TAG, "Retrieving products for booth id: " + boothId);
		
		String selection = BOOTH_PRODUCT_TABLE + "." + COL_BOOTH_PRODUCT_BOOTH_ID + " = ?";
		String[] selectionArgs = new String[] {"" + boothId};
		String[] projection = new String[] { COL_BOOTH_PRODUCT_ID, COL_BOOTH_PRODUCT_NAME, COL_BOOTH_PRODUCT_DESCRIPTION };
		String orderBy = COL_BOOTH_PRODUCT_NAME;
		
		Cursor c = database.query(BOOTH_PRODUCT_TABLE, projection, selection, selectionArgs, null, null, orderBy);
		return c;
	}
	
	public Cursor getProductData(int productId) {
		String selection = BOOTH_PRODUCT_TABLE + "." + COL_BOOTH_PRODUCT_ID + " = ?";
		String[] selectionArgs = new String[] {"" + productId};
		
		Cursor c = database.query(BOOTH_PRODUCT_TABLE, null, selection, selectionArgs, null, null, null);
		return c;
	}
	
	
	public Cursor searchBoothQuery(String query) {
		String wildCardQuery = appendWildcard(query);
		String selection = BOOTH_DESCRIPTION_FTS_TABLE + " MATCH ?";
		String[] selectionArgs = new String [] { "'" + wildCardQuery + "'" };
		
		Log.d(TAG, "BOOTH DESCRIPTION SEARCH QUERY WHERE CLAUSE: " + selectionArgs);
		
		return database.query(true, BOOTH_DESCRIPTION_FTS_TABLE, null, 
				selection, selectionArgs, null, null, null, null);
	}
	
	
	public Cursor searchProductQuery(String query) {
		String wildCardQuery = appendWildcard(query);
		String selection = BOOTH_PRODUCT_FTS_TABLE + " MATCH ?";
		String[] selectionArgs = new String [] { "'" + wildCardQuery + "'" };
		String orderBy = COL_BOOTH_PRODUCT_FTS_NAME;
		
		Log.d(TAG, "BOOTH PRODUCT SEARCH QUERY WHERE CLAUSE: " + selectionArgs);
		
		return database.query(true, BOOTH_PRODUCT_FTS_TABLE, null, 
				selection, selectionArgs, null, null, orderBy, null);
	}
}
