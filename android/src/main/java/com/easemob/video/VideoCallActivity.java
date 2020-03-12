package com.easemob.video;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.easemob.im_flutter_sdk.R;
import com.hyphenate.chat.EMCallStateChangeListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.EMNoActiveCallException;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.media.EMCallSurfaceView;
import com.superrtc.sdk.VideoView;

public class VideoCallActivity extends AppCompatActivity implements EMCallStateChangeListener {

    LinearLayout lReceive;
    LinearLayout lCalling;

    ImageButton handMute;
    ImageButton handOut;
    ImageButton handSwitch;
    ImageButton handRefuse;
    ImageButton handAnswer;

    EMCallSurfaceView localSurface;
    EMCallSurfaceView oppositeSurface;

    TextView handTip;

    String username = "";
    String type = "";

    boolean isMute = false;

    private AudioManager audioManager;

    boolean localView = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        // 设置全屏无状态栏
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        init();

        Intent msgIntent = getIntent();

        type = msgIntent.getStringExtra("type");
        username = msgIntent.getStringExtra("username");
        if ("call".equals(type)) {
            lReceive.setVisibility(View.GONE);
            lCalling.setVisibility(View.VISIBLE);
            handTip.setText("正在等待对方接受邀请...");
        } else {
            lReceive.setVisibility(View.VISIBLE);
            lCalling.setVisibility(View.GONE);
            handTip.setText("对方请求与您视频通话...");
        }

        setPicture();

        connectSurface();



    }

    @Override
    protected void onDestroy() {
        EMClient.getInstance().callManager().removeCallStateChangeListener(this);
        super.onDestroy();
    }

    private void init() {

        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        EMClient.getInstance().callManager().addCallStateChangeListener(this);

        lReceive = (LinearLayout) findViewById(R.id.l_receive);
        lCalling = (LinearLayout) findViewById(R.id.l_calling);

        handMute = (ImageButton) findViewById(R.id.hand_mute);
        handOut = (ImageButton) findViewById(R.id.hand_out);
        handSwitch = (ImageButton) findViewById(R.id.hand_switch);
        handAnswer = (ImageButton) findViewById(R.id.hand_answer);
        handRefuse = (ImageButton) findViewById(R.id.hand_refuse);

        localSurface = (EMCallSurfaceView) findViewById(R.id.localSurface);
        oppositeSurface = (EMCallSurfaceView) findViewById(R.id.oppositeSurface);

        oppositeSurface.setScaleMode(VideoView.EMCallViewScaleMode.EMCallViewScaleModeAspectFill);
        localSurface.setScaleMode(VideoView.EMCallViewScaleMode.EMCallViewScaleModeAspectFill);

        handTip = (TextView) findViewById(R.id.hand_tip);

//        localSurface.setOnClickListener(view -> {
//            changeSurface();
//        });

        //静音
        handMute.setOnClickListener(view -> {
            try {
                if (!isMute) {
                    EMClient.getInstance().callManager().pauseVoiceTransfer();
                    handMute.setImageResource(R.drawable.em_call_mic_off);
                    isMute = true;
                } else {
                    EMClient.getInstance().callManager().resumeVoiceTransfer();
                    handMute.setImageResource(R.drawable.em_call_mic_on);
                    isMute = false;
                }

            } catch (HyphenateException e) {
                e.printStackTrace();
            }
        });

        //挂断
        handOut.setOnClickListener(view -> {
            try {
                EMClient.getInstance().callManager().endCall();
            } catch (EMNoActiveCallException e) {
                e.printStackTrace();
            }
            finishVideo();
        });

        //切换摄像头
        handSwitch.setOnClickListener(view -> {
            EMClient.getInstance().callManager().switchCamera();
        });

        //接听
        handAnswer.setOnClickListener(view -> {
            try {
                EMClient.getInstance().callManager().answerCall();
            } catch (EMNoActiveCallException e) {
                e.printStackTrace();
            }
        });

        //拒接
        handRefuse.setOnClickListener(view -> {
            try {
                EMClient.getInstance().callManager().rejectCall();
            } catch (EMNoActiveCallException e) {
                e.printStackTrace();
            }
            finishVideo();
        });


    }

    void setPicture() {
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        System.out.println("width:" + dm.widthPixels);
        System.out.println("width:" + dm.heightPixels);
        EMClient.getInstance().callManager().getCallOptions().setVideoResolution(0,0);
    }

    /**
     * 打开扬声器
     */
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

    private void setAudioNormal() {
        try {
            if (audioManager != null) {
                audioManager.setMicrophoneMute(false);
                audioManager.setMode(AudioManager.MODE_NORMAL);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 连通视频
     */
    void connectSurface() {
        EMClient.getInstance().callManager().setSurfaceView(localSurface,oppositeSurface);
    }

    void changeSurface() {
        if (localView) {
            localView = false;
            EMClient.getInstance().callManager().setSurfaceView(oppositeSurface,localSurface);
        } else {
            localView = true;
            EMClient.getInstance().callManager().setSurfaceView(localSurface,oppositeSurface);
        }

    }

    /**
     * 结束视频通话
     */
    void finishVideo() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        },500);
    }

    void setTip(String tip) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                handTip.setText(tip);
            }
        });
    }

    @Override
    public void onCallStateChanged(CallState callState, CallError error) {
        switch (callState) {
            case CONNECTING: // 正在连接对方

                break;
            case CONNECTED: // 双方已经建立连接

                break;

            case ACCEPTED: // 电话接通成功

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (lReceive.getVisibility() == View.VISIBLE) {
                            lReceive.setVisibility(View.GONE);
                        }
                        lCalling.setVisibility(View.VISIBLE);
                        handTip.setText("");

                        openSpeakerOn();
                    }
                });

                break;
            case DISCONNECTED: // 电话断了
                if(error == CallError.ERROR_UNAVAILABLE){
                    // 对方不在线
                    setTip("对方不在线");
                    finishVideo();
                } else if (error == CallError.ERROR_BUSY) {
                    setTip("对方正忙");
                    finishVideo();
                } else if (error == CallError.REJECTED) {
                    setTip("对方拒绝接听");
                    finishVideo();
                } else {
                    setTip("通话已结束");
                    finishVideo();
                }
                setAudioNormal();
                break;
            case NETWORK_UNSTABLE: //网络不稳定
                if(error == CallError.ERROR_NO_DATA){
                    //无通话数据
                }else{

                }
                break;
            case NETWORK_NORMAL: //网络恢复正常

                break;
            case NETWORK_DISCONNECTED: //通话中对方断网会执行

                break;
            default:
                break;
        }
    }
}
