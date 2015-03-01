/**
 *  Copyright 2014 Huy Phan <huyphan@playxiangqi.com>
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

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * The main (entry-point) activity.
 */
public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";

    private static final int JOIN_TABLE_REQUEST = 1;  // The request code
    
    private Fragment placeholderFragment_;
    private BoardView boardView_;
    private TextView topPlayerLabel_;
    private TextView bottomPlayerLabel_;
    private Button topPlayerButton_;
    private Button bottomPlayerButton_;
    
    private boolean isBlackOnTop_ = true; // Normal view. Black player is at the top position.
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Log.d(TAG, "onCreate: savedInstanceState = " + savedInstanceState + ".");
        placeholderFragment_ = new PlaceholderFragment();
        
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, placeholderFragment_)
                    .commit();
        }
        
        HoxApp.getApp().registerMainActivity(this);
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
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "(ActionBar) onPrepareOptionsMenu");
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_logout).setVisible(
                HoxApp.getApp().isOnline());
        
        final boolean isInOnlineTable = HoxApp.getApp().getMyTable().isValid();
        final ColorEnum myColor = HoxApp.getApp().getMyColor();
        final boolean isGameOver = HoxApp.getApp().isGameOver();
        final int moveCount = boardView_.getMoveCount();
        
        if (myColor == ColorEnum.COLOR_BLACK || myColor == ColorEnum.COLOR_RED) {
            if (moveCount >= 2) { // The game has actually started?
                menu.findItem(R.id.action_offer_draw).setVisible(!isGameOver);
                menu.findItem(R.id.action_offer_resign).setVisible(!isGameOver);
                menu.findItem(R.id.action_close_table).setVisible(isGameOver);
            } else {
                menu.findItem(R.id.action_offer_draw).setVisible(false);
                menu.findItem(R.id.action_offer_resign).setVisible(false);
                menu.findItem(R.id.action_close_table).setVisible(true);
            }
            menu.findItem(R.id.action_reset_table).setVisible(
                    isGameOver || (moveCount < 2));
            
        } else {
            menu.findItem(R.id.action_offer_draw).setVisible(false);
            menu.findItem(R.id.action_offer_resign).setVisible(false);
            menu.findItem(R.id.action_close_table).setVisible(isInOnlineTable);
            menu.findItem(R.id.action_reset_table).setVisible(false);
        }
        return true; // display the menu
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_new_table:
                HoxApp.getApp().handleRequestToOpenNewTable();
                return true;
            case R.id.action_close_table:
                HoxApp.getApp().handleRequestToCloseCurrentTable();
                return true;
            case R.id.action_offer_draw:
                HoxApp.getApp().handleRequestToOfferDraw();
                return true;
            case R.id.action_offer_resign:
                HoxApp.getApp().handleRequestToOfferResign();
                return true;
            case R.id.action_reset_table:
                HoxApp.getApp().handleRequestToResetTable();
                return true;
            case R.id.action_play_online:
                HoxApp.getApp().handlePlayOnlineClicked();
                return true;
            case R.id.action_logout:
                HoxApp.getApp().logoutFromNetwork();
                return true;
            case R.id.action_settings:
                openSettingsView();
                return true;
            case R.id.action_reverse_view:
                reverseView();
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
    
    private void reverseView() {
        Log.d(TAG, "Reverse view...");
        
        isBlackOnTop_ = !isBlackOnTop_;
        boardView_.reverseView();
        
        // Time and Player trackers.
        HoxApp.getApp().getTimeTracker().reverseView();
        HoxApp.getApp().getPlayerTracker().reverseView();
    }
    
    public void startActvityToListTables(String content) {
        Log.d(TAG, "Start activity (LIST): ENTER.");
        Intent intent = new Intent(this, TablesActivity.class);
        intent.putExtra("content", content);
        startActivityForResult(intent, JOIN_TABLE_REQUEST);
    }

    public void updateBoardWithNewAIMove(Position fromPos, Position toPos) {
        Log.d(TAG, "Update board with a new AI move = " + fromPos + " => " + toPos);
        boardView_.onAIMoveMade(fromPos, toPos);
        boardView_.invalidate();
    }
    
    public void updateBoardWithNewTableInfo(TableInfo tableInfo) {
        Log.d(TAG, "Update board with new network Table info (I_TABLE)...");
        boardView_.resetBoard();
        boardView_.setTableType(TableType.TABLE_TYPE_NETWORK);
    }

    public void resetBoardWithNewMoves(String[] moves) {
        Log.d(TAG, "Reset board with new (MOVES): ENTER.");
        for (String move : moves) {
            Log.d(TAG, ".......... Move [" + move + "]");
            int row1 = move.charAt(1) - '0';
            int col1 = move.charAt(0) - '0';
            int row2 = move.charAt(3) - '0';
            int col2 = move.charAt(2) - '0';
            Log.i(TAG, "... Network move [ " + row1 + ", " + col1 + " => " + row2 + ", " + col2 + "]");
            boardView_.onNetworkMoveMade(new Position(row1, col1), new Position(row2, col2));
        }
        boardView_.invalidate();
        
        final ColorEnum nextColor = HoxApp.getApp().getReferee().getNextColor();
        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        timeTracker.setInitialColor(nextColor);
        timeTracker.start();
    }

    public void openNewPracticeTable() {
        Log.d(TAG, "Open a new practice table");
        boardView_.resetBoard();
        boardView_.setTableType(TableType.TABLE_TYPE_LOCAL);
    }
    
    public void updateBoardWithNewMove(String move) {
        Log.d(TAG, "Update board with a new (MOVE): Move: [" + move + "]");
        int row1 = move.charAt(1) - '0';
        int col1 = move.charAt(0) - '0';
        int row2 = move.charAt(3) - '0';
        int col2 = move.charAt(2) - '0';
        Log.i(TAG, "... Network move [ " + row1 + ", " + col1 + " => " + row2 + ", " + col2 + "]");
        boardView_.onNetworkMoveMade(new Position(row1, col1), new Position(row2, col2));
        boardView_.invalidate();
    }
    
    public void clearTable() {
        Log.d(TAG, "Clear the table. Make it an empty one.");
        boardView_.resetBoard();
    }
    
    public void onGameEnded(Enums.GameStatus gameStatus) {
        boardView_.onGameEnded(gameStatus);
    }

    public void onGameReset() {
        boardView_.resetBoard();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == JOIN_TABLE_REQUEST) {
            if (resultCode == RESULT_OK) {
                final String tableId = data.getStringExtra("tid");
                HoxApp.getApp().handleTableSelection(tableId);
            }
        }
    }
    
    private void onBoardViewCreated() {
        Log.d(TAG, "onBoardViewCreated...");
        
        boardView_ = (BoardView) placeholderFragment_.getView().findViewById(R.id.board_view);
        topPlayerLabel_ = (TextView) placeholderFragment_.getView().findViewById(R.id.top_player_label);
        bottomPlayerLabel_ = (TextView) placeholderFragment_.getView().findViewById(R.id.bottom_player_label);
        topPlayerButton_ = (Button) placeholderFragment_.getView().findViewById(R.id.top_button);
        bottomPlayerButton_ = (Button) placeholderFragment_.getView().findViewById(R.id.bottom_button);
        
        // Game timers.
        TextView topGameTimeView = (TextView) placeholderFragment_.getView().findViewById(R.id.top_game_time);
        TextView topMoveTimeView = (TextView) placeholderFragment_.getView().findViewById(R.id.top_move_time);
        TextView bottomGameTimeView = (TextView) placeholderFragment_.getView().findViewById(R.id.bottom_game_time);
        TextView bottomMoveTimeView = (TextView) placeholderFragment_.getView().findViewById(R.id.bottom_move_time);
    
        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        timeTracker.setUITextViews(
                topGameTimeView, topMoveTimeView, bottomGameTimeView, bottomMoveTimeView);
        timeTracker.reset();
        
        // Player tracker.
        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        playerTracker.setUIViews(
                topPlayerLabel_, topPlayerButton_, bottomPlayerLabel_, bottomPlayerButton_);
        playerTracker.syncUI();
        
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
        
        final GameStatus gameStatus = HoxApp.getApp().getGameStatus();
        Log.d(TAG, "... gameStatus = " + gameStatus);
        if (    gameStatus == GameStatus.GAME_STATUS_BLACK_WIN ||
                gameStatus == GameStatus.GAME_STATUS_RED_WIN ||
                gameStatus == GameStatus.GAME_STATUS_DRAWN) {
           boardView_.onGameEnded(gameStatus);
        }
        
        boardView_.invalidate();
    }
    
    public void onReplayBegin(View view) {
        boardView_.onReplay_BEGIN();
    }
    
    public void onReplayPrevious(View view) {
        boardView_.onReplay_PREV(true);
    }

    public void onReplayNext(View view) {
        boardView_.onReplay_NEXT(true);
    }
    
    public void onReplayEnd(View view) {
        boardView_.onReplay_END();
    }
    
    public void onTopButtonClick(View view) {
        Enums.ColorEnum clickedColor =
                (isBlackOnTop_ ? ColorEnum.COLOR_BLACK : ColorEnum.COLOR_RED);
        HoxApp.getApp().handlePlayerButtonClick(clickedColor);
    }

    public void onBottomButtonClick(View view) {
        Enums.ColorEnum clickedColor =
                (isBlackOnTop_ ? ColorEnum.COLOR_RED : ColorEnum.COLOR_BLACK);
        HoxApp.getApp().handlePlayerButtonClick(clickedColor);
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
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
        
        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            Log.d(TAG, "onActivityCreated...");
            
            ((MainActivity) getActivity()).onBoardViewCreated();
        }
        
        @Override
        public void onDestroy () {
            super.onDestroy();
            Log.e(TAG, "onDestroy...");
        }
        
        @Override
        public void onDetach () {
            super.onDetach();
            Log.e(TAG, "onDetach...");
        }
    }
}
