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


#import "Game.h"
#import "Piece.h"
#import "QuartzUtils.h"

///////////////////////////////////////////////////////////////////////////////
//
//    Private methods
//
///////////////////////////////////////////////////////////////////////////////

#pragma mark -
#pragma mark The private interface of CChessGame

@interface Game (/* Private interface */)

- (void) _createPiece:(PieceEnum)type color:(ColorEnum)color
                  row:(int)row col:(int)col;
- (void) _setupPieces;
- (void) _resetPieces;
- (void) _setPiece:(Piece*)piece toRow:(int)row toCol:(int)col;
- (void) _checkAndUpdateGameStatus;
- (NSString*) _pieceToString:(PieceEnum)type;

@end


///////////////////////////////////////////////////////////////////////////////
//
//    Implementation of Public methods
//
///////////////////////////////////////////////////////////////////////////////

#pragma mark -
#pragma mark The implementation of the interface of CChessGame

@implementation Game

@synthesize gameResult=_gameResult;
@synthesize blackAtTopSide=_blackAtTopSide;

- (id) initWithBoard:(CALayer*)board boardType:(int)boardType
{
    if ( (self = [super init]) )
    {
        _board = [board retain];
        
        CGFloat    cellSize = 35;
        CGPoint    cellOffset = CGPointMake(0, 0);
        CGPoint    boardPosition = CGPointMake(0, 29);
        CGRect     boardFrame = CGRectMake(0, 0, 320, 352);
        CGColorRef backgroundColor = nil;
        CGColorRef lineColor       = nil;
        CGColorRef highlightColor  = kLightBlueColor;
        CGColorRef animateColor    = kLightBlueColor;
        
        switch (boardType)
        {
            case 1:  // Western background.
            {
                backgroundColor = GetCGPatternNamed(@"Western.png");
                boardPosition = CGPointMake(2.5, 29);
                boardFrame = CGRectMake(0, 0, 315, 350);
                break;
            }
            case 2:  // The custom-drawn background.
            {
                backgroundColor = GetCGPatternNamed(@"board_320x355.png");
                lineColor = kLightRedColor;
                boardPosition = CGPointMake(2.5, 29);
                boardFrame = CGRectMake(0, 0, 320, 355);
                break;
            }
            case 3:  // SKELETON background.
            {
                backgroundColor = GetCGPatternNamed(@"SKELETON.png");
                cellSize = 34.84;
                cellOffset = CGPointMake(2.7, 1.5);
                break;
            }
            case 4:  // WOOD background.
            {
                backgroundColor = GetCGPatternNamed(@"WOOD.png");
                cellSize = 34.84;
                cellOffset = CGPointMake(2.7, 1.5);
                break;
            }
            default: // PlayXiangqi background.
            {
                backgroundColor = GetCGPatternNamed(@"PlayXiangqi.png");
                cellOffset = CGPointMake(2.5, 2);
                boardFrame = CGRectMake(0, 0, 320, 355);
                break;
            }
        }
        
        CGSize spacing = CGSizeMake(cellSize, cellSize);
        _grid = [[Grid alloc] initWithRows:10 columns:9
                             boardPosition:boardPosition
                                boardFrame:boardFrame
                                   spacing:spacing
                                cellOffset:cellOffset
                           backgroundColor:backgroundColor];
        _grid.lineColor = lineColor;
        _grid.highlightColor = highlightColor;
        _grid.animateColor = animateColor;
        
        //_grid.borderColor = kTranslucentLightGrayColor;
        //_grid.borderWidth = 2;
        
        [_grid addAllCells];
        [_board addSublayer:_grid];
        
        _pieceBox = [[NSMutableArray alloc] initWithCapacity:32];
        [self _setupPieces];
        
        [_grid cellAtRow: 3 column: 0].dotted = YES;
        [_grid cellAtRow: 6 column: 0].dotted = YES;
        [_grid cellAtRow: 2 column: 1].dotted = YES;
        [_grid cellAtRow: 7 column: 1].dotted = YES;
        [_grid cellAtRow: 3 column: 2].dotted = YES;
        [_grid cellAtRow: 6 column: 2].dotted = YES;
        [_grid cellAtRow: 3 column: 4].dotted = YES;
        [_grid cellAtRow: 6 column: 4].dotted = YES;
        [_grid cellAtRow: 3 column: 6].dotted = YES;
        [_grid cellAtRow: 6 column: 6].dotted = YES;
        [_grid cellAtRow: 2 column: 7].dotted = YES;
        [_grid cellAtRow: 7 column: 7].dotted = YES;
        [_grid cellAtRow: 3 column: 8].dotted = YES;
        [_grid cellAtRow: 6 column: 8].dotted = YES;
        
        _blackAtTopSide = YES;
        _gameResult = HC_GAME_STATUS_IN_PROGRESS;
        
        // Create a Referee to manage the Game.
        _referee = [[Referee alloc] init];
        [_referee initGame];
    }
    return self;
}

