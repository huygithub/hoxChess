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

import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

/**
 * The controller that manages an empty table.
 */
public class EmptyTableController extends BaseTableController {

    private static final String TAG = "EmptyTableController";
    
    public EmptyTableController() {
        Log.v(TAG, "[CONSTRUCTOR]: ...");
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Context context, Menu menu) {
        menu.findItem(R.id.action_logout).setVisible(HoxApp.getApp().isOnline());
        menu.findItem(R.id.action_new_table).setVisible(true);
        menu.findItem(R.id.action_close_table).setVisible(false);
        return true; // display the menu
    }

    @Override
    public void onClick_resetTable(final Context context, View view) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.table_actions, popup.getMenu());

        popup.getMenu().removeItem(R.id.action_offer_draw);
        popup.getMenu().removeItem(R.id.action_offer_resign);
        popup.getMenu().removeItem(R.id.action_reset_table);
        popup.getMenu().removeItem(R.id.action_close_table);
        popup.getMenu().findItem(R.id.action_new_table).setVisible(true);

        if (popup.getMenu().size() == 0) {
            Log.i(TAG, "(on 'Reset' button click) No need to show popup menu!");
            return;
        }

        super.setupListenerForResetButton(context, popup);
        popup.show();
    }

    @Override
    public void handleRequestToOpenNewTable() {
        Log.i(TAG, "Request to open a new table...");

        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        AIEngine aiEngine = HoxApp.getApp().getAiEngine();
        NetworkController networkController = HoxApp.getApp().getNetworkController();

        // Case 1: I am not online at all.
        if (!networkController.isOnline() && !networkController.isMyTableValid()) {
            playerTracker.setTableType(TableType.TABLE_TYPE_LOCAL); // A new practice table.
            playerTracker.syncUI();
            aiEngine.initGame();
            timeTracker.stop();
            timeTracker.reset();
            MainActivity mainActivity = mainActivity_.get();
            if (mainActivity != null) {
                mainActivity.openNewPracticeTable();
                mainActivity.setTableController(TableType.TABLE_TYPE_LOCAL);
                mainActivity.invalidateOptionsMenu(); // Recreate the options menu
            }
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
    public void handleTableSelection(String tableId) {
        NetworkController networkController = HoxApp.getApp().getNetworkController();
        networkController.handleTableSelection(tableId);
    }

    @Override
    public void handleLogoutFromNetwork() {
        NetworkController networkController = HoxApp.getApp().getNetworkController();
        networkController.logoutFromNetwork();
    }

}
