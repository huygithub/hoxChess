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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.playxiangqi.hoxchess.Enums.TableType;
import com.playxiangqi.hoxchess.Piece.Move;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

public class BoardView extends ImageView {

    private static final String TAG = "BoardView";
    
    private TableType tableType_ = TableType.TABLE_TYPE_LOCAL;
    
    private static final int offset_ = 50; // in pixels
    private int cellSize_;
    private int pieceSize_ = 64;  // (in pixels) Note: It should be an even number.
    
    private boolean isBlackOnTop_ = true; // Normal view. Black player is at the top position.
    
    private Paint linePaint_;
    private Paint selectPaint_;
    private Paint recentPaint_;
    private Paint noticePaint_;
    
    private Piece[] _redPieces = new Piece[16];
    private Piece[] _blackPieces = new Piece[16];
    
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
	
	private AIEngine aiEngine_ = new AIEngine();
	
    /**
     * History index.
     * NOTE: Do not change the constants 'values below.
     */
    private static final int HISTORY_INDEX_END = -2;
    private static final int HISTORY_INDEX_BEGIN = -1;
    
	private List<Move> historyMoves_ = new ArrayList<Move>(); // All (past) Moves made so far.
	private int historyIndex_ = HISTORY_INDEX_END; // Which Move the user is reviewing.
	
	private List<Piece> captureStack_ = new ArrayList<Piece>(); // A stack of captured pieces.
	
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
        
        nativeCreateReferee();
        
        Log.d(TAG, " ... AI 's info = [" + aiEngine_.getInfo() + "]");
        aiEngine_.initGame();
        
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
    
    private Position getViewPosition(Position pos) {
        return ( isBlackOnTop_ // normal view?
                ? new Position( pos.row, pos.column )
                : new Position( Math.abs(pos.row - 9), Math.abs(pos.column - 8) ) );
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
    	super.onDraw(canvas);
    	Log.d(TAG, "onDraw(): ENTER.");

    	drawBoard(canvas, Color.BLACK, Color.WHITE);
    	drawAllPieces(canvas);
    
    	if (isBoardInReviewMode()) {
    	    drawReplayStatus(canvas);
    	} else if (!isGameInProgress()) {
            onGameOver(canvas);
        }
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
        final boolean bDescending = (! isBlackOnTop_);
        final int imageRadius = (int) (pieceSize_/2);
        drawHeaderRow(canvas, offset_ - imageRadius - 10, offset_, true /*bDescending*/);
        drawHeaderRow(canvas, offset_ + cellSize_*8 + imageRadius, offset_, true /*bDescending*/);
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
            if (!piece.isCaptured()) { // still alive?
                drawPiece(canvas, piece, PieceDrawMode.PIECE_DRAW_MODE_NORMAL);
            }
            
            piece = _blackPieces[i];
            if (!piece.isCaptured()) { // still alive?
                drawPiece(canvas, piece, PieceDrawMode.PIECE_DRAW_MODE_NORMAL);
            }
        }
        
