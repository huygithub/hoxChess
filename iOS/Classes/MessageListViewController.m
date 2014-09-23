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

#import "MessageListViewController.h"

//------------------------------------------------
@implementation MessageInfo
@synthesize sender, msg, time;

- (id)init
{
    if ( (self = [super init]) ) {
        self.time = [NSDate date];
    }
    return self;
}

@end

//------------------------------------------------
@interface MessageListViewController (/* Private interface */)

- (void) _addNewMessage;

@end

//------------------------------------------------
@implementation MessageListViewController

@synthesize tableId, nNew;
@synthesize addButton;
@synthesize listView;
@synthesize delegate;
@synthesize _dateFormatter;


- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    if ( (self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil]) )
    {
        self.tableId = nil;
        nNew = 0;
        _messages = [[NSMutableArray alloc] init];
        _dateFormatter = [[NSDateFormatter alloc] init];
        [_dateFormatter setDateStyle:NSDateFormatterShortStyle];
        [_dateFormatter setTimeStyle:kCFDateFormatterMediumStyle];
        _inputController = nil;
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];

    self.title = NSLocalizedString(@"Messages", @"");

    // Create the Add button.
    self.addButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemAdd
                                                              target:self action:@selector(_addNewMessage)];
    self.navigationItem.rightBarButtonItem = addButton;
}

- (void)viewWillAppear:(BOOL)animated
{
    [self.navigationController setNavigationBarHidden:NO animated:YES];
}

- (void)viewDidAppear:(BOOL)animated 
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    nNew = 0;
}

- (void) setTableId:(NSString *)id
{
    tableId = id;
    self.navigationItem.rightBarButtonItem = (tableId ? addButton : nil);
}

- (void)didReceiveMemoryWarning {
	// Releases the view if it doesn't have a superview.
    [super didReceiveMemoryWarning];
	
	// Release any cached data, images, etc that aren't in use.
}

#pragma mark Table view methods

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}

// Customize the number of rows in the table view.
- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    NSLog(@"%s: ENTER. section = [%ld]", __FUNCTION__, (long)section);
    return [_messages count];
}

// Customize the appearance of table view cells.
- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSLog(@"%s: ENTER. indexPath.row = [%ld]", __FUNCTION__, (long)indexPath.row);
    static NSString *CellIdentifier = @"MessageCell";
    
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier];
    if (cell == nil) {
        cell = [[[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:CellIdentifier] autorelease];
        cell.textLabel.font = [UIFont systemFontOfSize:12];
        cell.detailTextLabel.font = [UIFont systemFontOfSize:10];
    }
    
    // Set up the cell...
    MessageInfo* message = [_messages objectAtIndex:indexPath.row];
    cell.textLabel.text = [NSString stringWithFormat:@"%@: %@", message.sender, message.msg];
    cell.detailTextLabel.text = [_dateFormatter stringFromDate:message.time];
    cell.accessoryType = UITableViewCellAccessoryNone;

    return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSLog(@"%s: ENTER. indexPath.row = [%ld]", __FUNCTION__, (long)indexPath.row);
}

- (void)dealloc
{
    [tableId release];
    self.addButton = nil;
    self.listView = nil;
    [_messages release];
    [delegate release];
    [_dateFormatter release];
    [_inputController release];
    [super dealloc];
}

- (void) newMessage:(NSString*)msg from:(NSString*)pid
{
    MessageInfo* message = [MessageInfo new];
    message.sender = pid;
    message.msg = msg;
    [_messages addObject:message];
    [message release];
    [self.listView reloadData];
    [self.listView scrollToRowAtIndexPath:[NSIndexPath indexPathForRow:([_messages count]-1) inSection:0]
                         atScrollPosition:UITableViewScrollPositionTop animated:YES];
    if (self != [self.navigationController topViewController]) {
        ++nNew;
    }
}

- (void) handleItemInput:(NSString *)button input:(NSString*)input
{
    [self dismissViewControllerAnimated:YES completion:nil];
    if (button) {
        [delegate handeNewMessageFromList:input];
    }
}

///////////////////////////////////////////////////////////////////////////////
//
//    Implementation of Private methods
//
///////////////////////////////////////////////////////////////////////////////

#pragma mark -
#pragma mark Private methods

- (void) _addNewMessage
{
    if (_inputController == nil) {
        _inputController = [[ItemInputViewController alloc] initWithNibName:@"ItemInputView" bundle:nil];
        _inputController.delegate = self;
    }
    UINavigationController* navigationController = [[UINavigationController alloc] initWithRootViewController:_inputController];
    [[self navigationController] presentViewController:navigationController animated:YES completion:nil];
    [navigationController release];
}

@end

