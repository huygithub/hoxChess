/**
 *  Copyright 2015 Huy Phan <huyphan@playxiangqi.com>
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
 * A player
 */
public class PlayerInfo {

    public String pid = "";
    public String rating = "0";
    
    public PlayerInfo() {
        // empty
    }
    
    public PlayerInfo(String pid, String rating) {
        this.pid = pid;
        this.rating = rating;
    }
    
    public boolean hasPid(String pid) {
        return this.pid.equals(pid);
    }
    
    public boolean isValid() {
        return (pid.length() > 0);
    }
    
    public String getInfo() {
        return formatPlayerInfo(pid, rating);
    }
    
    @Override
    public String toString() {
        return getInfo();
    }

    static public String formatPlayerInfo(String pid, String rating) {
        return (pid.length() == 0
                ? "*" : String.format("%s(%s)", pid, rating));
    }
}
