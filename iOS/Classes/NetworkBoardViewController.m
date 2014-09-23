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

#import "NetworkBoardViewController.h"
#import "Types.h"

@interface NetworkBoardViewController (/* Private interface */)

- (void) _handleCommandLogout;
- (void) _connectToNetwork;
- (void) _sendLoginInfo:(NSString*)username password:(NSString*)password;
- (void) _showLoginView:(NSString*)errorStr;
- (void) _showListTableView:(NSString*)event;
- (void) _dismissLoginView;
- (void) _dismissListTableView;
- (void) _resetAndClearTable;
- (void) _animateEmptyBoard;
- (void) _onNewMessage:(NSString*)msg from:(NSString*)pid;
- (void) _onMyRatingUpdated:(NSString*)newRating;
- (NSString*) _getLocalizedLoginError:(int)code defaultError:(NSString*)error;

- (NSMutableDictionary*) _allocNewEvent:(NSString*)event;
- (void) _handleNetworkEvent_LOGIN:(int)code withContent:(NSString*)event;
- (void) _handleNetworkEvent_LIST:(NSString*)event;
- (void) _handleNetworkEvent_I_TABLE:(NSString*)event;
- (void) _handleNetworkEvent_I_MOVES:(NSString*)event;
- (void) _handleNetworkEvent_MOVE:(NSString*)event;
- (void) _handleNetworkEvent_E_END:(NSString*)event;
- (void) _handleNetworkEvent_RESET:(NSString*)event;
- (void) _handleNetworkEvent_E_JOIN:(NSString*)event;
- (void) _handleNetworkEvent_LEAVE:(NSString*)event;
- (void) _handleNetworkEvent_UPDATE:(NSString*)event;
- (void) _handleNetworkEvent_MSG:(NSString*)event;
- (void) _handleNetworkEvent_DRAW:(NSString*)event;
- (void) _handleNetworkEvent_INVITE:(NSString*)event toTable:(NSString*)tableId;
- (void) _handleNetworkEvent_E_SCORE:(NSString*)event;
- (void) _handleNetworkEvent_PLAYER_INFO:(NSString*)event;

- (NSString*) _generateGuestUserName;
- (int) _generateRandomNumber:(unsigned int)max_value;

@end

///////////////////////////////////////////////////////////////////////////////
//
//    Implementation of Public methods
//
///////////////////////////////////////////////////////////////////////////////

@implementation NetworkBoardViewController

@synthesize _board;
@synthesize _game;
@synthesize _tableId;
@synthesize ownerColor=_myColor;
@synthesize _username, _password, _rating;
@synthesize _redId, _blackId;
@synthesize _connection;
@synthesize _loginController;
@synthesize _tableListController;
@synthesize _messageListController;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    if ( (self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil]) ) {
        // Empty.
    }
    
    return self;
}

- (void) prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    NSString *segueName = segue.identifier;
    if ([segueName isEqualToString: @"boardview_embed"]) {
        NSLog(@"%s: ... Obtain [boardview]", __FUNCTION__);
        _board = [(BoardViewController *) [segue destinationViewController] retain];
        _board.boardOwner = self;
    }
}

- (void)viewDidLoad
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    [super viewDidLoad];

    _actionButton.enabled = NO;
    _messagesButton.enabled = NO;

    self._username = nil;
    self._password = nil;
    self._rating = nil;
    _redId = nil;
    _blackId = nil;
    _isGameOver = NO;
    _loginCanceled = NO;
    _loginAuthenticated = NO;
    _logoutPending = NO;
    self._loginController = nil;
    self._tableListController = nil;

    _messageListController = [[MessageListViewController alloc] initWithNibName:@"MessageListView" bundle:nil];
    _messageListController.delegate = self;

    self._connection = nil;
    [self.view bringSubviewToFront:_mainView];
    self._game = _board.game;

    _tableId = nil;
    _myColor = HC_COLOR_UNKNOWN;
} 

- (void)viewWillAppear:(BOOL)animated
{
    //NSLog(@"%s: ENTER.", __FUNCTION__);
    [super viewWillAppear:animated];

    [self.navigationController setNavigationBarHidden:YES animated:YES];
    _messagesButton.title = @"0";
}

