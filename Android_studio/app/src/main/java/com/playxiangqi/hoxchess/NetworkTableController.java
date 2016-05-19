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

import com.playxiangqi.hoxchess.Enums.ColorEnum;

import android.util.Log;

/**
 * The controller that manages a network table.
 */
public class NetworkTableController {

    private static final String TAG = "NetworkTableController";

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // NOTE: The referee (JNI based) has a limitation that it has ONLY one instance created!
    //    For more details, see ./app/src/main/jni/Referee.cpp
    //    and pay attention to the "static hoxReferee *referee_".
    // As a result, we must share the global referee under HoxApp.
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    private Referee referee_;

    private Enums.TableType myTableType_ = Enums.TableType.TABLE_TYPE_EMPTY;
    private final TableTimeTracker timeTracker_ = new TableTimeTracker();
    private final TablePlayerTracker playerTracker_ = new TablePlayerTracker(Enums.TableType.TABLE_TYPE_EMPTY);

    // ***************************
    private BoardController boardController_;

    public interface BoardController {
        // APIs that are required for AI/Practice tables.
        //void reverseBoardView();
        void updateBoardWithNewMove(MoveInfo move);

        // APIs that are required for Network tables.
        void updateBoardWithNewTableInfo(TableInfo tableInfo);
        void resetBoardWithNewMoves(MoveInfo[] moves);
        void clearTable();
        void onLocalPlayerJoined(Enums.ColorEnum myColor);
        void onPlayerJoin(String pid, String rating, Enums.ColorEnum playerColor);
        void onPlayerLeave(String pid);
        void showGameMessage_DRAW(String pid);
        void onGameEnded(Enums.GameStatus gameStatus);
        void onGameReset();
        void onPlayerInfoReceived(String pid, String rating, String wins, String draws, String losses);
        void onJoinTableError(String errorMessage, Enums.ErrorCode errorCode);
        void onNetworkError();
    }
    public void setBoardController(BoardController controller) {
        boardController_ = controller;
    }
    // ***************************

    // The singleton instance.
    private static NetworkTableController instance_;

    /** Singleton API to return the instance. */
    public static NetworkTableController getInstance() {
        if (instance_ == null) {
            instance_ = new NetworkTableController();
        }
        return instance_;
    }

    /** Constructor */
    private NetworkTableController() {
        Log.v(TAG, "[CONSTRUCTOR]: ...");
    }

    public void setReferee(Referee referee) { referee_ = referee; }

    public Referee getReferee() { return referee_; }
    public TableTimeTracker getTimeTracker() { return timeTracker_; }
    public TablePlayerTracker getPlayerTracker() { return playerTracker_; }

    public int getMoveCount() {
        return referee_.getMoveCount();
    }

    public boolean isGameInProgress() {
        return ( !HoxApp.getApp().getNetworkController().isGameOver() &&
                 referee_.getMoveCount() > 1 );
    }

    public void onNetworkTableEnter(TableInfo tableInfo) {
        Log.d(TAG, "onNetworkTableEnter: Set table-type to NETWORK...");
        myTableType_ = Enums.TableType.TABLE_TYPE_NETWORK;

        timeTracker_.stop();
        timeTracker_.setInitialColor(ColorEnum.COLOR_RED);
        timeTracker_.setInitialTime( new TimeInfo(tableInfo.itimes) );
        timeTracker_.setBlackTime( new TimeInfo(tableInfo.blackTimes) );
        timeTracker_.setRedTime( new TimeInfo(tableInfo.redTimes) );
        timeTracker_.syncUI();

        playerTracker_.setTableType(myTableType_);
        playerTracker_.setBlackInfo(tableInfo.blackId, tableInfo.blackRating);
        playerTracker_.setRedInfo(tableInfo.redId, tableInfo.redRating);
        playerTracker_.setObservers(tableInfo.observers);
        playerTracker_.syncUI();

        if (boardController_ != null) {
            boardController_.updateBoardWithNewTableInfo(tableInfo);
        }
    }

    public void onNetworkPlayerJoin(PlayerInfo playerInfo, Enums.ColorEnum playerColor,
                                    Enums.ColorEnum myNewColor) {
        Log.d(TAG, "onNetworkPlayerJoin: playerInfo = "  + playerInfo);

        if (myNewColor != ColorEnum.COLOR_UNKNOWN) { // my role has changed?
            if (boardController_ != null) {
                boardController_.onLocalPlayerJoined(myNewColor);
            }
        }

        if (boardController_ != null) {
            boardController_.onPlayerJoin(playerInfo.pid, playerInfo.rating, playerColor);
        }

        playerTracker_.onPlayerJoin(playerInfo.pid, playerInfo.rating, playerColor);
        playerTracker_.syncUI();
    }

