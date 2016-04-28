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
import java.util.List;

import com.playxiangqi.hoxchess.Enums.ColorEnum;
import com.playxiangqi.hoxchess.Enums.GameStatus;
import com.playxiangqi.hoxchess.Enums.TableType;
import com.playxiangqi.hoxchess.Piece.Move;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * The main (entry-point) activity.
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                MessageManager.EventListener {

    private static final String TAG = "MainActivity";

    private static final String STATE_IS_BLACK_ON_TOP = "isBlackOnTop";

    // The request codes
    private static final int JOIN_TABLE_REQUEST = 1;

    private DrawerLayout drawerLayout_;
    private ActionBarDrawerToggle drawerToggle_;
    private MainPagerAdapter pagerAdapter_;

    private Fragment placeholderFragment_;
    private ProgressBar progressBar_;
    private BoardView boardView_;
    private TextView topPlayerLabel_;
    private TextView bottomPlayerLabel_;
    private Button topPlayerButton_;
    private Button bottomPlayerButton_;

    private boolean isBlackOnTop_ = true; // Normal view. Black player is at the top position.

    private boolean isWaitingForTables = false;

    // Keep a reference to fragments because I don't know when they are destroyed or created.
    // Also, the way ViewPager handles fragments makes it hard to retain references to fragments.
    // See:
    //  http://stackoverflow.com/questions/19393076/how-to-properly-handle-screen-rotation-with-a-viewpager-and-nested-fragments
    private WeakReference<ChatFragment> myChatFragment_ = new WeakReference<ChatFragment>(null);

    // TODO: We should persist this counter somewhere else because it is lost when the
    //       device is rotated, for example.
    private int notifCount_ = 0;

    private BaseTableController tableController_ = new BaseTableController();

    public void setTableController(TableType tableType) {
        tableController_ = BaseTableController.getTableController(tableType);
        tableController_.setMainActivity(this);

        // Note: Set the listener again even though we already do in onBoardViewCreated !!!
        if (boardView_ != null) {
            boardView_.setBoardEventListener(tableController_);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        setupDrawer(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        } else {
            Log.w(TAG, "onCreate: getSupportActionBar() = null. Do not set Display options!");
        }

        Log.d(TAG, "onCreate: savedInstanceState = " + savedInstanceState + ".");

        pagerAdapter_ = new MainPagerAdapter(this, getSupportFragmentManager());
        ViewPager mViewPager = (ViewPager) findViewById(R.id.main_view_pager);
        mViewPager.setAdapter(pagerAdapter_);

//        if (savedInstanceState == null) {
//            placeholderFragment_ = new PlaceholderFragment();
//            Log.d(TAG, "onCreate: (NEW): Created board-fragment = " + placeholderFragment_);
//
//            getSupportFragmentManager().beginTransaction()
//                    .add(R.id.container, placeholderFragment_, "board")
//                    .commit();
//        } else {
//            placeholderFragment_ = getSupportFragmentManager().findFragmentByTag("board");
//            Log.d(TAG, "onCreate: (savedInstanceState): Found board-fragment = " + placeholderFragment_);
//        }

        // NOTE: It is important to control our App 's audio volume using the Hardware Control Keys.
        // Reference:
        //    http://developer.android.com/training/managing-audio/volume-playback.html
        setVolumeControlStream(SoundManager.getInstance().getStreamType());

        HoxApp.getApp().registerMainActivity(this);
    }

    private void setupDrawer(Toolbar toolbar) {
        drawerLayout_ = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawerToggle_ = new ActionBarDrawerToggle(this, drawerLayout_, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                handleEvent_onDrawerOpened(drawerView);
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                Log.d(TAG, "onDrawerClosed");
                //getSupportActionBar().setTitle(mActivityTitle);
                //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        drawerLayout_.addDrawerListener(drawerToggle_);
        drawerToggle_.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Navigation header view: onClick");
                openSettingsView();
                drawerLayout_.closeDrawer(GravityCompat.START);
            }
        });

        drawerToggle_.setDrawerIndicatorEnabled(true);
    }

    private void handleEvent_onDrawerOpened(View drawerView) {
        Log.d(TAG, "Handle event: onDrawerOpened");

        // Update header items.
        TextView playerNameView = (TextView) drawerView.findViewById(R.id.textview_player_name);
        if (playerNameView == null) { // Sanity check. This should not happen!
            Log.e(TAG, "onDrawerOpened: Player Name TextView not found!!!");
            return;
        }
        TextView playerIdView = (TextView) drawerView.findViewById(R.id.textview_player_id);
        if (playerIdView == null) { // Sanity check. This should not happen!
            Log.e(TAG, "onDrawerOpened: Player ID TextView not found!!!");
            return;
        }

        String playerName;
        String playerID;

        if (SettingsActivity.getLoginWithAccountFlag(this)) {
            playerName = getString(R.string.playxiangqi_account);
            playerID = SettingsActivity.getAccountPid(this);
        } else { // Login as Guest
            playerName = getString(R.string.guest_account_name);
            playerID = HoxApp.getApp().getMyPid(); // currentGuestId
            if (TextUtils.isEmpty(playerID)) {
                playerID = getString(R.string.guest_id_default);
            }
        }
        playerNameView.setText(playerName);
        playerIdView.setText(playerID);

        // Update sub-header items.
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        MenuItem logoutItem = navigationView.getMenu().findItem(R.id.action_logout);
        if (logoutItem != null) {
            logoutItem.setVisible(HoxApp.getApp().isOnline());
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Log.d(TAG, "onPostCreate");
        drawerToggle_.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle_.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_view_tables:
                onViewTablesClicked();
                break;
            case R.id.action_view_players:
                onViewPlayersClicked();
                break;
            case R.id.action_settings:
                openSettingsView();
                break;
            case R.id.action_about: {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.action_www_playxiangqi: {
                openWebsiteToServer();
                break;
            }
            case R.id.action_logout:
                tableController_.handleLogoutFromNetwork();
                break;
            default:
                break;
        }

        drawerLayout_.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        //HoxApp.getApp().getNetworkController().addListener(this);
        MessageManager.getInstance().addListener(this);
        adjustScreenOnFlagBasedOnGameStatus();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onSaveInstanceState");
        // Save the table's current game state
        savedInstanceState.putBoolean(STATE_IS_BLACK_ON_TOP, isBlackOnTop_);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onRestoreInstanceState");
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance
        boolean isBlackOnTop = savedInstanceState.getBoolean(STATE_IS_BLACK_ON_TOP, true);
        if (!isBlackOnTop) {
            reverseView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        //HoxApp.getApp().getNetworkController().removeListener(this);
        MessageManager.getInstance().removeListener(this);
        adjustScreenOnFlagBasedOnGameStatus();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");

        if (tableController_.handleBackPressed()) { // already handled?
            return;
        } else {
            super.onBackPressed();
        }
    }
    
    public void onGameStatusChanged() {
        adjustScreenOnFlagBasedOnGameStatus();
    }
    
    private void adjustScreenOnFlagBasedOnGameStatus() {
        if (HoxApp.getApp().isGameInProgress()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        HoxApp.getApp().registerMainActivity(null);
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
        return tableController_.onPrepareOptionsMenu(this, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "(ActionBar) onOptionsItemSelected");

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle_.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                tableController_.handleRequestToCloseCurrentTable();
                return true;
            case R.id.action_new_table:
                tableController_.handleRequestToOpenNewTable();
                return true;
            case R.id.action_close_table:
                tableController_.handleRequestToCloseCurrentTable();
                return true;
            case R.id.action_view_tables:
                onViewTablesClicked();
                return true;
            case R.id.action_view_players:
                onViewPlayersClicked();
                return true;
            case R.id.action_notifications:
                openChatView();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openSettingsView() {
        Log.d(TAG, "Open 'Settings' view...");
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void openWebsiteToServer() {
        final String url = Enums.HC_URL_SERVER;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

        // Note: To avoid being crashed if there is no application that can handle this
        //  implicit intent, verify that the intent will resolve to an activity.
        //    + resolveActivity() returns null if none are registered.
        //
        // Reference:
        //    http://developer.android.com/guide/components/intents-filters.html#ExampleSend
        //
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.w(TAG, "There is no activity that can visit this URL : " + url);
        }
    }

    private void openChatView() {
        Log.d(TAG, "Open 'Chat' view...");
        Intent intent = new Intent(this, ChatBubbleActivity.class);
        startActivity(intent);

        notifCount_ = 0;
        invalidateOptionsMenu();
    }
    
    private void reverseView() {
        Log.d(TAG, "Reverse view...");
        isBlackOnTop_ = !isBlackOnTop_;
        boardView_.reverseView();

        HoxApp.getApp().getTimeTracker().reverseView();
        HoxApp.getApp().getPlayerTracker().reverseView();
    }

    private void askNetworkControllerForTableList() {
        //Snackbar.make(this.findViewById(R.id.container), R.string.msg_get_list_tables,
        //        Snackbar.LENGTH_SHORT)
        //        .show();
        HoxApp.getApp().getNetworkController().sendRequestForTableList();
    }

    private void onViewTablesClicked() {
        Log.d(TAG, "On ViewTables clicked...");
        //progressBar_.setVisibility(View.VISIBLE);

        PlayerManager.getInstance().clearTables(); // will get a new list

        if (HoxApp.getApp().isOnlineAndLoginOK()) {
            askNetworkControllerForTableList();
        } else {
            isWaitingForTables = true;
            HoxApp.getApp().loginServer();
        }

        startActivityToListTables();
    }

    private void onViewPlayersClicked() {
        Log.d(TAG, "On ViewPlayers clicked...");
        //progressBar_.setVisibility(View.VISIBLE);

        PlayerManager.getInstance().clearTables(); // will get a new list

        if (HoxApp.getApp().isOnlineAndLoginOK()) {
            askNetworkControllerForTableList();
        } else {
            isWaitingForTables = true; // Need tables info as well.
            HoxApp.getApp().loginServer();
        }

        startActivityToListPlayers();
    }

    private void startActivityToListTables() {
        Log.d(TAG, "Start activity (TABLES): ENTER.");

        //progressBar_.setVisibility(View.GONE);
        
        Intent intent = new Intent(this, TablesActivity.class);
        startActivityForResult(intent, JOIN_TABLE_REQUEST);
    }

    public void startActivityToListPlayers() {
        Log.d(TAG, "Start activity (PLAYERS): ENTER.");

        //progressBar_.setVisibility(View.GONE);

        Intent intent = new Intent(this, PlayersActivity.class);
        startActivity(intent);
    }

    public void updateBoardWithNewAIMove(Position fromPos, Position toPos) {
        Log.d(TAG, "Update board with a new AI move = " + fromPos + " => " + toPos);
        boardView_.makeMove(fromPos, toPos, true);
    }
    
    public void updateBoardWithNewTableInfo(TableInfo tableInfo) {
        Log.d(TAG, "Update board with new network Table info (I_TABLE)...");
        
        setAndShowTitle(tableInfo.tableId);
        invalidateOptionsMenu(); // Recreate the options menu
        boardView_.resetBoard();
    }

    public void resetBoardWithNewMoves(String[] moves) {
        Log.d(TAG, "Reset board with new (MOVES): length = " + moves.length);
        for (String move : moves) {
            int row1 = move.charAt(1) - '0';
            int col1 = move.charAt(0) - '0';
            int row2 = move.charAt(3) - '0';
            int col2 = move.charAt(2) - '0';
            boardView_.makeMove(new Position(row1, col1), new Position(row2, col2), false);
        }
        boardView_.invalidate();
        adjustScreenOnFlagBasedOnGameStatus();
    }

    public void openNewPracticeTable() {
        Log.d(TAG, "Open a new practice table");
        boardView_.resetBoard();
        tableController_.setTableTitle();
    }
    
    public void updateBoardWithNewMove(String move) {
        Log.d(TAG, "Update board with a new (MOVE): Move: [" + move + "]");
        int row1 = move.charAt(1) - '0';
        int col1 = move.charAt(0) - '0';
        int row2 = move.charAt(3) - '0';
        int col2 = move.charAt(2) - '0';
        boardView_.makeMove(new Position(row1, col1), new Position(row2, col2), true);
    }

    /**
     * Make the current table an EMPTY one.
     */
    public void clearTable() {
        Log.d(TAG, "Clear the table. Make it an empty one.");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE); // No title.
        } else {
            Log.w(TAG, "clearTable: getSupportActionBar() = null. Do not set Display options!");
        }
        invalidateOptionsMenu(); // Recreate the options menu
        boardView_.resetBoard();

        ChatFragment chatFragment = myChatFragment_.get();
        if (chatFragment != null) {
            chatFragment.clearAll();
        } else {
            Log.w(TAG, "clearTable: Did not find the chat fragment. Do nothing.");
        }

        adjustScreenOnFlagBasedOnGameStatus();
    }
    
    public void onLocalPlayerJoined(ColorEnum myColor) {
        // Reverse the board view so that my seat is always at the bottom of the screen.
        if (  (myColor == ColorEnum.COLOR_RED && !isBlackOnTop_) ||
              (myColor == ColorEnum.COLOR_BLACK && isBlackOnTop_) ) {
            reverseView();
        }
    }
    
    public void onGameEnded(Enums.GameStatus gameStatus) {
        boardView_.onGameEnded(gameStatus);
        adjustScreenOnFlagBasedOnGameStatus();
    }

    public void onGameReset() {
        boardView_.resetBoard();
    }

    @Override
    public void onMessageReceived(MessageInfo messageInfo) {
        Log.d(TAG, "On new message: [" + messageInfo + "]");
        // Only interest in certain message-types.
        if (messageInfo.type == MessageInfo.MessageType.MESSAGE_TYPE_INVITE_TO_PLAY ||
                messageInfo.type == MessageInfo.MessageType.MESSAGE_TYPE_CHAT_PRIVATE) {
            notifCount_++;
            invalidateOptionsMenu(); // Recreate the options menu
        }
    }

    public void onNetworkCode(int networkCode) {
        //progressBar_.setVisibility(View.GONE);
    }

    public void showBriefMessage(int resId, int duration) {
        Snackbar.make(boardView_, resId, duration).show();
    }

    public void onLoginSuccess() {
        Log.d(TAG, "On Login Success...");
        if (isWaitingForTables) {
            isWaitingForTables = false;
            askNetworkControllerForTableList();
        }
    }

    public void showGameMessage_DRAW(String pid) {
        Snackbar.make(boardView_,
                getString(R.string.msg_player_offered_draw, pid), Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == JOIN_TABLE_REQUEST) {
            if (resultCode == RESULT_OK) {
                final String tableId = data.getStringExtra("tid");
                tableController_.handleTableSelection(tableId);
            }
        }
    }

    private void onBoardViewCreated(final MainActivity activity) {
        Log.d(TAG, "onBoardViewCreated...");

        progressBar_ = (ProgressBar) findViewById(R.id.progress_spinner);
        boardView_ = (BoardView) activity.findViewById(R.id.board_view);
        topPlayerLabel_ = (TextView) activity.findViewById(R.id.top_player_label);
        bottomPlayerLabel_ = (TextView) activity.findViewById(R.id.bottom_player_label);
        topPlayerButton_ = (Button) activity.findViewById(R.id.top_button);
        bottomPlayerButton_ = (Button) activity.findViewById(R.id.bottom_button);

        boardView_.setBoardEventListener(tableController_);

        // Setup the long-click handlers to handle BEGIN and END actions of replay.
        ImageButton previousButton = (ImageButton) activity.findViewById(R.id.replay_previous);
        if (previousButton != null) {
            previousButton.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    activity.onReplayBegin();
                    return true;
                }
            });
        }
        
        ImageButton nextButton = (ImageButton) activity.findViewById(R.id.replay_next);
        if (nextButton != null) {
            nextButton.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    activity.onReplayEnd();
                    return true;
                }
            });
        }
        
        // Game timers.
        TextView topGameTimeView = (TextView) activity.findViewById(R.id.top_game_time);
        TextView topMoveTimeView = (TextView) activity.findViewById(R.id.top_move_time);
        TextView bottomGameTimeView = (TextView) activity.findViewById(R.id.bottom_game_time);
        TextView bottomMoveTimeView = (TextView) activity.findViewById(R.id.bottom_move_time);
    
        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        timeTracker.setUITextViews(
                topGameTimeView, topMoveTimeView, bottomGameTimeView, bottomMoveTimeView);
        timeTracker.reset();
        
        // Player tracker.
        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        playerTracker.setUIViews(
                topPlayerLabel_, topPlayerButton_, bottomPlayerLabel_, bottomPlayerButton_);
        
        // Restore the previous state of the board.
        List<Move> historyMoves = HoxApp.getApp().getReferee().getHistoryMoves();
        int moveCount = historyMoves.size();
        int moveIndex = 0;
        for (Move move : historyMoves) {
            Log.d(TAG, "Update board with a new AI move = " + move.fromPosition + " => " + move.toPosition);
            final boolean isLastMove = (moveIndex == (moveCount - 1));
            boardView_.restoreMove(move.fromPosition, move.toPosition, isLastMove);
            ++moveIndex;
        }

        if (HoxApp.getApp().isGameOver()) {
            final GameStatus gameStatus = HoxApp.getApp().getGameStatus();
            Log.d(TAG, "... Game Over: gameStatus = " + gameStatus);
            boardView_.onGameEnded(gameStatus);
        }
        
        boardView_.invalidate();
        
        // Set table Id.
        tableController_.setTableTitle();

        SoundManager.getInstance().initialize(activity);
    }

    public void registerChatFragment(final ChatFragment fragment) {
        Log.d(TAG, "registerChatFragment: old:" + myChatFragment_.get() + " => new:" + fragment);
        myChatFragment_ = new WeakReference<ChatFragment>(fragment);
    }
    
    private void onBoardViewResume(MainActivity activity) {
        Log.d(TAG, "onBoardViewResume...");
        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        playerTracker.syncUI(); // Among things to be updated is AI Level.
    }
    
    public void setAndShowTitle(String title) {
        if (getSupportActionBar() == null) {
            Log.w(TAG, "setAndShowTitle: getSupportActionBar() = null. Do not set Display options!");
        } else if (TextUtils.isEmpty(title)) {
            getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE); // No title.
        } else {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
            getSupportActionBar().setTitle(getString(R.string.title_table, title));
        }
    }
    
    private void onReplayBegin() {
        boardView_.onReplay_BEGIN();
    }
    
    public void onReplayPrevious(View view) {
        boardView_.onReplay_PREV(true);
    }

    public void onReplayNext(View view) {
        boardView_.onReplay_NEXT(true);
    }
    
    private void onReplayEnd() {
        boardView_.onReplay_END();
    }

    public void onReverseView(View view) {
        reverseView();
    }

    public void onResetTable(View view) {
        tableController_.onClick_resetTable(this, view);
    }
    
    public void onTopButtonClick(View view) {
        Enums.ColorEnum clickedColor =
                (isBlackOnTop_ ? ColorEnum.COLOR_BLACK : ColorEnum.COLOR_RED);
        tableController_.handlePlayerButtonClick(clickedColor);
    }

    public void onBottomButtonClick(View view) {
        Enums.ColorEnum clickedColor =
                (isBlackOnTop_ ? ColorEnum.COLOR_RED : ColorEnum.COLOR_BLACK);
        tableController_.handlePlayerButtonClick(clickedColor);
    }
    
    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        private static final String TAG = "PlaceholderFragment";
        
        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            Log.d(TAG, "onCreateView...");
            return inflater.inflate(R.layout.fragment_main, container, false);
        }
        
        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            Log.d(TAG, "onActivityCreated...");
            
            MainActivity activity = (MainActivity) getActivity();
            activity.onBoardViewCreated(activity);
        }

        @Override
        public void onResume () {
            super.onResume();
            Log.i(TAG, "onResume...");
            MainActivity activity = (MainActivity) getActivity();
            activity.onBoardViewResume(activity);
        }
        
        @Override
        public void onDestroy () {
            super.onDestroy();
            Log.i(TAG, "onDestroy...");
        }
        
        @Override
        public void onDetach () {
            super.onDetach();
            Log.i(TAG, "onDetach...");
        }
    }

    /**
     * The ViewPager adapter for the main page.
     */
    public static class MainPagerAdapter extends FragmentPagerAdapter {
        private final Context context_;

        public MainPagerAdapter(Context context, FragmentManager fragmentManager) {
            super(fragmentManager);
            context_ = context;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return new PlaceholderFragment();
                case 1: return new ChatFragment();
                default: return new PlayersFragment();
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return context_.getString(R.string.label_table);
                case 1: return context_.getString(R.string.title_activity_chat);
                default: return context_.getString(R.string.action_view_players);
            }
        }

        /**
         * Reference: https://guides.codepath.com/android/ViewPager-with-FragmentPagerAdapter
         */
        @Override
        public float getPageWidth (int position) {
            switch (position) {
                case 0: return 0.95f;
                case 1: return 0.9f;
                default: return 1f;
            }
        }
    }
}
