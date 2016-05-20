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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkTableActivity extends AppCompatActivity
                    implements ViewPager.OnPageChangeListener,
                               BoardFragment.OnFragmentInteractionListener,
                               PlayersFragment.OnFragmentInteractionListener,
                               //ChatFragment.OnChatFragmentListener,
                               NetworkTableController.BoardController,
                               MessageManager.EventListener,
                               BoardView.BoardEventListener {

    private static final String TAG = "NetworkTableActivity";

    private ProgressBar progressBar_;
    private ViewPager viewPager_;

    private static final String EXTRA_TABLE_ID = "extra.table.id";
    private static final int JOIN_TABLE_REQUEST = 1;

    private WeakReference<BoardFragment> myBoardFragment_ = new WeakReference<BoardFragment>(null);
    //private WeakReference<ChatFragment> myChatFragment_ = new WeakReference<ChatFragment>(null);
    private WeakReference<PlayersFragment> myPlayersFragment_ = new WeakReference<PlayersFragment>(null);

    private ChatInTableSheet chatSheet_;

    private final NetworkTableController tableController_ = NetworkTableController.getInstance();
    private final Referee referee_ = tableController_.getReferee();
    private TableTimeTracker timeTracker_ = tableController_.getTimeTracker();
    private TablePlayerTracker playerTracker_ = tableController_.getPlayerTracker();

    private String tableId_;

    private MenuItem notificationMenuItem_;

    /**
     * Starts the table activity with the given tableID.
     */
    public static void start(Context context, String tableId) {
        Intent intent = new Intent(context, NetworkTableActivity.class);
        intent.putExtra(EXTRA_TABLE_ID, tableId);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
        setContentView(R.layout.activity_network_table);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        progressBar_ = (ProgressBar) findViewById(R.id.progress_spinner);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        MainPagerAdapter pagerAdapter = new MainPagerAdapter(this, getSupportFragmentManager());
        viewPager_ = (ViewPager) findViewById(R.id.network_table_view_pager);
        viewPager_.setAdapter(pagerAdapter);
        viewPager_.setOffscreenPageLimit(2); // Performance: Keep the 3rd page from being destroyed!
        viewPager_.addOnPageChangeListener(this);

        tableController_.setBoardController(this);

        if (savedInstanceState == null) {
            tableId_ = getIntent().getStringExtra(EXTRA_TABLE_ID);
            Log.d(TAG, "onCreate: tableId = [" + tableId_ + "]");
            if (TextUtils.isEmpty(tableId_)) { // Requesting for a NEW table?
                NetworkController.getInstance().sendRequestToOpenNewTable();
            } else {
                progressBar_.setVisibility(View.VISIBLE);
                NetworkController.getInstance().handleTableSelection(tableId_);
            }
        } else {
            tableId_ = NetworkController.getInstance().getMyTableId();
            Log.d(TAG, "onCreate: (from Network controller) tableId = [" + tableId_ + "]");
            setAndShowTitle(tableId_);
        }

        // NOTE: It is important to control our App 's audio volume using the Hardware Control Keys.
        // Reference:
        //    http://developer.android.com/training/managing-audio/volume-playback.html
        setVolumeControlStream(SoundManager.getInstance().getStreamType());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (notificationMenuItem_ != null) {
            BadgeDrawable.setBadgeCount(this, notificationMenuItem_,
                    MessageManager.getInstance().getNotificationCount());
        }
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
        tableController_.setBoardController(null);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "(ActionBar) onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);

        // Set up the notification item.
        notificationMenuItem_ = menu.findItem(R.id.action_notifications);
        BadgeDrawable.setBadgeCount(this, notificationMenuItem_,
                MessageManager.getInstance().getNotificationCount());

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "(ActionBar) onPrepareOptionsMenu");
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_new_table).setVisible(false);
        menu.findItem(R.id.action_close_table).setVisible(false);
        //menu.findItem(R.id.action_view_tables).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "(ActionBar) onOptionsItemSelected");
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:  // NOTE: This is the BACK key!
                confirmUserLeavingTable();
                return true;
            //case R.id.action_new_table:
            //    tableController_.handleRequestToOpenNewTable();
            //    return true;
            case R.id.action_view_tables:
                displayTableList();
                return true;
            case R.id.action_notifications:
                openNotificationView();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        confirmUserLeavingTable();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode:" + requestCode + ", resultCode:" + resultCode);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == JOIN_TABLE_REQUEST) {
            final String tableId = data.getStringExtra("tid");
            Log.d(TAG, "onActivityResult: Get new tableId = " + tableId);
            if (!TextUtils.equals(tableId, tableId_)) {
                tableId_ = tableId;
                progressBar_.setVisibility(View.VISIBLE);
                NetworkController.getInstance().handleTableSelection(tableId_);
            }
        }
    }

    /**
     * Implementation ViewPager.OnPageChangeListener
     */
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    /**
     * This method will be invoked when a new page becomes selected. Animation is not
     * necessarily complete.
     *
     * @param position Position index of the new selected page.
     */
    public void onPageSelected(int position) {
        Log.d(TAG, "onPageSelected: position:"+ position);
        if (position == MainPagerAdapter.POSITION_BOARD) {
            int unreadCount = MessageManager.getInstance().getUnreadCount(
                    MessageInfo.MessageType.MESSAGE_TYPE_CHAT_IN_TABLE);
            Log.d(TAG, "onPageSelected: ... unreadCount = " + unreadCount);
            BoardFragment boardFragment = myBoardFragment_.get();
            if (boardFragment != null) {
                boardFragment.setTableMessageCount(unreadCount);
            }
        } /*else if (position == MainPagerAdapter.POSITION_CHAT) {
            List<MessageInfo> newMessages = MessageManager.getInstance().getMessages();
            ChatFragment chatFragment = myChatFragment_.get();
            if (chatFragment != null) {
                chatFragment.addNewMessages(newMessages);
            }
            MessageManager.getInstance().removeMessages(MessageInfo.MessageType.MESSAGE_TYPE_CHAT_IN_TABLE);
        }*/
    }

    /**
     * Called when the scroll state changes. Useful for discovering when the user
     * begins dragging, when the pager is automatically settling to the current page,
     * or when it is fully stopped/idle.
     *
     * @param state The new scroll state.
     * @see ViewPager#SCROLL_STATE_IDLE
     * @see ViewPager#SCROLL_STATE_DRAGGING
     * @see ViewPager#SCROLL_STATE_SETTLING
     */
    public void onPageScrollStateChanged(int state) {
    }
    // ******

    // **** Implements BoardFragment.OnFragmentInteractionListener ***
    @Override
    public void onBoardFragment_CreateView(BoardFragment fragment) {
        Log.d(TAG, "Board fragment view created");
        myBoardFragment_ = new WeakReference<BoardFragment>(fragment);

        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.setBoardEventListener(this);
            boardFragment.setupUIForTimeTracker(timeTracker_);
            boardFragment.setupUIForPlayerTracker(playerTracker_);
            timeTracker_.syncUI();
            playerTracker_.syncUI();
            restoreHistoryMoves(boardFragment);
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
        actionSheet.setHeaderText(getTitleForTableActionSheet());
        setupListenersInTableActionSheet(actionSheet);

        if (isTableEmpty()) {
            actionSheet.hideAction(TableActionSheet.Action.ACTION_RESET_TABLE);
            actionSheet.hideAction(TableActionSheet.Action.ACTION_REVERSE_BOARD);
            actionSheet.hideAction(TableActionSheet.Action.ACTION_CLOSE_TABLE);
            actionSheet.hideAction(TableActionSheet.Action.ACTION_OFFER_DRAW);
            actionSheet.hideAction(TableActionSheet.Action.ACTION_OFFER_RESIGN);

        } else {
            final boolean isGameOver = HoxApp.getApp().isGameOver();
            final Enums.ColorEnum myColor = HoxApp.getApp().getNetworkController().getMyColor();
            final boolean amIPlaying = (myColor == Enums.ColorEnum.COLOR_BLACK || myColor == Enums.ColorEnum.COLOR_RED);
            final int moveCount = referee_.getMoveCount();

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
    public void onShowMessageViewClick(View v) {
        // OPTION #1:
        // Utils.animatePagerTransition(viewPager_, true /* forward */, 500);

        // OPTION #2:
        // Using the BottomSheet.

        List<MessageInfo> newMessages =
                MessageManager.getInstance().getMessages(MessageInfo.MessageType.MESSAGE_TYPE_CHAT_IN_TABLE);
        //ChatFragment chatFragment = myChatFragment_.get();
        //if (chatFragment != null) {
        //    chatFragment.addNewMessages(newMessages);
        //}

        TableInfo tableInfo = new TableInfo();
        tableInfo.tableId = tableId_;
        tableInfo.itimes = Enums.DEFAULT_INITIAL_GAME_TIMES;
        chatSheet_ = new ChatInTableSheet(this, newMessages, tableInfo);
        chatSheet_.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "ChatInTableSheet dismissed!");
                chatSheet_ = null;
            }
        });
        Log.d(TAG, "Show chatSheet... #1");
        chatSheet_.show();
        Log.d(TAG, "After showing chatSheet... #2");

        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.setTableMessageCount(0);
        }
        MessageManager.getInstance().markMessagesOfTypeRead(MessageInfo.MessageType.MESSAGE_TYPE_CHAT_IN_TABLE);
    }

    @Override
    public void onChangeRoleRequest(Enums.ColorEnum clickedColor) {
        tableController_.handlePlayerButtonClick(clickedColor);
    }

    // **** Implements PlayersFragment.OnFragmentInteractionListener ***
    @Override
    public void onPlayersFragment_CreateView(PlayersFragment fragment) {
        myPlayersFragment_ = new WeakReference<PlayersFragment>(fragment);
    }

    @Override
    public void onPlayersFragment_DestroyView(PlayersFragment fragment) {
        PlayersFragment playersFragment = myPlayersFragment_.get();
        if (playersFragment != null && playersFragment == fragment) {
            myPlayersFragment_ = new WeakReference<PlayersFragment>(null);
            Log.d(TAG, "Release Players fragment: " + playersFragment);
        }
    }

    @Override
    public List<PlayerInfo> onRequestToRefreshPlayers() {
        List<PlayerInfo> players = new ArrayList<PlayerInfo>();

        PlayerInfo redPlayer = playerTracker_.getRedPlayer();
        if (redPlayer.isValid()) players.add(redPlayer);

        PlayerInfo blackPlayer = playerTracker_.getBlackPlayer();
        if (blackPlayer.isValid()) players.add(blackPlayer);

        Map<String, PlayerInfo> observers = playerTracker_.getObservers();
        for (HashMap.Entry<String, PlayerInfo> entry : observers.entrySet()) {
            players.add(entry.getValue());
        }

        return players;
    }

    @Override
    public void onPlayerClick(PlayerInfo playerInfo, final String tableId) {
        // Ask for PLAYER-INFO.
        HoxApp.getApp().getNetworkController().handleRequestToGetPlayerInfo(playerInfo.pid);

        final String playerId = playerInfo.pid;
        final Activity activity = this;
        final PlayerActionSheet dialog = new PlayerActionSheet(this, playerInfo);

        dialog.hideAction(PlayerActionSheet.Action.ACTION_JOIN_TABLE);

        dialog.setActionListener(new PlayerActionSheet.ActionListener() {
            @Override
            public void onActionClick(PlayerActionSheet.Action action) {
                if (action == PlayerActionSheet.Action.ACTION_SEND_MESSAGE) {
                    PlayerActionSheet.sendMessageToPlayer(activity, playerId);
                } else if (action == PlayerActionSheet.Action.ACTION_INVITE_PLAYER) {
                    HoxApp.getApp().getNetworkController().handleRequestToInvite(playerId);
                }
            }
        });

        dialog.show();
    }

    // **** Implementation of ChatFragment.OnChatFragmentListener
