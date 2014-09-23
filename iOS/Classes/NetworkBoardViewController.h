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

#import "BoardViewController.h"
#import "Game.h"
#import "LoginViewController.h"
#import "TableListViewController.h"
#import "MessageListViewController.h"
#import "NetworkConnection.h"

@interface NetworkBoardViewController : UIViewController<UITableViewDelegate, UITableViewDataSource,
    BoardOwner, LoginDelegate, TableListDelegate, MessageListDelegate,
    NetworkHandler, UIActionSheetDelegate>
{
    IBOutlet UIToolbar*               _toolbar;
    IBOutlet UIActivityIndicatorView* _activity;
    IBOutlet UIBarButtonItem*         _actionButton;
    IBOutlet UIBarButtonItem*         _messagesButton;

    IBOutlet UITableView*             _mainView;
    IBOutlet UIView*                  _containerView;
    UITableViewCell*                  _infoCell;

    BoardViewController* _board;
    Game*                _game;
    NSString*            _tableId;
    ColorEnum            _myColor;  // The color (role) of the LOCAL player.

    NSString*            _username;
    NSString*            _password;
    NSString*            _rating;
    NetworkConnection*   _connection;

    NSString*            _redId;
    NSString*            _blackId;

    BOOL                 _isGameOver;

    BOOL                 _loginCanceled;
    BOOL                 _loginAuthenticated;
    BOOL                 _logoutPending;

    LoginViewController*       _loginController;
    TableListViewController*   _tableListController;
    MessageListViewController* _messageListController;
}

@property (nonatomic, retain) BoardViewController* _board;
@property (nonatomic, retain) Game* _game;
@property (nonatomic, retain) NSString* _tableId;
@property (nonatomic, retain) NSString* _username;
@property (nonatomic, retain) NSString* _password;
@property (nonatomic, retain) NSString* _rating;
@property (nonatomic, retain) NSString* _redId;
@property (nonatomic, retain) NSString* _blackId;
@property (nonatomic, retain) NetworkConnection* _connection;
@property (nonatomic, retain) LoginViewController* _loginController;
@property (nonatomic, retain) TableListViewController* _tableListController;
@property (nonatomic, retain) MessageListViewController* _messageListController;

- (IBAction)homePressed:(id)sender;
- (IBAction)searchPressed:(id)sender;
- (IBAction)actionPressed:(id)sender;
- (IBAction)messagesPressed:(id)sender;

- (void) onLocalMoveMadeFrom:(Position)from toPosition:(Position)to;
- (void) goBackToHomeMenu;

@end
