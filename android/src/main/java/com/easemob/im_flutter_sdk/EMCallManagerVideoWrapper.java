package com.easemob.im_flutter_sdk;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.easemob.video.VideoCallActivity;
import com.hyphenate.chat.EMCallManager;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.EMServiceNotReadyException;

import org.json.JSONException;
import org.json.JSONObject;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;

public class EMCallManagerVideoWrapper implements MethodCallHandler {

    private EMCallManager callManager = null;
    // method channel for event broadcast back to flutter
    public static MethodChannel channel;
    public static PluginRegistry.Registrar registrar;

    EMCallManagerVideoWrapper(PluginRegistry.Registrar registrar, MethodChannel channel) {
        EMCallManagerVideoWrapper.channel = channel;
        EMCallManagerVideoWrapper.registrar = registrar;
    }

    private void init() {

    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        if (callManager == null) {
            callManager = EMClient.getInstance().callManager();
            init();
        }
        if ("makeVideoCall".equals(call.method)) {
            makeVideoCall(call.arguments,result);
        }
    }

    private void makeVideoCall(Object args, MethodChannel.Result result) {
        JSONObject argMap = (JSONObject)args;
        try {//单参数
            String username = argMap.getString("username");
            EMClient.getInstance().callManager().makeVideoCall(username);

            // 跳转至视频通话页面
            Intent intent = new Intent()
                    .setClass(registrar.context(), VideoCallActivity.class)
                    .putExtra("username", username).putExtra("type", "call");

            registrar.activity().startActivity(intent);

        } catch (EMServiceNotReadyException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
