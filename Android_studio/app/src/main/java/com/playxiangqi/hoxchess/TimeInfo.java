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
 * Time information in a table.
 */
public class TimeInfo {

    public int  gameTime;  // Game-time (in seconds).
    public int  moveTime;  // Move-time (in seconds).
    public int  freeTime;  // Free-time (in seconds).
    
    public TimeInfo() {
        gameTime = 0;
        moveTime = 0;
        freeTime = 0;
    }
    
    public TimeInfo(String timeContent) {
        final String[] components = timeContent.split("/");        
        gameTime =  Integer.parseInt(components[0]);
        moveTime = Integer.parseInt(components[1]);
        freeTime = Integer.parseInt(components[2]);
    }
    
    public void initWith(final TimeInfo other) {
        gameTime = other.gameTime;
        moveTime = other.moveTime;
        freeTime = other.freeTime;
    }
    
    public void decrement() {
        if (gameTime > 0) --gameTime;
        if (moveTime > 0) --moveTime;
    }
}
