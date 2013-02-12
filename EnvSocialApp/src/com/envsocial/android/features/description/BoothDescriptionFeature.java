package com.envsocial.android.features.description;

import java.util.Calendar;

import android.content.Context;
import android.database.Cursor;

import com.envsocial.android.Envived;
import com.envsocial.android.R;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.FeatureDbHelper;

public class BoothDescriptionFeature extends Feature {
	private static final long serialVersionUID = 1L;
	private static final String TAG = "ProgramFeature";
	
	private transient BoothDescriptionDbHelper dbHelper;
	
	public static final String BOOTH_DESCRIPTION = "booth_description";
	public static final String BOOTH_DESCRIPTION_ID = "id";
	public static final String BOOTH_DESCRIPTION_DESCRIPTION = "description";
	public static final String BOOTH_DESCRIPTION_TAGS = "tags";
	public static final String BOOTH_DESCRIPTION_IMAGE_URL = "image_url";
	public static final String BOOTH_DESCRIPTION_CONTACT_EMAIL = "contact_email";
	public static final String BOOTH_DESCRIPTION_CONTACT_WEBSITE = "contact_website";
	public static final String BOOTH_DESCRIPTION_PRODUCTS = "products";
	
	public static final String BOOTH_PRODUCT = "booth_product";
	public static final String BOOTH_PRODUCT_ID = "product_id";
	public static final String BOOTH_PRODUCT_NAME = "product_name";
	public static final String BOOTH_PRODUCT_DESCRIPTION = "product_description";
	public static final String BOOTH_PRODUCT_IMAGE_URL = "product_image_url";
	public static final String BOOTH_PRODUCT_WEBSITE_URL = "product_website_url";
	
	
	public BoothDescriptionFeature(String category, int version, Calendar timestamp, 
			String resourceUrl, String environmentUrl,
			String areaUrl, String data, boolean virtualAccess) {
		
		super(category, version, timestamp, resourceUrl, environmentUrl, areaUrl, data, virtualAccess);
		
	}

	@Override
	protected void featureInit(boolean insert) throws EnvSocialContentException {
		// instantiate local database
		String databaseName = getLocalCacheFileName(category, environmentUrl, areaUrl, version);
				
		if (dbHelper == null) {
			dbHelper = new BoothDescriptionDbHelper(Envived.getContext(), databaseName, this, version);
		}
				
		if (dbHelper != null) {
			dbHelper.init(insert);
		}
	}

	@Override
	protected void featureUpdate() throws EnvSocialContentException {
		// instantiate local database
		String databaseName = getLocalCacheFileName(category, environmentUrl,
				areaUrl, version);

		if (dbHelper == null) {
			dbHelper = new BoothDescriptionDbHelper(Envived.getContext(), databaseName, this, version);
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
	}

	@Override
	public boolean hasLocalQuerySupport() {
		return true;
	}

	@Override
	public int getDisplayThumbnail() {
		return R.drawable.details_icon_description_white;
	}

	@Override
	public String getDisplayName() {
		return "Description";
	}

	@Override
	public FeatureDbHelper getLocalDatabaseSupport() {
		return dbHelper;
	}
	
	
	public Cursor getBoothData() {
		if (dbHelper != null) {
			return dbHelper.getBoothData();
		}
		
		return null;
	}
	
	
	public Cursor getAllProducts(int boothId) {
		if (dbHelper != null) {
			return dbHelper.getAllProducts(boothId);
		}
		
		return null;
	}
	
	
	public Cursor getProductData(int productId) {
		if (dbHelper != null) {
			return dbHelper.getAllProducts(productId);
		}
		
		return null;
	}
	
	
	public Cursor localBoothSearchQuery(String query) {
		if (dbHelper != null) {
			return dbHelper.searchBoothQuery(query);
		}
		
		return null;
	}
	
	
	public Cursor localProductSearchQuery(String query) {
		if (dbHelper != null) {
			return dbHelper.searchProductQuery(query);
		}
		
		return null;
	}
}
