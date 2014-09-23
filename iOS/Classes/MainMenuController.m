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

#import "MainMenuController.h"
#import "AIBoardViewController.h"
#import "NetworkBoardViewController.h"
#import "OptionsViewController.h"

@implementation MainMenuController

- (void)viewDidLoad
{
    [super viewDidLoad];
    self.title = NSLocalizedString(@"Home", @"");

    // Add a single space in front to separate the Image and the Text.
    [ai_game setTitle:[@" " stringByAppendingString:NSLocalizedString(@"Practice", @"")] forState:UIControlStateNormal];
    [online_game setTitle:[@" " stringByAppendingString:NSLocalizedString(@"Play Online", @"")] forState:UIControlStateNormal];
    [setting setTitle:[@" " stringByAppendingString:NSLocalizedString(@"Settings", @"")] forState:UIControlStateNormal];
}

- (void)viewWillAppear:(BOOL)animated 
{
    [super viewWillAppear:animated];
    [self.navigationController setNavigationBarHidden:YES animated:YES];
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

- (void)dealloc
{
    [ai_game release];
    [online_game release];
    [setting release];
    [super dealloc];
}

- (IBAction)settingPressed:(id)sender
{
    OptionsViewController* optionsController = [[OptionsViewController alloc] initWithNibName:@"OptionsView" bundle:nil];
    [self.navigationController pushViewController:optionsController animated:YES];
    [optionsController release];
}

@end
