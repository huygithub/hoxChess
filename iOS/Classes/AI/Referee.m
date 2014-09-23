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

#import "Referee.h"

// Declarations of methods defined under "XQWLight_Referee.cpp"
extern void Referee_init_game();
extern int  Referee_generate_move_from( int sqSrc, int *mvs );
extern int  Referee_is_legal_move( int mv );

extern void Referee_make_move( int mv, int* ppcCaptured );
extern int  Referee_rep_status(int nRecur, int *repValue);
extern int  Referee_is_checked();
extern int  Referee_is_mate();
extern int  Referee_get_nMoveNum();
extern int  Referee_get_sdPlayer();

///////////////////////////////////////////////////////////////////////////////
//
//    Implementation of Public methods
//
///////////////////////////////////////////////////////////////////////////////

@implementation Referee

- (void)dealloc
{
    [super dealloc];
}

- (id) init
{
    self = [super init];
    return self;
}

- (int) initGame
{
    Referee_init_game();
    return HC_RC_REF_OK;
}

- (int) generateMoveFrom:(int)sqSrc moves:(int*)moves
{
    return Referee_generate_move_from(sqSrc, moves);
}

- (BOOL) isLegalMove:(int)move
{
    int bLegal = Referee_is_legal_move( move );
    return ( bLegal == 1 ? YES : NO );
}

- (void) makeMove:(int)move captured:(int*) ppcCaptured
{
    Referee_make_move(move, ppcCaptured);
}

- (int) repStatus:(int)nRecur repValue:(int*)pRepVal
{
    return Referee_rep_status(nRecur, pRepVal);
}

- (BOOL) isChecked
{
    return (Referee_is_checked() ? YES : NO);
}

- (BOOL) isMate
{
    return (Referee_is_mate() ? YES : NO);
}

- (int) get_nMoveNum
{
    return Referee_get_nMoveNum();
}

- (int) get_sdPlayer
{
    return Referee_get_sdPlayer();
}

@end
