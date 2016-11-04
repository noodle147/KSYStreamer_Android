package com.ksyun.media.diversity.screenstreamer.demo;

import com.ksyun.media.diversity.screenstreamer.kit.KSYCameraPreview;
import com.ksyun.media.diversity.screenstreamer.kit.KSYScreenStreamer;
import com.ksyun.media.streamer.filter.audio.AudioFilterBase;
import com.ksyun.media.streamer.filter.audio.AudioReverbFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterMgt;
import com.ksyun.media.streamer.kit.StreamerConstants;
import com.ksyun.media.streamer.logstats.StatsLogReport;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 录屏示例窗口
 * 提供悬浮摄像头预览窗口示例
 * 1 只在Android5.0 以上系统支持
 * 2 支持ENCODE_METHOD_SOFTWARE和ENCODE_METHOD_HARDWARE,不支持ENCODE_METHOD_SOFTWARE_COMPAT格式
 * 3 可动态添加/删除水印
 * 4 可动态添加/删除摄像头预览
 * 5 支持静音
 * 6 支持音频滤镜
 * 7 支持摄像头预览美颜
 * 8 不可设置Screen采集帧率
 */

public class ScreenActivity extends Activity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = ScreenActivity.class.getSimpleName();

    private final static int PERMISSION_REQUEST_RECORD_AUDIO = 2;  //推流录音权限
    private final static int PERMISSION_REQUEST_CAMERA = 1;  //悬浮窗口摄像头权限

    private int mCameraPreviewerPermFlag = 0x0;  //摄像头预览悬浮窗口权限标记
    private final static int CAMERA_PREVIEW_PERMISSION_CAMERA = 0x01;  //摄像头权限
    private final static int CAMERA_PREVIEW_PERMISSION_OVERLAY = 0x02;  //android6.0以上悬浮窗口权限
    private final static int PREVIEW_PERMISSION_ALL_GRANTED = 0x03;
    private final static int OVERLAY_PERMISSION_RESULT_CODE = 10;

    //图片水印示例路径(图片在sd卡中) or "assets://test.png"(图片在assert目录下面)
    private String mLogoPath = "file:///sdcard/test.png";

    //params from DemoActivity
    public final static String URL = "url";
    public final static String FRAME_RATE = "framerate";
    public final static String VIDEO_BITRATE = "video_bitrate";
    public final static String AUDIO_BITRATE = "audio_bitrate";
    public final static String VIDEO_RESOLUTION = "video_resolution";
    public final static String LANDSCAPE = "landscape";
    public final static String ENCDODE_METHOD = "encode_method";
    public final static String START_ATUO = "start_auto";
    public static final String SHOW_DEBUGINFO = "show_debuginfo";


    //button
    private TextView mUrlTextView;
    private TextView mDebugInfoTextView;
    private Chronometer mChronometer;

    private CheckBox mMuteCheckBox;
    private CheckBox mWaterMarkCheckBox;
    private CheckBox mReverbCheckBox;
    private CheckBox mCameraPreviewWindowCheckBox;
    private CheckBox mBeautyCameraPreviewCheckBox;//切换悬浮摄像头预览窗口的滤镜示例按钮,未开启悬浮窗口时该按钮无效
    private View mDeleteView;
    private Button mShootingText;

    //ButtonListener
    private ScreenActivity.ButtonObserver mObserverButton;  //所有按键响应
    private ScreenActivity.CheckBoxObserver mCheckBoxObserver; //所有checkbox响应

    private String mUrl;
    private String mDebugInfo = "";

    private KSYScreenStreamer mScreenStreamer;       //kit类,封装录屏推流相关接口
    private KSYCameraPreview mCameraPreviewKit; //如果不开启Camera悬浮窗,不需要这个实例
    private Timer mTimer;  //推流时间计时
    private Handler mMainHandler;

    //status
    private boolean mRecording = false;
    private boolean mPreviewWindowShow = false;  //悬浮摄像头预览window是否正在显示

    //user params
    private boolean mIsLandspace;
    private boolean mAutoStart;
    private boolean mPrintDebugInfo = false;

    //preview window just demo
    private FloatView mFloatLayout;   //悬浮窗口的layout, 可以是xml,也可以在代码中创建
    private WindowManager.LayoutParams mWmParams;  //layout的布局
    private WindowManager mWindowManager;

    //camera preview  params
    private GLSurfaceView mCameraPreview;  //悬浮窗口上的GLSurfaceView,用于显示摄像头预览
    private ImageView mSwitchCameraView;  //切换悬浮窗口的摄像头
    //当悬浮窗口预览窗口的摄像头角度和top Acitvity不一致时,可手动调用这个按钮调整
    private ImageView mSwitchCameraRotate;

    private int mPreviewFps;   //摄像头预览的采集帧率
    private int mPreviewResolution;  //摄像头预览的分辨率
    //做悬浮窗口rotate的监控(1s Ticker),使悬浮窗口的rotate和top acitvity的一直
    //暂时没有找到动态得到top acitvity 的横竖屏变更的接口
    private Timer mPreviewRotateTimer;
    private int mLastRotate; //屏幕旋转角度

    public static void startActivity(Context context,
                                     String rtmpUrl, int targetFrameRate,
                                     int videoBitrate, int audioBitrate,
                                     int videoResolution, boolean isLandscape,
                                     int encodeMethod, boolean startAuto,
                                     boolean showDebugInfo) {
        Intent intent = new Intent(context, ScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(URL, rtmpUrl);
        intent.putExtra(VIDEO_BITRATE, videoBitrate);
        intent.putExtra(AUDIO_BITRATE, audioBitrate);
        intent.putExtra(FRAME_RATE, targetFrameRate);
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

        setContentView(R.layout.screen_activity);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mUrlTextView = (TextView) findViewById(R.id.url);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        mDebugInfoTextView = (TextView) findViewById(R.id.debuginfo);

        mObserverButton = new ScreenActivity.ButtonObserver();
        mShootingText = (Button) findViewById(R.id.click_to_shoot);
        mShootingText.setOnClickListener(mObserverButton);
        mDeleteView = findViewById(R.id.backoff);
        mDeleteView.setOnClickListener(mObserverButton);

        mCheckBoxObserver = new ScreenActivity.CheckBoxObserver();
        mMuteCheckBox = (CheckBox) findViewById(R.id.mute);
        mMuteCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mWaterMarkCheckBox = (CheckBox) findViewById(R.id.watermark);
        mWaterMarkCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mReverbCheckBox = (CheckBox) findViewById(R.id.click_to_select_audio_filter);
        mReverbCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mCameraPreviewWindowCheckBox = (CheckBox) findViewById(R.id.screenCameraWindow);
        mCameraPreviewWindowCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mBeautyCameraPreviewCheckBox = (CheckBox) findViewById(R.id.beautyCameraWindow);
        mBeautyCameraPreviewCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);

        //无悬浮窗口时,按钮响应无效
        if (mBeautyCameraPreviewCheckBox.isChecked()) {
            mBeautyCameraPreviewCheckBox.setEnabled(true);
        } else {
            mBeautyCameraPreviewCheckBox.setEnabled(false);
        }

        mMainHandler = new Handler();
        mScreenStreamer = new KSYScreenStreamer(this);

        //set params
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            //推流地址  must set
            String url = bundle.getString(URL);
            if (!TextUtils.isEmpty(url)) {
                mUrl = url;
                mUrlTextView.setText(mUrl);
                mScreenStreamer.setUrl(url);
            }

            //推流帧率,若设置了无效值(<=0),该接口会抛出IllegalArgumentException异常
            int frameRate = bundle.getInt(FRAME_RATE, 0);
            if (frameRate > 0) {
                mScreenStreamer.setTargetFps(frameRate);
            }

            //推流视频码率 must > 0 default value 600 * 1000 bps
            //若设置了无效值(<=0),该接口会抛出IllegalArgumentException异常
            int videoBitrate = bundle.getInt(VIDEO_BITRATE, 0);
            if (videoBitrate > 0) {
                mScreenStreamer.setVideoKBitrate(videoBitrate * 3 / 4, videoBitrate, videoBitrate / 4);
            }
            //推流音频码率 must >0 default value 48 * 1000 bps
            //若设置了无效值(<=0),该接口会抛出IllegalArgumentException异常
            int audioBitrate = bundle.getInt(AUDIO_BITRATE, 0);
            if (audioBitrate > 0) {
                mScreenStreamer.setAudioKBitrate(audioBitrate);
            }
            //推流分辨率 must be <0,1,2,3>default VIDEO_RESOLUTION_360P(0)
            //若设置了无效值(<=0),该接口会抛出IllegalArgumentException异常
            int videoResolution = bundle.getInt(VIDEO_RESOLUTION, 0);
            mScreenStreamer.setTargetResolution(videoResolution);

            //编码方式  must be <2,3> default value METHOD_SOFTWARE(3)
            //若设置了无效值(ENCODE_METHOD_SOFTWARE_COMPAT)@throws IllegalArgumentException
            int encode_method = bundle.getInt(ENCDODE_METHOD);
            mScreenStreamer.setEncodeMethod(encode_method);

            //推流的横竖屏设置,默认竖屏
            mIsLandspace = bundle.getBoolean(LANDSCAPE, false);
            mScreenStreamer.setIsLandspace(mIsLandspace);

            //just for demo not necessary
            if (mIsLandspace) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }

            mAutoStart = bundle.getBoolean(START_ATUO, false);
            mPrintDebugInfo = bundle.getBoolean(SHOW_DEBUGINFO, false);
        }

        mScreenStreamer.setEnableStreamStatModule(true);  //default true
        mScreenStreamer.enableDebugLog(true);  //default false
        //default false may change duraing the streaming
        mScreenStreamer.setMuteAudio(mMuteCheckBox.isChecked());
        mScreenStreamer.setOnInfoListener(mOnInfoListener);
        mScreenStreamer.setOnErrorListener(mOnErrorListener);
        mScreenStreamer.setOnLogEventListener(mOnLogEventListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        requestPermission();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }

        if (mTimer != null) {
            mTimer.cancel();
        }

        if (mPreviewRotateTimer != null) {
            mPreviewRotateTimer.cancel();
        }

        if (mPreviewWindowShow) {
            removeSurfaceWindow();
        }

        if (mCameraPreviewKit != null) {
            mCameraPreviewKit.release();
        }

        mScreenStreamer.release();
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

    /**
     * 开始推流
     */
    private void startStream() {
        mScreenStreamer.startStream();
        mShootingText.setText(R.string.stop_streaming);
        mShootingText.postInvalidate();
        mRecording = true;
        if (mWaterMarkCheckBox.isChecked()) {
            showWaterMark();
        }
    }

    /**
     * 停止推流
     */
    private void stopStream() {
        mScreenStreamer.stopStream();
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.stop();
        mShootingText.setText(R.string.start_streaming);
        mShootingText.postInvalidate();
        mRecording = false;
        if (mWaterMarkCheckBox.isChecked()) {
            hideWaterMark();
        }
    }

    private void onBackoffClick() {
        new AlertDialog.Builder(ScreenActivity.this).setCancelable(true)
                .setTitle("结束直播?")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        return;
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        //停止计时,关闭窗口
                        mChronometer.stop();
                        mRecording = false;
                        ScreenActivity.this.finish();
                    }
                }).show();
    }

    /**
     * 推流开始停止示例
     */
    private void onShootClick() {
        if (mRecording) {
            stopStream();
        } else {
            startStream();
        }
    }

    /**
     * 音频滤镜修改示例
     *
     * @param isChecked
     */
    private void onAudioFilterChecked(boolean isChecked) {
        showChooseAudioFilter();
    }

    /**
     * 音频静音示例
     *
     * @param isChecked
     */
    private void onMuteChecked(boolean isChecked) {
        mScreenStreamer.setMuteAudio(isChecked);
    }

    /**
     * 水印显示隐藏示例
     *
     * @param isChecked
     */
    private void onWaterMarkChecked(boolean isChecked) {
        if (isChecked) {
            if (mRecording) {
                showWaterMark();
            }
        } else {
            hideWaterMark();
        }
    }

    /**
     * show watermark in specific location
     * do not effect on camera preview window
     */
    private void showWaterMark() {
        if (!mIsLandspace) {
            mScreenStreamer.showWaterMarkLogo(mLogoPath, 0.08f, 0.04f, 0.20f, 0, 0.8f);
            mScreenStreamer.showWaterMarkTime(0.03f, 0.01f, 0.35f, Color.WHITE, 1.0f);
        } else {
            mScreenStreamer.showWaterMarkLogo(mLogoPath, 0.05f, 0.09f, 0, 0.20f, 0.8f);
            mScreenStreamer.showWaterMarkTime(0.01f, 0.03f, 0.22f, Color.WHITE, 1.0f);
        }
    }

    /**
     * hide watermark
     * do not effect on camera preview window
     */
    private void hideWaterMark() {
        mScreenStreamer.hideWaterMarkLogo();
        mScreenStreamer.hideWaterMarkTime();
    }

    /**
     * 音频滤镜示例代码
     */
    private boolean[] mChooseFilter = {false, false};

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
                        //添加多种音频滤镜示例代码
                        if (mChooseFilter[0] && mChooseFilter[1]) {
                            List<AudioFilterBase> filters = new LinkedList<>();
                            AudioReverbFilter reverbFilter = new AudioReverbFilter();
                            DemoAudioFilter demofilter = new DemoAudioFilter();
                            filters.add(reverbFilter);
                            filters.add(demofilter);
                            mScreenStreamer.getAudioFilterMgt().setFilter(filters);
                        }
                        //添加一种音频滤镜示例代码(reverb 美声)
                        else if (mChooseFilter[0]) {
                            AudioReverbFilter reverbFilter = new AudioReverbFilter();
                            mScreenStreamer.getAudioFilterMgt().setFilter(reverbFilter);
                        } else if (mChooseFilter[1]) {
                            DemoAudioFilter demofilter = new DemoAudioFilter();
                            mScreenStreamer.getAudioFilterMgt().setFilter(demofilter);
                        }
                        //关闭音频滤镜示例代码
                        else {
                            mScreenStreamer.getAudioFilterMgt().setFilter((AudioFilterBase) null);
                        }
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private class ButtonObserver implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.preview_switch_cam:
                    onSwitchCamera();
                    break;
                case R.id.preview_switch_rotate:
                    onSwitchRotate();
                    break;
                case R.id.backoff:
                    onBackoffClick();
                    break;
                case R.id.click_to_shoot:
                    onShootClick();
                    break;
                default:
                    break;
            }
        }
    }

    private class CheckBoxObserver implements CompoundButton.OnCheckedChangeListener {

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.click_to_select_audio_filter:
                    onAudioFilterChecked(isChecked);
                    break;
                case R.id.mute:
                    onMuteChecked(isChecked);
                    break;
                case R.id.watermark:
                    onWaterMarkChecked(isChecked);
                    break;
                case R.id.screenCameraWindow:
                    onPreviewWindowChecked(isChecked);
                    break;
                case R.id.beautyCameraWindow:
                    onBeautyPreviewChecked(isChecked);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 推流状态变更消息回调
     * 通过IKSYScreenStreamer.setOnInfoListener接口设置
     */
    private KSYScreenStreamer.OnInfoListener mOnInfoListener =
            new KSYScreenStreamer.OnInfoListener() {
                @Override
                public void onInfo(int what, int msg1, int msg2) {
                    switch (what) {
                        case StreamerConstants.KSY_STREAMER_OPEN_STREAM_SUCCESS:
                            Log.d(TAG, "KSY_STREAMER_OPEN_STREAM_SUCCESS");
                            mChronometer.setBase(SystemClock.elapsedRealtime());
                            mChronometer.start();
                            beginInfoUploadTimer();
                            if (mCameraPreviewWindowCheckBox.isChecked() && !mPreviewWindowShow) {
                                startCameraPreview();
                            }
                            break;
                        case StreamerConstants.KSY_STREAMER_FRAME_SEND_SLOW:
                            Log.d(TAG, "KSY_STREAMER_FRAME_SEND_SLOW " + msg1 + "ms");
                            Toast.makeText(ScreenActivity.this, "Network not good!",
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

    /**
     * 推流错误回调
     * 通过IKSYScreenStreamer.setOnErrorListener接口设置
     */
    private KSYScreenStreamer.OnErrorListener mOnErrorListener =
            new KSYScreenStreamer.OnErrorListener() {
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
                        case KSYScreenStreamer.KSY_STREAMER_SCREEN_RECORD_UNSUPPORTED:
                            Log.d(TAG, "KSY_STREAMER_SCREEN_RECORD_UNSUPPORTED");
                            Toast.makeText(ScreenActivity.this, "you android system is below 21, " +
                                            "can not use screenRecord",
                                    Toast.LENGTH_LONG).show();
                            break;
                        case KSYScreenStreamer.KSY_STREAMER_SCREEN_RECORD_PERMISSION_DENIED:
                            Log.d(TAG, "KSY_STREAMER_SCREEN_RECORD_PERMISSION_DENIED");
                            Toast.makeText(ScreenActivity.this, "No ScreenRecord permission, please check",
                                    Toast.LENGTH_LONG).show();
                            break;
                        default:
                            Log.d(TAG, "what=" + what + " msg1=" + msg1 + " msg2=" + msg2);
                            break;
                    }
                    switch (what) {
                        case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED:
                        case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN:
                        case KSYScreenStreamer.KSY_STREAMER_SCREEN_RECORD_UNSUPPORTED:
                            mChronometer.stop();
                            mShootingText.setText(R.string.start_streaming);
                            mShootingText.postInvalidate();
                            mRecording = false;
                            break;
                        default:
                            //重连示例代码
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

    /**
     * 悬浮窗口摄像头预览状态回调
     * 通过接口IKSYCameraPreview.setOnInfoListener接口设置
     */
    private KSYCameraPreview.OnInfoListener mOnPreviewInfoListener =
            new KSYCameraPreview.OnInfoListener() {
                @Override
                public void onInfo(int what, int msg1, int msg2) {
                    switch (what) {
                        case StreamerConstants.KSY_STREAMER_CAMERA_INIT_DONE:
                            Log.d(TAG, "KSY_STREAMER_CAMERA_INIT_DONE");
                            break;
                        default:
                            Log.d(TAG, "OnPreviewInfo: " + what + " msg1: " + msg1 + " msg2: " + msg2);
                            break;
                    }
                }
            };

    /**
     * 悬浮窗口摄像头错误回调
     * 通过接口IKSYCameraPreview.setOnErrorListener接口设置
     */
    private KSYCameraPreview.OnErrorListener mOnPreviewErrorListener =
            new KSYCameraPreview.OnErrorListener() {
                @Override
                public void onError(int what, int msg1, int msg2) {
                    switch (what) {
                        case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_START_FAILED:
                            Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_START_FAILED");
                            break;
                        case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_SERVER_DIED:
                            Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_SERVER_DIED");
                            break;
                        case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_UNKNOWN:
                            Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_UNKNOWN");
                            break;
                        default:
                            Log.d(TAG, "OnPreviewError: " + what + " msg1: " + msg1 + " msg2: " + msg2);
                            break;
                    }

                    if (what == StreamerConstants.KSY_STREAMER_CAMERA_ERROR_SERVER_DIED) {
                        stopCameraPreview();
                        mMainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startCameraPreviewWithPermissionCheck();
                            }
                        }, 5000);
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
        if (mScreenStreamer == null) return;
        mDebugInfo = String.format(Locale.getDefault(),
                "RtmpHostIP()=%s DroppedFrameCount()=%d \n " +
                        "ConnectTime()=%d DnsParseTime()=%d \n " +
                        "UploadedKB()=%d EncodedFrames()=%d \n" +
                        "CurrentKBitrate=%d \n" +
                        "libscreenrecordVersion=%s \n" +
                        "KSYStreamerVersion=%s \n",
                mScreenStreamer.getRtmpHostIP(), mScreenStreamer.getDroppedFrameCount(),
                mScreenStreamer.getConnectTime(), mScreenStreamer.getDnsParseTime(),
                mScreenStreamer.getUploadedKBytes(), mScreenStreamer.getEncodedFrames(),
                mScreenStreamer.getCurrentUploadKBitrate(), mScreenStreamer.getLibScreenStreamerVersion(),
                mScreenStreamer.getKSYStreamerSDKVersion());
    }

    /**
     * 申请权限示例代码
     */
    private void requestPermission() {
        //audio
        int audioperm = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (audioperm != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Log.e(TAG, "No RECORD_AUDIO permission, please check");
                Toast.makeText(this, "No RECORD_AUDIO permission, please check",
                        Toast.LENGTH_LONG).show();
            } else {
                String[] permissions = {Manifest.permission.RECORD_AUDIO};
                ActivityCompat.requestPermissions(this, permissions,
                        PERMISSION_REQUEST_RECORD_AUDIO);
            }
        } else {
            if (mAutoStart) {
                startStream();
            }
        }

        //camera
        if (mPreviewWindowShow) {
            int cameraPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if (cameraPerm != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    Log.e(TAG, "No CAMERA permission, please check");
                    Toast.makeText(this, "No CAMERA permission, please check",
                            Toast.LENGTH_LONG).show();
                } else {
                    String[] permissions = {Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE};
                    ActivityCompat.requestPermissions(this, permissions,
                            PERMISSION_REQUEST_CAMERA);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Log.e(TAG, "No RECORD_AUDIO permission");
                    Toast.makeText(this, "No RECORD_AUDIO permission",
                            Toast.LENGTH_LONG).show();
                }
                break;

            }
            case PERMISSION_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCameraPreviewerPermFlag |= CAMERA_PREVIEW_PERMISSION_CAMERA;
                    if (mCameraPreviewWindowCheckBox.isChecked() && !mPreviewWindowShow) {
                        if (mCameraPreviewerPermFlag == PREVIEW_PERMISSION_ALL_GRANTED) {
                            startCameraPreview();
                        }
                    }
                } else {
                    //没有权限,如果摄像头预览悬浮窗口开着,建议关闭
                    if (mPreviewWindowShow) {
                        stopCameraPreview();
                    }

                    Log.e(TAG, "No CAMERA permission");
                    Toast.makeText(this, "No CAMERA permission",
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    /**
     * 切换摄像头预览悬浮窗口的前后摄像头示例
     */
    private void onSwitchCamera() {
        if (mPreviewWindowShow) {
            mCameraPreviewKit.switchCamera();
        }
    }

    /**
     * 旋转摄像头预览示例代码
     */
    private void onSwitchRotate() {
        if (mPreviewWindowShow) {
            int rotate = mWindowManager.getDefaultDisplay().getRotation();
            if (rotate != mLastRotate) {
                if (rotate == Surface.ROTATION_0
                        || rotate == Surface.ROTATION_180) {
                    //设置摄像头旋转角度
                    if (rotate == Surface.ROTATION_0) {
                        mCameraPreviewKit.setRotateDegrees(0);
                    } else {
                        mCameraPreviewKit.setRotateDegrees(180);
                    }

                    int width = mCameraPreview.getHeight();
                    int height = mCameraPreview.getWidth();
                    LinearLayout.LayoutParams layoutParams =
                            new LinearLayout.LayoutParams(width, height);
                    layoutParams.gravity = Gravity.BOTTOM | Gravity.TOP
                            | Gravity.LEFT | Gravity.RIGHT;
                    //更新CameraPreview布局
                    mFloatLayout.updateViewLayout(mCameraPreview, layoutParams);

                } else if (rotate == Surface.ROTATION_90 ||
                        rotate == Surface.ROTATION_270) {
                    if (rotate == Surface.ROTATION_90) {
                        mCameraPreviewKit.setRotateDegrees(90);
                    } else {
                        mCameraPreviewKit.setRotateDegrees(270);
                    }
                    int width = mCameraPreview.getHeight();
                    int height = mCameraPreview.getWidth();
                    LinearLayout.LayoutParams layoutParams =
                            new LinearLayout.LayoutParams(width, height);
                    layoutParams.gravity = Gravity.BOTTOM | Gravity.TOP
                            | Gravity.LEFT | Gravity.RIGHT;
                    mFloatLayout.updateViewLayout(mCameraPreview, layoutParams);
                }
                //横竖屏切换后，悬浮窗口回到初始位置，您可根据自己的自己的时机需求调整
                updateViewPosition();
                mLastRotate = rotate;
            }
        }
    }

    /**
     * 配置摄像哦图预览悬浮窗口的参数示例代码
     */
    private class CameraPreviewParamsAlertDialog extends AlertDialog {
        private RadioButton mRes360Button;
        private RadioButton mRes480Button;
        private RadioButton mRes540Button;
        private EditText mPreviewFpsEditText;

        protected CameraPreviewParamsAlertDialog(Context context) {
            super(context);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.preview_params_layout);
            mPreviewFpsEditText = (EditText) findViewById(R.id.frameRatePicker);
            mRes360Button = (RadioButton) findViewById(R.id.r360p);
            mRes480Button = (RadioButton) findViewById(R.id.r480p);
            mRes540Button = (RadioButton) findViewById(R.id.r540p);

            Button mConfimButton = (Button) findViewById(R.id.preview_params_confim);
            mConfimButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!TextUtils.isEmpty(mPreviewFpsEditText.getText().toString())) {
                        mPreviewFps = Integer.parseInt(mPreviewFpsEditText.getText().toString());
                    }

                    if (mRes360Button.isChecked()) {
                        mPreviewResolution = StreamerConstants.VIDEO_RESOLUTION_360P;
                    } else if (mRes480Button.isChecked()) {
                        mPreviewResolution = StreamerConstants.VIDEO_RESOLUTION_480P;
                    } else if (mRes540Button.isChecked()) {
                        mPreviewResolution = StreamerConstants.VIDEO_RESOLUTION_540P;
                    } else {
                        mPreviewResolution = StreamerConstants.VIDEO_RESOLUTION_720P;
                    }

                    ScreenActivity.this.mMainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startCameraPreviewWithPermissionCheck();
                        }
                    }, 100);

                    CameraPreviewParamsAlertDialog.this.dismiss();
                }
            });
        }

    }

    /**
     * 打开关闭 摄像头预览悬浮窗口示例
     *
     * @param isChecked
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private void onPreviewWindowChecked(boolean isChecked) {
        if (isChecked) {
            //参数配置
            CameraPreviewParamsAlertDialog alertDialog = new CameraPreviewParamsAlertDialog(this);
            alertDialog.setCancelable(false);
            alertDialog.show();

            //美颜按钮开启
            mBeautyCameraPreviewCheckBox.setEnabled(true);

        } else {
            if (mPreviewWindowShow) {
                stopCameraPreview();
            }
            mBeautyCameraPreviewCheckBox.setEnabled(false);
        }
    }

    /**
     * 摄像头悬浮窗口美颜切换示例
     *
     * @param isChecked
     */
    private void onBeautyPreviewChecked(boolean isChecked) {
        if (isChecked) {
            if (mPreviewWindowShow) {
                showChooseFilter();
            } else {
                Toast.makeText(this, "only effect on preview camera window",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            if (mPreviewWindowShow) {
                mCameraPreviewKit.getImgTexFilterMgt().setFilter(
                        mCameraPreviewKit.getGLRender(),
                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_DISABLE);
            }
        }
    }

    /**
     * 开启摄像头预览悬浮窗口示例
     */
    private void startCameraPreviewWithPermissionCheck() {
        if (mCameraPreviewWindowCheckBox.isChecked() && !mPreviewWindowShow) {
            //检查摄像头权限
            int cameraPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            boolean canDrawOverlay = true;
            //6.0 需要检查overlay权限
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                canDrawOverlay = Settings.canDrawOverlays(this);
            }

            if (cameraPerm != PackageManager.PERMISSION_GRANTED || !canDrawOverlay) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    Log.e(TAG, "No CAMERA or overLay permission, please check");
                    Toast.makeText(this, "No CAMERA or overLay permission, please check",
                            Toast.LENGTH_LONG).show();
                } else {
                    //没有overlay权限
                    if (!canDrawOverlay) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, OVERLAY_PERMISSION_RESULT_CODE);
                    } else {
                        mCameraPreviewerPermFlag |= CAMERA_PREVIEW_PERMISSION_OVERLAY;
                    }

                    //没有摄像头权限
                    if (cameraPerm != PackageManager.PERMISSION_GRANTED) {
                        String[] permissions = {Manifest.permission.CAMERA,
                                Manifest.permission.READ_EXTERNAL_STORAGE};
                        ActivityCompat.requestPermissions(this, permissions,
                                PERMISSION_REQUEST_CAMERA);
                    } else {
                        mCameraPreviewerPermFlag |= CAMERA_PREVIEW_PERMISSION_CAMERA;
                    }
                }
            } else {
                mCameraPreviewerPermFlag = 0x03;
                startCameraPreview();
            }
        }
    }

    /**
     * 申请overlay权限窗口返回
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_RESULT_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    // SYSTEM_ALERT_WINDOW permission not granted...
                    Toast.makeText(ScreenActivity.this, "SYSTEM_ALERT_WINDOW not granted",
                            Toast.LENGTH_SHORT).show();
                } else {
                    mCameraPreviewerPermFlag |= CAMERA_PREVIEW_PERMISSION_OVERLAY;

                    if (mCameraPreviewerPermFlag == PREVIEW_PERMISSION_ALL_GRANTED) {
                        startCameraPreview();
                    }
                }
            }
        }
    }

    /**
     * 调用该接口时需要确保preview窗口不显示
     */
    private void startCameraPreview() {
        initSurfaceWindow();

        if (mCameraPreviewKit == null) {
            mCameraPreviewKit = new KSYCameraPreview(this);
            mCameraPreviewKit.setDisplayPreview(mCameraPreview);
            mCameraPreviewKit.setOnErrorListener(mOnPreviewErrorListener);
            mCameraPreviewKit.setOnInfoListener(mOnPreviewInfoListener);
        }

        addSurfaceWindow();

        //设置采集帧率,默认15
        //设置无效值(<=0 时,该接口会抛出异常IllegalArgumentException
        if (mPreviewFps != 0) {
            mCameraPreviewKit.setPreviewFps(mPreviewFps);
        }

        //设置摄像头预览分辨率
        //设置无效值时,该接口会抛出异常该接口会抛出异常IllegalArgumentException
        if (mPreviewResolution >= StreamerConstants.VIDEO_RESOLUTION_360P ||
                mPreviewResolution <= StreamerConstants.VIDEO_RESOLUTION_720P) {
            mCameraPreviewKit.setPreviewResolution(mPreviewResolution);
        }

        //设想摄像头旋转角度
        mCameraPreviewKit.setRotateDegrees(mIsLandspace ? 90 : 0);

        //开始预览
        mCameraPreviewKit.startCameraPreview();
    }

    /**
     * 调用该接口时需要确保preview窗口在显示
     */
    private void stopCameraPreview() {
        if (mCameraPreviewKit != null) {
            mCameraPreviewKit.stopCameraPreview();
        }

        removeSurfaceWindow();
    }

    /**
     * 初始化悬浮窗口示例
     */
    private void initSurfaceWindow() {
        if (mWindowManager == null) {
            mWmParams = new WindowManager.LayoutParams();
            mWindowManager = (WindowManager) getApplication().
                    getSystemService(getApplication().WINDOW_SERVICE);

            Log.i(TAG, "mWindowManager--->" + mWindowManager);

            //设置window type
            mWmParams.type = WindowManager.LayoutParams.TYPE_TOAST;

            //设置图片格式，效果为背景透明
            mWmParams.format = PixelFormat.RGBA_8888;

            //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
            mWmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            //接收touch事件
            mWmParams.flags |= WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
            //排版不受限制
            mWmParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            //调整悬浮窗显示的停靠位置为左侧置顶
            mWmParams.gravity = Gravity.RIGHT | Gravity.TOP;


            // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
            mWmParams.x = 0;
            mWmParams.y = 0;

            //设置悬浮窗口长宽数据(这里取屏幕长宽的等比率缩小值作为悬浮窗口的长宽)
            int screenWidth = mWindowManager.getDefaultDisplay().getWidth();
            int screenHeight = mWindowManager.getDefaultDisplay().getHeight();
            int width;
            int height;
            if ((mIsLandspace && screenWidth < screenHeight) ||
                    (!mIsLandspace) && screenWidth > screenHeight) {
                screenWidth = mWindowManager.getDefaultDisplay().getHeight();
                screenHeight = mWindowManager.getDefaultDisplay().getWidth();
            }

            if (screenWidth < screenHeight) {
                width = align(screenWidth / 3, 8);
                height = align(screenHeight / 4, 8);
            } else {
                width = align(screenWidth / 4, 8);
                height = align(screenHeight / 3, 8);
            }
            mWmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            mWmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

            LayoutInflater inflater = LayoutInflater.from(getApplication());

            //获取浮动窗口视图所在布局
            mFloatLayout = (FloatView) inflater.inflate(R.layout.camera_preview, null);
            mSwitchCameraView = (ImageView) mFloatLayout.findViewById(R.id.preview_switch_cam);
            mSwitchCameraView.setOnClickListener(mObserverButton);
            mSwitchCameraRotate = (ImageView) mFloatLayout.findViewById(R.id.preview_switch_rotate);
            mSwitchCameraRotate.setOnClickListener(mObserverButton);

            mCameraPreview = new GLSurfaceView(this);
            LinearLayout.LayoutParams previewLayoutParams =
                    new LinearLayout.LayoutParams(width, height);
            previewLayoutParams.gravity = Gravity.BOTTOM | Gravity.TOP | Gravity.LEFT | Gravity.RIGHT;

            mFloatLayout.addView(mCameraPreview, previewLayoutParams);
            mFloatLayout.setWmParams(mWmParams);
            mLastRotate = mWindowManager.getDefaultDisplay().getRotation();

            mPreviewRotateTimer = new Timer("PreviewRotateMonitor");
            mPreviewRotateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onSwitchRotate();
                        }
                    });
                }
            }, 100, 1000);
        }
    }

    private void addSurfaceWindow() {
        if (mWindowManager != null) {
            //添加mFloatLayout
            mWindowManager.addView(mFloatLayout, mWmParams);
        }
        mPreviewWindowShow = true;
    }

    private void removeSurfaceWindow() {
        if (mWindowManager != null) {
            mWindowManager.removeViewImmediate(mFloatLayout);
        }
        mPreviewWindowShow = false;
    }

    /**
     * 摄像头预览滤镜示例
     */
    private void showChooseFilter() {
        AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(this)
                .setTitle("请选择悬浮窗口美颜滤镜")
                .setSingleChoiceItems(
                        new String[]{"BEAUTY_SOFT", "SKIN_WHITEN", "BEAUTY_ILLUSION", "DENOISE",
                                "BEAUTY_SMOOTH", "DEMOFILTER", "GROUP_FILTER"}, -1,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which < 5) {
                                    mCameraPreviewKit.getImgTexFilterMgt().setFilter(
                                            mCameraPreviewKit.getGLRender(), which + 16);
                                } else if (which == 5) {
                                    mCameraPreviewKit.getImgTexFilterMgt().setFilter(
                                            new DemoFilter(mCameraPreviewKit.getGLRender()));
                                } else if (which == 6) {
                                    List<ImgTexFilter> groupFilter = new LinkedList<>();
                                    groupFilter.add(new DemoFilter2(mCameraPreviewKit.getGLRender()));
                                    groupFilter.add(new DemoFilter3(mCameraPreviewKit.getGLRender()));
                                    groupFilter.add(new DemoFilter4(mCameraPreviewKit.getGLRender()));
                                    mCameraPreviewKit.getImgTexFilterMgt().setFilter(groupFilter);
                                }
                                dialog.dismiss();
                            }
                        })
                .create();
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void updateViewPosition() {
        if (mWmParams != null && mWindowManager != null) {
            mWmParams.gravity = Gravity.RIGHT | Gravity.TOP;

            // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
            mWmParams.x = 0;
            mWmParams.y = 0;
            mWindowManager.updateViewLayout(mFloatLayout, mWmParams);  //刷新显示
        }
    }

    private int align(int val, int align) {
        return (val + align - 1) / align * align;
    }
}
