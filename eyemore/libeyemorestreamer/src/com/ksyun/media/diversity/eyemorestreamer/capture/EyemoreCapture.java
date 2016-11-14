package com.ksyun.media.diversity.eyemorestreamer.capture;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.ksyun.media.streamer.util.gles.GLRender;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.BitmapFactory;
import com.ksyun.media.streamer.capture.ImgTexSrcPin;

/**
 * The EyemoreCapture class is used to capture video frames from eyemore.
 */
public class EyemoreCapture implements EyeWrapper.OnFrameBuf{
    private final static String TAG = "EyemoreCapture";
    private final static boolean VERBOSE = false;

    //Eyemore状态
    public final static int EYEMORE_STATE_IDLE = 0;
    public final static int EYEMORE_STATE_INITIALIZING = 1;
    public final static int EYEMORE_STATE_STOPPING = 2;

    //Eyemore消息msg
    private final static int MSG_EYEMORE_RELEASE = 1;
    private final static int MSG_EYEMORE_QUIT = 2;

    public ImgTexSrcPin mSrcPin;

    //状态标记
    private AtomicInteger mState;

    private HandlerThread mEyemoreSetUpThread;
    private Handler mEyemoreSetupHandler;
    private GLRender mGLRender;
    private ExecutorService mExecutorService;
    private EyeWrapper mEyemore;

    public EyemoreCapture(Context context, GLRender glRender) {
        mGLRender = glRender;
        mState = new AtomicInteger(EYEMORE_STATE_IDLE);
        mSrcPin = new ImgTexSrcPin(glRender);
        mExecutorService = Executors.newSingleThreadExecutor();
        mEyemore = new EyeWrapper();
        mEyemore.setonFrameBufCallback(this);

        initEyemoreSetupThread();
    }

    public void start() {
        if (mState.get() != EYEMORE_STATE_IDLE) {
            Log.e(TAG, "Call start on invalid state");
            return;
        }
        mState.set(EYEMORE_STATE_INITIALIZING);
    }

    public void stop() {
        if (mState.get() == EYEMORE_STATE_IDLE || mState.get() == EYEMORE_STATE_STOPPING) {
            return;
        }
        mState.set(EYEMORE_STATE_STOPPING);
        mEyemoreSetupHandler.removeMessages(MSG_EYEMORE_RELEASE);
        mEyemoreSetupHandler.sendEmptyMessage(MSG_EYEMORE_RELEASE);
    }

    public void release() {
        stop();
        mSrcPin.release();

        if (mEyemoreSetUpThread != null) {
            mEyemoreSetupHandler.sendEmptyMessage(MSG_EYEMORE_QUIT);
            try {
                mEyemoreSetUpThread.join();
            } catch (InterruptedException e) {
                Log.d(TAG, "EyemoreSetUpThread Interrupted!");
            } finally {
                mEyemoreSetUpThread = null;
            }
        }
    }

    private void initEyemoreSetupThread() {
        mEyemoreSetUpThread = new HandlerThread("eyemore_thread", Thread.NORM_PRIORITY);
        mEyemoreSetUpThread.start();
        mEyemoreSetupHandler = new Handler(mEyemoreSetUpThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_EYEMORE_RELEASE: {
                        doEyemoreRelease();
                        break;
                    }
                    case MSG_EYEMORE_QUIT: {
                        mEyemoreSetUpThread.quit();
                    }
                }
            }
        };
    }

    private void doEyemoreRelease() {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                mSrcPin.updateFrame(null, false);
            }
        });
        mState.set(EYEMORE_STATE_IDLE);
    }

    public int init_USB(int fd, String devPath) {
        return mEyemore.initUsb(fd, devPath);
    }

    public void destroy_USB() {
        mEyemore.destroyUsb();
    }

    @Override
    public void onFrameBuf(byte[] data, int len) {
        if (data != null && data.length == len){
            final long pts = System.nanoTime() / 1000 / 1000;

            final Bitmap argbBitmap = BitmapFactory.decodeByteArray(data, 0, len);

            mExecutorService.execute(new Runnable() {
                @Override
                public void run() {mSrcPin.updateFrame(argbBitmap, pts, true);
                    mGLRender.requestRender();
                }
            });
        }
    }
}
