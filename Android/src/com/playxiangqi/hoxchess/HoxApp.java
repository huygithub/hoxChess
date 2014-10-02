/**
 *  Copyright 2014 Huy Phan <huyphan@playxiangqi.com>
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

import java.util.ArrayList;
import java.util.List;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

public class HoxApp extends Application {

    private static final String TAG = "HoxApp";
    
    private static final String SHARED_PREFERENCES_AI_LEVEL = "AI_LEVEL";
    private static final String KEY_SAVED_AI_LEVEL_INDEX = "SAVED_AI_LEVEL_INDEX";
    
    private static HoxApp thisApp_;
    
    private List<SettingsObserver> observers_ = new ArrayList<>();
    private final Object observerMutex_ = new Object();
    
    private int currentAILevel_ = -1; // Default = "invalid level"
    
    public HoxApp() {
        // Do nothing.
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()...");
        thisApp_ = this;
    }

    public static HoxApp getApp() {
        return thisApp_;
    }
    
    public static interface SettingsObserver {
        public void onAILevelChanged(int newLevel);
    }

    public void registerSettingsObserver(SettingsObserver newObserver) {
        if (newObserver == null) {
            throw new NullPointerException("Null Observer");
        }
        
        if (!observers_.contains(newObserver)) {
            observers_.add(newObserver);
        }
    }
    
    private void notifyObservers() {
        List<SettingsObserver> observersLocal = null;
        // Synchronization is used to make sure any observer registered
        // after message is received is not notified
        synchronized (observerMutex_) {
            observersLocal = new ArrayList<>(this.observers_);
        }
        for (SettingsObserver obj : observersLocal) {
            obj.onAILevelChanged(currentAILevel_);
        }
    }
    
    public int loadAILevelPreferences() {
        SharedPreferences sharedPreferences =
                thisApp_.getSharedPreferences(SHARED_PREFERENCES_AI_LEVEL, MODE_PRIVATE);
        int aiLevel = sharedPreferences.getInt(KEY_SAVED_AI_LEVEL_INDEX, 0);
        Log.d(TAG, "Load existing AI level: " + aiLevel);
        
        currentAILevel_ = aiLevel;
        return aiLevel;
    }
    
    public void savePreferences(int aiLevel) {
        Log.d(TAG, "Save the new AI level: " + aiLevel);
        SharedPreferences sharedPreferences =
                thisApp_.getSharedPreferences(SHARED_PREFERENCES_AI_LEVEL, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_SAVED_AI_LEVEL_INDEX, aiLevel);
        editor.commit();
        
        if (aiLevel != currentAILevel_) {
            currentAILevel_ = aiLevel;
            notifyObservers();
        }
    }
    
}
