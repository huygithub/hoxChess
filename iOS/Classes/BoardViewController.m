/***************************************************************************
 *  Copyright 2010-2014 Huy Phan <huyphan@playxiangqi.com>                 *
 *                                                                         * 
 *  This file is part of HOXChess.                                         *
 *                                                                         *
 *  HOXChess is free software: you can redistribute it and/or modify       *
 *  it under the terms of the GNU General Public License as published by   *
 *  the Free Software Foundation, either version 3 of the License, or      *
 *  (at your option) any later version.                                    *
 *                                                                         *
 *  HOXChess is distributed in the hope that it will be useful,            *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 *  GNU General Public License for more details.                           *
 *                                                                         *
 *  You should have received a copy of the GNU General Public License      *
 *  along with HOXChess.  If not, see <http://www.gnu.org/licenses/>.      *
 ***************************************************************************/

#import "BoardViewController.h"
#import "QuartzUtils.h"
#import "Grid.h"
#import "Piece.h"
#import "Types.h"
#import "SoundManager.h"

enum HistoryIndex // NOTE: Do not change the constants 'values below.
{
    HISTORY_INDEX_END   = -2,
    HISTORY_INDEX_BEGIN = -1
};

// NOTE: These tags are needed so that the '_game_over_msg' label can be shared
//       more effectively.
enum InfoLabelTag
{
    INFO_LABEL_TAG_NONE       = 0,
    INFO_LABEL_TAG_GAME_OVER  = 1,  // Need to be non-zero.
    INFO_LABEL_TAG_REPLAY     = 2
};

///////////////////////////////////////////////////////////////////////////////
//
//    BoardViewController
//
///////////////////////////////////////////////////////////////////////////////

@interface BoardViewController (/* Private interface */)
- (CGRect) _gameBoardFrame;
- (void) _setHighlightCells:(BOOL)highlighted;
- (void) _setPickedUpPiece:(Piece*)piece;
- (void) _animateLatestMove:(MoveAtom*)pMove;
- (void) _clearAllAnimation;
- (void) _clearAllHighlight;
- (void) _setReplayMode:(BOOL)on;
- (void) _ticked:(NSTimer*)timer;
- (void) _updateTimer;
- (NSString*) _stringFrom:(int)seconds;
@end

@implementation BoardViewController

@synthesize game=_game;
@synthesize boardOwner=_boardOwner;
@synthesize _timer, _replayLastTouched;


- (void)awakeFromNib
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    [super awakeFromNib];
    
    if ( [self initWithNibName:@"BoardView" bundle:nil] )
    {
        // Additional settings if needed.
    }
}

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    if ( (self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil]) )
    {
        _gameboard = [[CALayer alloc] init];
        _gameboard.frame = [self _gameBoardFrame];
        
        /* NOTE: I tried [... insertSublayer atIndex:0] but sometimes got a
         *       black-blank screen with the board got covered by the background.
         * See http://code.google.com/p/hoxchess/issues/detail?id=17
         */
        [self.view.layer addSublayer:_gameboard];
        [self.view bringSubviewToFront:_game_over_msg];

        int boardType = (int) [[NSUserDefaults standardUserDefaults] integerForKey:@"board_type"];
        _game = [[Game alloc] initWithBoard:_gameboard boardType:boardType];

        _boardOwner = nil;

        _initialTime = [TimeInfo allocTimeFromString:@"1500/240/30"];
        _redTime = [[TimeInfo alloc] initWithTime:_initialTime];
        _blackTime = [[TimeInfo alloc] initWithTime:_initialTime];
        _red_time.font = [UIFont fontWithName:@"DBLCDTempBlack" size:14.0];
        _red_move_time.font = [UIFont fontWithName:@"DBLCDTempBlack" size:15.0];
        _red_time.text = [self _stringFrom:_redTime.gameTime];
        _red_move_time.text = [self _stringFrom:_redTime.moveTime];
        
        _black_time.font = [UIFont fontWithName:@"DBLCDTempBlack" size:14.0];
        _black_move_time.font = [UIFont fontWithName:@"DBLCDTempBlack" size:15.0];
        _black_time.text = [self _stringFrom:_blackTime.gameTime];
        _black_move_time.text = [self _stringFrom:_blackTime.moveTime];

        _red_label.text = @"";
        _black_label.text = @"";

        _game_over_msg.hidden = YES;
        _game_over_msg.tag = INFO_LABEL_TAG_NONE;
        _gameOver = NO;

        _moves = [[NSMutableArray alloc] initWithCapacity:HC_MAX_MOVES_PER_GAME];
        _nthMove = HISTORY_INDEX_END;
        _hl_nMoves = 0;

        _animatedPiece = nil;
        _pickedUpPiece = nil;
        _checkedKing = nil;

        _replayView.layer.cornerRadius = 9;
        _replayView.layer.backgroundColor = kTranslucentGrayColor; 

        self._replayLastTouched = [[NSDate date] dateByAddingTimeInterval:-60]; // 1-minute earlier.
        self._timer = [NSTimer scheduledTimerWithTimeInterval:1.0 target:self selector:@selector(_ticked:) userInfo:nil repeats:YES];
    }

    return self;
}

