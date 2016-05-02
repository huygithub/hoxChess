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

import java.util.ArrayList;

/**
 * A move
 */
public class MoveInfo {

    public final Position fromPosition;
    public final Position toPosition;

    public int gameStatus = Referee.hoxGAME_STATUS_UNKNOWN;

    public MoveInfo(Position from, Position to) {
        fromPosition = from;
        toPosition = to;
    }

    @Override
    public String toString() {
        return fromPosition + " => " + toPosition + " (" + Referee.gameStatusToString(gameStatus) + ")";
    }

    /**
     * Parse a string containing a move from PlayXiangqi server to return a move.
     */
    public static MoveInfo parseForNetworkMove(String moveStr) {
        int row1 = moveStr.charAt(1) - '0';
        int col1 = moveStr.charAt(0) - '0';
        int row2 = moveStr.charAt(3) - '0';
        int col2 = moveStr.charAt(2) - '0';
        return new MoveInfo(new Position(row1, col1), new Position(row2, col2));
    }

    public static MoveInfo[] parseForListOfNetworkMoves(String movesStr) {
        final String[] moveStringArray = movesStr.split("/");
        ArrayList<MoveInfo> moves = new ArrayList<MoveInfo>();
        for (String moveStr : moveStringArray) {
            moves.add(parseForNetworkMove(moveStr));
        }

        return moves.toArray(new MoveInfo[moves.size()]);
    }

}
