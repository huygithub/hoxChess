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
#import "SingleSelectionController.h"
#import "NetworkSettingController.h"

@interface OptionsViewController : UITableViewController <SingleSelectionDelegate,
                                                          NetworkSettingDelegate>
{
    IBOutlet UITableViewCell* _soundCell;
    IBOutlet UITableViewCell* _boardCell;
    IBOutlet UITableViewCell* _pieceCell;
    IBOutlet UITableViewCell* _aiLevelCell;
    IBOutlet UITableViewCell* _aiTypeCell;
    IBOutlet UITableViewCell* _networkCell;

    IBOutlet UISwitch*        _soundSwitch;

    // --- Piece Style.
    NSArray*           _pieceChoices;
    NSUInteger         _pieceType;

    // --- Board Style.
    NSArray*           _boardChoices;
    NSUInteger         _boardType;

    // --- AI Level and Type.
    NSArray*           _aiLevelChoices;
    NSUInteger         _aiLevel;

    NSArray*           _aiTypeChoices;
    NSUInteger         _aiType;

    // --- Network
    NSString*          _username;
}

- (IBAction) autoConnectValueChanged:(id)sender;

@end
