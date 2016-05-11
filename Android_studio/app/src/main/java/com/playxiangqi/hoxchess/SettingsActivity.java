/**
 *  Copyright 2016 Huy Phan <huyphan@playxiangqi.com>
 * 
 *  This file is part of HOXChess.
 * 
 *  HOXChess is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  HOXChess is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with HOXChess.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.playxiangqi.hoxchess;

import android.app.Activity;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    
    private static final String KEY_PREF_AI_LEVEL = "pref_key_ai_level";
    private static final String KEY_PREF_SOUND_ENABLED = "pref_key_sound_enabled";
    private static final String KEY_PREF_ACCOUNT_LOGIN = "pref_key_playxiangqi_login_with_account";
    private static final String KEY_PREF_ACCOUNT_USERNAME = "pref_key_playxiangqi_username";
    private static final String KEY_PREF_ACCOUNT_PASSWORD = "pref_key_playxiangqi_password";

    public static class SettingsInfo {
        public int aiLevel;
        public boolean soundEnabled;
        public boolean loginWithAccount;
        public String myPid;
        public String myPassword;
    }

    public static SettingsInfo getCurrentSettingsInfo(Context context) {
        SettingsInfo settingsInfo = new SettingsInfo();
        settingsInfo.aiLevel = getAILevel(context);
        settingsInfo.soundEnabled = getSoundEnabledFlag(context);
        settingsInfo.loginWithAccount = getLoginWithAccountFlag(context);
        settingsInfo.myPid = getAccountPid(context);
        settingsInfo.myPassword = getAccountPassword(context);
        return settingsInfo;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate:");
        
        setContentView(R.layout.activity_settings);

        getFragmentManager().beginTransaction()
            .add(R.id.container, new SettingsFragment())
            .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "(ActionBar) onOptionsItemSelected");

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long);
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home: // To handle the BACK button!
                Intent result = new Intent();
                //result.putExtra("changed", true);
                setResult(Activity.RESULT_OK, result);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static int getAILevel(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int aiLevel = Integer.parseInt(sharedPreferences.getString(KEY_PREF_AI_LEVEL,
                context.getString(R.string.AILevel_default)));       
        Log.d(TAG, ".... Got AI Level: " +  aiLevel);
        return aiLevel;
    }

    public static boolean getSoundEnabledFlag(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean soundEnabled = sharedPreferences.getBoolean(KEY_PREF_SOUND_ENABLED, true);
        Log.d(TAG, ".... Got Sound enabled: " +  soundEnabled);
        return soundEnabled;
    }

    public static boolean getLoginWithAccountFlag(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean loginWithAccount = sharedPreferences.getBoolean(KEY_PREF_ACCOUNT_LOGIN, false);
        Log.d(TAG, ".... Got loginWithAccount: " +  loginWithAccount);
        return loginWithAccount;
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
        Log.d(TAG, ".... Got Account password.");
        return password;
    }
    
    /**
     * A placeholder fragment containing a Preference fragment.
     */
    public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            addPreferencesFromResource(R.xml.preferences);
            loadExistingSettings();
        }
        
        private void loadExistingSettings() {
            Log.d(TAG, "Load existing settings...");
            
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            ListPreference aiPref = (ListPreference) findPreference(KEY_PREF_AI_LEVEL);        
            Log.d(TAG, ".... AI Level: " +  aiPref.getValue() + ", " + aiPref.getEntry());
            aiPref.setSummary(aiPref.getEntry());
            
            Preference pref = findPreference(KEY_PREF_ACCOUNT_USERNAME);
            pref.setSummary(sharedPreferences.getString(KEY_PREF_ACCOUNT_USERNAME, ""));
            
            pref = findPreference(KEY_PREF_ACCOUNT_PASSWORD);
            String password = sharedPreferences.getString(KEY_PREF_ACCOUNT_PASSWORD, "");
            pref.setSummary(getPasswordForDisplay(password));
        }
        
        private String getPasswordForDisplay(String value) {
            return TextUtils.isEmpty(value) ? "" : getString(R.string.password_masked_value);
        }

      @Override
      public void onResume() {
          super.onResume();
          getPreferenceScreen().getSharedPreferences()
                  .registerOnSharedPreferenceChangeListener(this);
      }
  
      @Override
      public void onPause() {
          super.onPause();
          getPreferenceScreen().getSharedPreferences()
                  .unregisterOnSharedPreferenceChangeListener(this);
      }
        
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
              
          } else if (key.equals(KEY_PREF_SOUND_ENABLED)) {
              CheckBoxPreference soundPref = (CheckBoxPreference) pref;
              SoundManager.getInstance().setSoundEnabled(getActivity(), soundPref.isChecked());

          } else if (key.equals(KEY_PREF_ACCOUNT_LOGIN)) {
              boolean loginWithAccount = sharedPreferences.getBoolean(key, false);
              //HoxApp.getApp().onLoginWithAccountFlagChanged(loginWithAccount);

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
    }
}
