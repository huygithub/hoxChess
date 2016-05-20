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

import com.playxiangqi.hoxchess.Enums.GameStatus;
import com.playxiangqi.hoxchess.Enums.TableType;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

public class HoxApp extends Application {

    private static final String TAG = "HoxApp";
    
    private static HoxApp thisApp_;
    
    private String pid_ = ""; // My player 's ID.
    private String password_ = ""; // My player 's password.

    private final AIEngine aiEngine_ = new AIEngine();

    private NetworkController networkController_;

    public HoxApp() { /* Empty */ }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()...");
        thisApp_ = this;

        loadPreferences_Account();

        aiEngine_.setAILevel(SettingsActivity.getAILevel(this));
        networkController_ = NetworkController.getInstance();

        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // NOTE: The referee (JNI based) has a limitation that it has ONLY one instance created!
        //    For more details, see ./app/src/main/jni/Referee.cpp
        //    and pay attention to the "static hoxReferee *referee_".
        // As a result, we must share the global referee under HoxApp.
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        Referee theSharedReferee = new Referee();
        AIController.getInstance().setReferee(theSharedReferee);
        NetworkTableController.getInstance().setReferee(theSharedReferee);
    }

    public static HoxApp getApp() {
        return thisApp_;
    }
    
    public int getAILevel() { return aiEngine_.getAILevel(); }

    public void onAILevelChanged(int aiLevel) {
        Log.d(TAG, "On new AI level: " + aiLevel);
        aiEngine_.setAILevel(aiLevel);
    }

//    public void onLoginWithAccountFlagChanged(boolean loginWithAccount) {
//        Log.d(TAG, "On new login-with-account flag: " + loginWithAccount);
//        //if (!TextUtils.equals(pid_, pid)) {
//            if (this.isOnline() && networkController_.isLoginOK()) {
//                Log.i(TAG, "... (online & LoginOK) Skip using the new flag: " + loginWithAccount + ".");
//            } else {
//                Log.i(TAG, "... (offline) Save new flag: " + loginWithAccount + ".");
//                //pid_ = pid;
//                loadPreferences_Account();
//            }
//        //}
//    }

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
    
    public void loadPreferences_Account() {
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

    public String getMyPid() { return pid_; }
    public AIEngine getAiEngine() { return aiEngine_; }
    public NetworkController getNetworkController() { return networkController_; }
    public boolean isOnline() { return networkController_.isOnline(); }
    public boolean isGameOver() { return networkController_.isGameOver(); }
    public GameStatus getGameStatus() { return networkController_.getGameStatus(); }

    public void loginServer() {
        // NOTE: Do not call loadPreferences_Account() again because it may generate
        //       another new Guest ID while the existing one is being used by network controller.
        networkController_.setLoginInfo(pid_,  password_);
        networkController_.connectToServer();
    }

    public void disconnectAndLoginToServer() {
        Log.i(TAG, "Disconnect and login again...");
        loadPreferences_Account(); // to get pid_ and password_
        networkController_.setLoginInfo(pid_,  password_);
        networkController_.reconnectToServer();
    }

}
