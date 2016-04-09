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

import android.graphics.Bitmap;
import android.graphics.PointF;

/**
 * NOTE:
 *  This class was converted directly from the one defined in my Flex-based 'ChessWhiz' project.
 */
public class Piece {

    //private final int _imageRadius = 30; // TODO: Assuming Piece's size = 60 pixels.

    private String _type;
    private String _color;
    private int _row;
    private int _column;
    private PointF _pointF = new PointF(0, 0);  // The top-left point.
    private PointF _previousPointF = new PointF(0, 0);
    private BoardView _board;

    private int _initialRow;
    private int _initialColumn;
    //private String _imageSrc;
    private int _resId;
    private Bitmap _bitmap;
    //private var _image:Image      = new Image();
    //private var _skinIndex:int    = -1;
    private boolean _captured = false;
    private boolean _isAnimated = false;
    
    public Piece(String type, String color, int row, int column, BoardView board) {
        _type    = type;
        _color   = color;
        _row     = row;
        _column  = column;
        _board   = board;

        _initialRow    = _row;
        _initialColumn = _column;
        
        // Determine the piece 's resource ID.
        //_imageSrc      = ("Red".equals(_color) ? "r" : "b") + _type + ".png";
        if ("Red".equals(_color)) {
            if ("chariot".equals(_type)) _resId = R.drawable.rchariot;
            if ("horse".equals(_type)) _resId = R.drawable.rhorse;
            if ("elephant".equals(_type)) _resId = R.drawable.relephant;
            if ("advisor".equals(_type)) _resId = R.drawable.radvisor;
            if ("king".equals(_type)) _resId = R.drawable.rking;
            if ("cannon".equals(_type)) _resId = R.drawable.rcannon;
            if ("pawn".equals(_type)) _resId = R.drawable.rpawn;
        } else { // "Black"
            if ("chariot".equals(_type)) _resId = R.drawable.bchariot;
            if ("horse".equals(_type)) _resId = R.drawable.bhorse;
            if ("elephant".equals(_type)) _resId = R.drawable.belephant;
            if ("advisor".equals(_type)) _resId = R.drawable.badvisor;
            if ("king".equals(_type)) _resId = R.drawable.bking;
            if ("cannon".equals(_type)) _resId = R.drawable.bcannon;
            if ("pawn".equals(_type)) _resId = R.drawable.bpawn;
        }
        
        final int pieceSize = _board.getPieceSize();
        _bitmap = BoardView.decodeSampledBitmapFromResource(
                _board.getResources(), _resId,
                pieceSize, pieceSize);
    }

    public String getColor() { return _color; }
    public boolean isCaptured() { return _captured; }
    public Position getPosition() { return new Position(_row, _column); }
    public Position getInitialPosition() { return new Position(_initialRow, _initialColumn); }
    
    public void setCapture(boolean flag) { _captured = flag;}
    
    public void setPosition(Position newPos) {
        _row = newPos.row;
        _column = newPos.column;
    }

    /**
     * This API is currently needed when animating the piece 's movement.
     * See: movePieceToPositionWithAnimation() of BoardView class.
     *
     * @param point The point to move to.
     */
    public void setPointF(PointF point) {
        _previousPointF.set(_pointF);
        _pointF.x = point.x;
        _pointF.y = point.y;
    }

    public PointF getPointF() { return _pointF; }
    public PointF getPreviousPointF() { return _previousPointF; }

    public boolean isAnimated() { return _isAnimated; }
    public void setIsAnimated(boolean animated) { _isAnimated = animated; }

    public Bitmap getBitmap() { return _bitmap; }
    
    /**
     * Represents a piece 's move.
     */
    public static class Move {
        public Position fromPosition;
        public Position toPosition;
        public boolean isCaptured = false;
    }
}