- (void)dealloc
{
    //NSLog(@"%s: ENTER.", __FUNCTION__);
    [_boardOwner release];
    [_timer release];
    [_moves release];
    [_replayLastTouched release];
    [_game release];
    [_gameboard removeFromSuperlayer];
    [_gameboard release];
    [super dealloc];
}

- (CGRect) _gameBoardFrame
{
    CGRect bounds = self.view.layer.bounds;
/*
    bounds.origin.x += 2;
    bounds.origin.y += 2;
    bounds.size.width -= 4;
    bounds.size.height -= 24;
    self.layer.bounds = bounds;
*/
    return bounds;
}


#pragma mark -
#pragma mark HIT-TESTING:


// Locates the layer at a given point in window coords.
//    If the leaf layer doesn't pass the layer-match callback, the nearest ancestor that does is returned.
//    If outOffset is provided, the point's position relative to the layer is stored into it.
- (CALayer*) hitTestPoint:(CGPoint)locationInWindow
       LayerMatchCallback:(LayerMatchCallback)match offset:(CGPoint*)outOffset
{
    CGPoint where = locationInWindow;
    where = [_gameboard convertPoint: where fromLayer:self.view.layer];
    CALayer *layer = [_gameboard hitTest:where];
    while ( layer ) {
        if ( match(layer) ) {
            CGPoint bitPos = [self.view.layer convertPoint:layer.position
                                                 fromLayer:layer.superlayer];
            if ( outOffset ) {
                *outOffset = CGPointMake( bitPos.x-where.x, bitPos.y-where.y);
            }
            return layer;
        } else {
            layer = layer.superlayer;
        }
    }
    return nil;
}

- (void) setRedLabel:(NSString*)label  { _red_label.text = label; }
- (void) setBlackLabel:(NSString*)label { _black_label.text = label; }

- (void) _animateLabel:(UILabel*)label withText:(NSString*)newText
           highlighted:(BOOL)highlighted
{
    [UIView beginAnimations:nil context:NULL];
    [UIView setAnimationDuration:1.0];
    [UIView setAnimationTransition:UIViewAnimationTransitionFlipFromRight
                           forView:label cache:YES];
    label.text = newText;
    label.textColor = (highlighted ? [UIColor redColor] : [UIColor whiteColor]);
    [UIView commitAnimations];
}

- (void) setRedLabel:(NSString*)label animated:(BOOL)animated
         highlighted:(BOOL)highlighted
{
    if (animated) {
        [self _animateLabel:_red_label withText:label highlighted:highlighted];
    } else {
        _red_label.text = label;
    }
}

- (void) setBlackLabel:(NSString*)label animated:(BOOL)animated
           highlighted:(BOOL)highlighted
{
    if (animated) {
        [self _animateLabel:_black_label withText:label highlighted:highlighted];
    } else {
        _black_label.text = label;
    }
}

