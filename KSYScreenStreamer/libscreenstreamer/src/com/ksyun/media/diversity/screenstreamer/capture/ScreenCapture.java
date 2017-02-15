package com.ksyun.media.diversity.screenstreamer.capture;

import com.ksyun.media.streamer.framework.ImgTexFormat;
import com.ksyun.media.streamer.framework.ImgTexFrame;
import com.ksyun.media.streamer.framework.SrcPin;
import com.ksyun.media.streamer.logstats.StatsLogReport;
import com.ksyun.media.streamer.util.gles.GLRender;
import com.ksyun.media.streamer.util.gles.GlUtil;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * capture video frames from screen
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ScreenCapture implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = ScreenCapture.class.getSimpleName();

    public static final String ASSISTANTACITVITYCREATED_INTENT = "ScreenCapture.OnAssistantActivityCreated";
    public static final int MEDIAPROJECTION_REQUEST_CODE = 1001;

    private Context mContext;
    private OnScreenCaptureListener mOnScreenCaptureListener;
    public MediaProjectionManager mMediaProjectManager;  //mMediaProjectionManager
    private MediaProjection mMediaProjection;  //mMediaProjection
    private VirtualDisplay mVirtualDisplay;  // mVirtualDisplay
    public static ScreenCaptureAssistantActivity mScreenCaptureActivity;
    private BroadcastReceiver mScreenBroadcastReceiver = new ScreenBroadcastReceiver(this);


    private int mWidth = 1280;  //mWidth
    private int mHeight = 720;  //mHegith

    //录屏状态
    public final static int SCREEN_STATE_IDLE = 0;
    public final static int SCREEN_STATE_INITIALIZING = 1;
    public final static int SCREEN_STATE_INITIALIZED = 2;
    public final static int SCREEN_STATE_STOPPING = 3;
    public final static int SCREEN_STATE_CAPTUREING = 4;

    //录屏错误码
    public final static int SCREEN_ERROR_SYSTEM_UNSUPPORTED = -1;
    public final static int SCREEN_ERROR_PERMISSION_DENIED = -2;

    //录屏状态码
    public final static int SCREEN_RECORD_STARTED = 4;
    public final static int SCREEN_RECORD_FAILED = 5;

    //录屏处理msg
    private final static int MSG_SCREEN_START_SCREEN_ACTIVITY = 1;
    private final static int MSG_SCREEN_INIT_PROJECTION = 2;
    private final static int MSG_SCREEN_START = 3;
    private final static int MSG_SCREEN_RELEASE = 4;
    private final static int MSG_SCREEN_QUIT = 5;

    private final static int RELEASE_SCREEN_THREAD = 1;

    //状态标记
    private AtomicInteger mState;

    private GLRender mGLRender;
    private int mTextureId;
    private Surface mSurface;
    private SurfaceTexture mSurfaceTexture;
    private boolean mTexInited = false;
    private ImgTexFormat mImgTexFormat;

    private Handler mMainHandler;
    private HandlerThread mScreenSetupThread;
    private Handler mScreenSetupHandler;

    // fill extra frame
    private Runnable mFillFrameRunnable;

    private final static boolean TRACE = true;
    // Performance trace
    private long mLastTraceTime;
    private long mFrameDrawed;

    /**
     * Source pin transfer ImgTexFrame, used for gpu path and preview
     */
    public SrcPin<ImgTexFrame> mImgTexSrcPin;

    public ScreenCapture(Context context, GLRender render) {
        if (context == null || render == null) {
            throw new IllegalArgumentException("the context or render must be not null");
        }

        mContext = context;
        mGLRender = render;
        mGLRender.addListener(mGLRenderListener);
        mImgTexSrcPin = new SrcPin<>();
        mMainHandler = new MainHandler(this);
        mState = new AtomicInteger(SCREEN_STATE_IDLE);
        mFillFrameRunnable = new Runnable() {
            @Override
            public void run() {
                if (mState.get() == SCREEN_STATE_CAPTUREING) {
                    mGLRender.requestRender();
                    mMainHandler.postDelayed(mFillFrameRunnable, 100);
                }
            }
        };

        initScreenSetupThread();
    }

    /**
     * Start screen record.<br/>
     * Can only be called on mState IDLE.
     */
    public boolean start() {
        if (StatsLogReport.getInstance().getEnableDebugLog()) {
            Log.d(TAG, "start");
        }

        if (mState.get() != SCREEN_STATE_IDLE) {
            return false;
        }

        if (Build.VERSION.SDK_INT < 21) {
            Message msg = mMainHandler.obtainMessage(SCREEN_RECORD_FAILED, SCREEN_ERROR_SYSTEM_UNSUPPORTED, 0);
            mMainHandler.sendMessage(msg);
            return false;
        }

        //if the screen permission is show, will start failed
        if (mScreenCaptureActivity != null) {
            Log.e(TAG, "start failed you may best confim the user permission");
            return false;
        }

        mState.set(SCREEN_STATE_INITIALIZING);
        mScreenSetupHandler.removeMessages(MSG_SCREEN_START_SCREEN_ACTIVITY);
        mScreenSetupHandler.sendEmptyMessage(MSG_SCREEN_START_SCREEN_ACTIVITY);
        return true;
    }

    /**
     * stop screen record
     */
    public void stop() {
        if (StatsLogReport.getInstance().getEnableDebugLog()) {
            Log.d(TAG, "stop");
        }

        if (mState.get() == SCREEN_STATE_IDLE) {
            return;
        }

        // stop fill frame
        mMainHandler.removeCallbacks(mFillFrameRunnable);

        Message msg = new Message();
        msg.what = MSG_SCREEN_RELEASE;
        msg.arg1 = ~RELEASE_SCREEN_THREAD;

        mState.set(SCREEN_STATE_STOPPING);
        mScreenSetupHandler.removeMessages(MSG_SCREEN_RELEASE);
        mScreenSetupHandler.sendMessage(msg);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public void release() {
        // stop fill frame
        if (mMainHandler != null) {
            mMainHandler.removeCallbacks(mFillFrameRunnable);
        }

        if (mState.get() == SCREEN_STATE_IDLE) {
            mScreenSetupHandler.removeMessages(MSG_SCREEN_QUIT);
            mScreenSetupHandler.sendEmptyMessage(MSG_SCREEN_QUIT);
            quitThread();
            return;
        }

        Message msg = new Message();
        msg.what = MSG_SCREEN_RELEASE;
        msg.arg1 = RELEASE_SCREEN_THREAD;

        mState.set(SCREEN_STATE_STOPPING);
        mScreenSetupHandler.removeMessages(MSG_SCREEN_RELEASE);
        mScreenSetupHandler.sendMessage(msg);

        quitThread();
    }

    /**
     * screen status changed listener
     *
     * @param listener
     */
    public void setOnScreenCaptureListener(OnScreenCaptureListener listener) {
        mOnScreenCaptureListener = listener;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (mState.get() != SCREEN_STATE_CAPTUREING) {
            return;
        }
        mGLRender.requestRender();
        if (mMainHandler != null) {
            mMainHandler.removeCallbacks(mFillFrameRunnable);
            mMainHandler.postDelayed(mFillFrameRunnable, 100);
        }
    }

    private void initTexFormat() {
        mImgTexFormat = new ImgTexFormat(ImgTexFormat.COLOR_EXTERNAL_OES, mWidth, mHeight);
        mImgTexSrcPin.onFormatChanged(mImgTexFormat);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public final void initProjection(int requestCode, int resultCode, Intent intent) {
        if (StatsLogReport.getInstance().getEnableDebugLog()) {
            Log.d(TAG, "initProjection");
        }
        mContext.unregisterReceiver(this.mScreenBroadcastReceiver);
        if (requestCode != MEDIAPROJECTION_REQUEST_CODE) {
            if (StatsLogReport.getInstance().getEnableDebugLog()) {
                Log.d(TAG, "Unknown request code: " + requestCode);
            }
        } else if (resultCode != -1) {
            Log.e(TAG, "Screen Cast Permission Denied, resultCode:" + resultCode);
            Message msg = mMainHandler.obtainMessage(SCREEN_RECORD_FAILED,
                    SCREEN_ERROR_PERMISSION_DENIED, 0);
            mMainHandler.sendMessage(msg);
            stop();
        } else {
            //get mediaProjectiong and vierturlDisplay
            mMediaProjection = mMediaProjectManager.getMediaProjection(resultCode, intent);

            if (mSurface != null) {
                startScreenCapture();
            } else {
                mState.set(SCREEN_STATE_INITIALIZED);
            }
        }
    }

    private GLRender.GLRenderListener mGLRenderListener = new GLRender.GLRenderListener() {
        @Override
        public void onReady() {
            Log.d(TAG, "onReady");
        }

        @Override
        public void onSizeChanged(int width, int height) {
            Log.d(TAG, "onSizeChanged : " + width + "*" + height);
            mWidth = width;
            mHeight = height;

            mTexInited = false;

            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
                mVirtualDisplay = null;
            }

            mTextureId = GlUtil.createOESTextureObject();
            if (mSurfaceTexture != null) {
                mSurfaceTexture.release();
            }

            if (mSurface != null) {
                mSurface.release();
            }
            mSurfaceTexture = new SurfaceTexture(mTextureId);
            mSurfaceTexture.setDefaultBufferSize(mWidth, mHeight);
            mSurface = new Surface(mSurfaceTexture);

            mSurfaceTexture.setOnFrameAvailableListener(ScreenCapture.this);

            if (mState.get() >= SCREEN_STATE_INITIALIZED && mVirtualDisplay == null) {
                mScreenSetupHandler.removeMessages(MSG_SCREEN_START);
                mScreenSetupHandler.sendEmptyMessage(MSG_SCREEN_START);
            }
        }

        @Override
        public void onDrawFrame() {
            long pts = System.nanoTime() / 1000 / 1000;
            try {
                mSurfaceTexture.updateTexImage();
            } catch (Exception e) {
                Log.e(TAG, "updateTexImage failed, ignore");
                return;
            }

            if (!mTexInited) {
                mTexInited = true;
                initTexFormat();
            }

            float[] texMatrix = new float[16];
            mSurfaceTexture.getTransformMatrix(texMatrix);
            ImgTexFrame frame = new ImgTexFrame(mImgTexFormat, mTextureId, texMatrix, pts);
            try {
                mImgTexSrcPin.onFrameAvailable(frame);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Draw frame failed, ignore");
            }

            if (TRACE) {
                mFrameDrawed++;
                long tm = System.currentTimeMillis();
                long tmDiff = tm - mLastTraceTime;
                if (tmDiff >= 5000) {
                    float fps = mFrameDrawed * 1000.f / tmDiff;
                    Log.d(TAG, "screen fps: " + String.format(Locale.getDefault(), "%.2f", fps));
                    mFrameDrawed = 0;
                    mLastTraceTime = tm;
                }
            }
        }

        @Override
        public void onReleased() {

        }
    };

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScreenCapture() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenCapture",
                mWidth, mHeight, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mSurface,
                null, null);

        mState.set(SCREEN_STATE_CAPTUREING);
        Message msg = mMainHandler.obtainMessage(SCREEN_RECORD_STARTED, 0, 0);
        mMainHandler.sendMessage(msg);
    }

    private static class MainHandler extends Handler {
        private final WeakReference<ScreenCapture> weakCapture;

        public MainHandler(ScreenCapture screenCapture) {
            super();
            this.weakCapture = new WeakReference<>(screenCapture);
        }

        @Override
        public void handleMessage(Message msg) {
            ScreenCapture screenCapture = weakCapture.get();
            if (screenCapture == null) {
                return;
            }
            switch (msg.what) {
                case SCREEN_RECORD_STARTED:
                    if (screenCapture.mOnScreenCaptureListener != null) {
                        screenCapture.mOnScreenCaptureListener.onStarted();
                    }
                    break;
                case SCREEN_RECORD_FAILED:
                    if (screenCapture.mOnScreenCaptureListener != null) {
                        screenCapture.mOnScreenCaptureListener.onError(msg.arg1);
                    }
                    break;
                default:
                    break;

            }
        }
    }

    private void initScreenSetupThread() {
        mScreenSetupThread = new HandlerThread("screen_setup_thread", Thread.NORM_PRIORITY);
        mScreenSetupThread.start();
        mScreenSetupHandler = new Handler(mScreenSetupThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_SCREEN_START_SCREEN_ACTIVITY: {
                        doScreenSetup();
                        break;
                    }
                    case MSG_SCREEN_INIT_PROJECTION: {
                        initProjection(msg.arg1, msg.arg2, mProjectionIntent);
                        break;
                    }
                    case MSG_SCREEN_START: {
                        startScreenCapture();
                        break;
                    }
                    case MSG_SCREEN_RELEASE: {
                        doScreenRelease(msg.arg1);
                        break;
                    }
                    case MSG_SCREEN_QUIT: {
                        mScreenSetupThread.quit();
                    }
                }
            }
        };
    }

    private void quitThread() {
        try {
            mScreenSetupThread.join();
        } catch (InterruptedException e) {
            Log.d(TAG, "ScreenSetUpThread Interrupted!");
        } finally {
            mScreenSetupThread = null;
        }

        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }
    }

    private void doScreenSetup() {
        if (StatsLogReport.getInstance().getEnableDebugLog()) {
            Log.d(TAG, "doScreenSetup");
        }

        if (mMediaProjectManager == null) {
            mMediaProjectManager = (MediaProjectionManager) mContext.getSystemService(
                    Context.MEDIA_PROJECTION_SERVICE);
        }
        //registReveiver
        IntentFilter intentFilter;
        (intentFilter = new IntentFilter()).addAction(ASSISTANTACITVITYCREATED_INTENT);
        mContext.registerReceiver(mScreenBroadcastReceiver, intentFilter);

        //start ScreenCaptureAssistantActivity for mediaprojection onActivityResult
        Intent intent;
        (intent = new Intent(mContext, ScreenCapture.ScreenCaptureAssistantActivity.class)).addFlags(268435456);
        mContext.startActivity(intent);
    }

    private void doScreenRelease(int isQuit) {
        if (StatsLogReport.getInstance().getEnableDebugLog()) {
            Log.d(TAG, "doRelease");
        }

        mState.set(SCREEN_STATE_IDLE);

        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }

        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }

        mVirtualDisplay = null;
        mMediaProjection = null;

        if (isQuit == RELEASE_SCREEN_THREAD) {
            mScreenSetupHandler.sendEmptyMessage(MSG_SCREEN_QUIT);
        }
    }

    public Intent mProjectionIntent;

    public static class ScreenCaptureAssistantActivity extends Activity {
        public ScreenCapture mScreenCapture;  //init in the ScreenBroadcastReceiver;

        public ScreenCaptureAssistantActivity() {
        }

        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            requestWindowFeature(1);
            ScreenCapture.mScreenCaptureActivity = this;
            //send to start the media_projecton activity
            Intent intent = new Intent(ASSISTANTACITVITYCREATED_INTENT);
            sendBroadcast(intent);
        }

        public void onActivityResult(int requestCode, int resultCode, Intent intent) {
            //mScreenCaptureActivity 代表外部已经要求停止推流了,此时忽略本次的用户响应
            if (mScreenCapture != null && mScreenCapture.mState.get() != SCREEN_STATE_IDLE) {
                Message msg = new Message();
                msg.what = MSG_SCREEN_INIT_PROJECTION;
                msg.arg1 = requestCode;
                msg.arg2 = resultCode;
                mScreenCapture.mProjectionIntent = intent;
                mScreenCapture.mScreenSetupHandler.removeMessages(MSG_SCREEN_INIT_PROJECTION);
                mScreenCapture.mScreenSetupHandler.sendMessage(msg);
            }
            //拿到MediaProjection,该窗口就没有任何意义了了,直接关闭
            finish();
            mScreenCapture = null;
            ScreenCapture.mScreenCaptureActivity = null;
        }
    }


    public interface OnScreenCaptureListener {

        /**
         * Notify screen capture started.
         */
        void onStarted();


        /**
         * Notify error occurred while camera capturing.
         *
         * @param err err code.
         * @see #SCREEN_ERROR_SYSTEM_UNSUPPORTED
         * @see #SCREEN_ERROR_PERMISSION_DENIED
         */
        void onError(int err);
    }

}
