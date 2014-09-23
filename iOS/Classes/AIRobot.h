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

#import <Foundation/Foundation.h>
#import "Types.h"

@class AIEngine;

// --------------------------------------
@protocol AIRobotDelegate <NSObject>
- (void) onMoveGeneratedByAI:(NSNumber *)moveInfo;
- (void) onResetDoneByAI;
- (void) onAIRobotStopped;
@end

// --------------------------------------
@interface AIRobot : NSObject
{
    NSString*    _aiName;
    int          _aiLevel;
    AIEngine*    _aiEngine;
    id           _delegate;
    
    NSThread*     _robot;
    NSPort*      _robotPort; // the port is used to instruct the robot to do works
    CFRunLoopRef _robotLoop; // the loop robot is on, used to control its lifecycle
}

@property (nonatomic, readonly) NSString* aiName;
@property (nonatomic)           int       aiLevel;

- (id) initWith:(id)delegate;

/** The following APIs are performned within the Robot's thread. */
- (void) runStopRobot;
- (void) runResetRobot;
- (void) runGenerateMove;

/** The following (synchronous) APIs are performed within the caller's thread. */
- (void) onMove_sync:(Position)from toPosition:(Position)to;
- (void) resetRobot_sync;

@end
