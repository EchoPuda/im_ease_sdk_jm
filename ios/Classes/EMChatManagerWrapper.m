//
//  EMChatManagerWrapper.m
//  
//
//  Created by 杜洁鹏 on 2019/10/8.
//

#import "EMChatManagerWrapper.h"
#import "EMSDKMethod.h"
#import "EMHelper.h"
#import "EMCallManagerVideoWrapper.h"

@interface EMChatManagerWrapper () <EMChatManagerDelegate, EMCallManagerDelegate> {
    FlutterEventSink _progressEventSink;
    FlutterEventSink _resultEventSink;
}

@end

@implementation EMChatManagerWrapper
- (instancetype)initWithChannelName:(NSString *)aChannelName
                          registrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    if(self = [super initWithChannelName:aChannelName
                               registrar:registrar]) {
        [EMClient.sharedClient.chatManager addDelegate:self delegateQueue:nil];
        [EMClient.sharedClient.callManager addDelegate:self delegateQueue:nil];
    }
    return self;
}

#pragma mark - FlutterPlugin

- (void)handleMethodCall:(FlutterMethodCall*)call
                  result:(FlutterResult)result {
    if ([EMMethodKeySendMessage isEqualToString:call.method]) {
        [self sendMessage:call.arguments result:result];
    } else if ([EMMethodKeyAckMessageRead isEqualToString:call.method]) {
        [self ackMessageRead:call.arguments result:result];
    } else if ([EMMethodKeyRecallMessage isEqualToString:call.method]) {
        [self recallMessage:call.arguments result:result];
    } else if ([EMMethodKeyGetMessage isEqualToString:call.method]) {
        [self getMessage:call.arguments result:result];
    } else if ([EMMethodKeyGetConversation isEqualToString:call.method]) {
        [self getConversation:call.arguments result:result];
    } else if ([EMMethodKeyMarkAllChatMsgAsRead isEqualToString:call.method]) {
        [self markAllMessagesAsRead:call.arguments result:result];
    } else if ([EMMethodKeyGetUnreadMessageCount isEqualToString:call.method]) {
        [self getUnreadMessageCount:call.arguments result:result];
    } else if ([EMMethodKeySaveMessage isEqualToString:call.method]) {
        [self saveMessage:call.arguments result:result];
    } else if ([EMMethodKeyUpdateChatMessage isEqualToString:call.method]) {
        [self updateChatMessage:call.arguments result:result];
    } else if ([EMMethodKeyDownloadAttachment isEqualToString:call.method]) {
        [self downloadAttachment:call.arguments result:result];
    } else if ([EMMethodKeyDownloadThumbnail isEqualToString:call.method]) {
        [self downloadThumbnail:call.arguments result:result];
    } else if ([EMMethodKeyImportMessages isEqualToString:call.method]) {
        [self importMessages:call.arguments result:result];
    } else if ([EMMethodKeyGetConversationsByType isEqualToString:call.method]) {
        [self getConversationsByType:call.arguments result:result];
    } else if ([EMMethodKeyDownloadFile isEqualToString:call.method]) {
        [self downloadFile:call.arguments result:result];
    } else if ([EMMethodKeyGetAllConversations isEqualToString:call.method]) {
        [self getAllConversations:call.arguments result:result];
    } else if ([EMMethodKeyLoadAllConversations isEqualToString:call.method]) {
        [self loadAllConversations:call.arguments result:result];
    } else if ([EMMethodKeyDeleteConversation isEqualToString:call.method]) {
        [self deleteConversation:call.arguments result:result];
    } else if ([EMMethodKeySetVoiceMessageListened isEqualToString:call.method]) {
        [self setVoiceMessageListened:call.arguments result:result];
    } else if ([EMMethodKeyUpdateParticipant isEqualToString:call.method]) {
        [self updateParticipant:call.arguments result:result];
    } else if ([EMMethodKeyFetchHistoryMessages isEqualToString:call.method]) {
        [self fetchHistoryMessages:call.arguments result:result];
    } else if ([EMMethodKeySearchChatMsgFromDB isEqualToString:call.method]) {
        [self searchChatMsgFromDB:call.arguments result:result];
    } else if ([EMMethodKeyGetCursor isEqualToString:call.method]) {
        [self getCursor:call.arguments result:result];
    }
    
    else if ([@"addMessageListener" isEqualToString:call.method]) {
        [self getCursor:call.arguments result:result];
    }
    else if ([@"makeVoiceCall" isEqualToString:call.method]) {
        [self makeVoiceCall:call.arguments result:result];
    } else if ([@"answerCall" isEqualToString:call.method]) {
        [self answerCall:call.arguments result:result];
    } else if ([@"rejectCall" isEqualToString:call.method]) {
        [self rejectCall:call.arguments result:result];
    } else if ([@"endCall" isEqualToString:call.method]) {
        [self endCall:call.arguments result:result];
    } else if ([@"openSpeaker" isEqualToString:call.method]) {
        [self openSpeaker:call.arguments result:result];
    } else if ([@"closeSpeaker" isEqualToString:call.method]) {
        [self closeSpeaker:call.arguments result:result];
    } else if ([@"pauseVoice" isEqualToString:call.method]) {
        [self pauseVoice:call.arguments result:result];
    } else if ([@"resumeVoice" isEqualToString:call.method]) {
        [self resumeVoice:call.arguments result:result];
    } else if ([@"getInComingCall" isEqualToString:call.method]) {
        [self getInComingCall:call.arguments result:result];
    } else {
        [super handleMethodCall:call result:result];
    }
}

