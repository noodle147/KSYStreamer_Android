package com.ksyun.media.diversity.sticker.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ksyun.media.diversity.sticker.demo.utils.ApiHttpUrlConnection;
import com.ksyun.media.diversity.sticker.demo.utils.TriggerActionUtils;
import com.ksyun.media.diversity.sticker.demo.widget.GridViewAdapter;
import com.ksyun.media.diversity.sticker.demo.widget.HorizontalListView;
import com.ksyun.media.diversity.sticker.demo.widget.STListViewAdapter;
import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.player.KSYMediaPlayer;
import com.ksyun.media.streamer.capture.camera.CameraTouchHelper;
import com.ksyun.media.streamer.filter.audio.AudioFilterBase;
import com.ksyun.media.streamer.filter.audio.AudioReverbFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgBeautyDenoiseFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgFilterBase;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterBase;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterMgt;
import com.ksyun.media.streamer.kit.KSYStreamer;
import com.ksyun.media.streamer.kit.StreamerConstants;
import com.ksyun.media.streamer.logstats.StatsLogReport;
import com.sensetime.sensear.SenseArBroadcasterClient;
import com.sensetime.sensear.SenseArClient;
import com.sensetime.sensear.SenseArMaterial;
import com.sensetime.sensear.SenseArMaterialGroupId;
import com.sensetime.sensear.SenseArMaterialRender;
import com.sensetime.sensear.SenseArMaterialService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by qyvideo on 11/7/16.
 */

