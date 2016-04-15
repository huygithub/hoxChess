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

import android.app.Application;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class HoxApp extends Application {

    private static final String TAG = "HoxApp";
    
    private static HoxApp thisApp_;
    
    private String pid_ = ""; // My player 's ID.
    private String password_ = ""; // My player 's password.
    
    private final Referee referee_ = new Referee();
    private final AIEngine aiEngine_ = new AIEngine();
    
    private WeakReference<MainActivity> mainActivity_ = new WeakReference<MainActivity>(null);

    // My color in the local (ie. to practice with AI).
    // NOTE: Currently, we cannot change my role in this type of table.
    private final ColorEnum myColorInLocalTable_ = ColorEnum.COLOR_RED;
    
    private TableTimeTracker timeTracker_ = new TableTimeTracker();
    private TablePlayerTracker playerTracker_ = new TablePlayerTracker(TableType.TABLE_TYPE_LOCAL);

    private NetworkController networkController_;

    public HoxApp() { /* Empty */ }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()...");
        thisApp_ = this;
        
        aiEngine_.initGame();
        aiEngine_.setAILevel(SettingsActivity.getAILevel(this));

        networkController_ = new NetworkController(timeTracker_,
                playerTracker_,
                referee_);
    }

    public static HoxApp getApp() {
        return thisApp_;
    }
    
    public int getAILevel() { return aiEngine_.getAILevel(); }

    public void onAILevelChanged(int aiLevel) {
        Log.d(TAG, "On new AI level: " + aiLevel);
        aiEngine_.setAILevel(aiLevel);
    }

    public void onAccountPidChanged(String pid) {
        Log.d(TAG, "On new pid: " + pid);
        if (!TextUtils.equals(pid_, pid)) {
            if (this.isOnline() && networkController_.isLoginOK()) {
                Log.i(TAG, "... (online & LoginOK) Skip using the new pid: " + pid + ".");
            } else {
                Log.i(TAG, "... (offline) Save new pid: " + pid + ".");
                pid_ = pid;
            }
        }
    }

    public void onAccountPasswordChanged(String password) {
        Log.d(TAG, "On new password...");
        if (!TextUtils.equals(password_, password)) {
            if (this.isOnline() && networkController_.isLoginOK()) {
                Log.i(TAG, "... (online & LoginOK) Skip using the new password.");
            } else {
                Log.i(TAG, "... (offline) Save new password.");
                password_ = password;
            }
        }
    }
    
    private void loadPreferences_Account() {
        boolean loginWithAccount = SettingsActivity.getLoginWithAccountFlag(this);
        if (loginWithAccount) {
            pid_ = SettingsActivity.getAccountPid(this);
            password_ = SettingsActivity.getAccountPassword(this);
            Log.d(TAG, "Load existing account. Player ID: [" + pid_ + "]");
        } else {
            pid_ = Utils.generateGuestPid();
            password_ = "";
            Log.d(TAG, "Load existing account. Guest ID: [" + pid_ + "]");
        }
    }
    
    //---------------------------------------------------------
    public void registerMainActivity(MainActivity activity) {
        mainActivity_ = new WeakReference<MainActivity>(activity);
        networkController_.setMainActivity(activity);
    }

    public void registerChatActivity(ChatBubbleActivity activity) {
        networkController_.setChatActivity(activity);
    }
    
    // --------------------------------------------------------
    /**
     * A message handler to handle UI related tasks.
     */
    private static final int MSG_AI_MOVE_READY = 1;
    private static final int MSG_NETWORK_EVENT = 2;
    private static final int MSG_NETWORK_CODE = 3;
    private final MessageHandler messageHandler_ = new MessageHandler();
    static class MessageHandler extends Handler {

        private Runnable aiRequest_; // Saved so that we could cancel it later.

        MessageHandler() {
            // empty
        }
        
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_AI_MOVE_READY:
            {
                HoxApp.getApp().onAIMoveMade((String) msg.obj);
                break;
            }
            case MSG_NETWORK_EVENT:
            {
                final String event = (String) msg.obj;
                HoxApp.getApp().onNetworkEvent(event);
                break;
            }
            case MSG_NETWORK_CODE:
            {
                HoxApp.getApp().onNetworkCode((Integer) msg.obj);
                break;
            }
            default:
                break;
            }
        }

        public void cancelPendingAIRequest() {
            if (aiRequest_ != null) {
                Log.d(TAG, "(MessageHandler) Cancel the pending AI request...");
                removeCallbacks(aiRequest_);
                aiRequest_ = null;
            }
        }

        public void postAIRequest(final AIEngine aiEngine, long delayMillis) {
            aiRequest_ = new Runnable() {
                public void run() {
                    aiRequest_ = null;
                    final String aiMove = aiEngine.generateMove();
                    Log.d(TAG, "... AI returned this move [" + aiMove + "].");
                    sendMessage(obtainMessage(MSG_AI_MOVE_READY, aiMove) );
                }
            };
            postDelayed(aiRequest_, delayMillis);
        }
    }
    
    private void onAIMoveMade(String aiMove) {
        Log.d(TAG, "AI returned this move [" + aiMove + "].");

        Position fromPos = new Position(aiMove.charAt(0) - '0', aiMove.charAt(1) - '0');
        Position toPos = new Position(aiMove.charAt(2) - '0', aiMove.charAt(3) - '0');
        
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.updateBoardWithNewAIMove(fromPos, toPos);
        }

        timeTracker_.nextColor();

        if (referee_.getMoveCount() == 2) {
            timeTracker_.start();
            if (mainActivity != null) {
                mainActivity.onGameStatusChanged();
            }
        }

        if (!referee_.isGameInProgress()) {
            Log.i(TAG, "The game has ended. Do nothing.");
            onGameEnded();
        }
    }
    
    private void onNetworkEvent(String eventString) {
        networkController_.handleNetworkEvent(eventString);
    }
    
    private void onNetworkCode(int networkCode) {
        Log.d(TAG, "On Network code: " + networkCode);
        
        switch (networkCode) {
            case NetworkPlayer.NETWORK_CODE_CONNECTED:
                Toast.makeText(HoxApp.thisApp_,
                        getString(R.string.msg_connection_established),
                        Toast.LENGTH_LONG).show();
                break;
            
            case NetworkPlayer.NETWORK_CODE_UNRESOLVED_ADDRESS:
                Toast.makeText(HoxApp.thisApp_,
                        "Failed to connect to the game server (UnresolvedAddressException)!",
                        Toast.LENGTH_LONG).show();
                break;

            case NetworkPlayer.NETWORK_CODE_IO_EXCEPTION:
                networkController_.handleNetworkError();
                break;

            case NetworkPlayer.NETWORK_CODE_DISCONNECTED:
                Toast.makeText(HoxApp.thisApp_,
                        getString(R.string.msg_connection_disconnected),
                        Toast.LENGTH_LONG).show();
                break;

            default:
                break;
        }
        
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.onNetworkCode(networkCode);
        }
    }

    private void onGameEnded() {
        Log.d(TAG, "On game-ended...");
        timeTracker_.stop();
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.onGameStatusChanged();
        }
    }
    
    public void handlePlayOnlineClicked() {
        Log.d(TAG, "Action 'Play Online' clicked...");
        if ( !networkController_.isOnline() || !networkController_.isLoginOK() ) {
            loadPreferences_Account(); // to get pid_ and password_

            networkController_.setLoginInfo(pid_,  password_);
            networkController_.connectToServer();
        } else {
            networkController_.handleMyRequestToGetListOfTables();
        }
    }

    public boolean isOnline() {
        return networkController_.isOnline();
    }

    public String getMyPid() {
        return pid_;
    }
    
    public ColorEnum getMyColor() {
        switch (playerTracker_.getTableType()) {
            case TABLE_TYPE_LOCAL:
                return myColorInLocalTable_;

            case TABLE_TYPE_NETWORK:
                return networkController_.getMyColor();

            case TABLE_TYPE_EMPTY: // falls through
            default:
                return ColorEnum.COLOR_UNKNOWN;
        }
    }
    
    public boolean isGameOver() {
        return networkController_.isGameOver();
    }
    
    public boolean isGameInProgress() {
        return ( !isGameOver() &&
                referee_.getMoveCount() > 1 );
    }
    
    public GameStatus getGameStatus() {
        return networkController_.getGameStatus();
    }

    public boolean isMyNetworkTableValid() {
        return networkController_.isMyTableValid();
    }

    public String getMyNetworkTableId() {
        return networkController_.getMyTableId();
    }

    public TableTimeTracker getTimeTracker() {
        return timeTracker_;
    }

    public TablePlayerTracker getPlayerTracker() {
        return playerTracker_;
    }
    
    public Referee getReferee() {
        return referee_;
    }
    
    public List<ChatMessage> getNewMessages() {
        return networkController_.getNewMessages();
    }
    
    public boolean isMyTurn() {
        switch (playerTracker_.getTableType()) {
            case TABLE_TYPE_LOCAL:
                return (myColorInLocalTable_ == referee_.getNextColor());
                
            case TABLE_TYPE_NETWORK:
                final ColorEnum myColor = networkController_.getMyColor();
                return ((myColor == ColorEnum.COLOR_RED || myColor == ColorEnum.COLOR_BLACK) &&
                        playerTracker_.hasEnoughPlayers() &&
                        myColor == referee_.getNextColor());
                
            case TABLE_TYPE_EMPTY: // falls through
            default:
                return false;
        }
    }
    
    public void logoutFromNetwork() {
        networkController_.logoutFromNetwork();
    }
    
    public void handleTableSelection(String tableId) {
        Log.i(TAG, "Select table: " + tableId + ".");
        networkController_.handleTableSelection(tableId);
    }

    public void handleRequestToCloseCurrentTable() {
        Log.i(TAG, "Close the current table...");
        networkController_.handleRequestToCloseCurrentTable();
    }
    
    public void handleRequestToOpenNewTable() {
        Log.i(TAG, "Request to open a new table...");
        
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity == null) {
            Log.w(TAG, "The main activity is NULL. Ignore this 'open new table request'.");
            return;
        }
        
        // Case 1: I am not online at all.
        if (!this.isOnline() && !networkController_.isMyTableValid()) {
            playerTracker_.setTableType(TableType.TABLE_TYPE_LOCAL); // A new practice table.
            playerTracker_.syncUI();
            aiEngine_.initGame();
            timeTracker_.stop();
            timeTracker_.reset();
            mainActivity.openNewPracticeTable();
        }
        // Case 2: I am online and am not playing in any table.
        else if (this.isOnline()) {
            networkController_.handleMyRequestToOpenNewTable();
        }
        else {
            Log.w(TAG, "Either offline or currently playing. Ignore this 'open new table request'.");
        }
    }
    
    public void handleRequestToOfferDraw() {
        networkController_.handleRequestToOfferDraw();
    }
    
    public void handleRequestToOfferResign() {
        networkController_.handleRequestToOfferResign();
    }
    
    public void handleRequestToResetTable() {
        Log.i(TAG, "Handle request to 'Reset Table'...");
        switch (playerTracker_.getTableType()) {
            case TABLE_TYPE_LOCAL:
                messageHandler_.cancelPendingAIRequest();
                playerTracker_.syncUI();
                aiEngine_.initGame();
                timeTracker_.stop();
                timeTracker_.reset();
                MainActivity mainActivity = mainActivity_.get();
                if (mainActivity != null) {
                    mainActivity.openNewPracticeTable();
                }
                break;

            case TABLE_TYPE_NETWORK:
                networkController_.handleRequestToResetTable();
                break;

            case TABLE_TYPE_EMPTY: // falls through
            default:
                Log.e(TAG, "Handle request to reset table: Unsupported table type.");
                break;
        }
    }
    
    public void handlePlayerButtonClick(Enums.ColorEnum clickedColor) {
        Log.i(TAG, "Handle player-button click. clickedColor = " + clickedColor);

        switch (playerTracker_.getTableType()) {
            case TABLE_TYPE_LOCAL:
                break; // Do nothing if this is NOT a network table.

            case TABLE_TYPE_NETWORK:
                networkController_.handleMyRequestToChangeRole(clickedColor);
                break;

            case TABLE_TYPE_EMPTY: // falls through
            default:
                Log.e(TAG, "Handle player-button click: Unsupported table type.");
                break;
        }
    }
    
    public void handleLocalMessage(ChatMessage chatMsg) {
        Log.d(TAG, "Handle local message: [" + chatMsg.message + "]");
        networkController_.handleLocalMessage(chatMsg);
    }

    public void handleLocalMove(Position fromPos, Position toPos) {
        Log.i(TAG, "Handle local move: referee 's moveCount = " + referee_.getMoveCount());
        
        timeTracker_.nextColor();
        
        if (referee_.getMoveCount() == 2) {
            timeTracker_.start();
            MainActivity mainActivity = mainActivity_.get();
            if (mainActivity != null) {
                mainActivity.onGameStatusChanged();
            }
        }
        
        if (referee_.getMoveCount() > 1) { // The game has started?
            playerTracker_.syncUI();
        }

        switch (playerTracker_.getTableType()) {
            case TABLE_TYPE_LOCAL:
                aiEngine_.onHumanMove(fromPos.row, fromPos.column, toPos.row, toPos.column);
                if (!referee_.isGameInProgress()) {
                    onGameEnded(); // The game has ended. Do nothing
                } else {
                    final long delayMillis = 2000; // Add delay so we have time to observe the move.
                    Log.d(TAG, "Will ask AI to generate a move after some delay:" + delayMillis);
                    messageHandler_.postAIRequest(aiEngine_, delayMillis);
                }
                break;

            case TABLE_TYPE_NETWORK:
                networkController_.handleRequestToSendMove(fromPos, toPos);
                break;

            case TABLE_TYPE_EMPTY: // falls through
            default:
                Log.e(TAG, "Handle local move:: Unsupported table type.");
                break;
        }
    }
    
    public void postNetworkEvent(String eventString) {
        Log.d(TAG, "Post Network event = [" + eventString + "]");
        messageHandler_.sendMessage(
                messageHandler_.obtainMessage(MSG_NETWORK_EVENT, eventString) );
    }

    public void postNetworkCode(int networkCode) {
        Log.d(TAG, "Post Network code = [" + networkCode + "]");
        messageHandler_.sendMessage(
                messageHandler_.obtainMessage(MSG_NETWORK_CODE, networkCode) );
    }
    
}