- (void) setInitialTime:(NSString*)times
{
    [_initialTime release];
    _initialTime = [TimeInfo allocTimeFromString:times];
}

- (void) setRedTime:(NSString*)times
{
    [_redTime release];
    _redTime = [TimeInfo allocTimeFromString:times];
    _red_time.text = [self _stringFrom:_redTime.gameTime];
    _red_move_time.text = [self _stringFrom:_redTime.moveTime];
}

- (void) setBlackTime:(NSString*)times
{
    [_blackTime release];
    _blackTime = [TimeInfo allocTimeFromString:times];
    _black_time.text = [self _stringFrom:_blackTime.gameTime];
    _black_move_time.text = [self _stringFrom:_blackTime.moveTime];
}

- (BOOL) _isInReplay
{
    return (_nthMove != HISTORY_INDEX_END);
}

/**
 * Reset the MOVE time to the initial value.
 * If the GAME time is already zero, then reset the FREE time as well.
 */
- (void) resetMoveTime:(int)color
{
    if ( color == HC_COLOR_RED ) {
        _redTime.moveTime = _initialTime.moveTime;
        if (_redTime.gameTime == 0) {
            _redTime.moveTime = _initialTime.freeTime;
        }
    }
    else {
        _blackTime.moveTime = _initialTime.moveTime;
        if (_blackTime.gameTime == 0) {
            _blackTime.moveTime = _initialTime.freeTime;
        }
    }
}

- (void) _setHighlightCells:(BOOL)highlighted
{
    for (int i = 0; i < _hl_nMoves; ++i) {
        [_game getCellAt:DST(_hl_moves[i])].highlighted = highlighted;
    }

    if ( ! highlighted ) {
        _hl_nMoves = 0;
    }
}

- (void) _setPickedUpPiece:(Piece*)piece
{
    if (_pickedUpPiece) {
        _pickedUpPiece.pickedUp = NO;
        _pickedUpPiece = nil;
    }

    if (piece)
    {
        _pickedUpPiece = piece;
        _pickedUpPiece.pickedUp = YES;

        // Temporarily stop the latest piece's animation.
        if (_animatedPiece && _animatedPiece.highlightState == HC_HL_ANIMATED) {
            _animatedPiece.highlightState = HC_HL_NONE;
        }
        if (_checkedKing && _checkedKing.highlightState == HC_HL_CHECKED) {
            _checkedKing.highlightState = HC_HL_NONE;
        }
    }
    else
    {
        // Restore the latest piece's animation.
        if (_animatedPiece && _animatedPiece.highlightState != HC_HL_ANIMATED) {
            _animatedPiece.highlightState = HC_HL_ANIMATED;
        }
        if (_checkedKing && _checkedKing.highlightState != HC_HL_CHECKED) {
            _checkedKing.highlightState = HC_HL_CHECKED;
        }
    }
}

- (void) _animateLatestMove:(MoveAtom*)pMove
{
    _animatedPiece = pMove.srcPiece;
    _animatedPiece.highlightState = HC_HL_ANIMATED;

    if (pMove.checkedKing) {
        _checkedKing = pMove.checkedKing;
        _checkedKing.highlightState = HC_HL_CHECKED;
    }
}

- (void) _clearAllAnimation
{
    if (_animatedPiece) {
        _animatedPiece.highlightState = HC_HL_NONE;
        _animatedPiece = nil;
    }
    if (_checkedKing) {
        _checkedKing.highlightState = HC_HL_NONE;
        _checkedKing = nil;
    }
}

- (void) _clearAllHighlight
{
    [self _setHighlightCells:NO];
    _pickedUpPiece.pickedUp = NO;
    _pickedUpPiece = nil;
}

- (NSString*) _stringFrom:(int)seconds
{
    return [NSString stringWithFormat:@"%d:%02d", (seconds / 60), (seconds % 60)];
}

