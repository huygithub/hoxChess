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

import com.playxiangqi.hoxchess.Enums.ColorEnum;
import com.playxiangqi.hoxchess.Enums.TableType;

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
public class MainActivity extends ActionBarActivity implements HoxApp.SettingsObserver {

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
        
        HoxApp.getApp().registerSettingsObserver(this);
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
        } else {
            menu.findItem(R.id.action_offer_draw).setVisible(false);
            menu.findItem(R.id.action_offer_resign).setVisible(false);
            menu.findItem(R.id.action_close_table).setVisible(isInOnlineTable);
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
                Log.d(TAG, "Action 'New Table' clicked...");
                HoxApp.getApp().handleRequestToOpenNewTable();
                return true;
            case R.id.action_close_table:
                Log.d(TAG, "Action 'Close Table' clicked...");
                HoxApp.getApp().handleRequestToCloseCurrentTable();
                return true;
            case R.id.action_offer_draw:
                Log.d(TAG, "Action 'Offer Draw' clicked...");
                HoxApp.getApp().handleRequestToOfferDraw();
                return true;
            case R.id.action_offer_resign:
                Log.d(TAG, "Action 'Offer Resign' clicked...");
                HoxApp.getApp().handleRequestToOfferResign();
                return true;
            case R.id.action_play_online:
                Log.d(TAG, "Action 'Play Online' clicked...");
                HoxApp.getApp().handlePlayOnlineClicked();
                return true;
            case R.id.action_logout:
                Log.d(TAG, "Action 'Logout' clicked...");
                HoxApp.getApp().logoutFromNetwork();
                return true;
            case R.id.action_settings:
                Log.d(TAG, "Action 'Settings' clicked...");
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_reverse_view:
                Log.d(TAG, "Action 'Reverse View' clicked...");
                reverseView();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void reverseView() {
        isBlackOnTop_ = !isBlackOnTop_;
        boardView_.reverseView();
        
        // Swap the player labels and buttons.
        CharSequence savedText = topPlayerLabel_.getText();
        topPlayerLabel_.setText(bottomPlayerLabel_.getText());
        bottomPlayerLabel_.setText(savedText);
        
        savedText = topPlayerButton_.getText();
        topPlayerButton_.setText(bottomPlayerButton_.getText());
        bottomPlayerButton_.setText(savedText);
        
        // Timer.
        HoxApp.getApp().getTimeTracker().reverseView();
    }
    
    public void startActvityToListTables(String content) {
        Log.d(TAG, "Start activity (LIST): ENTER.");
        Intent intent = new Intent(this, TablesActivity.class);
        intent.putExtra("content", content);
        startActivityForResult(intent, JOIN_TABLE_REQUEST);
    }

    public void updateBoardWithNewTableInfo(TableInfo tableInfo) {
        Log.d(TAG, "Update board with new Table info (I_TABLE) from network: ENTER.");
        boardView_.resetBoard();
        boardView_.setTableType(TableType.TABLE_TYPE_NETWORK);
        setBlackInfo(tableInfo.getBlackInfo());
        setRedInfo(tableInfo.getRedInfo());
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
        
        final ColorEnum nextColor = boardView_.getNextColor();
        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        timeTracker.setInitialColor(nextColor);
        timeTracker.start();
    }

    public void openNewPracticeTable() {
        Log.d(TAG, "Open a new practice table");
        boardView_.onNewTableActionClicked();
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
    
    public void updateBoardAfterILeftTable() {
        Log.d(TAG, "Update board after I left the current table...");
        final int aiLevel = HoxApp.getApp().loadAILevelPreferences();
        updateAILabel(aiLevel); // Update the top-player 's label.
        bottomPlayerLabel_.setText(getString(R.string.you_label));
        boardView_.resetBoard();
    }

    private void setBlackInfo(String playerInfo) {
        if (isBlackOnTop_) {
            topPlayerLabel_.setText(playerInfo);
        } else {
            bottomPlayerLabel_.setText(playerInfo);
        }
    }

    private void setRedInfo(String playerInfo) {
        if (isBlackOnTop_) {
            bottomPlayerLabel_.setText(playerInfo);
        } else {
            topPlayerLabel_.setText(playerInfo);
        }
    }
    
    private void setBlackPossibleMode_PLAY() {
        if (isBlackOnTop_) {
            topPlayerButton_.setText(getString(R.string.button_play_black));
        } else {
            bottomPlayerButton_.setText(getString(R.string.button_play_black));
        }
    }

    private void setRedPossibleMode_PLAY() {
        if (isBlackOnTop_) {
            bottomPlayerButton_.setText(getString(R.string.button_play_red));
        } else {
            topPlayerButton_.setText(getString(R.string.button_play_red));
        }
    }
    
    private void setBlackPossibleMode_LEAVE() {
        if (isBlackOnTop_) {
            topPlayerButton_.setText("X");
        } else {
            bottomPlayerButton_.setText("X");
        }
    }
    
    private void setRedPossibleMode_LEAVE() {
        if (isBlackOnTop_) {
            bottomPlayerButton_.setText("X");
        } else {
            topPlayerButton_.setText("X");
        }
    }
    
    private void onMyColorChanged(Enums.ColorEnum myNewColor, Enums.ColorEnum myLastColor) {
        switch (myNewColor) {
            case COLOR_BLACK:
                setBlackPossibleMode_LEAVE();
                break;
                
            case COLOR_RED:
                setRedPossibleMode_LEAVE();
                break;
                
            case COLOR_NONE:
                if (myLastColor == ColorEnum.COLOR_BLACK) {
                    setBlackPossibleMode_PLAY();
                } else if (myLastColor == ColorEnum.COLOR_RED) {
                    setRedPossibleMode_PLAY();
                }
                break;
                
            default:
                break;
        }
    }
    
    public void updateBoardWithPlayerInfo(Enums.ColorEnum playerColor, String playerInfo) {
        if (playerColor == ColorEnum.COLOR_BLACK) {
            topPlayerLabel_.setText(playerInfo);
        } else if (playerColor == ColorEnum.COLOR_RED) {
            bottomPlayerLabel_.setText(playerInfo);
        }
    }

    public void onPlayerJoinedTable(String pid,
            Enums.ColorEnum playerColor, Enums.ColorEnum playerPreviousColor,
            String playerInfo,
            boolean myColorChanged, Enums.ColorEnum myLastColor) {
        
        // Cleanup the old seat first.
        if (playerPreviousColor == ColorEnum.COLOR_BLACK) {
            setBlackInfo("*");
        } else if (playerPreviousColor == ColorEnum.COLOR_RED) {
            setRedInfo("*");
        }
        
        // Assign the new seat.
        switch (playerColor) {
            case COLOR_BLACK:
            {
                setBlackInfo(playerInfo);
                break;
            }
            case COLOR_RED:
            {
                setRedInfo(playerInfo);
                break;
            }
            case COLOR_NONE:
            {
                break;
            }
            default:
                break; // Do nothing
        }
        
        // Handle the special case in which I changed my seat.
        if (myColorChanged) {
            onMyColorChanged(playerColor, myLastColor);
        }
    }
    
    public void onOtherPlayerLeftTable(Enums.ColorEnum playerPreviousColor) {
        Log.d(TAG, "Update board after other player (not I) left the current table...");
        if (playerPreviousColor == ColorEnum.COLOR_BLACK) {
            setBlackInfo("*");
            setBlackPossibleMode_PLAY();
        } else if (playerPreviousColor == ColorEnum.COLOR_RED) {
            setRedInfo("*");
            setRedPossibleMode_PLAY();
        }
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
        boardView_ = (BoardView) placeholderFragment_.getView().findViewById(R.id.board_view);
        if (boardView_ == null) {
            Log.e(TAG, "onCreate: The board view could not be found the placeholder fragment.");
        }
        
        topPlayerLabel_ = (TextView) placeholderFragment_.getView().findViewById(R.id.top_player_label);
        if (topPlayerLabel_ == null) {
            Log.e(TAG, "The Top-Player Label could not be found the placeholder fragment.");
        }
        bottomPlayerLabel_ = (TextView) placeholderFragment_.getView().findViewById(R.id.bottom_player_label);
        if (bottomPlayerLabel_ == null) {
            Log.e(TAG, "The Bottom-Player Label could not be found the placeholder fragment.");
        }

        topPlayerButton_ = (Button) placeholderFragment_.getView().findViewById(R.id.top_button);
        if (topPlayerButton_ == null) {
            Log.e(TAG, "The Top-Player Button could not be found the placeholder fragment.");
        }
        bottomPlayerButton_ = (Button) placeholderFragment_.getView().findViewById(R.id.bottom_button);
        if (bottomPlayerButton_ == null) {
            Log.e(TAG, "The Bottom-Player Button could not be found the placeholder fragment.");
        }
        
        // Game timers.
        TextView topGameTimeView = (TextView) placeholderFragment_.getView().findViewById(R.id.top_game_time);
        TextView topMoveTimeView = (TextView) placeholderFragment_.getView().findViewById(R.id.top_move_time);
        TextView bottomGameTimeView = (TextView) placeholderFragment_.getView().findViewById(R.id.bottom_game_time);
        TextView bottomMoveTimeView = (TextView) placeholderFragment_.getView().findViewById(R.id.bottom_move_time);
    
        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        timeTracker.setUITextViews(
                topGameTimeView, topMoveTimeView, bottomGameTimeView, bottomMoveTimeView);
        timeTracker.reset();
        
        // --------------
        
        final int aiLevel = HoxApp.getApp().loadAILevelPreferences();
        updateAILabel(aiLevel);
        updateAILevelOfBoard(aiLevel);
    }
    
    private boolean isBoardReady() {
        return (boardView_ != null);
    }
    
    /**
     * Callback when the settings are changed.
     * 
     * @see SettingsObserver
     */
    @Override
    public void onAILevelChanged(int newLevel) {
        Log.d(TAG, "on AI Level changed. newLevel = " +  newLevel);
        if (isBoardReady()) {
            updateAILevelOfBoard(newLevel);
            updateAILabel(newLevel);
        }
    }

    private void updateAILevelOfBoard(int newLevel) {
        boardView_.onAILevelChanged(newLevel);
    }
    
    private void updateAILabel(int aiLevel) {
        String labelString;
        switch (aiLevel) {
            case 1: labelString = getString(R.string.ai_label_medium); break;
            case 2: labelString = getString(R.string.ai_label_difficult); break;
            case 0: /* falls through */
            default: labelString = getString(R.string.ai_label_easy);
        }
        topPlayerLabel_.setText(labelString);
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
