package com.ksyun.media.diversity.kiwiandroid;

import android.content.Context;
import android.util.Log;

import com.ksyun.media.diversity.kiwiandroid.KwTrackerWrapper;
import com.ksyun.media.streamer.capture.CameraCapture;
import com.ksyun.media.streamer.filter.imgtex.ImgFilterBase;
import com.ksyun.media.streamer.framework.ImgBufFrame;
import com.ksyun.media.streamer.framework.ImgTexFormat;
import com.ksyun.media.streamer.framework.ImgTexFrame;
import com.ksyun.media.streamer.framework.SinkPin;
import com.ksyun.media.streamer.framework.SrcPin;
import com.ksyun.media.streamer.util.gles.GLRender;

/**
 * Created by xingkai on 2017/7/28.
 */

public class KwfaceFilter extends ImgFilterBase {

    private static final String TAG = "KwfaceFilter";
    private static final int SINK_NUM = 2;
    private GLRender mGLRender;
    private SinkPin<ImgTexFrame> mTexSinkPin;
    private SinkPin<ImgBufFrame> mBufSinkPin;
    private SrcPin<ImgTexFrame> mSrcPin;

    private byte[] mInputBufArray = null;
    private Object BUF_LOCK = new Object();

    private Context mContext;
    private int mOutTexture = ImgTexFrame.NO_TEXTURE;
    private CameraCapture mCamera;

    private KwTrackerWrapper mKwTracker;
    private boolean mInited = false;
    private boolean mIsSurfaceCreated = false;

    public KwfaceFilter(Context context, GLRender glRender, CameraCapture camera) {
        mContext = context;
        mGLRender = glRender;
        mCamera = camera;

        mTexSinkPin = new KwfaceTexSinPin();
        mBufSinkPin = new KwfaceBufSinPin();
        mSrcPin = new SrcPin<>();

        mGLRender.addListener(mGLRenderListener);
    }

    public SinkPin<ImgTexFrame> getTexSinkPin() {
        return mTexSinkPin;
    }

    public SinkPin<ImgBufFrame> getBufSinkPin() {
        return mBufSinkPin;
    }

    @Override
    public int getSinkPinNum() {
        return SINK_NUM;
    }

    @Override
    public SinkPin<ImgTexFrame> getSinkPin(int i) {
        return mTexSinkPin;
    }

    public SrcPin<ImgTexFrame> getSrcPin() {
        return mSrcPin;
    }

    public void setMirror(boolean isMirror) {
    }

    public void release() {
        mSrcPin.disconnect(true);
        if (mOutTexture != ImgTexFrame.NO_TEXTURE) {
            mGLRender.getFboManager().unlock(mOutTexture);
            mOutTexture = ImgTexFrame.NO_TEXTURE;
        }
        mInputBufArray = null;
    }


    private class KwfaceTexSinPin extends SinkPin<ImgTexFrame> {
        @Override
        public void onFormatChanged(Object format) {
            ImgTexFormat fmt = (ImgTexFormat) format;

            mSrcPin.onFormatChanged(fmt);
        }

        @Override
        public void onFrameAvailable(ImgTexFrame frame) {
            if (mSrcPin.isConnected()) {
                if (!mInited) {
                    mKwTracker.onSurfaceChanged(frame.format.width, frame.format.height,
                            frame.format.width, frame.format.height);
                    mInited = true;
                }
                synchronized (BUF_LOCK) {
                    mOutTexture = mKwTracker.onDrawOESTexture(frame.textureId, frame.format.width, frame.format.height);
                }
            }

            ImgTexFrame outFrame = new ImgTexFrame(frame.format, mOutTexture, null, frame.pts);
            mSrcPin.onFrameAvailable(outFrame);
        }

        @Override
        public void onDisconnect(boolean recursive) {
            if (recursive) {
                release();
            }
        }
    }

    private class KwfaceBufSinPin extends SinkPin<ImgBufFrame> {

        @Override
        public void onFormatChanged(Object format) {

        }

        @Override
        public void onFrameAvailable(ImgBufFrame frame) {

        }

        @Override
        public void onDisconnect(boolean recursive) {

        }

    }

    public void setKwTrackerWrapper(KwTrackerWrapper kwTrackerWrapper) {
        this.mKwTracker = kwTrackerWrapper;
    }

    GLRender.GLRenderListener mGLRenderListener = new GLRender.GLRenderListener() {
        @Override
        public void onReady() {
            Log.d(TAG, "onReady: ");
            mKwTracker.onSurfaceCreated(mContext);
        }

        @Override
        public void onSizeChanged(int width, int height) {
                mKwTracker.onSurfaceChanged(width, height, width,height);
        }

        @Override
        public void onDrawFrame() {
            if (!mIsSurfaceCreated) {
                mKwTracker.onSurfaceCreated(mContext);
                mIsSurfaceCreated = true;
            }
        }

        @Override
        public void onReleased() {
            Log.d(TAG, "onReleased: ");
            mKwTracker.onSurfaceDestroyed();
        }
    };
}