- (void) _playSoundAfterMove:(MoveAtom*)pMove
{
    const GameStatusEnum result     = _game.gameResult;
    const ColorEnum      ownerColor = _boardOwner.ownerColor;
    const ColorEnum      moveColor  = pMove.srcPiece.color;
    NSString*            sound      = nil;

    if (   result != HC_GAME_STATUS_IN_PROGRESS // NOTE: just for optimization!
        && (![self _isInReplay] || _nthMove == [_moves count] - 1))
    {
        if (   (result == HC_GAME_STATUS_RED_WIN && ownerColor == HC_COLOR_RED)
            || (result == HC_GAME_STATUS_BLACK_WIN && ownerColor == HC_COLOR_BLACK))
        {
            sound = @"WIN";
        }
        else if (  (result == HC_GAME_STATUS_RED_WIN && ownerColor == HC_COLOR_BLACK)
                 || (result == HC_GAME_STATUS_BLACK_WIN && ownerColor == HC_COLOR_RED))
        {
            sound = @"LOSS";
        }
        else if (result == HC_GAME_STATUS_DRAWN) {
            sound = @"DRAW";
        }
        else if (result == HC_GAME_STATUS_TOO_MANY_MOVES) {
            sound = @"ILLEGAL";
        }
    }
    else if (pMove.checkedKing) {
        sound = (moveColor == HC_COLOR_RED ? @"Check1" : @"CHECK2");
    }
    else if (pMove.capturedPiece) {
        sound = (moveColor == HC_COLOR_RED ? @"CAPTURE" : @"CAPTURE2");
    }
    else {
        sound= (moveColor == HC_COLOR_RED ? @"MOVE" : @"MOVE2");
    }

    [[SoundManager sharedInstance] playSound:sound];
}

- (void) _updateUIOnNewMove:(MoveAtom*)pMove animated:(BOOL)animated
{
    int sqDst = DST(pMove.move);
    Position toPosition = { ROW(sqDst), COLUMN(sqDst) };

    if (animated) [self _clearAllAnimation];
    [_game movePiece:pMove.srcPiece toPosition:toPosition animated:NO /*YES*/];

    [pMove.capturedPiece destroyWithAnimation:animated];

    if (animated) {
        [self _playSoundAfterMove:pMove];
        [self _animateLatestMove:pMove];
    }

    if (   _game.gameResult != HC_GAME_STATUS_IN_PROGRESS
        && (![self _isInReplay] || _nthMove == [_moves count] - 1))
    {
        [self onGameOver];
    }
}

/**
 * This function is dedicated to process only NEW incoming move that is
 * sent from one of the following sources:
 *    (1) The Local User.
 *    (2) The AI Robot.
 *    (3) The remote network user.
 */
- (void) onNewMoveFromPosition:(Position)from toPosition:(Position)to
                     setupMode:(BOOL)setup
{
    int sqSrc = TOSQUARE(from.row, from.col);
    int sqDst = TOSQUARE(to.row, to.col);
    int move = MOVE(sqSrc, sqDst);

    MoveAtom* pMove = [[[MoveAtom alloc] initWithMove:move] autorelease];
    [_moves addObject:pMove];

    ColorEnum moveColor = (_game.nextColor == HC_COLOR_RED ? HC_COLOR_BLACK : HC_COLOR_RED);
    if (!setup) {
        [self resetMoveTime:moveColor];
    }

    // Delay update the UI if in Replay mode.
    // NOTE: We do not update pMove.srcPiece (leaving it equal to nil)
    //       to signal that it is NOT yet processed.
    if ([self _isInReplay]) {
        return;
    }

    // Full update the Move's information.
    pMove.srcPiece = [_game getPieceAtRow:from.row col:from.col];
    pMove.capturedPiece = [_game getPieceAtRow:to.row col:to.col];

    if ([_game isChecked]) {
        ColorEnum checkedColor = (pMove.srcPiece.color == HC_COLOR_RED
                                  ? HC_COLOR_BLACK : HC_COLOR_RED);
        pMove.checkedKing = [_game getKingOfColor:checkedColor];
    }

    // Finally, update the Board's UI accordingly.
    [self _updateUIOnNewMove:pMove animated:!setup];
}

