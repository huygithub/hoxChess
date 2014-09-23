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

#import "AI_XQWLight.h"

#define XQWLight_DEFAULT_LEVEL 3  // AI Default difficulty level

// Declarations of methods defined under "XQWLight.cpp"
extern void XQWLight_init_engine( int searchDepth );
extern void XQWLight_init_game();
extern void XQWLight_generate_move( int* pRow1, int* pCol1, int* pRow2, int* pCol2 );
extern void XQWLight_on_human_move( int row1, int col1, int row2, int col2 );
extern void XQWLight_load_book( const char *bookfile );

///////////////////////////////////////////////////////////////////////////////
//
//    Implementation of Public methods
//
///////////////////////////////////////////////////////////////////////////////

@implementation AI_XQWLight

- (void)dealloc
{
    [super dealloc];
}

- (id) init
{
    self = [super init];
    if (self != nil) {
        [self setDifficultyLevel:XQWLight_DEFAULT_LEVEL];
        //XQWLight_InitEngine();
    }
    return self;
}

- (int) setDifficultyLevel: (int)nAILevel
{
    int searchDepth = 1;

    if      ( nAILevel > 10 ) searchDepth = 10;
    else if ( nAILevel < 1 )  searchDepth = 1;
    else                      searchDepth = nAILevel;

    XQWLight_init_engine( searchDepth );
    return AI_RC_OK;
}

- (int) initGame
{
    XQWLight_init_game();
    const char *szBookPath =
        [[[NSBundle mainBundle] pathForResource:@"BOOK.DAT"
                                         ofType:nil
                                    inDirectory:@"books"] UTF8String];
    XQWLight_load_book(szBookPath);
    return AI_RC_OK;
}

- (int) generateMove:(int*)pRow1 fromCol:(int*)pCol1
               toRow:(int*)pRow2 toCol:(int*)pCol2
{
    XQWLight_generate_move( pCol1, pRow1, pCol2, pRow2 );
    return AI_RC_OK;
}

- (int) onHumanMove:(int)row1 fromCol:(int)col1
              toRow:(int)row2 toCol:(int)col2
{
    XQWLight_on_human_move( col1, row1, col2, row2 );
    return AI_RC_OK;
}

- (NSString *) getInfo
{
    return @"Morning Yellow\n"
            "www.elephantbase.net";
}

@end
