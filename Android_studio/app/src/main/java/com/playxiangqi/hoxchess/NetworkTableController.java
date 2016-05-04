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

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

/**
 * The controller that manages a network table.
 */
public class NetworkTableController extends BaseTableController {

    private static final String TAG = "NetworkTableController";

    private Enums.TableType myTableType_ = Enums.TableType.TABLE_TYPE_EMPTY;

    public NetworkTableController() {
        Log.v(TAG, "[CONSTRUCTOR]: ...");
    }

    @Override
    public void onNetworkLoginSuccess() {
        Log.d(TAG, "onNetworkLoginSuccess: Set table-type to EMPTY...");
        myTableType_ = Enums.TableType.TABLE_TYPE_EMPTY;

        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        playerTracker.setTableType(myTableType_);
        playerTracker.syncUI();

        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.setTableController(this);
            mainActivity.clearTable();
            mainActivity.onLoginSuccess();
        }
    }

    @Override
    public void onNetworkError() {
        Log.d(TAG, "onNetworkError:...");
        clearCurrentTableIfNeeded();

        // Attempt to login again if we are observing a network table.
        if (!isTableEmpty()) {
            HoxApp.getApp().getNetworkController().connectToServer();

        } else if (HoxApp.getApp().getNetworkController().isLoginOK()) {
            // NOTE: Only show this error while being logged in! Otherwise, login-related errors,
            //       such as "Wrong password" message, may be suppressed.
            MainActivity mainActivity = mainActivity_.get();
            if (mainActivity != null) {
                mainActivity.showBriefMessage(R.string.msg_network_error_io_exception_exception, Snackbar.LENGTH_SHORT);
            }
        }
    }

    @Override
    public void onNetworkTableEnter(TableInfo tableInfo) {
        Log.d(TAG, "onNetworkTableEnter: Set table-type to NETWORK...");
        myTableType_ = Enums.TableType.TABLE_TYPE_NETWORK;

        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        timeTracker.stop();
        timeTracker.setInitialColor(ColorEnum.COLOR_RED);
        timeTracker.setInitialTime( new TimeInfo(tableInfo.itimes) );
        timeTracker.setBlackTime( new TimeInfo(tableInfo.blackTimes) );
        timeTracker.setRedTime( new TimeInfo(tableInfo.redTimes) );
        timeTracker.syncUI();

        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        playerTracker.setTableType(myTableType_);
        playerTracker.setBlackInfo(tableInfo.blackId, tableInfo.blackRating);
        playerTracker.setRedInfo(tableInfo.redId, tableInfo.redRating);
        playerTracker.setObservers(tableInfo.observers);
        playerTracker.syncUI();

        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.updateBoardWithNewTableInfo(tableInfo);
        }
    }

    @Override
    public void onNetworkPlayerLeave(String pid) {
        Log.d(TAG, "onNetworkPlayerLeave: PlayerId = "  + pid);

        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        MainActivity mainActivity = mainActivity_.get();

        // Check if I just left the Table.
        if (pid.equals(HoxApp.getApp().getMyPid())) {
            Log.i(TAG, "I just left my table...");

            Log.d(TAG, "onNetworkPlayerLeave: Set table-type to EMPTY...");
            myTableType_ = Enums.TableType.TABLE_TYPE_EMPTY;

            HoxApp.getApp().getTimeTracker().stop();
            playerTracker.clearAllPlayers();
            playerTracker.setTableType(myTableType_);
            if (mainActivity != null) {
                mainActivity.clearTable();
            }

        } else { // Other player left my table?
            playerTracker.onPlayerLeave(pid);
            if (mainActivity != null) {
                mainActivity.onPlayerLeave(pid);
            }
        }

        playerTracker.syncUI();
    }

    @Override
    public void setTableTitle() {
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.setAndShowTitle(
                    HoxApp.getApp().getNetworkController().getMyTableId());
        }
    }

    @Override
    public boolean handleBackPressed() {
        if (isTableEmpty()) return false;
        handleRequestToCloseCurrentTable();
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Context context, Menu menu) {
        if (isTableEmpty()) {
            menu.findItem(R.id.action_new_table).setVisible(true);
            menu.findItem(R.id.action_close_table).setVisible(false);
        } else {
            final ColorEnum myColor = HoxApp.getApp().getNetworkController().getMyColor();
            final boolean isGameOver = HoxApp.getApp().isGameOver();
            final int moveCount = HoxApp.getApp().getReferee().getMoveCount();

            menu.findItem(R.id.action_new_table).setVisible(false);

            if (myColor == ColorEnum.COLOR_BLACK || myColor == ColorEnum.COLOR_RED) {
                if (moveCount >= 2) { // The game has actually started?
                    menu.findItem(R.id.action_close_table).setVisible(isGameOver);
                } else {
                    menu.findItem(R.id.action_close_table).setVisible(true);
                }
            } else {
                menu.findItem(R.id.action_close_table).setVisible(true);
            }
        }

        return true; // display the menu
    }

    @Override
    public void onClick_resetTable(final Context context, View view) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.table_actions, popup.getMenu());

        if (isTableEmpty()) {
            popup.getMenu().removeItem(R.id.action_offer_draw);
            popup.getMenu().removeItem(R.id.action_offer_resign);
            popup.getMenu().removeItem(R.id.action_reset_table);
            popup.getMenu().removeItem(R.id.action_close_table);
            popup.getMenu().findItem(R.id.action_new_table).setVisible(true);

        } else {
            final boolean isGameOver = HoxApp.getApp().isGameOver();
            final ColorEnum myColor = HoxApp.getApp().getNetworkController().getMyColor();
            final boolean amIPlaying = (myColor == ColorEnum.COLOR_BLACK || myColor == ColorEnum.COLOR_RED);
            final int moveCount = HoxApp.getApp().getReferee().getMoveCount();

            if (isGameOver) {
                popup.getMenu().removeItem(R.id.action_offer_draw);
                popup.getMenu().removeItem(R.id.action_offer_resign);
                popup.getMenu().removeItem(R.id.action_close_table);
            } else if (!amIPlaying) {
                popup.getMenu().removeItem(R.id.action_offer_draw);
                popup.getMenu().removeItem(R.id.action_offer_resign);
                popup.getMenu().removeItem(R.id.action_reset_table);
            } else if (moveCount >= 2) { // game has started?
                popup.getMenu().removeItem(R.id.action_reset_table);
                popup.getMenu().removeItem(R.id.action_close_table);
            } else {
                popup.getMenu().removeItem(R.id.action_offer_draw);
                popup.getMenu().removeItem(R.id.action_offer_resign);
            }
        }

        if (popup.getMenu().size() == 0) {
            Log.i(TAG, "(on 'Reset' button click) No need to show popup menu!");
            return;
        }

        super.setupListenerForResetButton(context, popup);
        popup.show();
    }

    @Override
    public void handleRequestToOpenNewTable() {
        Log.d(TAG, "Request to open a new table...");

        NetworkController networkController = HoxApp.getApp().getNetworkController();

        // Case 1: I am not online at all.
        if (!networkController.isOnline() && !networkController.isMyTableValid()) {
            BaseTableController.setCurrentController(Enums.TableType.TABLE_TYPE_LOCAL);

            // FIXME: Transfer the UI control to the new controller.
            BaseTableController.getCurrentController().setMainActivity(mainActivity_.get());

            BaseTableController.getCurrentController().handleRequestToOpenNewTable();
        }
        // Case 2: I am online and am not playing in any table.
        else if (networkController.isOnline()) {
            networkController.handleMyRequestToOpenNewTable();
        }
        else {
            Log.w(TAG, "Either offline or not playing. Ignore this 'open new table request'.");
        }
    }

    @Override
    public boolean isMyTurn() {
        Referee referee = HoxApp.getApp().getReferee();
        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        NetworkController networkController = HoxApp.getApp().getNetworkController();

        final ColorEnum myColor = networkController.getMyColor();
        return ((myColor == ColorEnum.COLOR_RED || myColor == ColorEnum.COLOR_BLACK) &&
                playerTracker.hasEnoughPlayers() &&
                myColor == referee.getNextColor());
    }

    @Override
    public void handlePlayerButtonClick(Enums.ColorEnum clickedColor) {
        Log.d(TAG, "Handle player-button click. clickedColor = " + clickedColor);
        NetworkController networkController = HoxApp.getApp().getNetworkController();
        networkController.handleMyRequestToChangeRole(clickedColor);
    }

    @Override
    public void handleTableSelection(String tableId) {
        NetworkController networkController = HoxApp.getApp().getNetworkController();
        networkController.handleTableSelection(tableId);
    }

    @Override
    public void handleRequestToCloseCurrentTable() {
        NetworkController networkController = HoxApp.getApp().getNetworkController();
        networkController.handleRequestToCloseCurrentTable();
    }

    @Override
    public void handleLogoutFromNetwork() {
        Log.d(TAG, "handleLogoutFromNetwork: ...");
        clearCurrentTableIfNeeded();
        HoxApp.getApp().getNetworkController().logoutFromNetwork();
    }

    @Override
    public void onTableClear() {
        MessageManager.getInstance().removeMessages(MessageInfo.MessageType.MESSAGE_TYPE_CHAT_IN_TABLE);
    }

    @Override
    public void onNetworkMove(MoveInfo move) {
        move.gameStatus = HoxApp.getApp().getReferee().validateMove(
                move.fromPosition.row, move.fromPosition.column,
                move.toPosition.row, move.toPosition.column);

        if (move.gameStatus == Referee.hoxGAME_STATUS_UNKNOWN) { // Move is not valid?
            Log.e(TAG, "[SINGLE] This move =" + move + " is NOT valid. Do nothing.");
            return;
        }

        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.updateBoardWithNewMove(move);
        }
    }

    @Override
    public void onResetBoardWithMoves(MoveInfo[] moves) {
        for (MoveInfo move : moves) {
            move.gameStatus = HoxApp.getApp().getReferee().validateMove(
                    move.fromPosition.row, move.fromPosition.column,
                    move.toPosition.row, move.toPosition.column);

            if (move.gameStatus == Referee.hoxGAME_STATUS_UNKNOWN) { // Move is not valid?
                Log.e(TAG, "[LIST] This move =" + move + " is NOT valid. Do nothing.");
                return;
            }
        }

        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.resetBoardWithNewMoves(moves);
        }
    }

    @Override
    public void onLocalMove(Position fromPos, Position toPos) {
        Referee referee = HoxApp.getApp().getReferee();
        Log.i(TAG, "Handle local move: referee 's moveCount = " + referee.getMoveCount());

        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        NetworkController networkController = HoxApp.getApp().getNetworkController();

        timeTracker.nextColor();

        if (referee.getMoveCount() == 2) {
            timeTracker.start();
            MainActivity mainActivity = mainActivity_.get();
            if (mainActivity != null) {
                mainActivity.onGameStatusChanged();
            }
        }

        if (referee.getMoveCount() > 1) { // The game has started?
            playerTracker.syncUI();
        }

        networkController.handleRequestToSendMove(fromPos, toPos);
    }

    @Override
    protected void handleRequestToResetTable() {
        Log.i(TAG, "Handle request to 'Reset Table'...");
        NetworkController networkController = HoxApp.getApp().getNetworkController();
        networkController.handleRequestToResetTable();
    }

    @Override
    protected void handleRequestToOfferDraw() {
        NetworkController networkController = HoxApp.getApp().getNetworkController();
        networkController.handleRequestToOfferDraw();
    }

    @Override
    protected void handleRequestToOfferResign() {
        NetworkController networkController = HoxApp.getApp().getNetworkController();
        networkController.handleRequestToOfferResign();
    }

    // ***************************************************************************
    //
    //         Private APIs
    //
    // ***************************************************************************

    private boolean isTableEmpty() {
        return (myTableType_ == Enums.TableType.TABLE_TYPE_EMPTY);
    }

    private void clearCurrentTableIfNeeded() {
        if (!isTableEmpty()) { // Are we in a network table?
            Log.d(TAG, "Clear the current table...");
            HoxApp.getApp().getTimeTracker().stop();
            MainActivity mainActivity = mainActivity_.get();
            if (mainActivity != null) {
                mainActivity.clearTable();
            }
        }

        Log.d(TAG, "clearCurrentTableIfNeeded: Set table-type to EMPTY...");
        myTableType_ = Enums.TableType.TABLE_TYPE_EMPTY;

        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        playerTracker.setTableType(myTableType_);
        playerTracker.syncUI();
    }

}