- (void)dealloc
{
    //NSLog(@"%s: ENTER.", __FUNCTION__);
    [_grid removeAllCells];
    [_grid release];
    [_pieceBox release];
    [_referee release];
    [_board release];
    _board = nil;    
    [super dealloc];
}


#pragma mark -
#pragma mark Piece/Cell Public API

- (void) movePiece:(Piece*)piece toPosition:(Position)position
          animated:(BOOL)animated;
{
    int row = position.row, col = position.col;
    if (!_blackAtTopSide) {
        row = 9 - row;
        col = 8 - col;
    }
    GridCell* newCell = [_grid cellAtRow:row column:col];
    CGPoint newPosition = [newCell getMidInLayer:_board];
    piece.holder = newCell;
    [piece movePieceTo:newPosition animated:animated];
}

- (Piece*) getPieceAtRow:(int)row col:(int)col
{
    if (!_blackAtTopSide) {
        row = 9 - row;
        col = 8 - col;
    }
    GridCell* cell = [_grid cellAtRow:row column:col]; 
    CALayer* piece = [_board hitTest:[cell getMidInLayer:_board]];
    if (piece && [piece isKindOfClass:[Piece class]]) {
        return (Piece*)piece;
    }
    
    return nil;
}

- (Piece*) getPieceAtCell:(int)square
{
    return [self getPieceAtRow:ROW(square) col:COLUMN(square)];
}

- (Piece*) getKingOfColor:(ColorEnum)color
{
    for (Piece* piece in _pieceBox) {
        if (piece.type == HC_PIECE_KING &&  piece.color == color) {
            return piece;
        }
    }
    return nil;
}

- (GridCell*) getCellAtRow:(int)row col:(int)col
{
    if (!_blackAtTopSide) {
        row = 9 - row;
        col = 8 - col;
    }
    GridCell* cell = [_grid cellAtRow:row column:col];
    return cell;
}

- (GridCell*) getCellAt:(int)square
{
    return [self getCellAtRow:ROW(square) col:COLUMN(square)];
}

- (Position) getActualPositionAtCell:(GridCell*)cell
{
    int row = cell.row, col = cell.column;
    if (!self.blackAtTopSide) {
        row = 9 - row;
        col = 8 - col;
    }
    Position position = { row, col };
    return position;
}

#pragma mark -
#pragma mark Move/Game Public API

- (int) doMoveFrom:(Position)from toPosition:(Position)to
{
    int sqSrc = TOSQUARE(from.row, from.col);
    int sqDst = TOSQUARE(to.row, to.col);
    int move = MOVE(sqSrc, sqDst);
    int captured = 0;
    
    [_referee makeMove:move captured:&captured];
    [self _checkAndUpdateGameStatus];
    
    return captured;
}

- (int) generateMoveFrom:(Position)from moves:(int*)mvs
{
    int sqSrc = TOSQUARE(from.row, from.col);
    return [_referee generateMoveFrom:sqSrc moves:mvs];
}

- (BOOL) isMoveLegalFrom:(Position)from toPosition:(Position)to
{
    int sqSrc = TOSQUARE(from.row, from.col);
    int sqDst = TOSQUARE(to.row, to.col);
    int move = MOVE(sqSrc, sqDst);
    return [_referee isLegalMove:move];
}

