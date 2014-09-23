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

#import "LoginViewController.h"

enum LoginButtonEnum
{
    BUTTON_NONE,
    BUTTON_LOGIN,
    BUTTON_GUEST
};

@implementation LoginViewController

@synthesize delegate;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    if ( (self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil]) ) {
        self.delegate = nil;
        _clickedButton = BUTTON_NONE;
    }
    return self;
}

- (void)viewDidLoad
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    [super viewDidLoad];
    self.title = NSLocalizedString(@"Login", @"");
    _username.placeholder = NSLocalizedString(@"Username", @"");
    _password.placeholder = NSLocalizedString(@"Password", @"");
    [_guestButton setTitle:NSLocalizedString(@"Guest", @"") forState:UIControlStateNormal];
    [_loginButton setTitle:NSLocalizedString(@"Login", @"") forState:UIControlStateNormal];
    [_registerButton setTitle:NSLocalizedString(@"Need to register? Join now", @"")
                     forState:UIControlStateNormal];
    _error.text = @"";

    // Load the existing Login info, if available, the 1st time.
    NSString* username = [[NSUserDefaults standardUserDefaults] stringForKey:@"network_username"];
    if (username && [username length]) {
        NSString* password = [[NSUserDefaults standardUserDefaults] stringForKey:@"network_password"];
        NSLog(@"%s: Load existing LOGIN [%@, %@].", __FUNCTION__, username, password);
        [self setInitialLogin:username password:password];
    }

    [_username becomeFirstResponder]; // to have the first keyboard focus
}

- (void)setInitialLogin:(NSString *)username password:(NSString*)password
{
    _username.text = username;
    _password.text = password;
}

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
    [self.navigationController setNavigationBarHidden:NO animated:YES];
    [_activity stopAnimating];
}

- (void)didReceiveMemoryWarning {
	// Releases the view if it doesn't have a superview.
    [super didReceiveMemoryWarning];
	
	// Release any cached data, images, etc that aren't in use.
}

- (void)viewDidDisappear:(BOOL)animated
{
    NSLog(@"%s: ENTER. Clicked-button = [%d]", __FUNCTION__, _clickedButton);
    if (_clickedButton == BUTTON_NONE) { // canceled?
        [delegate handleLoginRequest:nil username:nil password:nil];
    }
}

- (void)viewDidUnload {
    NSLog(@"%s: ENTER.", __FUNCTION__);
	// Release any retained subviews of the main view.
	// e.g. self.myOutlet = nil;
}

- (void)dealloc
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    self.delegate = nil;
    [super dealloc];
}

- (void)setErrorString:(NSString*)errorStr
{
    if (errorStr) {
        _error.text = errorStr;
        _clickedButton = BUTTON_NONE;
        [_activity stopAnimating];
    }
}

- (IBAction)loginButtonPressed:(id)sender
{
    NSLog(@"%s: ENTER. [%@]", __FUNCTION__, _username.text);
    if ([_username.text length] == 0) {
        [self setErrorString:NSLocalizedString(@"Username is empty", @"")];
        return;
    } else if ([_password.text length] == 0) {
        [self setErrorString:NSLocalizedString(@"Password is empty", @"")];
        return;
    }
    _clickedButton = BUTTON_LOGIN;
    [_activity startAnimating];
    [delegate handleLoginRequest:@"login" username:_username.text password:_password.text];
}

- (IBAction)guestButtonPressed:(id)sender
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    _clickedButton = BUTTON_GUEST;
    [_activity startAnimating];
    [delegate handleLoginRequest:@"guest" username:@"" password:@""];
}

- (IBAction)registerButtonPressed:(id)sender
{
    NSURL* url = [NSURL URLWithString:@"http://www.playxiangqi.com/index.php?page=register"];
    [[UIApplication sharedApplication] openURL:url];
}

@end
