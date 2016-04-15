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

/**
 * An AI engine
 */
public class AIEngine {

    private int currentAILevel_ = -1; // Default = "invalid level"

    public AIEngine() {
        // Do nothing.
    }

    public int getAILevel() { return currentAILevel_; }

    public void setAILevel(int aiLevel) {
        currentAILevel_ = aiLevel;
        setDifficultyLevel(currentAILevel_);
    }

    // ****************************** Native code **********************************
    public native String getInfo();
    
    public native int setDifficultyLevel(int nAILevel);
    public native int initGame();
    public native String generateMove();
    public native int onHumanMove(int row1, int col1, int row2, int col2);
    
    static {
        System.loadLibrary("AI_MaxQi");
    }
    // *****************************************************************************
    
}
