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

import com.playxiangqi.hoxchess.Enums.ColorEnum;

import android.util.Log;

/**
 * This referee is based on native code (based on NDK).
 */
public class Referee {

    private static final String TAG = "Referee";
    
    public Referee() {
        Log.d(TAG, "Create a new referee...");
        nativeCreateReferee();
    }
    
    public void resetGame() {
        nativeResetGame();
    }
    
    public ColorEnum getNextColor() {
        final int nextColor = nativeGetNextColor();
        if (nextColor == hoxCOLOR_RED) return ColorEnum.COLOR_RED;
        if (nextColor == hoxCOLOR_BLACK) return ColorEnum.COLOR_BLACK;
        return ColorEnum.COLOR_UNKNOWN;
        
    }
    
    public int validateMove(int row1, int col1, int row2, int col2) {
        return nativeValidateMove(row1, col1, row2, col2);
    }
    
    // ****************************** Native code **********************************
    // TODO: Need to fix for invalid moves when "king-facing-king"!!!
    private native int nativeCreateReferee();
    private native int nativeResetGame();
    private native int nativeGetNextColor();
    private native int nativeValidateMove(int row1, int col1, int row2, int col2);
    
    // The native referee 's game-status.
    // DO NOT CHANGE the constants' values.
    public final static int hoxGAME_STATUS_UNKNOWN = -1;
    public final static int hoxGAME_STATUS_OPEN = 0;        // Open but not enough Player.
    public final static int hoxGAME_STATUS_READY = 1;       // Enough (2) players, waiting for 1st Move.
    public final static int hoxGAME_STATUS_IN_PROGRESS = 2; // At least 1 Move has been made.
    public final static int hoxGAME_STATUS_RED_WIN = 3;     // Game Over: Red won.
    public final static int hoxGAME_STATUS_BLACK_WIN = 4;   // Game Over: Black won.
    public final static int hoxGAME_STATUS_DRAWN = 5;       // Game Over: Drawn.
    
    private final static int hoxCOLOR_RED = 0;
    private final static int hoxCOLOR_BLACK = 1;
    
    static {
        System.loadLibrary("Referee");
    }
    // *****************************************************************************
}
