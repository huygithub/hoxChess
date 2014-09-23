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

#import "AIBoardViewController.h"
#import "Enums.h"
#import "Types.h"
#import "SoundManager.h"

#define ACTION_BUTTON_INDEX     4
#define SUSPEND_AI_BUTTON_INDEX 8

enum AlertViewEnum
{
    HC_ALERT_END_GAME,
    HC_ALERT_RESET_GAME
};

enum ActionSheetEnum
{
    HC_ACTION_SHEET_CANCEL = 1, // Must be non-zero.
    HC_ACTION_SHEET_UNDO   = 2
};

///////////////////////////////////////////////////////////////////////////////
//
//    Private methods
//
///////////////////////////////////////////////////////////////////////////////

@interface AIBoardViewController (/* Private interface */)

- (void) _handleEndGameInUI;
- (void) _loadListOfMoves:(NSArray*)moves;
- (void) _loadPendingGame:(NSString *)sPendingGame;
- (void) _undoLastMove;
- (void) _countDownToAIMove;
- (void) _askAIToGenerateMove;
- (void) _onAfterDidMove;
- (void) _onAISuspendChanged;

@end


///////////////////////////////////////////////////////////////////////////////
//
//    Implementation of Public methods
//
///////////////////////////////////////////////////////////////////////////////

@implementation AIBoardViewController

@synthesize ownerColor=_myColor;
@synthesize _aiTimer;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    if ( (self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil]) )
    {
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

    //_board = [[BoardViewController alloc] initWithNibName:@"BoardView" bundle:nil];
    //_board.boardOwner = self;
    //[self.view addSubview:_board.view];
    [self.view bringSubviewToFront:_toolbar];
    [self.view bringSubviewToFront:_activity];

    _game = _board.game;
    _aiTimer = nil;

    _aiRobot = [[AIRobot alloc] initWith:self];
    _resumeAIButton = [[UIBarButtonItem alloc]
                       initWithImage:[UIImage imageNamed:@"identity.png"]
                       style:UIBarButtonItemStylePlain
                       target:self action:_suspendAIButton.action];

    _myColor = HC_COLOR_RED;
    [_board setRedLabel:NSLocalizedString(@"You", @"")];

    _aiSuspended = [[NSUserDefaults standardUserDefaults] boolForKey:@"ai_suspended"];
    [self _onAISuspendChanged];

    _aiThinkingActivity = [[UIActivityIndicatorView alloc]
                           initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleGray];
    _aiThinkingButton = [[UIBarButtonItem alloc] initWithCustomView:_aiThinkingActivity];

    NSString* color = [[NSUserDefaults standardUserDefaults] stringForKey:@"my_color"];
    _myColor = ([color isEqualToString:@"Black"] ? HC_COLOR_BLACK
                                                 : HC_COLOR_RED );
    if (_myColor == HC_COLOR_BLACK) {
        [_board reverseRole];
    }

    // Restore pending game, if any.
    NSString* sPendingGame = [[NSUserDefaults standardUserDefaults] stringForKey:@"pending_game"];
    if ([sPendingGame length]) {
        [self _loadPendingGame:sPendingGame];
    } else if (_myColor == HC_COLOR_BLACK) {
        [self _countDownToAIMove];
    }
    
    [_activity stopAnimating];
}

- (void)dealloc
{
    //NSLog(@"%s: ENTER.", __FUNCTION__);
    [_aiRobot release];
    [_aiThinkingActivity release];
    [_aiThinkingButton release];
    [_reverseRoleButton release];
    [_resumeAIButton release];
    if (_aiTimer) {
        [_aiTimer invalidate];
        self._aiTimer = nil;
    }
    _game = nil;
    _board = nil;
    [super dealloc];
}

- (void)viewDidDisappear:(BOOL)animated
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    [self saveGame];
    [_board.view removeFromSuperview];
    [_board release];
    _board = nil;
}

