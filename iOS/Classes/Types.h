/***************************************************************************
 *  Copyright 2009-2010 Nevo Hua  <nevo.hua@playxiangqi.com>               *
 *                                                                         * 
 *  This file is part of NevoChess.                                        *
 *                                                                         *
 *  NevoChess is free software: you can redistribute it and/or modify      *
 *  it under the terms of the GNU General Public License as published by   *
 *  the Free Software Foundation, either version 3 of the License, or      *
 *  (at your option) any later version.                                    *
 *                                                                         *
 *  NevoChess is distributed in the hope that it will be useful,           *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 *  GNU General Public License for more details.                           *
 *                                                                         *
 *  You should have received a copy of the GNU General Public License      *
 *  along with NevoChess.  If not, see <http://www.gnu.org/licenses/>.     *
 ***************************************************************************/

/*
 *  Types.h
 *  Created by Huy Phan on 02/28/2010.
 *
 *  Containing the common types that are used throughout the project.
 */

#import <Foundation/Foundation.h>

enum ActionIndexEnum
{
    ACTION_INDEX_CLOSE,
    ACTION_INDEX_RESIGN,
    ACTION_INDEX_DRAW,
    ACTION_INDEX_RESET,
    ACTION_INDEX_LOGOUT,
    ACTION_INDEX_CANCEL
};

@interface BoardActionSheet : UIActionSheet
{
    int closeIndex;  // Close the current table.
    int resignIndex;
    int drawIndex;
    int resetIndex;
    int logoutIndex;
    int cancelIndex;
}
- (id)initWithTableState:(NSString *)state delegate:(id<UIActionSheetDelegate>)delegate
                   title:(NSString*)title;
- (NSInteger) valueOfClickedButtonAtIndex:(NSInteger)buttonIndex;
@end

////////////////////////////////////////////////////////////////////
//
// A Position inside a Board.
//
////////////////////////////////////////////////////////////////////

typedef struct Position {
    int row;
    int col; // Column
} Position;

////////////////////////////////////////////////////////////////////
//
// Move replay holder unit
//
////////////////////////////////////////////////////////////////////

@class Piece;

@interface MoveAtom : NSObject
{
    int    move;
    Piece* srcPiece;
    Piece* capturedPiece;
    Piece* checkedKing; // The King that is checked after this Move.
}

@property (nonatomic)         int    move;
@property (nonatomic, assign) Piece* srcPiece;
@property (nonatomic, assign) Piece* capturedPiece;
@property (nonatomic, assign) Piece* checkedKing;

- (id)initWithMove:(int)mv;

@end

////////////////////////////////////////////////////////////////////
//
// TimeInfo
//
////////////////////////////////////////////////////////////////////
@interface TimeInfo : NSObject
{
    int  gameTime;  // Game-time (in seconds).
    int  moveTime;  // Move-time (in seconds).
    int  freeTime;  // Free-time (in seconds).
}

@property (nonatomic) int gameTime;
@property (nonatomic) int moveTime;
@property (nonatomic) int freeTime;

- (id)initWithTime:(TimeInfo*)other;
- (void) decrement;
+ (id)allocTimeFromString:(NSString *)timeContent;

@end

///////////////////////////////////////////////////////////////////////////////
//
//    Utilities functions
//
///////////////////////////////////////////////////////////////////////////////
@interface Utils : NSObject
{
}

+ (UIImage*) imageWithName:(NSString*)name inDirectory:(NSString *)subpath;

@end