+ (void)registerWithRegistrar:(nonnull NSObject<FlutterPluginRegistrar> *)registrar {
    
}


#pragma mark - Actions

- (void)sendMessage:(NSDictionary *)param result:(FlutterResult)result {
    
    EMMessage *msg = [EMHelper dictionaryToMessage:param];
    
    __block void (^progress)(int progress) = ^(int progress) {
        [self.channel invokeMethod:EMMethodKeyOnMessageStatusOnProgress
                         arguments:@{@"progress":@(progress)}];
    };
    
    __block void (^completion)(EMMessage *message, EMError *error) = ^(EMMessage *message, EMError *error) {
        [self wrapperCallBack:result
                        error:error
                     userInfo:@{@"message":[EMHelper messageToDictionary:message]}];
    };
    
    
    [EMClient.sharedClient.chatManager sendMessage:msg
                                          progress:progress
                                        completion:completion];
}

- (void)ackMessageRead:(NSDictionary *)param result:(FlutterResult)result {
    
}

- (void)recallMessage:(NSDictionary *)param result:(FlutterResult)result {
    
}

- (void)getMessage:(NSDictionary *)param result:(FlutterResult)result {
    
}

- (void)getConversation:(NSDictionary *)param result:(FlutterResult)result {
    NSString *conversationId = param[@"id"];
    EMConversationType type;
    if ([param[@"type"] isKindOfClass:[NSNull class]]){
        type = EMConversationTypeChat;
    }else {
        type = (EMConversationType)[param[@"type"] intValue];
    }
    BOOL isCreateIfNotExists = [param[@"createIfNotExists"] boolValue];
    
    EMConversation *conversation = [EMClient.sharedClient.chatManager getConversation:conversationId
                                                                                 type:type
                                                                     createIfNotExist:isCreateIfNotExists];
    [self wrapperCallBack:result
                    error:nil
                 userInfo:@{@"conversation":[EMHelper conversationToDictionary:conversation]}];
}

// TODO: ios需调添加该实现
- (void)markAllMessagesAsRead:(NSDictionary *)param result:(FlutterResult)result {
    
}

// TODO: ios需调添加该实现
- (void)getUnreadMessageCount:(NSDictionary *)param result:(FlutterResult)result {
    NSArray *conversations = [[EMClient sharedClient].chatManager getAllConversations];
    NSInteger unreadCount = 0;
    for (EMConversation *conversation in conversations) {
        unreadCount += conversation.unreadMessagesCount;
    }
    NSNumber *count = [NSNumber numberWithInteger:unreadCount];
    NSMutableDictionary *dic = [NSMutableDictionary dictionary];
    dic[@"success"] = @YES;
    dic[@"count"] = count;
    result(dic);
}

// TODO: 目前这种方式实现后，消息id不一致，考虑如何处理。
- (void)saveMessage:(NSDictionary *)param result:(FlutterResult)result {
    
}

// TODO: 目前这种方式实现后，消息id不一致，考虑如何处理。
- (void)updateChatMessage:(NSDictionary *)param result:(FlutterResult)result {
    __weak typeof(self)weakSelf = self;
    EMMessage *msg = [EMHelper updateDataMapToMessage:param];
    [EMClient.sharedClient.chatManager updateMessage:msg completion:^(EMMessage *aMessage, EMError *aError)
     {
        [weakSelf wrapperCallBack:result
                            error:aError
                         userInfo:@{@"status" : [NSNumber numberWithBool:(aError == nil)]}];
    }];
}

