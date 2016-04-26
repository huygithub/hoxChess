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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private boolean playersLoaded_ = false; // already loaded with the initial list of players?

    private List<TableInfo> tables_ = new ArrayList<TableInfo>();
    private boolean tablesLoaded_ = false; // already loaded with the new list of tables?

    // *************************************************************************************
    public interface EventListener {
        void onPlayersLoaded();
        void onTablesLoaded();
    }
    private Set<EventListener> listeners_ = new HashSet<EventListener>();

    public void addListener(EventListener listener) {
        listeners_.add(listener);
        Log.d(TAG, "addListener: listeners-size:" + listeners_.size());
    }

    public void removeListener(EventListener listener) {
        listeners_.remove(listener);
        Log.d(TAG, "removeListener: listeners-size:" + listeners_.size());
    }

    // *************************************************************************************

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

    public void clearTables() {
        tables_.clear();
        tablesLoaded_ = false;
    }

    public boolean areTablesLoaded() {
        return tablesLoaded_;
    }

    public List<TableInfo> getTables() {
        return tables_;
    }

    public int size() {
        return players_.size();
    }

    public HashMap<String, PlayerInfo> getPlayers() {
        return players_;
    }

    public void setInitialPlayers(List<PlayerInfo> players) {
        players_.clear();
        for (PlayerInfo playerInfo : players) {
            players_.put(playerInfo.pid, playerInfo);
        }
        playersLoaded_ = true;

        Log.d(TAG, "setInitialPlayers: just loaded. Notify listeners-size:" + listeners_.size());
        for (EventListener listener : listeners_) {
            listener.onPlayersLoaded();
        }
    }

    public void setTables(List<TableInfo> tables) {
        tables_.clear();
        tables_.addAll(tables);
        tablesLoaded_ = true;

        Log.d(TAG, "setTables: just loaded. Notify listeners-size:" + listeners_.size());
        for (EventListener listener : listeners_) {
            listener.onTablesLoaded();
        }
    }

    public void addPlayer(PlayerInfo playerInfo) {
        players_.put(playerInfo.pid, playerInfo);
    }

    public void removePlayer(String pid) {
        players_.remove(pid);
    }

    public boolean arePlayersLoaded() {
        return playersLoaded_;
    }

    public String findTableOfPlayer(String pid) {
        for (TableInfo table : tables_) {
            if (pid.equals(table.redId) || pid.equals(table.blackId)) {
                return table.tableId;
            }
        }
        return null;
    }

    // ***************************************************************
    //
    //              Private APIs
    //
    // ***************************************************************


}
