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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.playxiangqi.hoxchess.Enums.ColorEnum;
import com.playxiangqi.hoxchess.Enums.GameStatus;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class HoxApp extends Application {

    private static final String TAG = "HoxApp";
    
    private static final String SHARED_PREFERENCES_AI_LEVEL = "AI_LEVEL";
    private static final String KEY_SAVED_AI_LEVEL_INDEX = "SAVED_AI_LEVEL_INDEX";
    
    private static HoxApp thisApp_;
    
    private List<SettingsObserver> observers_ = new ArrayList<>();
    private final Object observerMutex_ = new Object();
    
    private int currentAILevel_ = -1; // Default = "invalid level"
    
    private String pid_ = "_THE_PLAYER_ID_"; // FIXME: ... _THE_PLAYER_ID_
    private String password_ = "_THE_PLAYER_PASSWORD_"; // FIXME: .... _THE_PLAYER_PASSWORD_
    private String myRating_ = "0";
    
    private NetworkPlayer networkPlayer_;
    private WeakReference<MainActivity> mainActivity_;
    private TableInfo myTable_ = new TableInfo();
    private ColorEnum myColor_ = ColorEnum.COLOR_UNKNOWN;
    
    public HoxApp() {
        // Empty
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()...");
        thisApp_ = this;
        
        networkPlayer_ = new NetworkPlayer();
        if (!networkPlayer_.isAlive()) {
            networkPlayer_.start();
        }
    }

    public static HoxApp getApp() {
        return thisApp_;
    }
    
    public static interface SettingsObserver {
        public void onAILevelChanged(int newLevel);
    }

    public void registerSettingsObserver(SettingsObserver newObserver) {
        if (newObserver == null) {
            throw new NullPointerException("Null Observer");
        }
        
        if (!observers_.contains(newObserver)) {
            observers_.add(newObserver);
        }
    }
    
    private void notifyObservers() {
        List<SettingsObserver> observersLocal = null;
        // Synchronization is used to make sure any observer registered
        // after message is received is not notified
        synchronized (observerMutex_) {
            observersLocal = new ArrayList<>(this.observers_);
        }
        for (SettingsObserver obj : observersLocal) {
            obj.onAILevelChanged(currentAILevel_);
        }
    }
    
    public int loadAILevelPreferences() {
        SharedPreferences sharedPreferences =
                thisApp_.getSharedPreferences(SHARED_PREFERENCES_AI_LEVEL, MODE_PRIVATE);
        int aiLevel = sharedPreferences.getInt(KEY_SAVED_AI_LEVEL_INDEX, 0);
        Log.d(TAG, "Load existing AI level: " + aiLevel);
        
        currentAILevel_ = aiLevel;
        return aiLevel;
    }
    
    public void savePreferences(int aiLevel) {
        Log.d(TAG, "Save the new AI level: " + aiLevel);
        SharedPreferences sharedPreferences =
                thisApp_.getSharedPreferences(SHARED_PREFERENCES_AI_LEVEL, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_SAVED_AI_LEVEL_INDEX, aiLevel);
        editor.commit();
        
        if (aiLevel != currentAILevel_) {
            currentAILevel_ = aiLevel;
            notifyObservers();
        }
    }
    
    //---------------------------------------------------------
    public void registerMainActivity(MainActivity activity) {
        mainActivity_ = new WeakReference<MainActivity>(activity);
    }
    
    // --------------------------------------------------------
    /**
     * A message handler to handle UI related tasks.
     */
    private static final int MSG_NETWORK_EVENT = 1;
    private Handler messageHandler_ = new MessageHandler(/*this*/);
    static class MessageHandler extends Handler {
        
        MessageHandler() {
            // empty
        }
        
        @Override
        public void handleMessage(Message msg){
            switch (msg.what) {
            case MSG_NETWORK_EVENT:
            {
                String event = (String) msg.obj;
                Log.d(TAG, "(MessageHandler) Network event arrived.");
                HoxApp.getApp().onNetworkEvent(event);
                break;
            }
                
            default:
                break;
            }
        }
    };
    
    private void onNetworkEvent(String eventString) {
        Log.d(TAG, "On Network event. ENTER.");
        
        HashMap<String, String> newEvent = new HashMap<String, String>();
        
        for (String token : eventString.split("&")) {
            //Log.d(TAG, "... token = [" + token + "]");
            final String[] pair = token.split("=");
            newEvent.put(pair[0], pair[1]);
            //Log.d(TAG, "... >>> pair[0]= [" + pair[0] + "], pair[1]=[" + pair[1] + "]");
        }
        
        final String op = newEvent.get("op");
        final int code = Integer.parseInt( newEvent.get("code") );
        final String content = newEvent.get("content");
        //final String tid = newEvent.get("tid");
        
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
        }
    }
    
    private void handleNetworkEvent_LOGIN(int code, String content) {
        Log.d(TAG, "Handle event (LOGIN): ENTER.");
        
        if (code != 0) {  // Error
            Log.i(TAG, "Login failed. Error: [" + content + "]");
            // [self _showLoginView:[self _getLocalizedLoginError:code defaultError:event]];
            return;
        }
        
        final String[] components = content.split(";");
        final String pid = components[0];
        final String rating = components[1];
        Log.d(TAG, ">>> [" + pid + " " + rating + "] LOGIN.");
        
        if (pid_.equals(pid)) { // my LOGIN?
            Log.i(TAG, ">>>>>> Got my LOGIN info [" + pid + " " + rating + "].");
            myRating_ = rating;
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
        if (mainActivity != null) {
            myColor_ = ColorEnum.COLOR_NONE;
            myTable_ = new TableInfo(content);
            Log.i(TAG, "... >>> Set my table Id: " + myTable_.tableId);
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
        
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.updateBoardWithNewMove(move);
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
        
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity == null) {
            Log.w(TAG, "The main activity is NULL. Ignore this LEAVE event.");
            //return;
        }
        
        
        // Special case: The player left Red/Black seat.
        Enums.ColorEnum playerPreviousColor = ColorEnum.COLOR_UNKNOWN;
        if (pid.equals(myTable_.blackId)) {
            playerPreviousColor = ColorEnum.COLOR_BLACK;
        } else if (pid.equals(myTable_.redId)) {
            playerPreviousColor = ColorEnum.COLOR_RED;
        }
        
        // Check if I just left the Table.
        if (pid.equals(pid_)) {
            Log.i(TAG, "I just left my table: " + tableId);
            if (mainActivity != null) {
                mainActivity.updateBoardAfterLeavingTable();
            }
            myTable_ = new TableInfo();
            myColor_ = ColorEnum.COLOR_UNKNOWN;
        
        } else { // Other player left my table?
            myTable_.onPlayerLeft(pid);
            if (mainActivity != null) {
                mainActivity.onPlayerLeftTable(pid, playerPreviousColor);
            }
        }
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
        
        // Special case: The player left Red/Black seat.
        Enums.ColorEnum playerPreviousColor = ColorEnum.COLOR_UNKNOWN;
        if (pid.equals(myTable_.blackId)) {
            playerPreviousColor = ColorEnum.COLOR_BLACK;
        } else if (pid.equals(myTable_.redId)) {
            playerPreviousColor = ColorEnum.COLOR_RED;
        }
        
        myTable_.onPlayerJoined(pid, rating, playerColor);
        
        final String playerInfo = TableInfo.formatPlayerInfo(pid, rating);
        boolean myColorChanged = false;
        Enums.ColorEnum myLastColor = myColor_;
        
        if ("Red".equals(color)) {
            if (pid.equals(pid_)) {
                myColor_ = ColorEnum.COLOR_RED;
                myColorChanged = true;
            }
            
        } else if ("Black".equals(color)) {
            if (pid.equals(pid_)) {
                myColor_ = ColorEnum.COLOR_BLACK;
                myColorChanged = true;
            }
            
        } else if ("None".equals(color)) {
            if (pid.equals(pid_)) {
                myColor_ = ColorEnum.COLOR_NONE;
                myColorChanged = true;
            }
        }
        mainActivity.onPlayerJoinedTable(pid, playerColor, playerPreviousColor, playerInfo, myColorChanged, myLastColor);
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
        } if ("drawn".equals(gameResult)) {
            gameStatus = GameStatus.GAME_STATUS_DRAWN;
        }
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
        
        mainActivity.onGameReset();
    }
    
    // --------------------------------------------------------
    
    public void createNetworkPlayer() {
        if ( ! networkPlayer_.isOnline() ) {
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
    
    public TableInfo getMyTable() {
        return myTable_;
    }
    
    public String getCurrentTableId() {
        return myTable_.tableId;
    }
    
    public void logoutFromNetwork() {
        if (networkPlayer_.isOnline() ) {
            networkPlayer_.disconnectFromServer();
        }
        myTable_ = new TableInfo();
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
                
            case COLOR_NONE: /* falls through */
            case COLOR_UNKNOWN: // FIXME: We should already set to "NONE" when we join the table.
                if (clickedColor == ColorEnum.COLOR_BLACK && myTable_.blackId.length() == 0) {
                    requestedColor = ColorEnum.COLOR_BLACK;
                } else if (clickedColor == ColorEnum.COLOR_RED && myTable_.redId.length() == 0) {
                    requestedColor = ColorEnum.COLOR_RED;
                }
                break;
                
            default:
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
        if (       (myColor_ == ColorEnum.COLOR_RED || myColor_ == ColorEnum.COLOR_BLACK)
                && (requestedColor == ColorEnum.COLOR_RED || requestedColor == ColorEnum.COLOR_BLACK) ) {
            
            networkPlayer_.sendRequest_JOIN(myTable_.tableId, "None");
        }
        
        networkPlayer_.sendRequest_JOIN(myTable_.tableId, joinColor);
    }
    
    @SuppressLint("DefaultLocale")
    public void handleLocalMove(Position fromPos, Position toPos) {
        Log.i(TAG, "Handle local move");
        if (myTable_.isValid()) {
            final String move = String.format("%d%d%d%d",
                    fromPos.column, fromPos.row, toPos.column, toPos.row);
            Log.i(TAG, " .... move: [" + move + "]");
            networkPlayer_.sendRequest_MOVE(myTable_.tableId, move);
        }
    }
    
    public void postNetworkEvent(String eventString) {
        Log.d(TAG, "Post Network event = [" + eventString + "]");
        messageHandler_.sendMessage(
                messageHandler_.obtainMessage(MSG_NETWORK_EVENT, eventString) );
    }
    
}