- (void)downloadAttachment:(NSDictionary *)param result:(FlutterResult)result {
    [EMClient.sharedClient.chatManager downloadMessageAttachment:[EMHelper dictionaryToMessage:param]
                                                        progress:^(int progress)
     {
        
    } completion:^(EMMessage *message, EMError *error)
     {
        
    }];
}

- (void)downloadThumbnail:(NSDictionary *)param result:(FlutterResult)result {
    [EMClient.sharedClient.chatManager downloadMessageThumbnail:[EMHelper dictionaryToMessage:param]
                                                       progress:^(int progress)
     {
        
    } completion:^(EMMessage *message, EMError *error)
     {
        
    }];
}

// TODO: 目前这种方式实现后，消息id不一致，考虑如何处理。
- (void)importMessages:(NSDictionary *)param result:(FlutterResult)result {
    
}

// TODO: ios需调添加该实现
- (void)getConversationsByType:(NSDictionary *)param result:(FlutterResult)result {
    //    EMConversationType type = (EMConversationType)[param[@"type"] intValue];
    //    EMClient.sharedClient.chatManager
}

// TODO: ios需调添加该实现
- (void)downloadFile:(NSDictionary *)param result:(FlutterResult)result {
    
}


- (void)getAllConversations:(NSDictionary *)param result:(FlutterResult)result {
    NSArray *conversations = [EMClient.sharedClient.chatManager getAllConversations];
    NSMutableArray *conversationDictList = [NSMutableArray array];
    for (EMConversation *conversation in conversations) {
        [conversationDictList addObject:[EMHelper conversationToDictionary:conversation]];
    }
    [self wrapperCallBack:result error:nil userInfo:@{@"conversations" : conversationDictList}];
}


- (void)loadAllConversations:(NSDictionary *)param result:(FlutterResult)result {
    [self getAllConversations:param result:result];
}

- (void)deleteConversation:(NSDictionary *)param result:(FlutterResult)result {
    __weak typeof(self)weakSelf = self;
    NSString *conversationId = param[@"userName"];
    BOOL deleteMessages = [param[@"deleteMessages"] boolValue];
    [EMClient.sharedClient.chatManager deleteConversation:conversationId
                                         isDeleteMessages:deleteMessages
                                               completion:^(NSString *aConversationId, EMError *aError)
     {
        [weakSelf wrapperCallBack:result
                            error:aError
                         userInfo:@{@"status" : [NSNumber numberWithBool:(aError == nil)]}];
    }];
}

// ??
- (void)setVoiceMessageListened:(NSDictionary *)param result:(FlutterResult)result {
    
}

// ??
- (void)updateParticipant:(NSDictionary *)param result:(FlutterResult)result {
    
}


- (void)fetchHistoryMessages:(NSDictionary *)param result:(FlutterResult)result {
    __weak typeof(self)weakSelf = self;
    NSString *conversationId = param[@"id"];
    EMConversationType type = (EMConversationType)[param[@"type"] intValue];
    int pageSize = [param[@"pageSize"] intValue];
    NSString *startMsgId = param[@"startMsgId"];
    [EMClient.sharedClient.chatManager asyncFetchHistoryMessagesFromServer:conversationId
                                                          conversationType:type
                                                            startMessageId:startMsgId
                                                                  pageSize:pageSize
                                                                completion:^(EMCursorResult *aResult, EMError *aError)
     {
        NSArray *msgAry = aResult.list;
        NSMutableArray *msgList = [NSMutableArray array];
        for (EMMessage *msg in msgAry) {
            [msgList addObject:[EMHelper messageToDictionary:msg]];
        }
        
        [weakSelf wrapperCallBack:result error:aError userInfo:@{@"messages" : msgList,
                                                                 @"cursor" : aResult.cursor}];
    }];
}

- (void)searchChatMsgFromDB:(NSDictionary *)param result:(FlutterResult)result {
    __weak typeof(self) weakSelf = self;
    NSString *keywords = param[@"keywords"];
    long long timeStamp = [param[@"timeStamp"] longLongValue];
    int maxCount = [param[@"maxCount"] intValue];
    NSString *from = param[@"from"];
    EMMessageSearchDirection direction = (EMMessageSearchDirection)[param[@"direction"] intValue];
    [EMClient.sharedClient.chatManager loadMessagesWithKeyword:keywords
                                                     timestamp:timeStamp
                                                         count:maxCount
                                                      fromUser:from
                                               searchDirection:direction
                                                    completion:^(NSArray *aMessages, EMError *aError)
     {
        NSMutableArray *msgList = [NSMutableArray array];
        for (EMMessage *msg in aMessages) {
            [msgList addObject:[EMHelper messageToDictionary:msg]];
        }
        
        [weakSelf wrapperCallBack:result error:aError userInfo:@{@"messages":msgList}];
    }];
}