- (BOOL) isChecked
{
    return [_referee isChecked];
}

- (ColorEnum) nextColor
{
    return [_referee get_sdPlayer] ? HC_COLOR_BLACK : HC_COLOR_RED;
}

- (int) getMoveCount { return [_referee get_nMoveNum]; }

- (void) resetGame
{
    BOOL saved_blackAtTopSide = _blackAtTopSide;
    _blackAtTopSide = YES;
    [self _resetPieces];
    if (!saved_blackAtTopSide) {
        [self reverseView];
    }
    _blackAtTopSide = saved_blackAtTopSide;
    
    [_referee initGame];
    _gameResult = HC_GAME_STATUS_IN_PROGRESS;
}

- (void) reverseView
{
    for (Piece* piece in _pieceBox) {
        if (piece.superlayer) { // not captured?
            GridCell* holder = piece.holder;
            unsigned row = 9 - holder.row;
            unsigned column = 8 - holder.column;
            [self _setPiece:piece toRow:row toCol:column];
        }
    }
    _blackAtTopSide = !_blackAtTopSide;
}


#pragma mark -
#pragma mark Private API
            
- (void) _createPiece:(PieceEnum)type color:(ColorEnum)color
                  row:(int)row col:(int)col 
{
    NSString* sType = [self _pieceToString:type];
    NSString* sFile = [NSString stringWithFormat:@"%c%@.png",
                       (color == HC_COLOR_RED ? 'r' : 'b'), sType];
    NSString* imageName = [[NSBundle mainBundle] pathForResource:sFile ofType:nil
                                                     inDirectory:_pieceFolder];
    GridCell* cell = [_grid cellAtRow:row column:col]; 
    Piece* piece = [[Piece alloc] initWithType:type color:color
                                     imageName:imageName scale:_pieceScale];
    piece.holder = cell;
    [_board addSublayer:piece];
    piece.position = [cell getMidInLayer:_board];
    [_pieceBox addObject:piece];
    [piece release];
}

- (void) _setPiece:(Piece*)piece toRow:(int)row toCol:(int)col
{
    GridCell* cell = [_grid cellAtRow:row column:col]; 
    piece.position = [cell getMidInLayer:_board];
    piece.holder = cell;
    if (!piece.superlayer) {
        [piece putbackInLayer:_board]; // Restore the captured piece.
    }
}

- (void) _checkAndUpdateGameStatus
{
    BOOL redMoved = (self.nextColor == HC_COLOR_BLACK); // Red just moved?
    int nRepVal = 0;

    if ( [_referee isMate] ) {
        _gameResult = (redMoved ? HC_GAME_STATUS_RED_WIN : HC_GAME_STATUS_BLACK_WIN);
    }
    else if ([_referee repStatus:3 repValue:&nRepVal] > 0) // Check repeat status
    {
        if (redMoved) {
            _gameResult = nRepVal < -WIN_VALUE ? HC_GAME_STATUS_RED_WIN 
                : (nRepVal > WIN_VALUE ? HC_GAME_STATUS_BLACK_WIN : HC_GAME_STATUS_DRAWN);
        } else {
            _gameResult = nRepVal > WIN_VALUE ? HC_GAME_STATUS_RED_WIN 
                : (nRepVal < -WIN_VALUE ? HC_GAME_STATUS_BLACK_WIN : HC_GAME_STATUS_DRAWN);
        }
    }
    else if ([_referee get_nMoveNum] > HC_MAX_MOVES_PER_GAME) {
        _gameResult = HC_GAME_STATUS_TOO_MANY_MOVES;
    }
}

- (void) _movePiece:(Piece*)piece toRow:(int)row toCol:(int)col
{
    if (!_blackAtTopSide) {
        row = 9 - row;
        col = 8 - col;
    }
    [self _setPiece:piece toRow:row toCol:col];
}

