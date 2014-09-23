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

#import "AIRobot.h"
#import "Enums.h"
#import "AI_HaQiKiD.h"
#import "AI_XQWLight.h"

///////////////////////////////////////////////////////////////////////////////
//
//    Private methods
//
///////////////////////////////////////////////////////////////////////////////

@interface AIRobot (/* Private interface */)
- (void) _robotThread;
- (void) _stopRobot;
- (void) _resetRobot;
- (void) _generateMove;
- (int) _convertStringToAIType:(NSString *)name;
@end


///////////////////////////////////////////////////////////////////////////////
//
//    Implementation of Public methods
//
///////////////////////////////////////////////////////////////////////////////

@implementation AIRobot

@synthesize aiName=_aiName;
@synthesize aiLevel=_aiLevel;

- (id) initWith:(id)delegate
{
    if ( (self = [super init]) )
    {
        // Initialize the Game's Engine.
        _aiName = [[NSUserDefaults standardUserDefaults] stringForKey:@"ai_type"];
        int aiType = [self _convertStringToAIType:_aiName];
        switch (aiType) {
            case HC_AI_XQWLight: _aiEngine = [[AI_XQWLight alloc] init]; break;
            case HC_AI_HaQiKiD:  _aiEngine = [[AI_HaQiKiD alloc] init]; break;
            default: _aiEngine = nil;
        }
        [_aiEngine initGame];
        _aiLevel = (int) [[NSUserDefaults standardUserDefaults] integerForKey:@"ai_level"];
        int nDifficulty = 1;
        switch (_aiLevel) {
            case 0: nDifficulty = 1; break;
            case 1: nDifficulty = 3; break;
            case 2: nDifficulty = 6; break;
            case 3: nDifficulty = 9; break;
        }
        [_aiEngine setDifficultyLevel:nDifficulty];

        _delegate = [delegate retain];

        _robotPort = [[NSMachPort port] retain];
        [NSThread detachNewThreadSelector:@selector(_robotThread) toTarget:self withObject:nil];
    }
    return self;
}

- (void) dealloc
{
    [_aiEngine release];
    [_delegate release];
    [_robotPort release];
    _robot = nil;
    [super dealloc];
}

- (void) _robotThread
{
 	NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    _robot = [NSThread currentThread];
    _robotLoop = CFRunLoopGetCurrent();

    // Set the priority to the highest so that Robot can use more time to think.
    [NSThread setThreadPriority:1.0];
    
    [[NSRunLoop currentRunLoop] addPort:_robotPort forMode:NSDefaultRunLoopMode];
    
    SInt32 result = 0;
    do  // Let the run loop process things.
    {
        // Start the run loop but return after each source is handled.
        result = CFRunLoopRunInMode(kCFRunLoopDefaultMode, 60, NO);
        //NSLog(@"%s: Loop returned with [%d].", __FUNCTION__, result);
        // If a source explicitly stopped the run loop, go and exit the loop
    } while (result != kCFRunLoopRunStopped);
    
    [pool release];   
}

- (void) runStopRobot
{
    [self performSelector:@selector(_stopRobot) onThread:_robot withObject:nil waitUntilDone:NO];
}

- (void) _stopRobot
{
    CFRunLoopStop(_robotLoop);
    _robot = nil;
    [_delegate performSelectorOnMainThread:@selector(onAIRobotStopped) withObject:nil waitUntilDone:NO];
}

- (void) resetRobot_sync
{
    [_aiEngine initGame];
}

- (void) runResetRobot
{
    [self performSelector:@selector(_resetRobot) onThread:_robot withObject:nil waitUntilDone:NO];
}

- (void) _resetRobot
{
    [[NSRunLoop currentRunLoop] cancelPerformSelectorsWithTarget:self];

    // NOTE: We "reset" the Board's data *here* inside the AI Thread to
    //       avoid clearing data while the AI is thinking of a Move.

    [_aiEngine initGame];
    [_delegate performSelectorOnMainThread:@selector(onResetDoneByAI) withObject:nil waitUntilDone:NO];
}

- (void) onMove_sync:(Position)from toPosition:(Position)to
{
    [_aiEngine onHumanMove:from.row fromCol:from.col toRow:to.row toCol:to.col];
}

- (void) runGenerateMove
{
    [self performSelector:@selector(_generateMove) onThread:_robot withObject:nil waitUntilDone:NO];
}

- (void) _generateMove
{
    int row1 = 0, col1 = 0, row2 = 0, col2 = 0;

    NSDate* startTime = [NSDate date];
    [_aiEngine generateMove:&row1 fromCol:&col1 toRow:&row2 toCol:&col2];
    NSTimeInterval timeInterval = - [startTime timeIntervalSinceNow]; // in seconds.
    //NSLog(@"%s: AI took [%.02f] seconds.", __FUNCTION__, timeInterval);
    if (timeInterval < 1.0) {
        [NSThread sleepForTimeInterval:(1.0 - timeInterval)];
    }

    int sqSrc = TOSQUARE(row1, col1);
    int sqDst = TOSQUARE(row2, col2);
    int move = MOVE(sqSrc, sqDst);

    if (move == INVALID_MOVE) {
        NSLog(@"ERROR: %s: Invalid move [%d].", __FUNCTION__, move); 
        return;
    }

    NSNumber* moveInfo = [NSNumber numberWithInteger:move];
    [_delegate performSelectorOnMainThread:@selector(onMoveGeneratedByAI:)
                                withObject:moveInfo waitUntilDone:NO];
}

- (int) _convertStringToAIType:(NSString *)name
{
    if ([name isEqualToString:@"XQWLight"]) { return HC_AI_XQWLight; }
    if ([name isEqualToString:@"HaQiKiD"]) { return HC_AI_HaQiKiD; }
    return HC_AI_XQWLight; // Default!
}

@end