- (void) onAIRobotStopped
{
    NSLog(@"%s: ENTER.", __FUNCTION__);
    [_aiRobot release];
    _aiRobot = nil;
    [self goBackToHomeMenu];
}

- (void) goBackToHomeMenu
{
    [self.navigationController popViewControllerAnimated:YES];
}

//
// Handle the "OK" button in the END-GAME and RESUME-GAME alert dialogs. 
//
- (void)alertView: (UIAlertView *)alertView clickedButtonAtIndex: (NSInteger)buttonIndex
{
    if ( alertView.tag == HC_ALERT_END_GAME ) {
        ; // Do nothing.
    }
    else if (    alertView.tag == HC_ALERT_RESET_GAME
             && buttonIndex != [alertView cancelButtonIndex] )
    {
        [_activity startAnimating];
        [_board rescheduleTimer];
        [_aiRobot runResetRobot];
    }
}

- (void)didReceiveMemoryWarning
{
	// Releases the view if it doesn't have a superview.
    [super didReceiveMemoryWarning];
	
	// Release any cached data, images, etc that aren't in use.
}

#pragma mark Button actions

- (IBAction)homePressed:(id)sender
{
    [_activity startAnimating];
    [_board destroyTimer];
    [_aiRobot runStopRobot];
}

- (IBAction)resetPressed:(id)sender
{
    if ([_game getMoveCount] == 0) {
        return;  // Do nothing if game not yet started.
    }
    else if (_game.gameResult != HC_GAME_STATUS_IN_PROGRESS) // Game Over?
    {
        [_aiRobot runResetRobot];
    }
    else {
        UIAlertView *alert =
            [[UIAlertView alloc] initWithTitle:nil
                                       message:NSLocalizedString(@"New game?", @"")
                                      delegate:self 
                             cancelButtonTitle:NSLocalizedString(@"No", @"")
                             otherButtonTitles:NSLocalizedString(@"Yes", @""), nil];
        alert.tag = HC_ALERT_RESET_GAME;
        [alert show];
        [alert release];
    }
}

- (IBAction)actionPressed:(id)sender
{
    NSUInteger moveCount = [[_board getMoves] count];
    if (moveCount == 0) {
        return;  // Do nothing.
    }

    UIActionSheet* actionSheet = nil;

    if (  !_aiSuspended
        && _myColor != _game.nextColor // Robot is thinking?
        && _game.gameResult == HC_GAME_STATUS_IN_PROGRESS)
    {
        actionSheet = [[UIActionSheet alloc] initWithTitle:nil
                                                  delegate:self
                                         cancelButtonTitle:NSLocalizedString(@"AI thinking...", @"")
                                    destructiveButtonTitle:nil
                                         otherButtonTitles:nil];
        actionSheet.tag = HC_ACTION_SHEET_CANCEL;
    }
    else
    {
        actionSheet = [[UIActionSheet alloc] initWithTitle:nil
                                                  delegate:self
                                         cancelButtonTitle:NSLocalizedString(@"Cancel", @"")
                                    destructiveButtonTitle:NSLocalizedString(@"Undo Move", @"")
                                         otherButtonTitles:nil];
        actionSheet.tag = HC_ACTION_SHEET_UNDO;
        
    }

    actionSheet.actionSheetStyle = UIActionSheetStyleAutomatic;
    [actionSheet showInView:self.view];
    [actionSheet release];
}

- (IBAction)reverseRolePressed:(id)sender
{
    if ([_game getMoveCount] > 0) {
        NSLog(@"%s: Game already started. Do nothing.", __FUNCTION__);
        return;
    }

    _myColor = (_myColor == HC_COLOR_RED ? HC_COLOR_BLACK : HC_COLOR_RED);
    [_board reverseRole];
    [[SoundManager sharedInstance] playSound:@"ChangeRole"];

    if (_myColor == HC_COLOR_BLACK) {
        [self _countDownToAIMove];
    } else {
        _reverseRoleButton.enabled = YES;
        if (_aiTimer) {
            NSLog(@"%s: Cancel the pending AI-timer...", __FUNCTION__);
            [_aiTimer invalidate];
            self._aiTimer = nil;
        }
    }
}

