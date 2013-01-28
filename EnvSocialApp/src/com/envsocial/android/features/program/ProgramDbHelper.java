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
import android.provider.BaseColumns;
import android.util.Log;

import com.envsocial.android.api.EnvSocialResource;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.utils.FeatureDbHelper;

public class ProgramDbHelper extends FeatureDbHelper {
	private static final long serialVersionUID = 1L;

	private static final String TAG = "ProgramDbHelper";
	
	protected static final String PRESENTATION_TABLE = "presentation";
	protected static final String COL_PRESENTATION_ID = BaseColumns._ID;
	protected static final String COL_PRESENTATION_TITLE = "title";
	protected static final String COL_PRESENTATION_TAGS = "tags";
	protected static final String COL_PRESENTATION_SESSIONID = "session_id";
	protected static final String COL_PRESENTATION_ABSTRACT = "abstract";
	protected static final String COL_PRESENTATION_START_TIME = "startTime";
	protected static final String COL_PRESENTATION_END_TIME = "endTime";
	
	protected static final String SESSION_TABLE = "session";
	protected static final String COL_SESSION_ID = BaseColumns._ID;
	protected static final String COL_SESSION_TITLE = "title";
	protected static final String COL_SESSION_TAG = "tag";
	protected static final String COL_SESSION_LOCATION = "location";
	
	
	protected static final String SPEAKER_TABLE = "speaker";
	protected static final String COL_SPEAKER_ID = BaseColumns._ID;
	protected static final String COL_SPEAKER_FIRST_NAME = "first_name";
	protected static final String COL_SPEAKER_LAST_NAME = "last_name";
	protected static final String COL_SPEAKER_AFFILIATION = "affiliation";
	protected static final String COL_SPEAKER_POSITION = "position";
	protected static final String COL_SPEAKER_BIOGRAPHY = "biography";
	protected static final String COL_SPEAKER_EMAIL = "email";
	protected static final String COL_SPEAKER_ONLINE_PROFILE_LINK = "online_profile_link";
	
