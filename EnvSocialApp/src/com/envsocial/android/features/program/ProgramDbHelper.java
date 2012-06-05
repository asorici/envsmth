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
import android.database.sqlite.SQLiteOpenHelper;

public class ProgramDbHelper extends SQLiteOpenHelper {

	public static final String DB_NAME = "db_program";
	public static final String ENTRY_TABLE = "entry";
	public static final String COL_ENTRY_ID = "id";
	public static final String COL_ENTRY_TITLE = "title";
	public static final String COL_ENTRY_SESSIONID = "sessionId";
	public static final String COL_ENTRY_SPEAKERS = "speakers";
	public static final String COL_ENTRY_START_TIME = "startTime";
	public static final String COL_ENTRY_END_TIME = "endTime";
	
	public static final String SESSION_TABLE = "session";
	public static final String COL_SESSION_ID = "id";
	public static final String COL_SESSION_TITLE = "title";
	public static final String COL_SESSION_TAG = "tag";
	public static final String COL_SESSION_LOCATION = "location";
	
	private static final int DATABASE_VERSION = 1;
	
	private SQLiteDatabase database;
	
	
	public ProgramDbHelper(Context context) {
		super(context, null, null, DATABASE_VERSION);
		database = this.getWritableDatabase();
	}
	
	public void close() {
		database.close();
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
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
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP IF TABLE EXISTS " + ENTRY_TABLE);
		db.execSQL("DROP IF TABLE EXISTS " + SESSION_TABLE);
	}
	
	public void insertSessions(JSONArray sessionsArray) throws JSONException {
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
		
		Cursor c = database.query(ENTRY_TABLE, new String[] {"SUBSTR(" + COL_ENTRY_START_TIME + ",1,10)"}, 
				null, null, COL_ENTRY_START_TIME, null, null);
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
		System.out.println("[DEBUG] >> #sessions: " + c.getCount());
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
		
		/*
		Cursor c = database.rawQuery("SELECT e1.id, e1.sessionId, e1.title, e1.speakers, e1.startTime, e1.endTime FROM entry e1" + 
				" WHERE SUBSTR(e1.startTime,1,10) = '" + day + "' AND e1.sessionId = (SELECT MIN(e2.sessionId) FROM entry e2 WHERE SUBSTR(e1.startTime,1,10) = '" + 
				day + "' AND e2.startTime <= e1.startTime AND e2.endTime > e1.startTime);", 
				null
				);
		*/
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
		
		/*
		System.out.println("[DEBUG] >> #entries: " + c.getCount());
		c.moveToFirst();
		while (!c.isAfterLast()) {
			System.out.println("[DEBUG] >> Select entry: " + c.getString(0) + " " + c.getString(2));
			c.moveToNext();
		}
		c.close();
		*/
		
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
		
		/*
		System.out.println("[DEBUG] >> #entries: " + c.getCount());
		c.moveToFirst();
		while (!c.isAfterLast()) {
			System.out.println("[DEBUG] >> Select entry: " + c.getString(0) + " " + c.getString(2));
			c.moveToNext();
		}
		c.close();
		*/
		
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
				 "(SELECT " + COL_ENTRY_START_TIME + ", " + COL_ENTRY_END_TIME + " " +
				    "FROM entry WHERE " + COL_ENTRY_ID + " = '" + entryId + "') AS e2" + " " +
			"WHERE " + 
				"e1." + COL_ENTRY_START_TIME + " <= e2." + COL_ENTRY_START_TIME + " " +
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

		/*
		 * System.out.println("[DEBUG] >> #entries: " + c.getCount());
		 * c.moveToFirst(); while (!c.isAfterLast()) {
		 * System.out.println("[DEBUG] >> Select entry: " + c.getString(0) + " "
		 * + c.getString(2)); c.moveToNext(); } c.close();
		 */

		return entries;
	}
	
	public Map<String,String> getOverlappingEntries(String entryId) {
		// TODO
		return null;
	}
	
	public Cursor getAllEntries() {
		Cursor c = database.query(ENTRY_TABLE, null, null, null, null, null, null);
		return c;
	}

}