- (void) _resetPieces
{
    // reset the pieces in pieceBox by the order they are created
    // chariot
    [self _movePiece:[_pieceBox objectAtIndex:0] toRow:0 toCol:0];
    [self _movePiece:[_pieceBox objectAtIndex:1] toRow:0 toCol:8];
    [self _movePiece:[_pieceBox objectAtIndex:2] toRow:9 toCol:0];
    [self _movePiece:[_pieceBox objectAtIndex:3] toRow:9 toCol:8];
    
    // horse
    [self _movePiece:[_pieceBox objectAtIndex:4] toRow:0 toCol:1];
    [self _movePiece:[_pieceBox objectAtIndex:5] toRow:0 toCol:7];
    [self _movePiece:[_pieceBox objectAtIndex:6] toRow:9 toCol:1];
    [self _movePiece:[_pieceBox objectAtIndex:7] toRow:9 toCol:7];
    
    // elephant
    [self _movePiece:[_pieceBox objectAtIndex:8] toRow:0 toCol:2];
    [self _movePiece:[_pieceBox objectAtIndex:9] toRow:0 toCol:6];
    [self _movePiece:[_pieceBox objectAtIndex:10] toRow:9 toCol:2];
    [self _movePiece:[_pieceBox objectAtIndex:11] toRow:9 toCol:6];
    
    // advisor
    [self _movePiece:[_pieceBox objectAtIndex:12] toRow:0 toCol:3];
    [self _movePiece:[_pieceBox objectAtIndex:13] toRow:0 toCol:5];
    [self _movePiece:[_pieceBox objectAtIndex:14] toRow:9 toCol:3];
    [self _movePiece:[_pieceBox objectAtIndex:15] toRow:9 toCol:5];
    
    // king
    [self _movePiece:[_pieceBox objectAtIndex:16] toRow:0 toCol:4];
    [self _movePiece:[_pieceBox objectAtIndex:17] toRow:9 toCol:4];
    
    // cannon
    [self _movePiece:[_pieceBox objectAtIndex:18] toRow:2 toCol:1];
    [self _movePiece:[_pieceBox objectAtIndex:19] toRow:2 toCol:7];
    [self _movePiece:[_pieceBox objectAtIndex:20] toRow:7 toCol:1];
    [self _movePiece:[_pieceBox objectAtIndex:21] toRow:7 toCol:7];
    
    // pawn
    [self _movePiece:[_pieceBox objectAtIndex:22] toRow:3 toCol:0];
    [self _movePiece:[_pieceBox objectAtIndex:23] toRow:3 toCol:2];
    [self _movePiece:[_pieceBox objectAtIndex:24] toRow:3 toCol:4];
    [self _movePiece:[_pieceBox objectAtIndex:25] toRow:3 toCol:6];
    [self _movePiece:[_pieceBox objectAtIndex:26] toRow:3 toCol:8];
    [self _movePiece:[_pieceBox objectAtIndex:27] toRow:6 toCol:0];
    [self _movePiece:[_pieceBox objectAtIndex:28] toRow:6 toCol:2];
    [self _movePiece:[_pieceBox objectAtIndex:29] toRow:6 toCol:4];
    [self _movePiece:[_pieceBox objectAtIndex:30] toRow:6 toCol:6];
    [self _movePiece:[_pieceBox objectAtIndex:31] toRow:6 toCol:8];
}

