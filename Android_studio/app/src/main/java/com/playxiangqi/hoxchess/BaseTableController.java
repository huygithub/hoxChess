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

import com.playxiangqi.hoxchess.Enums.TableType;

import android.util.Log;

/**
 * The base controller to manage a table.
 */
public class BaseTableController {

    private static final String TAG = "BaseTableController";
    private boolean DEBUG_LIFE_CYCLE = false;

    // Shared table controllers.
    private static NetworkTableController networkTableController_;

    protected BoardController boardController_;

    // ***************************
    public interface BoardController {
        // APIs that are required for AI/Practice tables.
        void reverseBoardView();
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
    }
    public void setBoardController(BoardController controller) {
        boardController_ = controller;
    }
    // ***************************

    /**
     * Constructor
     */
    public BaseTableController() {
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "[CONSTRUCTOR]: ...");
    }

    // ***************************************************************
    //
    //            APIs that are to be overridden
    //
    // ***************************************************************

    public void onNetworkCode(int errorMessageResId) {
        Log.w(TAG, "onNetworkCode:...");
        // TODO: MainActivity mainActivity = mainActivity_.get();
        //if (mainActivity != null) {
        //    mainActivity.showBriefMessage(errorMessageResId, Snackbar.LENGTH_SHORT);
        //}
    }

    public void onNetworkError() {
    }

    public void onNetworkTableEnter(TableInfo tableInfo) {
    }

    public void onNetworkPlayerJoin(PlayerInfo playerInfo, Enums.ColorEnum playerColor,
                                    Enums.ColorEnum myNewColor) {
    }

    public void onNetworkPlayerLeave(String pid) {
    }

    public void onGameEnded(Enums.GameStatus gameStatus) {
    }

    public void onGameReset() {
    }

    public void onGameDrawnRequested(String pid) {
    }

    public void onPlayerInfoReceived(String pid, String rating, String wins, String draws, String losses) {
    }

    public void onPlayerRatingUpdate(String pid, String newRating) {
    }

    public void handlePlayerButtonClick(Enums.ColorEnum clickedColor) {
    }

    public void onNetworkMove(MoveInfo move) {
    }

    public void onResetBoardWithMoves(MoveInfo[] moves) {
    }

    // ***************************************************************
    //
    //              Other public APIs
    //
    // ***************************************************************



    // ***************************************************************
    //
    //              Other protected APIs
    //
    // ***************************************************************


    // ***************************************************************
    //
    //              Static APIs
    //
    // ***************************************************************

    /**
     * The factory method for table controllers.
     */
    private static BaseTableController getTableController(TableType tableType) {
        BaseTableController controller;
        switch (tableType) {
            case TABLE_TYPE_LOCAL: // FIXME: falls through. We need to remove this.
            case TABLE_TYPE_EMPTY:
                // falls through. Network controller will take care the empty table!
            case TABLE_TYPE_NETWORK:
                if (networkTableController_ == null) {
                    networkTableController_ = new NetworkTableController();
                }
                controller = networkTableController_;
                break;
            default:
                throw new RuntimeException("Unsupported table-type = " + tableType);
        }

        return controller;
    }

    public static NetworkTableController getNetworkController() {
        return (NetworkTableController) getTableController(TableType.TABLE_TYPE_NETWORK);
    }

}