- (IBAction)suspendAIPressed:(id)sender
{
    _aiSuspended = !_aiSuspended;
    [self _onAISuspendChanged];

    if ( !_aiSuspended
         &&  (   _myColor != _game.nextColor
              && _game.gameResult == HC_GAME_STATUS_IN_PROGRESS ))
    {
        if (_aiTimer) {
            NSLog(@"%s: AI has already been scheduled to generate a Move.", __FUNCTION__);
            return;
        }
        [self _askAIToGenerateMove];
    }
}

- (void)actionSheet:(UIActionSheet *)actionSheet clickedButtonAtIndex:(NSInteger)buttonIndex
{
    if (actionSheet.tag == HC_ACTION_SHEET_CANCEL) {
        return;
    }

    switch (buttonIndex)
    {
        case 0:  // Undo Move
            if ([_game getMoveCount])
            {
                [_activity startAnimating];
                [self performSelector:@selector(_undoLastMove) withObject:nil afterDelay:0];
            }
            break;
    }
}

- (void) onMoveGeneratedByAI:(NSNumber *)moveInfo
{
    int  move = (int) [moveInfo integerValue];
    int sqSrc = SRC(move);
    int sqDst = DST(move);
    Position from, to;
    from.row = ROW(sqSrc);
    from.col = COLUMN(sqSrc);
    to.row = ROW(sqDst);
    to.col = COLUMN(sqDst);

    [_game doMoveFrom:from toPosition:to];
    [_board onNewMoveFromPosition:from toPosition:to setupMode:NO];

    if (_aiTimer) {
        self._aiTimer = nil;
    }
    
    NSMutableArray* newItems = [NSMutableArray arrayWithArray:_toolbar.items];
    [newItems replaceObjectAtIndex:ACTION_BUTTON_INDEX withObject:_actionButton];
    _toolbar.items = newItems;

    // TODO: Re-consider calling this function because the Board may be in Replay-Mode.
    [self _onAfterDidMove];
}

- (void) onLocalMoveMadeFrom:(Position)from toPosition:(Position)to
{
    [self _onAfterDidMove];

    if ( _game.gameResult == HC_GAME_STATUS_IN_PROGRESS ) {
        [_aiRobot onMove_sync:from toPosition:to];
        [self _askAIToGenerateMove];
    }
}

- (void) onResetDoneByAI
{
    [_activity stopAnimating];

    [UIView beginAnimations:nil context:NULL];
    [UIView setAnimationDuration:HC_TABLE_ANIMATION_DURATION];
    [UIView setAnimationTransition:UIViewAnimationTransitionFlipFromRight
                           forView:self.view cache:YES];
    [_board resetBoard];
    [UIView commitAnimations];

    _reverseRoleButton.enabled = YES;
    _resetButton.enabled = NO;
    _actionButton.enabled = NO;
    if (_myColor == HC_COLOR_BLACK) {
        [self _countDownToAIMove];
    }
}

///////////////////////////////////////////////////////////////////////////////
//
//    Implementation of Private methods
//
///////////////////////////////////////////////////////////////////////////////

#pragma mark -
#pragma mark Private methods

