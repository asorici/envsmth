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
import android.database.sqlite.SQLiteQueryBuilder;
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
	protected static final String COL_SESSION_LOCATION_URL = "location_url";
	protected static final String COL_SESSION_LOCATION_NAME = "location_name";
	
	
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
	
	protected static final String PRESENTATION_FTS_TABLE = "presentation_fts";
	protected static final String COL_PRESENTATION_FTS_ID = BaseColumns._ID;
	protected static final String COL_PRESENTATION_FTS_TITLE = "title";
	protected static final String COL_PRESENTATION_FTS_TAGS = "tags";
	protected static final String COL_PRESENTATION_FTS_ABSTRACT = "abstract";
	
	protected static final String SPEAKER_FTS_TABLE = "speaker_fts";
	protected static final String COL_SPEAKER_FTS_ID = BaseColumns._ID;
	protected static final String COL_SPEAKER_FTS_NAME = "name";
	protected static final String COL_SPEAKER_FTS_AFFILIATION = "affiliation";
	protected static final String COL_SPEAKER_FTS_POSITION = "position";
	protected static final String COL_SPEAKER_FTS_BIOGRAPHY = "biography";
	protected static final String COL_SPEAKER_IMAGE_URL = "image_url";
	
	
	
	public ProgramDbHelper(Context context, String databaseName, ProgramFeature feature, int version) throws EnvSocialContentException {
		super(context, databaseName, feature, version);
		//database = this.getWritableDatabase();
	}
	
	
	@Override
	public void onDbCreate(SQLiteDatabase db) {
		Log.d(TAG, "[DEBUG] >> ----------- Database " + getDBName() + " is being created. ------------");
		
		db.execSQL("CREATE TABLE " + SESSION_TABLE + "(" + 
					COL_SESSION_ID + " INTEGER PRIMARY KEY, " + 
					COL_SESSION_TITLE + " TEXT, " + 
					COL_SESSION_TAG + " TEXT, " + 
					COL_SESSION_LOCATION_URL + " TEXT, " +
					COL_SESSION_LOCATION_NAME + " TEXT" +
					");");
		
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
					COL_SPEAKER_IMAGE_URL + " TEXT, " +
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
		
		
		// create the full text search tables for presentations and speakers
		db.execSQL("CREATE VIRTUAL TABLE " + PRESENTATION_FTS_TABLE + " " + 
					"USING fts3(" + 
						COL_PRESENTATION_FTS_ID + ", " + 
						COL_PRESENTATION_FTS_TITLE + ", " + 
						COL_PRESENTATION_FTS_TAGS + ", " +
						COL_PRESENTATION_FTS_ABSTRACT +
					");");
		
		db.execSQL("CREATE VIRTUAL TABLE " + SPEAKER_FTS_TABLE + " " + 
				"USING fts3(" + 
					COL_SPEAKER_FTS_ID + ", " + 
					COL_SPEAKER_FTS_NAME + ", " + 
					COL_SPEAKER_FTS_AFFILIATION + ", " +
					COL_SPEAKER_FTS_POSITION + ", " +
					COL_SPEAKER_FTS_BIOGRAPHY + 
				");");
	}
	
	
	@Override
	public void onDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + SPEAKER_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + PRESENTATION_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + PRESENTATION_SPEAKERS_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + SESSION_TABLE);
		
		db.execSQL("DROP TABLE IF EXISTS " + PRESENTATION_FTS_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + SPEAKER_FTS_TABLE);
	}
	
	@Override
	public void onDbOpen(SQLiteDatabase db) {
		Log.d(TAG, "[DEBUG] >> ----------- Database " + getDBName() + " already created. Now opening ------------");
	}
	
	@Override
	public void init (boolean insert) throws EnvSocialContentException {
		// do initial program insertion here if new data available
		if (insert) {
			insertProgram();
		}
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
		database.delete(SPEAKER_TABLE, null, null);
		database.delete(PRESENTATION_TABLE, null, null);
		database.delete(PRESENTATION_SPEAKERS_TABLE, null, null);
		database.delete(SESSION_TABLE, null, null);
		
		database.delete(PRESENTATION_FTS_TABLE, null, null);
		database.delete(SPEAKER_FTS_TABLE, null, null);
	}
	
	private void insertProgram() throws EnvSocialContentException {
		// perform initial insertion of the program if and only if the database is created
		Log.d(TAG, "Inserting program");
		
		String programJSON = feature.getSerializedData();
		
		try {
			// Parse program's JSON
			JSONObject program = (JSONObject) new JSONObject(programJSON).getJSONObject("program");
			JSONArray sessionsArray = (JSONArray) program.getJSONArray("sessions");
			JSONArray presentationsArray = (JSONArray) program.getJSONArray("presentations");
			JSONArray speakersArray = (JSONArray) program.getJSONArray("speakers");
			JSONArray presentationSpeakersArray = (JSONArray) program.getJSONArray("presentation_speakers");
			
			insertSessions(sessionsArray);
			insertPresentations(presentationsArray);
			insertSpeakers(speakersArray);
			insertPresentationSpeakers(presentationSpeakersArray);
		} catch (JSONException ex) {
			cleanupTables();
			throw new EnvSocialContentException(programJSON, EnvSocialResource.FEATURE, ex);
		}
		
		dbStatus = DB_POPULATED;
	}


	public void insertSessions(JSONArray sessionsArray) throws JSONException {
		Log.d(TAG, "[DEBUG] >> ----------- INSERTING SESSIONS ------------");
		
		ContentValues values = new ContentValues();
		int n = sessionsArray.length();
		
		try {
			database.beginTransaction();
			
			for (int i = 0; i < n; ++ i) {
				JSONObject session = sessionsArray.getJSONObject(i);
				int sessionID = session.getInt(ProgramFeature.SESSION_ID);
				String sessionTitle = session.getString(ProgramFeature.SESSION_TITLE);
				String sessionTag = session.getString(ProgramFeature.SESSION_TAG);
				String sessionLocationUrl = session.getString(ProgramFeature.SESSION_LOCATION_URL);
				String sessionLocationName = session.getString(ProgramFeature.SESSION_LOCATION_NAME);
				
				// fill values container
				values.put(COL_SESSION_ID, sessionID);
				values.put(COL_SESSION_TITLE, sessionTitle);
				values.put(COL_SESSION_TAG, sessionTag);
				values.put(COL_SESSION_LOCATION_URL, sessionLocationUrl);
				values.put(COL_SESSION_LOCATION_NAME, sessionLocationName);
				
				// insert into sessions table
				database.insert(SESSION_TABLE, null, values);
				values.clear();
			}
			
			database.setTransactionSuccessful();
		}
		finally {
			database.endTransaction();
		}
	}
	
	
	public void insertPresentations(JSONArray presentationsArray) throws JSONException {
		Log.d(TAG, "[DEBUG] >> ----------- INSERTING PRESENTATIONS ------------");
		
		ContentValues values = new ContentValues();
		int n = presentationsArray.length();
		
		try {
			database.beginTransaction();
			
			for (int i = 0; i < n; ++ i) {
				JSONObject presentation = presentationsArray.getJSONObject(i);
				int presentationID = presentation.getInt(ProgramFeature.PRESENTATION_ID);
				int presentationSessionID = presentation.getInt(ProgramFeature.PRESENTATION_SESSION_ID);
				String presentationTitle = presentation.getString(ProgramFeature.PRESENTATION_TITLE);
				String presentationStartTime = presentation.getString(ProgramFeature.PRESENTATION_START_TIME);
				String presentationEndTime = presentation.getString(ProgramFeature.PRESENTATION_END_TIME);
				
				String presentationTags = presentation.optString(ProgramFeature.PRESENTATION_TAGS, null);
				String presentationAbstract = presentation.optString(ProgramFeature.PRESENTATION_ABSTRACT, null);
				
				// fill values container for presentation table
				values.put(COL_PRESENTATION_ID, presentationID);
				values.put(COL_PRESENTATION_SESSIONID, presentationSessionID);
				values.put(COL_PRESENTATION_TITLE, presentationTitle);
				values.put(COL_PRESENTATION_START_TIME, presentationStartTime);
				values.put(COL_PRESENTATION_END_TIME, presentationEndTime);
				
				if (presentationTags != null) {
					values.put(COL_PRESENTATION_TAGS, presentationTags);
				}
				
				if (presentationAbstract != null) {
					values.put(COL_PRESENTATION_ABSTRACT, presentationAbstract);
				}
				
				// insert values into presentation table
				database.insert(PRESENTATION_TABLE, null, values);
				values.clear();
				
				
				// fill values for presentation FTS table
				values.put(COL_PRESENTATION_FTS_ID, String.valueOf(presentationID));
				values.put(COL_PRESENTATION_FTS_TITLE, presentationTitle);
				if (presentationTags != null) {
					values.put(COL_PRESENTATION_FTS_TAGS, presentationTags);
				}
				
				if (presentationAbstract != null) {
					values.put(COL_PRESENTATION_FTS_ABSTRACT, presentationAbstract);
				}
				
				// insert values into presentation FTS table
				database.insert(PRESENTATION_FTS_TABLE, null, values);
				values.clear();
			}
			
			database.setTransactionSuccessful();
		}
		finally {
			database.endTransaction();
		}
	}
	
	
	private void insertSpeakers(JSONArray speakersArray) throws JSONException {
		Log.d(TAG, "[DEBUG] >> ----------- INSERTING SPEAKERS ------------");
		
		ContentValues values = new ContentValues();
		int n = speakersArray.length();
		
		try {
			database.beginTransaction();
			
			for (int i = 0; i < n; ++ i) {
				JSONObject speaker = speakersArray.getJSONObject(i);
				
				int speakerID = speaker.getInt(ProgramFeature.SPEAKER_ID);
				String speakerFirstName = speaker.getString(ProgramFeature.SPEAKER_FIRST_NAME);
				String speakerLastName = speaker.getString(ProgramFeature.SPEAKER_LAST_NAME);
				String speakerAffiliation = speaker.getString(ProgramFeature.SPEAKER_AFFILIATION);
				String speakerPosition = speaker.getString(ProgramFeature.SPEAKER_POSITION);
				
				String speakerBiography = speaker.optString(ProgramFeature.SPEAKER_BIOGRAPHY, null);
				String speakerEmail = speaker.optString(ProgramFeature.SPEAKER_EMAIL, null);
				String speakerOnlineProfileLink = speaker.optString(ProgramFeature.SPEAKER_ONLINE_PROFILE_LINK, null);
				String speakerImageUrl = speaker.optString(ProgramFeature.SPEAKER_IMAGE_URL, null);
				
				// fill values container for presentation table
				values.put(COL_SPEAKER_ID, speakerID);
				values.put(COL_SPEAKER_FIRST_NAME, speakerFirstName);
				values.put(COL_SPEAKER_LAST_NAME, speakerLastName);
				values.put(COL_SPEAKER_AFFILIATION, speakerAffiliation);
				values.put(COL_SPEAKER_POSITION, speakerPosition);
				
				if (speakerBiography != null) {
					values.put(COL_SPEAKER_BIOGRAPHY, speakerBiography);
				}
				
				if (speakerEmail != null) {
					values.put(COL_SPEAKER_EMAIL, speakerEmail);
				}
				
				if (speakerBiography != null) {
					values.put(COL_SPEAKER_ONLINE_PROFILE_LINK, speakerOnlineProfileLink);
				}
				
				if (speakerImageUrl != null) {
					values.put(COL_SPEAKER_IMAGE_URL, speakerImageUrl);
				}
				
				// insert values into speaker table
				database.insert(SPEAKER_TABLE, null, values);
				values.clear();
				
				
				// fill values for spaker FTS table
				values.put(COL_SPEAKER_FTS_ID, String.valueOf(speakerID));
				values.put(COL_SPEAKER_FTS_NAME, speakerFirstName + " " + speakerLastName);
				values.put(COL_SPEAKER_FTS_AFFILIATION, speakerAffiliation);
				values.put(COL_SPEAKER_FTS_POSITION, speakerPosition);
				
				if (speakerBiography != null) {
					values.put(COL_SPEAKER_FTS_BIOGRAPHY, speakerBiography);
				}
				
				// insert values into presentation FTS table
				database.insert(SPEAKER_FTS_TABLE, null, values);
				values.clear();
			}
			
			database.setTransactionSuccessful();
		}
		finally {
			database.endTransaction();
		}
	}
	
	
	private void insertPresentationSpeakers(JSONArray presentationSpeakersArray) throws JSONException {
		
		Log.d(TAG, "[DEBUG] >> ----------- INSERTING SPEAKERS ------------");

		ContentValues values = new ContentValues();
		int n = presentationSpeakersArray.length();

		try {
			database.beginTransaction();

			for (int i = 0; i < n; ++i) {
				JSONObject presentationSpeakersMap = presentationSpeakersArray.getJSONObject(i);

				int presentationID = 
						presentationSpeakersMap.getInt(ProgramFeature.PRESENTATION_SPEAKERS_PRESENTATION_ID);
				
				int speakerID = 
						presentationSpeakersMap.getInt(ProgramFeature.PRESENTATION_SPEAKERS_SPEAKER_ID);
				
				// fill values container for presentation table
				values.put(COL_PRESENTATION_SPEAKERS_PRESENTATION_ID, presentationID);
				values.put(COL_PRESENTATION_SPEAKERS_SPEAKER_ID, speakerID);
				
				// insert values into presentation_speaker table
				database.insert(PRESENTATION_SPEAKERS_TABLE, null, values);
				values.clear();
			}

			database.setTransactionSuccessful();
		} finally {
			database.endTransaction();
		}
	}
	
	
	public List<String> getDistinctDays() {
		List<String> days = new ArrayList<String>();
		
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
	 
	public Cursor getAllSessions() {
		Cursor c = database.query(SESSION_TABLE, new String[] {COL_SESSION_ID, COL_SESSION_TITLE, 
				COL_SESSION_TAG, COL_SESSION_LOCATION_URL, COL_SESSION_LOCATION_NAME}, 
				null, null, null, null, null);
		
		return c;
	}
	
	
	public Cursor getAllPresentations() {
		Cursor c = database.query(PRESENTATION_TABLE, null, null, null, null, null, null);
		return c;
	}
	

	public Cursor getPresentationsByDay(String day) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(PRESENTATION_TABLE + " LEFT OUTER JOIN " + SESSION_TABLE + " " +
				"ON (" + PRESENTATION_TABLE + "." + COL_PRESENTATION_SESSIONID + 
				" = " + SESSION_TABLE + "." + COL_SESSION_ID + ")");
		
		String selection = "SUBSTR(" + PRESENTATION_TABLE +  "." + COL_PRESENTATION_START_TIME + ",1,10) = ? ";
		String[] selectionArgs = new String[] { day };
		
		String[] projectionIn = new String[] {
				PRESENTATION_TABLE + "." + COL_PRESENTATION_ID + " AS " + COL_PRESENTATION_ID,
				PRESENTATION_TABLE + "." + COL_PRESENTATION_TITLE + " AS " + COL_PRESENTATION_TITLE,
				"SUBSTR(" + COL_PRESENTATION_START_TIME + ", " + "12, 5) AS " + COL_PRESENTATION_START_TIME ,
				"SUBSTR(" + COL_PRESENTATION_END_TIME + ", " + "12, 5) AS " + COL_PRESENTATION_END_TIME ,
				SESSION_TABLE + "." + COL_SESSION_TITLE + " AS " + ProgramFeature.SESSION,
				COL_SESSION_LOCATION_NAME
		};
		
		String orderBy = PRESENTATION_TABLE + "." + COL_PRESENTATION_START_TIME;
		
		Cursor c = qb.query(database, projectionIn, selection, selectionArgs, null, null, orderBy);
		
		return c;
		
		/*
		String queryString = 	"SELECT " +
									"e1." + COL_PRESENTATION_ID + ", " + 
									"e1." + COL_PRESENTATION_SESSIONID + ", " +
									"e1." + COL_PRESENTATION_TITLE + ", " +
									"e1." + COL_PRESENTATION_ABSTRACT + ", " + 
									"e1." + COL_PRESENTATION_START_TIME + ", " +
									"e1." + COL_PRESENTATION_END_TIME + " " + 
								"FROM presentation e1 " +
								"WHERE " + 
									"SUBSTR(e1." + COL_PRESENTATION_START_TIME + ",1,10) = '" + day + "' " + 
									"AND e1." + COL_PRESENTATION_SESSIONID + " = " +
													"(SELECT MIN(e2." + COL_PRESENTATION_SESSIONID + ") " +
														"FROM presentation e2 " +
														"WHERE " +
														  "SUBSTR(e2." + COL_PRESENTATION_START_TIME + ",1,10) = '" + day + "' " +
														  "AND e2." + COL_PRESENTATION_START_TIME + " <= e1." + COL_PRESENTATION_START_TIME + " " +
														  "AND e2." + COL_PRESENTATION_END_TIME + " > e1." + COL_PRESENTATION_START_TIME + 
													");";
		
		Cursor c = database.rawQuery(queryString, null);
		return c;
		*/
	}
	
	
	public Cursor getPresentationsByDay(String day, int sessionId) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		qb.setTables(PRESENTATION_TABLE + " LEFT OUTER JOIN " + SESSION_TABLE + " " +
				"ON (" + PRESENTATION_TABLE + "." + COL_PRESENTATION_SESSIONID + 
				" = " + SESSION_TABLE + "." + COL_SESSION_ID + ")");
		
		String selection = "SUBSTR(" + PRESENTATION_TABLE +  "." + COL_PRESENTATION_START_TIME + ",1,10) = ? " +
							"AND " + PRESENTATION_TABLE + "." + COL_PRESENTATION_SESSIONID + " = ?";
		String[] selectionArgs = new String[] { day, String.valueOf(sessionId) };
		
		String[] projectionIn = new String[] {
				PRESENTATION_TABLE + "." + COL_PRESENTATION_ID + " AS " + COL_PRESENTATION_ID,
				PRESENTATION_TABLE + "." + COL_PRESENTATION_TITLE + " AS " + COL_PRESENTATION_TITLE,
				"SUBSTR(" + COL_PRESENTATION_START_TIME + ", " + "12, 5) AS " + COL_PRESENTATION_START_TIME ,
				"SUBSTR(" + COL_PRESENTATION_END_TIME + ", " + "12, 5) AS " + COL_PRESENTATION_END_TIME ,
				SESSION_TABLE + "." + COL_SESSION_TITLE + " AS " + ProgramFeature.SESSION,
				COL_SESSION_LOCATION_NAME
		};
		
		String orderBy = PRESENTATION_TABLE + "." + COL_PRESENTATION_START_TIME;
		
		Cursor c = qb.query(database, projectionIn, selection, selectionArgs, null, null, orderBy);
		
		/*
		Log.d(TAG, "---------- Should have 2 presentations: " + c.getCount());
		while(c.moveToNext()) {
			Log.d(TAG, "Presentations: " + c.getInt(0) + ": " + c.getString(1));
		}
		
		c.moveToFirst();
		*/
		
		return c;
	}
	
	
	public Cursor getSessionsByDay(String selectedDayString) {
		String selection = "(SELECT COUNT(*) FROM " + PRESENTATION_TABLE + 
				" WHERE SUBSTR(" + PRESENTATION_TABLE +  "." + COL_PRESENTATION_START_TIME + ",1,10) = ? " +
				" AND " + PRESENTATION_TABLE + "." + COL_PRESENTATION_SESSIONID + 
						" = " + SESSION_TABLE + "." + COL_SESSION_ID + ") > 0";
		String[] selectionArgs = new String[] { selectedDayString };
		String orderBy = SESSION_TABLE + "." + COL_SESSION_TITLE;
		
		Cursor c = database.query(SESSION_TABLE, null, 
				selection, selectionArgs, null, null, orderBy);
		
		/*
		Log.d(TAG, "---------- Should have 2 presentations: " + c.getCount());
		while(c.moveToNext()) {
			Log.d(TAG, "Sessions: " + c.getInt(0) + ": " + c.getString(1));
		}
		
		c.moveToFirst();
		*/
		
		return c;
	}
	
	public List<Map<String,String>> getOverlappingPresentations(String presentationId) {
		List<Map<String,String>> entries = new ArrayList<Map<String,String>>();
		
		String queryString = 	"SELECT " +
				"e1." + COL_PRESENTATION_ID + ", " + 
				"e1." + COL_PRESENTATION_SESSIONID + ", " +
				"e1." + COL_PRESENTATION_TITLE + ", " +
				"e1." + COL_PRESENTATION_ABSTRACT + ", " + 
				"e1." + COL_PRESENTATION_START_TIME + ", " +
				"e1." + COL_PRESENTATION_END_TIME + " " + 
			"FROM presentation e1, " +
				 "(SELECT " + COL_PRESENTATION_ID + ", " + COL_PRESENTATION_SESSIONID + ", " + 
				 				COL_PRESENTATION_START_TIME + ", " + COL_PRESENTATION_END_TIME + " " +
				    "FROM presentation WHERE " + COL_PRESENTATION_ID + " = '" + presentationId + 
				    "' ORDER BY " + COL_PRESENTATION_SESSIONID + " ASC) AS e2" + " " +
			"WHERE " + 
				"e1." + COL_PRESENTATION_ID + " <> e2." + COL_PRESENTATION_ID + " " +
//				"AND SUBSTR(e1." + COL_ENTRY_START_TIME + ",1,10) = SUBSTR(e2." + COL_ENTRY_START_TIME + ",1,10) " +
				"AND e1." + COL_PRESENTATION_START_TIME + " <= e2." + COL_PRESENTATION_START_TIME + " " +
				"AND e1." + COL_PRESENTATION_END_TIME + " > e2." + COL_PRESENTATION_START_TIME + ";";
				

		Cursor c = database.rawQuery(queryString, null);
		c.moveToFirst();
		
		while (!c.isAfterLast()) {
			Map<String, String> presentation = new HashMap<String, String>();
			presentation.put(COL_PRESENTATION_ID, c.getString(0));
			presentation.put(COL_PRESENTATION_SESSIONID, c.getString(1));
			presentation.put(COL_PRESENTATION_TITLE, c.getString(2));
			presentation.put(COL_PRESENTATION_ABSTRACT, c.getString(3));
			presentation.put(COL_PRESENTATION_START_TIME, c.getString(4));
			presentation.put(COL_PRESENTATION_END_TIME, c.getString(5));
			entries.add(presentation);

			c.moveToNext();
		}
		c.close();
		
		return entries;
	}
	
	
	public Cursor getPresentationDetails(int presentationId) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(PRESENTATION_TABLE + " LEFT OUTER JOIN " + SESSION_TABLE + " " +
				"ON (" + PRESENTATION_TABLE + "." + COL_PRESENTATION_SESSIONID + 
				" = " + SESSION_TABLE + "." + COL_SESSION_ID + ")");
		
		String selection = PRESENTATION_TABLE + "." + COL_PRESENTATION_ID + " = ?";
		String[] selectionArgs = new String[] {"" + presentationId};
		String[] projectionIn = new String[] {
				PRESENTATION_TABLE + "." + COL_PRESENTATION_ID + " AS " + COL_PRESENTATION_ID,
				PRESENTATION_TABLE + "." + COL_PRESENTATION_TITLE + " AS " + COL_PRESENTATION_TITLE,
				COL_PRESENTATION_START_TIME,
				COL_PRESENTATION_END_TIME,
				COL_PRESENTATION_TAGS,
				COL_PRESENTATION_ABSTRACT,				
				SESSION_TABLE + "." + COL_SESSION_TITLE + " AS " + ProgramFeature.SESSION,
				COL_SESSION_LOCATION_NAME,
				COL_SESSION_LOCATION_URL
		};
		
		return qb.query(database, projectionIn, selection, selectionArgs, null, null, null);
	}


	public Cursor getPresentationSpeakerInfo(int presentationId) {
		String rawQuery = 
				"SELECT " +
					"s." + COL_SPEAKER_ID + " AS " + COL_SPEAKER_ID + ", " +
					"s." + COL_SPEAKER_FIRST_NAME + " AS " + COL_SPEAKER_FIRST_NAME + ", " +
					"s." + COL_SPEAKER_LAST_NAME + " AS " + COL_SPEAKER_LAST_NAME + ", " +
					"s." + COL_SPEAKER_IMAGE_URL + " AS " + COL_SPEAKER_IMAGE_URL + " " +
					"FROM " + 
						SPEAKER_TABLE + " AS s, " + 
						PRESENTATION_TABLE + " AS p, " +
						PRESENTATION_SPEAKERS_TABLE + " AS ps " +
					"WHERE " +
						"p." + COL_PRESENTATION_ID + " = " + presentationId + " AND " + 
						"p." + COL_PRESENTATION_ID + " = " + "ps." + COL_PRESENTATION_SPEAKERS_PRESENTATION_ID + " AND " + 
						"s." + COL_SPEAKER_ID + " = " + "ps." + COL_PRESENTATION_SPEAKERS_SPEAKER_ID + " " +
					"ORDER BY s." + COL_SPEAKER_LAST_NAME;
		
	
		Cursor c = database.rawQuery(rawQuery, null);
		return c;
	}
	
	public Cursor getSpeakerDetails(int speakerId) {
		String selection = SPEAKER_TABLE + "." + COL_SPEAKER_ID + " = ?";
		String[] selectionArgs = new String[] {"" + speakerId};
		
		Cursor c = database.query(SPEAKER_TABLE, null, selection, selectionArgs, null, null, null);
		return c;
	}


	public Cursor getSpeakerPresentationsInfo(int speakerId) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(PRESENTATION_TABLE + ", " + PRESENTATION_SPEAKERS_TABLE + ", " + SPEAKER_TABLE + ", " + 
						SESSION_TABLE);
		
		String selection = SPEAKER_TABLE + "." + COL_SPEAKER_ID + " = ?" +
							" AND " + PRESENTATION_SPEAKERS_TABLE + "." + COL_PRESENTATION_SPEAKERS_SPEAKER_ID +
								" = " + SPEAKER_TABLE + "." + COL_SPEAKER_ID + 
							" AND " + PRESENTATION_SPEAKERS_TABLE + "." + COL_PRESENTATION_SPEAKERS_PRESENTATION_ID +
								" = " + PRESENTATION_TABLE + "." + COL_PRESENTATION_ID + 
							" AND " + SESSION_TABLE + "." + COL_SESSION_ID + 
							 	" = " + PRESENTATION_TABLE + "." + COL_PRESENTATION_SESSIONID;
		
		String[] selectionArgs = new String[] {"" + speakerId};
		
		String[] projectionIn = new String[] {
				PRESENTATION_TABLE + "." + COL_PRESENTATION_ID + " AS " + COL_PRESENTATION_ID,
				PRESENTATION_TABLE + "." + COL_PRESENTATION_TITLE + " AS " + COL_PRESENTATION_TITLE,
				COL_PRESENTATION_START_TIME,
				COL_PRESENTATION_END_TIME,
				COL_SESSION_LOCATION_NAME
		};
		
		String orderBy = PRESENTATION_TABLE + "." + COL_PRESENTATION_TITLE + " ASC";
		
		return qb.query(database, projectionIn, selection, selectionArgs, null, null, orderBy);
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
