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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

public class BoardView extends ImageView {

	private static final String TAG = "MainActivity";
	
	private static final int offset_ = 50; // in pixels
	private int cellSize_;
	private int pieceSize_ = 64;  // (in pixels) Note: It should be an even number.
	
	private String topColor_ = "Black"; // Normal view: Black at the top.
	
	private Paint linePaint_;
	private Paint selectPaint_;
	//private Canvas savedCanvas_; // Not working!!!
	
	private Piece[] _redPieces = new Piece[16];
	private Piece[] _blackPieces = new Piece[16];
	
    /**
     * Constants for the different Move Modes.
     * NOTE: Do not change the constants 'values below.
     */
    //private static final int MOVE_MODE_CLICK_N_CLICK = 0;
    //private static final int MOVE_MODE_DRAG_N_DROP = 1;
    // !!! Only this mode is supported !!! private final int moveMode_ = MOVE_MODE_CLICK_N_CLICK;
    
	private Referee referee_ = new Referee();
	private Piece dragPiece_ = null;
	private Position dragStartPos_ = null;
	
    // ----
	boolean mDownTouch = false;
	
	/**
     * @param context
     */
    public BoardView(Context context) {
        super(context);
        Log.d(TAG, "BoardView(): ENTER (1).");
        init();
    }

