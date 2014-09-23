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
#import "AboutViewController.h"
#import "Version.h"

@implementation AboutViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    self.title = NSLocalizedString(@"About", @"");
    
    NSString* buildInfo = [NSString stringWithUTF8String:BUILD_INFO];
    NSString* releaseVersion = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleShortVersionString"];
    
    NSString* souceFile = [[NSBundle mainBundle] pathForResource:@"about" ofType:@"html"];
	NSString* rawText = [NSString stringWithContentsOfFile:souceFile encoding:NSUTF8StringEncoding error:nil];
    NSString* renderedPage = [NSString stringWithFormat:rawText, releaseVersion, buildInfo];
    [_webview loadHTMLString:renderedPage baseURL:nil];
}

- (void)dealloc
{
    [_webview release];
    [_activity release];
    [super dealloc];
}

#pragma mark -
#pragma mark WebView delegate

- (void)webViewDidFinishLoad:(UIWebView *)webView
{
    [_activity stopAnimating];
}

- (BOOL)webView:(UIWebView *)webView shouldStartLoadWithRequest:(NSURLRequest *)request
                                                 navigationType:(UIWebViewNavigationType)navigationType
{
    if (navigationType == UIWebViewNavigationTypeLinkClicked) {
        // Start the Safari browser to load in page url.
        [[UIApplication sharedApplication] openURL:[request URL]];
        return NO;
    }
    return YES;
}

@end
