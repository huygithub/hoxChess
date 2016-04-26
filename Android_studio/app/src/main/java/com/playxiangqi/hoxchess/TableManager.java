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
import java.util.List;

/**
 * This class manages the list of ALL online tables
 *
 */
public class TableManager {

    private static final String TAG = "TableManager";

    // The singleton instance.
    private static TableManager instance_;

    // Member variables...
    private List<TableInfo> tables_ = new ArrayList<TableInfo>();
    private boolean isLoaded_ = false; // already loaded with the list of tables?

    /**
     * Singleton API to return the instance.
     */
    public static TableManager getInstance() {
        if (instance_ == null) {
            instance_ = new TableManager();
        }
        return instance_;
    }

    /**
     * Constructor
     */
    public TableManager() {
        Log.v(TAG, "[CONSTRUCTOR]: ...");
    }

    // ***************************************************************
    //
    //              Public APIs
    //
    // ***************************************************************

    public int size() {
        return tables_.size();
    }

    public List<TableInfo> getTables() {
        return tables_;
    }

    public void setListContent(String listContent) {
        tables_.clear();
        final String[] entries = listContent.split("\n");
        for (String entry : entries) {
            TableInfo tableInfo = new TableInfo(entry);
            tables_.add(tableInfo);
        }
        isLoaded_ = true;
        Log.d(TAG, ">>> Number of tables = " + tables_.size() + ".");
    }

    public boolean isLoaded() {
        return isLoaded_;
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
