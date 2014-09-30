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

import android.util.Log;

/**
 * NOTE:
 *  This class was converted directly from the one defined in my Flex-based 'ChessWhiz' project.
 */
public class Referee {

    private static final String TAG = "Referee";
    
    /**
     * Constants for the different Move Types.
     */
    private static final int M_HORIZONTAL = 0;
    private static final int M_VERTICAL   = 1;
    private static final int M_DIAGONAL   = 2;
    private static final int M_LSHAPE     = 3;  // The L-shape.
    private static final int M_OTHER      = 4;  // The 'other' type.

    private String _nextColor = "None"; // Keep track who moves NEXT.
    private int _nMoves = 0; // The number of moves.

    private PieceInfo[] _redPieces;
    private PieceInfo[] _blackPieces;

    private PieceInfo _redKing;
    private PieceInfo _blackKing;
    private PieceInfo[][] _pieceMap;

    public Referee() {
        this.resetGame();
    }
    
    /**
     * Reset the Game back to the initial state.
     *
     * @note This function is optimized so that it will not reset TWICE
     *       so that outside callers can call it multiple times with much
     *       performance penalty.
     */
    public void resetGame() {
        if ( "Red".equals(_nextColor) && _nMoves == 0 ) {
            return; // Already in the initial state. 
        }

        _nextColor = "Red";
        _nMoves = 0;

        // --- Create piece objects.

        _redPieces = new PieceInfo[16];
        _redPieces[0]  = new PieceInfo("chariot",  "Red", new Position(9, 0));
        _redPieces[1]  = new PieceInfo("horse",    "Red", new Position(9, 1));
        _redPieces[2]  = new PieceInfo("elephant", "Red", new Position(9, 2));
        _redPieces[3]  = new PieceInfo("advisor",  "Red", new Position(9, 3));
        _redPieces[4]  = new PieceInfo("king",     "Red", new Position(9, 4));
        _redPieces[5]  = new PieceInfo("advisor",  "Red", new Position(9, 5));
        _redPieces[6]  = new PieceInfo("elephant", "Red", new Position(9, 6));
        _redPieces[7]  = new PieceInfo("horse",    "Red", new Position(9, 7));
        _redPieces[8]  = new PieceInfo("chariot",  "Red", new Position(9, 8));
        _redPieces[9]  = new PieceInfo("cannon",   "Red", new Position(7, 1));
        _redPieces[10] = new PieceInfo("cannon",   "Red", new Position(7, 7));
        for (int pawn = 0; pawn < 5; pawn++) {
            _redPieces[11 + pawn] = new PieceInfo("pawn", "Red", new Position(6, 2*pawn));
        }

        _blackPieces = new PieceInfo[16];
        _blackPieces[0]  = new PieceInfo("chariot",  "Black", new Position(0, 0));
        _blackPieces[1]  = new PieceInfo("horse",    "Black", new Position(0, 1));
        _blackPieces[2]  = new PieceInfo("elephant", "Black", new Position(0, 2));
        _blackPieces[3]  = new PieceInfo("advisor",  "Black", new Position(0, 3));
        _blackPieces[4]  = new PieceInfo("king",     "Black", new Position(0, 4));
        _blackPieces[5]  = new PieceInfo("advisor",  "Black", new Position(0, 5));
        _blackPieces[6]  = new PieceInfo("elephant", "Black", new Position(0, 6));
        _blackPieces[7]  = new PieceInfo("horse",    "Black", new Position(0, 7));
        _blackPieces[8]  = new PieceInfo("chariot",  "Black", new Position(0, 8));
        _blackPieces[9]  = new PieceInfo("cannon",   "Black", new Position(2, 1));
        _blackPieces[10] = new PieceInfo("cannon",   "Black", new Position(2, 7));
        for (int pawn = 0; pawn < 5; pawn++) {
            _blackPieces[11 + pawn] = new PieceInfo("pawn", "Black", new Position(3, 2*pawn));
        }

        // --- Initialize other internal variables.
        _redKing = _redPieces[4];
        _blackKing = _blackPieces[4];
        _initializePieceMap();
    }

