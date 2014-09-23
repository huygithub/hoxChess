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

#import "TableListViewController.h"

// Tags for elements in a Table-Cell.
enum CellLabelEnum {
    LABEL_TAG_ID = 1,  // Have to be non-zero!
    LABEL_TAG_MAIN,
    LABEL_TAG_SUB
};

//------------------------------------------------
@implementation TableInfo

@synthesize tableId;
@synthesize rated;
@synthesize itimes, redTimes, blackTimes;
@synthesize redId, redRating;
@synthesize blackId, blackRating;

+ (id)allocTableFromString:(NSString *)tableContent
{
    TableInfo* newTable = [TableInfo new];
    NSArray* components = [tableContent componentsSeparatedByString:@";"];

    newTable.tableId = [components objectAtIndex:0];
    newTable.rated = [[components objectAtIndex:2] isEqualToString:@"0"];
    newTable.itimes = [components objectAtIndex:3];
    newTable.redTimes = [components objectAtIndex:4];
    newTable.blackTimes = [components objectAtIndex:5];
    newTable.redId = [components objectAtIndex:6];
    newTable.redRating = [components objectAtIndex:7];
    newTable.blackId = [components objectAtIndex:8];
    newTable.blackRating = [components objectAtIndex:9];

    return newTable;
}

@end

//------------------------------------------------
@interface TableListViewController (/* Private interface */)

- (void) _parseTablesStr:(NSString *)tablesStr;
- (void) _addNewTable;

@end

//------------------------------------------------
@implementation TableListViewController

@synthesize addButton;
@synthesize listView;
@synthesize _delegate;
@synthesize viewOnly=_viewOnly;
@synthesize selectedTableId=_selectedTableId;

- (id)initWithDelegate:(id<TableListDelegate>)delegate
{
    if ( (self = [self initWithNibName:@"TableListView" bundle:nil]) ) {
        _tables = [[NSMutableArray alloc] init];
        self._delegate = delegate;
        self.selectedTableId = nil;
    }
    return self;
}

- (void)reinitWithList:(NSString *)tablesStr
{
    [self _parseTablesStr:tablesStr];
    [self.listView reloadData];
    if ([_tables count]) {
        [self.listView scrollToRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:0]
                             atScrollPosition:UITableViewScrollPositionTop animated:YES];
    }
    [_activity stopAnimating];
}

- (void)viewDidLoad
{
    [super viewDidLoad];

    self.title = NSLocalizedString(@"Tables", @"");

    // Create the Add button.
    self.addButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemAdd
                                                              target:self action:@selector(_addNewTable)];
    self.navigationItem.rightBarButtonItem = addButton;
}

- (void)viewWillAppear:(BOOL)animated
{
    //NSLog(@"%s: ENTER.", __FUNCTION__);
    [self.navigationController setNavigationBarHidden:NO animated:YES];
    [_activity startAnimating];
}

- (void)viewWillDisappear:(BOOL)animated
{
    //NSLog(@"%s: ENTER.", __FUNCTION__);
    [_activity stopAnimating];
}

- (void) setViewOnly:(BOOL)value
{
    _viewOnly = value;
    addButton.enabled = !_viewOnly;
}

- (void)didReceiveMemoryWarning {
	// Releases the view if it doesn't have a superview.
    [super didReceiveMemoryWarning];
	
	// Release any cached data, images, etc that aren't in use.
}

- (void)viewDidUnload {
	// Release any retained subviews of the main view.
	// e.g. self.myOutlet = nil;
}

#pragma mark Button methods

- (IBAction) refreshButtonPressed:(id)sender
{
    [_activity startAnimating];
    [_delegate handeRefreshFromList];
}

#pragma mark Table view methods

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}

// Customize the number of rows in the table view.
- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return [_tables count];
}