// ??
- (void)getCursor:(NSDictionary *)param result:(FlutterResult)result {
    
}

// ------------------------------------------------------------------------
#pragma mark - EMCallManager

static NSString *callId = @"";

// 拨打语音通话
- (void)makeVoiceCall:(NSDictionary *)param result:(FlutterResult)result {
    NSString *username = param[@"username"];
    [[EMClient sharedClient].callManager startCall:EMCallTypeVoice remoteName:username ext:nil completion:^(EMCallSession *aCakkSession, EMError *aError) {
        callSession = aCakkSession;
        callId = aCakkSession.callId;
    }];
}

// 接受语音通话
- (void)answerCall:(NSDictionary *)param result:(FlutterResult)result {
    EMError *error = nil;
    error = [[EMClient sharedClient].callManager answerIncomingCall:callId];
}

// 拒绝语音通话
- (void)rejectCall:(NSDictionary *)param result:(FlutterResult)result {
    EMError *error = nil;
    error = [[EMClient sharedClient].callManager endCall:callId reason:EMCallEndReasonDecline];
}

// 挂断语音通话
- (void)endCall:(NSDictionary *)param result:(FlutterResult)result {
    EMError *error = nil;
    error = [[EMClient sharedClient].callManager endCall:callId reason:EMCallEndReasonHangup];
}

// 获取当前是否有通话
- (void)getInComingCall:(NSDictionary *)param result:(FlutterResult)result {
    if (callSession == nil) {
        result(@NO);
    } else {
        if ([callId isEqualToString:@""]) {
            result(@NO);
        } else {
            result(@YES);
        }
    }
}

// 打开扬声器
- (void)openSpeaker:(NSDictionary *)param result:(FlutterResult)result {
    [[AVAudioSession sharedInstance] overrideOutputAudioPort:AVAudioSessionPortOverrideSpeaker error:nil];
}

// 关闭扬声器
- (void)closeSpeaker:(NSDictionary *)param result:(FlutterResult)result {
    [[AVAudioSession sharedInstance] overrideOutputAudioPort:AVAudioSessionPortOverrideNone error:nil];
}

// 停止声音传输
- (void)pauseVoice:(NSDictionary *)param result:(FlutterResult)result {
    EMError *error = nil;
    error = callSession.pauseVoice;
}

// 打开声音传输
- (void)resumeVoice:(NSDictionary *)param result:(FlutterResult)result {
    EMError *error = nil;
    error = callSession.resumeVoice;
}

- (void)dealloc{
    [[EMClient sharedClient].callManager removeDelegate:self];
}

#pragma mark - EMCallManagerDelegate

static EMCallSession *callSession = nil;

- (void)callDidReceive:(EMCallSession *)aSession{
    NSLog(@"onCallReceive-----------------------------------");
    if (callSession == nil) {
        callSession = aSession;
    }
    if (aSession.remoteName != NULL) {
        callId = aSession.callId;
        NSString *type;
        if (aSession.type == EMCallTypeVideo) {
            type = @"video";
//            [[EMCallManagerVideoWrapper self] receiveVoidoCall:aSession];
            EMCallManagerVideoWrapper *wrap = [[EMCallManagerVideoWrapper alloc] init];
            [wrap receiveVoidoCall:aSession];
        } else {
            type = @"voice";
            [self.channel invokeMethod:@"onCallReceive" arguments:@{@"from":aSession.remoteName,@"type":type}];
        }

    }
}

- (void)callDidConnect:(EMCallSession *)aSession{
    if (callSession == nil) {
        callSession = aSession;
    }
    callId = aSession.callId;
    EMError *error = nil;
    error = aSession.resumeVoice;
    [self.channel invokeMethod:@"onCallState" arguments:@{@"status":@2}];
}

- (void)callDidAccept:(EMCallSession *)aSession{
    if (callSession == nil) {
        callSession = aSession;
    }
    callId = aSession.callId;
    [self.channel invokeMethod:@"onCallState" arguments:@{@"status":@0}];
}

