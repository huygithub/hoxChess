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

#import "AI_HaQiKiD.h"


#define HaQiKiD_DEFAULT_LEVEL 3  /* AI Default defficulty level */

/** Declarations of methods defined under "AI_haqikidHOX.c" */
extern void        HaQiKiD_InitEngine();
extern void        HaQiKiD_InitGame();
extern void        HaQiKiD_SetMaxDepth( int searchDepth );
extern void        HaQiKiD_OnOpponentMove(const char *line);
extern const char* HaQiKiD_GenerateNextMove();


///////////////////////////////////////////////////////////////////////////////
//
//    Private methods
//
///////////////////////////////////////////////////////////////////////////////

//
// Convert our Move to an 'HaQiKiD' Move. 
//
static char*
_moveToHaQiKiD( int row1, int col1, int row2, int col2 )
{
    static char szMove[5] = {0, 0, 0, 0, 0 }; // NOTE: Single thread only.
    szMove[0] = ('a' + col1);
    szMove[1] = ('9' - row1);
    szMove[2] = ('a' + col2);
    szMove[3] = ('9' - row2);
    return szMove;
}

//
// Convert an 'HaQiKiD' Move to our Move. 
//
static void
_HaQiKiDToMove( const char* szMove,
                int* pRow1, int* pCol1, int* pRow2, int* pCol2 )
{
    *pCol1 = szMove[0] - 'a';
    *pRow1 = '9' - szMove[1];
    *pCol2 = szMove[2] - 'a';
    *pRow2 = '9' - szMove[3];
}

///////////////////////////////////////////////////////////////////////////////
//
//    Implementation of Public methods
//
///////////////////////////////////////////////////////////////////////////////

@implementation AI_HaQiKiD

- (void)dealloc
{
    [super dealloc];
}

- (id) init
{
    self = [super init];
    if (self != nil) {
        [self setDifficultyLevel:HaQiKiD_DEFAULT_LEVEL];
        HaQiKiD_InitEngine();
    }
    return self;
}

- (int) setDifficultyLevel: (int)nAILevel
{
    int searchDepth = 1;

    if      ( nAILevel > 10 ) searchDepth = 10;
    else if ( nAILevel < 1 )  searchDepth = 1;
    else                      searchDepth = nAILevel;

    HaQiKiD_SetMaxDepth( searchDepth );
    return AI_RC_OK;
}

- (int) initGame
{
    HaQiKiD_InitGame();
    return AI_RC_OK;
}

- (int) generateMove:(int*)pRow1 fromCol:(int*)pCol1
               toRow:(int*)pRow2 toCol:(int*)pCol2
{
    const char* aiMove = HaQiKiD_GenerateNextMove();
    _HaQiKiDToMove( aiMove, pRow1, pCol1, pRow2, pCol2 );
//#ifdef DEBUG
//    printf("[%s] from r%d c%d to r%d c%d\n", __func__, *pRow1, *pCol1, *pRow2, *pCol2);
//#endif
    return AI_RC_OK;
}

- (int) onHumanMove:(int)row1 fromCol:(int)col1
              toRow:(int)row2 toCol:(int)col2
{
    const char *szMove = _moveToHaQiKiD( row1, col1, row2, col2 );
//#ifdef DEBUG
//    printf("[%s] from r%d c%d to r%d c%d\n", __func__, row1, col1, row2, col2);
//#endif
    HaQiKiD_OnOpponentMove(szMove);
    return AI_RC_OK;
}

- (NSString *) getInfo
{
    return @"H.G. Muller\n"
            "home.hccnet.nl/h.g.muller/XQhaqikid.html";
}

@end
