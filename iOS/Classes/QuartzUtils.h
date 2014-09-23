/*

File: QuartzUtils.h

Abstract: Assorted CoreGraphics / Core Animation utility functions.

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


#import <CoreGraphics/CoreGraphics.h>


/** Constants for various commonly used colors. */
extern CGColorRef kBlackColor, kWhiteColor, 
                  kTranslucentGrayColor, kTranslucentLightGrayColor, 
                  kAlmostInvisibleWhiteColor,
                  kHighlightColor, kRedColor, kLightRedColor, kLightBlueColor;


/** Moves a layer from one superlayer to another, without changing its position onscreen. */
void ChangeSuperlayer( CALayer *layer, CALayer *newSuperlayer, int index );

/** Removes a layer from its superlayer without any fade-out animation. */
void RemoveImmediately( CALayer *layer );

/** Convenience for creating a CATextLayer. */
//CATextLayer* AddTextLayer( CALayer *superlayer,
//                           NSString *text, NSFont* font,
//                           enum CAAutoresizingMask align );


/** Loads an image or pattern file into a CGImage or CGPattern.
    If the name begins with "/", it's interpreted as an absolute filesystem path.
    Otherwise, it's the name of a resource that's looked up in the app bundle.
    The image must exist, or an assertion-failure exception will be raised!
    Loaded images/patterns are cached in memory, so subsequent calls with the same name
    are very fast. */
CGImageRef GetCGImageNamed( NSString *name );
CGColorRef GetCGPatternNamed( NSString *name );

/** Loads image data from the pasteboard into a CGImage. */
//CGImageRef GetCGImageFromPasteboard( NSPasteboard *pb );

/** Creates a CGPattern from a CGImage. */
CGPatternRef CreateImagePattern( CGImageRef image );

/** Creates a CGColor that draws the given CGImage as a pattern. */
CGColorRef CreatePatternColor( CGImageRef image );

/** Returns the alpha value of a single pixel in a CGImage, scaled to a particular size. */
float GetPixelAlpha( CGImageRef image, CGSize imageSize, CGPoint pt );

/** Returns the center point of a CGRect. */
static inline CGPoint GetCGRectCenter( CGRect rect ) {
    return CGPointMake(CGRectGetMidX(rect),CGRectGetMidY(rect));
}