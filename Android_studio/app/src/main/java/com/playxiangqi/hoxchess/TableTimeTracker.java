/**
 *  Copyright 2015 Huy Phan <huyphan@playxiangqi.com>
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

import java.util.Timer;
import java.util.TimerTask;

import com.playxiangqi.hoxchess.Enums.ColorEnum;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

/**
 * A table time tracker
 */
public class TableTimeTracker {

    private static final String TAG = "TableTimeTracker";

    private boolean hasUI_ = false;
    private TextView blackGameTimeView_;
    private TextView blackMoveTimeView_;
    private TextView redGameTimeView_;
    private TextView redMoveTimeView_;

    // Times.
    private TimeInfo initialTime_ = new TimeInfo(Enums.DEFAULT_INITIAL_GAME_TIMES);
    private TimeInfo blackTime_ = new TimeInfo();
    private TimeInfo redTime_ = new TimeInfo();
    
    private ColorEnum nextColor_ = ColorEnum.COLOR_RED;
    
    // Create timer to run in a daemon thread.
    // TODO: Can we just use the option to run the timer directly in the UI thread?
    private Timer myTimer_ = new Timer( true /* isDaemon */ );
    
    private boolean isRunning = false;
    
    public TableTimeTracker() {
        Log.d(TAG, "[CONSTRUCTOR]");
        myTimer_.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isRunning) {
                    runTimerTask();
                }
            }
        }, 0 /* no initial delay */, 1000 /* 1-second period */);
    }
    
    public void setUITextViews(
            TextView blackGameTimeView,
            TextView blackMoveTimeView,
            TextView redGameTimeView,
            TextView redMoveTimeView) {
        
        blackGameTimeView_ = blackGameTimeView;
        blackMoveTimeView_ = blackMoveTimeView;
        redGameTimeView_ = redGameTimeView;
        redMoveTimeView_ = redMoveTimeView;
        hasUI_ = true;
    }

    public void unsetUITextViews() {
        blackGameTimeView_ = null;
        blackMoveTimeView_ = null;
        redGameTimeView_ = null;
        redMoveTimeView_ = null;
        hasUI_ = false;
    }

    public void reset() {
        nextColor_ = ColorEnum.COLOR_RED;
        
        blackTime_.initWith(initialTime_);
        redTime_.initWith(initialTime_);
        
        // NOTE: We are in the main thread.
        //       Update the UI views directly.

        syncUI();
    }
    
    public void syncUI() {
        Log.d(TAG, "Sync UI: hasUI_ = " + hasUI_);

        if (!hasUI_) return;

        // NOTE: We are in the main thread.
        //       Update the UI views directly.

        blackGameTimeView_.setText(formatTime(blackTime_.gameTime));
        blackMoveTimeView_.setText(formatTime(blackTime_.moveTime));
        redGameTimeView_.setText(formatTime(redTime_.gameTime));
        redMoveTimeView_.setText(formatTime(redTime_.moveTime));
    }
    
    public void setInitialColor(ColorEnum color) {
        Log.d(TAG, "Set the initial color:" + color);
        nextColor_ = color;
    }
    
    public void nextColor() {
        final ColorEnum oldColor = nextColor_;
        nextColor_ = (nextColor_ == ColorEnum.COLOR_RED
                ? ColorEnum.COLOR_BLACK : ColorEnum.COLOR_RED);
        Log.d(TAG, "Change color: " + oldColor + " => " + nextColor_ + ".");

        // Reset the move time.
        if (nextColor_ == ColorEnum.COLOR_RED) {
            redTime_.moveTime = initialTime_.moveTime;
        } else if (nextColor_ == ColorEnum.COLOR_BLACK) {
            blackTime_.moveTime = initialTime_.moveTime;
        }
    }
    
    public void start() {
        if (!isRunning) {
            Log.i(TAG, "Start counting down...");
            isRunning = true;
        }
    }
    
    public void stop() {
        isRunning = false;
    }
    
    public void setInitialTime(TimeInfo timeInfo) {
        initialTime_.initWith(timeInfo);
    }
    
    public void setBlackTime(TimeInfo timeInfo) {
        blackTime_.initWith(timeInfo);
    }
    
    public void setRedTime(TimeInfo timeInfo) {
        redTime_.initWith(timeInfo);
    }

    public void reverseView() {
        TextView view = blackGameTimeView_;
        blackGameTimeView_ = redGameTimeView_;
        redGameTimeView_ = view;
        
        view = blackMoveTimeView_;
        blackMoveTimeView_ = redMoveTimeView_;
        redMoveTimeView_ = view;
    }
    
    @SuppressLint("DefaultLocale")
    private static String formatTime(int timeInSeconds) {
        final int minutes = timeInSeconds / 60;
        final int seconds = timeInSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    private Handler textViewUpdaterHandler_ = new Handler(Looper.getMainLooper());
    
    private class TextViewUpdater implements Runnable{
        private ColorEnum uiColor_ = ColorEnum.COLOR_UNKNOWN;
        private int uiGameTime_;
        private int uiMoveTime_;
        
        @Override
        public void run() {
            if (!hasUI_) return;

            if (uiColor_ == ColorEnum.COLOR_RED) {
                redGameTimeView_.setText(formatTime(uiGameTime_));
                redMoveTimeView_.setText(formatTime(uiMoveTime_));
            } else if (uiColor_ == ColorEnum.COLOR_BLACK) {
                blackGameTimeView_.setText(formatTime(uiGameTime_));
                blackMoveTimeView_.setText(formatTime(uiMoveTime_));
            }
        }
        
        public void setNewTimes(ColorEnum color, int gameTime, int moveTime) {
            uiColor_ = color;
            uiGameTime_ = gameTime;
            uiMoveTime_ = moveTime;
        }

    }
    
    private void runTimerTask() {
        //Log.v(TAG, "Run a timer task...");
        
        TextViewUpdater textViewUpdater = new TextViewUpdater();
        
        if (nextColor_ == ColorEnum.COLOR_RED) {
            redTime_.decrement();
            textViewUpdater.setNewTimes(nextColor_, redTime_.gameTime, redTime_.moveTime);
            
        } else if (nextColor_ == ColorEnum.COLOR_BLACK) {
            blackTime_.decrement();
            textViewUpdater.setNewTimes(nextColor_, blackTime_.gameTime, blackTime_.moveTime);
            
        } else {
            return;
        }
        
        // NOTE: We are in a daemon thread.
        //       Care must be taken to update UI elements (only the UI thread can do that).
        
        textViewUpdaterHandler_.post(textViewUpdater);
    }
    
}
