/*

File: QuartzUtils.m

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

#import <UIKit/UIKit.h>
#import <QuartzCore/QuartzCore.h>
#import "QuartzUtils.h"


CGColorRef kBlackColor, kWhiteColor, 
           kTranslucentGrayColor, kTranslucentLightGrayColor,
           kAlmostInvisibleWhiteColor,
           kHighlightColor, kRedColor, kLightRedColor, kLightBlueColor;

static CGColorRef CreateDeviceGrayColor(CGFloat w, CGFloat a)
{
    CGColorSpaceRef gray = CGColorSpaceCreateDeviceGray();
    CGFloat comps[] = {w, a};
    CGColorRef color = CGColorCreate(gray, comps);
    CGColorSpaceRelease(gray);
    return color;
}

static CGColorRef CreateDeviceRGBColor(CGFloat r, CGFloat g, CGFloat b, CGFloat a)
{
    CGColorSpaceRef rgb = CGColorSpaceCreateDeviceRGB();
    CGFloat comps[] = {r, g, b, a};
    CGColorRef color = CGColorCreate(rgb, comps);
    CGColorSpaceRelease(rgb);
    return color;
}

__attribute__((constructor))        // Makes this function run when the app loads
static void InitQuartzUtils()
{
    kBlackColor = CreateDeviceGrayColor(0.0, 1.0); //CGColorCreate( kCGColorSpaceGenericGray, rgba );
    kWhiteColor = CreateDeviceGrayColor(1.0, 1.0); //CGColorCreate( kCGColorSpaceGenericGray, {1.0, 1.0, 1.0});
    kTranslucentGrayColor = CreateDeviceGrayColor(0.0, 0.5); //CGColorCreate( kCGColorSpaceGenericGray, {0.0, 0.5, 1.0});
    kTranslucentLightGrayColor = CreateDeviceGrayColor(0.0, 0.25); //CGColorCreate( kCGColorSpaceGenericGray, {0.0, 0.25, 1.0});
    kAlmostInvisibleWhiteColor = CreateDeviceGrayColor(1, 0.05); //CGColorCreate( kCGColorSpaceGenericGray, {1, 0.05, 1.0});
    kHighlightColor = CreateDeviceRGBColor(1, 1, 0, 0.5); //CGColorCreate( kCGColorSpaceGenericRGB, {1, 1, 0, 0.5, 1.0});
    kRedColor = CreateDeviceRGBColor(1.0, 0, 0, 1);
    kLightRedColor = CreateDeviceRGBColor(0.7, 0, 0, 1);
    kLightBlueColor = CreateDeviceRGBColor(0, .8, .8, 1);
}


void ChangeSuperlayer( CALayer *layer, CALayer *newSuperlayer, int index )
{
    // Disable actions, else the layer will move to the wrong place and then back!
    [CATransaction flush];
    [CATransaction begin];
    [CATransaction setValue:(id)kCFBooleanTrue
                     forKey:kCATransactionDisableActions];

    CGPoint pos = [newSuperlayer convertPoint: layer.position 
                      fromLayer: layer.superlayer];
    [layer removeFromSuperlayer];
    if( index >= 0 )
        [newSuperlayer insertSublayer: layer atIndex: index];
    else
        [newSuperlayer addSublayer: layer];
    layer.position = pos;

    [CATransaction commit];
}


void RemoveImmediately( CALayer *layer )
{
    [CATransaction flush];
    [CATransaction begin];
    [CATransaction setValue:(id)kCFBooleanTrue
                     forKey:kCATransactionDisableActions];
    [layer removeFromSuperlayer];
    [CATransaction commit];
}    


//CATextLayer* AddTextLayer( CALayer *superlayer,
//                           NSString *text, NSFont* font,
//                           enum CAAutoresizingMask align )
//{
//    CATextLayer *label = [[CATextLayer alloc] init];
//    label.string = text;
//    label.font = font;
//    label.fontSize = font.pointSize;
//    label.foregroundColor = kBlackColor;
//    
//    NSString *mode;
//    if( align & kCALayerWidthSizable )
//        mode = @"center";
//    else if( align & kCALayerMinXMargin )
//        mode = @"right";
//    else
//        mode = @"left";
//    align |= kCALayerWidthSizable;
//    label.alignmentMode = mode;
//    
//    CGFloat inset = superlayer.borderWidth + 3;
//    CGRect bounds = CGRectInset(superlayer.bounds, inset, inset);
//    CGFloat height = font.ascender;
//    CGFloat y = bounds.origin.y;
//    if( align & kCALayerHeightSizable )
//        y += (bounds.size.height-height)/2.0;
//    else if( align & kCALayerMinYMargin )
//        y += bounds.size.height - height;
//    align &= ~kCALayerHeightSizable;
//    label.bounds = CGRectMake(0, font.descender,
//                              bounds.size.width, height - font.descender);
//    label.position = CGPointMake(bounds.origin.x,y+font.descender);
//    label.anchorPoint = CGPointMake(0,0);
//    
//    label.autoresizingMask = align;
//    [superlayer addSublayer: label];
//    return label;
//}


CGImageRef CreateCGImageFromFile( NSString *path )
{
    CGImageRef image = NULL;
    CFURLRef url = (CFURLRef) [NSURL fileURLWithPath: path];
    CGDataProviderRef provider = CGDataProviderCreateWithURL(url);
    if( provider ) {
        image = CGImageCreateWithPNGDataProvider(provider, NULL, NO, kCGRenderingIntentDefault);
        if(!image) { 
            NSLog(@"INFO: Cannot load image as PNG file %@ (ptr size=%lu)",path,sizeof(void*));
            //fall back to JPEG 
            image = CGImageCreateWithJPEGDataProvider(provider, NULL, NO, kCGRenderingIntentDefault);
        }
        CFRelease(provider);
    }
    return image;
}


CGImageRef GetCGImageNamed( NSString *name )
{
    // For efficiency, loaded images are cached in a dictionary by name.
    static NSMutableDictionary *sMap;
    if( ! sMap )
        sMap = [[NSMutableDictionary alloc] init];
    
    CGImageRef image = (CGImageRef) [sMap objectForKey: name];
    if( ! image ) {
        // Hasn't been cached yet, so load it:
        NSString *path;
        if( [name hasPrefix: @"/"] )
            path = name;
        else {
            path = [[NSBundle mainBundle] pathForResource: name ofType: nil];
            NSCAssert1(path,@"Couldn't find bundle image resource '%@'",name);
        }
        image = CreateCGImageFromFile(path);
        NSCAssert1(image,@"Failed to load image from %@",path);
        [sMap setObject: (id)image forKey: name];
    }
    return image;
}


CGColorRef GetCGPatternNamed( NSString *name )         // can be resource name or abs. path
{
    // For efficiency, loaded patterns are cached in a dictionary by name.
    static NSMutableDictionary *sMap;
    if( ! sMap )
        sMap = [[NSMutableDictionary alloc] init];
    
    CGColorRef pattern = (CGColorRef) [sMap objectForKey: name];
    if( ! pattern ) {
        pattern = CreatePatternColor( GetCGImageNamed(name) );
        [sMap setObject: (id)pattern forKey: name];
        CFRelease(pattern);
    }
    return pattern;
}


//CGImageRef GetCGImageFromPasteboard( NSPasteboard *pb )
//{
//    CGImageSourceRef src = NULL;
//    NSArray *paths = [pb propertyListForType: NSFilenamesPboardType];
//    if( paths.count==1 ) {
//        // If a file is being dragged, read it:
//        CFURLRef url = (CFURLRef) [NSURL fileURLWithPath: [paths objectAtIndex: 0]];
//        src = CGImageSourceCreateWithURL(url, NULL);
//    } else {
//        // Else look for an image type:
//        NSString *type = [pb availableTypeFromArray: [NSImage imageUnfilteredPasteboardTypes]];
//        if( type ) {
//            NSData *data = [pb dataForType: type];
//            src = CGImageSourceCreateWithData((CFDataRef)data, NULL);
//        }
//    }
//    if(src) {
//        CGImageRef image = CGImageSourceCreateImageAtIndex(src, 0, NULL);
//        CFRelease(src);
//        return image;
//    } else
//        return NULL;
//}    


float GetPixelAlpha( CGImageRef image, CGSize imageSize, CGPoint pt )
{
    // Trivial reject:
    if( pt.x<0 || pt.x>=imageSize.width || pt.y<0 || pt.y>=imageSize.height )
        return 0.0;
    
    // sTinyContext is a 1x1 CGBitmapContext whose pixmap stores only alpha.
    static UInt8 sPixel[1];
    static CGContextRef sTinyContext;
    if( ! sTinyContext ) {
        sTinyContext = CGBitmapContextCreate(sPixel, 1, 1, 
                                             8, 1,     // bpp, rowBytes
                                             NULL,
                                             kCGImageAlphaOnly);
        CGContextSetBlendMode(sTinyContext, kCGBlendModeCopy);
    }
    
    // Draw the image into sTinyContext, positioned so the desired point is at
    // (0,0), then examine the alpha value in the pixmap:
    CGContextDrawImage(sTinyContext, 
                       CGRectMake(-pt.x,-pt.y, imageSize.width,imageSize.height),
                       image);
    return sPixel[0] / 255.0;
}


#pragma mark -
#pragma mark PATTERNS:


// callback for CreateImagePattern.
static void drawPatternImage (void *info, CGContextRef ctx)
{
    CGImageRef image = (CGImageRef) info;
    CGContextDrawImage(ctx, 
                       CGRectMake(0,0, CGImageGetWidth(image),CGImageGetHeight(image)),
                       image);
}

// callback for CreateImagePattern.
static void releasePatternImage( void *info )
{
    CGImageRelease( (CGImageRef)info );
}


CGPatternRef CreateImagePattern( CGImageRef image )
{
    NSCParameterAssert(image);
    CGFloat width = CGImageGetWidth(image);
    CGFloat height = CGImageGetHeight(image);
    static const CGPatternCallbacks callbacks = {0, &drawPatternImage, &releasePatternImage};
    return CGPatternCreate (image,
                            CGRectMake (0, 0, width, height),
                            CGAffineTransformMake (1, 0, 0, 1, 0, 0),
                            width,
                            height,
                            kCGPatternTilingConstantSpacing,
                            true,
                            &callbacks);
}


CGColorRef CreatePatternColor( CGImageRef image )
{
    CGPatternRef pattern = CreateImagePattern(image);
    CGColorSpaceRef space = CGColorSpaceCreatePattern(NULL);
    CGFloat components[1] = {1.0};
    CGColorRef color = CGColorCreateWithPattern(space, pattern, components);
    CGColorSpaceRelease(space);
    CGPatternRelease(pattern);
    return color;
}
