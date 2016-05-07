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

import android.app.Activity;
import android.content.Context;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

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
        playerTracker.clearAllPlayers();
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
    public void onNetworkPlayerJoin(PlayerInfo playerInfo, Enums.ColorEnum playerColor,
                                    Enums.ColorEnum myNewColor) {
        Log.d(TAG, "onNetworkPlayerJoin: playerInfo = "  + playerInfo);
        MainActivity mainActivity = mainActivity_.get();

        if (myNewColor != ColorEnum.COLOR_UNKNOWN) { // my role has changed?
            if (mainActivity != null) {
                mainActivity.onLocalPlayerJoined(myNewColor);
            }
        }

        if (mainActivity != null) {
            mainActivity.onPlayerJoin(playerInfo.pid, playerInfo.rating, playerColor);
        }

        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        playerTracker.onPlayerJoin(playerInfo.pid, playerInfo.rating, playerColor);
        playerTracker.syncUI();
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
            playerTracker.setTableType(myTableType_);
            playerTracker.clearAllPlayers();
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
    public void onGameEnded(Enums.GameStatus gameStatus) {
        Log.d(TAG, "onGameEnded: gameStatus = "  + gameStatus);
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.onGameEnded(gameStatus);
        }
        HoxApp.getApp().getTimeTracker().stop();
        HoxApp.getApp().getPlayerTracker().syncUI();
    }

    @Override
    public void onGameReset() {
        Log.d(TAG, "onGameEnded:...");
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.onGameReset();
        }
        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        timeTracker.stop();
        timeTracker.reset();
    }

    @Override
    public void onGameDrawnRequested(String pid) {
        Log.d(TAG, "onGameDrawnRequested: pid = " + pid);
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.showGameMessage_DRAW(pid);
        }
    }

    @Override
    public void onPlayerInfoReceived(String pid, String rating, String wins, String draws, String losses) {
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.showBriefMessage(
                    mainActivity.getString(R.string.msg_player_record, pid, rating, wins, draws, losses),
                    Snackbar.LENGTH_LONG);
        }
    }

    @Override
    public void onPlayerRatingUpdate(String pid, String newRating) {
        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        playerTracker.onPlayerRatingUpdate(pid, newRating);
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
    public void handleTableMenuOnClick(final Context context, View view) {
        if (mainActivity_ == null || context != mainActivity_.get()) {
            throw new RuntimeException("The context must be the Main Activity");
        }

        MainActivity mainActivity = mainActivity_.get();
        final TableActionSheet actionSheet = new TableActionSheet(mainActivity);
        actionSheet.setHeaderText(getTitleForTableActionSheet());
        super.setupListenersInTableActionSheet(actionSheet);

        if (isTableEmpty()) {
            actionSheet.hideAction(TableActionSheet.Action.ACTION_RESET_TABLE);
            actionSheet.hideAction(TableActionSheet.Action.ACTION_REVERSE_BOARD);
            actionSheet.hideAction(TableActionSheet.Action.ACTION_CLOSE_TABLE);
            actionSheet.hideAction(TableActionSheet.Action.ACTION_OFFER_DRAW);
            actionSheet.hideAction(TableActionSheet.Action.ACTION_OFFER_RESIGN);

        } else {
            final boolean isGameOver = HoxApp.getApp().isGameOver();
            final ColorEnum myColor = HoxApp.getApp().getNetworkController().getMyColor();
            final boolean amIPlaying = (myColor == ColorEnum.COLOR_BLACK || myColor == ColorEnum.COLOR_RED);
            final int moveCount = HoxApp.getApp().getReferee().getMoveCount();

            actionSheet.hideAction(TableActionSheet.Action.ACTION_NEW_TABLE);
            if (isGameOver) {
                actionSheet.hideAction(TableActionSheet.Action.ACTION_OFFER_DRAW);
                actionSheet.hideAction(TableActionSheet.Action.ACTION_OFFER_RESIGN);
            } else if (!amIPlaying) {
                actionSheet.hideAction(TableActionSheet.Action.ACTION_RESET_TABLE);
                actionSheet.hideAction(TableActionSheet.Action.ACTION_OFFER_DRAW);
                actionSheet.hideAction(TableActionSheet.Action.ACTION_OFFER_RESIGN);
            } else if (moveCount >= 2) { // game has started?
                actionSheet.hideAction(TableActionSheet.Action.ACTION_RESET_TABLE);
                actionSheet.hideAction(TableActionSheet.Action.ACTION_CLOSE_TABLE);
            } else {
                actionSheet.hideAction(TableActionSheet.Action.ACTION_OFFER_DRAW);
                actionSheet.hideAction(TableActionSheet.Action.ACTION_OFFER_RESIGN);
            }
        }

        actionSheet.show();
    }

    @Override
    public void handlePlayerOnClickInTable(PlayerInfo playerInfo, String tableId) {
        HoxApp.getApp().getNetworkController().handleRequestToGetPlayerInfo(playerInfo.pid);

        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            PlayersInTableSheetDialog dialog = new PlayersInTableSheetDialog(mainActivity, playerInfo);
            dialog.show();
        }
    }

    @Override
    public void handleRequestToOpenNewTable() {
        Log.d(TAG, "Request to open a new table...");

        NetworkController networkController = HoxApp.getApp().getNetworkController();

        // Case 1: I am not online at all.
        if (!networkController.isOnline() && !networkController.isMyTableValid()) {
            BaseTableController.setCurrentController(Enums.TableType.TABLE_TYPE_LOCAL);
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

        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        timeTracker.nextColor();
        timeTracker.start();
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

        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        timeTracker.setInitialColor(HoxApp.getApp().getReferee().getNextColor());
        timeTracker.start();
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

    private String getTitleForTableActionSheet() {
        Context context = HoxApp.getApp();

        String tableHeaderTitle;
        if (isTableEmpty()) {
            tableHeaderTitle = context.getString(R.string.logged_in_player_info,
                    HoxApp.getApp().getMyPid(),
                    HoxApp.getApp().getNetworkController().getMyRating_());
        } else {
            TableInfo tableInfo = HoxApp.getApp().getNetworkController().getMyTableInfo();
            tableHeaderTitle = context.getString(R.string.table_network_info,
                    tableInfo.tableId, tableInfo.itimes);
        }
        return tableHeaderTitle;
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
        playerTracker.clearAllPlayers();
        playerTracker.syncUI();
    }

    private static class PlayersInTableSheetDialog extends BottomSheetDialog {
        public PlayersInTableSheetDialog(final Activity activity, PlayerInfo playerInfo) {
            super(activity);

            final String playerId = playerInfo.pid;
            View sheetView = activity.getLayoutInflater().inflate(R.layout.sheet_dialog_player, null);
            setContentView(sheetView);

            TextView playerInfoView = (TextView) sheetView.findViewById(R.id.sheet_player_info);
            View sendMessageView = sheetView.findViewById(R.id.sheet_send_private_message);
            View inviteView = sheetView.findViewById(R.id.sheet_invite_to_play);
            View joinView = sheetView.findViewById(R.id.sheet_join_table_of_player);

            playerInfoView.setText(
                    activity.getString(R.string.msg_player_info, playerId, playerInfo.rating));

            sendMessageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (activity instanceof MainActivity) {
                        ((MainActivity)activity).showBriefMessage("Not yet implement Send Personal Message",
                                Snackbar.LENGTH_SHORT);
                    }
                    dismiss(); // this the dialog.
                }
            });

            inviteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HoxApp.getApp().getNetworkController().handleRequestToInvite(playerId);
                    dismiss(); // this the dialog.
                }
            });

            joinView.setVisibility(View.GONE);
        }
    }

}