- (void)callDidEnd:(EMCallSession *)aSession reason:(EMCallEndReason)aReason error:(EMError *)aError{
    NSNumber *reasonCode;
    if (aReason != EMCallEndReasonHangup) {
        reasonCode = @-1;
        if (aError) {
            reasonCode = @-1;
        } else {
            NSString *reasonStr = @"通话结束";
            switch (aReason) {
                case EMCallEndReasonNoResponse:
                    reasonStr = @"没有响应";
                    reasonCode = @-6;
                    break;
                case EMCallEndReasonDecline:
                    reasonStr = @"对方拒绝接通通话";
                    reasonCode = @-3;
                    break;
                case EMCallEndReasonBusy:
                    reasonStr = @"对方正在通话中";
                    reasonCode = @-7;
                    break;
                case EMCallEndReasonFailed:
                    reasonStr = @"通话建立连接失败";
                    reasonCode = @-5;
                    break;
                case EMCallEndReasonRemoteOffline:
                    reasonStr = @"对方不在线，无法接听通话";
                    reasonCode = @-4;
                    break;
                case EMCallEndReasonNotEnable:
                    reasonStr = @"服务未开通";
                case EMCallEndReasonServiceArrearages:
                    reasonStr = @"余额不足";
                case EMCallEndReasonServiceForbidden:
                    reasonStr = @"服务被拒绝";
                default:
                    reasonCode = @-1;
                    break;
            }
            
        }
    } else {
        reasonCode = @-1;
    }
    callId = @"";
    callSession = nil;
    [self.channel invokeMethod:@"onCallState" arguments:@{@"status":reasonCode}];
}

// ------------------------------------------------------------------------

#pragma mark - EMChatManagerDelegate

// TODO: 安卓没有参数，是否参数一起返回？
- (void)conversationListDidUpdate:(NSArray *)aConversationList {
    [self.channel invokeMethod:EMMethodKeyOnConversationUpdate
                     arguments:nil];
}

- (void)messagesDidReceive:(NSArray *)aMessages {
    NSMutableArray *msgList = [NSMutableArray array];
    for (EMMessage *msg in aMessages) {
        [msgList addObject:[EMHelper messageToDictionary:msg]];
    }
    NSLog(@"has receive messages -- %@", msgList);
    [self.channel invokeMethod:EMMethodKeyOnMessageReceived arguments:@{@"messages":msgList}];
}

- (void)cmdMessagesDidReceive:(NSArray *)aCmdMessages {
    NSMutableArray *cmdMsgList = [NSMutableArray array];
    for (EMMessage *msg in aCmdMessages) {
        [cmdMsgList addObject:[EMHelper messageToDictionary:msg]];
    }
    
    [self.channel invokeMethod:EMMethodKeyOnCmdMessageReceived arguments:@{@"messages":cmdMsgList}];
}

- (void)messagesDidRead:(NSArray *)aMessages {
    NSMutableArray *msgList = [NSMutableArray array];
    for (EMMessage *msg in aMessages) {
        [msgList addObject:[EMHelper messageToDictionary:msg]];
    }
    
    [self.channel invokeMethod:EMMethodKeyOnMessageRead arguments:@{@"messages":msgList}];
}

- (void)messagesDidDeliver:(NSArray *)aMessages {
    NSMutableArray *msgList = [NSMutableArray array];
    for (EMMessage *msg in aMessages) {
        [msgList addObject:[EMHelper messageToDictionary:msg]];
    }
    
    [self.channel invokeMethod:EMMethodKeyOnMessageDelivered arguments:@{@"messages":msgList}];
}

- (void)messagesDidRecall:(NSArray *)aMessages {
    NSMutableArray *msgList = [NSMutableArray array];
    for (EMMessage *msg in aMessages) {
        [msgList addObject:[EMHelper messageToDictionary:msg]];
    }
    
    [self.channel invokeMethod:EMMethodKeyOnMessageRecalled arguments:@{@"messages":msgList}];
}

- (void)messageStatusDidChange:(EMMessage *)aMessage
                         error:(EMError *)aError {
    NSDictionary *msgDict = [EMHelper messageToDictionary:aMessage];
    [self.channel invokeMethod:EMMethodKeyOnMessageChanged arguments:@{@"message":msgDict}];
}

// TODO: 安卓未找到对应回调
- (void)messageAttachmentStatusDidChange:(EMMessage *)aMessage
                                   error:(EMError *)aError {
    
}


@end
