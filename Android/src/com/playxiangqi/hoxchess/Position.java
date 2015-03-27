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
 * NOTE:
 *  This class was converted directly from the one defined in my Flex-based 'ChessWhiz' project.
 */
public class Position {

    public int row = -1;
    public int column = -1;

    public Position() {
        // Do nothing. Just use the default values.
    }
    
    public Position(int aRow, int aColumn) {
        row = aRow;
        column = aColumn;
    }

    public Position clone() {
        return new Position(row, column);
    }
    
    public void reset() {
        row = -1;
        column = -1;
    }
    
    public boolean valid() {
        return (   (row >= 0 && row <= 9)
                && (column >= 0 && column <= 8));
    }
    
    public boolean equalTo(Position other) {
        return (row == other.row && column == other.column);
    }
    
    @Override
    public String toString() {
        // (Note: from ChessWhize) return "[" + row + "," + String.fromCharCode(97 + column) + "]";
        return "[" + row + "," + column + "]";
    }
    
    /**
     * Helper function to compare two positions.
     */
    public static boolean equals(Position pos1, Position pos2) {
        if (pos1 == null && pos2 == null) return true;
        if (pos1 == null && pos2 != null) return false;
        if (pos1 != null && pos2 == null) return false;
        return (   pos1.row == pos2.row
                && pos1.column == pos2.column);
    }
}
