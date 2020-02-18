package com.easemob.google;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.HashMap;

//import cn.jpush.android.api.JPushInterface;
//import cn.jpush.android.data.JPushLocalNotification;

import static com.easemob.im_flutter_sdk.EMChatManagerWrapper.registrar;

public class EMFCMMSGService extends FirebaseMessagingService {
    private static final String TAG = "EMFCMMSGService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() > 0) {
            String message = remoteMessage.getData().get("alert");
            Log.i(TAG, "onMessageReceived: " + message);
//            try {
//
//                if (remoteMessage.getNotification() != null) {
//                    JPushLocalNotification ln = new JPushLocalNotification();
////            long buildId = (long)map.get("buildId");
//                    ln.setBuilderId(1);
//                    long id = 100;
//                    ln.setNotificationId(id);
//                    ln.setTitle("陪伴社交");
//                    ln.setContent(remoteMessage.getNotification().getBody());
//
//                    long date = remoteMessage.getSentTime();
//                    ln.setBroadcastTime(date);
//
//                    JPushInterface.addLocalNotification(registrar.context(), ln);
//                }
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
    }
}
