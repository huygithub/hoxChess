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

import java.util.ArrayList;
import java.util.List;

import com.playxiangqi.hoxchess.Enums.ColorEnum;
import com.playxiangqi.hoxchess.Enums.GameStatus;
import com.playxiangqi.hoxchess.Piece.Move;

import android.util.Log;

/**
 * This referee is based on native code (based on NDK).
 *
 *  NOTE: The referee (JNI based) has a limitation that it has ONLY one instance created!
 *    For more details, see ./app/src/main/jni/Referee.cpp
 *    and pay attention to the "static hoxReferee *referee_".
 *    As a result, we must share the global referee under HoxApp.
 */
public class Referee {

    private static final String TAG = "Referee";
    
    private int gameStatus_ = Referee.hoxGAME_STATUS_READY;
    private List<Move> historyMoves_ = new ArrayList<Move>(); // All (past) Moves made so far.
    
    public Referee() {
        Log.d(TAG, "Create a new referee...");
        nativeCreateReferee();
    }
    
    public void resetGame() {
        Log.d(TAG, "Reset the game...");
        nativeResetGame();
        gameStatus_ = Referee.hoxGAME_STATUS_READY;
        historyMoves_.clear();
    }
    
    public ColorEnum getNextColor() {
        final int nextColor = nativeGetNextColor();
        if (nextColor == hoxCOLOR_RED) return ColorEnum.COLOR_RED;
        if (nextColor == hoxCOLOR_BLACK) return ColorEnum.COLOR_BLACK;
        return ColorEnum.COLOR_UNKNOWN;
    }
    
    public int validateMove(int row1, int col1, int row2, int col2) {
        final int status = nativeValidateMove(row1, col1, row2, col2);
        if (status == Referee.hoxGAME_STATUS_UNKNOWN) { // Move is not valid?
            Log.w(TAG, " This move [" + row1 + ", " + col1
                    + "] => [" + row2 + ", " + col2 + "] is NOT valid.");
            return status;
        }
        
        Piece.Move move = new Piece.Move();
        move.fromPosition = new Position(row1, col1);
        move.toPosition = new Position(row2, col2);
        move.isCaptured = false; // TODO: (capture != null);
        historyMoves_.add(move);
        
        gameStatus_ = status;
        return gameStatus_;
    }
    
    public int getGameStatus() {
        return gameStatus_;
    }
    
    public List<Move> getHistoryMoves() {
        return historyMoves_;
    }
    
    public int getMoveCount() {
        return historyMoves_.size();
    }
    
    public boolean isGameInProgress() {
        return (   gameStatus_ == Referee.hoxGAME_STATUS_READY
                || gameStatus_ == Referee.hoxGAME_STATUS_IN_PROGRESS);
    }

    public static String gameStatusToString(int gameStatus) {
        switch (gameStatus) {
            case Referee.hoxGAME_STATUS_UNKNOWN:     return "Unknown";
            //case Referee.hoxGAME_STATUS_OPEN:      return "Open";
            case Referee.hoxGAME_STATUS_READY:       return "Ready";
            case Referee.hoxGAME_STATUS_IN_PROGRESS: return "Progress";
            case Referee.hoxGAME_STATUS_RED_WIN:     return "Red_win";
            case Referee.hoxGAME_STATUS_BLACK_WIN:   return "Black_win";
            case Referee.hoxGAME_STATUS_DRAWN:       return "Drawn";
            default: return "__BUG_Not_Supported_Game_Status__:" + gameStatus;
        }
    }

    public static Enums.GameStatus gameStatusToEnum(int gameStatus) {
        switch (gameStatus) {
            case Referee.hoxGAME_STATUS_UNKNOWN:     return GameStatus.GAME_STATUS_UNKNOWN;
            //case Referee.hoxGAME_STATUS_OPEN:      return "Open";
            case Referee.hoxGAME_STATUS_READY:       return GameStatus.GAME_STATUS_IN_PROGRESS;
            case Referee.hoxGAME_STATUS_IN_PROGRESS: return GameStatus.GAME_STATUS_IN_PROGRESS;
            case Referee.hoxGAME_STATUS_RED_WIN:     return GameStatus.GAME_STATUS_RED_WIN;
            case Referee.hoxGAME_STATUS_BLACK_WIN:   return GameStatus.GAME_STATUS_BLACK_WIN;
            case Referee.hoxGAME_STATUS_DRAWN:       return GameStatus.GAME_STATUS_DRAWN;
            default: return Enums.GameStatus.GAME_STATUS_UNKNOWN;
        }
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
