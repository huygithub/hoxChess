package com.playxiangqi.hoxchess;

import android.text.TextUtils;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity
                        implements OnSharedPreferenceChangeListener {

    private static final String TAG = "SettingsActivity";
    
    private static final String KEY_PREF_AI_LEVEL = "pref_key_ai_level";
    private static final String KEY_PREF_ACCOUNT_USERNAME = "pref_key_playxiangqi_username";
    private static final String KEY_PREF_ACCOUNT_PASSWORD = "pref_key_playxiangqi_password";
    
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        
        Log.d(TAG, "onCreate:");
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        
        loadExistingSettings();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    private void loadExistingSettings() {
        Log.d(TAG, "Load existing settings...");
        
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        ListPreference aiPref = (ListPreference) findPreference(KEY_PREF_AI_LEVEL);        
        Log.d(TAG, ".... AI Level: " +  aiPref.getValue() + ", " + aiPref.getEntry());
        aiPref.setSummary(aiPref.getEntry());
        
        Preference pref = findPreference(KEY_PREF_ACCOUNT_USERNAME);
        pref.setSummary(sharedPreferences.getString(KEY_PREF_ACCOUNT_USERNAME, ""));
        
        pref = findPreference(KEY_PREF_ACCOUNT_PASSWORD);
        String password = sharedPreferences.getString(KEY_PREF_ACCOUNT_PASSWORD, "");
        pref.setSummary(getPasswordForDisplay(password));
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "On preferences changed: key=" + key);
        
        // Set summary to be the user-description for the selected value
        Preference pref = findPreference(key);
        
        if (key.equals(KEY_PREF_AI_LEVEL)) {
            ListPreference aiPref = (ListPreference) pref;
            pref.setSummary(aiPref.getEntry());
            int aiLevel = Integer.parseInt(aiPref.getValue());
            HoxApp.getApp().onAILevelChanged(aiLevel);
            
        } else if (key.equals(KEY_PREF_ACCOUNT_USERNAME)) {
            String pid = sharedPreferences.getString(key, "");
            pref.setSummary(pid);
            HoxApp.getApp().onAccountPidChanged(pid);
            
        } else if (key.equals(KEY_PREF_ACCOUNT_PASSWORD)) {
            String password = sharedPreferences.getString(key, "");
            pref.setSummary(getPasswordForDisplay(password));
            HoxApp.getApp().onAccountPasswordChanged(password);
        }
    }
    
    private String getPasswordForDisplay(String value) {
        return TextUtils.isEmpty(value) ? "" : getString(R.string.password_masked_value);
    }
    
    public static int getAILevel(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int aiLevel = Integer.parseInt(sharedPreferences.getString(KEY_PREF_AI_LEVEL, "0"));       
        Log.d(TAG, ".... Got AI Level: " +  aiLevel);
        return aiLevel;
    }

    public static String getAccountPid(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String pid = sharedPreferences.getString(KEY_PREF_ACCOUNT_USERNAME, "");    
        Log.d(TAG, ".... Got Account pid: " +  pid); // Player ID.
        return pid;
    }

    public static String getAccountPassword(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String password = sharedPreferences.getString(KEY_PREF_ACCOUNT_PASSWORD, "");    
        Log.d(TAG, ".... Got Account password: " +  password);
        return password;
    }
    
}
