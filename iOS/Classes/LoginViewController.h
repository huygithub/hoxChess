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

@protocol LoginDelegate <NSObject>
// button == nil on cancel
- (void) handleLoginRequest:(NSString *)button username:(NSString*)name password:(NSString*)passwd;
@end


@interface LoginViewController : UIViewController
{
    IBOutlet UITextField* _username;
    IBOutlet UITextField* _password;
    IBOutlet UILabel*     _error;
    IBOutlet UIActivityIndicatorView* _activity;
    IBOutlet UIButton*    _guestButton;
    IBOutlet UIButton*    _loginButton;
    IBOutlet UIButton*    _registerButton;

    id <LoginDelegate> delegate;
    int                _clickedButton;
}

@property (nonatomic, retain) id <LoginDelegate> delegate;

- (void)setInitialLogin:(NSString *)username password:(NSString*)password;
- (void)setErrorString:(NSString*)errorStr;

- (IBAction)loginButtonPressed:(id)sender;
- (IBAction)guestButtonPressed:(id)sender;
- (IBAction)registerButtonPressed:(id)sender;

@end
