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

@class SingleSelectionController;

// --------------------------------------
@protocol SingleSelectionDelegate <NSObject>
- (void) didSelect:(SingleSelectionController*)controller rowAtIndex:(NSUInteger)index;
@end

// --------------------------------------
@interface SingleSelectionController : UITableViewController
{
    id<SingleSelectionDelegate> _delegate;
    int                         _tag;

    NSArray*         _choices;
    NSArray*         _imageNames;
    NSArray*         _subTitles;
    unsigned int     _selectionIndex;
}

@property (nonatomic, retain) id<SingleSelectionDelegate> _delegate;
@property (nonatomic)         int tag;
@property (nonatomic)         unsigned int selectionIndex;
@property (nonatomic)         CGFloat rowHeight;

- (id) initWithChoices:(NSArray*)choices
              delegate:(id<SingleSelectionDelegate>)delegate;

- (id) initWithChoices:(NSArray*)choices imageNames:(NSArray*)imageNames
              delegate:(id<SingleSelectionDelegate>)delegate;

- (id) initWithChoices:(NSArray*)choices subTitles:(NSArray*)subTitles
              delegate:(id<SingleSelectionDelegate>)delegate;

- (id) initWithChoices:(NSArray*)choices
            imageNames:(NSArray*)imageNames
             subTitles:(NSArray*)subTitles
              delegate:(id<SingleSelectionDelegate>)delegate;

@end
