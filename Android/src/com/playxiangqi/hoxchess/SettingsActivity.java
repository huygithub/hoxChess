package com.playxiangqi.hoxchess;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class SettingsActivity extends ActionBarActivity {

    private static final String TAG = "SettingsActivity";
    
    private RadioGroup aiLevelRadioGroup_;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        Log.d(TAG, "onCreate:");
        
        aiLevelRadioGroup_ = (RadioGroup)findViewById(R.id.radiogroup_ai_level);
        aiLevelRadioGroup_.setOnCheckedChangeListener(aiLevelOnCheckedChangeListener_);
        
        int aiLevel = HoxApp.getApp().loadAILevelPreferences();
        RadioButton savedCheckedRadioButton =
                (RadioButton) aiLevelRadioGroup_.getChildAt(aiLevel);
        savedCheckedRadioButton.setChecked(true);
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
