package com.easemob.im_flutter_sdk;

import android.content.Context;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMCallManager;
import com.hyphenate.chat.EMCallStateChangeListener;
import com.hyphenate.chat.EMChatManager;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMCursorResult;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.exceptions.EMNoActiveCallException;
import com.hyphenate.exceptions.EMServiceNotReadyException;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

import static com.hyphenate.chat.EMClient.TAG;


@SuppressWarnings("unchecked")
public class EMChatManagerWrapper implements MethodCallHandler, EMWrapper{
    // delegates all methods call to this manager
    private EMChatManager manager = null;
    private EMCallManager callManager = null;
    // method channel for event broadcast back to flutter
    public static MethodChannel channel;
    public static PluginRegistry.Registrar registrar;
    // cursor result map for call back getCursor()
    private Map<String, EMCursorResult<EMMessage>> cursorResultList = new HashMap<String, EMCursorResult<EMMessage>>();

    EMChatManagerWrapper(PluginRegistry.Registrar registrar, MethodChannel channel) {
        EMChatManagerWrapper.channel = channel;
        EMChatManagerWrapper.registrar = registrar;
    }

    private void init() {
        //setup message listener
        manager.addMessageListener(new EMMessageListener() {
            @Override
            public void onMessageReceived(List<EMMessage> messages) {
                Map<String, Object> data = new HashMap<String, Object>();
                ArrayList<Map<String, Object>> msgs = new ArrayList<>();
                for(EMMessage message : messages) {
                    msgs.add(EMHelper.convertEMMessageToStringMap(message));
                }
                data.put("messages", msgs);
                EMLog.e("onMessageReceived->>",data.toString());
                post((Void)->{
                    channel.invokeMethod(EMSDKMethod.onMessageReceived, data);
                });
            }

            @Override
            public void onCmdMessageReceived(List<EMMessage> messages) {
                Map<String, Object> data = new HashMap<String, Object>();
                ArrayList<Map<String, Object>> msgs = new ArrayList<Map<String, Object>>();
                for(EMMessage message : messages) {
                    msgs.add(EMHelper.convertEMMessageToStringMap(message));
                }
                data.put("messages", msgs);
                post((Void)->{
                    channel.invokeMethod(EMSDKMethod.onCmdMessageReceived, data);
                });
            }

            @Override
            public void onMessageRead(List<EMMessage> messages) {
                Map<String, Object> data = new HashMap<String, Object>();
                ArrayList<Map<String, Object>> msgs = new ArrayList<Map<String, Object>>();
                for(EMMessage message : messages) {
                    msgs.add(EMHelper.convertEMMessageToStringMap(message));
                }
                data.put("messages", msgs);
                post((Void)->{
                    channel.invokeMethod(EMSDKMethod.onMessageRead, data);
                });
            }

            @Override
            public void onMessageDelivered(List<EMMessage> messages) {
                Map<String, Object> data = new HashMap<String, Object>();
                ArrayList<Map<String, Object>> msgs = new ArrayList<Map<String, Object>>();
                for(EMMessage message : messages) {
                    msgs.add(EMHelper.convertEMMessageToStringMap(message));
                }
                data.put("messages", msgs);
                post((Void)->{
                    channel.invokeMethod(EMSDKMethod.onMessageDelivered, data);
                });
            }

            @Override
            public void onMessageRecalled(List<EMMessage> messages) {
                Map<String, Object> data = new HashMap<String, Object>();
                ArrayList<Map<String, Object>> msgs = new ArrayList<Map<String, Object>>();
                for(EMMessage message : messages) {
                    msgs.add(EMHelper.convertEMMessageToStringMap(message));
                }
                data.put("messages", msgs);
                post((Void)->{
                    channel.invokeMethod(EMSDKMethod.onMessageRecalled, data);
                });
            }

            @Override
            public void onMessageChanged(EMMessage message, Object change) {
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("message", EMHelper.convertEMMessageToStringMap(message));
                post((Void)->{
                    channel.invokeMethod(EMSDKMethod.onMessageChanged, data);
                });

            }
        });
        //setup conversation listener
        manager.addConversationListener(() -> {
            Map<String, Object> data = new HashMap<String, Object>();
            post((Void)->{
                channel.invokeMethod(EMSDKMethod.onConversationUpdate,data);
            });
        });

        IntentFilter callFilter = new IntentFilter(callManager.getIncomingCallBroadcastAction());
        if(callReceiver == null){
            callReceiver = new CallReceiver();
        }
        registrar.context().registerReceiver(callReceiver, callFilter);

        callStateListener = (callState, error) -> {
            Map<String, Object> data = new HashMap<String, Object>();
            System.out.println("当前通话状态：" + callState.toString());
            switch (callState) {
                case CONNECTING: // 正在连接对方
                    post((Void)->{
                        data.put("status",1);
                        channel.invokeMethod("onCallState", data);
                    });
                    break;
                case CONNECTED: // 双方已经建立连接
                    post((Void)->{
                        data.put("status",2);
                        channel.invokeMethod("onCallState", data);
                    });
                    break;

                case ACCEPTED: // 电话接通成功
                    closeSpeakerOn();
                    post((Void)->{
                        data.put("status",0);
                        channel.invokeMethod("onCallState", data);
                    });
                    PhoneStateManager.get(registrar.activity()).addStateCallback(phoneStateCallback);
                    try {
                        callManager.resumeVoiceTransfer();
                    } catch (HyphenateException e) {
                        e.printStackTrace();
                    }
                    break;
                case DISCONNECTED: // 电话断了
                    switch (error) {
                        case REJECTED: //拒接
                            post((Void)->{
                                data.put("status",-3);
                                channel.invokeMethod("onCallState", data);
                            });
                            break;
                        case ERROR_UNAVAILABLE: //对方不在线
                            post((Void)->{
                                data.put("status",-4);
                                channel.invokeMethod("onCallState", data);
                            });
                            break;
                        case ERROR_TRANSPORT: //连接失败
                            post((Void)->{
                                data.put("status",-5);
                                channel.invokeMethod("onCallState", data);
                            });
                            break;
                        case ERROR_NORESPONSE: //无人接听
                            post((Void)->{
                                data.put("status",-6);
                                channel.invokeMethod("onCallState", data);
                            });
                            break;
                        case ERROR_BUSY: //对方正忙
                            post((Void)->{
                                data.put("status",-7);
                                channel.invokeMethod("onCallState", data);
                            });
                            break;
                        default:
                            post((Void)->{
                                data.put("status",-1);
                                channel.invokeMethod("onCallState", data);
                            });
                            break;
                    }
                    post((Void)->{
                        data.put("status",-1);
                        channel.invokeMethod("onCallState", data);
                    });
                    PhoneStateManager.get(registrar.activity()).removeStateCallback(phoneStateCallback);
                    break;
                case NETWORK_UNSTABLE: //网络不稳定
                    if(error == EMCallStateChangeListener.CallError.ERROR_NO_DATA){
                        //无通话数据
                        post((Void)->{
                            data.put("status",-2);
                            channel.invokeMethod("onCallState", data);
                        });
                    }else{
                        post((Void)->{
                            data.put("status",-2);
                            channel.invokeMethod("onCallState", data);
                        });
                    }
                    break;
                case NETWORK_NORMAL: //网络恢复正常
                    post((Void)->{
                        data.put("status",3);
                        channel.invokeMethod("onCallState", data);
                    });
                    break;
                case RINGING: //正在拨入
                    post((Void)->{
                        data.put("status",4);
                        channel.invokeMethod("onCallState", data);
                    });
                    break;
                case ANSWERING: //正在接听
                    post((Void)->{
                        data.put("status",5);
                        channel.invokeMethod("onCallState", data);
                    });
                    break;
                default:
                    break;
            }

        };
        callManager.addCallStateChangeListener(callStateListener);
        audioManager = (AudioManager) registrar.activity().getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if(manager == null) {
            manager = EMClient.getInstance().chatManager();
            callManager = EMClient.getInstance().callManager();
            init();
        }
        if (EMSDKMethod.sendMessage.equals(call.method)) {
            sendMessage(call.arguments, result);
        } else if (EMSDKMethod.ackMessageRead.equals(call.method)) {
            ackMessageRead(call.arguments, result);
        } else if (EMSDKMethod.recallMessage.equals(call.method)) {
            recallMessage(call.arguments, result);
        } else if (EMSDKMethod.getMessage.equals(call.method)) {
            getMessage(call.arguments, result);
        }else if(EMSDKMethod.getConversation.equals(call.method)) {
            getConversation(call.arguments, result);
        }else if(EMSDKMethod.markAllChatMsgAsRead.equals(call.method)) {
            markAllConversationsAsRead(call.arguments, result);
        }else if(EMSDKMethod.getUnreadMessageCount.equals(call.method)) {
            getUnreadMessageCount(call.arguments,result);
        }else if(EMSDKMethod.saveMessage.equals(call.method)) {
            saveMessage(call.arguments, result);
        }else if(EMSDKMethod.updateChatMessage.equals(call.method)) {
            updateMessage(call.arguments, result);
        }else if(EMSDKMethod.downloadAttachment.equals(call.method)) {
            downloadAttachment(call.arguments, result);
        }else if(EMSDKMethod.downloadThumbnail.equals(call.method)) {
            downloadThumbnail(call.arguments, result);
        }else if(EMSDKMethod.importMessages.equals(call.method)) {
            importMessages(call.arguments, result);
        }else if(EMSDKMethod.getConversationsByType.equals(call.method)) {
            getConversationsByType(call.arguments, result);
        }else if(EMSDKMethod.downloadFile.equals(call.method)) {
            downloadFile(call.arguments, result);
        }else if(EMSDKMethod.getAllConversations.equals(call.method)) {
            getAllConversations(call.arguments, result);
        }else if(EMSDKMethod.loadAllConversations.equals(call.method)) {
            loadAllConversations(call.arguments, result);
        }else if(EMSDKMethod.deleteConversation.equals(call.method)) {
            deleteConversation(call.arguments, result);
        }else if(EMSDKMethod.setVoiceMessageListened.equals(call.method)) {
            setVoiceMessageListened(call.arguments, result);
        }else if(EMSDKMethod.updateParticipant.equals(call.method)) {
            updateParticipant(call.arguments, result);
        }else if(EMSDKMethod.fetchHistoryMessages.equals(call.method)) {
            fetchHistoryMessages(call.arguments, result);
        }else if(EMSDKMethod.searchChatMsgFromDB.equals(call.method)) {
            searchMsgFromDB(call.arguments, result);
        }else if(EMSDKMethod.getCursor.equals(call.method)) {
            getCursor(call.arguments, result);
        }

        //----------------------------------------------------------------------------------------------
        //----------------------------------------------------------------------------------------------
        else if ("makeVoiceCall".equals(call.method)) {
            makeVoiceCall(call.arguments, result);
        }else if ("answerCall".equals(call.method)) {
            answerCall(call.arguments, result);
        }else if ("rejectCall".equals(call.method)) {
            rejectCall(call.arguments, result);
        }else if ("endCall".equals(call.method)) {
            endCall(call.arguments, result);
        }else if ("addCallReceiverListener".equals(call.method)) {
            addCallReceiverChangeListener(call.arguments, result);
        }else if ("addCallStateChangeListener".equals(call.method)) {
            addCallStateChangeListener(call.arguments, result);
        }else if ("removeCallStateChangeListener".equals(call.method)) {
            removeCallStateChangeListener(call.arguments, result);
        }else if ("openSpeaker".equals(call.method)) {
            openSpeaker(call.arguments, result);
        }else if ("closeSpeaker".equals(call.method)) {
            closeSpeaker(call.arguments, result);
        }else if ("pauseVoice".equals(call.method)) {
            pauseVoice(call.arguments, result);
        }else if ("resumeVoice".equals(call.method)) {
            resumeVoice(call.arguments, result);
        }else if ("getInComingCall".equals(call.method)) {
            getInComingCall(call.arguments, result);
        }

    }

