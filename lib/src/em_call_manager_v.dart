
import 'package:flutter/services.dart';

class EMCallManagerVideo {
  static const _channelPrefix = 'com.easemob.im';
  static const MethodChannel _emChatManagerChannel =
  const MethodChannel('$_channelPrefix/em_call_manager_v', JSONMethodCodec());

  static EMCallManagerVideo _instance;

  EMCallManagerVideo._internal() {
    _addNativeMethodCallHandler();
  }

  factory EMCallManagerVideo.getInstance() {
    return _instance = _instance ?? EMCallManagerVideo._internal();
  }

  void _addNativeMethodCallHandler() {

  }

  /// 拨打视频通话 [username]，自动进入视频通话界面，视频通话在原生处理
  void startVideoCall(String username) {
    _emChatManagerChannel.invokeMethod("makeVideoCall",{"username" : username});
  }



}