    /**
     * @param context
     * @param attrs
     */
    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "BoardView(): ENTER (2).");
        init();
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public BoardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Log.d(TAG, "BoardView(): ENTER (3).");
        init();
    }
    
    private void init() {
        setBackgroundColor(Color.BLACK);
    	
        linePaint_ = new Paint(/*Paint.ANTI_ALIAS_FLAG*/);
        linePaint_.setColor(Color.LTGRAY);
        linePaint_.setStrokeWidth(1.0f);
        linePaint_.setTextSize(18.0f);

        selectPaint_ = new Paint(/*Paint.ANTI_ALIAS_FLAG*/);
        selectPaint_.setColor(Color.CYAN);
        
        createPieces();
        
        final ViewTreeObserver vto = getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                int finalHeight = getMeasuredHeight();
                int finalWidth = getMeasuredWidth();
                Log.i(TAG, "~~~ onPreDraw(): Width x Height = " + finalWidth
                        + " x " + finalHeight);

                adjustBoardParameters(finalWidth);

                final ViewTreeObserver vto = getViewTreeObserver();
                vto.removeOnPreDrawListener(this);
                return true;
            }
        });
    }
    
    public int getPieceSize() { return pieceSize_; }
    
    private void createPieces() {        
        String color = "Red";
        _redPieces[0]  = new Piece("chariot",  color, 9, 0, this);
        _redPieces[1]  = new Piece("horse",    color, 9, 1, this);
        _redPieces[2]  = new Piece("elephant", color, 9, 2, this);
        _redPieces[3]  = new Piece("advisor",  color, 9, 3, this);
        _redPieces[4]  = new Piece("king",     color, 9, 4, this);
        _redPieces[5]  = new Piece("advisor",  color, 9, 5, this);
        _redPieces[6]  = new Piece("elephant", color, 9, 6, this);
        _redPieces[7]  = new Piece("horse",    color, 9, 7, this);
        _redPieces[8]  = new Piece("chariot",  color, 9, 8, this);
        _redPieces[9]  = new Piece("cannon",   color, 7, 1, this);
        _redPieces[10] = new Piece("cannon",   color, 7, 7, this);
        
        for (int pawn = 0; pawn < 5; pawn++) {
            _redPieces[11 + pawn] = new Piece("pawn", color, 6, 2*pawn, this);
        }
        
        color = "Black";
        _blackPieces[0]  = new Piece("chariot",  color, 0, 0, this);
        _blackPieces[1]  = new Piece("horse",    color, 0, 1, this);
        _blackPieces[2]  = new Piece("elephant", color, 0, 2, this);
        _blackPieces[3]  = new Piece("advisor",  color, 0, 3, this);
        _blackPieces[4]  = new Piece("king",     color, 0, 4, this);
        _blackPieces[5]  = new Piece("advisor",  color, 0, 5, this);
        _blackPieces[6]  = new Piece("elephant", color, 0, 6, this);
        _blackPieces[7]  = new Piece("horse",    color, 0, 7, this);
        _blackPieces[8]  = new Piece("chariot",  color, 0, 8, this);
        _blackPieces[9]  = new Piece("cannon",   color, 2, 1, this);
        _blackPieces[10] = new Piece("cannon",   color, 2, 7, this);

        for (int pawn = 0; pawn < 5; pawn++) {
            _blackPieces[11 + pawn] = new Piece("pawn", color, 3, 2*pawn, this);
        }
    }
    
    public Position getViewPosition(Position pos) {
        return ( "Black".equals(topColor_) // normal view?
                ? new Position( pos.row, pos.column )
                : new Position( Math.abs(pos.row - 9), Math.abs(pos.column - 8) ) );
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
    	super.onDraw(canvas);
    	Log.d(TAG, "onDraw(): ENTER.");

    	drawBoard(canvas, Color.BLACK, Color.WHITE);
    	drawAllPieces(canvas);
    	
    	//savedCanvas_ = canvas;
    }
	
    private void adjustBoardParameters(int boardWidth) {
        cellSize_ = (boardWidth - 2 * offset_)/8;
        Log.d(TAG, "adjustBoardParameters(): cellSize_ = " + cellSize_);
    }
    
    private void drawBoard(Canvas canvas, int bgColor_UNUSED, int lineColor_UNUSED) {
        // The empty board
        final int boardW = getMeasuredWidth();
        final int boardH = getMeasuredHeight();
        Log.i(TAG, "onDraw(): ~~~ board WxH = " + boardW + ", " + boardH + ".");

        for (int i = 0; i < 10; i++) { // Horizontal lines
            canvas.drawLine(offset_, offset_+i*cellSize_, offset_+8*cellSize_, offset_+i*cellSize_, linePaint_);
        }

        for (int i = 0; i < 9; i++) { // Vertical lines
            if (i == 0 || i == 8) {
                canvas.drawLine(offset_ + i*cellSize_, offset_, offset_ + i*cellSize_, offset_ + cellSize_*9, linePaint_);
            } else {
                canvas.drawLine(offset_ + i*cellSize_, offset_, offset_ + i*cellSize_, offset_ + cellSize_*4, linePaint_);
                canvas.drawLine(offset_ + i*cellSize_, offset_ + 5*cellSize_, offset_ + i*cellSize_, offset_ + 5*cellSize_ + cellSize_*4, linePaint_);
            }
        }
        
        // Diagonal lines to form the Fort (or the Palace).
        canvas.drawLine(offset_ + 3*cellSize_, offset_, offset_ + 3*cellSize_ + 2*cellSize_, offset_ + 2*cellSize_, linePaint_);
        canvas.drawLine(offset_ + 5*cellSize_, offset_, offset_ + 5*cellSize_ - 2*cellSize_, offset_ + cellSize_*2, linePaint_);
        canvas.drawLine(offset_ + 3*cellSize_, offset_ + 7*cellSize_, offset_ + 3*cellSize_ + 2*cellSize_, offset_ + 7*cellSize_ + 2*cellSize_, linePaint_);
        canvas.drawLine(offset_ + 5*cellSize_, offset_ + 7*cellSize_, offset_ + 5*cellSize_ - 2*cellSize_, offset_ + 7*cellSize_ + 2*cellSize_, linePaint_);
        
        // The labels (a-h and 0-9).
        final boolean bDescending = "Red".equals(topColor_);
        final int imageRadius = (int) (pieceSize_/2);
        drawHeaderRow(canvas, offset_ - imageRadius - 10, offset_, bDescending);
        drawHeaderRow(canvas, offset_ + cellSize_*8 + imageRadius, offset_, bDescending);
        drawHeaderColumn(canvas, offset_, offset_, bDescending);
        drawHeaderColumn(canvas, offset_, offset_ + 10*cellSize_ + 20, bDescending);
        
        // Draw the "mirror" lines for Cannons and Pawns.
        final int nSize  = cellSize_ / 7; // The "mirror" 's size.
        final int nSpace = 3;             // The "mirror" 's space (how close/far).

        int[][] mirrors = new int[][] /* Left sides */
            {
                { 1, 2 }, { 7, 2 },
                /* { 0, 3 }, */ { 2, 3 }, { 4, 3 }, { 6, 3 }, { 8, 3 },
                /* { 0, 6 }, */ { 2, 6 }, { 4, 6 }, { 6, 6 }, { 8, 6 },
                { 1, 7 }, { 7, 7 }
            };
        for (int[] m : mirrors) {
            int[] point = new int[] { offset_ + m[0]*cellSize_, offset_ + m[1]*cellSize_ };
            canvas.drawLine(point[0] - nSpace, point[1] - nSpace, point[0] - nSpace - nSize,  point[1] - nSpace, linePaint_);
            canvas.drawLine(point[0] - nSpace, point[1] - nSpace, point[0] - nSpace, point[1] - nSpace - nSize, linePaint_);
            canvas.drawLine(point[0] - nSpace, point[1] + nSpace, point[0] - nSpace - nSize, point[1] + nSpace, linePaint_);
            canvas.drawLine(point[0] - nSpace, point[1] + nSpace, point[0] - nSpace, point[1] + nSpace + nSize, linePaint_);
        }

        mirrors = new int[][] /* Right sides */
            {
                { 1, 2 }, { 7, 2 },
                { 0, 3 }, { 2, 3 }, { 4, 3 }, { 6, 3 }, /* { 8, 3 }, */
                { 0, 6 }, { 2, 6 }, { 4, 6 }, { 6, 6 }, /* { 8, 6 }, */
                { 1, 7 }, { 7, 7 }
            };
        for (int[] m : mirrors)
        {
            int[] point = { offset_ + m[0]*cellSize_, offset_ + m[1]*cellSize_ };
            canvas.drawLine(point[0] + nSpace, point[1] - nSpace, point[0] + nSpace + nSize, point[1] - nSpace, linePaint_);
            canvas.drawLine(point[0] + nSpace, point[1] - nSpace, point[0] + nSpace, point[1] - nSpace - nSize, linePaint_);
            canvas.drawLine(point[0] + nSpace, point[1] + nSpace, point[0] + nSpace + nSize, point[1] + nSpace, linePaint_);
            canvas.drawLine(point[0] + nSpace, point[1] + nSpace, point[0] + nSpace, point[1] + nSpace + nSize, linePaint_);
        }
    }
    
    private void drawAllPieces(Canvas canvas) {
        Piece piece;
        for (int i = 0; i < 16; i++) {
            piece = _redPieces[i];
            //piece.setCapture(false);
            //piece.setPosition( piece.getInitialPosition() );
            drawPiece(canvas, piece, false);
            
            piece = _blackPieces[i];
            //piece.setCapture(false);
            //piece.setPosition( piece.getInitialPosition() );
            drawPiece(canvas, piece, false);
        }
        
        /* !!! TO TEST HIGHLIGHT !!!
        for (int i = 0; i < 16; i++) {
            piece = _redPieces[i];
            Position initPos = piece.getInitialPosition();
            if (initPos.row == 7 && initPos.column == 1) {
                drawPiece(canvas, piece, true);
                break;
            }
        }
        */
        if (dragPiece_ != null) {
            drawPiece(canvas, dragPiece_, true);
        }
    }
    
    private void drawPiece(Canvas canvas, Piece piece, boolean selected) {
        final int imageRadius = pieceSize_ / 2;
        Bitmap bitmap = piece.getBitmap();
        
        Position viewPos = getViewPosition(piece.getPosition());
        //Log.d(TAG, "drawPiece(): viewPos = " + viewPos);
        
        final float left = offset_ - imageRadius + viewPos.column*cellSize_;
        final float top  = offset_ - imageRadius + viewPos.row*cellSize_;
        
        if (selected) {
            Log.d(TAG, "... highlight this piece.");
            canvas.drawCircle(
                    left + imageRadius,
                    top + imageRadius,
                    imageRadius + 6,
                    selectPaint_);
        }
        
        canvas.drawBitmap(bitmap, null, 
                new RectF( // left, top, right, bottom
                        left,
                        top,
                        left + pieceSize_,
                        top + pieceSize_),
                null);
    }
    
    private void drawHeaderRow(Canvas canvas, int offsetLeft, int offsetTop, boolean bDescending) {
        final int ROWS  = 10;
        int       top   = 0;
        final int left  = offsetLeft /*- 40*/;
        int       start = (bDescending ? 0 : ROWS - 1);

        for (int i = 0; i < ROWS; i++) {
            top = offsetTop + (i * cellSize_) /*- 6*/; 
            canvas.drawText("" + start, left, top, linePaint_);
            if (bDescending) { start++; }
            else             { start--; }
        }
    }
    
    private void drawHeaderColumn(Canvas canvas, int offsetLeft, int offsetTop, boolean bDescending) {
        final int COLS  = 9;
        final int top   = offsetTop - 35;
        int   left      = 0;
        int   start     = (bDescending ? COLS - 1 : 0);

        for (int i = 0; i < COLS; i++) {
            left = offsetLeft + (i * cellSize_) - 6;
            canvas.drawText(String.valueOf((char) ('a' + start)) /*"a" + start*/, left, top, linePaint_);
            if (bDescending) { start--; }
            else             { start++; }
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        // Listening for the down and up touch events
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownTouch = true;
                //Log.d(TAG, "ACTION_DOWN: [X = " + event.getX() + ", Y = " + event.getY() + "].");
                return true;

            case MotionEvent.ACTION_UP:
                if (mDownTouch) {
                    mDownTouch = false;
                    //Log.d(TAG, "ACTION_UP: [X = " + event.getX() + ", Y = " + event.getY() + "].");
                    handleTouchAtLocation(event.getX(), event.getY());
                    
                    performClick(); // Call this method to handle the response, and
                                    // thereby enable accessibility services to
                                    // perform this action for a user who cannot
                                    // click the touch screen.
                    return true;
                }
        }
        return false; // Return false for other touch events
    }

    /**
     * Handles a touch event at a given location.
     */
    private void handleTouchAtLocation(float eventX, float eventY) {
        Log.v(TAG, "handleTouchAtLocation(X=" + eventX + ", Y=" + eventY + ")");
        
        // Convert the screen position (X px, Y px) => the piece position (row, column).
        Position hitPosition = new Position();
        hitPosition.row = Math.round((eventY - offset_) / cellSize_);
        hitPosition.column = Math.round((eventX - offset_) / cellSize_);
        Position viewPos = getViewPosition(hitPosition);
        Log.d(TAG, "... Hit position = " + hitPosition + ", View-position = " + viewPos);
        
        //PieceInfo pieceInfo = referee_.findPieceAtPosition(viewPos);
        //if (pieceInfo == null) {
        //    Log.i(TAG, "... No piece is found at " + viewPos + ". Do nothing.");
        //    return;
        //}
        
        // CASE 1: No piece selected yet?
        if (dragStartPos_ == null) {
            Piece foundPiece = getPieceAtViewPosition(viewPos);
            if (foundPiece == null) {
                Log.i(TAG, "... No piece is found at " + viewPos + ". Do nothing.");
                return;
            }
            
            dragStartPos_ = viewPos;
            dragPiece_ = foundPiece;
        }
        // CASE 2: A piece has been selected already.
        else if ( ! Position.equals(dragStartPos_, viewPos) ) { // different location?
            Referee.MoveResult moveResult =
                    referee_.validateAndRecordMove(dragStartPos_, viewPos);
            if (moveResult.valid) {
                dragPiece_.setPosition(viewPos);
                dragStartPos_ = null;
            } else { // Move is not valid?
                Log.i(TAG, "... The move is not valid!");
                dragStartPos_ = null;
                dragPiece_ = null;  // Clear this "in-progress" move.
            }
        }
        
        // NOTE: We may want to find a better to update the board!
        //       The current method using invalidate() which will redraw the entire view.
        this.invalidate();
    }
    
    /**
     * Finds a piece at a given location.
     * 
     * @return the piece if found. Otherwise, return null.
     */
    private Piece getPieceAtViewPosition(Position position) {
        for (Piece piece : _redPieces) {
            if (Position.equals(piece.getPosition(), position)) {
                return piece;
            }
        }
        
        for (Piece piece : _blackPieces) {
            if (Position.equals(piece.getPosition(), position)) {
                return piece;
            }
        }
        
        return null;
    }
    
    @Override
    public boolean performClick() {
        // Calls the super implementation, which generates an AccessibilityEvent
        // and calls the onClick() listener on the view, if any
        super.performClick();

        // Handle the action for the custom click here
        Log.d(TAG, "performClick(): Do nothing.");
        
        return true;
    }
    
    /**
     * Reference: From Android 's "Getting Started"
     *    Title: Loading Large Bitmaps Efficiently
     * 
     *   http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     */
    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
    
    /**
     * Reference: From Android 's "Getting Started"
     *    Title: Loading Large Bitmaps Efficiently
     * 
     *   http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
            int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }
}