	protected static final String PRESENTATION_SPEAKERS_TABLE = "presentation_speaker";
	protected static final String COL_PRESENTATION_SPEAKERS_ID = BaseColumns._ID;
	protected static final String COL_PRESENTATION_SPEAKERS_PRESENTATION_ID = "presentation_id";
	protected static final String COL_PRESENTATION_SPEAKERS_SPEAKER_ID = "speaker_id";
	
	
	public ProgramDbHelper(Context context, String databaseName, ProgramFeature feature, int version) throws EnvSocialContentException {
		super(context, databaseName, feature, version);
		database = this.getWritableDatabase();
	}
	
	
	@Override
	public void onDbCreate(SQLiteDatabase db) {
		Log.d(TAG, "[DEBUG] >> ----------- Database " + getDatabaseName() + " is being created. ------------");
		
		db.execSQL("CREATE TABLE " + SESSION_TABLE + "(" + 
					COL_SESSION_ID + " INTEGER PRIMARY KEY, " + 
					COL_SESSION_TITLE + " TEXT, " + 
					COL_SESSION_TAG + " TEXT, " + 
					COL_SESSION_LOCATION + " TEXT);");
		
		db.execSQL("CREATE TABLE " + PRESENTATION_TABLE + "(" + COL_PRESENTATION_ID + " INTEGER PRIMARY KEY, " + 
					COL_PRESENTATION_TITLE + " TEXT, " +
					COL_PRESENTATION_TAGS + " TEXT, " +
					COL_PRESENTATION_ABSTRACT + " TEXT, " +
					COL_PRESENTATION_START_TIME + " TEXT, " +
					COL_PRESENTATION_END_TIME + " TEXT, " + 
					COL_PRESENTATION_SESSIONID + " INTEGER NOT NULL, " + 
					"FOREIGN KEY (" + COL_PRESENTATION_SESSIONID + ") REFERENCES " 
					+ SESSION_TABLE + "(" + COL_SESSION_ID + "));");
		
		db.execSQL("CREATE TABLE " + SPEAKER_TABLE + "(" + 
					COL_SPEAKER_ID + " INTEGER PRIMARY KEY, " + 
					COL_SPEAKER_FIRST_NAME + " TEXT, " +
					COL_SPEAKER_LAST_NAME + " TEXT, " +
					COL_SPEAKER_AFFILIATION + " TEXT, " +
					COL_SPEAKER_POSITION + " TEXT, " +					
					COL_SPEAKER_BIOGRAPHY + " TEXT, " +
					COL_SPEAKER_EMAIL + " TEXT, " +
					COL_SPEAKER_ONLINE_PROFILE_LINK + " TEXT);");
		
		db.execSQL("CREATE TABLE " + PRESENTATION_SPEAKERS_TABLE + "(" + 
					COL_PRESENTATION_SPEAKERS_ID + " INTEGER PRIMARY KEY, " +
					COL_PRESENTATION_SPEAKERS_PRESENTATION_ID + " INTEGER NOT NULL, " +
					COL_PRESENTATION_SPEAKERS_SPEAKER_ID + " INTEGER NOT NULL, " +
					"FOREIGN KEY (" + COL_PRESENTATION_SPEAKERS_PRESENTATION_ID + ") REFERENCES " 
					+ PRESENTATION_TABLE + "(" + COL_PRESENTATION_ID + ")" +
					"FOREIGN KEY (" + COL_PRESENTATION_SPEAKERS_SPEAKER_ID + ") REFERENCES " 
					+ SPEAKER_TABLE + "(" + COL_SPEAKER_ID + ")" +
					");");
		
		// Add trigger to enforce foreign key constraints, as SQLite does not support them.
		// Checks that when inserting a new presentation, a session with the specified session_id exists.
		db.execSQL("CREATE TRIGGER fk_session_presentation_id " +
				" BEFORE INSERT " +
			    " ON "+ PRESENTATION_TABLE +
			    " FOR EACH ROW BEGIN" +
			    " SELECT CASE WHEN ((SELECT " + COL_SESSION_ID + " FROM " + SESSION_TABLE + 
			    " WHERE "+ COL_SESSION_ID +"=new." + COL_PRESENTATION_SESSIONID + " ) IS NULL)" +
			    " THEN RAISE (ABORT,'Foreign Key Violation') END;"+
			    "  END;");
		
		// Add trigger to enforce foreign key constraints, as SQLite does not support them.
		// Checks that when inserting speakers for presentations in the m2m join table, the actual 
		// presentation_id and speaker_id exist
		db.execSQL("CREATE TRIGGER fk_presentation_speakers_id " + 
				" BEFORE INSERT " + " ON " + PRESENTATION_SPEAKERS_TABLE +
				" FOR EACH ROW BEGIN" + 
				" SELECT CASE WHEN " +
					"((SELECT " + COL_PRESENTATION_ID + " FROM " + PRESENTATION_TABLE + " WHERE " + 
						COL_PRESENTATION_ID + "=new." + COL_PRESENTATION_SPEAKERS_PRESENTATION_ID +
					" ) IS NULL OR" +
					"(SELECT " + COL_SPEAKER_ID + " FROM " + SPEAKER_TABLE + " WHERE " + 
							COL_SPEAKER_ID + "=new." + COL_PRESENTATION_SPEAKERS_SPEAKER_ID +
						" ) IS NULL " +
					")" +
				" THEN RAISE (ABORT,'Foreign Key Violation') END;" + "  END;");
		
		dbStatus = TABLES_CREATED;
	}
	
	
	@Override
	public void onDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + PRESENTATION_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + SESSION_TABLE);
		
		dbStatus = TABLES_INEXISTENT;
	}
	
	@Override
	public void onOpen(SQLiteDatabase db) {
		Log.d(TAG, "[DEBUG] >> ----------- Database " + getDatabaseName() + " already created. ------------");
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
		database.delete(PRESENTATION_TABLE, null, null);
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
				JSONArray presentationsArray = (JSONArray) program.getJSONArray("presentations");
				JSONArray speakersArray = (JSONArray) program.getJSONArray("speakers");
				
				insertSessions(sessionsArray);
				insertPresentations(presentationsArray);
				insertSpeakers(speakersArray);
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
	
	
	public void insertPresentations(JSONArray entriesArray) throws JSONException {
		Log.d(TAG, "[DEBUG] >> ----------- INSERTING ENTRIES ------------");
		
		ContentValues values = new ContentValues();
		int n = entriesArray.length();
		for (int i = 0; i < n; ++ i) {
			JSONObject session = entriesArray.getJSONObject(i);
			extractValues(session,
					values,
					new String[] { COL_PRESENTATION_ID, COL_PRESENTATION_SESSIONID },
					new String[] { COL_PRESENTATION_TITLE, COL_PRESENTATION_SPEAKERS, COL_PRESENTATION_START_TIME, COL_PRESENTATION_END_TIME }
					);
			database.insert(PRESENTATION_TABLE, COL_PRESENTATION_ID, values);
		}
	}
	
	
	private void insertSpeakers(JSONArray speakersArray) {
		// TODO Auto-generated method stub
		
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
		
		String queryString = "SELECT DISTINCT SUBSTR(" + COL_PRESENTATION_START_TIME + ",1,10) as Day FROM " + PRESENTATION_TABLE + ";";
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
									"e1." + COL_PRESENTATION_ID + ", " + 
									"e1." + COL_PRESENTATION_SESSIONID + ", " +
									"e1." + COL_PRESENTATION_TITLE + ", " +
									"e1." + COL_PRESENTATION_SPEAKERS + ", " + 
									"e1." + COL_PRESENTATION_START_TIME + ", " +
									"e1." + COL_PRESENTATION_END_TIME + " " + 
								"FROM entry e1 " +
								"WHERE " + 
									"SUBSTR(e1." + COL_PRESENTATION_START_TIME + ",1,10) = '" + day + "' " + 
									"AND e1." + COL_PRESENTATION_SESSIONID + " = " +
													"(SELECT MIN(e2." + COL_PRESENTATION_SESSIONID + ") " +
														"FROM entry e2 " +
														"WHERE " +
														  "SUBSTR(e2." + COL_PRESENTATION_START_TIME + ",1,10) = '" + day + "' " +
														  "AND e2." + COL_PRESENTATION_START_TIME + " <= e1." + COL_PRESENTATION_START_TIME + " " +
														  "AND e2." + COL_PRESENTATION_END_TIME + " > e1." + COL_PRESENTATION_START_TIME + 
													");";
		
		Cursor c = database.rawQuery(queryString, null);
		c.moveToFirst();
		
		while (!c.isAfterLast()) {
			Map<String,String> entry = new HashMap<String,String>();
			entry.put(COL_PRESENTATION_ID, c.getString(0));
			entry.put(COL_PRESENTATION_SESSIONID, c.getString(1));
			entry.put(COL_PRESENTATION_TITLE, c.getString(2));
			entry.put(COL_PRESENTATION_SPEAKERS, c.getString(3));
			entry.put(COL_PRESENTATION_START_TIME, c.getString(4));
			entry.put(COL_PRESENTATION_END_TIME, c.getString(5));
			entries.add(entry);
			
			c.moveToNext();
		}
		c.close();
		
		return entries;
	}
	
	public List<Map<String,String>> getEntriesByDay(String day, String sessionId) {
		List<Map<String,String>> entries = new ArrayList<Map<String,String>>();
		
		String queryString = 	"SELECT " +
									"e1." + COL_PRESENTATION_ID + ", " + 
									"e1." + COL_PRESENTATION_SESSIONID + ", " +
									"e1." + COL_PRESENTATION_TITLE + ", " +
									"e1." + COL_PRESENTATION_SPEAKERS + ", " + 
									"e1." + COL_PRESENTATION_START_TIME + ", " +
									"e1." + COL_PRESENTATION_END_TIME + " " + 
								"FROM entry e1 " +
								"WHERE " + 
									"SUBSTR(e1." + COL_PRESENTATION_START_TIME + ",1,10) = '" + day + "' " + 
									"AND e1." + COL_PRESENTATION_SESSIONID + " = '" + sessionId + "';";
		
		Cursor c = database.rawQuery(queryString, null);
		c.moveToFirst();
		
		while (!c.isAfterLast()) {
			Map<String,String> entry = new HashMap<String,String>();
			entry.put(COL_PRESENTATION_ID, c.getString(0));
			entry.put(COL_PRESENTATION_SESSIONID, c.getString(1));
			entry.put(COL_PRESENTATION_TITLE, c.getString(2));
			entry.put(COL_PRESENTATION_SPEAKERS, c.getString(3));
			entry.put(COL_PRESENTATION_START_TIME, c.getString(4));
			entry.put(COL_PRESENTATION_END_TIME, c.getString(5));
			entries.add(entry);
			
			c.moveToNext();
		}
		c.close();
		
		return entries;
	}
	
	public List<Map<String,String>> getOverlappingEntries(String entryId) {
		List<Map<String,String>> entries = new ArrayList<Map<String,String>>();
		
		String queryString = 	"SELECT " +
				"e1." + COL_PRESENTATION_ID + ", " + 
				"e1." + COL_PRESENTATION_SESSIONID + ", " +
				"e1." + COL_PRESENTATION_TITLE + ", " +
				"e1." + COL_PRESENTATION_SPEAKERS + ", " + 
				"e1." + COL_PRESENTATION_START_TIME + ", " +
				"e1." + COL_PRESENTATION_END_TIME + " " + 
			"FROM entry e1, " +
				 "(SELECT " + COL_PRESENTATION_ID + ", " + COL_PRESENTATION_SESSIONID + ", " + 
				 				COL_PRESENTATION_START_TIME + ", " + COL_PRESENTATION_END_TIME + " " +
				    "FROM entry WHERE " + COL_PRESENTATION_ID + " = '" + entryId + 
				    "' ORDER BY " + COL_PRESENTATION_SESSIONID + " ASC) AS e2" + " " +
			"WHERE " + 
				"e1." + COL_PRESENTATION_ID + " <> e2." + COL_PRESENTATION_ID + " " +
//				"AND SUBSTR(e1." + COL_ENTRY_START_TIME + ",1,10) = SUBSTR(e2." + COL_ENTRY_START_TIME + ",1,10) " +
				"AND e1." + COL_PRESENTATION_START_TIME + " <= e2." + COL_PRESENTATION_START_TIME + " " +
				"AND e1." + COL_PRESENTATION_END_TIME + " > e2." + COL_PRESENTATION_START_TIME + ";";
				

		Cursor c = database.rawQuery(queryString, null);
		c.moveToFirst();
		
		while (!c.isAfterLast()) {
			Map<String, String> entry = new HashMap<String, String>();
			entry.put(COL_PRESENTATION_ID, c.getString(0));
			entry.put(COL_PRESENTATION_SESSIONID, c.getString(1));
			entry.put(COL_PRESENTATION_TITLE, c.getString(2));
			entry.put(COL_PRESENTATION_SPEAKERS, c.getString(3));
			entry.put(COL_PRESENTATION_START_TIME, c.getString(4));
			entry.put(COL_PRESENTATION_END_TIME, c.getString(5));
			entries.add(entry);

			c.moveToNext();
		}
		c.close();
		
		return entries;
	}
	
	public Cursor getAllEntries() {
		Cursor c = database.query(PRESENTATION_TABLE, null, null, null, null, null, null);
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
