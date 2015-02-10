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
    
    private NetworkPlayer networkPlayer_;
    private WeakReference<MainActivity> mainActivity_;
    private TableInfo myTable_ = new TableInfo();
    //private String tableId_;
    private ColorEnum myColor_ = ColorEnum.COLOR_UNKNOWN;
    
    public HoxApp() {
        // Do nothing.
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
            TableInfo tableInfo = new TableInfo(content);
            myTable_ = tableInfo;
            Log.i(TAG, "... >>> Set my table Id: " + myTable_.tableId);
            mainActivity.updateBoardWithNewTableInfo(tableInfo);
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
        
        // Check if I just left the Table.
        if (myTable_.hasId(tableId) && pid.equals(pid_)) {
            Log.i(TAG, "I just left my table: " + tableId);
            MainActivity mainActivity = mainActivity_.get();
            if (mainActivity != null) {
                mainActivity.updateBoardAfterLeavingTable();
            }
            myTable_ = new TableInfo();
        }
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
        
        final String playerInfo = TableInfo.formatPlayerInfo(pid, rating);
        boolean myColorChanged = false;
        Enums.ColorEnum myLastColor = myColor_;
        
        if ("Red".equals(color)) {
            //mainActivity.updateBoardWithPlayerInfo(ColorEnum.COLOR_RED, playerInfo);
            if (pid.equals(pid_)) {
                myColor_ = ColorEnum.COLOR_RED;
                myColorChanged = true;
            }
            mainActivity.onPlayerJoinedTable(ColorEnum.COLOR_RED, playerInfo, myColorChanged, myLastColor);
            
        } else if ("Black".equals(color)) {
            //mainActivity.updateBoardWithPlayerInfo(ColorEnum.COLOR_BLACK, playerInfo);
            if (pid.equals(pid_)) {
                myColor_ = ColorEnum.COLOR_BLACK;
                myColorChanged = true;
            }
            mainActivity.onPlayerJoinedTable(ColorEnum.COLOR_BLACK, playerInfo, myColorChanged, myLastColor);
            
        } else if ("None".equals(color)) {
            if (pid.equals(pid_)) {
                myColor_ = ColorEnum.COLOR_NONE;
                myColorChanged = true;
            }
            mainActivity.onPlayerJoinedTable(ColorEnum.COLOR_NONE, playerInfo, myColorChanged, myLastColor);
        }
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
        networkPlayer_.sendRequest_JOIN(tableId, "None");
    }

    public void handleRequestToCloseCurrentTable() {
        Log.i(TAG, "Close the current table...");
        if (!myTable_.isValid()) {
            Log.w(TAG, "No current table. Ignore the request to Close the current Table");
            return;
        }
        networkPlayer_.sendRequest_LEAVE(myTable_.tableId);
        //tableId_ = null;
    }
    
    public void handleTopButtonClick() {
        Log.i(TAG, "Handle top-button click");
        if (myTable_.isValid()) {
            if (myColor_ == ColorEnum.COLOR_RED || myColor_ == ColorEnum.COLOR_BLACK) {
                networkPlayer_.sendRequest_JOIN(myTable_.tableId, "None");
            } else {
                networkPlayer_.sendRequest_JOIN(myTable_.tableId, "Black");
            }
        }
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
