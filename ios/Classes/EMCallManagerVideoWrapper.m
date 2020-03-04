//
//  EMCallManagerVideoWrapper.m
//  Pods
//
//  Created by mac on 2020/2/19.
//

#import "EMCallManagerVideoWrapper.h"
#import "EMHelper.h"
#import "EMSDKMethod.h"
//#import "Call/1v1/DemoCallManager.h"
//#import "Call/1v1/DemoCallManager.h"
//#import "DemoCallManager.h"


@interface EMCallManagerVideoWrapper () <EMCallManagerDelegate>
@property (nonatomic, strong) EMCallSession *callSession;
@end

@implementation EMCallManagerVideoWrapper {
    NSObject <FlutterPluginRegistrar> *_registrar;
}
- (instancetype)initWithChannelName:(NSString *)aChannelName registrar:(NSObject<FlutterPluginRegistrar> *)registrar {
    if (self = [super initWithChannelName:aChannelName registrar:registrar]) {
        _registrar = registrar;
        [EMClient.sharedClient.callManager addDelegate:self delegateQueue:nil];
    }
    return self;
}

#pragma mark - FlutterPlugin

- (void)handleMethodCall:(FlutterMethodCall *)call result:(FlutterResult)result {
    if ([@"makeVideoCall" isEqualToString:call.method]) {
        [self makeVideoCall:call.arguments result:result];
    } else {
        [super handleMethodCall:call result:result];
    }
}

- (void)makeVideoCall:(NSDictionary *)param result:(FlutterResult)result {
    NSString *username = param[@"username"];
    
//    [[EMClient sharedClient].callManager startCall:EMCallTypeVideo remoteName:username record:NO mergeStream:NO ext:nil completion:^(EMCallSession *aCallSession, EMError *aError) {
//        self.callSession = aCallSession;
//
//        CallViewController *callView = [[CallViewController alloc] init];
//        if (callView) {
//            UIWindow *window = [[UIApplication sharedApplication] keyWindow];
//            UIViewController *rootViewController = window.rootViewController;
//            [rootViewController presentViewController:callView animated:YES completion:nil];
//        }
//
//    }];
    
//    [[DemoCallManager sharedManager] _makeCallWithUsername:username type:EMCallTypeVideo record:NO mergeStream:NO ext:nil isCustomVideoData:NO completion:^(EMCallSession *aCallSession, EMError *aError) {
//        self.callSession = aCallSession;
//    }];
    
    
}

@end
