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
                           BoardView.BoardEventListener,
                           MessageManager.EventListener,
                           AIController.AIListener {

    private static final String TAG = "AITableActivity";
    private boolean DEBUG_LIFE_CYCLE = true;

    private WeakReference<BoardFragment> myBoardFragment_ = new WeakReference<BoardFragment>(null);

    // TODO: We should persist this counter somewhere else because it is lost when the
    //       device is rotated, for example.
    private int notifCount_ = 0;

    // My color in the local table (ie. to practice with AI).
    // NOTE: Currently, we cannot change my role in this type of table.
    private final Enums.ColorEnum myColorInLocalTable_ = Enums.ColorEnum.COLOR_RED;

    private final AIController aiController_ = new AIController();
    private final TableTimeTracker timeTracker_ = new TableTimeTracker();
    private final TablePlayerTracker playerTracker_ = new TablePlayerTracker(Enums.TableType.TABLE_TYPE_LOCAL);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onCreate");
        setContentView(R.layout.activity_ai_table);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Check that the activity is using the layout version with
        // the main_container FrameLayout
        if (findViewById(R.id.board_container) != null) {
            // However, if we're being restored from a previous state,
            // then we don't want to create another fragment, which would create the problem
            // of overlapping fragments.
            if (savedInstanceState == null) {
                BoardFragment boardFragment = BoardFragment.newInstance("AI");
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.board_container, boardFragment).commit();
            }
        }

        aiController_.setBoardController(this);

        setupNewTable();

        // NOTE: It is important to control our App 's audio volume using the Hardware Control Keys.
        // Reference:
        //    http://developer.android.com/training/managing-audio/volume-playback.html
        setVolumeControlStream(SoundManager.getInstance().getStreamType());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onResume");
        MessageManager.getInstance().addListener(this);
        //playerTracker_.syncUI(); // AI Level is one that needs to be updated.
        adjustScreenOnFlagBasedOnGameStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onPause");
        MessageManager.getInstance().removeListener(this);
        adjustScreenOnFlagBasedOnGameStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onDestroy");
        timeTracker_.stop();
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

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
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
            boardFragment.setBoardEventListener(this);

            boardFragment.setupUIForTimeTracker(timeTracker_);
            timeTracker_.syncUI();

            boardFragment.setupUIForPlayerTracker(playerTracker_);
            playerTracker_.syncUI();

            boardFragment.hideMessageBadgeView();
        }
    }

    @Override
    public void onBoardFragment_DestroyView(BoardFragment fragment) {
        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null && boardFragment == fragment) {
            Log.d(TAG, "Board fragment view destroyed. Release weak reference.");
            myBoardFragment_ = new WeakReference<BoardFragment>(null);
            timeTracker_.unsetUITextViews();
            playerTracker_.unsetUIViews();
        }
    }

    @Override
    public void onBoardFragment_ReverseView() {
        timeTracker_.reverseView();
        playerTracker_.reverseView();
    }

    @Override
    public void onTableMenuClick(View view) {
        final TableActionSheet actionSheet = new TableActionSheet(this);
        actionSheet.setHeaderText(getString(R.string.title_table_ai));
        setupListenersInTableActionSheet(actionSheet);

        actionSheet.hideAction(TableActionSheet.Action.ACTION_CLOSE_TABLE);
        actionSheet.hideAction(TableActionSheet.Action.ACTION_OFFER_DRAW);
        actionSheet.hideAction(TableActionSheet.Action.ACTION_OFFER_RESIGN);
        actionSheet.hideAction(TableActionSheet.Action.ACTION_NEW_TABLE);

        actionSheet.show();
    }

    public void onShowMessageViewClick(View v) {}
    public void onChangeRoleRequest(Enums.ColorEnum clickedColor) {}

    // **** Implementation of BoardView.BoardEventListener ****
    @Override
    public void onLocalMove(Position fromPos, Position toPos, Enums.GameStatus gameStatus) {
        Referee referee = HoxApp.getApp().getReferee();
        Log.d(TAG, "On local move: referee 's moveCount = " + referee.getMoveCount());

        timeTracker_.nextColor();

        if (referee.getMoveCount() == 2) {
            timeTracker_.start();
            adjustScreenOnFlagBasedOnGameStatus();
        }

        if (referee.getMoveCount() > 1) { // The game has started?
            playerTracker_.syncUI();
        }

        if (referee.isGameInProgress()) {
            aiController_.onHumanMove(fromPos, toPos);
        } else {
            onGameEnded(gameStatus); // The game has ended. Do nothing
        }
    }

    @Override
    public boolean isMyTurn() {
        return (myColorInLocalTable_ == HoxApp.getApp().getReferee().getNextColor());
    }

    // **** Implementation of AIController.AIListener ****
    @Override
    public void onAINewMove(MoveInfo move) {
        Log.d(TAG, "Update board with a new AI move = " + move);
        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.makeMove(move, true);
        }

        timeTracker_.nextColor();

        if (HoxApp.getApp().getReferee().getMoveCount() == 2) { // The game has started?
            timeTracker_.start();
            adjustScreenOnFlagBasedOnGameStatus();
        }

        if (!HoxApp.getApp().getReferee().isGameInProgress()) {
            Log.i(TAG, "... after AI move => the game has ended.");
            Enums.GameStatus gameStatus = Referee.gameStatusToEnum(move.gameStatus);
            onGameEnded(gameStatus);
        }
    }

    // ***************************************************************
    //
    //              Private APIs
    //
    // ***************************************************************

    private void reverseBoardView() {
        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.reverseView();
        }
    }

    private void openNewPracticeTable() {
        Log.d(TAG, "Open a new practice table");
        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.resetBoard();
        }
        Snackbar.make(findViewById(R.id.board_view), R.string.action_reset,
                Snackbar.LENGTH_SHORT)
                .show();
    }

    private void onGameEnded(Enums.GameStatus gameStatus) {
        Log.d(TAG, "onGameEnded: gameStatus = " + Utils.gameStatusToString(gameStatus));
        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.onGameEnded(gameStatus);
        }
        adjustScreenOnFlagBasedOnGameStatus();
    }

    private void adjustScreenOnFlagBasedOnGameStatus() {
        Referee referee = HoxApp.getApp().getReferee();
        if (referee.getMoveCount() > 1
                && referee.isGameInProgress()) {
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

    private void setupNewTable() {
        Log.d(TAG, "setupNewTable:...");
        HoxApp.getApp().getAiEngine().initGame();

        timeTracker_.stop();
        final TimeInfo initialTime = new TimeInfo(Enums.DEFAULT_INITIAL_GAME_TIMES);
        timeTracker_.setInitialColor(Enums.ColorEnum.COLOR_RED);
        timeTracker_.setInitialTime(initialTime);
        timeTracker_.setBlackTime(initialTime);
        timeTracker_.setRedTime(initialTime);

        playerTracker_.setTableType(Enums.TableType.TABLE_TYPE_LOCAL); // A new practice table.
        playerTracker_.setRedInfo(HoxApp.getApp().getString(R.string.you_label), "1501");
        playerTracker_.setBlackInfo(HoxApp.getApp().getString(R.string.ai_label), "1502");

        HoxApp.getApp().getReferee().resetGame();
    }

    private void setupListenersInTableActionSheet(final TableActionSheet actionSheet) {
        actionSheet.setOnClickListener_ResetTable(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aiController_.resetGame();

                playerTracker_.syncUI();

                timeTracker_.stop();
                timeTracker_.reset();

                openNewPracticeTable();
                actionSheet.dismiss();
            }
        });

        actionSheet.setOnClickListener_ReverseBoard(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reverseBoardView();
                actionSheet.dismiss();
            }
        });
    }
}