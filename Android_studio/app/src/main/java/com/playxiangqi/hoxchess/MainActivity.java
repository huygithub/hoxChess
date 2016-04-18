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

import java.util.List;

import com.playxiangqi.hoxchess.Enums.ColorEnum;
import com.playxiangqi.hoxchess.Enums.GameStatus;
import com.playxiangqi.hoxchess.Enums.TableType;
import com.playxiangqi.hoxchess.Piece.Move;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
                        implements BoardView.BoardEventListener {

    private static final String TAG = "MainActivity";

    private static final String STATE_IS_BLACK_ON_TOP = "isBlackOnTop";

    private static final int JOIN_TABLE_REQUEST = 1;  // The request code
    
    private Fragment placeholderFragment_;
    private ProgressBar progressBar_;
    private BoardView boardView_;
    private TextView topPlayerLabel_;
    private TextView bottomPlayerLabel_;
    private Button topPlayerButton_;
    private Button bottomPlayerButton_;

    private boolean isBlackOnTop_ = true; // Normal view. Black player is at the top position.
    
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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE); // No title.
        } else {
            Log.w(TAG, "onCreate: getSupportActionBar() = null. Do not set Display options!");
        }
        
        Log.d(TAG, "onCreate: savedInstanceState = " + savedInstanceState + ".");
        placeholderFragment_ = new PlaceholderFragment();
        
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, placeholderFragment_)
                    .commit();
        }
        
        HoxApp.getApp().registerMainActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
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
        
        // Set up the new badge.
        // Reference:
        //  http://stackoverflow.com/questions/17696486/actionbar-notification-count-icon-like-google-have
        //
        View countView = menu.findItem(R.id.badge).getActionView();
        Button countButton = (Button) countView.findViewById(R.id.notif_count);
        countButton.setText(String.valueOf(notifCount_));

        GradientDrawable notiBackgroundShape = (GradientDrawable) countButton.getBackground();
        int notiColor = (notifCount_ > 0 ? R.color.noti_shape_bg_new : R.color.noti_shape_bg_zero);
        notiBackgroundShape.setColor(getResources().getColor(notiColor));

        countButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                notifCount_ = 0;
                invalidateOptionsMenu();
                openChatView();
            }
        });
        
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
            case R.id.action_play_online:
                progressBar_.setVisibility(View.VISIBLE);
                HoxApp.getApp().handlePlayOnlineClicked();
                return true;
            case R.id.action_logout:
                tableController_.handleLogoutFromNetwork();
                return true;
            case R.id.action_settings:
                openSettingsView();
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

    private void openChatView() {
        Log.d(TAG, "Open 'Chat' view...");
        Intent intent = new Intent(this, ChatBubbleActivity.class);
        startActivity(intent);
    }
    
    private void reverseView() {
        Log.d(TAG, "Reverse view...");
        isBlackOnTop_ = !isBlackOnTop_;
        boardView_.reverseView();

        HoxApp.getApp().getTimeTracker().reverseView();
        HoxApp.getApp().getPlayerTracker().reverseView();
    }
    
    public void startActivityToListTables(String content) {
        Log.d(TAG, "Start activity (LIST): ENTER.");

        progressBar_.setVisibility(View.GONE);
        
        Intent intent = new Intent(this, TablesActivity.class);
        intent.putExtra("content", content);
        startActivityForResult(intent, JOIN_TABLE_REQUEST);
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
    }
    
    public void updateBoardWithNewMove(String move) {
        Log.d(TAG, "Update board with a new (MOVE): Move: [" + move + "]");
        int row1 = move.charAt(1) - '0';
        int col1 = move.charAt(0) - '0';
        int row2 = move.charAt(3) - '0';
        int col2 = move.charAt(2) - '0';
        boardView_.makeMove(new Position(row1, col1), new Position(row2, col2), true);
    }
    
    public void clearTable() {
        Log.d(TAG, "Clear the table. Make it an empty one.");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE); // No title.
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        } else {
            Log.w(TAG, "clearTable: getSupportActionBar() = null. Do not set Display options!");
        }
        invalidateOptionsMenu(); // Recreate the options menu
        boardView_.resetBoard();
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

    public void onMessageReceived(String sender, String message) {
        Log.d(TAG, "On new message from: " + sender + " = [" + message + "]");
        notifCount_++;
        invalidateOptionsMenu(); // Recreate the options menu
    }
    
    public void onNetworkCode(int networkCode) {
        progressBar_.setVisibility(View.GONE);
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

    @Override
    public void onLocalMove(Position fromPos, Position toPos) {
        tableController_.onLocalMove(fromPos, toPos);
    }

    @Override
    public boolean isMyTurn() {
        return tableController_.isMyTurn();
    }

    private void onBoardViewCreated(final MainActivity activity) {
        Log.d(TAG, "onBoardViewCreated...");

        Toolbar myToolbar = (Toolbar) activity.findViewById(R.id.my_toolbar);
        activity.setSupportActionBar(myToolbar);

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
        if (HoxApp.getApp().isMyNetworkTableValid()) {
            setAndShowTitle(HoxApp.getApp().getMyNetworkTableId());
        }
    }
    
    private void onBoardViewResume(MainActivity activity) {
        Log.d(TAG, "onBoardViewResume...");
        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        playerTracker.syncUI(); // Among things to be updated is AI Level.
    }
    
    private void setAndShowTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
            getSupportActionBar().setTitle(getString(R.string.title_table, title));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            Log.w(TAG, "setAndShowTitle: getSupportActionBar() = null. Do not set Display options!");
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
            super.onDestroy();
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
}
