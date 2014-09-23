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

#import "Enums.h"
#import "Types.h"
#import "Grid.h"
#import "Referee.h"

@interface Game : NSObject
{
    CALayer*        _board;
    Grid*           _grid;
    NSMutableArray* _pieceBox;
    NSString*       _pieceFolder;
    CGFloat         _pieceScale;
    BOOL            _blackAtTopSide;

    Referee*        _referee;
    GameStatusEnum  _gameResult;
}

- (id) initWithBoard:(CALayer*)board boardType:(int)boardType;

- (void) movePiece:(Piece*)piece toPosition:(Position)position
          animated:(BOOL)animated;
- (Piece*) getPieceAtRow:(int)row col:(int)col;
- (Piece*) getPieceAtCell:(int)square;
- (Piece*) getKingOfColor:(ColorEnum)color;
- (GridCell*) getCellAtRow:(int)row col:(int)col;
- (GridCell*) getCellAt:(int)square;

- (int) doMoveFrom:(Position)from toPosition:(Position)to;
- (int) generateMoveFrom:(Position)from moves:(int*)mvs;
- (BOOL) isMoveLegalFrom:(Position)from toPosition:(Position)to;
- (BOOL) isChecked;
- (int) getMoveCount;
- (void) resetGame;
- (void) reverseView;
- (Position) getActualPositionAtCell:(GridCell*)cell;

@property (nonatomic, readonly) BOOL blackAtTopSide;
@property (nonatomic, readonly) GameStatusEnum gameResult;
@property (readonly)            ColorEnum nextColor;

@end
