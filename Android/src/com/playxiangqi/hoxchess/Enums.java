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
public class Enums {
    public enum ColorEnum {
        
        COLOR_UNKNOWN,
                // This type indicates the absence of color or role.
                // For example, it is used to indicate the player is not even
                // at the table.

        COLOR_RED,   // RED color.
        COLOR_BLACK, // BLACK color.

        COLOR_NONE
            // NOTE: This type actually does not make sense for 'Piece',
            //       only for "Player". It is used to indicate the role of a player
            //       who is currently only observing the game, not playing.
    }
}
