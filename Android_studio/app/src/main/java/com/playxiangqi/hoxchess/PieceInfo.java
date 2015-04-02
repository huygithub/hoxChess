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
public class PieceInfo {

    public String type;
    public String color;
    public Position position;

    private boolean captured_ = false;
    
    public PieceInfo(String aType, String aColor, Position aPosition) {
        type = aType;
        color = aColor;
        position = aPosition.clone();
    }

    public PieceInfo clone() {
        return new PieceInfo(type, color, position);
    }
    
    public boolean isCaptured() {
        return captured_;
    }
    
    public void setCaptured(boolean val) {
        captured_ = val;
    }
    
    public void setPosition(Position newPosition) {
        position.row = newPosition.row;
        position.column = newPosition.column;
    }
}
