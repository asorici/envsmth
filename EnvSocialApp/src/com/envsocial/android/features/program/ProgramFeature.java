package com.envsocial.android.features.program;

import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.database.Cursor;

import com.envsocial.android.Envived;
import com.envsocial.android.R;
import com.envsocial.android.api.AppClient;
import com.envsocial.android.api.Location;
import com.envsocial.android.api.Url;
import com.envsocial.android.api.exceptions.EnvSocialContentException;
import com.envsocial.android.features.Feature;
import com.envsocial.android.utils.EnvivedNotificationDispatcher;
import com.envsocial.android.utils.EnvivedNotificationHandler;
import com.envsocial.android.utils.FeatureDbHelper;

public class ProgramFeature extends Feature {
	private static final long serialVersionUID = 1L;
	
	private EnvivedNotificationHandler notificationHandler;
	private static final String TAG = "ProgramFeature";
	
	public static final String PRESENTATION_QUERY_TYPE = "presentation";
	
	public static final String SESSION = "session";
	public static final String SESSION_ID = "id";
	public static final String SESSION_TITLE = "title";
	public static final String SESSION_TAG = "tag";
	public static final String SESSION_LOCATION_URL = "location_url";
	public static final String SESSION_LOCATION_NAME = "location_name";
	
	public static final String PRESENTATION = "presentation";
	public static final String PRESENTATION_ID = "id";
	public static final String PRESENTATION_SESSION_ID = "sessionId";
	public static final String PRESENTATION_TITLE = "title";
	public static final String PRESENTATION_TAGS = "tags";
	public static final String PRESENTATION_ABSTRACT = "abstract";
	public static final String PRESENTATION_START_TIME = "startTime";
	public static final String PRESENTATION_END_TIME = "endTime";
	
	public static final String SPEAKER = "speaker";
	public static final String SPEAKER_ID = "id";
	public static final String SPEAKER_FIRST_NAME = "first_name";
	public static final String SPEAKER_LAST_NAME = "last_name";
	public static final String SPEAKER_AFFILIATION = "affiliation";
	public static final String SPEAKER_POSITION = "position";
	public static final String SPEAKER_BIOGRAPHY = "biography";
	public static final String SPEAKER_EMAIL = "email";
	public static final String SPEAKER_ONLINE_PROFILE_LINK = "online_profile_link";
	public static final String SPEAKER_IMAGE_URL = "image_url";
	
	public static final String PRESENTATION_SPEAKERS = "presentation_speakers";
	public static final String PRESENTATION_SPEAKERS_PRESENTATION_ID = "presentation_id";
	public static final String PRESENTATION_SPEAKERS_SPEAKER_ID = "speaker_id";
	
	private transient ProgramDbHelper dbHelper;
	
	public ProgramFeature(String category, int version, Calendar timestamp, boolean isGeneral, 
			String resourceUri, String environmentUri, String areaUri, String data, boolean virtualAccess) 
					throws EnvSocialContentException {
		super(category, version, timestamp, isGeneral, resourceUri, environmentUri, areaUri, data, virtualAccess);

	}
	
	
	@Override
	protected void featureInit(boolean insert) throws EnvSocialContentException {
		// register program notification handler
		notificationHandler = new ProgramFeatureNotificationHandler();
		EnvivedNotificationDispatcher.registerNotificationHandler(notificationHandler);
		
		// instantiate local database
		String databaseName = getLocalCacheFileName(category, environmentUrl, areaUrl, version);
		
		if (dbHelper == null) {
			dbHelper = new ProgramDbHelper(Envived.getContext(), databaseName, this, version);
		}
		
		if (dbHelper != null) {
			dbHelper.init(insert);
		}
	}

	@Override
	protected void featureUpdate() throws EnvSocialContentException {
		// instantiate local database
		String databaseName = getLocalCacheFileName(category, environmentUrl, areaUrl, version);
		
		if (dbHelper == null) {
			dbHelper = new ProgramDbHelper(Envived.getContext(), databaseName, this, version);
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


	@Override
	public int getDisplayThumbnail() {
		return R.drawable.details_icon_schedule_white;
	}


	@Override
	public String getDisplayName() {
		return "Program";
	}
	
	
	public List<String> getDistinctDays() {
		if (dbHelper != null) {
			return dbHelper.getDistinctDays();
		}
		
		return null;
	}
	

	public Cursor getPresentationsByDay(String dayString) {
		if (dbHelper != null) {
			return dbHelper.getPresentationsByDay(dayString);
		}
		return null;
	}
	
	
	public Cursor getPresentationsByDay(String dayString, int sessionId) {
		if (dbHelper != null) {
			return dbHelper.getPresentationsByDay(dayString, sessionId);
		}
		return null;
	}
	
	public Cursor getSessionsByDay(String selectedDayString) {
		if (dbHelper != null) {
			return dbHelper.getSessionsByDay(selectedDayString);
		}
		return null;
	}

	
	
	public Cursor getPresentationDetails(int presentationId) {
		if (dbHelper != null) {
			return dbHelper.getPresentationDetails(presentationId);
		}
		return null;
	}


	public Cursor getPresentationSpeakerInfo(int presentationId) {
		if (dbHelper != null) {
			return dbHelper.getPresentationSpeakerInfo(presentationId);
		}
		return null;
	}


	public Cursor getSpeakerDetails(int speakerId) {
		if (dbHelper != null) {
			return dbHelper.getSpeakerDetails(speakerId);
		}
		return null;
	}


	public Cursor getSpeakerPresentationsInfo(int speakerId) {
		if (dbHelper != null) {
			return dbHelper.getSpeakerPresentationsInfo(speakerId);
		}
		return null;
	}

}
