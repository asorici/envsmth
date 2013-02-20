package com.envsocial.android;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;

public class EnvivedSettings extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String KEY_ENVIVED_NOTIFICATIONS = "envived_gcm_notifications";
    
    private CheckBoxPreference mEnvivedNotificationsPreference;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the XML preferences file
        addPreferencesFromResource(R.xml.envived_preferences);

        // Get a reference to the Envived notifications preference
        mEnvivedNotificationsPreference = (CheckBoxPreference)getPreferenceScreen().findPreference(
        		KEY_ENVIVED_NOTIFICATIONS);
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// TODO Auto-generated method stub
		
	}
}