- (void) _handleEndGameInUI
{
    NSString *msg   = nil;

    GameStatusEnum result = _game.gameResult;

    if (   (result == HC_GAME_STATUS_RED_WIN && _myColor == HC_COLOR_RED)
        || (result == HC_GAME_STATUS_BLACK_WIN && _myColor == HC_COLOR_BLACK) )
    {
        msg = NSLocalizedString(@"You won. Congratulations!", @"");
    }
    else if (  (result == HC_GAME_STATUS_RED_WIN && _myColor == HC_COLOR_BLACK)
           || (result == HC_GAME_STATUS_BLACK_WIN && _myColor == HC_COLOR_RED) )
    {
        msg = NSLocalizedString(@"Computer won. Don't give up. Please try again!", @"");
    }
    else if (result == HC_GAME_STATUS_DRAWN)
    {
        msg = NSLocalizedString(@"Sorry, we are in draw!", @"");
    }
    else if (result == HC_GAME_STATUS_TOO_MANY_MOVES)
    {
        msg = NSLocalizedString(@"Sorry, we made too many moves. Please restart again!", @"");
    }
    else
    {
        return;
    }

    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:nil
                                                    message:msg
                                                   delegate:self 
                                          cancelButtonTitle:nil
                                          otherButtonTitles:@"OK", nil];
    alert.tag = HC_ALERT_END_GAME;
    [alert show];
    [alert release];
}

- (void) saveGame
{
    NSMutableString* sMoves = [NSMutableString string];

    // Always save the current game regardless of its status.
    NSMutableArray* moves = [_board getMoves];
    for (MoveAtom *pMove in moves) {
        if ([sMoves length]) [sMoves appendString:@","];
        [sMoves appendFormat:@"%d",pMove.move];
    }

    [[NSUserDefaults standardUserDefaults] setObject:sMoves forKey:@"pending_game"];
    [[NSUserDefaults standardUserDefaults] setObject:(_myColor == HC_COLOR_RED ? @"Red" : @"Black")
                                              forKey:@"my_color"];
    [[NSUserDefaults standardUserDefaults] setBool:_aiSuspended forKey:@"ai_suspended"];
}

- (void) _loadPendingGame:(NSString *)sPendingGame
{
    NSArray* moves = [sPendingGame componentsSeparatedByString:@","];
    const int size = (int) [moves count];
    NSMutableArray* moveList = [NSMutableArray arrayWithCapacity:size];
    for (int i = 0; i < size; ++i)
    {
        int mv = [(NSNumber*)[moves objectAtIndex:i] intValue];
        [moveList addObject:[NSNumber numberWithInt:mv]];
    }

    [self _loadListOfMoves:moveList];
}

- (void) _undoLastMove
{
    NSArray* moves = [NSArray arrayWithArray:[_board getMoves]];
    NSUInteger moveCount = [moves count];

    int myLastMoveIndex = (int) (moveCount-1);
    if (!_aiSuspended) {
        myLastMoveIndex = (_myColor == HC_COLOR_RED
                           ? (int) ( (moveCount % 2) ? moveCount-1 : moveCount-2 )
                           : (int) ( (moveCount % 2) ? moveCount-2 : moveCount-1 ));
    }

    // NOTE: We know that at this time AI is not thinking.
    //       Therefore, we directly reset the Game to avoid race conditions.
    [_board rescheduleTimer];
    [_aiRobot resetRobot_sync];
    [_board resetBoard];

    // Re-load the moves before my last Move.
    const int size = myLastMoveIndex;
    NSMutableArray* moveList = [NSMutableArray arrayWithCapacity:size];
    for (int i = 0; i < size; ++i)
    {
        int mv = ((MoveAtom*)[moves objectAtIndex:i]).move;
        [moveList addObject:[NSNumber numberWithInt:mv]];
    }

    // Temporarily (force to disable) the Sound.
    const BOOL savedEnabled = [SoundManager sharedInstance].enabled;
    [SoundManager sharedInstance].enabled = NO;
    [self _loadListOfMoves:moveList];
    [SoundManager sharedInstance].enabled = savedEnabled;

    [_activity stopAnimating];
    [[SoundManager sharedInstance] playSound:@"Undo"];
}