- (void)viewDidAppear:(BOOL)animated 
{
    //NSLog(@"%s: ENTER.", __FUNCTION__);
    [super viewDidAppear:animated];

    if (!_loginAuthenticated && !_loginCanceled)
    {
        BOOL autoConnect = [[NSUserDefaults standardUserDefaults] boolForKey:@"network_autoConnect"];
        NSString* username = [[NSUserDefaults standardUserDefaults] stringForKey:@"network_username"];
        if (autoConnect && username && [username length]) {
            NSString* password = [[NSUserDefaults standardUserDefaults] stringForKey:@"network_password"];
            NSLog(@"%s: Auto-Connect with LOGIN [%@, %@].", __FUNCTION__, username, password);
            [self _sendLoginInfo:username password:password];
        }
        else {
            NSLog(@"%s: Show the Login view...", __FUNCTION__);
            [self _showLoginView:nil];
        }
    }
}

- (void) didReceiveMemoryWarning
{
	// Releases the view if it doesn't have a superview.
    [super didReceiveMemoryWarning];

	// Release any cached data, images, etc that aren't in use.
    self._loginController = nil;
    self._tableListController = nil;
}

- (void)dealloc
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    [_username release];
    [_password release];
    [_rating release];
    self._connection = nil;
    [_redId release];
    [_blackId release];
    self._loginController = nil;
    self._tableListController = nil;
    self._messageListController = nil;
    self._board = nil;
    self._tableId = nil;
    [super dealloc];
}


- (IBAction)homePressed:(id)sender
{
    if (!_loginAuthenticated || !_username) {
        [self goBackToHomeMenu];
        return;
    }

    NSString* title = [NSString stringWithFormat:@"%@ (%@)", _username, _rating];
    NSString* state = @"logout";
    BoardActionSheet* actionSheet = [[BoardActionSheet alloc] initWithTableState:state delegate:self title:title];
    [actionSheet showInView:self.view];
    [actionSheet release];
}

- (void) goBackToHomeMenu
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    [_board.view removeFromSuperview];
    [_board destroyTimer];
    self._board = nil;
    _messageListController.delegate = nil;
    _loginController.delegate = nil;
    _tableListController._delegate = nil;
    [self.navigationController popViewControllerAnimated:YES];
}

- (void) _handleCommandLogout
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    if (_connection == nil) {
        [self goBackToHomeMenu];
    }
    else if (_loginAuthenticated) {
        _logoutPending = YES;
        [_connection send_LOGOUT];
    } else {
        NSLog(@"%s: Disconnect the network connection...", __FUNCTION__);
        [_connection disconnect];
        self._connection = nil;
        [self goBackToHomeMenu];
    }

    // !!!!!!!!!!!!!!!!!!!
    // NOTE: Let the handler for the 'NSStreamEventEndEncountered' event
    //       take care of closing the IO streams.
    // !!!!!!!!!!!!!!!!!!!
}

- (IBAction)searchPressed:(id)sender
{
    if (!_loginAuthenticated) {
        [self _showLoginView:nil];
        return;
    }
    [self _showListTableView:nil];
    [_connection send_LIST];
}

- (void)actionSheet:(UIActionSheet *)actionSheet clickedButtonAtIndex:(NSInteger)buttonIndex
{
    //NSLog(@"%s: ENTER. buttonIndex = [%d].", __FUNCTION__, buttonIndex);
    BoardActionSheet* boardActionSheet = (BoardActionSheet*)actionSheet;
    NSInteger buttonValue = [boardActionSheet valueOfClickedButtonAtIndex:buttonIndex];

    if (buttonValue == ACTION_INDEX_LOGOUT) {
        [self _handleCommandLogout];
        return;
    }
    if (!_tableId) {
        NSLog(@"%s: No current table. Do nothing.", __FUNCTION__);
        return;
    }

    switch (buttonValue)
    {
        case ACTION_INDEX_CLOSE:
            [_connection send_LEAVE:_tableId];
            break;
        case ACTION_INDEX_RESIGN:
            [_connection send_RESIGN:_tableId];
            break;
        case ACTION_INDEX_DRAW:
            [_connection send_DRAW:_tableId];
            break;
        case ACTION_INDEX_RESET:
            [_connection send_RESET:_tableId];
            break;
        default:
            break; // Do nothing.
    };
}

- (IBAction)actionPressed:(id)sender
{
    NSString* state = @"";
    NSString* title = nil;

    if (_tableId)
    {
        title = [NSString stringWithFormat:NSLocalizedString(@"Table #%@", @""), _tableId];
        if (_myColor == HC_COLOR_RED || _myColor == HC_COLOR_BLACK)
        {
            if (_isGameOver) {
                state = @"ended";
            } else {
                state = ([_game getMoveCount] < 2 ? @"ready" : @"play");
            }
        } else {
            state = @"view";
        }
    }

    BoardActionSheet* actionSheet = [[BoardActionSheet alloc] initWithTableState:state delegate:self title:title];
    [actionSheet showInView:self.view];
    [actionSheet release];
}