    private void _initializePieceMap() {
        // --- Initialize piece map.
        _pieceMap = new PieceInfo[10][9];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                _pieceMap[i][j] = null;
            }
        }

        _pieceMap[0][0] = _blackPieces[0];
        _pieceMap[0][1] = _blackPieces[1];
        _pieceMap[0][2] = _blackPieces[2];
        _pieceMap[0][3] = _blackPieces[3];
        _pieceMap[0][4] = _blackPieces[4];
        _pieceMap[0][5] = _blackPieces[5];
        _pieceMap[0][6] = _blackPieces[6];
        _pieceMap[0][7] = _blackPieces[7];
        _pieceMap[0][8] = _blackPieces[8];
        _pieceMap[2][1] = _blackPieces[9];
        _pieceMap[2][7] = _blackPieces[10];
        for (int pawn = 0; pawn < 5; pawn++) {
            _pieceMap[3][2*pawn] = _blackPieces[11 + pawn];
        }

        _pieceMap[9][0] = _redPieces[0];
        _pieceMap[9][1] = _redPieces[1];
        _pieceMap[9][2] = _redPieces[2];
        _pieceMap[9][3] = _redPieces[3];
        _pieceMap[9][4] = _redPieces[4];
        _pieceMap[9][5] = _redPieces[5];
        _pieceMap[9][6] = _redPieces[6];
        _pieceMap[9][7] = _redPieces[7];
        _pieceMap[9][8] = _redPieces[8];
        _pieceMap[7][1] = _redPieces[9];
        _pieceMap[7][7] = _redPieces[10];
        for (int pawn = 0; pawn < 5; pawn++) {
            _pieceMap[6][2*pawn] = _redPieces[11 + pawn];
        }
    }
    
    public String nextColor() {
        return _nextColor;
    }

    /**
     * Find a piece at a given position.
     * 
     * @return The piece found at the position. If no piece is found, return null.
     */
    public PieceInfo findPieceAtPosition(Position pos) {
        if (pos == null || pos.valid() == false) {
            return null;
        }
        PieceInfo piece = _pieceMap[pos.row][pos.column];
        return ( piece != null ? piece.clone() : null );
    }
    
    private Position _getPositionOfKing(String color) {
        final PieceInfo king = ("Red".equals(color) ? _redKing : _blackKing);
        return king.position.clone();   
    }
    
    /**
     * @return The number of pieces between the two given positions.
     */
    private int _getIntervenedCount(Position curPos, Position newPos) {
        int numPieces = 0;   // The number intervened pieces.
        final int newRow = newPos.row;
        final int newCol = newPos.column;
        final int curRow = curPos.row;
        final int curCol = curPos.column;
        final int rowDiff = Math.abs(curRow - newRow);
        final int colDiff = Math.abs(curCol - newCol);

        final int startCol = (curCol > newCol ? newCol : curCol);
        final int startRow = (curRow > newRow ? newRow : curRow);

        int i = 0;

        final int move = _getMoveType(rowDiff, colDiff);
        switch ( move )
        {
            case M_HORIZONTAL:
            {
                for (i = 1; i < colDiff; i++) {
                    if (_pieceMap[curRow][startCol + i] != null) {
                        numPieces++;
                    }
                }
                break;
            }
            case M_VERTICAL:
            {
                for (i = 1; i < rowDiff; i++) {
                    if (_pieceMap[startRow + i][curCol] != null) {
                        numPieces++;
                    }
                }
                break;
            }
            case M_DIAGONAL:
            {
                if (curRow < newRow) {
                    for (i = 1; i < rowDiff; i++) {
                        if (curCol < newCol) {
                            if (_pieceMap[curRow + i][curCol + i] != null) {
                                numPieces++;
                            }
                        } else {
                            if (_pieceMap[curRow + i][curCol - i] != null) {
                                numPieces++;
                            }
                        }
                    }
                } else {
                    for (i = 1; i < rowDiff; i++) {
                        if (curCol < newCol) {
                            if (_pieceMap[curRow - i][curCol + i] != null) {
                                numPieces++;
                            }
                        } else {
                            if (_pieceMap[curRow - i][curCol - i] != null) {
                                numPieces++;
                            }
                        }
                    }
                }
                break;
            }
            case M_LSHAPE:
            {
                if (rowDiff == 1 && colDiff == 2) {
                    if (curCol > newCol) {
                        if (_pieceMap[curRow][curCol - 1] != null) {
                            numPieces++;
                        }
                    } else {
                        if (_pieceMap[curRow][curCol + 1] != null) {
                            numPieces++;
                        }
                    }
                } else {
                    if (curRow > newRow) {
                        if (_pieceMap[curRow - 1][curCol] != null) {
                            numPieces++;
                        }
                    } else {
                        if (_pieceMap[curRow + 1][curCol] != null) {
                            numPieces++;
                        }
                    }
                }
                break;
            }
        } /* switch (...) */

        return numPieces;
    }
    
    /**
     * Check whether a position is inside the Palace (or Fortress).
     */
    private boolean _isInsidePalace(String color, Position pos) {
        if ( "Black".equals(color) ) {
            return (pos.column <= 5 && pos.column >= 3) && (pos.row <= 2 && pos.row >= 0);
        }
        /*      "Red"     */
        return (pos.column <= 5 && pos.column >= 3) && (pos.row <= 9 && pos.row >= 7);
    }
    
    /**
     * Check whether a position is inside the same country
     * (i.e., not yet cross the River).
     */
    private boolean _isInsideCountry(String color, Position pos) {
        if ( "Black".equals(color) ) { return (pos.row >=0 && pos.row <= 4); }
        /*      "Red"       */         return (pos.row >= 5 && pos.row <= 9);
    }
    
    static class MoveResult {
        public boolean valid = false;     // is a valid move?
        public boolean captured = false;  // is a capture move?
        public MoveResult(boolean valid, boolean captured) {
            this.valid = valid;
            this.captured = captured;
        }
    }
    
    /**
     * Check whether a give Move is valid. If yes, record the Move. 
     *
     * @return the move-result which has two elements:
     *     (1) one to indicate whether the Move is valid.
     *     (2) one to indicate whether the Move is a capture move.
     */
    public MoveResult validateAndRecordMove(Position oldPos, Position newPos) {
        PieceInfo piece = _pieceMap[oldPos.row][oldPos.column];
        if ( piece == null ) {
            Log.e(TAG, "Referee: Logic Error! Piece is null.");
            return new MoveResult(false, false);
        }

        /* Check for 'turn' */
        if ( ! _nextColor.equals(piece.color) ) {
            return new MoveResult(false, false);
        }

        /* Perform a basic validation. */
        if ( ! _performBasicValidationOfMove(piece, newPos) ) {
            return new MoveResult(false, false);
        }

        /* At this point, the Move is valid.
         * Record this move (to validate future Moves).
         */
        final PieceInfo capturedPiece = _recordMove(piece, newPos);

        /* If the Move violates one rule, which says that
         * "Your own King should not be checked after your Move.",
         * then it is invalid and must be undone.
         *
         * NOTE: For the case of "King-facing-King", it has been taken
         *       care of inside the "King" basic validation.
         */
        if ( _isKingBeingChecked( piece.color ) ) {
            _undoMove(piece, oldPos, capturedPiece);
            return new MoveResult(false, false);
        }

        /* Set the next-turn. */
        _nextColor = ( "Red".equals(_nextColor) ? "Black" : "Red" );
        ++_nMoves;

        final boolean bCapturedMove = (capturedPiece != null);
        return new MoveResult(true, bCapturedMove);
    }
    
    /**
     * @return The piece being captured, if any..
     */
    private PieceInfo _recordMove(PieceInfo piece, Position newPos) {
        PieceInfo capturedPiece = _pieceMap[newPos.row][newPos.column];
        if (capturedPiece != null) {
            capturedPiece.setCaptured(true);
        }

        _pieceMap[newPos.row][newPos.column] = piece;
        _pieceMap[piece.position.row][piece.position.column] = null;

        piece.setPosition(newPos);

        return capturedPiece;
    }
    
    private void _undoMove(PieceInfo piece, Position oldPos, PieceInfo capturedPiece) {
        final Position curPos = piece.position;

        _pieceMap[oldPos.row][oldPos.column] = piece;
        _pieceMap[curPos.row][curPos.column] = capturedPiece;
        
        piece.setPosition(oldPos);

        if (capturedPiece != null) {
            capturedPiece.setCaptured(false);
            capturedPiece.setPosition(curPos);
        }
    }
    
    /**
     * Perform a basic validation.
     *
     * @return false if the Move is invalid.
     */
    private boolean _performBasicValidationOfMove(PieceInfo piece, Position newPos) {
        final String myColor = piece.color;
        final Position curPos = piece.position;
        final PieceInfo capture = _pieceMap[newPos.row][newPos.column];

        if (   (newPos.row == curPos.row && newPos.column == curPos.column) // Same position?
            || (capture != null && myColor.equals(capture.color)) ) // ... or same side?
        {
            Log.d(TAG, "Referee: Move is invalid (Same position or same side).");
            return false;
        }

        final int rowDiff = Math.abs(curPos.row - newPos.row);
        final int colDiff = Math.abs(curPos.column - newPos.column);

        final int move = _getMoveType(rowDiff, colDiff);
        final int nIntervened = _getIntervenedCount(curPos, newPos);

        if ("king".equals(piece.type)) {
            if (     _isInsidePalace(myColor, newPos)
                  && ((move == M_HORIZONTAL && colDiff == 1) || (move == M_VERTICAL && rowDiff == 1))) {
                return true;
            }
            if (  capture != null && "king".equals(capture.type) /* Flying king */
                    && move == M_VERTICAL && nIntervened == 0 ) {
                return true;
            }
        }
        else if ("advisor".equals(piece.type)) {
            if (     _isInsidePalace(myColor, newPos)
                  && (move == M_DIAGONAL && rowDiff == 1)) {
                return true;
            }
        }
        else if ("elephant".equals(piece.type)) {
            if (     _isInsideCountry(myColor, newPos)
                  && (move == M_DIAGONAL && rowDiff == 2 && nIntervened == 0)) {
                return true;
            }
        }
        else if ("horse".equals(piece.type)) {
            if (move == M_LSHAPE && nIntervened == 0) {
                return true;
            }
        }
        else if ("chariot".equals(piece.type)) {
            if (     (move == M_HORIZONTAL || move == M_VERTICAL)
                  && nIntervened == 0 ) {
                return true;
            }
        }
        else if ("cannon".equals(piece.type)) {
            if (move == M_HORIZONTAL || move == M_VERTICAL) {
                if (     (capture != null && nIntervened == 1)
                      || (capture == null && nIntervened == 0) ) {
                    return true;
                }
            }
        }
        else if ("pawn".equals(piece.type)) {
            // Make sure the Move is never a "backward" move.
            final boolean bFoward = (  ("Red".equals(myColor)   && newPos.row < curPos.row)
                                    || ("Black".equals(myColor) && newPos.row > curPos.row) ); 
            if (  _isInsideCountry(myColor, newPos) ) { // Within the country?
                if (move == M_VERTICAL && rowDiff == 1 && bFoward) {
                    return true;
                }
            }
            else { // Outside the country (already crossed the River)
                if (     (move == M_VERTICAL && rowDiff == 1 && bFoward)
                      || (move == M_HORIZONTAL && colDiff == 1) ) {
                    return true;
                }
            }
        }

        return false;  // Invalid Move.
    }
    
    private int _getMoveType(int rowDiff, int colDiff) {
        int move = M_OTHER;   // Move type.

        if      (rowDiff == 0)       { move = M_HORIZONTAL; }
        else if (colDiff == 0)       { move = M_VERTICAL;   }
        else if (rowDiff == colDiff) { move = M_DIAGONAL;   }
        else if ( (rowDiff == 1 && colDiff == 2) || (rowDiff == 2 && colDiff == 1) ) {
            move = M_LSHAPE;
        }

        return move;
    }

    /**
     * This function performs that check to see of any of my opponent pieces
     * can capture my own King.
     */
    private boolean _isKingBeingChecked(String myColor) {
        final Position myKingPos = _getPositionOfKing(myColor);
        final PieceInfo[] oppPieces = ("Red".equals(myColor) ? _blackPieces : _redPieces);

        for (PieceInfo oPiece : oppPieces) {
            if (     oPiece.isCaptured()
                  || "elephant".equals(oPiece.type) || "advisor".equals(oPiece.type) ) {
                continue;
            }

            if ( _performBasicValidationOfMove(oPiece, myKingPos) ) {
                return true;
            }
        }
        
        return false;
    }
}