public class StickerCameraActivity  extends Activity implements
        ActivityCompat.OnRequestPermissionsResultCallback{

    private static final String TAG = "CameraActivity";

    private GLSurfaceView mCameraPreviewView;
    //private TextureView mCameraPreviewView;
    private CameraHintView mCameraHintView;
    private Chronometer mChronometer;
    private View mDeleteView;
    private View mSwitchCameraView;
    private View mFlashView;
    private TextView mShootingText;
    private TextView mChangeSticker;
    private CheckBox mWaterMarkCheckBox;
    private CheckBox mBeautyCheckBox;
    private CheckBox mReverbCheckBox;
    private CheckBox mAudioPreviewCheckBox;
    private CheckBox mBgmCheckBox;
    private CheckBox mMuteCheckBox;
    private CheckBox mAudioOnlyCheckBox;
    private CheckBox mFrontMirrorCheckBox;
    private TextView mUrlTextView;
    private TextView mDebugInfoTextView;

    private StickerCameraActivity.ButtonObserver mObserverButton;
    private StickerCameraActivity.CheckBoxObserver mCheckBoxObserver;

    private KSYStreamer mStreamer;
    private Handler mMainHandler;
    private Timer mTimer;

    private SenseArMaterialService mSenseArService;
    private SenseArBroadcasterClient mBroadcaster;
    private String mBroadcasterID = null;
    private SenseArMaterial mMaterial = null;

    private String APPID = "20e834b0177d4b1d81b6cc1a6a9d9296";  //for online sever

    private List<MaterialInfoItem> mMaterialList2 = null;
    private List<MaterialInfoItem> mMaterialList1 = null;
    private List<MaterialInfoItem> mCurrentMaterialList = null;

    private GridView mGridView = null;
    private GridViewAdapter mGridViewAdapter = null;

    private RelativeLayout mShowMaterialsLayout;

    private final static String LICENSE_NAME = "SenseME.lic";
    private final static String PREF_ACTIVATE_CODE_FILE = "activate_code_file";
    private final static String PREF_ACTIVATE_CODE = "activate_code";
    private final static int MSG_LOADTHUMB = 0;
    private final static int MSG_DOWNLOADSUCCESS = 1;
    private final static int MSG_STARTDOWNLOAD = 2;
    private final static int MSG_ENABLEPUSH = 3;
    private final static int MSG_GETLISTSIZE = 4;

    private int mListCount = 1;

    public static String BROADCASTER_ID = "ST201601";

    private Accelerometer mAccelerometer;
    private ImgStickerFilter mImgStickerFilter;

    private static int mMateriallist1SelectIndex = -1;
    private static int mMateriallist2SelectIndex = -1;
    private static int mCurrentMaterialIndex = -1;
    private static int mCurrentMaterialType = 0;

    private FrameLayout mMakemoneyIntrLayout = null;
    private static boolean mShowIntroductionLayout = true;
    private boolean mIsFirstFetchMaterialList = true;
    private HorizontalListView mMaterialTypeView = null;
    private STListViewAdapter mMaterialTypeAdapter = null;
    private FrameLayout mMaterialItemIntroLayout = null;
    private FrameLayout mMaterialsListLayout = null;
    private ImageButton mQuestionBackBtn = null;
    private RelativeLayout mMaterialDetailInfoLayout = null;
    private MaterialDetailViewHolder mMaterialDetailHolder = null;

    private boolean mAutoStart;
    private boolean mIsLandscape;
    private boolean mPrintDebugInfo = false;
    private boolean mRecording = false;
    private boolean isFlashOpened = false;
    private boolean mPause = false;
    private String mUrl;
    private String mDebugInfo = "";
    private String mBgmPath = "/sdcard/test.mp3";
    private String mLogoPath = "file:///sdcard/test.png";
    private boolean mHasAuthorized = false;

    private Context mContext;

    private Bitmap mNullBitmap = null;

    private boolean mHWEncoderUnsupported;
    private boolean mSWEncoderUnsupported;

    private final static int PERMISSION_REQUEST_CAMERA_AUDIOREC = 1;
    private static final String START_STRING = "开始直播";
    private static final String STOP_STRING = "停止直播";

    public final static String URL = "url";
    public final static String FRAME_RATE = "framerate";
    public final static String VIDEO_BITRATE = "video_bitrate";
    public final static String AUDIO_BITRATE = "audio_bitrate";
    public final static String VIDEO_RESOLUTION = "video_resolution";
    public final static String LANDSCAPE = "landscape";
    public final static String ENCDODE_METHOD = "encode_method";
    public final static String START_ATUO = "start_auto";
    public static final String SHOW_DEBUGINFO = "show_debuginfo";

    protected Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MSG_GETLISTSIZE:
                    if (msg.arg1 == mListCount) {
                        initMaterialTabTypeView();
                        initMaterialsGridView();
                    }
                case MSG_LOADTHUMB:
                    if(msg.arg1 == mListCount){
                        mGridViewAdapter.setItemState(msg.arg2, GridViewAdapter.STATE_DOWNLOADTHUMBNAIL);
                        //Log.d(TAG, "handleMessage size: "+msg.arg2);
                        updateGridView(msg.arg2);
                        mGridViewAdapter.notifyDataSetChanged();
                    }

                    break;
                case MSG_DOWNLOADSUCCESS:
                    mGridViewAdapter.setItemState(msg.arg1, GridViewAdapter.STATE_DOWNLOADED);
                    updateGridView(msg.arg1);
                    updateCoolDownView();
                    break;
                case MSG_STARTDOWNLOAD:
                    mGridViewAdapter.setItemState(msg.arg1, GridViewAdapter.STATE_DOWNLOADING);
                    updateGridView(msg.arg1);
                    break;
                case Constants.REPORT_APPDATA_MESSAGE:
                    Bundle bundle = msg.getData();
                    String jsonStr = bundle.getString(Constants.REPORT_DATA);
                    break;
                case MSG_ENABLEPUSH:
                    ImageView v = (ImageView) msg.obj;
                    v.setEnabled(true);
                    break;
                default:
                    Log.e(TAG, "Invalid message");
                    break;
            }
        }
    };

    public static void startActivity(Context context, int fromType,
                                     String rtmpUrl, int frameRate,
                                     int videoBitrate, int audioBitrate,
                                     int videoResolution, boolean isLandscape,
                                     int encodeMethod, boolean startAuto,
                                     boolean showDebugInfo) {
        Intent intent = new Intent(context, StickerCameraActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("type", fromType);
        intent.putExtra(URL, rtmpUrl);
        intent.putExtra(FRAME_RATE, frameRate);
        intent.putExtra(VIDEO_BITRATE, videoBitrate);
        intent.putExtra(AUDIO_BITRATE, audioBitrate);
        intent.putExtra(VIDEO_RESOLUTION, videoResolution);
        intent.putExtra(LANDSCAPE, isLandscape);
        intent.putExtra(ENCDODE_METHOD, encodeMethod);
        intent.putExtra(START_ATUO, startAuto);
        intent.putExtra(SHOW_DEBUGINFO, showDebugInfo);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.camera_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCameraHintView = (CameraHintView) findViewById(R.id.camera_hint);
        mCameraPreviewView = (GLSurfaceView) findViewById(R.id.camera_preview);
        //mCameraPreviewView = (TextureView) findViewById(R.id.camera_preview);
        mUrlTextView = (TextView) findViewById(R.id.url);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        mDebugInfoTextView = (TextView) findViewById(R.id.debuginfo);

        mObserverButton = new StickerCameraActivity.ButtonObserver();
        mShootingText = (TextView) findViewById(R.id.click_to_shoot);
        mShootingText.setOnClickListener(mObserverButton);
        mChangeSticker = (TextView) findViewById(R.id.switch_sticker);
        mChangeSticker.setOnClickListener(mObserverButton);
        mDeleteView = findViewById(R.id.backoff);
        mDeleteView.setOnClickListener(mObserverButton);
        mSwitchCameraView = findViewById(R.id.switch_cam);
        mSwitchCameraView.setOnClickListener(mObserverButton);
        mFlashView = findViewById(R.id.flash);
        mFlashView.setOnClickListener(mObserverButton);

        mCheckBoxObserver = new StickerCameraActivity.CheckBoxObserver();
        mBeautyCheckBox = (CheckBox) findViewById(R.id.click_to_switch_beauty);
        mBeautyCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mReverbCheckBox = (CheckBox) findViewById(R.id.click_to_select_audio_filter);
        mReverbCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mBgmCheckBox = (CheckBox) findViewById(R.id.bgm);
        mBgmCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mAudioPreviewCheckBox = (CheckBox) findViewById(R.id.ear_mirror);
        mAudioPreviewCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mMuteCheckBox = (CheckBox) findViewById(R.id.mute);
        mMuteCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mWaterMarkCheckBox = (CheckBox) findViewById(R.id.watermark);
        mWaterMarkCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mFrontMirrorCheckBox = (CheckBox) findViewById(R.id.front_camera_mirror);
        mFrontMirrorCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mAudioOnlyCheckBox = (CheckBox) findViewById(R.id.audio_only);
        mAudioOnlyCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);

        mMainHandler = new Handler();
        mShowMaterialsLayout = (RelativeLayout) findViewById(R.id.materials_show_layout);
        ImageButton closeMaterialsShowBtn = (ImageButton) findViewById(R.id.close_materialsshow);
        closeMaterialsShowBtn.setOnClickListener(mObserverButton);

        mContext = this;
        mSenseArService = SenseArMaterialService.shareInstance();
        initAccelerometer();

        mStreamer = new KSYStreamer(this);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String url = bundle.getString(URL);
            if (!TextUtils.isEmpty(url)) {
                mUrl = url;
                mUrlTextView.setText(mUrl);
                mStreamer.setUrl(url);
            }

            int frameRate = bundle.getInt(FRAME_RATE, 0);
            if (frameRate > 0) {
                mStreamer.setPreviewFps(frameRate);
                mStreamer.setTargetFps(frameRate);
            }

            int videoBitrate = bundle.getInt(VIDEO_BITRATE, 0);
            if (videoBitrate > 0) {
                mStreamer.setVideoKBitrate(videoBitrate * 3 / 4, videoBitrate, videoBitrate / 4);
            }

            int audioBitrate = bundle.getInt(AUDIO_BITRATE, 0);
            if (audioBitrate > 0) {
                mStreamer.setAudioKBitrate(audioBitrate);
            }

            int videoResolution = bundle.getInt(VIDEO_RESOLUTION, 0);
            mStreamer.setPreviewResolution(videoResolution);
            mStreamer.setTargetResolution(videoResolution);

            int encode_method = bundle.getInt(ENCDODE_METHOD);
            mStreamer.setEncodeMethod(encode_method);

            mIsLandscape = bundle.getBoolean(LANDSCAPE, false);
            mStreamer.setRotateDegrees(mIsLandscape ? 90 : 0);
            if (mIsLandscape) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }

            mAutoStart = bundle.getBoolean(START_ATUO, false);
            mPrintDebugInfo = bundle.getBoolean(SHOW_DEBUGINFO, false);
        }
        mStreamer.setDisplayPreview(mCameraPreviewView);
        mStreamer.setEnableStreamStatModule(true);
        mStreamer.enableDebugLog(true);
        mStreamer.setFrontCameraMirror(mFrontMirrorCheckBox.isChecked());
        mStreamer.setMuteAudio(mMuteCheckBox.isChecked());
        mStreamer.setEnableAudioPreview(mAudioPreviewCheckBox.isChecked());
        mStreamer.setOnInfoListener(mOnInfoListener);
        mStreamer.setOnErrorListener(mOnErrorListener);
        mStreamer.setOnLogEventListener(mOnLogEventListener);
        //mStreamer.setOnAudioRawDataListener(mOnAudioRawDataListener);
        //mStreamer.setOnPreviewFrameListener(mOnPreviewFrameListener);
        mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                ImgTexFilterMgt.KSY_FILTER_BEAUTY_DENOISE);
        mStreamer.setEnableImgBufBeauty(true);
        mStreamer.getImgTexFilterMgt().setOnErrorListener(new ImgTexFilterBase.OnErrorListener() {
            @Override
            public void onError(ImgTexFilterBase filter, int errno) {
                Toast.makeText(StickerCameraActivity.this, "当前机型不支持该滤镜",
                        Toast.LENGTH_SHORT).show();
                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_DISABLE);
            }
        });

        // touch focus and zoom support
        CameraTouchHelper cameraTouchHelper = new CameraTouchHelper();
        cameraTouchHelper.setCameraCapture(mStreamer.getCameraCapture());
        mCameraPreviewView.setOnTouchListener(cameraTouchHelper);
        // set CameraHintView to show focus rect and zoom ratio
        cameraTouchHelper.setCameraHintView(mCameraHintView);

