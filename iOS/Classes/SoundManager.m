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

/*
 *  SoundManager.m
 *  Created by Huy Phan on 9/12/2010.
 *
 *  A singleton class that manages sounds in the entire application.
 */

#import "SoundManager.h"
#import "Enums.h"
#import <AudioToolbox/AudioToolbox.h>

@interface SoundManager (/* Private interface */)
- (SystemSoundID) createSoundIdFromPath_:(NSString*)soundPath;
@end

// ----------------------------------------------------------------------------
// References: 
//  http://stackoverflow.com/questions/145154/what-does-your-objective-c-singleton-look-like
//
// ----------------------------------------------------------------------------

#pragma mark -
#pragma mark The class (Singleton) instance object for SoundManager

static SoundManager* _sharedAudio = nil;

@implementation SoundManager

#pragma mark -
#pragma mark Singleton methods

+ (SoundManager*) sharedInstance
{
    @synchronized(self)
    {
        if (_sharedAudio == nil) {
            _sharedAudio = [[SoundManager alloc] init];
        }
    }
    return _sharedAudio;
}

+ (id) allocWithZone:(NSZone *)zone
{
    @synchronized(self) {
        if (_sharedAudio == nil) {
            _sharedAudio = [super allocWithZone:zone];
            return _sharedAudio;  // assignment and return on first allocation
        }
    }
    return nil; // on subsequent allocation attempts return nil
}

- (id) copyWithZone:(NSZone *)zone { return self; }
- (id) retain { return self; }
- (NSUInteger) retainCount { return UINT_MAX; /* ... cannot be released */ }
- (id) autorelease { return self; }


#pragma mark -
#pragma mark Normal methods

@synthesize enabled=enabled_;

- (SystemSoundID) createSoundIdFromPath_:(NSString*)soundPath
{
    SystemSoundID soundId = 0;
    
    CFURLRef url = CFURLCreateFromFileSystemRepresentation(NULL, (UInt8*)[soundPath UTF8String],
                                                           [soundPath length], FALSE);
    if (url) {
        AudioServicesCreateSystemSoundID(url, &soundId);
        CFRelease(url);
    }
    return soundId;
}

- (id) init
{
    if ( (self = [super init]) )
    {
        enabled_ = YES;
        loadedSounds_ = [[NSMutableDictionary alloc] init];

        NSArray* soundList =
            [NSArray arrayWithObjects: @"CAPTURE", @"CAPTURE2", @"CLICK",
                                       @"DRAW", @"LOSS", @"CHECK2",
                                       @"MOVE", @"MOVE2", @"WIN", @"ILLEGAL",
                                       @"Check1", @"Replay", @"Undo", @"ChangeRole", 
                                       nil];
        for (NSString* sound in soundList)
        {
            NSString* path = [[NSBundle  mainBundle] pathForResource:sound ofType:@"WAV"
                                                         inDirectory:HC_SOUND_PATH];
            if (!path) {
                NSLog(@"%s: WARN: Failed to locate the sound file [%@].", __FUNCTION__, sound);
                continue;
            }
            SystemSoundID soundId = [self createSoundIdFromPath_:path];
            NSNumber* soundObject = [NSNumber numberWithInt:soundId]; 
            [loadedSounds_ setObject:soundObject forKey:sound];
        }
    }
    return self;
}

- (void) dealloc
{
    // NOTE: We actually do not need to do anything because the Singleton
    //       object will never be released.
    //       The following code is provided to avoid warnings from
    //       Xcode's Build and Analyze tool.

    NSEnumerator* keyEnumerator = [loadedSounds_ keyEnumerator];
    NSString* aKey;
    while ( (aKey = [keyEnumerator nextObject]) )
    {
        NSNumber* soundObject = [loadedSounds_ objectForKey:aKey];
        AudioServicesDisposeSystemSoundID([soundObject intValue]);
    }
    
    [loadedSounds_ release];
    [super dealloc];
}

- (void) playSound:(NSString*)sound
{
    if (enabled_) {
        NSNumber* soundObject = [loadedSounds_ objectForKey:sound];
        AudioServicesPlaySystemSound([soundObject intValue]);
    }
}

@end
