//
//  EMCallManagerVideoWrapper.h
//  Pods
//
//  Created by mac on 2020/2/19.
//

#import <EMWrapper.h>

NS_ASSUME_NONNULL_BEGIN

@interface EMCallManagerVideoWrapper : EMWrapper
- (void)receiveVoidoCall:(EMCallSession *)aSession;
@end

NS_ASSUME_NONNULL_END
