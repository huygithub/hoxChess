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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.playxiangqi.hoxchess.Enums.ColorEnum;
import com.playxiangqi.hoxchess.Enums.GameStatus;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

public class NetworkController implements NetworkPlayer.NetworkEventListener {

    private static final String TAG = "NetworkController";

    // The singleton instance.
    private static NetworkController instance_;

    private final NetworkPlayer networkPlayer_ = new NetworkPlayer();
    private boolean isLoginOK_ = false;
    private String myRating_ = "1500";

    private TableInfo myTable_ = new TableInfo();
    private ColorEnum myColor_ = ColorEnum.COLOR_UNKNOWN;
    
    private GameStatus gameStatus_ = GameStatus.GAME_STATUS_UNKNOWN;

    // *************************************************************************************
    public interface NetworkEventListener {
        void onLoginSuccess();
        void onLogout();
        void onLoginFailure(int errorMessageResId);
    }
    private Set<NetworkEventListener> listeners_ = new HashSet<NetworkEventListener>();
    public void addListener(NetworkEventListener listener) { listeners_.add(listener); }
    public void removeListener(NetworkEventListener listener) { listeners_.remove(listener); }

    // *************************************************************************************
    public static class Request {
        final public String op; // The operation (e.g., "list", "new_table").
        public Map<String, String> params; // Pairs of <key, value> if needed.
        public Request(String operation) { this.op = operation; }
    }
    private LinkedList<Request> pendingRequestsQueue_ = new LinkedList<Request>();

    private void addPendingRequest(Request request) {
        pendingRequestsQueue_.addLast(request);
    }

    private Request getAndRemoveFirstPendingRequest() {
        if (pendingRequestsQueue_.size() == 0) { return null; }
        return pendingRequestsQueue_.removeFirst();
    }

    private void processRequest(Request request) {
        final String op = request.op;
        if ("list".equals(op)) {
            networkPlayer_.sendRequest_LIST();
        } else if ("new".equals(op)) {
            networkPlayer_.sendRequest_NEW(Enums.DEFAULT_INITIAL_GAME_TIMES);
        } else if ("connect".equals(op)) {
            networkPlayer_.connectToServer();
        } else {
            throw new RuntimeException("Unsupported request:" + op);
        }
    }

    private void processFirstPendingRequest() {
        Request firstRequest = getAndRemoveFirstPendingRequest();
        if (firstRequest != null) {
            Log.d(TAG, "Process a pending request:" + firstRequest.op);
            processRequest(firstRequest);
        }
    }

    // *************************************************************************************

    /**
     * Singleton API to return the instance.
     */
    public static NetworkController getInstance() {
        if (instance_ == null) {
            instance_ = new NetworkController();
        }
        return instance_;
    }

    /**
     * Constructor
     */
    private NetworkController() {
        Log.d(TAG, "[CONSTRUCTOR]: ...");
        networkPlayer_.setNetworkEventListener(this);
        if (!networkPlayer_.isAlive()) {
            networkPlayer_.start();
        }
    }

    @Override
    public void onNetworkEvent(String eventString) {
        Log.d(TAG, "On network event = [" + eventString + "]");
        messageHandler_.sendMessage(
                messageHandler_.obtainMessage(MSG_NETWORK_EVENT, eventString) );
    }

    @Override
    public void onNetworkCode(int networkCode) {
        Log.d(TAG, "On network code = [" + networkCode + "]");
        messageHandler_.sendMessage(
                messageHandler_.obtainMessage(MSG_NETWORK_CODE, networkCode) );
    }

    public void onLocalMessage(ChatMessage chatMsg) {
        handleLocalTableMessage(chatMsg.message);
    }

    /**
     * A message handler to handle UI related tasks.
     */
    private static final int MSG_NETWORK_EVENT = 1;
    private static final int MSG_NETWORK_CODE = 2;
    private final MessageHandler messageHandler_ = new MessageHandler(this);

    static class MessageHandler extends Handler {
        private final NetworkController controller_;

