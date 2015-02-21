package com.playxiangqi.hoxchess;

import com.playxiangqi.hoxchess.HoxApp.AccountInfo;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class SettingsActivity extends ActionBarActivity {

    private static final String TAG = "SettingsActivity";
    
    private RadioGroup aiLevelRadioGroup_;
    private TextView accountUsername_;
    private TextView accountPassword_;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        Log.d(TAG, "onCreate:");
        
        aiLevelRadioGroup_ = (RadioGroup)findViewById(R.id.radiogroup_ai_level);
        aiLevelRadioGroup_.setOnCheckedChangeListener(aiLevelOnCheckedChangeListener_);
        
        accountUsername_ = (TextView)findViewById(R.id.account_username);
        accountPassword_ = (TextView)findViewById(R.id.account_password);
        
        loadExistingSettings();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                Log.d(TAG, "onOptionsItemSelected: The HOME button clicked");
                saveNewSettings();
                break;
                
            default:
                break;
        
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed): ...");
        saveNewSettings();
        super.onBackPressed();
    }
    
    private void loadExistingSettings() {
        Log.d(TAG, "Load existing settings...");
        
        int aiLevel = HoxApp.getApp().getAILevel();
        RadioButton savedCheckedRadioButton =
                (RadioButton) aiLevelRadioGroup_.getChildAt(aiLevel);
        savedCheckedRadioButton.setChecked(true);
        
        final AccountInfo accountInfo = HoxApp.getApp().loadPreferences_Account();
        accountUsername_.setText(accountInfo.username);
        accountPassword_.setText(accountInfo.password);
    }
    
    private void saveNewSettings() {
        Log.d(TAG, "Save new settings...");
        
        final String username = accountUsername_.getText().toString();
        final String password = accountPassword_.getText().toString();
        HoxApp.getApp().savePreferences_Account(username, password);
    }
    
    private OnCheckedChangeListener aiLevelOnCheckedChangeListener_ =
            new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            RadioButton checkedRadioButton =
                    (RadioButton) aiLevelRadioGroup_.findViewById(checkedId);
            int checkedIndex = aiLevelRadioGroup_.indexOfChild(checkedRadioButton);
            HoxApp.getApp().savePreferences(checkedIndex);
        }
    };
    
}
