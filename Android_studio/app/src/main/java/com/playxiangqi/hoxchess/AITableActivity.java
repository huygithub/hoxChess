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

import android.content.Intent;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import java.lang.ref.WeakReference;

public class AITableActivity extends AppCompatActivity
                implements BoardFragment.OnFragmentInteractionListener,
                           MessageManager.EventListener,
                           BaseTableController.BoardController {

    private static final String TAG = "AITableActivity";

    private WeakReference<BoardFragment> myBoardFragment_ = new WeakReference<BoardFragment>(null);

    // TODO: We should persist this counter somewhere else because it is lost when the
    //       device is rotated, for example.
    private int notifCount_ = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
        setContentView(R.layout.activity_ai_table);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Check that the activity is using the layout version with
        // the main_container FrameLayout
        if (findViewById(R.id.board_container) != null) {
            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            BoardFragment boardFragment = BoardFragment.newInstance("AI");

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.board_container, boardFragment).commit();
        }

        BaseTableController.setCurrentController(Enums.TableType.TABLE_TYPE_LOCAL);
        BaseTableController.getCurrentController().setBoardController(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        MessageManager.getInstance().addListener(this);
        adjustScreenOnFlagBasedOnGameStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        MessageManager.getInstance().removeListener(this);
        adjustScreenOnFlagBasedOnGameStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "(ActionBar) onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);

        // Set up the notification item.
        // Reference:
        //  http://stackoverflow.com/questions/18156477/how-to-make-an-icon-in-the-action-bar-with-the-number-of-notification
        MenuItem item = menu.findItem(R.id.action_notifications);
        LayerDrawable icon = (LayerDrawable) item.getIcon();
        BadgeDrawable.setBadgeCount(this, icon, notifCount_);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "(ActionBar) onPrepareOptionsMenu");
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_new_table).setVisible(false);
        menu.findItem(R.id.action_close_table).setVisible(false);
        menu.findItem(R.id.action_view_tables).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "(ActionBar) onOptionsItemSelected");

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        //if (drawerToggle_.onOptionsItemSelected(item)) {
        //    return true;
        //}
        // Handle your other action bar items...

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            //case android.R.id.home:
            //    tableController_.handleRequestToCloseCurrentTable();
            //    return true;
            //case R.id.action_new_table:
            //    tableController_.handleRequestToOpenNewTable();
            //    return true;
            //case R.id.action_close_table:
            //    tableController_.handleRequestToCloseCurrentTable();
            //    return true;
            //case R.id.action_view_tables:
            //    onViewTablesClicked();
            //    return true;
            case R.id.action_notifications:
                openNotificationView();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // **** Implementation of MessageManager.EventListener ****
    @Override
    public void onMessageReceived(MessageInfo messageInfo) {
        Log.d(TAG, "On new message: {#" + messageInfo.getId() + " " + messageInfo + "}");
        // Only interest in certain message-types.
        if (messageInfo.type == MessageInfo.MessageType.MESSAGE_TYPE_INVITE_TO_PLAY ||
                messageInfo.type == MessageInfo.MessageType.MESSAGE_TYPE_CHAT_PRIVATE) {
            notifCount_++;
            invalidateOptionsMenu(); // Recreate the options menu

        }
    }

    // **** Implementation of BoardFragment.OnFragmentInteractionListener ****

    @Override
    public void onBoardFragment_CreateView(BoardFragment fragment) {
        myBoardFragment_ = new WeakReference<BoardFragment>(fragment);

        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.setBoardEventListener(
                    BaseTableController.getCurrentController()
                    //this /*tableController_*/
            );
        }

        //tableController_.setTableTitle();
    }

    @Override
    public void onBoardFragment_DestroyView(BoardFragment fragment) {
        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null && boardFragment == fragment) {
            Log.d(TAG, "Board fragment view destroyed. Release weak reference.");
            myBoardFragment_ = new WeakReference<BoardFragment>(null);
        }
    }
    @Override
    public void onTableMenuClick(View view) {
        BaseTableController.getCurrentController().handleTableMenuOnClick(this);
    }
    public void onShowMessageViewClick(View v) {}
    public void onChangeRoleRequest(Enums.ColorEnum clickedColor) {}

    // **** Implementation of BoardView.BoardEventListener ****
    //public void onLocalMove(Position fromPos, Position toPos) {}
    //public boolean isMyTurn() { return true; }

    // **** Implementation of BaseTableController.BoardController ****
    @Override
    public void updateBoardWithNewMove(MoveInfo move) {
        Log.d(TAG, "Update board with a new AI move = " + move);
        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.makeMove(move, true);
        }

        if (HoxApp.getApp().getReferee().getMoveCount() == 2) { // The game has started?
            adjustScreenOnFlagBasedOnGameStatus();
        }
    }

    @Override
    public void reverseBoardView() {
        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.reverseView();
        }
    }

    @Override
    public void openNewPracticeTable() {
        Log.d(TAG, "Open a new practice table");
        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.resetBoard();
        }
        BaseTableController.getCurrentController().setTableTitle();
        Snackbar.make(findViewById(R.id.board_view), R.string.action_reset,
                Snackbar.LENGTH_SHORT)
                .show();
    }

    @Override
    public void updateBoardWithNewTableInfo(TableInfo tableInfo) {}
    @Override
    public void resetBoardWithNewMoves(MoveInfo[] moves) {}
    @Override
    public void clearTable() {}
    @Override
    public void onLocalPlayerJoined(Enums.ColorEnum myColor) {}
    @Override
    public void onPlayerJoin(String pid, String rating, Enums.ColorEnum playerColor) {}
    @Override
    public void onPlayerLeave(String pid) {}
    @Override
    public void showGameMessage_DRAW(String pid) {}
    @Override
    public void onGameEnded(Enums.GameStatus gameStatus) {}
    @Override
    public void onGameReset() {}

    // *****
    private void adjustScreenOnFlagBasedOnGameStatus() {
        if (HoxApp.getApp().isGameInProgress()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void openNotificationView() {
        Log.d(TAG, "Open 'Notification' view...");
        Intent intent = new Intent(this, ChatBubbleActivity.class);
        startActivity(intent);

        notifCount_ = 0;
        invalidateOptionsMenu();
    }
}