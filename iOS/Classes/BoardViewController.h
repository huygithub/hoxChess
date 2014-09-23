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

#import <UIKit/UIKit.h>
#import "Game.h"

@class TimeInfo;

// --------------------------------------
@protocol BoardOwner <NSObject>
- (BOOL) isMyTurnNext;
- (BOOL) isGameReady;
- (void) onLocalMoveMadeFrom:(Position)from toPosition:(Position)to;
@property (readonly) ColorEnum ownerColor;
@end

// --------------------------------------
@interface BoardViewController : UIViewController
{
    Game*                 _game;           // Current Game
    CALayer*              _gameboard;      // Game's main layer

    id <BoardOwner>       _boardOwner;

    IBOutlet UIButton*    _red_seat;
    IBOutlet UIButton*    _black_seat;
    IBOutlet UILabel*     _red_label;
    IBOutlet UILabel*     _black_label;
    IBOutlet UILabel*     _red_time;
    IBOutlet UILabel*     _red_move_time;
    IBOutlet UILabel*     _black_time;
    IBOutlet UILabel*     _black_move_time;

    IBOutlet UILabel*     _game_over_msg;
    BOOL                  _gameOver;

    IBOutlet UIView*      _replayView;
    IBOutlet UIButton*    _replay_begin;
    IBOutlet UIButton*    _replay_prev;
    IBOutlet UIButton*    _replay_next;
    IBOutlet UIButton*    _replay_end;

    NSDate*               _replayLastTouched;    
    NSTimer*              _timer;
    
    TimeInfo*             _initialTime;
    TimeInfo*             _redTime;
    TimeInfo*             _blackTime;

    // Members to keep track of (H)igh(L)ight moves (e.g., move-hints).
    int                   _hl_moves[MAX_GEN_MOVES];
    int                   _hl_nMoves;    
    Piece*                _animatedPiece; // The last Piece that was animated.
    Piece*                _pickedUpPiece;
    Piece*                _checkedKing;   // A King that is being checked.

    NSMutableArray*       _moves;       // MOVE history
    int                   _nthMove;     // pivot for the Move Replay
}

- (CALayer*) hitTestPoint:(CGPoint)locationInWindow
       LayerMatchCallback:(LayerMatchCallback)match offset:(CGPoint*)outOffset;

@property (readonly) Game* game;
@property (nonatomic, retain) id <BoardOwner> boardOwner;
@property (nonatomic, retain) NSTimer* _timer;
@property (nonatomic, retain) NSDate* _replayLastTouched;

- (IBAction) replayPressed_BEGIN:(id)sender;
- (IBAction) replayPressed_PREVIOUS:(id)sender;
- (IBAction) replayPressed_NEXT:(id)sender;
- (IBAction) replayPressed_END:(id)sender;

- (void) setRedLabel:(NSString*)label;
- (void) setBlackLabel:(NSString*)label;
- (void) setRedLabel:(NSString*)label animated:(BOOL)animated
         highlighted:(BOOL)highlighted;
- (void) setBlackLabel:(NSString*)label animated:(BOOL)animated
           highlighted:(BOOL)highlighted;
- (void) setInitialTime:(NSString*)times;
- (void) setRedTime:(NSString*)times;
- (void) setBlackTime:(NSString*)times;
- (void) rescheduleTimer;
- (void) destroyTimer;
- (void) onNewMoveFromPosition:(Position)from toPosition:(Position)to
                     setupMode:(BOOL)setup;
- (void) onGameOver;
- (NSMutableArray*) getMoves;
- (void) resetBoard;
- (void) reverseBoardView;
- (void) reverseRole;

@end