    public void onNetworkPlayerLeave(String pid) {
        Log.d(TAG, "onNetworkPlayerLeave: PlayerId = "  + pid);

        // Check if I just left the Table.
        if (pid.equals(HoxApp.getApp().getMyPid())) {
            Log.i(TAG, "I just left my table...");

            Log.d(TAG, "onNetworkPlayerLeave: Set table-type to EMPTY...");
            myTableType_ = Enums.TableType.TABLE_TYPE_EMPTY;

            timeTracker_.stop();
            playerTracker_.setTableType(myTableType_);
            playerTracker_.clearAllPlayers();
            if (boardController_ != null) {
                boardController_.clearTable();
            }

        } else { // Other player left my table?
            playerTracker_.onPlayerLeave(pid);
            if (boardController_ != null) {
                boardController_.onPlayerLeave(pid);
            }
        }

        playerTracker_.syncUI();
    }

    public void onGameEnded(Enums.GameStatus gameStatus) {
        Log.d(TAG, "onGameEnded: gameStatus = "  + gameStatus);
        if (boardController_ != null) {
            boardController_.onGameEnded(gameStatus);
        }
        timeTracker_.stop();
        playerTracker_.syncUI();
    }

    public void onGameReset() {
        Log.d(TAG, "onGameReset:...");
        if (boardController_ != null) {
            boardController_.onGameReset();
        }
        timeTracker_.stop();
        timeTracker_.reset();
    }

    public void onGameDrawnRequested(String pid) {
        Log.d(TAG, "onGameDrawnRequested: pid = " + pid);
        if (boardController_ != null) {
            boardController_.showGameMessage_DRAW(pid);
        }
    }

    public void onPlayerInfoReceived(String pid, String rating, String wins, String draws, String losses) {
        if (boardController_ != null) {
            boardController_.onPlayerInfoReceived(pid, rating, wins, draws, losses);
        }
    }

    public void onPlayerRatingUpdate(String pid, String newRating) {
        playerTracker_.onPlayerRatingUpdate(pid, newRating);
        playerTracker_.syncUI();
    }

    public void handlePlayerButtonClick(Enums.ColorEnum clickedColor) {
        Log.d(TAG, "Handle player-button click. clickedColor = " + clickedColor);
        NetworkController networkController = HoxApp.getApp().getNetworkController();
        networkController.handleMyRequestToChangeRole(clickedColor);
    }

    public void onNetworkMove(MoveInfo move) {
        move.gameStatus = referee_.validateMove(
                move.fromPosition.row, move.fromPosition.column,
                move.toPosition.row, move.toPosition.column);

        if (move.gameStatus == Referee.hoxGAME_STATUS_UNKNOWN) { // Move is not valid?
            Log.e(TAG, "[SINGLE] This move =" + move + " is NOT valid. Do nothing.");
            return;
        }

        if (boardController_ != null) {
            boardController_.updateBoardWithNewMove(move);
        }

        timeTracker_.nextColor();
        timeTracker_.start();
    }

    public void onResetBoardWithMoves(MoveInfo[] moves) {
        for (MoveInfo move : moves) {
            move.gameStatus = referee_.validateMove(
                    move.fromPosition.row, move.fromPosition.column,
                    move.toPosition.row, move.toPosition.column);

            if (move.gameStatus == Referee.hoxGAME_STATUS_UNKNOWN) { // Move is not valid?
                Log.e(TAG, "[LIST] This move =" + move + " is NOT valid. Do nothing.");
                return;
            }
        }

        if (boardController_ != null) {
            boardController_.resetBoardWithNewMoves(moves);
        }

        timeTracker_.setInitialColor(referee_.getNextColor());
        timeTracker_.start();
    }

    public void onJoinTableError(String errorMessage, Enums.ErrorCode errorCode) {
        Log.d(TAG, "onJoinTableError:...");
        if (boardController_ != null) {
            boardController_.onJoinTableError(errorMessage, errorCode);
        }
    }

    public void onNetworkCode(int errorMessageResId) {
        Log.w(TAG, "onNetworkCode:..."); // FIXME
    }

    public void onNetworkError() {
        Log.w(TAG, "onNetworkCode:...");
        if (boardController_ != null) {
            boardController_.onNetworkError();
        }
    }

    // ***************************************************************************
    //
    //         Private APIs
    //
    // ***************************************************************************

}
