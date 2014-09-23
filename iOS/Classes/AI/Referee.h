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

#import <Foundation/Foundation.h>


//
// Referee 's error codes (or Return-Codes).
//
#define HC_RC_REF_UNKNOWN       -1
#define HC_RC_REF_OK             0  // A generic success
#define HC_RC_REF_ERR            1  // A generic error

//
// Constants required as a result of porting XQWLight source code.
//
#define MAX_GEN_MOVES      128
#define SRC(mv)            ((mv) & 255)
#define DST(mv)            ((mv) >> 8)
#define MOVE(sqSrc, sqDst) ((sqSrc) + (sqDst) * 256)

#define MATE_VALUE  10000
#define WIN_VALUE   (MATE_VALUE - 200)

//
// The Referee to judge a given Game.
//
@interface Referee : NSObject
{
    // Empty
}

- (id)   init;
- (int)  initGame;
- (int)  generateMoveFrom:(int)sqSrc moves:(int*)moves;
- (BOOL) isLegalMove:(int)move; 

- (void) makeMove:(int)move captured:(int*) ppcCaptured;
- (int) repStatus:(int)nRecur repValue:(int*)pRepVal;
- (BOOL) isChecked;
- (BOOL) isMate;
- (int) get_nMoveNum;
- (int) get_sdPlayer;

@end