- (void) _loadListOfMoves:(NSArray*)moves
{
    int move = 0;
    int sqSrc = 0;
    int sqDst = 0;
    Position from, to;
    
    const int moveCount = (int) [moves count];
    const int lastResumedIndex = moveCount - 1;
    
    for (int i = 0; i < moveCount; ++i)
    {
        move = [(NSNumber*)[moves objectAtIndex:i] intValue];
        sqSrc = SRC(move);
        sqDst = DST(move);
        from.row = ROW(sqSrc);
        from.col = COLUMN(sqSrc);
        to.row = ROW(sqDst);
        to.col = COLUMN(sqDst);
        
        [_game doMoveFrom:from toPosition:to];
        [_aiRobot onMove_sync:from toPosition:to];
        [_board onNewMoveFromPosition:from toPosition:to setupMode:(i < lastResumedIndex)];
    }
    
    if ([_game getMoveCount] == 0)
    {
        // Handle the special case if the game is reset to the beginning.
        _reverseRoleButton.enabled = YES;
        _resetButton.enabled = NO;
        _actionButton.enabled = NO;
        if (_myColor == HC_COLOR_BLACK) {
            [self _countDownToAIMove];
        }
    }
    else
    {
        _reverseRoleButton.enabled = NO;
        _resetButton.enabled = YES;
        _actionButton.enabled = YES;
        if (   _myColor != _game.nextColor
            && _game.gameResult == HC_GAME_STATUS_IN_PROGRESS )
        {
            [self _askAIToGenerateMove];
        }
    }
}

- (void) _countDownToAIMove
{
    if (!_aiSuspended) {
        NSLog(@"%s: Schedule AI to run the 1st move in 5 seconds.", __FUNCTION__);
        self._aiTimer = [NSTimer scheduledTimerWithTimeInterval:5.0
            target:self selector:@selector(_askAIToGenerateMove) userInfo:nil repeats:NO];
    }
}

- (void) _askAIToGenerateMove
{
    if (!_aiSuspended) {
        [_aiThinkingActivity startAnimating];
        NSMutableArray* newItems = [NSMutableArray arrayWithArray:_toolbar.items];
        [newItems replaceObjectAtIndex:ACTION_BUTTON_INDEX withObject:_aiThinkingButton];
        _toolbar.items = newItems;

        _reverseRoleButton.enabled = NO;
        [_aiRobot runGenerateMove];
    }
    else if (_aiTimer) {
        NSLog(@"%s: Cancel the pending AI-timer...", __FUNCTION__);
        [_aiTimer invalidate];
        self._aiTimer = nil;
    }
}

- (void) _onAfterDidMove
{    
    if ([_game getMoveCount] == 1) {
        _reverseRoleButton.enabled = NO;
        _resetButton.enabled = YES;
        _actionButton.enabled = YES;
    }

    if ( _game.gameResult != HC_GAME_STATUS_IN_PROGRESS ) { // Game Over?
        [self _handleEndGameInUI];
    }
}

- (void) _onAISuspendChanged
{
    NSMutableArray* newItems = [NSMutableArray arrayWithArray:_toolbar.items];
    [newItems replaceObjectAtIndex:SUSPEND_AI_BUTTON_INDEX
                        withObject:(_aiSuspended ? _resumeAIButton : _suspendAIButton)];
    _toolbar.items = newItems;
    NSString* otherLabel = (_aiSuspended ? NSLocalizedString(@"[AI Disabled]", @"")
                            : [NSString stringWithFormat:@"%@ [%d]", _aiRobot.aiName, _aiRobot.aiLevel + 1]);
    if (_myColor == HC_COLOR_RED) {
        [_board setBlackLabel:otherLabel animated:YES highlighted:_aiSuspended];
    } else {
        [_board setRedLabel:otherLabel animated:YES highlighted:_aiSuspended];
    }
}

- (BOOL) isMyTurnNext
{
    return (_game.nextColor == _myColor || _aiSuspended);
}

- (BOOL) isGameReady
{
    return YES;
}

@end
