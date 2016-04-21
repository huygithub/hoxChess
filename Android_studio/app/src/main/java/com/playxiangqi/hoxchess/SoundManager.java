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

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.util.SparseIntArray;

/**
 * This class manages sound effects in the entire app.
 *
 * Reference:
 *     (Android Sound Manager) https://gist.github.com/FR073N/9902559
 */
public class SoundManager {

    private static final String TAG = "SoundManager";
    private static final int MAX_STREAMS = 2;
    private static final int MY_STREAM_TYPE = AudioManager.STREAM_MUSIC;

    // Sound resource IDs.
    public static final int SOUND_MOVE = R.raw.move;
    public static final int SOUND_CAPTURE = R.raw.capture;

    // The singleton instance.
    private static SoundManager instance_;

    // Member variables...
    private boolean initialized_ = false;
    private final SoundPool soundPool_;
    private final SparseIntArray soundMap_= new SparseIntArray();
    private boolean soundEnabled_ = true;

    /**
     * Singleton API to return the instance.
     */
    public static SoundManager getInstance() {
        if (instance_ == null) {
            instance_ = new SoundManager();
        }
        return instance_;
    }

    /**
     * Constructor
     */
    @SuppressWarnings("deprecation")
    public SoundManager() {
        Log.v(TAG, "[CONSTRUCTOR]: ...");
        soundPool_ = new SoundPool(MAX_STREAMS, MY_STREAM_TYPE, 0 /* srcQuality */);

        soundPool_.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int mySoundId, int status) {
                // Note: status 0 => success
                Log.d(TAG, "... onLoadComplete: soundId:" + mySoundId + ", status:" + status);
            }
        });
    }

    // ***************************************************************
    //
    //              Public APIs
    //
    // ***************************************************************

    /**
     * Initializes a sound manager.
     */
    public void initialize(Context context) {
        if (initialized_) {
            Log.i(TAG, "The sound manager has already initialized. Do nothing.");
        } else {
            soundEnabled_ = SettingsActivity.getSoundEnabledFlag(context);
            Log.i(TAG, "Initialize the sound manager: soundEnabled:" + soundEnabled_);
            if (soundEnabled_) {
                allAllSupportedSounds(context);
            }
            initialized_ = true;
        }
    }

    public void playSound(int soundType) {
        if (!soundEnabled_) return;

        final int soundId = soundMap_.get(soundType, -1 /* valueIfKeyNotFound */);
        if (soundId != -1) { // found?
            //Log.d(TAG, "Play sound: playing this soundId = " + soundId);
            soundPool_.play(soundId,
                    1    /* leftVolume. Note: range = 0.0 to 1.0 */,
                    1    /* rightVolume.Note: range = 0.0 to 1.0 */,
                    1    /* priority. Note: 0 = lowest priority */,
                    0    /* no loop */,
                    1.0f /* 1.0 = normal playback, */);
        } else {
            Log.w(TAG, "Play sound: Did not find sound type = " + soundType);
        }
    }

    public void setSoundEnabled(Context context, boolean enabled) {
        Log.d(TAG, "Set sound: " + soundEnabled_ + " => " + enabled);
        if (soundEnabled_ != enabled) {
            soundEnabled_ = enabled;
            // Add all sounds if we have not done so during initialization.
            if (soundEnabled_ && initialized_) {
                allAllSupportedSounds(context);
            }
        }
    }

    public int getStreamType() {
        return MY_STREAM_TYPE;
    }

    // ***************************************************************
    //
    //              Private APIs
    //
    // ***************************************************************

    private void allAllSupportedSounds(Context context) {
        Log.d(TAG, "Add all supported sounds...");
        addSound(context, SOUND_MOVE);
        addSound(context, SOUND_CAPTURE);
    }

    private void addSound(Context context, int soundResId) {
        final int soundId = soundPool_.load(context, soundResId, 1 /* priority */);
        soundMap_.put(soundResId, soundId);
    }
}
