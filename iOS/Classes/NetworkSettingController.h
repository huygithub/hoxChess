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

@class NetworkSettingController;

// --------------------------------------
@protocol NetworkSettingDelegate <NSObject>
- (void) didChangeUsername:(NetworkSettingController*)controller username:(NSString*)username;
@end

// --------------------------------------
@interface NetworkSettingController : UITableViewController
{
    id<NetworkSettingDelegate> _delegate;

    IBOutlet UITableViewCell* _serverCell;
    IBOutlet UITableViewCell* _usernameCell;
    IBOutlet UITableViewCell* _passwordCell;
    IBOutlet UITableViewCell* _autoConnectCell;
    
    IBOutlet UITextField* _usernameText;
    IBOutlet UITextField* _passwordText;
    IBOutlet UISwitch*    _autoConnectSwitch;
}

@property (nonatomic, retain) id<NetworkSettingDelegate> delegate;

- (IBAction) textFieldDidEndEditing:(UITextField *)textField;
- (IBAction) autoConnectValueChanged:(id)sender;

@end
