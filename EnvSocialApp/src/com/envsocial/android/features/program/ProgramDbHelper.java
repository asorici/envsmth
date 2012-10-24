package com.envsocial.android.features.program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.envsocial.android.api.EnvSocialResource;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.utils.FeatureDbHelper;

public class ProgramDbHelper extends FeatureDbHelper {
	private static final long serialVersionUID = 1L;

	private static final String TAG = "ProgramDbHelper";
	
	public static final String DATABASE_NAME = "db_program";
	
	protected static final String ENTRY_TABLE = "entry";
	protected static final String COL_ENTRY_ID = "id";
	protected static final String COL_ENTRY_TITLE = "title";
	protected static final String COL_ENTRY_SESSIONID = "sessionId";
	protected static final String COL_ENTRY_SPEAKERS = "speakers";
	protected static final String COL_ENTRY_ABSTRACT = "abstract";
	protected static final String COL_ENTRY_START_TIME = "startTime";
	protected static final String COL_ENTRY_END_TIME = "endTime";
	
	protected static final String SESSION_TABLE = "session";
	protected static final String COL_SESSION_ID = "id";
	protected static final String COL_SESSION_TITLE = "title";
	protected static final String COL_SESSION_TAG = "tag";
	protected static final String COL_SESSION_LOCATION = "location";
	
	
	public ProgramDbHelper(Context context, ProgramFeature feature, int version) throws EnvSocialContentException {
		super(context, DATABASE_NAME, feature, version);
		database = this.getWritableDatabase();
		
		//insertProgram();
	}
	
	
	@Override
	public void onDbCreate(SQLiteDatabase db) {
		Log.d(TAG, "[DEBUG] >> ----------- Database " + DATABASE_NAME + " JUST NOW created. ------------");
		db.execSQL("CREATE TABLE " + SESSION_TABLE + "(" + COL_SESSION_ID + " INTEGER PRIMARY KEY, " + 
				COL_SESSION_TITLE + " TEXT, " + COL_SESSION_TAG + " TEXT, " + COL_SESSION_LOCATION + " TEXT);");
		
		db.execSQL("CREATE TABLE " + ENTRY_TABLE + "(" + COL_ENTRY_ID + " INTEGER PRIMARY KEY, " + 
				COL_ENTRY_TITLE + " TEXT, " + COL_ENTRY_SPEAKERS + " TEXT, " + COL_ENTRY_START_TIME + " TEXT, " +
				COL_ENTRY_END_TIME + " TEXT, " + COL_ENTRY_SESSIONID + " INTEGER NOT NULL, FOREIGN KEY (" + 
				COL_ENTRY_SESSIONID + ") REFERENCES " + SESSION_TABLE + "(" + COL_SESSION_ID + "));");
		
		// Add trigger to enforce foreign key constraints, as SQLite does not support them.
		// Checks that when inserting a new entry, a session with the specified sessionId exists.
		db.execSQL("CREATE TRIGGER fk_session_entryid " +
				" BEFORE INSERT "+
			    " ON "+ ENTRY_TABLE +
			    " FOR EACH ROW BEGIN" +
			    " SELECT CASE WHEN ((SELECT " + COL_SESSION_ID + " FROM " + SESSION_TABLE + 
			    " WHERE "+ COL_SESSION_ID +"=new." + COL_ENTRY_SESSIONID + " ) IS NULL)" +
			    " THEN RAISE (ABORT,'Foreign Key Violation') END;"+
			    "  END;");
		
		dbStatus = TABLES_CREATED;
	}
	
	
	@Override
	public void onDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + ENTRY_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + SESSION_TABLE);
		
		dbStatus = TABLES_INEXISTENT;
	}
	
	@Override
	public void onOpen(SQLiteDatabase db) {
		Log.d(TAG, "[DEBUG] >> ----------- Database " + DATABASE_NAME + " already created. ------------");
	}
	
	@Override
	public void init () throws EnvSocialContentException {
		// do initial program insertion here
		insertProgram();
	}
	
	@Override
	public void update () throws EnvSocialContentException {
		// since the update message does not yet specify individual entries do delete and insert
		// the update procedure is a simple DELETE TABLES followed by a new insertion of the program
		cleanupTables();
		
		// do update program insertion here
		insertProgram();
	}
	
	
	private void cleanupTables() {
		database.delete(ENTRY_TABLE, null, null);
		database.delete(SESSION_TABLE, null, null);
		
		dbStatus = TABLES_CREATED;
	}
	
	private void insertProgram() throws EnvSocialContentException {
		if (dbStatus == TABLES_CREATED) {
			// perform initial insertion of the program if and only if the database is created
			
			String programJSON = feature.getSerializedData();
			
			try {
				// Parse program's JSON
				JSONObject program = (JSONObject) new JSONObject(programJSON).getJSONObject("program");
				JSONArray sessionsArray = (JSONArray) program.getJSONArray("sessions");
				JSONArray entriesArray = (JSONArray) program.getJSONArray("entries");
				
				insertSessions(sessionsArray);
				insertEntries(entriesArray);
			} catch (JSONException ex) {
				cleanupTables();
				throw new EnvSocialContentException(programJSON, EnvSocialResource.FEATURE, ex);
			}
		
			dbStatus = TABLES_POPULATED;
		}
	}
	
	public void insertSessions(JSONArray sessionsArray) throws JSONException {
		Log.d(TAG, "[DEBUG] >> ----------- INSERTING SESSIONS ------------");
		
		ContentValues values = new ContentValues();
		int n = sessionsArray.length();
		for (int i = 0; i < n; ++ i) {
			JSONObject session = sessionsArray.getJSONObject(i);
			extractValues(session,
					values,
					new String[] { COL_SESSION_ID },
					new String[] { COL_SESSION_TITLE, COL_SESSION_TAG, COL_SESSION_LOCATION }
					);
			database.insert(SESSION_TABLE, COL_SESSION_ID, values);
		}
	}
	
	public void insertEntries(JSONArray entriesArray) throws JSONException {
		Log.d(TAG, "[DEBUG] >> ----------- INSERTING ENTRIES ------------");
		
		ContentValues values = new ContentValues();
		int n = entriesArray.length();
		for (int i = 0; i < n; ++ i) {
			JSONObject session = entriesArray.getJSONObject(i);
			extractValues(session,
					values,
					new String[] { COL_ENTRY_ID, COL_ENTRY_SESSIONID },
					new String[] { COL_ENTRY_TITLE, COL_ENTRY_SPEAKERS, COL_ENTRY_START_TIME, COL_ENTRY_END_TIME }
					);
			database.insert(ENTRY_TABLE, COL_ENTRY_ID, values);
		}
	}
	
	private ContentValues extractValues(JSONObject obj, ContentValues values, 
			String[] intColumns, String[] textColumns) throws JSONException {
		
		// Make sure values is empty, we reuse it
		values.clear();
		
		for (String col : intColumns) {
			values.put(col, obj.getInt(col));
		}
		
		for (String col : textColumns) {
			values.put(col, obj.getString(col));
		}
		
		return values;
	}
	
	public List<String> getDays() {
		List<String> days = new ArrayList<String>();
		
//		Cursor c = database.query(ENTRY_TABLE, new String[] {"SUBSTR(" + COL_ENTRY_START_TIME + ",1,10)"}, 
//				null, null, COL_ENTRY_START_TIME, null, null);
		
		String queryString = "SELECT DISTINCT SUBSTR(" + COL_ENTRY_START_TIME + ",1,10) as Day FROM " + ENTRY_TABLE + ";";
		Cursor c = database.rawQuery(queryString, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			days.add(c.getString(0));
			c.moveToNext();
		}
		c.close();
		
		return days;
	}
	 
	public Map<String,Map<String,String>> getAllSessions() {
		Map<String,Map<String,String>> sessions = new HashMap<String,Map<String,String>>();
		
		Cursor c = database.query(SESSION_TABLE, new String[] {COL_SESSION_ID, COL_SESSION_TITLE, 
				COL_SESSION_TAG, COL_SESSION_LOCATION}, null, null, null, null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			Map<String,String> ses = new HashMap<String,String>();
			ses.put(COL_SESSION_ID, c.getString(0));
			ses.put(COL_SESSION_TITLE, c.getString(1));
			ses.put(COL_SESSION_TAG, c.getString(2));
			ses.put(COL_SESSION_LOCATION, c.getString(3));
			sessions.put(c.getString(0), ses);
			c.moveToNext();
		}
		c.close();
		
		return sessions;
	}
	
	public List<Map<String,String>> getEntriesByDay(String day) {
		List<Map<String,String>> entries = new ArrayList<Map<String,String>>();
		
		String queryString = 	"SELECT " +
									"e1." + COL_ENTRY_ID + ", " + 
									"e1." + COL_ENTRY_SESSIONID + ", " +
									"e1." + COL_ENTRY_TITLE + ", " +
									"e1." + COL_ENTRY_SPEAKERS + ", " + 
									"e1." + COL_ENTRY_START_TIME + ", " +
									"e1." + COL_ENTRY_END_TIME + " " + 
								"FROM entry e1 " +
								"WHERE " + 
									"SUBSTR(e1." + COL_ENTRY_START_TIME + ",1,10) = '" + day + "' " + 
									"AND e1." + COL_ENTRY_SESSIONID + " = " +
													"(SELECT MIN(e2." + COL_ENTRY_SESSIONID + ") " +
														"FROM entry e2 " +
														"WHERE " +
														  "SUBSTR(e2." + COL_ENTRY_START_TIME + ",1,10) = '" + day + "' " +
														  "AND e2." + COL_ENTRY_START_TIME + " <= e1." + COL_ENTRY_START_TIME + " " +
														  "AND e2." + COL_ENTRY_END_TIME + " > e1." + COL_ENTRY_START_TIME + 
													");";
		
		Cursor c = database.rawQuery(queryString, null);
		c.moveToFirst();
		
		while (!c.isAfterLast()) {
			Map<String,String> entry = new HashMap<String,String>();
			entry.put(COL_ENTRY_ID, c.getString(0));
			entry.put(COL_ENTRY_SESSIONID, c.getString(1));
			entry.put(COL_ENTRY_TITLE, c.getString(2));
			entry.put(COL_ENTRY_SPEAKERS, c.getString(3));
			entry.put(COL_ENTRY_START_TIME, c.getString(4));
			entry.put(COL_ENTRY_END_TIME, c.getString(5));
			entries.add(entry);
			
			c.moveToNext();
		}
		c.close();
		
		return entries;
	}
	
	public List<Map<String,String>> getEntriesByDay(String day, String sessionId) {
		List<Map<String,String>> entries = new ArrayList<Map<String,String>>();
		
		String queryString = 	"SELECT " +
									"e1." + COL_ENTRY_ID + ", " + 
									"e1." + COL_ENTRY_SESSIONID + ", " +
									"e1." + COL_ENTRY_TITLE + ", " +
									"e1." + COL_ENTRY_SPEAKERS + ", " + 
									"e1." + COL_ENTRY_START_TIME + ", " +
									"e1." + COL_ENTRY_END_TIME + " " + 
								"FROM entry e1 " +
								"WHERE " + 
									"SUBSTR(e1." + COL_ENTRY_START_TIME + ",1,10) = '" + day + "' " + 
									"AND e1." + COL_ENTRY_SESSIONID + " = '" + sessionId + "';";
		
		Cursor c = database.rawQuery(queryString, null);
		c.moveToFirst();
		
		while (!c.isAfterLast()) {
			Map<String,String> entry = new HashMap<String,String>();
			entry.put(COL_ENTRY_ID, c.getString(0));
			entry.put(COL_ENTRY_SESSIONID, c.getString(1));
			entry.put(COL_ENTRY_TITLE, c.getString(2));
			entry.put(COL_ENTRY_SPEAKERS, c.getString(3));
			entry.put(COL_ENTRY_START_TIME, c.getString(4));
			entry.put(COL_ENTRY_END_TIME, c.getString(5));
			entries.add(entry);
			
			c.moveToNext();
		}
		c.close();
		
		return entries;
	}
	
	public List<Map<String,String>> getOverlappingEntries(String entryId) {
		List<Map<String,String>> entries = new ArrayList<Map<String,String>>();
		
		String queryString = 	"SELECT " +
				"e1." + COL_ENTRY_ID + ", " + 
				"e1." + COL_ENTRY_SESSIONID + ", " +
				"e1." + COL_ENTRY_TITLE + ", " +
				"e1." + COL_ENTRY_SPEAKERS + ", " + 
				"e1." + COL_ENTRY_START_TIME + ", " +
				"e1." + COL_ENTRY_END_TIME + " " + 
			"FROM entry e1, " +
				 "(SELECT " + COL_ENTRY_ID + ", " + COL_ENTRY_SESSIONID + ", " + 
				 				COL_ENTRY_START_TIME + ", " + COL_ENTRY_END_TIME + " " +
				    "FROM entry WHERE " + COL_ENTRY_ID + " = '" + entryId + 
				    "' ORDER BY " + COL_ENTRY_SESSIONID + " ASC) AS e2" + " " +
			"WHERE " + 
				"e1." + COL_ENTRY_ID + " <> e2." + COL_ENTRY_ID + " " +
//				"AND SUBSTR(e1." + COL_ENTRY_START_TIME + ",1,10) = SUBSTR(e2." + COL_ENTRY_START_TIME + ",1,10) " +
				"AND e1." + COL_ENTRY_START_TIME + " <= e2." + COL_ENTRY_START_TIME + " " +
				"AND e1." + COL_ENTRY_END_TIME + " > e2." + COL_ENTRY_START_TIME + ";";
				

		Cursor c = database.rawQuery(queryString, null);
		c.moveToFirst();
		
		while (!c.isAfterLast()) {
			Map<String, String> entry = new HashMap<String, String>();
			entry.put(COL_ENTRY_ID, c.getString(0));
			entry.put(COL_ENTRY_SESSIONID, c.getString(1));
			entry.put(COL_ENTRY_TITLE, c.getString(2));
			entry.put(COL_ENTRY_SPEAKERS, c.getString(3));
			entry.put(COL_ENTRY_START_TIME, c.getString(4));
			entry.put(COL_ENTRY_END_TIME, c.getString(5));
			entries.add(entry);

			c.moveToNext();
		}
		c.close();
		
		return entries;
	}
	
	public Cursor getAllEntries() {
		Cursor c = database.query(ENTRY_TABLE, null, null, null, null, null, null);
		return c;
	}

	
	/**
	 * Searches the FTS Program table for item and category names matching the query string 
	 * @param query
	 */
	public Cursor searchQuery(String query) {
		/*
		String orderBy = COL_ORDER_FTS_CATEGORY + ", " + COL_ORDER_FTS_ITEM;
		
		String wildCardQuery = appendWildcard(query);
		String selection = MENU_ORDER_TABLE_FTS + " MATCH ?";
		String[] selectionArgs = new String[] {
				"'" + COL_ORDER_FTS_ITEM + ":\"" + wildCardQuery + "\"" + " OR "
					+ COL_ORDER_FTS_CATEGORY 	+ ":\"" + wildCardQuery + "\"" + "'" };
		
		Log.d("OrderDbHelper", "SEARCH QUERY WHERE CLAUSE: " + selectionArgs);
		
		return database.query(true, MENU_ORDER_TABLE_FTS, searchableColumns, 
				selection, selectionArgs, null, null, orderBy, null);
		*/
		
		return null;
	}
}
