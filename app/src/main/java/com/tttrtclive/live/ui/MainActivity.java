package com.tttrtclive.live.ui;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.orangefilter.OrangeFilter;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tttrtclive.live.Helper.WEChatShare;
import com.tttrtclive.live.Helper.WindowManager;
import com.tttrtclive.live.LocalConfig;
import com.tttrtclive.live.LocalConstans;
import com.tttrtclive.live.MainApplication;
import com.tttrtclive.live.R;
import com.tttrtclive.live.bean.EnterUserInfo;
import com.tttrtclive.live.bean.JniObjs;
import com.tttrtclive.live.callback.MyTTTRtcEngineEventHandler;
import com.tttrtclive.live.callback.PhoneListener;
import com.tttrtclive.live.dialog.ExitRoomDialog;
import com.tttrtclive.live.utils.MyLog;
import com.tttrtclive.live.yybeautfysdk.EffectManager;
import com.tttrtclive.live.yybeautfysdk.utils.BeautyUtil;
import com.tttrtclive.live.yybeautfysdk.utils.CameraView;
import com.tttrtclive.live.yybeautfysdk.utils.FilterUtil;
import com.tttrtclive.live.yybeautfysdk.utils.GLTexture;
import com.wushuangtech.bean.TTTVideoFrame;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.TTTRtcEngine;
import com.wushuangtech.wstechapi.model.VideoCanvas;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import static com.wushuangtech.library.Constants.CLIENT_ROLE_ANCHOR;

public class MainActivity extends BaseActivity {

    private long mUserId;
    private long mAnchorId = -1;

    private TextView mAudioSpeedShow, mVideoSpeedShow, mFpsSpeedShow, mBeautfyUI;
    private ImageView mAudioChannel;

    private ExitRoomDialog mExitRoomDialog;
    private AlertDialog.Builder mErrorExitDialog;
    private MyLocalBroadcastReceiver mLocalBroadcast;
    private boolean mIsMute = false;
    private boolean mIsHeadset;
    private boolean mIsPhoneComing;
    private boolean mIsSpeaker, mIsBackCamera;

    private WindowManager mWindowManager;
    private TelephonyManager mTelephonyManager;
    private PhoneListener mPhoneListener;
    private int mRole = CLIENT_ROLE_ANCHOR;
    private boolean mHasLocalView = false;
    private WEChatShare mWEChatShare;
    private long mRoomID;
    private final Object obj = new Object();
    private boolean mIsReceiveSei;
    private Map<Long, Boolean> mUserMutes = new HashMap<>();
    private boolean mIsShowBeautfyUI;