//    @Override
//    public void onChatFragment_CreateView(ChatFragment fragment) {
//        myChatFragment_ = new WeakReference<ChatFragment>(fragment);
//    }
//
//    @Override
//    public void onChatFragment_DestroyView(ChatFragment fragment) {
//        ChatFragment chatFragment = myChatFragment_.get();
//        if (chatFragment != null && chatFragment == fragment) {
//            myChatFragment_ = new WeakReference<ChatFragment>(null);
//            Log.d(TAG, "Release Chat fragment: " + chatFragment);
//        }
//    }

    // **** Implementation of BaseTableController.BoardController ****
    @Override
    public void updateBoardWithNewMove(MoveInfo move) {
        Log.d(TAG, "Update board with a new AI move = " + move);
        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.makeMove(move, true);
        }

        if (referee_.getMoveCount() == 2) { // The game has started?
            adjustScreenOnFlagBasedOnGameStatus();
        }
    }

    private void reverseBoardView() {
        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.reverseView();
        }
    }

    @Override
    public void updateBoardWithNewTableInfo(TableInfo tableInfo) {
        Log.d(TAG, "Update board with new network Table info (I_TABLE)...");

        tableId_ = tableInfo.tableId;

        setAndShowTitle(tableId_);
        progressBar_.setVisibility(View.GONE);
        invalidateOptionsMenu(); // Recreate the options menu

        referee_.resetGame();

        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.resetBoard();
        }

        PlayersFragment playersFragment = myPlayersFragment_.get();
        if (playersFragment != null) {
            playersFragment.refreshPlayersIfNeeded();
        }
    }

    @Override
    public void resetBoardWithNewMoves(MoveInfo[] moves) {
        Log.d(TAG, "Reset board with a list of new network moves (I_MOVES)...");
        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.resetBoardWithNewMoves(moves);
        }

        adjustScreenOnFlagBasedOnGameStatus();
    }

    /**
     * Make the current table an EMPTY one.
     */
    @Override
    public void clearTable() {
        Log.d(TAG, "Clear the table. Make it an empty one.");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE); // No title.
        } else {
            Log.w(TAG, "clearTable: getSupportActionBar() = null. Do not set Display options!");
        }
        invalidateOptionsMenu(); // Recreate the options menu

        referee_.resetGame();

        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.resetBoard();
        }

        //ChatFragment chatFragment = myChatFragment_.get();
        //if (chatFragment != null) {
        //    chatFragment.clearAll();
        //}

        PlayersFragment playersFragment = myPlayersFragment_.get();
        if (playersFragment != null) {
            playersFragment.clearAll();
        }

        MessageManager.getInstance().removeMessages(MessageInfo.MessageType.MESSAGE_TYPE_CHAT_IN_TABLE);

        adjustScreenOnFlagBasedOnGameStatus();
    }

    @Override
    public void onLocalPlayerJoined(Enums.ColorEnum myColor) {
        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.onLocalPlayerJoined(myColor);
        }
    }

    @Override
    public void onPlayerJoin(String pid, String rating, Enums.ColorEnum playerColor) {
        PlayersFragment playersFragment = myPlayersFragment_.get();
        if (playersFragment != null) {
            playersFragment.onPlayerJoin(pid, rating, playerColor);
        }
    }

    @Override
    public void onPlayerLeave(String pid) {
        PlayersFragment playersFragment = myPlayersFragment_.get();
        if (playersFragment != null) {
            playersFragment.onPlayerLeave(pid);
        }
    }

    @Override
    public void showGameMessage_DRAW(String pid) {
        Snackbar.make(viewPager_,
                getString(R.string.msg_player_offered_draw, pid), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onGameEnded(Enums.GameStatus gameStatus) {
        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.onGameEnded(gameStatus);
        }
        adjustScreenOnFlagBasedOnGameStatus();
    }

    @Override
    public void onGameReset() {
        referee_.resetGame();

        BoardFragment boardFragment = myBoardFragment_.get();
        if (boardFragment != null) {
            boardFragment.resetBoard();
        }
    }

    @Override
    public void onPlayerInfoReceived(String pid, String rating, String wins, String draws, String losses) {
        showBriefMessage(
                getString(R.string.msg_player_record, pid, rating, wins, draws, losses),
                Snackbar.LENGTH_LONG);
    }

    @Override
    public void onJoinTableError(String errorMessage, Enums.ErrorCode errorCode) {
        Log.i(TAG, "Error in joining a table: " + errorMessage);
        progressBar_.setVisibility(View.GONE);

        if (errorCode == Enums.ErrorCode.ERROR_CODE_NOT_FOUND) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.dialog_error_table_not_found, tableId_));
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    @Override
    public void onNetworkError() {
        showBriefMessage(
                getString(R.string.msg_network_error_io_exception_exception),
                Snackbar.LENGTH_LONG);
    }

    // **** Implementation of MessageManager.EventListener ****
    @Override
    public void onMessageReceived(MessageInfo messageInfo) {
        Log.d(TAG, "On new message: {#" + messageInfo.getId() + " " + messageInfo + "}");
        if (MessageManager.getInstance().isNotificationType(messageInfo.type)) {
            invalidateOptionsMenu(); // Recreate the options menu

        } else if (messageInfo.type == MessageInfo.MessageType.MESSAGE_TYPE_CHAT_IN_TABLE) {

            if (chatSheet_ != null) { // The chat sheet is visible to the user?
                chatSheet_.addNewMessage(messageInfo);
                messageInfo.markRead();
                return;
            }

            int currentPageIndex = viewPager_.getCurrentItem();
            if (currentPageIndex == MainPagerAdapter.POSITION_BOARD) {
                BoardFragment boardFragment = myBoardFragment_.get();
                if (boardFragment != null) {
                    int unreadCount = MessageManager.getInstance().getUnreadCount(
                            MessageInfo.MessageType.MESSAGE_TYPE_CHAT_IN_TABLE);
                    Log.d(TAG, "On new message: ... unreadCount = " + unreadCount);
                    boardFragment.setTableMessageCount(unreadCount);
                }
            } /*else if (currentPageIndex == MainPagerAdapter.POSITION_CHAT) {
                ChatFragment chatFragment = myChatFragment_.get();
                if (chatFragment != null) {
                    chatFragment.addNewMessage(messageInfo);
                }
                MessageManager.getInstance().removeMessage(messageInfo);
            }*/
        }
    }

    // **** Implementation of BoardView.BoardEventListener ****
    @Override
    public void onLocalMove(Position fromPos, Position toPos, Enums.GameStatus gameStatus) {
        Log.d(TAG, "Handle local move: referee 's moveCount = " + referee_.getMoveCount());

        timeTracker_.nextColor();

        if (referee_.getMoveCount() == 2) {
            timeTracker_.start();
            adjustScreenOnFlagBasedOnGameStatus();
        }

        if (referee_.getMoveCount() > 1) { // The game has started?
            playerTracker_.syncUI();
        }

        HoxApp.getApp().getNetworkController().handleRequestToSendMove(fromPos, toPos);
    }

    @Override
    public boolean isMyTurn() {
        final Enums.ColorEnum myColor = HoxApp.getApp().getNetworkController().getMyColor();
        return ((myColor == Enums.ColorEnum.COLOR_RED || myColor == Enums.ColorEnum.COLOR_BLACK) &&
                playerTracker_.hasEnoughPlayers() &&
                myColor == referee_.getNextColor());
    }

    @Override
    public int validateMove(Position fromPos, Position toPos) {
        return referee_.validateMove(
                fromPos.row, fromPos.column, toPos.row, toPos.column);
    }

    // ***************************************************************
    //
    //              Private APIs
    //
    // ***************************************************************

    private void restoreHistoryMoves(BoardFragment boardFragment) {
        List<Piece.Move> historyMoves = referee_.getHistoryMoves();
        if (historyMoves.size() > 0) {
            Log.d(TAG, "Restore history moves: size = " + historyMoves.size());
            // Make a copy of the list before accessing it to avoid "concurrent" exception.
            Piece.Move[] moves = historyMoves.toArray(new Piece.Move[historyMoves.size()]);
            for (Piece.Move move : moves) {
                referee_.validateMove(move.fromPosition.row, move.fromPosition.column,
                        move.toPosition.row, move.toPosition.column);
            }

            int lastGameStatus = referee_.getGameStatus();
            boardFragment.restoreMoveHistory(historyMoves, lastGameStatus);
        }
    }

    private void adjustScreenOnFlagBasedOnGameStatus() {
        if (tableController_.isGameInProgress()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void setAndShowTitle(String title) {
        if (getSupportActionBar() == null) {
            Log.w(TAG, "setAndShowTitle: getSupportActionBar() = null. Do not set Display options!");
        } else if (TextUtils.isEmpty(title)) {
            getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE); // No title.
        } else {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
            getSupportActionBar().setTitle(getString(R.string.title_table, title));
        }
    }

    private void openNotificationView() {
        Log.d(TAG, "Open 'Notification' view...");
        startActivity(new Intent(this, NotificationActivity.class));
        invalidateOptionsMenu(); // // Recreate the options menu
    }

    private void displayTableList() {
        PlayerManager.getInstance().clearTables(); // will get a new list
        NetworkController.getInstance().sendRequestForTableList();

        Intent intent = new Intent(this, TablesActivity.class);
        startActivityForResult(intent, JOIN_TABLE_REQUEST);
    }

    private void confirmUserLeavingTable() {
        // If I am not playing, then go ahead to leave without confirmation.
        if (!NetworkController.getInstance().amIPlaying()) {
            closeCurrentTable();
            return;
        }

        Log.d(TAG, "Confirm that the user really wants to leave...");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_leave_table_confirm_message);
        builder.setPositiveButton(R.string.button_leave, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                closeCurrentTable();
            }
        });
        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "Cancel the request to leave the table. Do nothing.");
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void closeCurrentTable() {
        Log.d(TAG, "Close the current table...");
        NetworkController.getInstance().handleRequestToCloseCurrentTable();
        MessageManager.getInstance().removeMessages(MessageInfo.MessageType.MESSAGE_TYPE_CHAT_IN_TABLE);
        finish();
    }

    private boolean isTableEmpty() {
        return TextUtils.isEmpty(tableId_);
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

    private void setupListenersInTableActionSheet(final TableActionSheet actionSheet) {
        actionSheet.setOnClickListener_ResetTable(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NetworkController.getInstance().handleRequestToResetTable();
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

        actionSheet.setOnClickListener_Close(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmUserLeavingTable();
                actionSheet.dismiss();
            }
        });

        actionSheet.setOnClickListener_OfferDrawn(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NetworkController.getInstance().handleRequestToOfferDraw();
                actionSheet.dismiss();
            }
        });

        actionSheet.setOnClickListener_OfferResign(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NetworkController.getInstance().handleRequestToOfferResign();
                actionSheet.dismiss();
            }
        });

        actionSheet.setOnClickListener_NewTable(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                throw new RuntimeException("This action should not be enabled!");
            }
        });
    }

    private void showBriefMessage(CharSequence text, int duration) {
        Snackbar.make(viewPager_, text, duration).show();
    }

    /**
     * The ViewPager adapter for the main page.
     */
    private static class MainPagerAdapter extends FragmentPagerAdapter {
        private final Context context_;

        public static final int POSITION_BOARD = 0;
        //public static final int POSITION_CHAT = 1;

        public MainPagerAdapter(Context context, FragmentManager fragmentManager) {
            super(fragmentManager);
            context_ = context;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return BoardFragment.newInstance("NETWORK");
                //case 1: return new ChatFragment();
                default: return new PlayersFragment();
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return context_.getString(R.string.label_table);
                //case 1: return context_.getString(R.string.title_activity_chat);
                default: return context_.getString(R.string.action_view_players);
            }
        }

        /**
         * Reference: https://guides.codepath.com/android/ViewPager-with-FragmentPagerAdapter
         */
        @Override
        public float getPageWidth (int position) {
            switch (position) {
                case 0: return 1f; // 0.95f;
                //case 1: return 1f; // 0.9f;
                default: return 1f;
            }
        }
    }

}