- (IBAction)messagesPressed:(id)sender
{
    [self.navigationController pushViewController:_messageListController animated:YES];
    [_messageListController setTableId:_tableId];
}

- (void) onLocalMoveMadeFrom:(Position)from toPosition:(Position)to
{
    // Send over the network.
    NSString* moveStr = [NSString stringWithFormat:@"%d%d%d%d",
                         from.col, from.row, to.col, to.row];
    [_connection send_MOVE:_tableId move:moveStr];
}

- (BOOL) isMyTurnNext
{
    return (_game.nextColor == _myColor);
}

- (BOOL) isGameReady
{
    return ( !_isGameOver && _redId && _blackId );
}

#pragma mark -
#pragma mark Table view methods

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 3;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section
{
    switch (section) {
        case 0: return NSLocalizedString(@"Server", @"");
        case 1: return NSLocalizedString(@"Account", @"");
    }
    return nil;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    switch (section) {
        case 0: return 1;
        case 1: return 3;
        case 2: return 1;
    }
    return 0;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    UITableViewCell* cell = nil;
    
    switch (indexPath.section)
    {
        case 0: // ----- Server
        {
            static NSString* cellId = @"ServerCell";
            cell = [tableView dequeueReusableCellWithIdentifier:cellId];
            if (!cell) {
                cell = [[[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:cellId] autorelease];
                cell.textLabel.text = @"www.PlayXiangqi.com";
                cell.selectionStyle = UITableViewCellSelectionStyleNone;
            }
            break;
        }
        case 1: // ----- User
        {
            switch (indexPath.row)
            {
                case 0:
                {
                    static NSString* cellId = @"UserCell";
                    cell = [tableView dequeueReusableCellWithIdentifier:cellId];
                    if (!cell) {
                        cell = [[[UITableViewCell alloc] initWithStyle:UITableViewCellStyleValue2 reuseIdentifier:cellId] autorelease];
                        cell.textLabel.text = NSLocalizedString(@"Username", @"");
                    }
                    cell.detailTextLabel.text = _username;
                    break;
                }
                case 1:
                {
                    static NSString* cellId = @"RatingCell";
                    cell = [tableView dequeueReusableCellWithIdentifier:cellId];
                    if (!cell) {
                        cell = [[[UITableViewCell alloc] initWithStyle:UITableViewCellStyleValue2 reuseIdentifier:cellId] autorelease];
                        cell.textLabel.text = NSLocalizedString(@"Rating", @"");                    }
                    cell.detailTextLabel.text = _rating;
                    break;
                }
                case 2:
                {
                    static NSString* cellId = @"StatisticsCell";
                    cell = [tableView dequeueReusableCellWithIdentifier:cellId];
                    if (!cell) {
                        cell = [[[UITableViewCell alloc] initWithStyle:UITableViewCellStyleValue2 reuseIdentifier:cellId] autorelease];
                        cell.textLabel.text = NSLocalizedString(@"Statistics", @"");
                    }
                    _infoCell = cell;
                    break;
                }
            }
            cell.selectionStyle = UITableViewCellSelectionStyleNone;
            break;
        }
        case 2: // ----- Search
        {
            static NSString* cellId = @"SearchOrLoginCell";
            cell = [tableView dequeueReusableCellWithIdentifier:cellId];
            if (!cell) {
                cell = [[[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:cellId] autorelease];
                cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
                cell.selectionStyle = UITableViewCellSelectionStyleNone;
            }
            
            if (_loginAuthenticated) {
                cell.textLabel.text = NSLocalizedString(@"Select a Table to join", @"");
            } else {
                cell.textLabel.text = NSLocalizedString(@"Not connected to network", @"");
            }
            break;
        }
    }
    
    return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath 
{    
    switch (indexPath.section)
    {
        case 2: // ----- Search
        {
            [self searchPressed:nil];
            break;
        }
    }
}

#pragma mark -
#pragma mark Delegate callback functions

- (void) handleLoginRequest:(NSString *)button username:(NSString*)name password:(NSString*)passwd
{
    NSLog(@"%s: ENTER.", __FUNCTION__);

    if (button == nil) // "Cancel" button clicked?
    {
        NSLog(@"%s: Login got canceled.", __FUNCTION__);
        _loginCanceled = YES;
        [self _dismissLoginView];
        return;
    }

    NSLog(@"%s: Username = [%@:%@]", __FUNCTION__, name, passwd);
    if ([button isEqualToString:@"guest"])
    {
        name = [self _generateGuestUserName];
        NSLog(@"%s: Generated a Guest username: [%@].", __FUNCTION__, name);
    }
    [self _sendLoginInfo:name password:passwd];
}

- (void) handeNewFromList
{
    [self _dismissListTableView];
    if (_tableId) {
        [_connection send_LEAVE:_tableId]; // Leave the old table.
    }
    [_connection send_NEW:@"900/180/20"];
}

- (void) handeTableJoin:(TableInfo *)table color:(NSString*)joinColor
{
    [self _dismissListTableView];
    if ([_tableId isEqualToString:table.tableId]) {
        NSLog(@"%s: Same table [%@]. Ignore request.", __FUNCTION__, table.tableId);
        return;
    }

    if (_tableId) {
        [_connection send_LEAVE:_tableId]; // Leave the old table.
    }
    [_connection send_JOIN:table.tableId color:joinColor];
}

- (void) handeRefreshFromList
{
    // DO NOT dismiss the existing List-of-Tables view!
    [_connection send_LIST];
}

- (void) handeNewMessageFromList:(NSString*)msg
{
    if (!_username || !_tableId) {
        NSLog(@"%s: No current table. Do nothing.", __FUNCTION__);
        return;
    }
    [self _onNewMessage:msg from:_username];
    [_connection send_MSG:_tableId msg:msg];
}

- (void) _connectToNetwork
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    if (!_connection) {
        NSLog(@"%s: Connecting to network...", __FUNCTION__);
        [_activity startAnimating];
        _connection = [[NetworkConnection alloc] init];
        _connection.delegate = self;
        [_connection connect];
    }
}

- (void) _sendLoginInfo:(NSString*)username password:(NSString*)password
{
    self._username = username;
    self._password = password;
    [self _connectToNetwork]; // Connect if needed.
    [_connection setLoginInfo:_username password:_password];
    [_connection send_LOGIN];
}

- (void) _showLoginView:(NSString*)errorStr
{
    //NSLog(@"%s: ENTER.", __FUNCTION__);
    if (!_loginController) {
        NSLog(@"%s: Creating new Login view...", __FUNCTION__);
        _loginController = [[LoginViewController alloc] initWithNibName:@"LoginView" bundle:nil];
        _loginController.delegate = self;
    }
    [_loginController setErrorString:errorStr];

    UIViewController* topController = [self.navigationController topViewController];
    if (topController != _loginController) {
        [self.navigationController pushViewController:_loginController animated:YES];
    }

    _loginCanceled = NO;
}

- (void) _showListTableView:(NSString*)event
{
    //NSLog(@"%s: ENTER.", __FUNCTION__);
    if (!_tableListController) {
        _tableListController = [[TableListViewController alloc] initWithDelegate:self];
    }
    if (event) {
        [_tableListController reinitWithList:event];
    }

    UIViewController* topController = [self.navigationController topViewController];
    if (topController != _tableListController) {
        [self.navigationController pushViewController:_tableListController animated:YES];
    }

    _tableListController.viewOnly =
        ( _tableId
         && ([_username isEqualToString:_redId] || [_username isEqualToString:_blackId]));
    _tableListController.selectedTableId = _tableId;
}

- (void) _dismissLoginView
{
    [self.navigationController popToViewController:self animated:YES];
    [_activity stopAnimating];
}

- (void) _dismissListTableView
{
    [self.navigationController popToViewController:self animated:YES];
}

- (void) _resetAndClearTable
{
    [UIView beginAnimations:nil context:NULL];
    [UIView setAnimationDuration:HC_TABLE_ANIMATION_DURATION];
    [UIView setAnimationTransition:(_board.view.superview ? UIViewAnimationTransitionFlipFromRight
                                                          : UIViewAnimationTransitionCurlDown)
                           forView:self.view cache:YES];

    [self.view bringSubviewToFront:_containerView];
    [self.view bringSubviewToFront:_toolbar];
    [self.view bringSubviewToFront:_activity];

    [UIView commitAnimations];

    [_board resetBoard];
    _isGameOver = NO;
}

- (void) _animateEmptyBoard
{
    self._tableId = nil;
    _myColor = HC_COLOR_UNKNOWN;
    _actionButton.enabled = NO;

    [UIView beginAnimations:nil context:NULL];
    [UIView setAnimationDuration:HC_TABLE_ANIMATION_DURATION];
    [UIView setAnimationTransition:UIViewAnimationTransitionCurlUp
                           forView:self.view cache:YES];
    [self.view bringSubviewToFront:_mainView];
    [UIView commitAnimations];
}

- (void) _onNewMessage:(NSString*)msg from:(NSString*)pid
{
    [_messageListController newMessage:msg from:pid];
    _messagesButton.title = [NSString stringWithFormat:@"%d", _messageListController.nNew];
}

- (void) _onMyRatingUpdated:(NSString*)newRating
{
    self._rating = newRating;
    [_mainView reloadData];
}

#pragma mark -
#pragma mark Network-event handers

- (void) handleNetworkEvent:(ConnectionEventEnum)code event:(NSString*)event
{
    switch(code)
    {
        case HC_CONN_EVENT_OPEN:
        {
            NSLog(@"%s: Got HC_CONN_EVENT_OPEN.", __FUNCTION__);
            break;
        }
        case HC_CONN_EVENT_DATA:
        {
            //NSLog(@"%s: A new event [%@].", __FUNCTION__, event);
            NSMutableDictionary* newEvent = [self _allocNewEvent:event];
            NSString* op = [newEvent objectForKey:@"op"];
            int code = (int) [[newEvent objectForKey:@"code"] integerValue];
            NSString* content = [newEvent objectForKey:@"content"];
            NSString* tableId = [newEvent objectForKey:@"tid"];

            if ([op isEqualToString:@"LOGIN"]) {
                [self _handleNetworkEvent_LOGIN:code withContent:content];
            }
            else if (code != 0) {  // Error
                NSLog(@"%s: Received an ERROR event: [%d: %@].", __FUNCTION__, code, content);
            }
            else {
                if ([op isEqualToString:@"LIST"]) {
                    [self _handleNetworkEvent_LIST:content];
                } else if ([op isEqualToString:@"I_TABLE"]) {
                    [self _handleNetworkEvent_I_TABLE:content];
                } else if ([op isEqualToString:@"I_MOVES"]) {
                    [self _handleNetworkEvent_I_MOVES:content];
                } else if ([op isEqualToString:@"MOVE"]) {
                    [self _handleNetworkEvent_MOVE:content];
                } else if ([op isEqualToString:@"E_END"]) {
                    [self _handleNetworkEvent_E_END:content];
                } else if ([op isEqualToString:@"RESET"]) {
                    [self _handleNetworkEvent_RESET:content];
                } else if ([op isEqualToString:@"E_JOIN"]) {
                    [self _handleNetworkEvent_E_JOIN:content];
                } else if ([op isEqualToString:@"LEAVE"]) {
                    [self _handleNetworkEvent_LEAVE:content];
                } else if ([op isEqualToString:@"UPDATE"]) {
                    [self _handleNetworkEvent_UPDATE:content];
                } else if ([op isEqualToString:@"MSG"]) {
                    [self _handleNetworkEvent_MSG:content];
                } else if ([op isEqualToString:@"DRAW"]) {
                    [self _handleNetworkEvent_DRAW:content];
                } else if ([op isEqualToString:@"INVITE"]) {
                    [self _handleNetworkEvent_INVITE:content toTable:tableId];
                } else if ([op isEqualToString:@"E_SCORE"]) {
                    [self _handleNetworkEvent_E_SCORE:content];
                } else if ([op isEqualToString:@"PLAYER_INFO"]) {
                    [self _handleNetworkEvent_PLAYER_INFO:content];
                }
            }

            [newEvent release];
            break;
        }
        case HC_CONN_EVENT_END:
        {
            NSLog(@"%s: Got HC_CONN_EVENT_END.", __FUNCTION__);
            _loginAuthenticated = NO;
            self._connection = nil;
            if (_logoutPending) {
                [self goBackToHomeMenu];
            } else {
                [self _showLoginView:nil];
            }
            break;
        }
        case HC_CONN_EVENT_ERROR:
        {
            NSLog(@"%s: Got HC_CONN_EVENT_ERROR.", __FUNCTION__);
            _loginAuthenticated = NO;
            [_connection disconnect];
            self._connection = nil;
            [self _showLoginView:@"Connection error"];
            break;
        }
    }
}

- (NSMutableDictionary*) _allocNewEvent:(NSString*)event
{
    NSMutableDictionary* entries = [[NSMutableDictionary alloc] init];
    
    NSArray *components = [event componentsSeparatedByString:@"&"];
    for (NSString *entry in components) {
        NSArray *pair = [entry componentsSeparatedByString:@"="];
        [entries setValue:[pair objectAtIndex:1] forKey:[pair objectAtIndex:0]];
    }
    
    return entries;
}

- (NSString*) _getLocalizedLoginError:(int)code defaultError:(NSString*)error
{
    switch (code)
    {
        case 5: return NSLocalizedString(@"Password is wrong", @"");
        case 6: return NSLocalizedString(@"Username is wrong", @"");
    }
    return error;
}
   
- (void) _handleNetworkEvent_LOGIN:(int)code withContent:(NSString*)event
{
    if (code != 0) {  // Error
        NSLog(@"%s: Login failed. Error: [%@].", __FUNCTION__, event);
        [self _showLoginView:[self _getLocalizedLoginError:code defaultError:event]];
        return;
    }
    NSArray* components = [event componentsSeparatedByString:@";"];
    NSString* pid = [components objectAtIndex:0];
    NSString* rating = [components objectAtIndex:1];
    NSLog(@"%s: [%@ %@] LOGIN.", __FUNCTION__, pid, rating);

    if (![_username isEqualToString:pid]) { // not mine?
        return; // Other users' login. Ignore for now.
    }

    [self _onMyRatingUpdated:rating]; // Save my Rating.
    _loginAuthenticated = YES;
    [self _dismissLoginView];

    _messagesButton.enabled = YES;

    // Save the Login info after a successful login.
    if (![_username hasPrefix:HC_GUEST_PREFIX]) { // A normal account?
        [[NSUserDefaults standardUserDefaults] setObject:_username forKey:@"network_username"];
        [[NSUserDefaults standardUserDefaults] setObject:_password forKey:@"network_password"];
    }
    
    // Retrieve my statistics.
    [_connection send_PLAYER_INFO:_username];
}

- (void) _handleNetworkEvent_LIST:(NSString*)event
{
    [self _showListTableView:event];
}

- (void) _handleNetworkEvent_I_TABLE:(NSString*)event
{
    TableInfo* table = [[TableInfo allocTableFromString:event] autorelease];

    [self _resetAndClearTable];
    self._tableId = table.tableId;

    ColorEnum myColor = HC_COLOR_NONE; // Default: an observer.
    if      ([_username isEqualToString:table.redId])   { myColor = HC_COLOR_RED;   }
    else if ([_username isEqualToString:table.blackId]) { myColor = HC_COLOR_BLACK; }
    _myColor = myColor;

    // Reverse the View if necessary.
    if (   (myColor == HC_COLOR_BLACK && _game.blackAtTopSide)
        || (myColor != HC_COLOR_BLACK && !_game.blackAtTopSide) )
    {
        [_board reverseBoardView];
    }
    
    NSString* redInfo = ([table.redId length] == 0 ? @"*"
                         : [NSString stringWithFormat:@"%@ (%@)", table.redId, table.redRating]);
    NSString* blackInfo = ([table.blackId length] == 0 ? @"*"
                           : [NSString stringWithFormat:@"%@ (%@)", table.blackId, table.blackRating]);
    [_board setRedLabel:redInfo];
    [_board setBlackLabel:blackInfo];
    [_board setInitialTime:table.itimes];
    [_board setRedTime:table.redTimes];
    [_board setBlackTime:table.blackTimes];

    self._redId = ([table.redId length] == 0 ? nil : table.redId);
    self._blackId = ([table.blackId length] == 0 ? nil : table.blackId);
    _actionButton.enabled = YES;
}

- (void) _handleNetworkEvent_I_MOVES:(NSString*)event
{
    NSArray* components = [event componentsSeparatedByString:@";"];
    NSString* tableId = [components objectAtIndex:0];
    NSString* movesStr = [components objectAtIndex:1];

    if ( ! [_tableId isEqualToString:tableId] ) {
        NSLog(@"%s: I_MOVES:[%@] from table:[%@] ignored.", __FUNCTION__, movesStr, tableId);
        return;
    }
    Position from, to;
    NSArray* moves = [movesStr componentsSeparatedByString:@"/"];

    const int moveCount = (int) [moves count];
    const int lastResumedIndex = moveCount - 1;

    for (int i = 0; i < moveCount; ++i)
    {
        NSString* moveStr = [moves objectAtIndex:i];
        from.row = [moveStr characterAtIndex:1] - '0';
        from.col = [moveStr characterAtIndex:0] - '0';
        to.row = [moveStr characterAtIndex:3] - '0';
        to.col = [moveStr characterAtIndex:2] - '0';

        [_game doMoveFrom:from toPosition:to];
        [_board onNewMoveFromPosition:from toPosition:to setupMode:(i < lastResumedIndex)];
    }
}

- (void) _handleNetworkEvent_MOVE:(NSString*)event
{
    NSArray* components = [event componentsSeparatedByString:@";"];
    NSString* tableId = [components objectAtIndex:0];
    NSString* moveStr = [components objectAtIndex:2];

    if ( ! [_tableId isEqualToString:tableId] ) {
        NSLog(@"%s: Move:[%@] from table:[%@] ignored.", __FUNCTION__, moveStr, tableId);
        return;
    }
    Position from, to;
    from.row = [moveStr characterAtIndex:1] - '0';
    from.col = [moveStr characterAtIndex:0] - '0';
    to.row = [moveStr characterAtIndex:3] - '0';
    to.col = [moveStr characterAtIndex:2] - '0';

    [_game doMoveFrom:from toPosition:to];
    [_board onNewMoveFromPosition:from toPosition:to setupMode:NO];
}

- (void) _handleNetworkEvent_E_END:(NSString*)event
{
    NSArray* components = [event componentsSeparatedByString:@";"];
    NSString* tableId = [components objectAtIndex:0];
    NSString* gameResult = [components objectAtIndex:1];
    
    NSLog(@"%s: Table:[%@] - Game Over: [%@].", __FUNCTION__, tableId, gameResult);

    if ( [_tableId isEqualToString:tableId] ) {
        _isGameOver = YES;
        [_board onGameOver];
        if (_myColor == HC_COLOR_RED || _myColor == HC_COLOR_BLACK) {
            [_connection send_PLAYER_INFO:_username];
        }
    }
}

- (void) _handleNetworkEvent_RESET:(NSString*)event
{
    NSArray* components = [event componentsSeparatedByString:@";"];
    NSString* tableId = [components objectAtIndex:0];
    
    NSLog(@"%s: Table:[%@] - Game Reset.", __FUNCTION__, tableId);
    if ( [_tableId isEqualToString:tableId] ) {
        [self _resetAndClearTable];
    }
}

- (void) _handleNetworkEvent_E_JOIN:(NSString*)event
{
    NSArray* components = [event componentsSeparatedByString:@";"];
    NSString* tableId = [components objectAtIndex:0];
    NSString* pid = [components objectAtIndex:1];
    NSString* rating = [components objectAtIndex:2];
    NSString* color = [components objectAtIndex:3];

    NSString* playerInfo = ([pid length] == 0 ? @"*"
                            : [NSString stringWithFormat:@"%@ (%@)", pid, rating]);

    if ( ! [_tableId isEqualToString:tableId] ) {
        NSLog(@"%s: E_JOIN:[%@ as %@] from table:[%@] ignored.", __FUNCTION__, playerInfo, color, tableId);
        return;
    }
    if ([color isEqualToString:@"Red"])
    {
        self._redId = pid;
        [_board setRedLabel:playerInfo];
        if ([pid isEqualToString:_blackId]) {
            self._blackId = nil;
            [_board setBlackLabel:@"*"];
        }
        if ([_username isEqualToString:pid]) _myColor = HC_COLOR_RED;
    }
    else if ([color isEqualToString:@"Black"])
    {
        self._blackId = pid;
        [_board setBlackLabel:playerInfo];
        if ([pid isEqualToString:_redId]) {
            self._redId = nil;
            [_board setRedLabel:@"*"];
        }
        if ([_username isEqualToString:pid]) _myColor = HC_COLOR_BLACK;
    }
    else if ([color isEqualToString:@"None"])
    {
        if ([pid isEqualToString:_redId]) {
            self._redId = nil;
            [_board setRedLabel:@"*"];
        } else if ([pid isEqualToString:_blackId]) {
            self._blackId = nil;
            [_board setBlackLabel:@"*"];
        }
        if ([_username isEqualToString:pid]) _myColor = HC_COLOR_NONE;
    }

    // Reverse the View if necessary.
    if (   (_myColor == HC_COLOR_BLACK && _game.blackAtTopSide)
        || (_myColor != HC_COLOR_BLACK && !_game.blackAtTopSide) )
    {
        [_board reverseBoardView];
    }
}

- (void) _handleNetworkEvent_LEAVE:(NSString*)event
{
    NSArray* components = [event componentsSeparatedByString:@";"];
    NSString* tableId = [components objectAtIndex:0];
    NSString* pid = [components objectAtIndex:1];

    // Check if I just left the Table.
    if ([_tableId isEqualToString:tableId] && [_username isEqualToString:pid]) {
        return [self _animateEmptyBoard];
    }
    else if (![_tableId isEqualToString:tableId]) {
        NSLog(@"%s: E_LEAVE:[%@] from table:[%@] ignored.", __FUNCTION__, pid, tableId);
        return;
    }

    if ([pid isEqualToString:_redId]) {
        self._redId = nil;
        [_board setRedLabel:@"*"];
    } else if ([pid isEqualToString:_blackId]) {
        self._blackId = nil;
        [_board setBlackLabel:@"*"];
    }
}

- (void) _handleNetworkEvent_UPDATE:(NSString*)event
{
    NSArray* components = [event componentsSeparatedByString:@";"];
    NSString* tableId = [components objectAtIndex:0];
    NSString* pid = [components objectAtIndex:1];
    NSString* itimes = [components objectAtIndex:3];

    if ( ! [_tableId isEqualToString:tableId] ) {
        NSLog(@"%s: [%@] UPDATE time [%@] at table:[%@] ignored.", __FUNCTION__, pid, itimes, tableId);
        return;
    }

    [_board setInitialTime:itimes];
    [_board setRedTime:itimes];
    [_board setBlackTime:itimes];
}

- (void) _handleNetworkEvent_MSG:(NSString*)event
{
    NSArray* components = [event componentsSeparatedByString:@";"];
    NSString* pid = [components objectAtIndex:0];
    NSString* msg = [components objectAtIndex:1];

    NSLog(@"%s: [%@] sent MSG [%@].", __FUNCTION__, pid, msg);
    [self _onNewMessage:msg from:pid];
}

- (void) _handleNetworkEvent_DRAW:(NSString*)event
{
    NSArray* components = [event componentsSeparatedByString:@";"];
    NSString* tableId = [components objectAtIndex:0];
    NSString* pid = [components objectAtIndex:1];
    
    NSLog(@"%s: [%@] sent DRAW at table [%@].", __FUNCTION__, pid, tableId);

    NSString* msg = @"Requesting a DRAW";
    [self _onNewMessage:msg from:pid];
}

- (void) _handleNetworkEvent_INVITE:(NSString*)event toTable:(NSString*)tableId
{
    NSArray* components = [event componentsSeparatedByString:@";"];
    NSString* pid = [components objectAtIndex:0];
    NSString* rating = [components objectAtIndex:1];

    NSString* playerInfo = [NSString stringWithFormat:@"%@ (%@)", pid, rating];
    NSLog(@"%s: [%@] sent INVITE to [%@].", __FUNCTION__, playerInfo, tableId);

    NSString* msg = [NSString stringWithFormat:@"*INVITE to Table [%@]", tableId ? tableId : @""];
    [self _onNewMessage:msg from:playerInfo];
}

- (void) _handleNetworkEvent_E_SCORE:(NSString*)event
{
    NSArray* components = [event componentsSeparatedByString:@";"];
    NSString* tableId = [components objectAtIndex:0];
    NSString* pid = [components objectAtIndex:1];
    NSString* rating = [components objectAtIndex:2];
    
    NSLog(@"%s: Rating of [%@] updated to [%@] at Table [%@].", __FUNCTION__, pid, rating, tableId);

    if ([_username isEqualToString:pid]) {
        [self _onMyRatingUpdated:rating];
    }

    NSString* playerInfo = [NSString stringWithFormat:@"%@ (%@)", pid, rating];
    if ([_redId isEqualToString:pid]) {
        [_board setRedLabel:playerInfo];
    } else if ([_blackId isEqualToString:pid]) {
        [_board setBlackLabel:playerInfo];
    }
}

- (void) _handleNetworkEvent_PLAYER_INFO:(NSString*)event
{
    NSArray* components = [event componentsSeparatedByString:@";"];
    NSString* pid = [components objectAtIndex:0];
    NSString* rating = [components objectAtIndex:1];
    NSString* wins = [components objectAtIndex:2];
    NSString* draws = [components objectAtIndex:3];
    NSString* losses = [components objectAtIndex:4];

    NSLog(@"%s: PLAYER_INFO of [%@ (%@)] = [W%@ D%@ L%@].", __FUNCTION__, pid, rating, wins, draws, losses);

    if ([_username isEqualToString:pid]) {
        _infoCell.detailTextLabel.text = [NSString stringWithFormat:@"W%@  D%@  L%@", wins, draws, losses];
        [_mainView reloadData];
    }
}

#pragma mark -
#pragma mark Other helper functions

- (NSString*) _generateGuestUserName
{
    const unsigned int MAX_GUEST_ID = 10000;

    const int randNum = [self _generateRandomNumber:MAX_GUEST_ID];
    NSString* sGuestId = [NSString stringWithFormat:@"%@ip%d", HC_GUEST_PREFIX, randNum];
    return sGuestId;
}

- (int) _generateRandomNumber:(unsigned int)max_value
{        
    const unsigned int _RAND_MAX = (2u << 31) - 1;
    const int randNum =
        1 + (int) ((double)max_value * (arc4random() / (_RAND_MAX + 1.0)));
    
    return randNum;
}

@end
