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

import java.lang.ref.WeakReference;
import java.util.HashMap;

import com.playxiangqi.hoxchess.Enums.ColorEnum;
import com.playxiangqi.hoxchess.Enums.GameStatus;
import com.playxiangqi.hoxchess.Enums.TableType;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class HoxApp extends Application {

    private static final String TAG = "HoxApp";
    
    private static HoxApp thisApp_;
    
    private int currentAILevel_ = -1; // Default = "invalid level"
    
    private String pid_ = ""; // My player 's ID.
    private String password_ = ""; // My player 's password.
    private String myRating_ = "0";
    
    private Referee referee_;
    
    private AIEngine aiEngine_ = new AIEngine();
    
    private NetworkPlayer networkPlayer_;
    private boolean isLoginOK_ = false;
    
    private WeakReference<MainActivity> mainActivity_;
    private TableInfo myTable_ = new TableInfo();
    private ColorEnum myColor_ = ColorEnum.COLOR_RED;
    
    private boolean isGameOver_ = false;
    private GameStatus gameStatus_ = GameStatus.GAME_STATUS_UNKNOWN;
    
    private TableTimeTracker timeTracker_ = new TableTimeTracker();
    private TablePlayerTracker playerTracker_ = new TablePlayerTracker(TableType.TABLE_TYPE_LOCAL);
    
    // ----------------
    public class AccountInfo {
        public String username;
        public String password;
    }
    // ----------------
    
    public HoxApp() {
        // Empty
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()...");
        thisApp_ = this;
        
        referee_ = new Referee();
        
        aiEngine_.initGame();
        currentAILevel_ = SettingsActivity.getAILevel(this);
        aiEngine_.setDifficultyLevel(currentAILevel_);
        
        networkPlayer_ = new NetworkPlayer();
        if (!networkPlayer_.isAlive()) {
            networkPlayer_.start();
        }
    }

    public static HoxApp getApp() {
        return thisApp_;
    }
    
    public int getAILevel() {
        return currentAILevel_;
    }

    public void onAILevelChanged(int aiLevel) {
        Log.d(TAG, "On new AI level: " + aiLevel);        
        currentAILevel_ = aiLevel;
        aiEngine_.setDifficultyLevel(currentAILevel_);
    }

    public void onAccountPidChanged(String pid) {
        Log.d(TAG, "On new pid: " + pid);
        if (!TextUtils.equals(pid_, pid)) {
            if (this.isOnline() && isLoginOK_) {
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
            if (this.isOnline() && isLoginOK_) {
                Log.i(TAG, "... (online & LoginOK) Skip using the new password.");
            } else {
                Log.i(TAG, "... (offline) Save new password.");
                password_ = password;
            }
        }
    }
    
    public AccountInfo loadPreferences_Account() {
        AccountInfo accountInfo = new AccountInfo();
        
        accountInfo.username = SettingsActivity.getAccountPid(this);
        accountInfo.password = SettingsActivity.getAccountPassword(this);
        Log.d(TAG, "Load existing account. username: [" + accountInfo.username + "]");
        
        return accountInfo;
    }
    
    //---------------------------------------------------------
    public void registerMainActivity(MainActivity activity) {
        mainActivity_ = new WeakReference<MainActivity>(activity);
    }
    
    // --------------------------------------------------------
    /**
     * A message handler to handle UI related tasks.
     */
    private static final int MSG_AI_MOVE_READY = 1;
    private static final int MSG_NETWORK_EVENT = 2;
    private static final int MSG_NETWORK_CODE = 3;
    private Handler messageHandler_ = new MessageHandler();
    static class MessageHandler extends Handler {
        
        MessageHandler() {
            // empty
        }
        
        @Override
        public void handleMessage(Message msg){
            switch (msg.what) {
            case MSG_AI_MOVE_READY:
            {
                String aiMove = (String) msg.obj;
                Log.d(TAG, "(MessageHandler) AI returned this move [" + aiMove + "].");
                int row1 = aiMove.charAt(0) - '0';
                int col1 = aiMove.charAt(1) - '0';
                int row2 = aiMove.charAt(2) - '0';
                int col2 = aiMove.charAt(3) - '0';
                HoxApp.getApp().onAIMoveMade(new Position(row1, col1), new Position(row2, col2));
                break;
            }
            case MSG_NETWORK_EVENT:
            {
                String event = (String) msg.obj;
                Log.d(TAG, "(MessageHandler) Network event arrived.");
                HoxApp.getApp().onNetworkEvent(event);
                break;
            }
            case MSG_NETWORK_CODE:
            {
                int networkCode = ((Integer) msg.obj).intValue();
                HoxApp.getApp().onNetworkCode(networkCode);
                break;
            }            
            default:
                break;
            }
        }
    };
    
    private void onAIMoveMade(Position fromPos, Position toPos) {
        Log.d(TAG, "On AI move = " + fromPos + " => " + toPos);
        
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.updateBoardWithNewAIMove(fromPos, toPos);
        }
    }
    
    private void onNetworkEvent(String eventString) {
        Log.d(TAG, "On Network event. ENTER.");
        
        HashMap<String, String> newEvent = new HashMap<String, String>();
        
        for (String token : eventString.split("&")) {
            //Log.d(TAG, "... token = [" + token + "]");
            final String[] pair = token.split("=");
            newEvent.put(pair[0], pair[1]);
        }
        
        final String op = newEvent.get("op");
        final int code = Integer.parseInt( newEvent.get("code") );
        final String content = newEvent.get("content");
        
        if ("LOGIN".equals(op)) {
            handleNetworkEvent_LOGIN(code, content);
        } else if (code != 0) {  // Error
            Log.i(TAG, "... Received an ERROR event: [" + code + ": " + content + "]");
        } else if ("LIST".equals(op)) {
            handleNetworkEvent_LIST(content);
        } else if ("I_TABLE".equals(op)) {
            handleNetworkEvent_I_TABLE(content);
        } else if ("I_MOVES".equals(op)) {
            handleNetworkEvent_I_MOVES(content);
        } else if ("MOVE".equals(op)) {
            handleNetworkEvent_MOVE(content);
        } else if ("LEAVE".equals(op)) {
            handleNetworkEvent_LEAVE(content);
        } else if ("E_JOIN".equals(op)) {
            handleNetworkEvent_E_JOIN(content);
        } else if ("E_END".equals(op)) {
            handleNetworkEvent_E_END(content);
        } else if ("RESET".equals(op)) {
            handleNetworkEvent_RESET(content);
        } else if ("DRAW".equals(op)) {
            handleNetworkEvent_DRAW(content);
        }
    }
    
    private void onNetworkCode(int networkCode) {
        Log.d(TAG, "On Network code:" + networkCode + ". ENTER.");
        
        switch (networkCode) {
            case NetworkPlayer.NETWORK_CODE_CONNECTED:
                Toast.makeText(HoxApp.thisApp_,
                        "Connection is establed",
                        Toast.LENGTH_LONG).show();
                break;
            
            case NetworkPlayer.NETWORK_CODE_UNRESOLVED_ADDRESS:
                Toast.makeText(HoxApp.thisApp_,
                        "Failed to connect to the game server (UnresolvedAddressException)!",
                        Toast.LENGTH_LONG).show();
                break;

            case NetworkPlayer.NETWORK_CODE_IO_EXCEPTION:
                Toast.makeText(HoxApp.thisApp_,
                        "An IOException exception while handling network messages!",
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
    
    private String getLocalizedLoginError(int code) {
        switch (code)
        {
            case 6: return getString(R.string.login_error_wrong_password);
            case 7: return getString(R.string.login_error_wrong_username);
        }
        return getString(R.string.login_error_general_error);
    }
    
    private void handleNetworkEvent_LOGIN(int code, String content) {
        Log.d(TAG, "Handle event (LOGIN): ENTER.");
        
        isLoginOK_ = (code == 0);
        
        if (!isLoginOK_) {  // Error
            Log.i(TAG, "Login failed. Code: [" + code + "], Error: [" + content + "]");
            Toast.makeText(getApplicationContext(),
                    getLocalizedLoginError(code), Toast.LENGTH_LONG).show();
            
            networkPlayer_.disconnectFromServer();
            return;
        }
        
        final String[] components = content.split(";");
        final String pid = components[0];
        final String rating = components[1];
        Log.d(TAG, ">>> [" + pid + " " + rating + "] LOGIN.");
        
        if (pid_.equals(pid)) { // my LOGIN?
            Log.i(TAG, "Received my LOGIN info [" + pid + " " + rating + "].");
            myRating_ = rating;
            
            myColor_ = ColorEnum.COLOR_UNKNOWN;
            playerTracker_.setTableType(TableType.TABLE_TYPE_EMPTY);
            playerTracker_.syncUI();
            
            networkPlayer_.sendRequest_LIST();
        }
    }
    
    private void handleNetworkEvent_LIST(String content) {
        Log.d(TAG, "Handle event (LIST): ENTER.");
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.startActvityToListTables(content);
        }
    }

    private void handleNetworkEvent_I_TABLE(String content) {
        Log.d(TAG, "Handle event (I_TABLE): ENTER.");
        
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity == null) {
            Log.w(TAG, "The main activity is NULL. Ignore this I_TABLE event.");
            return;
        }
        
        isGameOver_ = false;
        gameStatus_ = GameStatus.GAME_STATUS_UNKNOWN;
        myTable_ = new TableInfo(content);
        
        if (pid_.equals(myTable_.blackId)) {
            myColor_ = ColorEnum.COLOR_BLACK;
        } else if (pid_.equals(myTable_.redId)) {
            myColor_ = ColorEnum.COLOR_RED;
        } else {
            myColor_ = ColorEnum.COLOR_NONE;
        }
        
        Log.i(TAG, "Set my table Id: " + myTable_.tableId + ", myColor: " + myColor_);
        
        timeTracker_.stop();
        timeTracker_.setInitialColor(ColorEnum.COLOR_RED);
        timeTracker_.setInitialTime( new TimeInfo(myTable_.itimes) );
        timeTracker_.setBlackTime( new TimeInfo(myTable_.blackTimes) );
        timeTracker_.setRedTime( new TimeInfo(myTable_.redTimes) );
        timeTracker_.syncUI();
        
        playerTracker_.setTableType(TableType.TABLE_TYPE_NETWORK);
        playerTracker_.setBlackInfo(myTable_.blackId, myTable_.blackRating);
        playerTracker_.setRedInfo(myTable_.redId, myTable_.redRating);
        playerTracker_.syncUI();
        
        mainActivity.updateBoardWithNewTableInfo(myTable_);
    }

    private void handleNetworkEvent_I_MOVES(String content) {
        Log.d(TAG, "Handle event (I_MOVES): ENTER.");
        final String[] components = content.split(";");
        final String tableId = components[0];
        final String movesStr = components[1];
        
        if (!myTable_.hasId(tableId)) { // not the table I am interested in?
            Log.w(TAG, "Ignore the list of MOVES from table: " + tableId);
            return;
        }
        
        final String[] moves = movesStr.split("/");
        
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.resetBoardWithNewMoves(moves);
        }
    }

    private void handleNetworkEvent_MOVE(String content) {
        Log.d(TAG, "Handle event (MOVE): ENTER.");
        final String[] components = content.split(";");
        final String tableId = components[0];
        final String move = components[2];
        
        if (!myTable_.hasId(tableId)) { // not the table I am interested in?
            Log.w(TAG, "Ignore a MOVE from table: " + tableId);
            return;
        }
        
        timeTracker_.nextColor();
        timeTracker_.start();
        
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.updateBoardWithNewMove(move);
        }
        
        if (referee_.getMoveCount() > 1) { // The game has started?
            playerTracker_.syncUI();
        }
    }
    
    private void handleNetworkEvent_LEAVE(String content) {
        Log.d(TAG, "Handle event (LEAVE): ENTER.");
        final String[] components = content.split(";");
        final String tableId = components[0];
        final String pid = components[1];
        
        if (!myTable_.hasId(tableId)) { // not the table I am interested in?
            Log.w(TAG, "Ignore the LEAVE event.");
            return;
        }
        
        // Check if I just left the Table.
        if (pid.equals(pid_)) {
            Log.i(TAG, "I just left my table: " + tableId);
            myTable_ = new TableInfo();
            myColor_ = ColorEnum.COLOR_UNKNOWN;
            gameStatus_ = GameStatus.GAME_STATUS_UNKNOWN;
            timeTracker_.stop();
            MainActivity mainActivity = mainActivity_.get();
            if (mainActivity != null) {
                mainActivity.clearTable();
            }
        
        } else { // Other player left my table?
            myTable_.onPlayerLeft(pid);
        }
        
        playerTracker_.onPlayerLeave(pid);
        playerTracker_.syncUI();
    }
    
    private Enums.ColorEnum stringToPlayerColor(String color) {
        if ("Red".equals(color)) return ColorEnum.COLOR_RED;
        if ("Black".equals(color)) return ColorEnum.COLOR_BLACK;
        if ("None".equals(color)) return ColorEnum.COLOR_NONE;
        return ColorEnum.COLOR_UNKNOWN;
    }
    
    private void handleNetworkEvent_E_JOIN(String content) {
        Log.d(TAG, "Handle event (E_JOIN): ENTER.");
        final String[] components = content.split(";");
        final String tableId = components[0];
        final String pid = components[1];
        final String rating = components[2];
        final String color = components[3];
        
        if (!myTable_.hasId(tableId)) { // not the table I am interested in?
            Log.w(TAG, "Ignore the E_JOIN event.");
            return;
        }
        
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity == null) {
            Log.w(TAG, "The main activity is NULL. Ignore this E_JOIN event.");
            return;
        }
        
        final Enums.ColorEnum playerColor = stringToPlayerColor(color);
        
        myTable_.onPlayerJoined(pid, rating, playerColor);
        
        if ("Red".equals(color)) {
            if (pid.equals(pid_)) {
                myColor_ = ColorEnum.COLOR_RED;
            }
            
        } else if ("Black".equals(color)) {
            if (pid.equals(pid_)) {
                myColor_ = ColorEnum.COLOR_BLACK;
            }
            
        } else if ("None".equals(color)) {
            if (pid.equals(pid_)) {
                myColor_ = ColorEnum.COLOR_NONE;
            }
        }
        
        playerTracker_.onPlayerJoin(pid, rating, playerColor);
        playerTracker_.syncUI();
    }
    
    private void handleNetworkEvent_E_END(String content) {
        Log.d(TAG, "Handle event (E_END): ENTER.");
        final String[] components = content.split(";");
        final String tableId = components[0];
        final String gameResult = components[1];
        
        if (!myTable_.hasId(tableId)) { // not the table I am interested in?
            Log.w(TAG, "Ignore the E_END event.");
            return;
        }
        
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity == null) {
            Log.w(TAG, "The main activity is NULL. Ignore this E_END event.");
            return;
        }
        
        isGameOver_ = true;
        timeTracker_.stop();
        playerTracker_.syncUI();
        
        GameStatus gameStatus = GameStatus.GAME_STATUS_UNKNOWN;
        
        if ("black_win".equals(gameResult)) {
            gameStatus = GameStatus.GAME_STATUS_BLACK_WIN;
        } else if ("red_win".equals(gameResult)) {
            gameStatus = GameStatus.GAME_STATUS_RED_WIN;
        } if ("drawn".equals(gameResult)) {
            gameStatus = GameStatus.GAME_STATUS_DRAWN;
        }
        gameStatus_ = gameStatus;
        mainActivity.onGameEnded(gameStatus);
    }
    
    private void handleNetworkEvent_RESET(String content) {
        Log.d(TAG, "Handle event (RESET): ENTER.");
        final String[] components = content.split(";");
        final String tableId = components[0];
        
        if (!myTable_.hasId(tableId)) { // not the table I am interested in?
            Log.w(TAG, "Ignore the E_END event.");
            return;
        }
        
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity == null) {
            Log.w(TAG, "The main activity is NULL. Ignore this E_END event.");
            return;
        }
        
        isGameOver_ = false;
        gameStatus_ = GameStatus.GAME_STATUS_UNKNOWN;
        mainActivity.onGameReset();
        timeTracker_.stop();
        timeTracker_.reset();
    }
    
    private void handleNetworkEvent_DRAW(String content) {
        Log.d(TAG, "Handle event (DRAW): ENTER.");
        final String[] components = content.split(";");
        final String tableId = components[0];
        final String pid = components[1];
        
        if (!myTable_.hasId(tableId)) { // not the table I am interested in?
            Log.w(TAG, "Ignore the DRAW event.");
            return;
        }
        
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity == null) {
            Log.w(TAG, "The main activity is NULL. Ignore this E_END event.");
            return;
        }
        
        Toast.makeText(getApplicationContext(),
                pid + " offered to DRAW the game", Toast.LENGTH_LONG).show();
    }
    
    private void onGameEnded() {
        Log.d(TAG, "On game-ended...");
        timeTracker_.stop();
    }
    
    public void handlePlayOnlineClicked() {
        Log.d(TAG, "Action 'Play Online' clicked...");
        if ( !networkPlayer_.isOnline() || !isLoginOK_ ) {
            final AccountInfo accountInfo = loadPreferences_Account();
            pid_ = accountInfo.username;
            password_ = accountInfo.password;
            
            networkPlayer_.setLoginInfo(pid_,  password_);
            networkPlayer_.connectToServer();
        } else {
            networkPlayer_.sendRequest_LIST();
        }
    }

    public boolean isOnline() {
        return networkPlayer_.isOnline();
    }

    public String getMyPid() {
        return pid_;
    }

    public String getMyRating() {
        return myRating_;
    }
    
    public ColorEnum getMyColor() {
        return myColor_;
    }
    
    public boolean isGameOver() {
        return isGameOver_;
    }
    
    public GameStatus getGameStatus() {
        return gameStatus_;
    }
    
    public TableInfo getMyTable() {
        return myTable_;
    }
    
    public String getCurrentTableId() {
        return myTable_.tableId;
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
    
    public boolean isMyTurn() {
        switch (playerTracker_.getTableType()) {
            case TABLE_TYPE_LOCAL:
                return (myColor_ == referee_.getNextColor());
                
            case TABLE_TYPE_NETWORK:
                return ((myColor_ == ColorEnum.COLOR_RED || myColor_ == ColorEnum.COLOR_BLACK) &&
                        playerTracker_.hasEnoughPlayers() &&
                        myColor_ == referee_.getNextColor());
                
            case TABLE_TYPE_EMPTY: // falls through
            default:
                return false;
        }
    }
    
    public void logoutFromNetwork() {
        Log.d(TAG, "Logout from network...");
        if (networkPlayer_.isOnline() ) {
            networkPlayer_.disconnectFromServer();
        }
        myTable_ = new TableInfo();
        isLoginOK_ = false;
    }
    
    public NetworkPlayer getNetworkPlayer() {
        return networkPlayer_;
    }
    
    public void handleTableSelection(String tableId) {
        Log.i(TAG, "Select table: " + tableId + ".");
        
        if (myTable_.hasId(tableId)) {
            Log.w(TAG, "Same table: " + tableId + ". Ignore the request.");
            return;
        }
        
        if (myTable_.isValid()) {
            networkPlayer_.sendRequest_LEAVE(myTable_.tableId); // Leave the old table.
        }
        
        networkPlayer_.sendRequest_JOIN(tableId, "None");
    }

    public void handleRequestToCloseCurrentTable() {
        Log.i(TAG, "Close the current table...");
        if (!myTable_.isValid()) {
            Log.w(TAG, "No current table. Ignore the request to Close the current Table");
            return;
        }
        networkPlayer_.sendRequest_LEAVE(myTable_.tableId);
    }
    
    public void handleRequestToOpenNewTable() {
        Log.i(TAG, "Request to open a new table...");
        
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity == null) {
            Log.w(TAG, "The main activity is NULL. Ignore this 'open new table request'.");
            return;
        }
        
        // Case 1: I am not online at all.
        if (!this.isOnline() && !myTable_.isValid()) {
            playerTracker_.setTableType(TableType.TABLE_TYPE_LOCAL); // A new practice table.
            playerTracker_.syncUI();
            myColor_ = ColorEnum.COLOR_RED;
            aiEngine_.initGame();
            timeTracker_.stop();
            timeTracker_.reset();
            mainActivity.openNewPracticeTable();
        }
        // Case 2: I am online and am not playing in any table.
        else if (this.isOnline() && 
              ( myColor_ == ColorEnum.COLOR_UNKNOWN ||
                myColor_ == ColorEnum.COLOR_NONE ||
               isGameOver_ )) {
            
            if (myTable_.isValid()) {
                networkPlayer_.sendRequest_LEAVE(myTable_.tableId); // Leave the current table.
            }
            final String itimes = "900/180/20"; // The initial times.
            networkPlayer_.sendRequest_NEW(itimes);
        }
        else {
            Log.w(TAG, "Either offline or currently playing. Ignore this 'open new table request'.");
        }
    }
    
    public void handleRequestToOfferDraw() {
        Log.i(TAG, "Send request to 'Offer Draw'...");
        if (!myTable_.isValid()) {
            Log.w(TAG, "No current table. Ignore the request to 'Offer Draw' the current Table");
            return;
        }
        networkPlayer_.sendRequest_DRAW(myTable_.tableId);
    }
    
    public void handleRequestToOfferResign() {
        Log.i(TAG, "Send request to 'Offer Resign'...");
        if (!myTable_.isValid()) {
            Log.w(TAG, "No current table. Ignore the request to 'Offer Resign' the current Table");
            return;
        }
        networkPlayer_.sendRequest_RESIGN(myTable_.tableId);
    }

    public void handleRequestToResetTable() {
        Log.i(TAG, "Send request to 'Reset Table'...");
        if (!myTable_.isValid()) {
            Log.w(TAG, "No current table. Ignore the request to 'Reset Table' the current Table");
            return;
        }
        networkPlayer_.sendRequest_RESET(myTable_.tableId);
    }
    
    public void handlePlayerButtonClick(Enums.ColorEnum clickedColor) {
        Log.i(TAG, "Handle player-button click. clickedColor = " + clickedColor);
        
        if (!myTable_.isValid()) return;
        
        Enums.ColorEnum requestedColor = ColorEnum.COLOR_UNKNOWN;
     
        switch (myColor_) {
            case COLOR_RED:
                if (clickedColor == myColor_) {
                    requestedColor = ColorEnum.COLOR_NONE;
                } else if (myTable_.blackId.length() == 0) {
                    requestedColor = ColorEnum.COLOR_BLACK;
                }
                break;
              
            case COLOR_BLACK:
                if (clickedColor == myColor_) {
                    requestedColor = ColorEnum.COLOR_NONE;
                } else if (myTable_.redId.length() == 0) {
                    requestedColor = ColorEnum.COLOR_RED;
                }
                break;
                
            case COLOR_NONE:
                if (clickedColor == ColorEnum.COLOR_BLACK && myTable_.blackId.length() == 0) {
                    requestedColor = ColorEnum.COLOR_BLACK;
                } else if (clickedColor == ColorEnum.COLOR_RED && myTable_.redId.length() == 0) {
                    requestedColor = ColorEnum.COLOR_RED;
                }
                break;
            
            case COLOR_UNKNOWN: /* falls through */
                // Note: We should already set to "NONE" when we join the table.
            default:
                Log.e(TAG, "Handle player-button click: Unsupported myColor = " + myColor_);
                break;
        }
        
        String joinColor = null;
        if (requestedColor == ColorEnum.COLOR_NONE) joinColor = "None";
        else if (requestedColor == ColorEnum.COLOR_RED) joinColor = "Red";
        else if (requestedColor == ColorEnum.COLOR_BLACK) joinColor = "Black";
        else {
            return;
        }
        
        if (myColor_ == requestedColor) {
            return; // No need to do anything.
        }
        
        /* NOTE:
         *  It appears that the Flashed-based client cannot handle the case in which the player
         *  directly changes seat from Red => Black (or Black => Red).
         *  So, we will break it down into 2 separate requests. For example,
         *      (1) Red => None
         *      (2) None => Black
         */
        if ( (myColor_ == ColorEnum.COLOR_RED || myColor_ == ColorEnum.COLOR_BLACK)
          && (requestedColor == ColorEnum.COLOR_RED || requestedColor == ColorEnum.COLOR_BLACK) ) {
            
            networkPlayer_.sendRequest_JOIN(myTable_.tableId, "None");
        }
        
        networkPlayer_.sendRequest_JOIN(myTable_.tableId, joinColor);
    }
    
    @SuppressLint("DefaultLocale")
    public void handleLocalMove(Position fromPos, Position toPos) {
        Log.i(TAG, "Handle local move: referee 's moveCount = " + referee_.getMoveCount());
        
        timeTracker_.nextColor();
        
        if (referee_.getMoveCount() == 2) {
            timeTracker_.start();
        }
        
        if (referee_.getMoveCount() > 1) { // The game has started?
            playerTracker_.syncUI();
        }
        
        if (!myTable_.isValid()) { // a local table (with AI)
            aiEngine_.onHumanMove(fromPos.row, fromPos.column, toPos.row, toPos.column);
            
            if (!referee_.isGameInProgress()) {
                Log.i(TAG, "The game has ended. Do nothing.");
                onGameEnded();
                return;
            }
            
            Log.d(TAG, "Ask AI (MaxQi) to generate a new move...");
            new Thread(new Runnable() {
                public void run() {
                    final String aiMove = aiEngine_.generateMove();
                    Log.d(TAG, "... AI returned this move [" + aiMove + "].");
                    messageHandler_.sendMessage(
                            messageHandler_.obtainMessage(MSG_AI_MOVE_READY, aiMove) );
                }
            }).start();
            
        }
        else { // a network table
            final String move = String.format("%d%d%d%d",
                    fromPos.column, fromPos.row, toPos.column, toPos.row);
            Log.i(TAG, " .... move: [" + move + "]");
            networkPlayer_.sendRequest_MOVE(myTable_.tableId, move);
        }
    }
    
    public void handleAIMove(Position fromPos, Position toPos) {
        Log.i(TAG, "Handle AI move: referee 's moveCount = " + referee_.getMoveCount());
        timeTracker_.nextColor();
        
        if (referee_.getMoveCount() == 2) {
            timeTracker_.start();
        }
        
        if (!referee_.isGameInProgress()) {
            Log.i(TAG, "The game has ended. Do nothing.");
            onGameEnded();
            return;
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
                messageHandler_.obtainMessage(MSG_NETWORK_CODE, Integer.valueOf(networkCode)) );
    }
    
}
