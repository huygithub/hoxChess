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
import java.util.List;

import com.playxiangqi.hoxchess.Piece.Move;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import junit.framework.Assert;

public class BoardView extends ImageView
        implements ValueAnimator.AnimatorUpdateListener {

    private static final String TAG = "BoardView";
    
    private static final int offset_ = 20; // of row/column labels in pixels

    // NOTE After many tries of using addOnPreDrawListener() and addOnGlobalLayoutListener,
    //     I still end up relying onDraw() => drawBoard() to adjust the two dimensions.
    private int finalWidth_ = 0;
    private int finalHeight_ = 0;

    // The sizes of Cell and Piece will be adjusted at runtime based on the board 's dimension.
    private int cellSize_;
    private int pieceSize_ = 64;  // (in pixels) Note: It should be an even number.
    private int startP_; // the offset at which we draw vertical/horizontal lines (in pixels).

    private boolean isBlackOnTop_ = true; // Normal view. Black player is at the top position.
    
    private int gameStatus_ = Referee.hoxGAME_STATUS_READY;

    private boolean downTouch_ = false;  // A touch is being in the DOWN position.

    private Paint linePaint_;
    private Paint selectPaint_;
    private Paint recentPaint_;
    private Paint noticePaint_;

    private static final long DURATION_OF_ANIMATION = 500; // duration of animation in milliseconds.
    private ObjectAnimator animator_;

    private Piece[] redPieces_ = new Piece[16];
    private Piece[] blackPieces_ = new Piece[16];
    
    private enum PieceDrawMode {
        PIECE_DRAW_MODE_NORMAL,
        PIECE_DRAW_MODE_SELECTED,  // The piece that is being selected as a "from" piece.
        PIECE_DRAW_MODE_RECENT     // The piece that recently moved (or, the "to" piece).
    }

    /**
     * Constants for the different Move Modes.
     * NOTE: Do not change the constants 'values below.
     */
    //private static final int MOVE_MODE_CLICK_N_CLICK = 0;
    //private static final int MOVE_MODE_DRAG_N_DROP = 1;
    // !!! Only this mode is supported !!! private final int moveMode_ = MOVE_MODE_CLICK_N_CLICK;
    
    private Piece selectedPiece_ = null;
    private Position selectedPosition_ = null;
    
    private Piece recentPiece_ = null;

    /**
     * History index.
     * NOTE: Do not change the constants 'values below.
     */
    private static final int HISTORY_INDEX_END = -2;
    private static final int HISTORY_INDEX_BEGIN = -1;
    
    private List<Move> historyMoves_ = new ArrayList<Move>(); // All (past) Moves made so far.
    private int historyIndex_ = HISTORY_INDEX_END; // Which Move the user is reviewing.
    
    private List<Piece> captureStack_ = new ArrayList<Piece>(); // A stack of captured pieces.

    private BoardEventListener boardEventListener_;

    /**
     * The Container of this Board must implement this interface.
     */
    public interface BoardEventListener {
        int validateMove(Position fromPos, Position toPos);
        void onLocalMove(Position fromPos, Position toPos, Enums.GameStatus gameStatus);
        boolean isMyTurn();
    }

    public void setBoardEventListener(BoardEventListener listener) {
        boardEventListener_ = listener;
    }

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
        Log.d(TAG, "init() ...");
        setBackgroundColor(Color.BLACK);
    	
        linePaint_ = new Paint();
        linePaint_.setColor(Color.LTGRAY);
        linePaint_.setStrokeWidth(1.0f);
        linePaint_.setTextSize(18.0f);

        selectPaint_ = new Paint();
        selectPaint_.setColor(Color.CYAN);

        recentPaint_ = new Paint();
        recentPaint_.setColor(Color.CYAN);

        noticePaint_ = new Paint();
        noticePaint_.setColor(Color.RED);
        noticePaint_.setTextSize(40.0f);
        
        createPieces();

        // Note: We can also use addOnGlobalLayoutListener instead of addOnPreDrawListener()
        //      but I have not seen any difference between using one vs. another.
        final ViewTreeObserver vto = getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                int finalHeight = getMeasuredHeight();
                int finalWidth = getMeasuredWidth();

                adjustBoardParameters(finalWidth, finalHeight);

                final ViewTreeObserver vto = getViewTreeObserver();
                vto.removeOnPreDrawListener(this);
                return true;
            }
        });
    }
    
    public int getPieceSize() { return pieceSize_; }
    
    private void createPieces() {        
        String color = "Red";
        redPieces_[0]  = new Piece("chariot",  color, 9, 0, this);
        redPieces_[1]  = new Piece("horse",    color, 9, 1, this);
        redPieces_[2]  = new Piece("elephant", color, 9, 2, this);
        redPieces_[3]  = new Piece("advisor",  color, 9, 3, this);
        redPieces_[4]  = new Piece("king",     color, 9, 4, this);
        redPieces_[5]  = new Piece("advisor",  color, 9, 5, this);
        redPieces_[6]  = new Piece("elephant", color, 9, 6, this);
        redPieces_[7]  = new Piece("horse",    color, 9, 7, this);
        redPieces_[8]  = new Piece("chariot",  color, 9, 8, this);
        redPieces_[9]  = new Piece("cannon",   color, 7, 1, this);
        redPieces_[10] = new Piece("cannon",   color, 7, 7, this);
        
        for (int pawn = 0; pawn < 5; pawn++) {
            redPieces_[11 + pawn] = new Piece("pawn", color, 6, 2*pawn, this);
        }
        
        color = "Black";
        blackPieces_[0]  = new Piece("chariot",  color, 0, 0, this);
        blackPieces_[1]  = new Piece("horse",    color, 0, 1, this);
        blackPieces_[2]  = new Piece("elephant", color, 0, 2, this);
        blackPieces_[3]  = new Piece("advisor",  color, 0, 3, this);
        blackPieces_[4]  = new Piece("king",     color, 0, 4, this);
        blackPieces_[5]  = new Piece("advisor",  color, 0, 5, this);
        blackPieces_[6]  = new Piece("elephant", color, 0, 6, this);
        blackPieces_[7]  = new Piece("horse",    color, 0, 7, this);
        blackPieces_[8]  = new Piece("chariot",  color, 0, 8, this);
        blackPieces_[9]  = new Piece("cannon",   color, 2, 1, this);
        blackPieces_[10] = new Piece("cannon",   color, 2, 7, this);

        for (int pawn = 0; pawn < 5; pawn++) {
            blackPieces_[11 + pawn] = new Piece("pawn", color, 3, 2*pawn, this);
        }
    }
    
    private Position getViewPosition(Position pos) {
        return ( isBlackOnTop_ // normal view?
                ? new Position( pos.row, pos.column )
                : new Position( Math.abs(pos.row - 9), Math.abs(pos.column - 8) ) );
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
    	super.onDraw(canvas);
    	drawBoard(canvas, Color.BLACK, Color.WHITE);
    	drawAllPieces(canvas);
    
    	if (isBoardInReviewMode()) {
    	    drawReplayStatus(canvas);
    	} else if (!isGameInProgress()) {
            onGameOver(canvas);
        }
    }

    private void adjustBoardParameters(int finalWidth, final int finalHeight) {
        /* Reference:
         *   http://stackoverflow.com/questions/2795833/check-orientation-on-android-phone
         */
        int configuration = getContext().getResources().getConfiguration().orientation;
        Log.i(TAG, "adjustBoardParameters():WxH = " + finalWidth + " x " + finalHeight
                + ", " + Utils.orientationToString(configuration));

        final int cellSizeByHeight = (finalHeight - 2 * offset_) / 10;
        final int cellSizeByWidth = (finalWidth - 2 * offset_) / 9;
        final int proposedCellSize = Math.min(cellSizeByHeight, cellSizeByWidth);

//        if (configuration == Configuration.ORIENTATION_LANDSCAPE) {
//            //finalWidth = (cellSizeByHeight * 9) + 2 * offset_;
//            //Log.i(TAG, "adjustBoardParameters(): (LANDSCAPE mode) WxH Adjusted Width => " + finalWidth);
//
//            //getLayoutParams().width = finalWidth;
//            //requestLayout();
//        }
//        else if (finalWidth > finalHeight) {
//            // NOTE: This is a special case that I found: in Google Nexus 9,
//            //       Width is greater Height
//            //       e.g., from logcat: WxH = 1516 x 1462, ORIENTATION_PORTRAIT
//            //
//            // TODO: We should look at how to this entire function again!
//            //
//            final int cellSizeByHeight = (finalHeight - 2 * offset_) / 10;
//            finalWidth = (cellSizeByHeight * 9) + 2 * offset_;
//            Log.i(TAG, "adjustBoardParameters(): (by Height) WxH Adjusted Width => " + finalWidth);
//            getLayoutParams().width = finalWidth;
//            requestLayout();
//        }

        // Save the dimensions so that we can later use in onDraw().
        finalWidth_ = finalWidth;
        finalHeight_ = finalHeight;

        //final int boardWidth = Math.min(finalWidth, finalHeight);
        cellSize_ = proposedCellSize; //(boardWidth - 2*offset_)/9;
        pieceSize_ = (int) (cellSize_ * 0.8f);
        if ((pieceSize_ % 2) == 1) { --pieceSize_; } // Make it an event number.
        startP_ = offset_ + cellSize_ / 2;
        Log.d(TAG, "adjustBoardParameters(): cellSize_:" + cellSize_ + ", pieceSize_:" + pieceSize_
                + ", startP_:" + startP_);
    }

    private void drawBoard(Canvas canvas, int bgColor_UNUSED, int lineColor_UNUSED) {
        // The empty board
        final int boardW = getMeasuredWidth();
        final int boardH = getMeasuredHeight();
        Log.v(TAG, "drawBoard(): WxH = " + boardW + "x" + boardH + ". blackOnTop = " + isBlackOnTop_);

        // NOTE: Because we still see this function reports different values for
        //       the width and height, we have to manually adjust them here (again)!
        if (boardW != finalWidth_ || boardH != finalHeight_) {
            Log.d(TAG, "drawBoard(): WxH is different. Adjust parameters again... ");
            adjustBoardParameters(boardW, boardH);
        }

        for (int i = 0; i < 10; i++) { // Horizontal lines
            canvas.drawLine(startP_, startP_+i*cellSize_, startP_+8*cellSize_, startP_+i*cellSize_, linePaint_);
        }

        for (int i = 0; i < 9; i++) { // Vertical lines
            if (i == 0 || i == 8) {
                canvas.drawLine(startP_ + i*cellSize_, startP_, startP_ + i*cellSize_, startP_ + cellSize_*9, linePaint_);
            } else {
                canvas.drawLine(startP_ + i*cellSize_, startP_, startP_ + i*cellSize_, startP_ + cellSize_*4, linePaint_);
                canvas.drawLine(startP_ + i*cellSize_, startP_ + 5*cellSize_, startP_ + i*cellSize_, startP_ + 5*cellSize_ + cellSize_*4, linePaint_);
            }
        }
        
        // Diagonal lines to form the Fort (or the Palace).
        canvas.drawLine(startP_ + 3*cellSize_, startP_, startP_ + 3*cellSize_ + 2*cellSize_, startP_ + 2*cellSize_, linePaint_);
        canvas.drawLine(startP_ + 5*cellSize_, startP_, startP_ + 5*cellSize_ - 2*cellSize_, startP_ + cellSize_*2, linePaint_);
        canvas.drawLine(startP_ + 3*cellSize_, startP_ + 7*cellSize_, startP_ + 3*cellSize_ + 2*cellSize_, startP_ + 7*cellSize_ + 2*cellSize_, linePaint_);
        canvas.drawLine(startP_ + 5*cellSize_, startP_ + 7*cellSize_, startP_ + 5*cellSize_ - 2*cellSize_, startP_ + 7*cellSize_ + 2*cellSize_, linePaint_);
        
        // The labels (row: 0-9 and column: 0-8).
        final boolean bDescending = (! isBlackOnTop_);
        drawHeaderRow(canvas, offset_ /*- imageRadius - 10*/, startP_, true /*bDescending*/);
        drawHeaderRow(canvas, offset_ + cellSize_*9, startP_, true /*bDescending*/);
        drawHeaderColumn(canvas, startP_, offset_, bDescending);
        drawHeaderColumn(canvas, startP_, offset_ + cellSize_*10 /*+ 20*/, bDescending);
        
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
            int[] point = new int[] { startP_ + m[0]*cellSize_, startP_ + m[1]*cellSize_ };
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
            int[] point = { startP_ + m[0]*cellSize_, startP_ + m[1]*cellSize_ };
            canvas.drawLine(point[0] + nSpace, point[1] - nSpace, point[0] + nSpace + nSize, point[1] - nSpace, linePaint_);
            canvas.drawLine(point[0] + nSpace, point[1] - nSpace, point[0] + nSpace, point[1] - nSpace - nSize, linePaint_);
            canvas.drawLine(point[0] + nSpace, point[1] + nSpace, point[0] + nSpace + nSize, point[1] + nSpace, linePaint_);
            canvas.drawLine(point[0] + nSpace, point[1] + nSpace, point[0] + nSpace, point[1] + nSpace + nSize, linePaint_);
        }
    }
    
    private void drawAllPieces(Canvas canvas) {
        Piece piece;
        for (int i = 0; i < 16; i++) {
            piece = redPieces_[i];
            if (!piece.isCaptured()) { // still alive?
                drawPiece(canvas, piece, PieceDrawMode.PIECE_DRAW_MODE_NORMAL);
            }
            
            piece = blackPieces_[i];
            if (!piece.isCaptured()) { // still alive?
                drawPiece(canvas, piece, PieceDrawMode.PIECE_DRAW_MODE_NORMAL);
            }
        }
        
        if (selectedPiece_ != null && !selectedPiece_.isAnimated()) {
            drawPiece(canvas, selectedPiece_, PieceDrawMode.PIECE_DRAW_MODE_SELECTED);
        } else if (recentPiece_ != null) {
            drawPiece(canvas, recentPiece_, PieceDrawMode.PIECE_DRAW_MODE_RECENT);
        }
    }

    private PointF convertPositionToPoint(Position position) {
        final int imageRadius = pieceSize_ / 2;

        Position viewPos = getViewPosition(position);

        final float left = startP_ - imageRadius + viewPos.column*cellSize_;
        final float top  = startP_ - imageRadius + viewPos.row*cellSize_;
        return new PointF(left, top);
    }

    private void drawPieceAtPoint(Canvas canvas, Piece piece, PointF point) {
        Bitmap bitmap = piece.getBitmap();

        final float left = point.x;
        final float top  = point.y;

        canvas.drawBitmap(bitmap, null,
                new RectF( // left, top, right, bottom
                        left,
                        top,
                        left + pieceSize_,
                        top + pieceSize_),
                null);
    }

    private void drawPiece(Canvas canvas, Piece piece, PieceDrawMode drawMode) {
        // Special case for a piece that is being animated.
        if (piece.isAnimated()) {
            final PointF point = piece.getPointF();
            drawPieceAtPoint(canvas, piece, point);
            return;
        }

        final int imageRadius = pieceSize_ / 2;
        Bitmap bitmap = piece.getBitmap();
        
        Position viewPos = getViewPosition(piece.getPosition());
        final float left = startP_ - imageRadius + viewPos.column*cellSize_;
        final float top  = startP_ - imageRadius + viewPos.row*cellSize_;
        
        if (drawMode == PieceDrawMode.PIECE_DRAW_MODE_SELECTED) {
            Log.d(TAG, "... select this piece.");
            canvas.drawRect( // left, top, right, bottom, paint
                    left - 3,
                    top - 3,
                    left + pieceSize_ + 3,
                    top + pieceSize_ + 3,
                    selectPaint_);
        }
        else if (drawMode == PieceDrawMode.PIECE_DRAW_MODE_RECENT) {
            Log.v(TAG, "... highlight this piece.");
            canvas.drawCircle(
                    left + imageRadius,
                    top + imageRadius,
                    imageRadius + 6,
                    recentPaint_);
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
        int       top;
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
        /*
         * NOTE: I still need to learn the way Android draws text, which direction, etc.
         *   http://www.slideshare.net/rtc1/intro-todrawingtextandroid
         *   http://stackoverflow.com/questions/10606410/android-canvas-drawtext-y-position-of-text
         *
         *       final String text = "0";
         *       Rect textBounds = new Rect();
         *       linePaint_.getTextBounds(text, 0, text.length(), textBounds);
         *       Log.d(TAG, "textBounds: " + textBounds);
         *       canvas.drawText(text, 0, textBounds.height(), paint);
         *
         *  I will use HACK_TOP_OFFSET_IN_PIXELS for now.
         */
        final int HACK_TOP_OFFSET_IN_PIXELS = 5; // TODO: Hack the column labels (see above).

        final int COLS  = 9;
        final int top   = offsetTop + HACK_TOP_OFFSET_IN_PIXELS;
        int   left;
        int   start     = (bDescending ? COLS - 1 : 0);

        for (int i = 0; i < COLS; i++) {
            left = offsetLeft + (i * cellSize_) /*- 6*/;
            canvas.drawText(String.valueOf((char) ('0' + start)), left, top, linePaint_);
            if (bDescending) { start--; }
            else             { start++; }
        }
    }
    
    private void drawReplayStatus(Canvas canvas) {
        canvas.drawText(
                HoxApp.getApp().getString(R.string.replay_text,
                        historyIndex_ + 1, historyMoves_.size()),
                startP_ + cellSize_*2.5f,
                startP_ + cellSize_*4.7f,
                noticePaint_);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        // Listening for the down and up touch events
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downTouch_ = true;
                //Log.d(TAG, "ACTION_DOWN: [X = " + event.getX() + ", Y = " + event.getY() + "].");
                return true;

            case MotionEvent.ACTION_UP:
                if (downTouch_) {
                    downTouch_ = false;
                    if (isGameInProgress() && !isBoardInReviewMode() 
                            && (boardEventListener_ != null && boardEventListener_.isMyTurn())) {
                        handleTouchAtLocation(event.getX(), event.getY());
                    }
                    
                    performClick(); // Call this method to handle the response, and
                                    // thereby enable accessibility services to
                                    // perform this action for a user who cannot
                                    // click the touch screen.
                    return true;
                }
        }
        return false; // Return false for other touch events
    }

    // *******************************************************************************************
    public class PositionEvaluator implements TypeEvaluator {
        @Override
        public Object evaluate(float fraction, Object startValue, Object endValue) {
            final PointF fromPoint = (PointF) startValue;
            final PointF toPoint = (PointF) endValue;
            //Log.v(TAG, "evaluate: fraction: " + fraction + ", " + fromPoint + " => " + toPoint);

            float startX = fromPoint.x;
            float endX = toPoint.x;
            float finalX = startX + fraction * (endX - startX);

            float startY = fromPoint.y;
            float endY = toPoint.y;
            float finalY = startY + fraction * (endY - startY);

            final PointF finalPoint = new PointF(finalX, finalY);
            //Log.v(TAG, "evaluate: ... finalPoint = " + finalPoint);
            return finalPoint;
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        //Log.v(TAG, "onAnimationUpdate: animation.getAnimatedValue() = "
        //        + animation.getAnimatedValue()
        //        + ", repeatCount=" + animation.getRepeatCount());

        Piece piece = (Piece) animator_.getTarget();
        //Log.v(TAG, "onAnimationUpdate: ...piece.getPreviousPointF() = " + piece.getPreviousPointF()
        //        + ", pointF=" + piece.getPointF());

        final int padding = 5; // To make sure the area around the piece getting re-drawn.

        PointF previousPoint = piece.getPreviousPointF();
        int preLeft = (int) previousPoint.x;
        int preTop = (int) previousPoint.y;
        invalidate(// left, top, right, bottom
                preLeft - padding,
                preTop - padding,
                preLeft + pieceSize_ + 2*padding,
                preTop + pieceSize_ + 2*padding);

        PointF point = (PointF) animation.getAnimatedValue();
        int left = (int) point.x;
        int top = (int) point.y;
        invalidate(// left, top, right, bottom
                left - padding,
                top - padding,
                left + pieceSize_ + 2*padding,
                top + pieceSize_ + 2*padding);
    }
    // *******************************************************************************************

    /**
     * Handles a touch event at a given location.
     */
    private void handleTouchAtLocation(float eventX, float eventY) {
        Log.v(TAG, "A touch occurred at location(X=" + eventX + ", Y=" + eventY + ")");
        
        // Convert the screen position (X px, Y px) => the piece position (row, column).
        Position hitPosition = new Position();
        hitPosition.row = Math.round((eventY - startP_) / cellSize_);
        hitPosition.column = Math.round((eventX - startP_) / cellSize_);
        final Position viewPos = getViewPosition(hitPosition);
        Log.d(TAG, "... Hit position = " + hitPosition + ", View-position = " + viewPos);
        
        // CASE 1: No piece selected yet?
        if (selectedPosition_ == null) {
            Piece foundPiece = getPieceAtViewPosition(viewPos);
            if (foundPiece == null) {
                Log.i(TAG, "... No piece is found at " + viewPos + ". Do nothing.");
                return;
            }
            
            selectedPosition_ = viewPos;
            selectedPiece_ = foundPiece;
        }
        // CASE 2: A piece has been selected already.
        else if ( ! Position.equals(selectedPosition_, viewPos) ) { // different location?
            final int status = boardEventListener_ .validateMove(selectedPosition_, viewPos);
            Log.d(TAG, "... (native referee) move-validation returned status = " + status);
            
            if (status == Referee.hoxGAME_STATUS_UNKNOWN) { // Move is not valid?
                Log.i(TAG, "... The move is not valid!");
                selectedPosition_ = null;
                selectedPiece_ = null;  // Clear this "in-progress" move.
            }
            else {
                final Piece capture = getPieceAtViewPosition(viewPos);
                final Position fromPos = selectedPosition_; // Save before resetting it.
                selectedPosition_ = null;
                recentPiece_ = null;

                Animator.AnimatorListener listener = new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        selectedPiece_.setPosition(viewPos);
                        selectedPiece_.setIsAnimated(false);
                        recentPiece_ = selectedPiece_;
                        if (capture != null) {
                            capture.setCapture(true);
                        }
                        onLocalMoveMade(fromPos, viewPos, capture, status);
                        selectedPiece_ = null;
                        BoardView.this.invalidate();
                    }
                };

                movePieceToPositionWithAnimation(selectedPiece_, fromPos, viewPos, listener);
            }
        }
        
        // NOTE: We may want to find a better to update the board!
        //       The current method using invalidate() which will redraw the entire view.
        this.invalidate();
    }

    private void movePieceToPositionWithAnimation(final Piece piece, final Position fromPos,
                                                  final Position toPos,
                                                  Animator.AnimatorListener listener) {
        piece.setIsAnimated(true);

        final PointF fromPoint = convertPositionToPoint(fromPos);
        final PointF toPoint = convertPositionToPoint(toPos);

        animator_ = ObjectAnimator.ofObject(piece, "pointF",
                new PositionEvaluator(), fromPoint, toPoint);
        animator_.setDuration(DURATION_OF_ANIMATION);
        animator_.addUpdateListener(this);
        animator_.addListener(listener);
        animator_.start();
    }

    private void onLocalMoveMade(Position fromPos, Position toPos, Piece capture, int gameStatus) {
        Log.d(TAG, "On Local move = " + fromPos + " => " + toPos);
        
        addMoveToHistory(fromPos, toPos, capture);
        didMoveOccur(capture, gameStatus, true);
        if (boardEventListener_ != null) {
            Enums.GameStatus gameStatusEnum = Referee.gameStatusToEnum(gameStatus);
            boardEventListener_.onLocalMove(fromPos, toPos, gameStatusEnum);
        }
    }

    public void makeMove(final Position fromPos, final Position toPos,
                         final int gameStatus, boolean animated) {
        Log.d(TAG, "*** Make move = " + fromPos + " => " + toPos);

        // Do not update the Pieces on Board if we are in the review mode.
        if (isBoardInReviewMode()) {
            Log.d(TAG, "*** Make move: Board is in Review mode. ONLY add move to History!!!");
            addMoveToHistory(fromPos, toPos, null /* capture: we don't know yet */);
            return;
        }

        final Piece fromPiece = getPieceAtViewPosition(fromPos);
        Assert.assertNotNull("No 'from' piece is found at " + fromPos, fromPiece);

        if (animated) {
            Animator.AnimatorListener listener = new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    // NOTE: Board may have entered Review mode while the animation is in progress!
                    //      However, we will handle such a race condition in the onReplay_PREV()
                    //      by checking whether a MOVE animation is in progress...
                    //      See: onReplay_PREV
                    final Piece capture = tryCapturePieceAtPosition(toPos);
                    fromPiece.setPosition(toPos);
                    fromPiece.setIsAnimated(false);
                    recentPiece_ = fromPiece;
                    addMoveToHistory(fromPos, toPos, capture);
                    didMoveOccur(capture, gameStatus, true);
                    BoardView.this.invalidate();
                }
            };
            movePieceToPositionWithAnimation(fromPiece, fromPos, toPos, listener);

        } else {
            final Piece capture = tryCapturePieceAtPosition(toPos);
            fromPiece.setPosition(toPos);
            recentPiece_ = fromPiece;
            addMoveToHistory(fromPos, toPos, capture);
            didMoveOccur(capture, gameStatus, false);
        }
    }

    public void restoreMoveHistory(List<Piece.Move> historyMoves, int lastGameStatus) {
        for (Piece.Move move : historyMoves) {
            restoreMove(move.fromPosition, move.toPosition);
        }

        if (historyMoves.size() > 0) {
            gameStatus_ = lastGameStatus;
        }
    }

    private void restoreMove(Position fromPos, Position toPos) {
        Log.d(TAG, "Restore move = " + fromPos + " => " + toPos);
        
        Piece capture = tryCapturePieceAtPosition(toPos);
        addMoveToHistory(fromPos, toPos, capture);
        
        Piece fromPiece = getPieceAtViewPosition(fromPos);
        if (fromPiece == null) {
            Log.e(TAG, "... No 'from' piece is found at " + fromPos);
            return;
        }
        
        fromPiece.setPosition(toPos);
        recentPiece_ = fromPiece;
        
        if (capture != null) {
            captureStack_.add(capture);
        }
    }
    
    /**
     * Try to capture a piece at a given location.
     * 
     * @return the captured piece if any. Otherwise, return null.
     */
    private Piece tryCapturePieceAtPosition(Position position) {
        Piece foundPiece = getPieceAtViewPosition(position);
        if (foundPiece == null) {
            //Log.v(TAG, "... No piece is (to be captured) found at " + position);
            return null;
        }
        Log.d(TAG, "Capture a piece at " + position);
        foundPiece.setCapture(true);
        return foundPiece;
    }
    
    /**
     * Finds a piece at a given location.
     * 
     * @return the piece if found. Otherwise, return null.
     */
    private Piece getPieceAtViewPosition(Position position) {
        for (Piece piece : redPieces_) {
            if (!piece.isCaptured() && Position.equals(piece.getPosition(), position)) {
                return piece;
            }
        }
        
        for (Piece piece : blackPieces_) {
            if (!piece.isCaptured() && Position.equals(piece.getPosition(), position)) {
                return piece;
            }
        }
        
        return null;
    }
    
    private void didMoveOccur(Piece capture, int status, boolean playSound) {
        Log.d(TAG, "A move just occurred. game-status:" + status + ", playSound:" + playSound);
        
        gameStatus_ = status;
        
        if (status == Referee.hoxGAME_STATUS_RED_WIN) {
            Log.i(TAG, "The game is OVER. RED won.");
        }
        else if (status == Referee.hoxGAME_STATUS_BLACK_WIN) {
            Log.i(TAG, "The game is OVER. BLACK won.");
        }
        
        if (capture != null) {
            captureStack_.add(capture);
        }

        if (playSound) {
            SoundManager.getInstance().playSound(SoundManager.SOUND_MOVE);
        }
    }
    
    private void addMoveToHistory(Position fromPos, Position toPos, Piece capture) {
        Piece.Move move = new Piece.Move();
        move.fromPosition = fromPos.clone();
        move.toPosition = toPos.clone();
        move.isCaptured = (capture != null);
        historyMoves_.add(move);
    }
    
    private void onGameOver(Canvas canvas) {
        canvas.drawText(
                this.getContext().getString(R.string.game_over_text),
                startP_ + cellSize_*2.5f,
                startP_ + cellSize_*4.7f,
                noticePaint_);
    }
    
    private boolean isGameInProgress() {
        return (   gameStatus_ == Referee.hoxGAME_STATUS_READY 
                || gameStatus_ == Referee.hoxGAME_STATUS_IN_PROGRESS);
    }
    
    @Override
    public boolean performClick() {
        // Calls the super implementation, which generates an AccessibilityEvent
        // and calls the onClick() listener on the view, if any
        super.performClick();

        // Handle the action for the custom click here
        // ...
        return true;
    }

    public void reverseView() {
        Log.d(TAG, "reverseView...");
        isBlackOnTop_ = !isBlackOnTop_;
        this.invalidate(); // Request to redraw the board.
    }
    
    public void resetBoard() {
        Log.d(TAG, "Reset board to the initial state...");

        // It is essential that we end any Move animation now for the Move to take effect
        // immediately before resetting the table. Otherwise, the Move may occur after
        // the board 's initial state.
        if (animator_ != null && animator_.isRunning()) {
            Log.i(TAG, "Reset board: Move animation is running. End it now!");
            animator_.end();
        }
        
        // Reset the pieces.
        Piece piece;
        for (int i = 0; i < 16; i++) {
            piece = redPieces_[i];
            piece.setCapture(false);
            piece.setPosition(piece.getInitialPosition());
            
            piece = blackPieces_[i];
            piece.setCapture(false);
            piece.setPosition(piece.getInitialPosition());
        }
        
        // Reset other internal variables
        selectedPiece_ = null;
        selectedPosition_ = null;
        recentPiece_ = null;
        gameStatus_ = Referee.hoxGAME_STATUS_READY;
        
        historyMoves_.clear();
        historyIndex_ = HISTORY_INDEX_END;
        captureStack_.clear();

        this.invalidate(); // Request to redraw the board.
    }
    
    public void onGameEnded(Enums.GameStatus gameStatus) {
        switch (gameStatus) {
            case GAME_STATUS_BLACK_WIN: gameStatus_ = Referee.hoxGAME_STATUS_BLACK_WIN; break;
            case GAME_STATUS_RED_WIN: gameStatus_ = Referee.hoxGAME_STATUS_RED_WIN; break;
            case GAME_STATUS_DRAWN: gameStatus_ = Referee.hoxGAME_STATUS_DRAWN; break;
            default:
                Log.w(TAG, "Unsupported game status = " + gameStatus);
                // Do nothing.
        }
        this.invalidate(); // Request to redraw the board.
    }
    
    public int getMoveCount() {
        return historyMoves_.size();
    }
    
    private boolean isBoardInReviewMode() {
        return (historyIndex_ != HISTORY_INDEX_END);
    }

    public void onReplay_BEGIN() {
        Log.d(TAG, "on replay-BEGIN...");
        while ( onReplay_PREV(false) ) { }
        this.invalidate(); // Request to redraw the board.
    }
    
    /**
     * @return true if a replay move was made. false, otherwise.
     */
    public boolean onReplay_PREV(boolean redrawNow) {
        Log.d(TAG, "on replay-PREVIOUS: " + historyIndex_ + " / " + historyMoves_.size() + "...");
        
        if (historyMoves_.size() == 0) {
            return false;
        }

        if (animator_ != null && animator_.isRunning()) {
            Log.i(TAG, "on replay-PREVIOUS... Move animation is running. Do nothing.");
            return false;
        }

        if (historyIndex_ == HISTORY_INDEX_END) { // at the END mark?
            historyIndex_ = historyMoves_.size() - 1; // Get the latest move.
        }
        else if (historyIndex_ == HISTORY_INDEX_BEGIN) {
            return false;
        }
   
        Move move = historyMoves_.get(historyIndex_);
        
        Piece piece = getPieceAtViewPosition(move.toPosition);
        piece.setPosition(move.fromPosition);
        
        // Put back the captured piece, if any.
        if (move.isCaptured) {
            // The capture must be at the top of the Move-Stack.
            Log.d(TAG, "on replay-PREVIOUS... captureStack_.size() = " + captureStack_.size());
            Piece capture = captureStack_.remove(captureStack_.size()-1);
            capture.setCapture(false);
        }
        
        // Highlight the Piece (if any) of the "next-PREV" Move.
        --historyIndex_;
        if (historyIndex_ >= 0) {
            move = historyMoves_.get(historyIndex_);
            piece = getPieceAtViewPosition(move.toPosition);
            recentPiece_ = piece;
        } else {
            recentPiece_ = null;
        }
        
        if (redrawNow) {
            this.invalidate(); // Request to redraw the board.
        }
        return true;
    }
    
    /**
     * @return true if a replay move was made. false, otherwise.
     */
    public boolean onReplay_NEXT(boolean redrawNow) {
        Log.d(TAG, "on replay-NEXT: " + historyIndex_ + " / " + historyMoves_.size() + "...");
        
        if (historyMoves_.size() == 0) {
            return false;
        }

        if (historyIndex_ == HISTORY_INDEX_END) { // at the END mark?
            return false;
        }

        ++historyIndex_;
        
        Move move = historyMoves_.get(historyIndex_);
        
        if (historyIndex_ == historyMoves_.size() - 1) {
            historyIndex_ = HISTORY_INDEX_END;
        }
        
        // Move the piece from ORIGINAL --> NEW position.
        Piece piece = getPieceAtViewPosition(move.fromPosition);
        Assert.assertNotNull("No 'from' piece is found at " + move.fromPosition, piece);
        Piece capture = getPieceAtViewPosition(move.toPosition);
        if (capture != null) {
            capture.setCapture(true);
            captureStack_.add(capture);
            
            move.isCaptured = true; // NOTE: Update the flag
                                    // (in case the move arrived while we are in replay mode).
        }
        
        piece.setPosition(move.toPosition);
        recentPiece_ = piece;
        
        if (redrawNow) {
            this.invalidate(); // Request to redraw the board.
        }
        return true;
    }
    
    public void onReplay_END() {
        Log.d(TAG, "on replay-END...");
        while ( onReplay_NEXT(false) ) { }
        this.invalidate(); // Request to redraw the board.
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