// Customize the appearance of table view cells.
- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    //NSLog(@"%s: ENTER. indexPath.row = [%d]", __FUNCTION__, indexPath.row);
    static NSString *CellIdentifier = @"TableCell";
    
    UILabel* idLabel = nil;
    UILabel* mainLabel = nil;
    UILabel* subLabel = nil;

    UITableViewCell* cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier];
    if (cell == nil) {
        cell = [[[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:CellIdentifier] autorelease];

        idLabel = [[[UILabel alloc] initWithFrame:CGRectMake(0.0, 5.0, 30.0, 30.0)] autorelease];
        idLabel.tag = LABEL_TAG_ID;
        idLabel.font = [UIFont systemFontOfSize:14.0];
        idLabel.textAlignment = NSTextAlignmentCenter;
        idLabel.textColor = [UIColor blueColor];
        //idLabel.autoresizingMask = UIViewAutoresizingFlexibleRightMargin | UIViewAutoresizingFlexibleWidth;
        [cell.contentView addSubview:idLabel];

        mainLabel = [[[UILabel alloc] initWithFrame:CGRectMake(30.0, 5.0, 250.0, 15.0)] autorelease];
        mainLabel.tag = LABEL_TAG_MAIN;
        mainLabel.font = [UIFont systemFontOfSize:12.0];
        mainLabel.textAlignment = NSTextAlignmentLeft;
        mainLabel.textColor = [UIColor blackColor];
        [cell.contentView addSubview:mainLabel];

        subLabel = [[[UILabel alloc] initWithFrame:CGRectMake(30.0, 25.0, 250.0, 15.0)] autorelease];
        subLabel.tag = LABEL_TAG_SUB;
        subLabel.font = [UIFont systemFontOfSize:10.0];
        subLabel.textAlignment = NSTextAlignmentLeft;
        subLabel.textColor = [UIColor darkGrayColor];
        [cell.contentView addSubview:subLabel];
    } else  {
        idLabel = (UILabel *)[cell.contentView viewWithTag:LABEL_TAG_ID];
        mainLabel = (UILabel *)[cell.contentView viewWithTag:LABEL_TAG_MAIN];
        subLabel = (UILabel *)[cell.contentView viewWithTag:LABEL_TAG_SUB];
    }
    
    // Set up the cell...
    TableInfo* table = [_tables objectAtIndex:indexPath.row];
    NSString* redInfo = ([table.redId length] == 0 ? @"*"
                         : [NSString stringWithFormat:@"%@(%@)", table.redId, table.redRating]);
    NSString* blackInfo = ([table.blackId length] == 0 ? @"*"
                         : [NSString stringWithFormat:@"%@(%@)", table.blackId, table.blackRating]); 

    idLabel.text = table.tableId;
    mainLabel.text = [NSString stringWithFormat:@"%@ vs. %@", redInfo, blackInfo];
    subLabel.text = [NSString stringWithFormat:@"%@  %@", table.rated ? @"Rated" : @"Nonrated", table.itimes];

    if ([table.tableId isEqualToString:_selectedTableId]) {
        cell.accessoryType = UITableViewCellAccessoryCheckmark;
    } else {
        cell.accessoryType = (_viewOnly ? UITableViewCellAccessoryNone
                              : UITableViewCellAccessoryDetailDisclosureButton);
    }

    return cell;
}

- (void)tableView:(UITableView *)tableView accessoryButtonTappedForRowWithIndexPath:(NSIndexPath *)indexPath
{
    //NSLog(@"%s: ENTER. indexPath.row = [%d]", __FUNCTION__, indexPath.row);
    [self tableView:tableView didSelectRowAtIndexPath:indexPath];
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    //NSLog(@"%s: ENTER. indexPath.row = [%d]", __FUNCTION__, indexPath.row);
    TableInfo* table = [_tables objectAtIndex:indexPath.row];

    if (_viewOnly || [table.tableId isEqualToString:_selectedTableId]) {
        return;
    }
    NSString* joinColor = @"None"; // Default: an observer.
    if ([table.redId length] == 0) { joinColor = @"Red"; }
    else if ([table.blackId length] == 0) { joinColor = @"Black"; }
    [_delegate handeTableJoin:table color:joinColor];
}

- (void)dealloc
{
    self.addButton = nil;
    self.listView = nil;
    [_tables release];
    self._delegate = nil;
    [_selectedTableId release];
    [super dealloc];
}

///////////////////////////////////////////////////////////////////////////////
//
//    Implementation of Private methods
//
///////////////////////////////////////////////////////////////////////////////

#pragma mark -
#pragma mark Private methods

- (void) _parseTablesStr:(NSString *)tablesStr
{
    if (!tablesStr || [tablesStr length] == 0) {
        return;
    }
    [_tables removeAllObjects];
    NSArray* entries = [tablesStr componentsSeparatedByString:@"\n"];
    for (NSString *entry in entries) {
        TableInfo* newTable = [TableInfo allocTableFromString:entry];
        [_tables addObject:newTable];
        [newTable release];
    }
}

- (void) _addNewTable
{
    [_delegate handeNewFromList];
}

@end

