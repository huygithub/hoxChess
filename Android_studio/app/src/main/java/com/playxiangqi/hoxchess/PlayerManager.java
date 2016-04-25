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

import android.util.Log;

import java.util.HashMap;

/**
 * This class manages the list of ALL online players
 *
 */
public class PlayerManager {

    private static final String TAG = "PlayerManager";

    // The singleton instance.
    private static PlayerManager instance_;

    // Member variables...
    private HashMap<String, PlayerInfo> players_ = new HashMap<String, PlayerInfo>();
    private boolean isLoaded_ = false; // already loaded with the initial list of players?

    /**
     * Singleton API to return the instance.
     */
    public static PlayerManager getInstance() {
        if (instance_ == null) {
            instance_ = new PlayerManager();
        }
        return instance_;
    }

    /**
     * Constructor
     */
    public PlayerManager() {
        Log.v(TAG, "[CONSTRUCTOR]: ...");
    }

    // ***************************************************************
    //
    //              Public APIs
    //
    // ***************************************************************

    public void clear() {
        players_.clear();
    }

    public int size() {
        return players_.size();
    }

    public HashMap<String, PlayerInfo> getPlayers() {
        return players_;
    }

    public void addPlayer(PlayerInfo playerInfo) {
        players_.put(playerInfo.pid, playerInfo);
    }

    public void removePlayer(String pid) {
        players_.remove(pid);
    }

    public boolean isLoaded() {
        return isLoaded_;
    }

    public void setLoaded() {
        isLoaded_ = true;
    }

    // ***************************************************************
    //
    //              Private APIs
    //
    // ***************************************************************


}
