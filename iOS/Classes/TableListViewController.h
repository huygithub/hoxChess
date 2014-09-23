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

@interface TableInfo : NSObject
{
    NSString* tableId;
    BOOL      rated;
    NSString* itimes;   // Initial times.
    NSString* redTimes;
    NSString* blackTimes;
    NSString* redId;
    NSString* redRating;
    NSString* blackId;
    NSString* blackRating;
}

@property (nonatomic, retain) NSString* tableId;
@property (nonatomic)         BOOL rated;
@property (nonatomic, retain) NSString* itimes;
@property (nonatomic, retain) NSString* redTimes;
@property (nonatomic, retain) NSString* blackTimes;
@property (nonatomic, retain) NSString* redId;
@property (nonatomic, retain) NSString* redRating;
@property (nonatomic, retain) NSString* blackId;
@property (nonatomic, retain) NSString* blackRating;

+ (id)allocTableFromString:(NSString *)tableContent;

@end

// --------------------------------------
@protocol TableListDelegate <NSObject>
- (void) handeNewFromList;
- (void) handeRefreshFromList;
- (void) handeTableJoin:(TableInfo *)table color:(NSString*)joinColor;
@end

// --------------------------------------
@interface TableListViewController : UIViewController <UITableViewDelegate, UITableViewDataSource>
{
    UIBarButtonItem*      addButton;
    IBOutlet UITableView* listView;
    IBOutlet UIActivityIndicatorView* _activity;

    NSMutableArray*       _tables;
    id<TableListDelegate> _delegate;
    BOOL                  _viewOnly;
    NSString*             _selectedTableId;
}

@property (nonatomic, retain) IBOutlet UIBarButtonItem* addButton;
@property (nonatomic, retain) IBOutlet UITableView* listView;
@property (nonatomic, retain) id<TableListDelegate> _delegate;
@property (nonatomic)         BOOL viewOnly;
@property (nonatomic, retain) NSString* selectedTableId;

- (IBAction) refreshButtonPressed:(id)sender;

- (id)initWithDelegate:(id<TableListDelegate>)delegate;
- (void)reinitWithList:(NSString *)tablesStr;

@end