- (void) _setupPieces
{
    _pieceFolder = nil;
    _pieceScale = 33; //_grid.spacing.width
    NSInteger pieceType = [[NSUserDefaults standardUserDefaults] integerForKey:@"piece_type"];
    switch (pieceType) {
        case 0: _pieceFolder = @"pieces/alfaerie"; break;
        case 1: _pieceFolder = @"pieces/xqwizard"; break;
        case 2: _pieceFolder = @"pieces/wikipedia"; break;
        case 3: _pieceFolder = @"pieces/Adventure"; break;
        default: _pieceFolder = @"pieces/HOXChess"; break;
    }

    // Chariot      
    [self _createPiece:HC_PIECE_CHARIOT color:HC_COLOR_BLACK row:0 col:0 ];
    [self _createPiece:HC_PIECE_CHARIOT color:HC_COLOR_BLACK row:0 col:8 ];         
    [self _createPiece:HC_PIECE_CHARIOT color:HC_COLOR_RED row:9 col:0];     
    [self _createPiece:HC_PIECE_CHARIOT color:HC_COLOR_RED row:9 col:8];  

    // Horse    
    [self _createPiece:HC_PIECE_HORSE color:HC_COLOR_BLACK row:0 col:1];        
    [self _createPiece:HC_PIECE_HORSE color:HC_COLOR_BLACK row:0 col:7];         
    [self _createPiece:HC_PIECE_HORSE color:HC_COLOR_RED row:9 col:1];      
    [self _createPiece:HC_PIECE_HORSE color:HC_COLOR_RED row:9 col:7];
    
    // Elephant      
    [self _createPiece:HC_PIECE_ELEPHANT color:HC_COLOR_BLACK row:0 col:2];        
    [self _createPiece:HC_PIECE_ELEPHANT color:HC_COLOR_BLACK row:0 col:6];        
    [self _createPiece:HC_PIECE_ELEPHANT color:HC_COLOR_RED row:9 col:2];     
    [self _createPiece:HC_PIECE_ELEPHANT color:HC_COLOR_RED row:9 col:6]; 
    
    // Advisor      
    [self _createPiece:HC_PIECE_ADVISOR color:HC_COLOR_BLACK row:0 col:3];         
    [self _createPiece:HC_PIECE_ADVISOR color:HC_COLOR_BLACK row:0 col:5];         
    [self _createPiece:HC_PIECE_ADVISOR color:HC_COLOR_RED row:9 col:3];        
    [self _createPiece:HC_PIECE_ADVISOR color:HC_COLOR_RED row:9 col:5];
    
    // King       
    [self _createPiece:HC_PIECE_KING color:HC_COLOR_BLACK row:0 col:4];       
    [self _createPiece:HC_PIECE_KING color:HC_COLOR_RED row:9 col:4];
    
    // Cannon     
    [self _createPiece:HC_PIECE_CANNON color:HC_COLOR_BLACK row:2 col:1];       
    [self _createPiece:HC_PIECE_CANNON color:HC_COLOR_BLACK row:2 col:7];          
    [self _createPiece:HC_PIECE_CANNON color:HC_COLOR_RED row:7 col:1];        
    [self _createPiece:HC_PIECE_CANNON color:HC_COLOR_RED row:7 col:7];

    // Pawn       
    [self _createPiece:HC_PIECE_PAWN color:HC_COLOR_BLACK row:3 col:0];         
    [self _createPiece:HC_PIECE_PAWN color:HC_COLOR_BLACK row:3 col:2];         
    [self _createPiece:HC_PIECE_PAWN color:HC_COLOR_BLACK row:3 col:4];        
    [self _createPiece:HC_PIECE_PAWN color:HC_COLOR_BLACK row:3 col:6];      
    [self _createPiece:HC_PIECE_PAWN color:HC_COLOR_BLACK row:3 col:8];     
    [self _createPiece:HC_PIECE_PAWN color:HC_COLOR_RED row:6 col:0];      
    [self _createPiece:HC_PIECE_PAWN color:HC_COLOR_RED row:6 col:2];         
    [self _createPiece:HC_PIECE_PAWN color:HC_COLOR_RED row:6 col:4];       
    [self _createPiece:HC_PIECE_PAWN color:HC_COLOR_RED row:6 col:6];      
    [self _createPiece:HC_PIECE_PAWN color:HC_COLOR_RED row:6 col:8];
}

- (NSString*) _pieceToString:(PieceEnum)type
{
    switch (type) {
        case HC_PIECE_KING:     return @"king";
        case HC_PIECE_ADVISOR:  return @"advisor";
        case HC_PIECE_ELEPHANT: return @"elephant";
        case HC_PIECE_CHARIOT:  return @"chariot";
        case HC_PIECE_HORSE:    return @"horse";
        case HC_PIECE_CANNON:   return @"cannon";
        case HC_PIECE_PAWN:     return @"pawn";
        default:                return nil;
    }
    return nil;
}

@end