//        mShowinfoFrameLayout = (FrameLayout) findViewById(R.id.show_info_frame);

        Button cancelBtn = (Button) findViewById(R.id.cancel_btn);
        Button starGainBtn = (Button) findViewById(R.id.start_gain_btn);
        cancelBtn.setOnClickListener(mObserverButton);
        starGainBtn.setOnClickListener(mObserverButton);

        mMaterialTypeView = (HorizontalListView) findViewById(R.id.stickertype_view);

        mMakemoneyIntrLayout = (FrameLayout) findViewById(R.id.intr_for_materials_layout);


        mBroadcasterID = BROADCASTER_ID;

        mBroadcaster = new SenseArBroadcasterClient();
        SenseArMaterialService.ConfigStatus status = mSenseArService.configureClientWithType
                (SenseArClient.Type.Broadcaster, mBroadcaster);

        //设置缓存大小
        mSenseArService.setMaterialCacheSize(getApplicationContext(), 100*1024*1024);


    }

    @Override
    public void onResume() {
        super.onResume();
        startCameraPreviewWithPermCheck();
        mPause = false;
        mStreamer.onResume();
        if (mStreamer.isRecording() && !mAudioOnlyCheckBox.isChecked()) {
            mStreamer.setAudioOnly(false);
        }
        if (mWaterMarkCheckBox.isChecked()) {
            showWaterMark();
        }
        // 开始主播
        startBroadcast();
        startAccelerometer();
        this.initMaterialsGridView();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPause = true;
        mStreamer.onPause();
        mStreamer.stopCameraPreview();
        if (mStreamer.isRecording() && !mAudioOnlyCheckBox.isChecked()) {
            mStreamer.setAudioOnly(true);
        }
        hideWaterMark();

        // 停止主播
        stopBroadCast();
        stopAccelerometer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //清空素材缓存
        mCurrentMaterialIndex = -1;
        mMateriallist1SelectIndex = -1;
        mMateriallist2SelectIndex = -1;
        mCurrentMaterialType = 0;
        SenseArMaterialService.shareInstance().clearCache(getApplicationContext());
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }
        mSenseArService.release();
        if (mTimer != null) {
            mTimer.cancel();
        }
        mStreamer.release();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                onBackoffClick();
                break;
            default:
                break;
        }
        return true;
    }

    private void startStream() {
        mStreamer.startStream();
        mShootingText.setText(STOP_STRING);
        mShootingText.postInvalidate();
        mRecording = true;
    }

    private void stopStream() {
        mStreamer.stopStream();
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.stop();
        mShootingText.setText(START_STRING);
        mShootingText.postInvalidate();
        mRecording = false;
    }

    private void beginInfoUploadTimer() {
        if (mPrintDebugInfo && mTimer == null) {
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateDebugInfo();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDebugInfoTextView.setText(mDebugInfo);
                        }
                    });
                }
            }, 100, 1000);
        }
    }

    //update debug info
    private void updateDebugInfo() {
        if (mStreamer == null) return;
        mDebugInfo = String.format(Locale.getDefault(),
                "RtmpHostIP()=%s DroppedFrameCount()=%d \n " +
                        "ConnectTime()=%d DnsParseTime()=%d \n " +
                        "UploadedKB()=%d EncodedFrames()=%d \n" +
                        "CurrentKBitrate=%d Version()=%s",
                mStreamer.getRtmpHostIP(), mStreamer.getDroppedFrameCount(),
                mStreamer.getConnectTime(), mStreamer.getDnsParseTime(),
                mStreamer.getUploadedKBytes(), mStreamer.getEncodedFrames(),
                mStreamer.getCurrentUploadKBitrate(), mStreamer.getVersion());
    }

    //show watermark in specific location
    private void showWaterMark() {
        if (!mIsLandscape) {
            mStreamer.showWaterMarkLogo(mLogoPath, 0.08f, 0.04f, 0.20f, 0, 0.8f);
            mStreamer.showWaterMarkTime(0.03f, 0.01f, 0.35f, Color.WHITE, 1.0f);
        } else {
            mStreamer.showWaterMarkLogo(mLogoPath, 0.05f, 0.09f, 0, 0.20f, 0.8f);
            mStreamer.showWaterMarkTime(0.01f, 0.03f, 0.22f, Color.WHITE, 1.0f);
        }
    }

    private void hideWaterMark() {
        mStreamer.hideWaterMarkLogo();
        mStreamer.hideWaterMarkTime();
    }

    // Example to handle camera related operation
    private void setCameraAntiBanding50Hz() {
        Camera.Parameters parameters = mStreamer.getCameraCapture().getCameraParameters();
        if (parameters != null) {
            parameters.setAntibanding(Camera.Parameters.ANTIBANDING_50HZ);
            mStreamer.getCameraCapture().setCameraParameters(parameters);
        }
    }

    private KSYStreamer.OnInfoListener mOnInfoListener = new KSYStreamer.OnInfoListener() {
        @Override
        public void onInfo(int what, int msg1, int msg2) {
            switch (what) {
                case StreamerConstants.KSY_STREAMER_CAMERA_INIT_DONE:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_INIT_DONE");
                    setCameraAntiBanding50Hz();
                    if (mAutoStart) {
                        startStream();
                    }
                    break;
                case StreamerConstants.KSY_STREAMER_OPEN_STREAM_SUCCESS:
                    Log.d(TAG, "KSY_STREAMER_OPEN_STREAM_SUCCESS");
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                    beginInfoUploadTimer();
                    break;
                case StreamerConstants.KSY_STREAMER_FRAME_SEND_SLOW:
                    Log.d(TAG, "KSY_STREAMER_FRAME_SEND_SLOW " + msg1 + "ms");
                    Toast.makeText(StickerCameraActivity.this, "Network not good!",
                            Toast.LENGTH_SHORT).show();
                    break;
                case StreamerConstants.KSY_STREAMER_EST_BW_RAISE:
                    Log.d(TAG, "BW raise to " + msg1 / 1000 + "kbps");
                    break;
                case StreamerConstants.KSY_STREAMER_EST_BW_DROP:
                    Log.d(TAG, "BW drop to " + msg1 / 1000 + "kpbs");
                    break;
                default:
                    Log.d(TAG, "OnInfo: " + what + " msg1: " + msg1 + " msg2: " + msg2);
                    break;
            }
        }
    };

    private void handleEncodeError() {
        int encodeMethod = mStreamer.getVideoEncodeMethod();
        if (encodeMethod == StreamerConstants.ENCODE_METHOD_HARDWARE) {
            mHWEncoderUnsupported = true;
            if (mSWEncoderUnsupported) {
                mStreamer.setEncodeMethod(
                        StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT);
                Log.e(TAG, "Got HW encoder error, switch to SOFTWARE_COMPAT mode");
            } else {
                mStreamer.setEncodeMethod(StreamerConstants.ENCODE_METHOD_SOFTWARE);
                Log.e(TAG, "Got HW encoder error, switch to SOFTWARE mode");
            }
        } else if (encodeMethod == StreamerConstants.ENCODE_METHOD_SOFTWARE) {
            mSWEncoderUnsupported = true;
            if (mHWEncoderUnsupported) {
                mStreamer.setEncodeMethod(
                        StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT);
                Log.e(TAG, "Got SW encoder error, switch to SOFTWARE_COMPAT mode");
            } else {
                mStreamer.setEncodeMethod(StreamerConstants.ENCODE_METHOD_HARDWARE);
                Log.e(TAG, "Got SW encoder error, switch to HARDWARE mode");
            }
        }
    }

    private KSYStreamer.OnErrorListener mOnErrorListener = new KSYStreamer.OnErrorListener() {
        @Override
        public void onError(int what, int msg1, int msg2) {
            switch (what) {
                case StreamerConstants.KSY_STREAMER_ERROR_DNS_PARSE_FAILED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_DNS_PARSE_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_CONNECT_FAILED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_CONNECT_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_PUBLISH_FAILED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_PUBLISH_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_CONNECT_BREAKED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_CONNECT_BREAKED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_AV_ASYNC:
                    Log.d(TAG, "KSY_STREAMER_ERROR_AV_ASYNC " + msg1 + "ms");
                    break;
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED:
                    Log.d(TAG, "KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED");
                    break;
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_ENCODER_ERROR_UNSUPPORTED:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_ENCODER_ERROR_UNSUPPORTED");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_ENCODER_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_ENCODER_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_START_FAILED:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_START_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_SERVER_DIED:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_SERVER_DIED");
                    break;
                default:
                    Log.d(TAG, "what=" + what + " msg1=" + msg1 + " msg2=" + msg2);
                    break;
            }
            switch (what) {
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_UNKNOWN:
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_START_FAILED:
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED:
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN:
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_SERVER_DIED:
                    mStreamer.stopCameraPreview();
                    mMainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startCameraPreviewWithPermCheck();
                        }
                    }, 5000);
                    break;
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED:
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN:
                    handleEncodeError();
                default:
                    stopStream();
                    mMainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startStream();
                        }
                    }, 3000);
                    break;
            }
        }
    };

    private StatsLogReport.OnLogEventListener mOnLogEventListener =
            new StatsLogReport.OnLogEventListener() {
                @Override
                public void onLogEvent(StringBuilder singleLogContent) {
                    Log.i(TAG, "***onLogEvent : " + singleLogContent.toString());
                }
            };

    private void onSwitchCamera() {
        mStreamer.switchCamera();
        mCameraHintView.hideAll();
    }

    private void onFlashClick() {
        if (isFlashOpened) {
            mStreamer.toggleTorch(false);
            isFlashOpened = false;
        } else {
            mStreamer.toggleTorch(true);
            isFlashOpened = true;
        }
    }

    private void onBackoffClick() {
        new AlertDialog.Builder(StickerCameraActivity.this).setCancelable(true)
                .setTitle("结束直播?")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        mChronometer.stop();
                        mRecording = false;
                        StickerCameraActivity.this.finish();
                    }
                }).show();
    }

    private void onShootClick() {
        if (mRecording) {
            stopStream();
        } else {
            startStream();
        }
    }

    private void showChooseFilter() {
        AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(this)
                .setTitle("请选择美颜滤镜")
                .setSingleChoiceItems(
                        new String[]{"BEAUTY_SOFT", "SKIN_WHITEN", "BEAUTY_ILLUSION", "DENOISE",
                                "BEAUTY_SMOOTH", "DEMOFILTER", "GROUP_FILTER"}, -1,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(mImgStickerFilter != null) {
                                    mImgStickerFilter = null;
                                }
                                if (which < 5) {
                                    mStreamer.getImgTexFilterMgt().setFilter(
                                            mStreamer.getGLRender(), which + 16);
                                } else if (which == 5) {
                                    mStreamer.getImgTexFilterMgt().setFilter(
                                            new DemoFilter(mStreamer.getGLRender()));
                                } else if (which == 6) {
                                    List<ImgTexFilter> groupFilter = new LinkedList<>();
                                    groupFilter.add(new DemoFilter2(mStreamer.getGLRender()));
                                    groupFilter.add(new DemoFilter3(mStreamer.getGLRender()));
                                    groupFilter.add(new DemoFilter4(mStreamer.getGLRender()));
                                    mStreamer.getImgTexFilterMgt().setFilter(groupFilter);
                                }
                                dialog.dismiss();
                            }
                        })
                .create();
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    boolean[] mChooseFilter = {false, false};

    private void showChooseAudioFilter() {
        AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(this)
                .setTitle("请选择音频滤镜")
                .setMultiChoiceItems(
                        new String[]{"REVERB", "DEMO",}, mChooseFilter,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                if (isChecked) {
                                    mChooseFilter[which] = true;
                                }
                            }
                        }
                ).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mChooseFilter[0] && mChooseFilter[1]) {
                            List<AudioFilterBase> filters = new LinkedList<>();
                            AudioReverbFilter reverbFilter = new AudioReverbFilter();
                            DemoAudioFilter demofilter = new DemoAudioFilter();
                            filters.add(reverbFilter);
                            filters.add(demofilter);
                            mStreamer.getAudioFilterMgt().setFilter(filters);
                        } else if (mChooseFilter[0]) {
                            AudioReverbFilter reverbFilter = new AudioReverbFilter();
                            mStreamer.getAudioFilterMgt().setFilter(reverbFilter);
                        } else if (mChooseFilter[1]) {
                            DemoAudioFilter demofilter = new DemoAudioFilter();
                            mStreamer.getAudioFilterMgt().setFilter(demofilter);
                        } else {
                            mStreamer.getAudioFilterMgt().setFilter((AudioFilterBase) null);
                        }
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void onBeautyChecked(boolean isChecked) {
        if (isChecked) {
            if (mStreamer.getVideoEncodeMethod() ==
                    StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT) {
                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_DENOISE);
                mStreamer.setEnableImgBufBeauty(true);
            } else {
                showChooseFilter();
            }
        } else {
            if(mImgStickerFilter != null) {
                mImgStickerFilter = null;
            }
            mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                    ImgTexFilterMgt.KSY_FILTER_BEAUTY_DISABLE);
            mStreamer.setEnableImgBufBeauty(false);
        }
    }

    private void onAudioFilterChecked(boolean isChecked) {
        showChooseAudioFilter();
    }

    private void onBgmChecked(boolean isChecked) {
        if (isChecked) {
            mStreamer.getAudioPlayerCapture().getMediaPlayer()
                    .setOnCompletionListener(new KSYMediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(IMediaPlayer iMediaPlayer) {
                            Log.d(TAG, "End of the currently playing music");
                        }
                    });
            mStreamer.getAudioPlayerCapture().getMediaPlayer()
                    .setOnErrorListener(new KSYMediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(IMediaPlayer iMediaPlayer, int what, int extra) {
                            Log.e(TAG, "onBgmError: " + what);
                            return false;
                        }
                    });
            mStreamer.getAudioPlayerCapture().setVolume(1.0f);
            mStreamer.getAudioPlayerCapture().setMute(false);
            mStreamer.startBgm(mBgmPath, true);
            mStreamer.setHeadsetPlugged(true);
        } else {
            mStreamer.stopBgm();
        }
    }

    private void initAccelerometer() {
        mAccelerometer = new Accelerometer(getApplicationContext());
    }

    private void startAccelerometer() {
        mAccelerometer.start();
    }

    private void stopAccelerometer() {
        mAccelerometer.stop();
    }

    private void saveSelectedIndex(int position) {
        if (mCurrentMaterialList == mMaterialList2) {
            mMateriallist1SelectIndex = position;
            mMateriallist2SelectIndex = -1;
        } else {
            mMateriallist1SelectIndex = -1;
            mMateriallist2SelectIndex = position;
        }
        mCurrentMaterialIndex = position;
    }


    private void updateGridView(int position) {
        mGridViewAdapter.updateItemView(position);
    }

    private void updateCoolDownView() {
        mGridViewAdapter.notifyDataSetChanged();
    }


    /**
     * 验证贴纸有效性的回调对象
     */
    private SenseArMaterialService.ValidateMaterialListener mValidateAdListener = new SenseArMaterialService.ValidateMaterialListener() {
        @Override
        public void onSuccess(SenseArMaterial senseArMaterial, boolean b) {
            if(mImgStickerFilter != null) {
                mImgStickerFilter.startShowSticker(senseArMaterial);
            }
        }

        @Override
        public void onFailure(SenseArMaterial senseArMaterial, int i, String s) {
            reportError("The material is invalid");
        }
    };

    /**
     * 单独下载贴纸素材的回调对象
     */
    private SenseArMaterialService.DownloadMaterialListener mDownloadListener = new SenseArMaterialService.DownloadMaterialListener() {
        /**
         * 下载成功
         * @param senseArMaterial 下载成功的素材
         */
        @Override
        public void onSuccess(SenseArMaterial senseArMaterial) {
            int position = 0;

            for (int j = 0; j < mCurrentMaterialList.size(); j++) {
                String stickerid = mCurrentMaterialList.get(j).material.id;
                if (stickerid != null && stickerid.equals(senseArMaterial.id)) {
                    position = j;
                    mCurrentMaterialList.get(j).setHasDownload(true);
                }
            }
            Log.d(TAG, "download success for position " + position);
            mHandler.sendMessage(mHandler.obtainMessage(MSG_DOWNLOADSUCCESS, position, 0));

        }

        /**
         * 下载失败
         * @param senseArMaterial 下载失败的素材
         * @param code 失败原因的错误代码
         * @param message 失败原因的解释
         */
        @Override
        public void onFailure(SenseArMaterial senseArMaterial, int code, String message) {
            mMaterial = null;

        }

        /**
         * 下载过程中的进度回调
         * @param material  正在下载素材
         * @param progress 当前下载的进度
         * @param size 已经下载素材的大小, 单位byte
         */
        @Override
        public void onProgress(SenseArMaterial material, float progress, int size) {
        }
    };


    protected String getFilePath(String fileName) {
        String path = null;
        File dataDir = getApplicationContext().getExternalFilesDir(null);
        if (dataDir != null) {
            path = dataDir.getAbsolutePath() + File.separator + fileName;
        }
        return path;
    }

    private void onAudioPreviewChecked(boolean isChecked) {
        mStreamer.setEnableAudioPreview(isChecked);
    }

    private void onMuteChecked(boolean isChecked) {
        mStreamer.setMuteAudio(isChecked);
    }

    private void onWaterMarkChecked(boolean isChecked) {
        if (isChecked)
            showWaterMark();
        else
            hideWaterMark();
    }

    private void onFrontMirrorChecked(boolean isChecked) {
        mStreamer.setFrontCameraMirror(isChecked);
    }

    private void onAudioOnlyChecked(boolean isChecked) {
        mStreamer.setAudioOnly(isChecked);
    }

    private class ButtonObserver implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.switch_cam:
                    onSwitchCamera();
                    break;
                case R.id.backoff:
                    onBackoffClick();
                    break;
                case R.id.flash:
                    onFlashClick();
                    break;
                case R.id.click_to_shoot:
                    onShootClick();
                    break;
                case R.id.switch_sticker:
                    showMaterialLists();
                    break;
                case R.id.cancel_btn:
                    mMakemoneyIntrLayout.setVisibility(View.GONE);
                    break;
                case R.id.start_gain_btn:
                    mMakemoneyIntrLayout.setVisibility(View.GONE);
                    mShowIntroductionLayout = false;
                    showMaterialLists();
                    break;
                case R.id.question:
                    if (mQuestionBackBtn.getTag().equals("1")) {
                        mMaterialsListLayout.setVisibility(View.GONE);
                        mMaterialItemIntroLayout.setVisibility(View.VISIBLE);
                        mQuestionBackBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.back));
                        mQuestionBackBtn.setTag("2");
                    } else if (mQuestionBackBtn.getTag().equals("2")) {
                        mMaterialsListLayout.setVisibility(View.VISIBLE);
                        mMaterialItemIntroLayout.setVisibility(View.GONE);
                        mQuestionBackBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.ques));
                        mQuestionBackBtn.setTag("1");
                    }
                    break;
                case R.id.close_materialsshow:
                    closeMaterialsShowLayer();
                    break;
                default:
                    break;
            }
        }
    }

    private class CheckBoxObserver implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.click_to_switch_beauty:
                    onBeautyChecked(isChecked);
                    break;
                case R.id.click_to_select_audio_filter:
                    onAudioFilterChecked(isChecked);
                    break;
                case R.id.bgm:
                    onBgmChecked(isChecked);
                    break;
                case R.id.ear_mirror:
                    onAudioPreviewChecked(isChecked);
                    break;
                case R.id.mute:
                    onMuteChecked(isChecked);
                    break;
                case R.id.watermark:
                    onWaterMarkChecked(isChecked);
                    break;
                case R.id.front_camera_mirror:
                    onFrontMirrorChecked(isChecked);
                    break;
                case R.id.audio_only:
                    onAudioOnlyChecked(isChecked);
                    break;
                default:
                    break;
            }
        }
    }

    private void startCameraPreviewWithPermCheck() {
        int cameraPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int audioPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (cameraPerm != PackageManager.PERMISSION_GRANTED ||
                audioPerm != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Log.e(TAG, "No CAMERA or AudioRecord permission, please check");
                Toast.makeText(this, "No CAMERA or AudioRecord permission, please check",
                        Toast.LENGTH_LONG).show();
            } else {
                String[] permissions = {Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permissions,
                        PERMISSION_REQUEST_CAMERA_AUDIOREC);
            }
        } else {
            mStreamer.startCameraPreview();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA_AUDIOREC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mStreamer.startCameraPreview();
                } else {
                    Log.e(TAG, "No CAMERA or AudioRecord permission");
                    Toast.makeText(this, "No CAMERA or AudioRecord permission",
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    /**
     * 检查APP_ID和APP_KEY合法性
     * @return true, 成功 false,失败
     */
    private boolean authorized(final boolean isFinished) {

        if (!isNetworkConnectionAvailable()) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Network unavailabel.", Toast.LENGTH_LONG).show();
                }
            });
            return false;
        }
        boolean authorized = SenseArMaterialService.shareInstance().authorizeWithAppId(Constants.APPID, Constants.KEY);

        if (!authorized) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "APPID: " + Constants.APPID + " key: " + Constants.KEY);
                    Toast.makeText(getApplicationContext(), "Application authorized failed", Toast.LENGTH_LONG).show();
                    if (isFinished) {
                        finish();
                    }
                }
            });

        }
        return authorized;
    }

    private boolean isNetworkConnectionAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) return false;
        NetworkInfo.State network = info.getState();
        return (network == NetworkInfo.State.CONNECTED || network == NetworkInfo.State.CONNECTING);
    }

    /**
     * 停止主播
     */
    protected void stopBroadCast() {
        mBroadcaster.broadcastEnd();
    }

    /**
     * 开始主播
     */
    protected void startBroadcast() {
        Log.d(TAG, "startBroadcast Enter");
        initBroadcasterInfo();
        mBroadcaster.broadcastStart();
    }

    /**
     * 填充主播属性信息,为了保证更好的得到主播的爱好,请务必
     * 真实有效的填写信息
     */
    private void initBroadcasterInfo() {
        if (mBroadcaster != null) {
            mBroadcaster.id = mBroadcasterID;
            mBroadcaster.birthday = "19920320";
            mBroadcaster.name = "ST_Broadcaster_" + mBroadcasterID;
            mBroadcaster.gender = "女";
            mBroadcaster.area = "北京市/海淀区";
            mBroadcaster.followCount = 12000;
            mBroadcaster.fansCount = 12000;
            mBroadcaster.audienceCount = 14000;
            mBroadcaster.currentFansCount = 2000;
            mBroadcaster.type = "娱乐";
            mBroadcaster.telephone = "13600234000";
            mBroadcaster.email = "broadcasterAndroid@126.com";
            mBroadcaster.latitude = 39.977813f;
            mBroadcaster.longitude = 116.317188f;
            mBroadcaster.postcode = "067306";
        }
    }

    /**
     * 获取贴纸列表, 从AR服务器获取到当前热门/或者符合主播属性的贴纸列表
     */
    protected void startGetMaterialList() {
        if (mMaterialList1 != null && mMaterialList2 != null &&
                mMaterialList1.size() > 1 && mMaterialList2.size() > 1) {
            return;
        }
        mMaterialList2 = new ArrayList<MaterialInfoItem>();
        mMaterialList1 = new ArrayList<MaterialInfoItem>();

        if (mNullBitmap == null) {
            mNullBitmap = getNullEffectBitmap();
        }
        MaterialInfoItem nullSticker = new MaterialInfoItem(new SenseArMaterial(), mNullBitmap);
        nullSticker.setHasDownload(true);
        mMaterialList2.add(nullSticker);
        mMaterialList1.add(nullSticker);

        int retrytimes = 2;

        mSenseArService.fetchAllGroups(new SenseArMaterialService.FetchGroupslListener() {
            @Override
            public void onSuccess(final List<SenseArMaterialGroupId> list) {
                StickerCameraActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mListCount = 1;
                        fetchMaterial("AD_LIST", mMaterialList1, mListCount);
                        mListCount = 2;
                        fetchMaterial("SE_LIST", mMaterialList2, mListCount);
                    }
                });

            }

            @Override
            public void onFailure(int i, String s) {
                mSenseArService.fetchAllGroups(new SenseArMaterialService.FetchGroupslListener() {
                    @Override
                    public void onSuccess(final List<SenseArMaterialGroupId> list) {
                        StickerCameraActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mListCount = 1;
                                fetchMaterial("AD_LIST", mMaterialList1, mListCount);
                                mListCount = 2;
                                fetchMaterial("SE_LIST", mMaterialList2, mListCount);
                            }
                        });

                    }

                    @Override
                    public void onFailure(int i, String s) {
                        reportError("fetchGroups failed");
                    }
                });
            }
        });


    }

    private Bitmap getNullEffectBitmap() {

        AssetManager am = getResources().getAssets();
        try {
            InputStream is = am.open("null_effect.png");
            mNullBitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            mNullBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.null_effect);
        }
        return mNullBitmap;
    }

    private void initMaterialTabTypeView() {
        mMaterialTypeAdapter = new STListViewAdapter(this);
        mMaterialTypeView.setAdapter(mMaterialTypeAdapter);
        mMaterialTypeAdapter.setSelectIndex(mCurrentMaterialType);
        if (mCurrentMaterialType == 0) {
            mCurrentMaterialList = mMaterialList2;
        } else {
            mCurrentMaterialList = mMaterialList1;
        }
        mMaterialTypeAdapter.notifyDataSetChanged();
        mMaterialTypeView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == mCurrentMaterialType) {
                    return;
                }
                mMaterialTypeAdapter.setSelectIndex(position);
                mMaterialTypeAdapter.notifyDataSetChanged();

                //boolean isSelectedMaterial = (mCurrentMaterialIndex != -1);
                mCurrentMaterialType = position;
                if (position == 0) {
                    boolean isSelectedMaterial = (mMateriallist2SelectIndex != -1);
                    mCurrentMaterialList = mMaterialList2;
//                    if (isSelectedMaterial) {
//                        mMateriallist1SelectIndex = -1;
//                    }
                    mCurrentMaterialIndex = mMateriallist1SelectIndex;
                    mQuestionBackBtn.setVisibility(View.INVISIBLE);
                } else {
                    boolean isSelectedMaterial = (mMateriallist1SelectIndex != -1);
                    mCurrentMaterialList = mMaterialList1;
//                    if (isSelectedMaterial) {
//                        mMateriallist2SelectIndex = -1;
//                    }
                    mCurrentMaterialIndex = mMateriallist2SelectIndex;
                    mQuestionBackBtn.setVisibility(View.VISIBLE);
                }

                initMaterialsGridView();
                if(position == 0){
                    mGridViewAdapter.setSelectIndex(mMateriallist1SelectIndex);
                }else {
                    mGridViewAdapter.setSelectIndex(mMateriallist2SelectIndex);
                }
            }
        });
    }

    private void initMaterialsGridView() {

        if (mCurrentMaterialList == null) {
            Log.e(TAG, "The ads list is null");
            return;
        }

//        mMaterialTypeView = (HorizontalListView) findViewById(R.id.stickertype_view);

        mMaterialItemIntroLayout = (FrameLayout) findViewById(R.id.materials_introduction);
        mMaterialsListLayout = (FrameLayout) findViewById(R.id.materialslist_framelayout);
        mGridView = (GridView) findViewById(R.id.grid_view);
        mQuestionBackBtn = (ImageButton) findViewById(R.id.question);
        mQuestionBackBtn.setOnClickListener(mObserverButton);
        mGridViewAdapter = new GridViewAdapter(
                getApplicationContext(), mCurrentMaterialList, mCurrentMaterialList == mMaterialList1);
        mGridViewAdapter.setGridView(mGridView);
        mGridView.setAdapter(mGridViewAdapter);
        //mGridViewAdapter.setSelectIndex(mCurrentMaterialIndex);
        //mCameraDisplay.startShowSticker(mMaterial);
        if(mCurrentMaterialType == 0){
            mGridViewAdapter.setSelectIndex(mMateriallist1SelectIndex);
        }else{
            mGridViewAdapter.setSelectIndex(mMateriallist2SelectIndex);
        }


        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                MaterialInfoItem adinfo = mCurrentMaterialList.get(position);

                if (position == 0) {
                    mMaterial = null;
                    mGridViewAdapter.setSelectIndex(position);
                    saveSelectedIndex(position);
                    mGridViewAdapter.notifyDataSetChanged();
                    if (mImgStickerFilter != null) {
                        mImgStickerFilter.startShowSticker(null);
                    }
                    closeMaterialsShowLayer();
                    return;
                }

                if (mGridViewAdapter.isCoolDowning(position)) {
                    return;
                } else {
                    mGridViewAdapter.triggerCoolDown(position);
                }

                mMaterial = adinfo.material;

                if (mSenseArService.isMaterialDownloaded(getApplicationContext(), adinfo.material)) {
                    if (!setMaterialDetaiInfo(adinfo.material, adinfo.thumbnail, position)) {
                        closeMaterialsShowLayer();
                        //mSenseArService.validateMaterial(mMaterial, mValidateAdListener);
                        if(mImgStickerFilter == null) {
                            mImgStickerFilter = new ImgStickerFilter(mStreamer.getGLRender());
                            mImgStickerFilter.startShowSticker(mMaterial);
                            List<ImgFilterBase> groupFilter = new LinkedList<>();
                            //you can choose the beauty filter here
                            groupFilter.add(new ImgBeautyDenoiseFilter(mStreamer.getGLRender()));
                            //add sticker filter
                            groupFilter.add(mImgStickerFilter);
                            mStreamer.getImgTexFilterMgt().setFilter(groupFilter);
                        } else {
                            mImgStickerFilter.startShowSticker(mMaterial);
                        }

                    }

                    if(mGridViewAdapter.getItemState(position) != MSG_DOWNLOADSUCCESS){
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_DOWNLOADSUCCESS, position, 0));
                    }

                    saveSelectedIndex(position);
                    mGridViewAdapter.setSelectIndex(position);
                } else {
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_STARTDOWNLOAD, position, 0));
                    mSenseArService.downloadMaterial(getApplicationContext(), adinfo.material, mDownloadListener);
                }
                return;
            }
        });
    }

    protected void reportError(final String info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(StickerCameraActivity.this, info, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchMaterial(String groupID, final List<MaterialInfoItem> materialList, final int count) {
        // 从AR服务器获取贴纸列表, 并保存其信息
        mSenseArService.fetchMaterialsFromGroupId(mBroadcasterID, groupID, new SenseArMaterialService.FetchMaterialListener() {
            @Override
            public void onSuccess(List<SenseArMaterial> list) {
                List<SenseArMaterial> adlist = list;
                for (int i = 0; i < adlist.size(); i++) {
                    SenseArMaterial material = adlist.get(i);
                    String thumbnailurlStr = material.thumbnail;
                    Bitmap thumbnail = null;
                    MaterialInfoItem adinfo = new MaterialInfoItem(material, thumbnail);

                    //adinfo.commissionType = material.pricingType;
                    materialList.add(adinfo);

                    Message msg = mHandler.obtainMessage(MSG_GETLISTSIZE);
                    msg.arg1 = count;
                    mHandler.sendMessage(msg);
                }
                for (int i = 0; i < adlist.size(); i++) {
                    SenseArMaterial material = adlist.get(i);
                    String thumbnailurlStr = material.thumbnail;
                    Bitmap thumbnail = null;
                    try {
                        thumbnail = ApiHttpUrlConnection.getImageBitmap(thumbnailurlStr);
                    } catch (Exception e) {
                        thumbnail = BitmapFactory.decodeResource(getResources(), R.drawable.love);
                        reportError("get material thumbnail failed");
                    }
                    MaterialInfoItem adinfo = new MaterialInfoItem(material, thumbnail);
                    if (mSenseArService.isMaterialDownloaded(StickerCameraActivity.this, material)) {
                        adinfo.setHasDownload(true);
                    }
                    adinfo.commissionType = material.pricingType;
                    materialList.set(i+1,adinfo);

                    Message msg = mHandler.obtainMessage(MSG_LOADTHUMB);
                    msg.arg1 = count;
                    msg.arg2 = i+1;
                    mHandler.sendMessage(msg);
                }
            }

            @Override
            public void onFailure(int i, String s) {
                reportError("fetchAds failed");
            }
        });
    }

    private SenseArMaterialRender.SetMaterialCallback mSetMaterialCallback = new SenseArMaterialRender.SetMaterialCallback() {
        @Override
        public void callback(SenseArMaterialRender.RenderStatus ret) {
            if (ret == SenseArMaterialRender.RenderStatus.RENDER_UNSUPPORTED_MATERIAL) {
                StickerCameraActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Dialog alertDialog = new AlertDialog.Builder(StickerCameraActivity.this).
                                setTitle("警告").
                                setMessage("素材不被当前版本支持,需要升级SDK.").
                                setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // TODO Auto-generated method stub
                                    }
                                }).show();
                        //Toast.makeText(StreamingBaseActivity.this, "", Toast.LENGTH_LONG).show();
                    }
                });
            }
            else if (ret == SenseArMaterialRender.RenderStatus.RENDER_MATERIAL_NOT_EXIST) {
                StickerCameraActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(StickerCameraActivity.this, "素材文件不存在", Toast.LENGTH_LONG).show();
                    }
                });
            }
            else if (ret == SenseArMaterialRender.RenderStatus.RENDER_UNKNOWN) {
                StickerCameraActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(StickerCameraActivity.this, "设置素材未知错误", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    };


    private void initMaterialDetailInfoLayout() {
        mMaterialDetailInfoLayout = (RelativeLayout) findViewById(R.id.material_detailinfo_layout);
        ImageButton disableWareBtn = (ImageButton) findViewById(R.id.disable_wear);
//        disableWareBtn.setOnClickListener(this);
        mMaterialDetailHolder = new MaterialDetailViewHolder();
        mMaterialDetailHolder.mMaterialTitleView = (TextView) findViewById(R.id.material_title);
        mMaterialDetailHolder.mMaterialTriggerInd = (TextView) findViewById(R.id.material_trigger_ind);
        mMaterialDetailHolder.mMaterialTriggerIcon = (ImageView) findViewById(R.id.material_trigger_icon);
        mMaterialDetailHolder.mMaterialCostTypeIcon = (ImageView) findViewById(R.id.material_detail_type);
        mMaterialDetailHolder.mMaterialCostText = (TextView) findViewById(R.id.material_detail_cost);
        mMaterialDetailHolder.mMaterialDetail = (TextView) findViewById(R.id.material_detail);
        mMaterialDetailHolder.mMaterialThumb = (ImageView) findViewById(R.id.material_thumb);
        mMaterialDetailHolder.mEnableWareBtn = (Button) findViewById(R.id.enable_sticker_btn);
    }

    private boolean setMaterialDetaiInfo(final SenseArMaterial material, Bitmap thumbnail, final int position) {
        if (mCurrentMaterialList != mMaterialList1) {
            showTriggerActionTip(material);
            return false;
        }
        mMaterialDetailInfoLayout.setVisibility(View.VISIBLE);
        mMaterialDetailInfoLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        mMaterialDetailHolder.mMaterialTitleView.setText(material.name);
        mMaterialDetailHolder.mMaterialTriggerInd.setText(material.triggerActionTip);
        int resID = TriggerActionUtils.getTriggerImageById(material.triggerActionId);
        if (resID == -1) {
            mMaterialDetailHolder.mMaterialTriggerIcon.setVisibility(View.GONE);
        } else {
            mMaterialDetailHolder.mMaterialTriggerIcon.setVisibility(View.VISIBLE);
            mMaterialDetailHolder.mMaterialTriggerIcon.setImageResource(resID);
        }

        String costText = null;
        if (material.type == 1) { //cpc
            mMaterialDetailHolder.mMaterialCostTypeIcon.setImageResource(R.drawable.hand);
            costText = getResources().getString(R.string.currency_cpc_ind) + material.price + getResources().getString(R.string.price_unit);
        } else if (material.type == 2) {
            mMaterialDetailHolder.mMaterialCostTypeIcon.setImageResource(R.drawable.eye);
            costText = getResources().getString(R.string.currency_cpm_ind) + material.price + getResources().getString(R.string.price_unit);
        }
        mMaterialDetailHolder.mMaterialCostText.setText(costText);
        mMaterialDetailHolder.mMaterialDetail.setText(material.materialInstructions);

        mMaterialDetailHolder.mMaterialThumb.setImageBitmap(thumbnail);
        mMaterialDetailHolder.mEnableWareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSenseArService.validateMaterial(mMaterial, mValidateAdListener);
                mMaterialDetailInfoLayout.setVisibility(View.GONE);
                mCurrentMaterialIndex = position;
                showTriggerActionTip(material);
                mGridViewAdapter.notifyDataSetChanged();
                closeMaterialsShowLayer();
            }
        });
        return true;
    }

    private void showTriggerActionTip(SenseArMaterial material){
        Toast toast = new Toast(getApplicationContext());
        LayoutInflater inflate = (LayoutInflater)
                getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View triggerIndView = inflate.inflate(R.layout.trigger_toast, null);
        TextView tv = (TextView) triggerIndView.findViewById(R.id.trigger_ind_text);
        ImageView iv = (ImageView) triggerIndView.findViewById(R.id.trigger_ind_icon);
        int resID = TriggerActionUtils.getTriggerImageById(material.triggerActionId);
        if (resID == -1) {
            iv.setVisibility(View.GONE);
        } else {
            iv.setVisibility(View.VISIBLE);
            iv.setImageResource(resID);
        }
        tv.setText(material.triggerActionTip);
        toast.setView(triggerIndView);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void showMaterialLists() {
        if (mShowIntroductionLayout) {
            mMakemoneyIntrLayout.setVisibility(View.VISIBLE);
        } else {
            mShowMaterialsLayout.setVisibility(View.VISIBLE);
            if(mIsFirstFetchMaterialList){
                startGetMaterialList();
                mIsFirstFetchMaterialList = false;
            }

        }
        if(mQuestionBackBtn != null) {
            mQuestionBackBtn.setVisibility(View.INVISIBLE);
        }
    }

    private void closeMaterialsShowLayer() {
        if(mMaterialsListLayout != null) {
            mMaterialsListLayout.setVisibility(View.VISIBLE);
        }
        if(mMaterialItemIntroLayout != null) {
            mMaterialItemIntroLayout.setVisibility(View.GONE);
        }
        if(mQuestionBackBtn != null) {
            mQuestionBackBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.ques));
            mQuestionBackBtn.setTag("1");
        }
        if(mShowMaterialsLayout != null) {
            mShowMaterialsLayout.setVisibility(View.GONE);
        }
        //mMaterialList2.clear();
        //mMaterialList1.clear();
        if (mGridViewAdapter != null) {
            mGridViewAdapter.notifyDataSetChanged();
        }
    }


}
