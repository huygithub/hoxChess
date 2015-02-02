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

/**
 * A table
 */
public class TableInfo {

    public String tableId;
    public boolean rated;
    public String itimes;   // Initial times.
    public String redTimes;
    public String blackTimes;
    public String redId;
    public String redRating;
    public String blackId;
    public String blackRating;
    
    public TableInfo() {
        // empty
    }
    
    public TableInfo(String tableStr) {
        final String[] components = tableStr.split(";");
        //for (String entry : entries) {
        //    Log.d(TAG, ">>> ..... entry [" + entry + "].");
        //}
        
        tableId = components[0];
        rated = "0".equals(components[2]);
        itimes = components[3];
        redTimes = components[4];
        blackTimes = components[5];
        redId = components[6];
        redRating = components[7];
        blackId = components[8];
        blackRating = components[9];
    }

    public boolean isValid() {
        return (tableId != null);
    }
    
    public boolean hasId(String tid) {
        return (tableId != null && tableId.equals(tid));
    }
    
    static public String formatPlayerInfo(String pid, String rating) {
        return (pid.length() == 0
                ? "*" : String.format("%s(%s)", pid, rating));
    }
    
    public String getRedInfo() {
        return formatPlayerInfo(redId, redRating);
        //return (redId.length() == 0
        //        ? "*" : String.format("%s(%s)", redId, redRating));
    }
    
    public String getBlackInfo() {
        return formatPlayerInfo(blackId, blackRating);
        //return (blackId.length() == 0
        //        ? "*" : String.format("%s(%s)", blackId, blackRating));
    }
    
    @Override
    public String toString() {
        final String redInfo = (redId.length() == 0
                ? "*" : String.format("%s(%s)", redId, redRating));
        final String blackInfo = (blackId.length() == 0
                ? "*" : String.format("%s(%s)", blackId, blackRating));
        
        return String.format("%s | %s | %s | %s ", tableId, itimes, redInfo, blackInfo);
    }
}
