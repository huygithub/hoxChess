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
#import "ItemInputViewController.h"

// --------------------------------------
@interface MessageInfo : NSObject
{
    NSString* sender;
    NSString* msg;
    NSDate* time;
}

@property (nonatomic, retain) NSString* sender;
@property (nonatomic, retain) NSString* msg;
@property (nonatomic, retain) NSDate* time;

@end

// --------------------------------------
@protocol MessageListDelegate <NSObject>
- (void) handeNewMessageFromList:(NSString*)msg;
@end

// --------------------------------------
@interface MessageListViewController : UIViewController <UITableViewDelegate, UITableViewDataSource, ItemInputDelegate>
{
    NSString*             tableId;
    int                   nNew; // The number of new messages

    UIBarButtonItem*      addButton;
    IBOutlet UITableView* listView;

    NSMutableArray* _messages;
    id <MessageListDelegate> delegate;
    
    NSDateFormatter* _dateFormatter;
    ItemInputViewController* _inputController;
}

@property (nonatomic, retain) NSString* tableId;
@property (readonly) int nNew;
@property (nonatomic, retain) IBOutlet UIBarButtonItem* addButton;
@property (nonatomic, retain) IBOutlet UITableView* listView;
@property (nonatomic, retain) id <MessageListDelegate> delegate;
@property (nonatomic, retain) NSDateFormatter* _dateFormatter;

- (void) newMessage:(NSString*)msg from:(NSString*)pid;

@end
