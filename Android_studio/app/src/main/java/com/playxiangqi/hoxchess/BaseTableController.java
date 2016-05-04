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

import java.lang.ref.WeakReference;

import com.playxiangqi.hoxchess.Enums.TableType;

import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

/**
 * The base controller to manage a table.
 */
public class BaseTableController implements BoardView.BoardEventListener {

    private static final String TAG = "BaseTableController";
    private boolean DEBUG_LIFE_CYCLE = false;

    // Shared table controllers.
    private static LocalTableController localTableController_;
    private static NetworkTableController networkTableController_;

    private static BaseTableController currentController_;

    protected WeakReference<MainActivity> mainActivity_ = new WeakReference<MainActivity>(null);

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

    public void setupNewTable() {
    }

    public void onNetworkLoginSuccess() {
    }

    public void onNetworkError() {
    }

    public void onNetworkTableEnter(TableInfo tableInfo) {
    }

    public void onNetworkPlayerLeave(String pid) {
    }

    public void setTableTitle() {
    }

    public boolean handleBackPressed() {
        return false;
    }

    public boolean onPrepareOptionsMenu(Context context, Menu menu) {
        return false; // Do not display the menu
    }

    public void onClick_resetTable(final Context context, View view) {
    }

    public void handleRequestToOpenNewTable() {
    }

    public boolean isMyTurn() {
        return false;
    }

    public void handlePlayerButtonClick(Enums.ColorEnum clickedColor) {
    }

    public void handleTableSelection(String tableId) {
    }

    public void handleRequestToCloseCurrentTable() {
    }

    public void handleLogoutFromNetwork() {
    }

    public void onTableClear() {
    }

    public void onNetworkMove(MoveInfo move) {
    }

    public void onResetBoardWithMoves(MoveInfo[] moves) {
    }

    @Override
    public void onLocalMove(Position fromPos, Position toPos) {
    }

    // ***************************************************************
    //
    //              Other public APIs
    //
    // ***************************************************************

    public void onMainActivityCreate(MainActivity activity) {
        Log.d(TAG, "onMainActivityCreate:...");
        mainActivity_ = new WeakReference<MainActivity>(activity);
        activity.setTableController(this);
    }

    public void onMainActivityDestroy(MainActivity activity) {
        Log.d(TAG, "onMainActivityDestroy:...");
        if (mainActivity_.get() == activity) {
            mainActivity_ = new WeakReference<MainActivity>(null);
        }
    }

    // ***************************************************************
    //
    //              Other protected APIs
    //
    // ***************************************************************

    protected void setMainActivity(MainActivity activity) {
        mainActivity_ = new WeakReference<MainActivity>(activity);
    }

    /* default */ MainActivity getMainActivity() {
        return mainActivity_.get();
    }

    /* default */ void setMainActivityFromController(BaseTableController otherController) {
        mainActivity_ = new WeakReference<MainActivity>(otherController.getMainActivity());
    }

    protected void setupListenerForResetButton(final Context context, PopupMenu popup) {
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_offer_draw:
                        handleRequestToOfferDraw();
                        Toast.makeText(HoxApp.getApp(),
                                context.getString(R.string.action_draw),
                                Toast.LENGTH_SHORT).show();
                        return true;

                    case R.id.action_offer_resign:
                        handleRequestToOfferResign();
                        Toast.makeText(HoxApp.getApp(),
                                context.getString(R.string.action_resign),
                                Toast.LENGTH_SHORT).show();
                        return true;

                    case R.id.action_reset_table:
                        handleRequestToResetTable();
                        return true;

                    case R.id.action_close_table:
                        handleRequestToCloseCurrentTable();
                        return true;

                    case R.id.action_new_table:
                        handleRequestToOpenNewTable();
                        return true;

                    default:
                        return true;
                }
            }
        });
    }

    protected void handleRequestToResetTable() {
    }

    protected void handleRequestToOfferDraw() {
    }

    protected void handleRequestToOfferResign() {
    }

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
            case TABLE_TYPE_EMPTY:
                // falls through. Network controller will take care the empty table!
            case TABLE_TYPE_NETWORK:
                if (networkTableController_ == null) {
                    networkTableController_ = new NetworkTableController();
                }
                controller = networkTableController_;
                break;

            case TABLE_TYPE_LOCAL: // falls through
            default:
                if (localTableController_ == null) {
                    localTableController_ = new LocalTableController();
                }
                controller = localTableController_;
                break;
        }

        return controller;
    }

    public static void setCurrentController(TableType tableType) {
        BaseTableController oldController = currentController_;
        currentController_ = getTableController(tableType);

        // Transfer the UI control to the new controller.
        if (oldController != null && oldController != currentController_) {
            currentController_.setMainActivityFromController(oldController);
        }
    }

    public static BaseTableController getCurrentController() {
        return currentController_;
    }

}
