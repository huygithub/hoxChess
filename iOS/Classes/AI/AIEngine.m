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

#import "AIEngine.h"


@implementation AIEngine

- (void)dealloc
{
    [super dealloc];
}

#pragma mark -
#pragma mark AI METHODS TO BE OVERRIDDEN:

- (id) init
{
    self = [super init];
    return self;
}

- (int) setDifficultyLevel: (int)nAILevel
{
    return AI_RC_OK;
}

- (int) initGame
{
    return AI_RC_OK;
}

- (int) generateMove:(int*)pRow1 fromCol:(int*)pCol1
               toRow:(int*)pRow2 toCol:(int*)pCol2
{
    return AI_RC_OK;
}

- (int) onHumanMove:(int)row1 fromCol:(int)col1
              toRow:(int)row2 toCol:(int)col2
{
    return AI_RC_OK;
}

- (NSString *) getInfo
{
    return @"Some unknown AI written by someone";
}

@end
