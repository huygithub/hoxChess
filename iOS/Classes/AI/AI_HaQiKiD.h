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


@interface AI_HaQiKiD : AIEngine
{
    // Empty.
}

- (id) init;
- (int) setDifficultyLevel: (int)nAILevel;
- (int) initGame;
- (int) generateMove:(int*)pRow1 fromCol:(int*)pCol1
               toRow:(int*) pRow2 toCol:(int*) pCol2;
- (int) onHumanMove:(int)row1 fromCol:(int)col1
              toRow:(int)row2 toCol:(int)col2;
- (NSString *) getInfo;

@end