- (void) _animateInfoLabel
{
    [UIView beginAnimations:nil context:NULL];
    [UIView setAnimationDuration:1.0];
    [UIView setAnimationTransition:UIViewAnimationTransitionFlipFromRight
                           forView:_game_over_msg cache:YES];
    [UIView commitAnimations];
}

- (void) onGameOver
{
    if (_game_over_msg.tag != INFO_LABEL_TAG_GAME_OVER) {
        _game_over_msg.text = NSLocalizedString(@"Game Over", @"");
        _game_over_msg.alpha = 1.0;
        _game_over_msg.hidden = NO;
        _game_over_msg.tag = INFO_LABEL_TAG_GAME_OVER;
        [self _animateInfoLabel];
    }
    _gameOver = YES;
}

- (void) _setReplayMode:(BOOL)on
{
    if (on) {
        _game_over_msg.text = [NSString stringWithFormat:@"%@ %d/%lu",
                               NSLocalizedString(@"Replay", @""),
                               _nthMove+1, (unsigned long)[_moves count]];
        if (_game_over_msg.tag != INFO_LABEL_TAG_REPLAY) {
            _game_over_msg.hidden = NO;
            _game_over_msg.alpha = 0.5;
            _game_over_msg.tag = INFO_LABEL_TAG_REPLAY;
            [self _animateInfoLabel];
        }
    } else if (_game.gameResult != HC_GAME_STATUS_IN_PROGRESS) { 
        [self onGameOver];
    } else {
        _game_over_msg.tag = INFO_LABEL_TAG_NONE;
        _game_over_msg.hidden = YES;
        [self _animateInfoLabel];
    }
}

- (void) _updateTimer
{
    if (_game.nextColor == HC_COLOR_BLACK) {
        _black_time.text = [self _stringFrom:_blackTime.gameTime];
        _black_move_time.text = [self _stringFrom:_blackTime.moveTime];
        [_blackTime decrement];
    } else {
        _red_time.text = [self _stringFrom:_redTime.gameTime];
        _red_move_time.text = [self _stringFrom:_redTime.moveTime];
        [_redTime decrement];
    }
}

- (void) _ticked:(NSTimer*)timer
{
    NSTimeInterval timeInterval = - [_replayLastTouched timeIntervalSinceNow]; // in seconds.
    if (![self _isInReplay] && timeInterval > 5) { // hide if older than 5 seconds?
        _replayView.hidden = YES;
    }
    
    // NOTE: On networked games, at least one Move made by EACH player before
    //       the timer is started. However, it is more user-friendly for
    //       this App (with AI only) to start the timer right after one Move
    //       is made (by RED).
    //
    if (!_gameOver && [_moves count] > 0) {
        [self _updateTimer];
    }
}

- (void) rescheduleTimer
{
    if (_timer) [_timer invalidate];
    self._timer = [NSTimer scheduledTimerWithTimeInterval:1.0 target:self selector:@selector(_ticked:) userInfo:nil repeats:YES];
}

- (void) destroyTimer
{
    if (_timer) [_timer invalidate];
    self._timer = nil;
}

- (NSMutableArray*) getMoves
{
    return _moves;
}

#pragma mark -
#pragma mark UI-Event Handlers:

- (BOOL) _doReplayPREV:(BOOL)animated
{
    if (    [_moves count] == 0              // No Moves made yet?
        || _nthMove == HISTORY_INDEX_BEGIN ) // ... or already at BEGIN mark?
    {
        return NO;
    }

    if (_nthMove == HISTORY_INDEX_END ) { // at the END mark?
        _nthMove = (int) ([_moves count] - 1); // Get the latest move.
    }
    
    MoveAtom* pMove = [_moves objectAtIndex:_nthMove];
    int move = pMove.move;
    int sqSrc = SRC(move);
    if (animated) [[SoundManager sharedInstance] playSound:@"Replay"];
    
    // For Move-Replay, just reverse the move order (sqDst->sqSrc)
    // Since it's only a replay, no need to make actual move in
    // the underlying game logic.

    [self _clearAllAnimation];
    Position oldPosition = { ROW(sqSrc), COLUMN(sqSrc) };
    [_game movePiece:pMove.srcPiece toPosition:oldPosition animated:NO];

    [pMove.capturedPiece putbackInLayer:_gameboard]; // Restore.
    
    // Highlight the Piece (if any) of the "next-PREV" Move.
    --_nthMove;
    if (_nthMove >= 0) {
        pMove = [_moves objectAtIndex:_nthMove];
        if (animated) [self _animateLatestMove:pMove];
    }
    return YES;
}