    public static int mCurrentAudioRoute;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initEngine();
        initDialog();
        mStartTime = System.currentTimeMillis();
        mTelephonyManager = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        mPhoneListener = new PhoneListener(this);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        // 启用 sdk 上报所有说话者的音量大小
        mTTTEngine.enableAudioVolumeIndication(300, 3);
        // 设置 SDK 的本地视频等级或参数
        if (mRole == Constants.CLIENT_ROLE_BROADCASTER) {
            // 若角色为副播，视频质量等级设置为120P
            mTTTEngine.setVideoProfile(Constants.TTTRTC_VIDEOPROFILE_120P, false);
        } else {
            // 若角色为主播，视频质量根据登录界面的设置参数决定
            if (LocalConfig.mLocalVideoProfile != 0) {
                TTTRtcEngine.getInstance().setVideoProfile(LocalConfig.mLocalVideoProfile, false);
            } else {
                if (LocalConfig.mLocalHeight != 0 && LocalConfig.mLocalWidth != 0 &&
                        LocalConfig.mLocalBitRate != 0 && LocalConfig.mLocalFrameRate != 0) {
                    TTTRtcEngine.getInstance().setVideoProfile(LocalConfig.mLocalHeight, LocalConfig.mLocalWidth,
                            LocalConfig.mLocalFrameRate, LocalConfig.mLocalBitRate);
                } else {
                    mTTTEngine.setVideoProfile(Constants.TTTRTC_VIDEOPROFILE_360P, false);
                }
            }
        }
        MyLog.d("MainActivity onCreate ...");
    }

    @Override
    public void onBackPressed() {
        mExitRoomDialog.show();
    }

    @Override
    protected void onDestroy() {
        if (mPhoneListener != null && mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
            mPhoneListener = null;
            mTelephonyManager = null;
        }
        unregisterReceiver(mLocalBroadcast);

        mTTTEngine.muteLocalAudioStream(false);
        if (mIsBackCamera) {
            mTTTEngine.switchCamera();
        }
        super.onDestroy();
        MyLog.d("MainActivity onDestroy... ");
    }

    private void initView() {
        mAudioSpeedShow = findViewById(R.id.main_btn_audioup);
        mVideoSpeedShow = findViewById(R.id.main_btn_videoup);
        mFpsSpeedShow = findViewById(R.id.main_btn_fpsup);
        mAudioChannel = findViewById(R.id.main_btn_audio_channel);

        Intent intent = getIntent();
        mRoomID = intent.getLongExtra("ROOM_ID", 0);
        mUserId = intent.getLongExtra("USER_ID", 0);
        mRole = intent.getIntExtra("ROLE", CLIENT_ROLE_ANCHOR);
        String localChannelName = getString(R.string.ttt_prefix_channel_name) + ":" + mRoomID;
        ((TextView) findViewById(R.id.main_btn_title)).setText(localChannelName);

        findViewById(R.id.main_btn_exit).setOnClickListener((v) -> mExitRoomDialog.show());

        mAudioChannel.setOnClickListener(v -> {
            if (mRole != CLIENT_ROLE_ANCHOR) return;
            mIsMute = !mIsMute;
            if (mIsHeadset)
                mAudioChannel.setImageResource(mIsMute ? R.drawable.mainly_btn_muted_headset_selector : R.drawable.mainly_btn_headset_selector);
            else
                mAudioChannel.setImageResource(mIsMute ? R.drawable.mainly_btn_mute_speaker_selector : R.drawable.mainly_btn_speaker_selector);
            mTTTEngine.muteLocalAudioStream(mIsMute);
        });

        // 由于YY美颜提供视频采集，翻转摄像头功能不能用
        findViewById(R.id.main_btn_switch_camera).setVisibility(View.GONE);
        findViewById(R.id.main_btn_switch_camera).setOnClickListener(v -> {
            mTTTEngine.switchCamera();
            mIsBackCamera = !mIsBackCamera;
        });

        findViewById(R.id.main_button_share).setOnClickListener(v -> {
            findViewById(R.id.main_share_layout).setVisibility(View.VISIBLE);
        });

        mWEChatShare = new WEChatShare(this);
        findViewById(R.id.main_share_layout).findViewById(R.id.friend).setOnClickListener(v -> {
            if (LocalConfig.VERSION_FLAG == LocalConstans.VERSION_WHITE) {
                mWEChatShare.sendText(SendMessageToWX.Req.WXSceneSession, mRoomID,
                        "http://wushuangtech.com/live.html?flv=http://pull.wushuangtech.com/sdk/" + mRoomID + ".flv&hls=http://pull.wushuangtech.com/sdk/" + mRoomID + ".m3u8");
            } else {
                mWEChatShare.sendText(SendMessageToWX.Req.WXSceneSession, mRoomID, getWXLink());
            }
            findViewById(R.id.main_share_layout).setVisibility(View.GONE);
        });

        findViewById(R.id.friend_circle).setOnClickListener(v -> {
            if (LocalConfig.VERSION_FLAG == LocalConstans.VERSION_WHITE) {
                mWEChatShare.sendText(SendMessageToWX.Req.WXSceneTimeline, mRoomID,
                        "http://wushuangtech.com/live.html?flv=http://pull.wushuangtech.com/sdk/" + mRoomID + ".flv&hls=http://pull.wushuangtech.com/sdk/" + mRoomID + ".m3u8");
            } else {
                mWEChatShare.sendText(SendMessageToWX.Req.WXSceneTimeline, mRoomID, getWXLink());
            }

            findViewById(R.id.main_share_layout).setVisibility(View.GONE);
        });

        findViewById(R.id.shared_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                // 将文本内容放到系统剪贴板里。
                cm.setText(getWXLink());
                Toast.makeText(mContext, getString(R.string.ttt_copy_success), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.local_view_layout).setOnClickListener(v -> {
            if (findViewById(R.id.main_share_layout).getVisibility() == View.VISIBLE)
                findViewById(R.id.main_share_layout).setVisibility(View.GONE);
        });

        findViewById(R.id.friend_circle_close).setOnClickListener(v -> {
            findViewById(R.id.main_share_layout).setVisibility(View.GONE);
        });

        mBeautfyUI = findViewById(R.id.main_button_beautfy);
        mBeautfyUI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsShowBeautfyUI) {
                    mBeautfyUI.setText("美颜UI已展示");
                    mLayout.setVisibility(View.VISIBLE);
                } else {
                    mBeautfyUI.setText("美颜UI已隐藏");
                    mLayout.setVisibility(View.GONE);
                }
                mIsShowBeautfyUI = !mIsShowBeautfyUI;
            }
        });

        if (mRole == CLIENT_ROLE_ANCHOR) {
            // 打开本地预览视频，并开始推流
            String localUserName = getString(R.string.ttt_prefix_user_name) + ":" + mUserId;
            ((TextView) findViewById(R.id.main_btn_host)).setText(localUserName);
            setCameraView();
            ((ConstraintLayout) findViewById(R.id.local_view_layout)).addView(mCameraView);
        } else {
            mBeautfyUI.setVisibility(View.GONE);
        }
    }

    public void setTextViewContent(TextView textView, int resourceID, String value) {
        String string = getResources().getString(resourceID);
        String result = String.format(string, value);
        textView.setText(result);
    }

    private void initEngine() {
        mLocalBroadcast = new MyLocalBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addCategory("ttt.test.interface");
        filter.addAction("ttt.test.interface.string");
        filter.addAction(MyTTTRtcEngineEventHandler.TAG);
        registerReceiver(mLocalBroadcast, filter);
        ((MainApplication) getApplicationContext()).mMyTTTRtcEngineEventHandler.setIsSaveCallBack(false);
    }

    private void initDialog() {
        mExitRoomDialog = new ExitRoomDialog(mContext, R.style.NoBackGroundDialog);
        mExitRoomDialog.setCanceledOnTouchOutside(false);
        mExitRoomDialog.mConfirmBT.setOnClickListener(v -> {
            exitRoom();
            mExitRoomDialog.dismiss();
        });
        mExitRoomDialog.mDenyBT.setOnClickListener(v -> mExitRoomDialog.dismiss());


        //添加确定按钮
        mErrorExitDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.ttt_error_exit_dialog_title))//设置对话框标题
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ttt_confirm), (dialog, which) -> {//确定按钮的响应事件
                    exitRoom();
                });
    }

    private void initData() {
        mWindowManager = new WindowManager(this);
        if (mCurrentAudioRoute != Constants.AUDIO_ROUTE_SPEAKER) {
            mIsHeadset = true;
            mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_selector);
        }
    }

    public void exitRoom() {
        MyLog.d("exitRoom was called!... leave room");
        mTTTEngine.leaveChannel();
        setResult(1);
        finish();
    }

    public String getWXLink() {
        return "http://3ttech.cn/3tplayer.html?flv=http://pull.3ttech.cn/sdk/" + mRoomID + ".flv&hls=http://pull.3ttech.cn/sdk/" + mRoomID + ".m3u8";
    }

    /**
     * Author: wangzg <br/>
     * Time: 2017-11-21 18:08:37<br/>
     * Description: 显示因错误的回调而退出的对话框
     *
     * @param message the message 错误的原因
     */
    public void showErrorExitDialog(String message) {
        if (!TextUtils.isEmpty(message)) {
            String msg = getString(R.string.ttt_error_exit_dialog_prefix_msg) + ": " + message;
            mErrorExitDialog.setMessage(msg);//设置显示的内容
            mErrorExitDialog.show();
        }
    }


    private RelativeLayout mLayout;
    private CameraView mCameraView;
    private EffectManager mEffectManager;
    private AlertDialog mDialog;
    PointF mTouchPos;

    private int mOFContext;
    private Effect mBeautyEffect = new Effect();
    private Effect mFilterEffect = new Effect();
    private Effect mStickerEffect = new Effect();
    private BeautyUtil mBeautyUtil = new BeautyUtil();
    private FilterUtil mFilterUtil = new FilterUtil();

    private OrangeFilter.OF_FrameData mFrameData;
    private OrangeFilter.OF_Texture[] mInputs;
    private OrangeFilter.OF_Texture[] mOutputs;
    private long mStartTime = 0;

    private void setCameraView() {
        // extract assets
        final String venusModelPath = getFilesDir().getPath() + "/orangefilter/models/venus_models";
        File modelDir = new File(venusModelPath);
        if (!(modelDir.isDirectory() && modelDir.exists())) {
            new File(venusModelPath + "/face").mkdirs();
            OrangeFilter.extractAssetsDir(getAssets(), "models/venus_models/face", getFilesDir().getPath() + "/orangefilter/models/venus_models/face");
        }

        mLayout = new RelativeLayout(this);
        mCameraView = new CameraView(this);

        // for render
        mCameraView.setDrawFrameCallback(360, 640, new CameraView.DrawFrameCallback() {
            @Override
            public void onDrawFrame(byte[] data, GLTexture textureIn, GLTexture textureOut) {
                if (mOFContext == 0) {
                    mCameraView.copyTexture(textureIn, textureOut);
                    return;
                }

                boolean useCustomHarsLib = false;

                if (!useCustomHarsLib) {
                    if (mFrameData.faceFrameDataArr == null) {
                        mFrameData.faceFrameDataArr = new OrangeFilter.OF_FaceFrameData[0];
                    }
                }

                // update effects if need
                boolean beautyUpdated = updateEffect(mBeautyEffect);
                if (beautyUpdated) {
                    mBeautyUtil.setEffect(mOFContext, mBeautyEffect.effect);
                    onBeautyEffectReady();
                }
                boolean filterUpdated = updateEffect(mFilterEffect);
                if (filterUpdated) {
                    mFilterUtil.setEffect(mOFContext, mFilterEffect.effect);
                }
                updateEffect(mStickerEffect);

                mInputs[0].width = textureIn.getWidth();
                mInputs[0].height = textureIn.getHeight();
                mInputs[0].format = textureIn.getFormat();
                mInputs[0].target = textureIn.getTarget();
                mInputs[0].textureID = textureIn.getTextureId();

                mOutputs[0].width = textureOut.getWidth();
                mOutputs[0].height = textureOut.getHeight();
                mOutputs[0].format = textureOut.getFormat();
                mOutputs[0].target = textureOut.getTarget();
                mOutputs[0].textureID = textureOut.getTextureId();

                mFrameData.imageData = data;
                mFrameData.timestamp = (System.currentTimeMillis() - mStartTime) / 1000.0f;
                mFrameData.isUseCustomHarsLib = useCustomHarsLib;
                mFrameData.trackOn = false;
                mFrameData.curNode = 0;
                mFrameData.pickOn = false;
                mFrameData.pickResult = false;

                if (mBeautyEffect.effect != 0 || mFilterEffect.effect != 0 || mStickerEffect.effect != 0) {
                    OrangeFilter.prepareFrameData(mOFContext, textureIn.getWidth(), textureIn.getHeight(), mFrameData);

                    Vector<Integer> effects = new Vector<>();

                    if (mBeautyEffect.effect != 0 && mBeautyEffect.enable) {
                        effects.add(mBeautyEffect.effect);
                    }
                    if (mFilterEffect.effect != 0 && mFilterEffect.enable) {
                        effects.add(mFilterEffect.effect);
                    }
                    if (mStickerEffect.effect != 0 && mStickerEffect.enable) {
                        effects.add(mStickerEffect.effect);
                    }

                    if (effects.size() > 0) {
                        int[] effectArray = new int[effects.size()];
                        int[] resultArray = new int[effects.size()];
                        for (int i = 0; i < effects.size(); ++i) {
                            effectArray[i] = effects.get(i);
                        }
                        OrangeFilter.applyFrameBatch(mOFContext, effectArray, mInputs, mOutputs, resultArray);
                    } else {
                        mCameraView.copyTexture(textureIn, textureOut);
                    }
                } else {
                    mCameraView.copyTexture(textureIn, textureOut);
                }
            }

            @Override
            public void onOutputFrame(byte[] data, int width, int height) {
                TTTVideoFrame mTTTVideoFrame = new TTTVideoFrame();
                mTTTVideoFrame.buf = data;
                mTTTVideoFrame.format = TTTVideoFrame.FORMAT_ABGR;
                mTTTVideoFrame.stride = width;
                mTTTVideoFrame.height = height;
                mTTTEngine.pushExternalVideoFrame(mTTTVideoFrame);
            }

            @Override
            public void onInit() {
                MyLog.i("YYSDK", "onInit");

                // check license
                // replace to your serial number
                final String ofSerialNumber = getResources().getString(R.string.orangefilter_serial_number);
                final String ofLicenseName = "of_offline_license.license";
                String ofLicensePath = getFilesDir().getPath() + "/" + ofLicenseName;
                int ret = OrangeFilter.checkSerialNumber(MainActivity.this, ofSerialNumber, ofLicensePath);
                if (ret != OrangeFilter.OF_Result_Success) {
                    MyLog.i("YYSDK", "OrangeFilter license invalid");
                }

                mOFContext = OrangeFilter.createContext(venusModelPath);
                if (mOFContext == 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setCancelable(false);
                            builder.setMessage("OrangeFilter SDK初始化失败，请检查授权是否过期。");
                            builder.setPositiveButton("好的", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    dialog.dismiss();
                                }
                            });
                            mDialog = builder.show();
                        }
                    });
                }
                mFrameData = new OrangeFilter.OF_FrameData();
                mInputs = new OrangeFilter.OF_Texture[1];
                mOutputs = new OrangeFilter.OF_Texture[1];
                mInputs[0] = new OrangeFilter.OF_Texture();
                mOutputs[0] = new OrangeFilter.OF_Texture();
                mBeautyEffect.effect = 0;
                mFilterEffect.effect = 0;
                mStickerEffect.effect = 0;
            }

            @Override
            public void onRelease() {
                MyLog.i("YYSDK", "onRelease");

                if (mBeautyEffect.effect != 0) {
                    OrangeFilter.destroyEffect(mOFContext, mBeautyEffect.effect);
                    mBeautyEffect.effect = 0;
                    mBeautyEffect.currentPath = null;
                }
                if (mFilterEffect.effect != 0) {
                    OrangeFilter.destroyEffect(mOFContext, mFilterEffect.effect);
                    mFilterEffect.effect = 0;
                    mFilterEffect.currentPath = null;
                }
                if (mStickerEffect.effect != 0) {
                    OrangeFilter.destroyEffect(mOFContext, mStickerEffect.effect);
                    mStickerEffect.effect = 0;
                    mStickerEffect.currentPath = null;
                }

                if (mOFContext != 0) {
                    OrangeFilter.destroyContext(mOFContext);
                    mOFContext = 0;
                }

                mBeautyUtil.clearEffect();
                mFilterUtil.clearEffect();

                if (mDialog != null) {
                    mDialog.dismiss();
                    mDialog = null;
                }
            }
        });

        mCameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mTouchPos = new PointF();
                    mTouchPos.x = event.getX();
                    mTouchPos.y = event.getY();
                }
                return false;
            }
        });
        mCameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraView.touchFocus(new PointF(mTouchPos.x / view.getWidth(), mTouchPos.y / view.getHeight()), 400);
            }
        });

        ViewGroup vp = findViewById(R.id.root_view_ly);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        vp.addView(mLayout, params);
        mLayout.setVisibility(View.GONE);

        mLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                if (mEffectManager == null) {
                    mEffectManager = new EffectManager(MainActivity.this, mLayout, mBeautyUtil, mFilterUtil);
                    mEffectManager.setOnSetBeautyEffectListener(new EffectManager.OnSetEffectListener() {
                        @Override
                        public void onSetEffect(String path) {
                            mBeautyEffect.targetPath = path;
                        }
                    });
                    mEffectManager.setOnEnableBeautyEffectListener(new EffectManager.OnEnableEffectListener() {
                        @Override
                        public void onEnableEffect(boolean enable) {
                            mBeautyEffect.enable = enable;
                        }
                    });
                    mEffectManager.setOnSetFilterEffectListener(new EffectManager.OnSetEffectListener() {
                        @Override
                        public void onSetEffect(String path) {
                            mFilterEffect.targetPath = path;
                        }
                    });
                    mEffectManager.setOnSetStickerEffectListener(new EffectManager.OnSetEffectListener() {
                        @Override
                        public void onSetEffect(String path) {
                            mStickerEffect.targetPath = path;
                        }
                    });
                    mEffectManager.requestEffectList();
                }
            }
        });
    }

    private void onBeautyEffectReady() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mEffectManager.initUI();
            }
        });
    }

    private boolean updateEffect(Effect effect) {
        boolean updated = false;

        if (effect.targetPath != null && (effect.currentPath == null || !effect.currentPath.equals(effect.targetPath))) {
            effect.currentPath = effect.targetPath;

            if (effect.effect != 0) {
                OrangeFilter.destroyEffect(mOFContext, effect.effect);
                effect.effect = 0;
                updated = true;
            }

            if (effect.currentPath.length() > 0) {
                if (new File(effect.currentPath).exists()) {
                    effect.effect = OrangeFilter.createEffectFromPackage(mOFContext, effect.currentPath);
                    if (effect.effect != 0) {
                        updated = true;
                    }
                } else {
                    MyLog.e("YYSDK", "effect file not exist: " + effect.currentPath);
                }
            }
        }

        return updated;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCameraView != null) {
            mCameraView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mCameraView != null) {
            mCameraView.onPause();
        }
        super.onPause();
    }

    private class MyLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyTTTRtcEngineEventHandler.TAG.equals(action)) {
                JniObjs mJniObjs = intent.getParcelableExtra(MyTTTRtcEngineEventHandler.MSG_TAG);
                MyLog.d("UI onReceive callBack... mJniType : " + mJniObjs.mJniType);
                switch (mJniObjs.mJniType) {
                    case LocalConstans.CALL_BACK_ON_USER_KICK:
                        String message = "";
                        int errorType = mJniObjs.mErrorType;
                        if (errorType == Constants.ERROR_KICK_BY_HOST) {
                            message = getResources().getString(R.string.ttt_error_exit_kicked);
                        } else if (errorType == Constants.ERROR_KICK_BY_PUSHRTMPFAILED) {
                            message = getResources().getString(R.string.ttt_error_exit_push_rtmp_failed);
                        } else if (errorType == Constants.ERROR_KICK_BY_SERVEROVERLOAD) {
                            message = getResources().getString(R.string.ttt_error_exit_server_overload);
                        } else if (errorType == Constants.ERROR_KICK_BY_MASTER_EXIT) {
                            message = getResources().getString(R.string.ttt_error_exit_anchor_exited);
                        } else if (errorType == Constants.ERROR_KICK_BY_RELOGIN) {
                            message = getResources().getString(R.string.ttt_error_exit_relogin);
                        } else if (errorType == Constants.ERROR_KICK_BY_NEWCHAIRENTER) {
                            message = getResources().getString(R.string.ttt_error_exit_other_anchor_enter);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOAUDIODATA) {
                            message = getResources().getString(R.string.ttt_error_exit_noaudio_upload);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOVIDEODATA) {
                            message = getResources().getString(R.string.ttt_error_exit_novideo_upload);
                        } else if (errorType == Constants.ERROR_TOKEN_EXPIRED) {
                            message = getResources().getString(R.string.ttt_error_exit_token_expired);
                        }
                        showErrorExitDialog(message);
                        break;
                    case LocalConstans.CALL_BACK_ON_CONNECTLOST:
                        showErrorExitDialog(getString(R.string.ttt_error_network_disconnected));
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_JOIN:
                        long uid = mJniObjs.mUid;
                        int identity = mJniObjs.mIdentity;
                        if (identity == CLIENT_ROLE_ANCHOR) {
                            mAnchorId = uid;
                            String localAnchorName = getString(R.string.ttt_role_anchor) + "ID: " + mRoomID;
                            ((TextView) findViewById(R.id.main_btn_host)).setText(localAnchorName);
                        }
                        if (mRole == CLIENT_ROLE_ANCHOR) {
                            EnterUserInfo userInfo = new EnterUserInfo(uid, identity);
                            mWindowManager.addAndSendSei(mUserId, userInfo);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_OFFLINE:
                        long offLineUserID = mJniObjs.mUid;
                        mWindowManager.removeAndSendSei(mUserId, offLineUserID);
                        break;
                    case LocalConstans.CALL_BACK_ON_SEI:
                        TreeSet<EnterUserInfo> mInfos = new TreeSet<>();
                        try {
                            JSONObject jsonObject = new JSONObject(mJniObjs.mSEI);
                            JSONArray jsonArray = jsonObject.getJSONArray("pos");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonobject2 = (JSONObject) jsonArray.get(i);
                                String devid = jsonobject2.getString("id");
                                float x = Float.valueOf(jsonobject2.getString("x"));
                                float y = Float.valueOf(jsonobject2.getString("y"));
                                float w = Float.valueOf(jsonobject2.getString("w"));
                                float h = Float.valueOf(jsonobject2.getString("h"));

                                long userId;
                                int index = devid.indexOf(":");
                                if (index > 0) {
                                    userId = Long.parseLong(devid.substring(0, index));
                                } else {
                                    userId = Long.parseLong(devid);
                                }
                                MyLog.d("CALL_BACK_ON_SEI", "parse user id : " + userId);
                                if (userId != mAnchorId) {
                                    EnterUserInfo temp = new EnterUserInfo(userId, Constants.CLIENT_ROLE_BROADCASTER);
                                    temp.setXYLocation(x, y);
                                    mInfos.add(temp);
                                } else {
                                    if (!mHasLocalView) {
                                        mHasLocalView = true;
                                        SurfaceView mSurfaceView = mTTTEngine.CreateRendererView(MainActivity.this);
                                        mTTTEngine.setupRemoteVideo(new VideoCanvas(userId, Constants.RENDER_MODE_HIDDEN, mSurfaceView));
                                        ((ConstraintLayout) findViewById(R.id.local_view_layout)).addView(mSurfaceView);
                                    }
                                }

                            }
                        } catch (JSONException e) {
                            MyLog.d("CALL_BACK_ON_SEI", "parse xml error : " + e.getLocalizedMessage());
                        }

                        int count = 0;
                        for (EnterUserInfo temp : mInfos) {
                            temp.mShowIndex = count;
                            count++;
                        }

                        for (EnterUserInfo next : mInfos) {
                            MyLog.d("CALL_BACK_ON_SEI", "user list : " + next.getId() + " | index : " + next.mShowIndex);
                            mWindowManager.add(mUserId, next.getId(), getRequestedOrientation(), next.mShowIndex);
                        }

                        synchronized (obj) {
                            if (mUserMutes.size() > 0) {
                                Set<Map.Entry<Long, Boolean>> entries = mUserMutes.entrySet();
                                for (Map.Entry<Long, Boolean> next : entries) {
                                    mWindowManager.muteAudio(next.getKey(), next.getValue());
                                }
                            }
                            mUserMutes.clear();
                            mIsReceiveSei = true;
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_AUDIO_STATE:
                        if (mJniObjs.mRemoteAudioStats.getUid() != mAnchorId) {
                            String audioString = getResources().getString(R.string.ttt_audio_downspeed);
                            String audioResult = String.format(audioString, String.valueOf(mJniObjs.mRemoteAudioStats.getReceivedBitrate()));
                            mWindowManager.updateAudioBitrate(mJniObjs.mRemoteAudioStats.getUid(), audioResult);
                        } else
                            setTextViewContent(mAudioSpeedShow, R.string.ttt_audio_downspeed, String.valueOf(mJniObjs.mRemoteAudioStats.getReceivedBitrate()));
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_VIDEO_STATE:
                        if (mJniObjs.mRemoteVideoStats.getUid() != mAnchorId) {
                            String videoString = getResources().getString(R.string.ttt_video_downspeed);
                            String videoResult = String.format(videoString, String.valueOf(mJniObjs.mRemoteVideoStats.getReceivedBitrate()));
                            mWindowManager.updateVideoBitrate(mJniObjs.mRemoteVideoStats.getUid(), videoResult);
                        } else
                            setTextViewContent(mVideoSpeedShow, R.string.ttt_video_downspeed, String.valueOf(mJniObjs.mRemoteVideoStats.getReceivedBitrate()));
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_AUDIO_STATE:
                        if (mRole == CLIENT_ROLE_ANCHOR)
                            setTextViewContent(mAudioSpeedShow, R.string.ttt_audio_upspeed, String.valueOf(mJniObjs.mLocalAudioStats.getSentBitrate()));
                        else {
                            String localAudioString = getResources().getString(R.string.ttt_audio_upspeed);
                            String localAudioResult = String.format(localAudioString, String.valueOf(mJniObjs.mLocalAudioStats.getSentBitrate()));
                            mWindowManager.updateAudioBitrate(mUserId, localAudioResult);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_VIDEO_STATE:
                        if (mRole == CLIENT_ROLE_ANCHOR) {
                            mFpsSpeedShow.setText("FPS-" + mJniObjs.mLocalVideoStats.getSentFrameRate());
                            setTextViewContent(mVideoSpeedShow, R.string.ttt_video_upspeed, String.valueOf(mJniObjs.mLocalVideoStats.getSentBitrate()));
                        } else {
                            String localVideoString = getResources().getString(R.string.ttt_video_upspeed);
                            String localVideoResult = String.format(localVideoString, String.valueOf(mJniObjs.mLocalVideoStats.getSentBitrate()));
                            mWindowManager.updateVideoBitrate(mUserId, localVideoResult);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_MUTE_AUDIO:
                        long muteUid = mJniObjs.mUid;
                        boolean mIsMuteAuido = mJniObjs.mIsDisableAudio;
                        MyLog.i("OnRemoteAudioMuted CALL_BACK_ON_MUTE_AUDIO start! .... " + mJniObjs.mUid
                                + " | mIsMuteAuido : " + mIsMuteAuido);
                        if (muteUid == mAnchorId) {
//                            mIsMute = mIsMuteAuido;
//                            if (mIsHeadset)
//                                mAudioChannel.setImageResource(mIsMuteAuido ? R.drawable.mainly_btn_muted_headset_selector : R.drawable.mainly_btn_headset_selector);
//                            else
//                                mAudioChannel.setImageResource(mIsMuteAuido ? R.drawable.mainly_btn_mute_speaker_selector : R.drawable.mainly_btn_speaker_selector);
                        } else {
                            if (mRole != Constants.CLIENT_ROLE_ANCHOR) {
                                if (mIsReceiveSei) {
                                    mWindowManager.muteAudio(muteUid, mIsMuteAuido);
                                } else {
                                    mUserMutes.put(muteUid, mIsMuteAuido);
                                }
                            } else {
                                mWindowManager.muteAudio(muteUid, mIsMuteAuido);
                            }
                        }
                        break;

                    case LocalConstans.CALL_BACK_ON_AUDIO_ROUTE:
                        int mAudioRoute = mJniObjs.mAudioRoute;
                        if (mAudioRoute == Constants.AUDIO_ROUTE_SPEAKER || mAudioRoute == Constants.AUDIO_ROUTE_HEADPHONE) {
                            mIsHeadset = false;
                            mAudioChannel.setImageResource(mIsMute ? R.drawable.mainly_btn_mute_speaker_selector : R.drawable.mainly_btn_speaker_selector);
                        } else {
                            mIsHeadset = true;
                            mAudioChannel.setImageResource(mIsMute ? R.drawable.mainly_btn_muted_headset_selector : R.drawable.mainly_btn_headset_selector);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_COME:
                        mIsPhoneComing = true;
                        mIsSpeaker = mTTTEngine.isSpeakerphoneEnabled();
                        if (mIsSpeaker) {
                            mTTTEngine.setEnableSpeakerphone(false);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_IDLE:
                        if (mIsPhoneComing) {
                            if (mIsSpeaker) {
                                mTTTEngine.setEnableSpeakerphone(true);
                            }
                            mIsPhoneComing = false;
                        }
                    case LocalConstans.CALL_BACK_ON_AUDIO_VOLUME_INDICATION:
                        if (mIsMute) return;
                        int volumeLevel = mJniObjs.mAudioLevel;
                        if (mJniObjs.mUid == mUserId) {
                            if (mIsHeadset) {
                                if (volumeLevel >= 0 && volumeLevel <= 3) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_small_selector);
                                } else if (volumeLevel > 3 && volumeLevel <= 6) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_middle_selector);
                                } else if (volumeLevel > 6 && volumeLevel <= 9) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_big_selector);
                                }
                            } else {
                                if (volumeLevel >= 0 && volumeLevel <= 3) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_small_selector);
                                } else if (volumeLevel > 3 && volumeLevel <= 6) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_middle_selector);
                                } else if (volumeLevel > 6 && volumeLevel <= 9) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_big_selector);
                                }
                            }
                        } else {
                            mWindowManager.updateSpeakState(mJniObjs.mUid, mJniObjs.mAudioLevel);
                        }
                        break;
                }
            }
        }
    }

    class Effect {
        public String targetPath;
        public String currentPath;
        public int effect;
        public boolean enable = true;
    }
}
