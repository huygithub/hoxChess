/*

File: Piece.h

Abstract: A playing piece. A concrete subclass of Bit that displays an image..

Version: 1.0

Disclaimer: IMPORTANT:  This Apple software is supplied to you by 
Apple Inc. ("Apple") in consideration of your agreement to the
following terms, and your use, installation, modification or
redistribution of this Apple software constitutes acceptance of these
terms.  If you do not agree with these terms, please do not use,
install, modify or redistribute this Apple software.

In consideration of your agreement to abide by the following terms, and
subject to these terms, Apple grants you a personal, non-exclusive
license, under Apple's copyrights in this original Apple software (the
"Apple Software"), to use, reproduce, modify and redistribute the Apple
Software, with or without modifications, in source and/or binary forms;
provided that if you redistribute the Apple Software in its entirety and
without modifications, you must retain this notice and the following
text and disclaimers in all such redistributions of the Apple Software. 
Neither the name, trademarks, service marks or logos of Apple Inc. 
may be used to endorse or promote products derived from the Apple
Software without specific prior written permission from Apple.  Except
as expressly stated in this notice, no other rights or licenses, express
or implied, are granted by Apple herein, including but not limited to
any patent rights that may be infringed by your derivative works or by
other works in which the Apple Software may be incorporated.

The Apple Software is provided by Apple on an "AS IS" basis.  APPLE
MAKES NO WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
THE IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE, REGARDING THE APPLE SOFTWARE OR ITS USE AND
OPERATION ALONE OR IN COMBINATION WITH YOUR PRODUCTS.

IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL
OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION,
MODIFICATION AND/OR DISTRIBUTION OF THE APPLE SOFTWARE, HOWEVER CAUSED
AND WHETHER UNDER THEORY OF CONTRACT, TORT (INCLUDING NEGLIGENCE),
STRICT LIABILITY OR OTHERWISE, EVEN IF APPLE HAS BEEN ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

Copyright (C) 2007 Apple Inc. All Rights Reserved.

*/

#import <QuartzCore/QuartzCore.h>
#import "Enums.h"

@class GridCell;

/** Standard Z positions */
enum {
    kBoardZ    = 1,
    kPieceZ    = 2,

    kPickedUpZ = 100
};

/** A playing piece that displays an image. */
@interface Piece : CALayer
{
    GridCell*      holder;
    PieceEnum     _type;
    ColorEnum     _color;
    HighlightEnum _highlightState;  // Highlight state.

    NSString*     _imageName;
    int           _restingZ;  // The original z-position, saved while pickedUp
}

/** Conveniences for getting/setting the layer's scale and rotation */
@property CGFloat scale;
@property int rotation;         // in degrees! Positive = clockwise

/** "Picking up" a Piece makes it larger, translucent, and in front of everything else */
@property BOOL pickedUp;

@property (nonatomic, retain)   GridCell* holder;
@property (nonatomic, readonly) PieceEnum type;
@property (nonatomic, readonly) ColorEnum color;
@property (nonatomic)           HighlightEnum highlightState;

/** Initialize a Piece with a given Type, Color, and an image file.
 @param imageName a resource name from the app bundle, or an absolute path.
 @param scale
   - scale == 0.0, the image's natural size will be used.
   - 0.0 < scale < 4.0, the image will be scaled by that factor.
   - scale >= 4.0, it will be used as the size to scale the maximum dimension.
 */
- (id) initWithType:(PieceEnum)type color:(ColorEnum)color
          imageName:(NSString*)imageName scale:(CGFloat)scale;

/** Removes this Bit while running a explosion/fade-out animation */
- (void) destroyWithAnimation:(BOOL)animated;

/** Adds this Bit back ("un-destroy") to a given layer without animation */
- (void) putbackInLayer:(CALayer*)superLayer;

- (void) setImage:(CGImageRef)image scale:(CGFloat)scale;
- (void) setImage:(CGImageRef)image;

- (void) movePieceTo:(CGPoint)newPosition animated:(BOOL)animated;

@end
