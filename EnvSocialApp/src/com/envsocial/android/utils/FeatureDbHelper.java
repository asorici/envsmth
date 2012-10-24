package com.envsocial.android.utils;

import java.io.Serializable;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;

public abstract class FeatureDbHelper extends SQLiteOpenHelper implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final String TAG = "FeatureDbHelper";
	
	protected Feature feature;
	protected String databaseName;
	
	protected transient SQLiteDatabase database;
	
	protected static final int TABLES_INEXISTENT = 0;
	protected static final int TABLES_CREATED = 1;
	protected static final int TABLES_POPULATED = 2;
	
	protected int dbStatus = TABLES_INEXISTENT;
	
	public FeatureDbHelper(Context context, String databaseName, Feature feature, int version) {
		super(context, databaseName, null, version);
		
		this.feature = feature;
		this.databaseName = databaseName;
	}
	
	public SQLiteDatabase getDatabase() {
		return database;
	}
	
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		onDbCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onDbUpgrade(db, oldVersion, newVersion);
	}
	
	@Override
	public void close() {
		Log.i(TAG, "---------------- Close called on FeatureDbHelper. -----------------");
		database.close();
	}
	
	/**
	 * Allows the initialization of the database tables typically by
	 * performing the initial insertion of the serialized data from the feature.
	 * <br/>
	 * Default method does nothing.
	 * 
	 * @throws {@link EnvSocialContentException} if the parsing of the serialized data
	 * from the feature is unsuccessful.
	 */
	public void init() throws EnvSocialContentException {
		
	}
	
	/**
	 * Allows the update of the database tables typically by
	 * doing an insertion of the serialized data from an updated feature.
	 * <br/>
	 * Default method does nothing.
	 * 
	 * @throws {@link EnvSocialContentException} if the parsing of the serialized data
	 * from the feature is unsuccessful.
	 */
	public void update() throws EnvSocialContentException {
		
	}
	
	public Feature getFeature() {
		return feature;
	}
	
	protected abstract void onDbCreate(SQLiteDatabase db);
	
	protected abstract void onDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
	
	/**
	 * Appends * at the end of each word in the query.
	 * @param query - search query to be typically used against an FTS table
	 * @return
	 */
	protected String appendWildcard(String query) {
        if (TextUtils.isEmpty(query)) return query;
 
        final StringBuilder builder = new StringBuilder();
        final String[] splits = TextUtils.split(query, " ");
 
        for (String split : splits)
          builder.append(split).append("*").append(" ");
 
        return builder.toString().trim();
    }
}
