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
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

/**
 * The controller that manages a network table.
 */
public class NetworkTableController extends BaseTableController {

    private static final String TAG = "NetworkTableController";
    
    public NetworkTableController() {
        Log.v(TAG, "[CONSTRUCTOR]: ...");
    }

    @Override
    public boolean handleBackPressed() {
        NetworkController networkController = HoxApp.getApp().getNetworkController();
        networkController.handleRequestToCloseCurrentTable();
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Context context, Menu menu) {
        NetworkController networkController = HoxApp.getApp().getNetworkController();

        menu.findItem(R.id.action_logout).setVisible(HoxApp.getApp().isOnline());

        final ColorEnum myColor = networkController.getMyColor();
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
        return true; // display the menu
    }

    @Override
    public void onClick_resetTable(final Context context, View view) {
        NetworkController networkController = HoxApp.getApp().getNetworkController();

        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.table_actions, popup.getMenu());

        final boolean isGameOver = HoxApp.getApp().isGameOver();
        final ColorEnum myColor = networkController.getMyColor();
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
        }  else if (moveCount >= 2) { // game has started?
            popup.getMenu().removeItem(R.id.action_reset_table);
            popup.getMenu().removeItem(R.id.action_close_table);
        } else {
            popup.getMenu().removeItem(R.id.action_offer_draw);
            popup.getMenu().removeItem(R.id.action_offer_resign);
        }

        if (popup.getMenu().size() == 0) {
            Log.i(TAG, "(on 'Reset' button click) No need to show popup menu!");
            return;
        }

        super.setupListenerForResetButton(context, popup);
        popup.show();
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
        NetworkController networkController = HoxApp.getApp().getNetworkController();
        networkController.logoutFromNetwork();
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

}