        if (selectedPiece_ != null) {
            drawPiece(canvas, selectedPiece_, PieceDrawMode.PIECE_DRAW_MODE_SELECTED);
        } else if (recentPiece_ != null) {
            drawPiece(canvas, recentPiece_, PieceDrawMode.PIECE_DRAW_MODE_RECENT);
        }
    }
    
    private void drawPiece(Canvas canvas, Piece piece, PieceDrawMode drawMode) {
        final int imageRadius = pieceSize_ / 2;
        Bitmap bitmap = piece.getBitmap();
        
        Position viewPos = getViewPosition(piece.getPosition());
        
        final float left = offset_ - imageRadius + viewPos.column*cellSize_;
        final float top  = offset_ - imageRadius + viewPos.row*cellSize_;
        
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
            Log.d(TAG, "... highlight this piece.");
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
            //canvas.drawText(String.valueOf((char) ('a' + start)), left, top, linePaint_);
            canvas.drawText(String.valueOf((char) ('0' + start)), left, top, linePaint_);
            if (bDescending) { start--; }
            else             { start++; }
        }
    }
    
    private void drawReplayStatus(Canvas canvas) {
        canvas.drawText(
                HoxApp.getApp().getString(R.string.replay_text,
                        historyIndex_ + 1, historyMoves_.size()),
                offset_ + cellSize_*2.5f,
                offset_ + cellSize_*4.7f,
                noticePaint_);
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
                    if (isGameInProgress() && !isBoardInReviewMode()) {
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

    /**
     * Handles a touch event at a given location.
     */
    private void handleTouchAtLocation(float eventX, float eventY) {
        Log.v(TAG, "A touch occurred at location(X=" + eventX + ", Y=" + eventY + ")");
        
        // Convert the screen position (X px, Y px) => the piece position (row, column).
        Position hitPosition = new Position();
        hitPosition.row = Math.round((eventY - offset_) / cellSize_);
        hitPosition.column = Math.round((eventX - offset_) / cellSize_);
        Position viewPos = getViewPosition(hitPosition);
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
           final int status = nativeValidateMove(selectedPosition_.row, selectedPosition_.column,
                                                 viewPos.row, viewPos.column);
            Log.d(TAG, "... (native referee) move-validation returned status = " + status);
            
            if (status == hoxGAME_STATUS_UNKNOWN) { // Move is not valid?
                Log.i(TAG, "... The move is not valid!");
                selectedPosition_ = null;
                selectedPiece_ = null;  // Clear this "in-progress" move.
            }
            else {
                Piece capture = tryCapturePieceAtPostion(viewPos);
                
                selectedPiece_.setPosition(viewPos);
                Position fromPos = selectedPosition_; // Save before resetting it.
                selectedPosition_ = null;
                recentPiece_ = selectedPiece_;
                onLocalMoveMade(fromPos, viewPos, capture, status);
                selectedPiece_ = null;
            }
        }
        
        // NOTE: We may want to find a better to update the board!
        //       The current method using invalidate() which will redraw the entire view.
        this.invalidate();
    }
    
    private void onLocalMoveMade(Position fromPos, Position toPos, Piece capture, int gameStatus) {
        Log.d(TAG, " on Local move = " + fromPos + " => " + toPos);
        
        addMoveToHistory(fromPos, toPos, capture);
        
        didMoveOccur(fromPos, toPos, capture, gameStatus);
        
        if (!isGameInProgress()) {
            Log.w(TAG, "The game has ended. Do nothing.");
            return;
        }
        
        if (tableType_ == TableType.TABLE_TYPE_LOCAL) {
            aiEngine_.onHumanMove(fromPos.row, fromPos.column, toPos.row, toPos.column);
            
            Log.d(TAG, "Ask AI (MaxQi) to generate a new move...");
            new Thread(new Runnable() {
                public void run() {
                    String aiMove = aiEngine_.generateMove();
                    Log.d(TAG, "... AI returned this move [" + aiMove + "].");
                    messageHandler_.sendMessage(
                            messageHandler_.obtainMessage(MSG_AI_MOVE_READY, aiMove) );
                }
            }).start();
        
        } else if (tableType_ == TableType.TABLE_TYPE_NETWORK) {
            Log.d(TAG, "Send the move over the network...");
            HoxApp.getApp().handleLocalMove(fromPos, toPos);
        }
    }
    
    public void onAIMoveMade(Position fromPos, Position toPos) {
        Log.d(TAG, " on AI move = " + fromPos + " => " + toPos);
        
        final int status = nativeValidateMove(fromPos.row, fromPos.column,
                                              toPos.row, toPos.column);
        Log.d(TAG, "... (native referee) move-validation returned status = " + status);
        
        if (status == hoxGAME_STATUS_UNKNOWN) { // Move is not valid?
            Log.e(TAG, " This AI move =" + fromPos + " => " + toPos + " is NOT valid. Do nothing.");
            return;
        }
        
        // Do not update the Pieces on Board if we are in the review mode.
        if (isBoardInReviewMode()) {
            addMoveToHistory(fromPos, toPos, null /* capture: we don't know yet */);
            return;
        }
        
        Piece capture = tryCapturePieceAtPostion(toPos);
        addMoveToHistory(fromPos, toPos, capture);
        
        Piece fromPiece = getPieceAtViewPosition(fromPos);
        if (fromPiece == null) {
            Log.e(TAG, "... No 'from' piece is found at " + fromPos);
            return;
        }
        
        fromPiece.setPosition(toPos);
        recentPiece_ = fromPiece;
        
        didMoveOccur(fromPos, toPos, capture, status);
    }
    
    /**
     * Try to capture a piece at a given location.
     * 
     * @return the captured piece if any. Otherwise, return null.
     */
    private Piece tryCapturePieceAtPostion(Position position) {
        Piece foundPiece = getPieceAtViewPosition(position);
        if (foundPiece == null) {
            Log.d(TAG, "... No piece is (to be captured) found at " + position);
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
        for (Piece piece : _redPieces) {
            if (piece.isCaptured() == false && Position.equals(piece.getPosition(), position)) {
                return piece;
            }
        }
        
        for (Piece piece : _blackPieces) {
            if (piece.isCaptured() == false && Position.equals(piece.getPosition(), position)) {
                return piece;
            }
        }
        
        return null;
    }
    
    private void didMoveOccur(Position fromPos, Position toPos, Piece capture, int status) {
        Log.d(TAG, "A move just occurred. game 's status = " + status);
        
        gameStatus_ = status;
        
        if (status == hoxGAME_STATUS_RED_WIN) {
            Log.i(TAG, "The game is OVER. RED won.");
        }
        else if (status == hoxGAME_STATUS_BLACK_WIN) {
            Log.i(TAG, "The game is OVER. BLACK won.");
        }
        
        if (capture != null) {
            captureStack_.add(capture);
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
                "Game Over",
                offset_ + cellSize_*2.5f,
                offset_ + cellSize_*4.7f,
                noticePaint_);
    }
    
    boolean isGameInProgress() {
        return (   gameStatus_ == hoxGAME_STATUS_READY 
                || gameStatus_ == hoxGAME_STATUS_IN_PROGRESS);
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

    public void onNewTableActionClicked() {
        Log.d(TAG, "The 'New Table' action clicked...");
        resetBoard();
    }

    public void reverseView() {
        Log.d(TAG, "The 'Reverse View' action clicked...");
        isBlackOnTop_ = !isBlackOnTop_;
        this.invalidate(); // Request to redraw the board.
    }
    
    public void resetBoard() {
        Log.d(TAG, "Reset board to the initial state...");
        
        nativeResetGame(); // ask the referee to reset the game.
        aiEngine_.initGame();
        
        // Reset the pieces.
        Piece piece;
        for (int i = 0; i < 16; i++) {
            piece = _redPieces[i];
            piece.setCapture(false);
            piece.setPosition(piece.getInitialPosition());
            
            piece = _blackPieces[i];
            piece.setCapture(false);
            piece.setPosition(piece.getInitialPosition());
        }
        
        // Reset other internal variables
        selectedPiece_ = null;
        selectedPosition_ = null;
        recentPiece_ = null;
        gameStatus_ = hoxGAME_STATUS_READY;
        
        historyMoves_.clear();
        historyIndex_ = HISTORY_INDEX_END;
        captureStack_.clear();
        
        this.invalidate(); // Request to redraw the board.
    }
    
    public void onGameEnded(Enums.GameStatus gameStatus) {
        switch (gameStatus) {
            case GAME_STATUS_BLACK_WIN: gameStatus_ = hoxGAME_STATUS_BLACK_WIN; break;
            case GAME_STATUS_RED_WIN: gameStatus_ = hoxGAME_STATUS_RED_WIN; break;
            case GAME_STATUS_DRAWN: gameStatus_ = hoxGAME_STATUS_DRAWN; break;
            default:
                Log.w(TAG, "Unsupported game status = " + gameStatus);
                // Do nothing.
        }
        this.invalidate(); // Request to redraw the board.
    }
    
    public void onAILevelChanged(int newLevel) {
        Log.d(TAG, "On AI Level changed. newLevel = " + newLevel);
        aiEngine_.setDifficultyLevel(newLevel);
    }
    
    public void setTableType(TableType tableType) {
        tableType_ = tableType;
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
        Log.d(TAG, "on replay-PREVIOUS...");
        
        if (historyMoves_.size() == 0) {
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
        Log.d(TAG, "on replay-NEXT...");
        
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
    
    /**
     * A message handler to handle UI related tasks.
     */
    private static final int MSG_AI_MOVE_READY = 1;
    private Handler messageHandler_ = new MessageHandler(this);
    static class MessageHandler extends Handler {
        private final WeakReference<BoardView> boardView_;
        
        MessageHandler(BoardView boardView) {
            boardView_ = new WeakReference<BoardView>(boardView);
        }
        
        @Override
        public void handleMessage(Message msg){
            switch (msg.what) {
            case MSG_AI_MOVE_READY:
            {
                String aiMove = (String) msg.obj;
                Log.d(TAG, "(MessageHandler) AI returned this move [" + aiMove + "].");
                int row1 = aiMove.charAt(0) - '0';
                int col1 = aiMove.charAt(1) - '0';
                int row2 = aiMove.charAt(2) - '0';
                int col2 = aiMove.charAt(3) - '0';
                Log.i(TAG, "... AI 's move [ " + row1 + ", " + col1 + " => " + row2 + ", " + col2 + "]");
                BoardView boardView = boardView_.get();
                if (boardView != null) {
                    boardView.onAIMoveMade(new Position(row1, col1), new Position(row2, col2));
                    boardView.invalidate();
                }
                break;
            }
                
            default:
                break;
            }
        }
    };
    
    // ****************************** Native code **********************************
    // TODO: Need to fix for invalid moves when "king-facing-king"!!!
    public native int nativeCreateReferee();
    public native int nativeResetGame();
    public native int nativeGetNextColor();
    public native int nativeValidateMove(int row1, int col1, int row2, int col2);
    
    // The native referee 's game-status.
    // DO NOT CHANGE the constants' values.
    private final static int hoxGAME_STATUS_UNKNOWN = -1;
    private final static int hoxGAME_STATUS_OPEN = 0;        // Open but not enough Player.
    private final static int hoxGAME_STATUS_READY = 1;       // Enough (2) players, waiting for 1st Move.
    private final static int hoxGAME_STATUS_IN_PROGRESS = 2; // At least 1 Move has been made.
    private final static int hoxGAME_STATUS_RED_WIN = 3;     // Game Over: Red won.
    private final static int hoxGAME_STATUS_BLACK_WIN = 4;   // Game Over: Black won.
    private final static int hoxGAME_STATUS_DRAWN = 5;       // Game Over: Drawn.
    
    private int gameStatus_ = hoxGAME_STATUS_READY;
    
    static {
        System.loadLibrary("Referee");
    }
    // *****************************************************************************
}
