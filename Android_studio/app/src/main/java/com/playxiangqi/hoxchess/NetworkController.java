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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.playxiangqi.hoxchess.Enums.ColorEnum;
import com.playxiangqi.hoxchess.Enums.GameStatus;
import com.playxiangqi.hoxchess.Enums.TableType;

import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class NetworkController {

    private static final String TAG = "NetworkController";
    
    private final Referee referee_;
    
    private final NetworkPlayer networkPlayer_ = new NetworkPlayer();
    private boolean isLoginOK_ = false;
    // (Not yet supported) private String myRating_ = "0";
    
    private WeakReference<MainActivity> mainActivity_ = new WeakReference<MainActivity>(null);
    private WeakReference<ChatBubbleActivity> chatActivity_ = new WeakReference<ChatBubbleActivity>(null);
    private TableInfo myTable_ = new TableInfo();
    private ColorEnum myColor_ = ColorEnum.COLOR_UNKNOWN;
    
    private GameStatus gameStatus_ = GameStatus.GAME_STATUS_UNKNOWN;
    
    private TableTimeTracker timeTracker_ = new TableTimeTracker();
    private TablePlayerTracker playerTracker_ = new TablePlayerTracker(TableType.TABLE_TYPE_LOCAL);
    
    private List<ChatMessage> newMessages_ = new ArrayList<ChatMessage>();
    
    public NetworkController(TableTimeTracker timeTracker,
                             TablePlayerTracker playerTracker,
                             Referee referee) {
        Log.d(TAG, "[CONSTRUCTOR]: ...");
        timeTracker_ = timeTracker;
        playerTracker_ = playerTracker;
        referee_ = referee;

        if (!networkPlayer_.isAlive()) {
            networkPlayer_.start();
        }
    }

    public void setMainActivity(MainActivity activity) {
        mainActivity_ = new WeakReference<MainActivity>(activity);
    }

    public void setChatActivity(ChatBubbleActivity activity) {
        chatActivity_ = new WeakReference<ChatBubbleActivity>(activity);
    }

    public boolean isLoginOK() { return isLoginOK_; }
    public ColorEnum getMyColor() { return myColor_; }
    public GameStatus getGameStatus() { return gameStatus_; }
    public boolean isMyTableValid() { return myTable_.isValid(); }
    public String getMyTableId() { return myTable_.tableId; }
    public List<ChatMessage> getNewMessages() { return newMessages_; }

    /**
     * The main handler to handle ALL incoming network events.
     *
     * @param eventString The event
     */
    public void handleNetworkEvent(String eventString) {
        Log.d(TAG, "Handle a network event: ENTER.");

        HashMap<String, String> newEvent = new HashMap<String, String>();

        for (String token : eventString.split("&")) {
            //Log.d(TAG, "... token = [" + token + "]");
            final String[] pair = token.split("=");
            newEvent.put(pair[0], pair[1]);
        }

        final String op = newEvent.get("op");
        final int code = Integer.parseInt( newEvent.get("code") );
        final String content = newEvent.get("content");
        final String tableId = newEvent.get("tid");

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
        } else if ("MSG".equals(op)) {
            handleNetworkEvent_MSG(content, tableId);
        }
    }

    private void handleNetworkEvent_LOGIN(int code, String content) {
        Log.d(TAG, "Handle event (LOGIN): ENTER.");

        isLoginOK_ = (code == 0);

        if (!isLoginOK_) {  // Error
            Log.i(TAG, "Login failed. Code: [" + code + "], Error: [" + content + "]");
            Toast.makeText(HoxApp.getApp().getApplicationContext(),
                    getLocalizedLoginError(code), Toast.LENGTH_LONG).show();

            networkPlayer_.disconnectFromServer();
            return;
        }

        final String[] components = content.split(";");
        final String pid = components[0];
        final String rating = components[1];
        Log.d(TAG, ">>> [" + pid + " " + rating + "] LOGIN.");

        final String myPid = HoxApp.getApp().getMyPid();
        if (myPid.equals(pid)) { // my LOGIN?
            Log.i(TAG, "Received my LOGIN info [" + pid + " " + rating + "].");
            //myRating_ = rating;

            myColor_ = ColorEnum.COLOR_UNKNOWN;
            playerTracker_.setTableType(TableType.TABLE_TYPE_EMPTY);
            playerTracker_.syncUI();

            networkPlayer_.sendRequest_LIST();

            MainActivity mainActivity = mainActivity_.get();
            if (mainActivity != null) {
                mainActivity.clearTable();
            }
        }
    }

    private void handleNetworkEvent_LIST(String content) {
        Log.d(TAG, "Handle event (LIST): ENTER.");
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.startActivityToListTables(content);
        }
    }

    private void handleNetworkEvent_I_TABLE(String content) {
        Log.d(TAG, "Handle event (I_TABLE): ENTER.");

        gameStatus_ = GameStatus.GAME_STATUS_UNKNOWN;
        myTable_ = new TableInfo(content);

        final String myPid = HoxApp.getApp().getMyPid();
        if (myPid.equals(myTable_.blackId)) {
            myColor_ = ColorEnum.COLOR_BLACK;
        } else if (myPid.equals(myTable_.redId)) {
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

        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.updateBoardWithNewTableInfo(myTable_);
        }
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

        timeTracker_.setInitialColor(referee_.getNextColor());
        timeTracker_.start();
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
            if (referee_.getMoveCount() == 2) { // The game has started?
                mainActivity.onGameStatusChanged();
            }
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
        final String myPid = HoxApp.getApp().getMyPid();
        if (pid.equals(myPid)) {
            Log.i(TAG, "I just left my table: " + tableId);
            myTable_ = new TableInfo();
            myColor_ = ColorEnum.COLOR_UNKNOWN;
            gameStatus_ = GameStatus.GAME_STATUS_UNKNOWN;
            timeTracker_.stop();
            newMessages_.clear();
            MainActivity mainActivity = mainActivity_.get();
            if (mainActivity != null) {
                mainActivity.clearTable();
            }
            // Other player left my table?
        } else {
            myTable_.onPlayerLeft(pid);
        }

        playerTracker_.onPlayerLeave(pid);
        playerTracker_.syncUI();
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

        final Enums.ColorEnum playerColor = Utils.stringToPlayerColor(color);
        myTable_.onPlayerJoined(pid, rating, playerColor);

        final String myPid = HoxApp.getApp().getMyPid();
        switch (playerColor) {
            case COLOR_RED:
                if (pid.equals(myPid)) myColor_ = ColorEnum.COLOR_RED;
                break;
            case COLOR_BLACK:
                if (pid.equals(myPid)) myColor_ = ColorEnum.COLOR_BLACK;
                break;
            case COLOR_NONE:
                if (pid.equals(myPid)) myColor_ = ColorEnum.COLOR_NONE;
                break;
            default:
                break;
        }
        mainActivity.onLocalPlayerJoined(myColor_);

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

        GameStatus gameStatus = GameStatus.GAME_STATUS_UNKNOWN;

        if ("black_win".equals(gameResult)) {
            gameStatus = GameStatus.GAME_STATUS_BLACK_WIN;
        } else if ("red_win".equals(gameResult)) {
            gameStatus = GameStatus.GAME_STATUS_RED_WIN;
        } else if ("drawn".equals(gameResult)) {
            gameStatus = GameStatus.GAME_STATUS_DRAWN;
        }
        gameStatus_ = gameStatus;
        mainActivity.onGameEnded(gameStatus);
        timeTracker_.stop();
        playerTracker_.syncUI();
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

        Toast.makeText(HoxApp.getApp().getApplicationContext(),
                pid + " offered to DRAW the game", Toast.LENGTH_LONG).show();
    }

    private void handleNetworkEvent_MSG(String content, String tableId) {
        Log.d(TAG, "Handle event (MSG): ENTER.");
        final String[] components = content.split(";");
        final String sender = components[0];
        if (components.length < 2) {
            Log.i(TAG, "... Received an empty message from [" + sender + "]. Ignore it.");
            return;
        }
        final String message = components[1];

        if (TextUtils.isEmpty(tableId) ||
                !myTable_.hasId(tableId)) { // not the table I am interested in?
            Log.w(TAG, "Ignore a MSG from sender: " + sender);
            return;
        }

        final String newMessage = sender + ": " + message;
        ChatMessage chatMsg = new ChatMessage(true, newMessage);
        newMessages_.add(chatMsg);

        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity == null) {
            Log.w(TAG, "The main activity is NULL. Ignore this E_END event.");
            return;
        }

        ChatBubbleActivity chatActivity = chatActivity_.get();
        if (chatActivity != null) {
            chatActivity.onMessageReceived(chatMsg);
        } else {
            mainActivity.onMessageReceived(sender, message);
        }
    }

    public void logoutFromNetwork() {
        Log.d(TAG, "Logout from network...");

        closeCurrentNetworkTable();

        if (networkPlayer_.isOnline() ) {
            networkPlayer_.disconnectFromServer();
        }
        isLoginOK_ = false;
    }

    public void handleNetworkError() {
        Log.d(TAG, "Handle network error...");

        // Attempt to login again if we are observing a network table.
        if (myTable_.isValid()) {
            closeCurrentNetworkTable();
            networkPlayer_.connectToServer();
        } else {
            Toast.makeText(HoxApp.getApp().getApplicationContext(),
                    "An IOException exception while handling network messages!",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void handleRequestToSendMove(Position fromPos, Position toPos) {
        Log.i(TAG, "Send request to 'Send Move': " + fromPos + " => " + toPos);
        if (!isMyTableValid()) {
            Log.w(TAG, "No current table. Ignore the request to 'Offer Resign' the current Table");
            return;
        }
        final String move = "" + fromPos.column + fromPos.row + toPos.column + toPos.row;
        networkPlayer_.sendRequest_MOVE(myTable_.tableId, move);
    }

    public void handleRequestToCloseCurrentTable() {
        Log.i(TAG, "Close the current table...");
        if (!isMyTableValid()) {
            Log.w(TAG, "No current table. Ignore the request to Close the current Table");
            return;
        }
        networkPlayer_.sendRequest_LEAVE(myTable_.tableId);
    }

    public void handleRequestToOfferDraw() {
        Log.i(TAG, "Send request to 'Offer Draw'...");
        if (!isMyTableValid()) {
            Log.w(TAG, "No current table. Ignore the request to 'Offer Draw' the current Table");
            return;
        }
        networkPlayer_.sendRequest_DRAW(myTable_.tableId);
    }

    public void handleRequestToOfferResign() {
        Log.i(TAG, "Send request to 'Offer Resign'...");
        if (!isMyTableValid()) {
            Log.w(TAG, "No current table. Ignore the request to 'Offer Resign' the current Table");
            return;
        }
        networkPlayer_.sendRequest_RESIGN(myTable_.tableId);
    }

    public void handleRequestToResetTable() {
        Log.i(TAG, "Send request to 'Reset Table'...");
        if (!isMyTableValid()) {
            Log.w(TAG, "No current table. Ignore the request to 'Reset Table' the current Table");
            return;
        }
        networkPlayer_.sendRequest_RESET(myTable_.tableId);
    }

    public void handleTableSelection(String tableId) {
        Log.i(TAG, "Select table: " + tableId + ".");

        if (myTable_.hasId(tableId)) {
            Log.w(TAG, "Same table: " + tableId + ". Ignore the request.");
            return;
        }

        if (isMyTableValid()) {
            networkPlayer_.sendRequest_LEAVE(myTable_.tableId); // Leave the old table.
        }

        networkPlayer_.sendRequest_JOIN(tableId, "None");
    }

    public void handleLocalMessage(ChatMessage chatMsg) {
        Log.d(TAG, "Handle local message: [" + chatMsg.message + "]");
        if (!isMyTableValid()) { // Not a network table?
            Log.i(TAG, "No network table. Ignore the local message.");
            return;
        }
        newMessages_.add(chatMsg);
        networkPlayer_.sendRequest_MSG(myTable_.tableId, chatMsg.message);
    }

    /**
     * Handle my request to change playing role.
     *
     * @param clickedColor The playing role to which I want to change.
     */
    public void handleMyRequestToChangeRole(Enums.ColorEnum clickedColor) {
        Log.d(TAG, "Handle my request to change role to = " + clickedColor);

        //if (!myTable_.isValid()) return; // Do nothing if this is NOT a network table.

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

        String joinColor;
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

    public void handleMyRequestToOpenNewTable() {
        if (isOnline() &&
           (myColor_ == ColorEnum.COLOR_UNKNOWN || myColor_ == ColorEnum.COLOR_NONE ||
             isGameOver() )) {

            if (myTable_.isValid()) {
                networkPlayer_.sendRequest_LEAVE(myTable_.tableId); // Leave the current table.
            }
            final String itimes = "900/180/20"; // The initial times.
            networkPlayer_.sendRequest_NEW(itimes);
        }
        else {
            Log.w(TAG, "Either offline or not playing. Ignore this 'open new table request'.");
        }
    }

    public void handleMyRequestToGetListOfTables() {
        networkPlayer_.sendRequest_LIST();
    }

    public void setLoginInfo(String pid, String password) {
        networkPlayer_.setLoginInfo(pid,  password);
    }

    public void connectToServer() {
        networkPlayer_.connectToServer();
    }

    public void closeCurrentNetworkTable() {
        if (myTable_.isValid()) {
            Log.d(TAG, "Close the current network table: " + myTable_.tableId);

            myTable_ = new TableInfo();
            myColor_ = ColorEnum.COLOR_UNKNOWN;
            gameStatus_ = GameStatus.GAME_STATUS_UNKNOWN;
            timeTracker_.stop();
            newMessages_.clear();
            MainActivity mainActivity = mainActivity_.get();
            if (mainActivity != null) {
                mainActivity.clearTable();
            }
        }

        playerTracker_.setTableType(TableType.TABLE_TYPE_EMPTY);
        playerTracker_.syncUI();
    }

    public boolean isGameOver() {
        return (gameStatus_ == GameStatus.GAME_STATUS_BLACK_WIN ||
                gameStatus_ == GameStatus.GAME_STATUS_RED_WIN ||
                gameStatus_ == GameStatus.GAME_STATUS_DRAWN );
    }

    public boolean isOnline() {
        return networkPlayer_.isOnline();
    }

    private String getLocalizedLoginError(int code) {
        final HoxApp context = HoxApp.getApp();
        switch (code)
        {
            case 6: return context.getString(R.string.login_error_wrong_password);
            case 7: return context.getString(R.string.login_error_wrong_username);
        }
        return context.getString(R.string.login_error_general_error);
    }

}