        MessageHandler(NetworkController controller) {
            controller_ = controller;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_NETWORK_EVENT:
                    controller_.handleNetworkEvent((String) msg.obj);
                    break;

                case MSG_NETWORK_CODE:
                    controller_.handleNetworkCode((Integer) msg.obj);
                    break;

                default:
                    break;
            }
        }
    }

    public boolean isLoginOK() { return isLoginOK_; }
    public ColorEnum getMyColor() { return myColor_; }
    public GameStatus getGameStatus() { return gameStatus_; }
    public boolean isMyTableValid() { return myTable_.isValid(); }
    public String getMyTableId() { return myTable_.tableId; }
    public TableInfo getMyTableInfo() { return myTable_; }
    public String getMyRating_() { return myRating_; }

    /**
     * The main handler to handle ALL incoming network events.
     *
     * @param eventString The event
     */
    public void handleNetworkEvent(String eventString) {
        Log.v(TAG, "Handle a network event: ENTER.");

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
            handleNetworkEvent_Error(code, op, content);
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
        } else if ("INVITE".equals(op)) {
            handleNetworkEvent_INVITE(content, tableId);
        } else if ("I_PLAYERS".equals(op)) {
            handleNetworkEvent_I_PLAYERS(content);
        } else if ("LOGOUT".equals(op)) {
            handleNetworkEvent_LOGOUT(content);
        } else if ("PLAYER_INFO".equals(op)) {
            handleNetworkEvent_PLAYER_INFO(content);
        } else if ("E_SCORE".equals(op)) {
            handleNetworkEvent_E_SCORE(content);
        }
    }

    public void handleNetworkCode(int networkCode) {
        Log.d(TAG, "On Network code: " + networkCode);

        int resId = -1;  // Default = Invalid
        switch (networkCode) {
            case NetworkPlayer.NETWORK_CODE_IO_EXCEPTION:
                handleNetworkError(); // NOTE: Handle specially
                break;

            case NetworkPlayer.NETWORK_CODE_CONNECTED:
                resId = R.string.msg_connection_established;
                break;
            case NetworkPlayer.NETWORK_CODE_UNRESOLVED_ADDRESS:
                resId = R.string.msg_connection_failed_unresolved_address_exception;
                break;
            case NetworkPlayer.NETWORK_CODE_DISCONNECTED: {
                resId = R.string.msg_connection_disconnected;
                // NOTE: The special LOGOUT event!
                Log.i(TAG, "On Network code: I just logged out the server.");
                for (NetworkEventListener listener : listeners_) {
                    listener.onLogout();
                }
                processFirstPendingRequest();
                break;
            }
            case NetworkPlayer.NETWORK_CODE_CLOSED:
                Log.i(TAG, "The network connection has been closed.");
                break;
            default:
                break;
        }

        if (resId != -1) {
            NetworkTableController.getInstance().onNetworkCode(resId);
        }
    }

    private void handleNetworkEvent_LOGIN(int code, String content) {
        Log.d(TAG, "Handle event (LOGIN): ENTER.");

        isLoginOK_ = (code == 0);

        if (!isLoginOK_) {  // Error
            Log.w(TAG, "Login failed. Code: [" + code + "], Error: [" + content + "]");
            for (NetworkEventListener listener : listeners_) {
                listener.onLoginFailure(getLocalizedLoginError(code));
            }
            networkPlayer_.disconnectFromServer();
            return;
        }

        final String[] components = content.split(";");
        final String pid = components[0];
        final String rating = components[1];
        Log.d(TAG, ">>> [" + pid + " " + rating + "] LOGIN.");

        PlayerManager.getInstance().addPlayer(new PlayerInfo(pid, rating));

        final String myPid = HoxApp.getApp().getMyPid();
        if (myPid.equals(pid)) { // my LOGIN?
            Log.i(TAG, "Received my LOGIN info [" + pid + " " + rating + "].");
            myRating_ = rating;
            myColor_ = ColorEnum.COLOR_UNKNOWN;
            for (NetworkEventListener listener : listeners_) {
                listener.onLoginSuccess();
            }
            processFirstPendingRequest();

        } else { // Other player 's LOGIN?
            Log.d(TAG, "Received other player LOGIN info [" + pid + " " + rating + "].");
        }
    }

    private void handleNetworkEvent_Error(int code, String op, String content) {
        Log.i(TAG, "Received ERROR event: [" + code + ": " + content + "], op=[" + op + "].");
        if ("JOIN".equals(op)) {
            // NOTE: The error code is usual 7, which means "NOT FOUND".
            // Currently, we will assume that this is the only error code returned by the server.
            NetworkTableController.getInstance().onJoinTableError(content, Enums.ErrorCode.ERROR_CODE_NOT_FOUND);
        }
    }

    private void handleNetworkEvent_LIST(String content) {
        Log.d(TAG, "Handle event (LIST): ENTER.");

        List<TableInfo> tables = new ArrayList<TableInfo>();
        final String[] entries = content.split("\n");
        for (String entry : entries) {
            TableInfo tableInfo = new TableInfo(entry);
            tables.add(tableInfo);
        }

        PlayerManager.getInstance().setTables(tables);
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

        NetworkTableController.getInstance().onNetworkTableEnter(myTable_);
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

        final MoveInfo[] moves = MoveInfo.parseForListOfNetworkMoves(movesStr);
        NetworkTableController.getInstance().onResetBoardWithMoves(moves);
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

        final MoveInfo moveInfo = MoveInfo.parseForNetworkMove(move);
        NetworkTableController.getInstance().onNetworkMove(moveInfo);
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
        if (pid.equals(HoxApp.getApp().getMyPid())) {
            Log.d(TAG, "I just left my table: " + tableId);
            myTable_ = new TableInfo();
            myColor_ = ColorEnum.COLOR_UNKNOWN;
            gameStatus_ = GameStatus.GAME_STATUS_UNKNOWN;

        // Other player left my table?
        } else {
            myTable_.onPlayerLeft(pid);
        }

        NetworkTableController.getInstance().onNetworkPlayerLeave(pid);
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

        final Enums.ColorEnum playerColor = Utils.stringToPlayerColor(color);
        myTable_.onPlayerJoined(pid, rating, playerColor);

        // Determine if my role has changed.
        ColorEnum myNewColor = ColorEnum.COLOR_UNKNOWN;
        if (pid.equals(HoxApp.getApp().getMyPid())) { // my new role?
            switch (playerColor) {
                case COLOR_RED: myNewColor = ColorEnum.COLOR_RED; break;
                case COLOR_BLACK: myNewColor = ColorEnum.COLOR_BLACK; break;
                case COLOR_NONE: myNewColor = ColorEnum.COLOR_NONE; break;
                default: break;
            }
        }

        if (myNewColor != ColorEnum.COLOR_UNKNOWN) { // my role has changed?
            myColor_ = myNewColor;
        }

        NetworkTableController.getInstance().onNetworkPlayerJoin(
                new PlayerInfo(pid, rating), playerColor, myNewColor);
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

        GameStatus gameStatus = GameStatus.GAME_STATUS_UNKNOWN;

        if ("black_win".equals(gameResult)) {
            gameStatus = GameStatus.GAME_STATUS_BLACK_WIN;
        } else if ("red_win".equals(gameResult)) {
            gameStatus = GameStatus.GAME_STATUS_RED_WIN;
        } else if ("drawn".equals(gameResult)) {
            gameStatus = GameStatus.GAME_STATUS_DRAWN;
        }
        gameStatus_ = gameStatus;

        NetworkTableController.getInstance().onGameEnded(gameStatus);
    }

    private void handleNetworkEvent_RESET(String content) {
        Log.d(TAG, "Handle event (RESET): ENTER.");
        final String[] components = content.split(";");
        final String tableId = components[0];

        if (!myTable_.hasId(tableId)) { // not the table I am interested in?
            Log.w(TAG, "Ignore the E_END event.");
            return;
        }

        gameStatus_ = GameStatus.GAME_STATUS_UNKNOWN;
        NetworkTableController.getInstance().onGameReset();
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

        NetworkTableController.getInstance().onGameDrawnRequested(pid);
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

        // NOTE: There are 2 types of messages:
        //   (1) For table messages, both "tid" and "pid" are present.
        //   (2) For private messages, "tid" is missing.
        //

        if (!TextUtils.isEmpty(tableId) && !myTable_.hasId(tableId)) {
            Log.w(TAG, "I am no longer in table: " + tableId + ". Ignore MSG from : " + sender);
            return; // Do nothing.
        }

        MessageInfo.MessageType msgType = (TextUtils.isEmpty(tableId)
                ? MessageInfo.MessageType.MESSAGE_TYPE_CHAT_PRIVATE
                :  MessageInfo.MessageType.MESSAGE_TYPE_CHAT_IN_TABLE);
        MessageInfo messageInfo = new MessageInfo(msgType, sender);
        messageInfo.content = message;
        messageInfo.tableId = tableId;
        MessageManager.getInstance().addMessage(messageInfo);
    }

    private void handleNetworkEvent_INVITE(String content, String tableId) {
        Log.d(TAG, "Handle event (INVITE): ENTER.");
        final String[] components = content.split(";");
        final String sender = components[0];
        final String senderRating = components[1];
        final String invitedPlayer = components[2]; // // The invited player

        final String myPid = HoxApp.getApp().getMyPid();
        if (!myPid.equals(invitedPlayer)) { // Am I invited?
            Log.w(TAG, "I am not the invited player:" + invitedPlayer
                    + ". Ignore this INVITE request from sender: " + sender);
            return;
        }

        final String tableIdString = (TextUtils.isEmpty(tableId) ? "?" : tableId);

        MessageInfo messageInfo = new MessageInfo(
                MessageInfo.MessageType.MESSAGE_TYPE_INVITE_TO_PLAY,
                sender);
        messageInfo.tableId = tableIdString;
        messageInfo.senderRating = senderRating;
        MessageManager.getInstance().addMessage(messageInfo);
    }

    private void handleNetworkEvent_I_PLAYERS(String content) {
        Log.d(TAG, "Handle event (I_PLAYERS): ENTER.");

        final String[] entries = content.split("\n");
        List<PlayerInfo> players = new ArrayList<PlayerInfo>();
        for (String entry : entries) {
            final String[] components = entry.split(";");
            final String pid = components[0];
            final String rating = components[1];

            PlayerInfo playerInfo = new PlayerInfo(pid, rating);
            players.add(playerInfo);
        }

        PlayerManager.getInstance().setInitialPlayers(players);
    }

    private void handleNetworkEvent_LOGOUT(String content) {
        Log.d(TAG, "Handle event (LOGOUT): ENTER.");
        final String pid = content;
        PlayerManager.getInstance().removePlayer(pid);
    }

    private void handleNetworkEvent_PLAYER_INFO(String content) {
        Log.d(TAG, "Handle event (PLAYER_INFO): ENTER.");
        final String[] components = content.split(";");
        final String pid = components[0];
        final String rating = components[1];
        final String wins = components[2];
        final String draws = components[3];
        final String losses = components[4];

        NetworkTableController.getInstance().onPlayerInfoReceived(pid, rating, wins, draws, losses);
    }

    private void handleNetworkEvent_E_SCORE(String content) {
        Log.d(TAG, "Handle event (E_SCORE): ENTER.");
        final String[] components = content.split(";");
        final String tableId = components[0];
        final String pid = components[1];
        final String rating = components[2];

        if (HoxApp.getApp().getMyPid().equals(pid)) { // my new rating?
            Log.i(TAG, "Received my new rating: " + myRating_ + " => " + rating);
            myRating_ = rating;
        }

        if (myTable_.hasId(tableId)) {
            NetworkTableController.getInstance().onPlayerRatingUpdate(pid, rating);
        }
    }

    public void logoutFromNetwork() {
        Log.d(TAG, "Logout from network...");

        if (myTable_.isValid()) {
            Log.d(TAG, "Clear existing data of the network table: " + myTable_.tableId);
            myTable_ = new TableInfo();
            myColor_ = ColorEnum.COLOR_UNKNOWN;
            gameStatus_ = GameStatus.GAME_STATUS_UNKNOWN;
        }

        if (networkPlayer_.isOnline() ) {
            networkPlayer_.disconnectFromServer();
        }
        isLoginOK_ = false;
    }

    private void handleNetworkError() {
        Log.i(TAG, "Handle network error...");

        // Attempt to login again if we are observing a network table.
        if (myTable_.isValid()) {
            myTable_ = new TableInfo();
            myColor_ = ColorEnum.COLOR_UNKNOWN;
            gameStatus_ = GameStatus.GAME_STATUS_UNKNOWN;
        }

        NetworkTableController.getInstance().onNetworkError();
    }

    public void handleRequestToSendMove(Position fromPos, Position toPos) {
        Log.i(TAG, "Send request to 'Send Move': " + fromPos + " => " + toPos);
        if (!isMyTableValid()) {
            Log.w(TAG, "No current table. Ignore the request to 'Offer Move' the current Table");
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

    private void handleLocalTableMessage(String msg) {
        Log.d(TAG, "Handle local table message: [" + msg + "]");
        if (!isMyTableValid()) { // Not a network table?
            Log.i(TAG, "No network table. Ignore the local message in the table.");
            return;
        }
        networkPlayer_.sendRequest_MSG(myTable_.tableId, null, msg);
    }

    public void handlePrivateMessage(String otherPID, String msg) {
        Log.d(TAG, "Handle private message: [" + otherPID + ": " + msg + "]");
        networkPlayer_.sendRequest_MSG(null, otherPID, msg);
    }

    /**
     * Handle my request to change playing role.
     *
     * @param clickedColor The playing role to which I want to change.
     */
    public void handleMyRequestToChangeRole(Enums.ColorEnum clickedColor) {
        Log.d(TAG, "Handle my request to change role. clickedColor = " + clickedColor);

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
            networkPlayer_.sendRequest_NEW(Enums.DEFAULT_INITIAL_GAME_TIMES);
        }
        else {
            Log.w(TAG, "Either offline or not playing. Ignore this 'open new table request'.");
        }
    }

    public void handleRequestToInvite(String invitee) {
        Log.i(TAG, "Send request to 'Invite'...");
        networkPlayer_.sendRequest_INVITE(invitee, myTable_.tableId);
    }

    public void handleRequestToGetPlayerInfo(String otherPID) {
        Log.i(TAG, "Send request for 'Player Info'...");
        networkPlayer_.sendRequest_PLAYER_INFO(otherPID);
    }

    public void sendRequestForTableList() {
        Log.d(TAG, "Send request to get 'LIST-of-tables'...");
        if (networkPlayer_.isOnline() && isLoginOK_) {
            networkPlayer_.sendRequest_LIST();
        } else {
            Request listRequest = new Request("list");
            addPendingRequest(listRequest);
            HoxApp.getApp().loginServer();
        }
    }

    public void sendRequestToOpenNewTable() {
        Log.d(TAG, "Send request to open 'NEW-table'...");
        if (networkPlayer_.isOnline() && isLoginOK_) {
            networkPlayer_.sendRequest_NEW(Enums.DEFAULT_INITIAL_GAME_TIMES);
        } else {
            Request listRequest = new Request("new");
            addPendingRequest(listRequest);
            HoxApp.getApp().loginServer();
        }
    }

    public void setLoginInfo(String pid, String password) {
        networkPlayer_.setLoginInfo(pid,  password);
    }

    public void connectToServer() {
        networkPlayer_.connectToServer();
    }

    public void reconnectToServer() {
        Log.d(TAG, "Re-connect to server...");
        if (networkPlayer_.isOnline() ) {
            networkPlayer_.disconnectFromServer();
            addPendingRequest(new Request("connect"));
        } else {
            networkPlayer_.connectToServer();
        }
    }

    public boolean isGameOver() {
        return (gameStatus_ == GameStatus.GAME_STATUS_BLACK_WIN ||
                gameStatus_ == GameStatus.GAME_STATUS_RED_WIN ||
                gameStatus_ == GameStatus.GAME_STATUS_DRAWN );
    }

    public boolean isOnline() {
        return networkPlayer_.isOnline();
    }

    public boolean amIPlaying() {
        return (myColor_ == ColorEnum.COLOR_RED || myColor_ == ColorEnum.COLOR_BLACK);
    }

    private int getLocalizedLoginError(int code) {
        switch (code)
        {
            case 6: return R.string.login_error_wrong_password;
            case 7: return R.string.login_error_wrong_username;
        }
        return R.string.login_error_general_error;
    }

}
