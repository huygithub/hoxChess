/*
 
 File: Grid.h
 
 Abstract: Abstract superclass of regular geometric grids of GridCells that Bits can be placed on.
 
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

#import "Grid.h"
#import "Piece.h"
#import "QuartzUtils.h"

BOOL layerIsPiece( CALayer* layer )    { return [layer isKindOfClass: [Piece class]]; }
BOOL layerIsGridCell( CALayer* layer ) { return [layer isKindOfClass: [GridCell class]]; }

// ---------------------------------------------------------------------------
@implementation Grid

@synthesize rows=_nRows, columns=_nColumns, spacing=_spacing;

- (id) initWithRows:(unsigned)nRows columns:(unsigned)nColumns
      boardPosition:(CGPoint)boardPosition
         boardFrame:(CGRect)boardFrame
            spacing:(CGSize)spacing
         cellOffset:(CGPoint)cellOffset
    backgroundColor:(CGColorRef)backgroundColor
{
    NSParameterAssert(nRows>0 && nColumns>0);
    if ( (self = [super init]) ) {
        _nRows = nRows;
        _nColumns = nColumns;
        _spacing = spacing;
        _lineColor = CGColorRetain(kLightRedColor); // Default setting.
        _highlightColor = nil;
        _animateColor = nil;
        _cellOffset = cellOffset;
        if (backgroundColor) {
            self.backgroundColor = CGColorRetain(backgroundColor);
        }
        self.bounds = boardFrame;
        self.position = boardPosition;
        self.anchorPoint = CGPointMake(0,0);
        self.zPosition = kBoardZ;
        self.needsDisplayOnBoundsChange = YES;
        
        unsigned n = nRows*nColumns;
        _cells = [[NSMutableArray alloc] initWithCapacity:n];
        id null = [NSNull null];
        while( n-- > 0 ) { [_cells addObject:null]; }
        [self setNeedsDisplay];
    }
    return self;
}

- (void) dealloc
{
    //NSLog(@"%s: ENTER.", __FUNCTION__);
    CGColorRelease(_lineColor);
    CGColorRelease(_highlightColor);
    CGColorRelease(_animateColor);
    [_cells release];
    [super dealloc];
}

- (void) _setColor:(CGColorRef *)var withNewColor:(CGColorRef)color
{
    if ( color != *var ) {
        // Garbage collection does not apply to CF objects like CGColors!
        CGColorRelease(*var);
        *var = CGColorRetain(color);
    }
}

- (CGColorRef) lineColor                { return _lineColor; }
- (void) setLineColor:(CGColorRef)color { [self _setColor:&_lineColor withNewColor:color]; }

- (CGColorRef) highlightColor                { return _highlightColor; }
- (void) setHighlightColor:(CGColorRef)color { [self _setColor:&_highlightColor withNewColor:color]; }

- (CGColorRef) animateColor                { return _animateColor; }
- (void) setAnimateColor:(CGColorRef)color { [self _setColor:&_animateColor withNewColor:color]; }

#pragma mark -
#pragma mark GEOMETRY:


- (GridCell*) cellAtRow:(unsigned)row column:(unsigned)col
{
    if ( row < _nRows && col < _nColumns ) {
        id cell = [_cells objectAtIndex: row*_nColumns+col];
        if ( cell != [NSNull null] ) {
            return cell;
        }
    }
    return nil;
}

- (GridCell*) addCellAtRow:(unsigned)row column:(unsigned)col
{
    NSParameterAssert(row<_nRows);
    NSParameterAssert(col<_nColumns);
    unsigned index = row*_nColumns+col;
    GridCell *cell = [_cells objectAtIndex:index];
    if ( (id)cell == [NSNull null] ) {
        CGRect frame = CGRectMake(_cellOffset.x + (col + 0.5)*_spacing.width,
                                  _cellOffset.y + (row + 0.5)*_spacing.height,
                                  _spacing.width,_spacing.height);
        cell = [[GridCell alloc] initWithGrid:self row:row column:col frame:frame];
        [_cells replaceObjectAtIndex:index withObject:cell];
        [self addSublayer:cell];
        [cell release];
        [self setNeedsDisplay];
    }
    return cell;
}

- (void) addAllCells
{
    for (int row = _nRows-1; row >= 0; --row) { // makes 'upper' cells be in 'back'
        for (int col = 0; col < _nColumns; ++col) {
            [self addCellAtRow:row column:col];
        }
    }
}

- (void) removeAllCells
{
    for (int row = _nRows-1; row >= 0; --row) {
        for (int col = 0; col < _nColumns; ++col) {
            [self removeCellAtRow:row column:col];
        }
    }
}

- (void) removeCellAtRow:(unsigned)row column:(unsigned)col
{
    NSParameterAssert(row<_nRows);
    NSParameterAssert(col<_nColumns);
    unsigned index = row*_nColumns+col;
    id cell = [_cells objectAtIndex:index];
    if( cell != [NSNull null] )
        [cell removeFromSuperlayer];
    [_cells replaceObjectAtIndex:index withObject:[NSNull null]];
    [self setNeedsDisplay];
}


#pragma mark -
#pragma mark DRAWING:


- (void) drawCellsInContext:(CGContextRef)ctx
{
    // Subroutine of -drawInContext:. Draws all the cells, with or without a fill.
    for (unsigned row = 0; row < _nRows; ++row) {
        for (unsigned col = 0; col < _nColumns; ++col) {
            GridCell *cell = [self cellAtRow:row column:col];
            if (cell) {
                [cell drawInParentContext:ctx];
            }
        }
    }
}

- (void)drawInContext:(CGContextRef)ctx
{
    // Custom CALayer drawing implementation. Delegates to the cells to draw themselves
    // in me; this is more efficient than having each cell have its own drawing.
    if (_lineColor) {
        CGContextSetStrokeColorWithColor(ctx, _lineColor);
        CGContextSetLineWidth(ctx, 1.2);
        [self drawCellsInContext:ctx];
    }
}

@end


// ---------------------------------------------------------------------------
#pragma mark -

@implementation GridCell

@synthesize row=_row;
@synthesize column=_column;
@synthesize highlightState=_highlightState;
@synthesize dotted;

- (id) initWithGrid:(Grid*)grid row:(unsigned)row column:(unsigned)col
              frame:(CGRect)frame
{
    if ( (self = [super init]) ) {
        _grid = grid;
        _row = row;
        _column = col;
        _highlightState = HC_HL_NONE;
        self.position = frame.origin;
        CGRect bounds = frame;
        bounds.origin.x = 0;
        bounds.origin.y = 0;
        self.bounds = bounds;
        self.anchorPoint = CGPointMake(0.5, 0.5);
        self.borderColor = _grid.highlightColor;
        self.cornerRadius = 9;
    }
    return self;
}

- (void) dealloc
{
    _grid  = nil;
    [super dealloc];
}

- (NSString*) description
{
    return [NSString stringWithFormat: @"%@(%u,%u)", [self class],_column,_row];
}

- (void) drawInParentContext:(CGContextRef)ctx
{
    CGRect frame = self.frame;
    const CGFloat midx = floor(CGRectGetMidX(frame)) + 0.5;
    const CGFloat midy = floor(CGRectGetMidY(frame)) + 0.5;

    CGPoint p[4] = { { CGRectGetMinX(frame), midy }, // From Right
                     { CGRectGetMaxX(frame), midy }, // ... to Left. 
                     { midx, CGRectGetMinY(frame) },   // From Top
                     { midx, CGRectGetMaxY(frame) } }; // ... to Bottom.
    if (   ! self.s 
        || ( _row == 5 && (_column != 0 && _column != 8) ) )
    {
        p[2].y = midy;
    }
    if (   ! self.n 
        || ( _row == 4 && (_column != 0 && _column != 8) ) )
    {
        p[3].y = midy;
    }
    if ( ! self.w )  p[0].x = midx;
    if ( ! self.e )  p[1].x = midx;
    CGContextStrokeLineSegments(ctx, p, 4);
    
    if ( dotted )
    {
        const CGFloat mid_offset = 5;
        CGPoint pos[16] = {
            {midx - 2, midy + 2}, {midx - 2, midy + 2 + mid_offset}, {midx - 2, midy + 2}, {midx - 2 - mid_offset, midy + 2},
            {midx + 2, midy + 2}, {midx + 2, midy + 2 + mid_offset}, {midx + 2, midy + 2}, {midx + 2 + mid_offset, midy + 2},
            {midx - 2, midy - 2}, {midx - 2, midy - 2 - mid_offset}, {midx - 2, midy - 2}, {midx - 2 - mid_offset, midy - 2},
            {midx + 2, midy - 2}, {midx + 2, midy - 2 - mid_offset}, {midx + 2, midy - 2}, {midx + 2 + mid_offset, midy - 2}};
        if ( ! self.w ) {
            pos[0].x = pos[1].x = pos[2].x = pos[3].x = pos[8].x = pos[9].x = pos[10].x = pos[11].x = midx;
            pos[0].y = pos[1].y = pos[2].y = pos[3].y = pos[8].y = pos[9].y = pos[10].y = pos[11].y = midy;
        }
        if ( ! self.e ) {
            pos[4].x = pos[5].x = pos[6].x = pos[7].x = pos[12].x = pos[13].x = pos[14].x = pos[15].x = midx;
            pos[4].y = pos[5].y = pos[6].y = pos[7].y = pos[12].y = pos[13].y = pos[14].y = pos[15].y = midy;
        }
        CGContextStrokeLineSegments(ctx, pos, 16);
    }

    if ( _column == 4 && (_row == 1 || _row == 8) ) // cross?
    {
        CGPoint crossp[4] = { { midx - CGRectGetWidth(frame), midy - CGRectGetHeight(frame) }, 
                              { midx + CGRectGetWidth(frame), midy + CGRectGetHeight(frame) },
                              { midx - CGRectGetWidth(frame), midy + CGRectGetHeight(frame) },
                              { midx + CGRectGetWidth(frame), midy - CGRectGetHeight(frame) } };
        CGContextStrokeLineSegments(ctx, crossp, 4);
    }
}

- (void)drawInContext:(CGContextRef)ctx
{
    if (_highlightState < HC_HL_ANIMATED) {
        return;
    }

    // INNER circle.
    CGFloat ds = 12.0;
    CGRect innerFrame = self.frame;
    innerFrame.origin = CGPointMake(0, 0);
    innerFrame.origin.x += ds/2;
    innerFrame.origin.y += ds/2;
    innerFrame.size.width -= ds;
    innerFrame.size.height -= ds;
    
    CGContextSetStrokeColorWithColor(ctx, kWhiteColor);
    CGContextSetLineWidth(ctx, 4.0);
    CGContextAddEllipseInRect(ctx, innerFrame);
    CGContextStrokePath(ctx);

    // OUTER circle.
    ds = 8.0;
    CGRect newFrame = self.frame;
    newFrame.origin = CGPointMake(0, 0);
    newFrame.origin.x += ds/2;
    newFrame.origin.y += ds/2;
    newFrame.size.width -= ds;
    newFrame.size.height -= ds;

    CGContextSetStrokeColorWithColor(ctx, self.borderColor);
    CGContextSetLineWidth(ctx, 2.0);
    CGContextAddEllipseInRect(ctx, newFrame);
    CGContextStrokePath(ctx);
}

- (void) setHighlightState:(HighlightEnum)hlState
{
    _highlightState = hlState;
    switch (hlState)
    {
        case HC_HL_NONE:
        {
            self.borderColor = _grid.highlightColor;
            self.borderWidth = 0;
            break;
        }
        case HC_HL_NORMAL:
        {
            self.borderColor = _grid.highlightColor;
            self.borderWidth = 2;
            break;
        }
        case HC_HL_ANIMATED:
        {
            self.borderColor = _grid.animateColor;
            self.borderWidth = 0;
            break;
        }
        case HC_HL_CHECKED:
        {
            self.borderColor = kRedColor;
            self.borderWidth = 0;
            break;
        }
    }
    [self setNeedsDisplay];
}

- (BOOL) highlighted
{
    return (_highlightState == HC_HL_NORMAL);
}

- (void) setHighlighted:(BOOL)highlighted
{
    self.highlightState = (highlighted ? HC_HL_NORMAL : HC_HL_NONE);
}

- (CGPoint) getMidInLayer:(CALayer*)layer
{
    CGRect frame = self.frame;
    CGPoint point = { CGRectGetMidX(frame), CGRectGetMidY(frame) };
    return [self.superlayer convertPoint:point toLayer:layer];
}

- (GridCell*) n { return [_grid cellAtRow:_row+1 column:_column  ]; }
- (GridCell*) e { return [_grid cellAtRow:_row   column:_column+1]; }
- (GridCell*) s { return [_grid cellAtRow:_row-1 column:_column  ]; }
- (GridCell*) w { return [_grid cellAtRow:_row   column:_column-1]; }

@end