- (IBAction) replayPressed_BEGIN:(id)sender
{
    self._replayLastTouched = [NSDate date];
    if (![self _isInReplay]) {
        [self _clearAllHighlight];
    }

    if (    [_moves count] == 0              // No Moves made yet?
        || _nthMove == HISTORY_INDEX_BEGIN ) // ... or already at BEGIN mark?
    {
        return;
    }
    [[SoundManager sharedInstance] playSound:@"Replay"];

    while ([self _doReplayPREV:NO]) { /* keep going until no more moves */}
    [self _setReplayMode:[self _isInReplay]];
}

- (IBAction) replayPressed_PREVIOUS:(id)sender
{
    self._replayLastTouched = [NSDate date];
    if (![self _isInReplay]) {
        [self _clearAllHighlight];
    }

    [self _doReplayPREV:YES];
    [self _setReplayMode:[self _isInReplay]];
}

- (BOOL) _doReplayNEXT:(BOOL)animated
{
    if (    [_moves count] == 0             // No Moves made yet?
         || _nthMove == HISTORY_INDEX_END ) // ... or at the END mark?
    {
        return NO;
    }

    ++_nthMove;
    NSAssert1(_nthMove >= 0 && _nthMove < [_moves count], @"Invalid index [%d]", _nthMove);
    
    MoveAtom* pMove = [_moves objectAtIndex:_nthMove];
    int move = pMove.move;

    if (!pMove.srcPiece) // not yet processed?
    {                    // ... then we process it as a NEW move.
        NSLog(@"%s: Process pending move [%@]...", __FUNCTION__, pMove);
        pMove.srcPiece = [_game getPieceAtCell:SRC(move)];
        pMove.capturedPiece = [_game getPieceAtCell:DST(move)];
        if ([_game isChecked]) {
            ColorEnum checkedColor = (pMove.srcPiece.color == HC_COLOR_RED
                                      ? HC_COLOR_BLACK : HC_COLOR_RED);
            pMove.checkedKing = [_game getKingOfColor:checkedColor];
        }
        [self _updateUIOnNewMove:pMove animated:YES];
    }
    else
    {
        [self _clearAllAnimation];
        [self _updateUIOnNewMove:pMove animated:animated];
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // NOTE: We delay updating the index to the "END" mark to avoid race
    //       conditions that could occur when there is a NEW move.
    //       The "END" mark is a signal that allows the main UI Thread to
    //       process new incoming Moves immediately.
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    if (_nthMove == [_moves count] - 1) {
        _nthMove = HISTORY_INDEX_END;
    }

    return YES;
}

- (IBAction) replayPressed_NEXT:(id)sender
{
    self._replayLastTouched = [NSDate date];
    [self _doReplayNEXT:YES];
    [self _setReplayMode:[self _isInReplay]];
}

- (IBAction) replayPressed_END:(id)sender
{
    self._replayLastTouched = [NSDate date];
    
    if (    [_moves count] == 0             // No Moves made yet?
        || _nthMove == HISTORY_INDEX_END ) // ... or at the END mark?
    {
        return;
    }
    
    const int lastMoveIndex = (int) ([_moves count] - 2);
    while (_nthMove < lastMoveIndex) {
        [self _doReplayNEXT:NO];
    }
    [self _doReplayNEXT:YES];
    
    [self _setReplayMode:[self _isInReplay]];
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    if ( [[event allTouches] count] != 1 ) { // Valid for single touch only
        return;
    }
    
    UITouch* touch = [[touches allObjects] objectAtIndex:0];
    CGPoint p = [touch locationInView:self.view];
    //NSLog(@"%s: p = [%f, %f].", __FUNCTION__, p.x, p.y);    

    Piece* piece = (Piece*)[self hitTestPoint:p LayerMatchCallback:layerIsPiece offset:NULL];

     if (!piece && p.y > 382) // ... near the y-coordinate of Replay buttons.
     {
         _replayView.hidden = NO;
         self._replayLastTouched = [NSDate date]; // now.
     }
    
    if (    [self _isInReplay]   // Do nothing if in the middle of Move-Replay.
        ||  _game.gameResult != HC_GAME_STATUS_IN_PROGRESS
        || ![_boardOwner isMyTurnNext] // Ignore when it is not my turn.
        || ![_boardOwner isGameReady] )
    { 
        return;
    }
    
    GridCell* holder = nil;
    
    if (piece) {
        holder = piece.holder;
        if (   (!_pickedUpPiece && piece.color == _game.nextColor) 
            || (_pickedUpPiece && piece.color == _pickedUpPiece.color) )
        {
            [self _setPickedUpPiece:piece]; // Must come before 'highlighting'!
            Position from = [_game getActualPositionAtCell:holder];
            [self _setHighlightCells:NO];
            _hl_nMoves = [_game generateMoveFrom:from moves:_hl_moves];
            [self _setHighlightCells:YES];
            [[SoundManager sharedInstance] playSound:@"CLICK"];
            return;
        }
    } else {
        holder = (GridCell*)[self hitTestPoint:p LayerMatchCallback:layerIsGridCell offset:NULL];
    }
    
    // Make a Move from the last selected cell to the current selected cell.
    _pickedUpPiece.pickedUp = NO;
    if (holder && holder.highlighted && _pickedUpPiece)
    {
        Position from = [_game getActualPositionAtCell:_pickedUpPiece.holder];
        Position to = [_game getActualPositionAtCell:holder];
        if ([_game isMoveLegalFrom:from toPosition:to])
        {
            [_game doMoveFrom:from toPosition:to];
            [self _setHighlightCells:NO]; // Must come before 'Move-animation'!
            [self onNewMoveFromPosition:from toPosition:to setupMode:NO];
            [_boardOwner onLocalMoveMadeFrom:from toPosition:to];
        }
        else {
            [[SoundManager sharedInstance] playSound:@"ILLEGAL"];
        }
    }

    [self _setHighlightCells:NO];
    [self _setPickedUpPiece:nil];
}

- (void) resetBoard
{
    [self _clearAllHighlight];
    [self _clearAllAnimation];

    [_redTime release];
    _redTime = [[TimeInfo alloc] initWithTime:_initialTime];
    [_blackTime release];
    _blackTime = [[TimeInfo alloc] initWithTime:_initialTime];
    _red_time.text = [self _stringFrom:_redTime.gameTime];
    _red_move_time.text = [self _stringFrom:_redTime.moveTime];
    _black_time.text = [self _stringFrom:_blackTime.gameTime];
    _black_move_time.text = [self _stringFrom:_blackTime.moveTime];

    _game_over_msg.hidden = YES;
    _game_over_msg.tag = INFO_LABEL_TAG_NONE;
    _gameOver = NO;

    [_game resetGame];
    [_moves removeAllObjects];
    _nthMove = HISTORY_INDEX_END;
}

- (void) reverseBoardView
{
    [_game reverseView];
    CGRect redRect = _red_label.frame;
    _red_label.frame = _black_label.frame;
    _black_label.frame = redRect;
    redRect = _red_time.frame;
    _red_time.frame = _black_time.frame;
    _black_time.frame = redRect;
    redRect = _red_move_time.frame;
    _red_move_time.frame = _black_move_time.frame;
    _black_move_time.frame = redRect;
}

- (void) reverseRole
{
    [self _clearAllHighlight];
    [self reverseBoardView];
    NSString* redText = _red_label.text;
    _red_label.text = _black_label.text;
    _black_label.text = redText;
    UIColor* redColor = _red_label.textColor;
    _red_label.textColor = _black_label.textColor;
    _black_label.textColor = redColor;
}

@end
