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
#import "SingleSelectionController.h"

@implementation SingleSelectionController

@synthesize _delegate;
@synthesize tag=_tag;
@synthesize selectionIndex=_selectionIndex;

- (id) initWithChoices:(NSArray*)choices
              delegate:(id<SingleSelectionDelegate>)delegate
{
    return [self initWithChoices:choices
                      imageNames:nil subTitles:nil delegate:delegate];
}

- (id) initWithChoices:(NSArray*)choices imageNames:(NSArray*)imageNames
              delegate:(id<SingleSelectionDelegate>)delegate
{
    return [self initWithChoices:choices
                      imageNames:imageNames subTitles:nil delegate:delegate];
}

- (id) initWithChoices:(NSArray*)choices subTitles:(NSArray*)subTitles
              delegate:(id<SingleSelectionDelegate>)delegate
{
    return [self initWithChoices:choices
                      imageNames:nil subTitles:(NSArray*)subTitles
                        delegate:delegate];
}

- (id) initWithChoices:(NSArray*)choices
            imageNames:(NSArray*)imageNames
             subTitles:(NSArray*)subTitles
              delegate:(id<SingleSelectionDelegate>)delegate
{
    if ( (self = [super initWithNibName:@"SingleSelectionView" bundle:nil]) )
    {
        self._delegate = delegate;
        _choices = [[NSArray alloc] initWithArray:choices];
        _imageNames = (imageNames ? [[NSArray alloc] initWithArray:imageNames]
                                  : nil);
        _subTitles = (subTitles ? [[NSArray alloc] initWithArray:subTitles]
                                : nil);
        _selectionIndex = 0;
    }
    return self;
}

- (void) setSelectionIndex:(unsigned int)selection
{
    if (selection < [_choices count]) {
        _selectionIndex = selection;
    }
}

- (CGFloat) rowHeight
{
    return self.tableView.rowHeight;
}

- (void) setRowHeight:(CGFloat)height
{
    self.tableView.rowHeight = height;
}

- (void) viewDidLoad
{
    [super viewDidLoad];
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


#pragma mark Table view methods

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return [_choices count];
}


- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSString* subTitle = nil;
    if (_subTitles) {
        subTitle = [_subTitles objectAtIndex:indexPath.row];
        if ([subTitle length] == 0) subTitle = nil;
    }

    NSString* cellId = [NSString stringWithFormat:@"%ld", (long)indexPath.row];
    UITableViewCell* cell = [tableView dequeueReusableCellWithIdentifier:cellId];
    if (!cell) {
        cell = [[[UITableViewCell alloc] initWithStyle:(subTitle ? UITableViewCellStyleSubtitle
                                                                 : UITableViewCellStyleDefault)
                                       reuseIdentifier:cellId] autorelease];
        cell.detailTextLabel.numberOfLines = 0;
    }
    cell.textLabel.text = [_choices objectAtIndex:indexPath.row];
    cell.accessoryType = (indexPath.row == _selectionIndex ? UITableViewCellAccessoryCheckmark
                                                           : UITableViewCellAccessoryNone);

    if (_imageNames) {
        NSString* imageName = [_imageNames objectAtIndex:indexPath.row];
        UIImage* theImage = [UIImage imageWithContentsOfFile:imageName];
        cell.imageView.image = theImage;
    }

    if (subTitle) {
        cell.detailTextLabel.text = subTitle;
    }

    return cell;
}


- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSIndexPath* oldIndexPath = [NSIndexPath indexPathForRow:_selectionIndex inSection:indexPath.section];
    UITableViewCell* oldCell = [tableView cellForRowAtIndexPath:oldIndexPath];
    oldCell.accessoryType = UITableViewCellAccessoryNone;

    UITableViewCell* cell = [tableView cellForRowAtIndexPath:indexPath];
    cell.accessoryType = UITableViewCellAccessoryCheckmark;
    _selectionIndex = (unsigned int) indexPath.row;

    [_delegate didSelect:self rowAtIndex:_selectionIndex];
}

- (void)dealloc
{
    [_choices release];
    [_imageNames release];
    [_subTitles release];
    [_delegate release];
    [super dealloc];
}


@end