    //----------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private CallReceiver callReceiver;
    private boolean isMuteState;
    private AudioManager audioManager;

    /**
     * 电话接收注册
     */
    private void addCallReceiverChangeListener(Object args, Result result) {
        Log.d(TAG, "addCallReceiverChangeListener: ---------------------------------------");
        IntentFilter callFilter = new IntentFilter(callManager.getIncomingCallBroadcastAction());
        if(callReceiver == null){
            callReceiver = new CallReceiver();
        }
        registrar.context().registerReceiver(callReceiver, callFilter);
    }

    private void makeVoiceCall(Object args, Result result) {
        JSONObject argMap = (JSONObject)args;
        try {
            String username = argMap.getString("username");
            callManager.makeVoiceCall(username);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (EMServiceNotReadyException e) {
            e.printStackTrace();
        }
    }

    private void answerCall(Object args, Result result) {
        try {
            callManager.answerCall();
        } catch (EMNoActiveCallException e) {
            e.printStackTrace();
        }
    }

    private void rejectCall(Object args, Result result) {
        try {
            callManager.rejectCall();
        } catch (EMNoActiveCallException e) {
            e.printStackTrace();
        }
    }

    private void endCall(Object args, Result result) {
        try {
            callManager.endCall();
        } catch (EMNoActiveCallException e) {
            e.printStackTrace();
        }
    }

    private void getInComingCall(Object args, Result result) {
        boolean isInComingCall;
        if (EMClient.getInstance().callManager().getCurrentCallSession() == null) {
            isInComingCall = false;
        } else {
            isInComingCall = true;
        }
        result.success(isInComingCall);
    }

    private EMCallStateChangeListener callStateListener;

    /**
     * 注册通话状态监听
     */
    private void addCallStateChangeListener(Object args, Result result) {
        Log.d(TAG, "addCallStateChangeListener: ---------------------------------------");
        audioManager = (AudioManager) registrar.activity().getSystemService(Context.AUDIO_SERVICE);
        callStateListener = (callState, error) -> {
            System.out.println("当前通话状态：" + callState.toString());
            switch (callState) {
                case CONNECTING: // 正在连接对方
                    post((Void)->{
                        channel.invokeMethod("onCallState", 1);
                    });
                    break;
                case CONNECTED: // 双方已经建立连接
                    post((Void)->{
                        channel.invokeMethod("onCallState", 2);
                    });
                    break;

                case ACCEPTED: // 电话接通成功
                    closeSpeakerOn();
                    post((Void)->{
                        channel.invokeMethod("onCallState", 0);
                    });
                    PhoneStateManager.get(registrar.activity()).addStateCallback(phoneStateCallback);
                    try {
                        callManager.resumeVoiceTransfer();
                    } catch (HyphenateException e) {
                        e.printStackTrace();
                    }
                    break;
                case DISCONNECTED: // 电话断了
                    switch (error) {
                        case REJECTED: //拒接
                            post((Void)->{
                                channel.invokeMethod("onCallState", -3);
                            });
                            break;
                        case ERROR_UNAVAILABLE: //对方不在线
                            post((Void)->{
                                channel.invokeMethod("onCallState", -4);
                            });
                            break;
                        case ERROR_TRANSPORT: //连接失败
                            post((Void)->{
                                channel.invokeMethod("onCallState", -5);
                            });
                            break;
                        case ERROR_NORESPONSE: //无人接听
                            post((Void)->{
                                channel.invokeMethod("onCallState", -6);
                            });
                            break;
                        case ERROR_BUSY: //对方正忙
                            post((Void)->{
                                channel.invokeMethod("onCallState", -7);
                            });
                            break;
                        default:
                            post((Void)->{
                                channel.invokeMethod("onCallState", -8);
                            });
                            break;
                    }
                    PhoneStateManager.get(registrar.activity()).removeStateCallback(phoneStateCallback);
                    break;
                case NETWORK_UNSTABLE: //网络不稳定
                    if(error == EMCallStateChangeListener.CallError.ERROR_NO_DATA){
                        //无通话数据
                        post((Void)->{
                            channel.invokeMethod("onCallState", -2);
                        });
                    }else{
                        post((Void)->{
                            channel.invokeMethod("onCallState", -2);
                        });
                    }
                    break;
                case NETWORK_NORMAL: //网络恢复正常
                    post((Void)->{
                        channel.invokeMethod("onCallState", 3);
                    });
                    break;
                case RINGING: //正在拨入
                    post((Void)->{
                        channel.invokeMethod("onCallState", 4);
                    });
                    break;
                case ANSWERING: //正在接听
                    post((Void)->{
                        channel.invokeMethod("onCallState", 5);
                    });
                    break;
                default:
                    break;
            }

        };
        callManager.addCallStateChangeListener(callStateListener);
    }

    private void removeCallStateChangeListener(Object args, Result result) {
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setMicrophoneMute(false);

        if(callStateListener != null) {
            callManager.removeCallStateChangeListener(callStateListener);
        }

    }

    private void openSpeaker(Object args, Result result) {
        openSpeakerOn();
    }

    private void closeSpeaker(Object args, Result result) {
        closeSpeakerOn();
    }

    private void openSpeakerOn() {
        try {
            if (!audioManager.isSpeakerphoneOn()) {
                audioManager.setSpeakerphoneOn(true);
            }
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeSpeakerOn() {
        try {
            if (audioManager != null) {
                if (audioManager.isSpeakerphoneOn()) {
                    audioManager.setSpeakerphoneOn(false);
                }
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pauseVoice(Object args, Result result) {
        try {
            callManager.pauseVoiceTransfer();
        } catch (HyphenateException e) {
            e.printStackTrace();
        }
        isMuteState = true;
    }

    private void resumeVoice(Object args, Result result) {
        try {
            callManager.resumeVoiceTransfer();
        } catch (HyphenateException e) {
            e.printStackTrace();
        }
        isMuteState = false;
    }

    private PhoneStateManager.PhoneStateCallback phoneStateCallback = new PhoneStateManager.PhoneStateCallback() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:   // 电话响铃
                    break;
                case TelephonyManager.CALL_STATE_IDLE:      // 电话挂断
                    // resume current voice conference.
                    if (isMuteState) {
                        try {
                            callManager.resumeVoiceTransfer();
                        } catch (HyphenateException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:   // 来电接通 或者 去电，去电接通  但是没法区分
                    // pause current voice conference.
                    if (!isMuteState) {
                        try {
                            callManager.pauseVoiceTransfer();
                        } catch (HyphenateException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
    };

    //----------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private void sendMessage(Object args, Result result) {
        JSONObject argMap = (JSONObject)args;
        EMMessage message = EMHelper.convertDataMapToMessage(argMap);
        String localMsgId = message.getMsgId();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("success", Boolean.TRUE);
        message.setMessageStatusCallback(new EMCallBack() {
            @Override
            public void onSuccess() {
                post((Void)->{
                    Map<String, Object> data = new HashMap<String, Object>();
                    data.put("success", Boolean.TRUE);
                    data.put("message", EMHelper.convertEMMessageToStringMap(message));
                    EMLog.e("callback", "onSuccess");
                    result.success(data);
                });
            }

            @Override
            public void onError(int code, String error) {
                post((Void)->{
                    Map<String, Object> data = new HashMap<String, Object>();
                    data.put("success", Boolean.FALSE);
                    data.put("code", code );
                    data.put("error", error);
                    result.success(data);
                });

            }

            @Override
            public void onProgress(int progress, String status) {
                post((Void)->{
                    data.put("progress", progress );
                    data.put("status", status);
                    data.put("localMsgId",localMsgId);
                    channel.invokeMethod(EMSDKMethod.onMessageStatusOnProgress, data);
                });
            }
        });
        manager.sendMessage(message);
    }

    private void ackMessageRead(Object args, Result result) {
        try {
            JSONObject argMap = (JSONObject) args;
            String to = argMap.getString("to");
            String messageId = argMap.getString("id");
            try {
                manager.ackMessageRead(to, messageId);
                onSuccess(result);
            } catch (HyphenateException e) {
                onError(result, e);
            }
        }catch (JSONException e){
            EMLog.e("JSONException", e.getMessage());
        }
    }

    private void recallMessage(Object args, Result result) {
        JSONObject argMap = (JSONObject)args;
        EMMessage message = EMHelper.convertDataMapToMessage(argMap);
        try{
            manager.recallMessage(message);
            onSuccess(result);
        }catch(HyphenateException e) {
            onError(result, e);
        }
    }

    private void getMessage(Object args, Result result) {
        try {
            JSONObject argMap = (JSONObject)args;
            String messageId = argMap.getString("id");
            EMMessage message = manager.getMessage(messageId);
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("success", Boolean.TRUE);
            data.put("message", EMHelper.convertEMMessageToStringMap(message));
            result.success(data);
        }catch (JSONException e){
            EMLog.e("JSONException", e.getMessage());
        }
    }

    private void getConversation(Object args, Result result) {
        try {
            JSONObject argMap = (JSONObject)args;
            String conversationId = argMap.getString("id");
            EMConversation.EMConversationType type = EMHelper.convertIntToEMConversationType(argMap.getInt("type"));
            Boolean createIfNotExists = argMap.getBoolean("createIfNotExists");
            EMConversation conversation = manager.getConversation(conversationId, type, createIfNotExists);
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("success", Boolean.TRUE);
            data.put("conversation", EMHelper.convertEMConversationToStringMap(conversation));
            result.success(data);
        }catch (JSONException e){
            EMLog.e("JSONException", e.getMessage());
        }
    }

    private void markAllConversationsAsRead(Object args, Result result) {
        manager.markAllConversationsAsRead();
    }

    private void getUnreadMessageCount(Object args, Result result) {
        int count = manager.getUnreadMessageCount();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("success", Boolean.TRUE);
        data.put("count", count);
        result.success(data);
    }

    private void saveMessage(Object args, Result result) {
        JSONObject argMap = (JSONObject)args;
        EMMessage message = EMHelper.convertDataMapToMessage(argMap);
        manager.saveMessage(message);
    }

    private void updateMessage(Object args, Result result) {
        JSONObject argMap = (JSONObject)args;
        Map<String, Object> data = new HashMap<>();
        try {
            EMMessage message = EMHelper.updateDataMapToMessage(argMap.getJSONObject("message"));

            if (message != null) {
                boolean status = EMClient.getInstance().chatManager().updateMessage(message);
                data.put("status", status);
            } else {
                data.put("status", false);
            }
            data.put("success", Boolean.TRUE);
            result.success(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void downloadAttachment(Object args, Result result) {
        JSONObject argMap = (JSONObject)args;
        EMMessage message = EMHelper.convertDataMapToMessage(argMap);
        manager.downloadAttachment(message);
    }

    private void downloadThumbnail(Object args, Result result) {
        JSONObject argMap = (JSONObject)args;
        EMMessage message = EMHelper.convertDataMapToMessage(argMap);
        manager.downloadThumbnail(message);
    }

    private void importMessages(Object args, Result result) {
        try {
            JSONObject argMap = (JSONObject)args;
            EMLog.e("importMessages", argMap.toString());
            JSONArray data = argMap.getJSONArray("messages");
            List<EMMessage> messages = new LinkedList<EMMessage>();
            for(int i = 0; i < data.length(); i++){
                messages.add(EMHelper.convertDataMapToMessage(data.getJSONObject(i)));
            }
            manager.importMessages(messages);
        }catch (JSONException e){
            EMLog.e("JSONException", e.getMessage());
        }
    }

    private void getConversationsByType(Object args, Result result) {
        try {
            JSONObject argMap = (JSONObject)args;
            int type = argMap.getInt("type");
            List<EMConversation> list = manager.getConversationsByType(EMHelper.convertIntToEMConversationType(type));
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("success", Boolean.TRUE);
            ArrayList<Map<String, Object>> conversations = new ArrayList<>();
            for(EMConversation conversation : list) {
                conversations.add(EMHelper.convertEMConversationToStringMap(conversation));
            }
            data.put("conversations",conversations);
            result.success(data);
        }catch (JSONException e){
            EMLog.e("JSONException", e.getMessage());
        }
    }

    private void downloadFile(Object args, Result result) {
        try {
            JSONObject argMap = (JSONObject)args;
            String remoteUrl = argMap.getString("remoteUrl");
            String localFilePath = argMap.getString("localFilePath");
            JSONObject json_headers = argMap.getJSONObject("headers");
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", json_headers.getString("json_headers"));
            manager.downloadFile(remoteUrl,localFilePath,headers,new EMWrapperCallBack(result));
        }catch (JSONException e){
            EMLog.e("JSONException", e.getMessage());
        }
    }

    private void getAllConversations(Object args, Result result) {
        assert(args instanceof Map);
        Map<String, EMConversation> list = manager.getAllConversations();
        List<Map<String, Object>> conversations = new LinkedList<Map<String, Object>>();
//        list.forEach((String id, EMConversation conversation)->{
//            conversations.add(EMHelper.convertEMConversationToStringMap(conversation));
//        });
        for(Map.Entry<String, EMConversation> m : list.entrySet()){
            conversations.add(EMHelper.convertEMConversationToStringMap( m.getValue()));
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("success", Boolean.TRUE);
        data.put("conversations", conversations);
        result.success(data);
    }

    private void loadAllConversations(Object args, Result result) {
        manager.loadAllConversations();
    }

    private void deleteConversation(Object args, Result result) {
        try {
            JSONObject argMap = (JSONObject)args;
            String userName = argMap.getString("userName");
            Boolean deleteMessages = argMap.getBoolean("deleteMessages");
            boolean status = manager.deleteConversation(userName,deleteMessages.booleanValue());
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("success", Boolean.TRUE);
            data.put("status", status);
            result.success(data);
        }catch (JSONException e){
            EMLog.e("JSONException", e.getMessage());
        }
    }


    private void setVoiceMessageListened(Object args, Result result) {
        JSONObject argMap = (JSONObject)args;
        EMMessage message = EMHelper.convertDataMapToMessage(argMap);
        manager.setVoiceMessageListened(message);
    }

    private void updateParticipant(Object args, Result result) {
        try {
            JSONObject argMap = (JSONObject)args;
            String from = argMap.getString("from");
            String changeTo = argMap.getString("changeTo");
            manager.updateParticipant(from, changeTo);
        }catch (JSONException e){
            EMLog.e("JSONException", e.getMessage());
        }
    }


    private void fetchHistoryMessages(Object args, Result result) {
        try {
            JSONObject argMap = (JSONObject)args;
            String conversationId = argMap.getString("id");
            int type = argMap.getInt("type");
            int pageSize = argMap.getInt("pageSize");
            String startMsgId = argMap.getString("startMsgId");
            try{
                EMCursorResult<EMMessage> cursorResult = manager.fetchHistoryMessages(conversationId, EMHelper.convertIntToEMConversationType(type), pageSize, startMsgId);
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("success", Boolean.TRUE);
                String cursorId = UUID.randomUUID().toString();
                cursorResultList.put(cursorId, cursorResult);
                data.put("cursorId", cursorId);
                result.success(data);
            }catch (HyphenateException e) {
                onError(result, e);
            }
        }catch (JSONException e){
            EMLog.e("JSONException", e.getMessage());
        }
    }

    //Incomplete implementation
    private void getCursor(Object args, Result result) {
        try {
            Map<String, Object> data = new HashMap<String, Object>();
            JSONObject argMap = (JSONObject)args;
            String id = argMap.getString("id");
            EMCursorResult<EMMessage> cursor = cursorResultList.get(id);
            if (cursor != null) {
                data.put("success", Boolean.TRUE);
                data.put("cursor", cursor.getCursor());
                data.put("message", cursor.getData());
            }
        }catch (JSONException e){
            EMLog.e("JSONException", e.getMessage());
        }
    }

    private void searchMsgFromDB(Object args, Result result) {
        try {
            JSONObject argMap = (JSONObject)args;
            String keywords = argMap.getString("keywords");
            long timeStamp = Long.parseLong(argMap.getString("timeStamp"));
            int maxCount = argMap.getInt("maxCount");
            String from = argMap.getString("from");
            int direction = argMap.getInt("direction");
            List<EMMessage> list = manager.searchMsgFromDB(keywords, timeStamp, maxCount, from, EMHelper.convertIntToEMSearchDirection(direction));
            List<Map<String, Object>> messages = new LinkedList<Map<String, Object>>();
//            list.forEach((message)->{
//                messages.add(EMHelper.convertEMMessageToStringMap(message));
//            });
            for (EMMessage message : list) {
                messages.add(EMHelper.convertEMMessageToStringMap(message));
            }
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("success", Boolean.TRUE);
            data.put("messages", messages);
            result.success(data);
        }catch (JSONException e){
            EMLog.e("JSONException", e.getMessage());
        }
    }
}

