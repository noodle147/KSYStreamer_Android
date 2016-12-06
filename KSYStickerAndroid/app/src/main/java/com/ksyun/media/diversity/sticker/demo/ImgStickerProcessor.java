package com.ksyun.media.diversity.sticker.demo;


import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.util.Log;

import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterBase;
import com.ksyun.media.streamer.framework.ImgBufFrame;
import com.ksyun.media.streamer.framework.ImgTexFormat;
import com.ksyun.media.streamer.framework.ImgTexFrame;
import com.ksyun.media.streamer.framework.SinkPin;
import com.ksyun.media.streamer.framework.SrcPin;
import com.ksyun.media.streamer.util.gles.FboManager;
import com.ksyun.media.streamer.util.gles.GLRender;
import com.sensetime.stmobile.STMobileStickerNative;

import javax.microedition.khronos.opengles.GL10;


/**
 * Created by qyvideo on 11/5/16.
 */

public class ImgStickerProcessor {

    private final String TAG = "ImgStickerProcessor";
    protected GLRender mGLRender;
    protected boolean mInited;
    protected int mOutTexture = ImgTexFrame.NO_TEXTURE;
    private int mOutFrameBuffer = -1;
    private int[] mViewPort = new int[4];
    private int mStickerInstanceCreated = -1;
    private byte[] mInputBufArray = null;
    private static final int SINK_NUM = 2;
    public static final int ST_ERROR = -1;
    private static final int UNINIT = -1;

    /**
     * Tex Input pin
     */
    private SinkPin<ImgTexFrame> mImgTexSinkPin;
    /**
     * Output pin
     */
    private SrcPin<ImgTexFrame> mSrcPin;

    /**
     * Buf Input pin, buf is used for face detection
     */
    private SinkPin<ImgBufFrame> mImgBufSinkPin;
    private STMobileStickerNative mStStickerNative;
    private ImgTexFormat mOutFormat;
    protected int mMainSinkPinIndex;
    private String mStickerPath;
    private String mModulePath;
    private Object mBufLock = new Object();

    protected Handler mMainHandler;
    protected ImgTexFilterBase.OnErrorListener mErrorListener;
    private float[] mTexMatrix; // flip vertical matrix


    public ImgStickerProcessor(GLRender glRender) {
        mSrcPin = new SrcPin<>();
        mMainHandler = new Handler();

        mGLRender = glRender;
        mGLRender.addListener(mGLRenderListener);
        mStStickerNative = new STMobileStickerNative();
        mImgBufSinkPin = new ImgStickerBufSinkPin(0);
        mImgTexSinkPin = new ImgStickerTexSinkPin(0);
        mTexMatrix = new float[16];
        Matrix.setIdentityM(mTexMatrix, 0);
        Matrix.translateM(mTexMatrix, 0, 0, 1, 0);
        Matrix.scaleM(mTexMatrix, 0, 1, -1, 1);
    }

    public int getSinkPinNum() {
        return SINK_NUM;
    }

    protected ImgTexFormat getSrcPinFormat() {
        return mOutFormat;
    }

    protected void onFormatChanged(final int inIdx, final ImgTexFormat format) {
        mOutFormat = new ImgTexFormat(ImgTexFormat.COLOR_RGBA,
                format.width, format.height);
//        if (inIdx == mMainSinkPinIndex) {
//            getSrcPin().onFormatChanged(mOutFormat);
//        }
    }

    public void setStickerPathAndModulePath(String stickerPath, String modulePath) {
        mStickerPath = stickerPath;
        mModulePath = modulePath;
    }

    private void onGLContextReady() {
        if (mStickerInstanceCreated != 0) {
            if (mStStickerNative == null) {
                mStStickerNative = new STMobileStickerNative();
            }
            mStickerInstanceCreated = mStStickerNative.createInstance(mStickerPath, mModulePath,
                    0x0000003F);
        }
    }

    /**
     * Get source pin
     *
     * @return SrcPin object or null
     */
    public SrcPin<ImgTexFrame> getSrcPin() {
        return mSrcPin;
    }

    /**
     * Get sink pin by index
     *
     * @return SinPin object or null
     */
    public SinkPin<ImgBufFrame> getBufSinkPin() {
        return mImgBufSinkPin;
    }

    public SinkPin<ImgTexFrame> getTexSinkPin() {
        return mImgTexSinkPin;
    }

    private class ImgStickerBufSinkPin extends SinkPin<ImgBufFrame> {
        private int mIndex;

        public ImgStickerBufSinkPin(int index) {
            mIndex = index;
        }

        @Override
        public void onFormatChanged(Object format) {
//            ImgStickerProcessor.this.onFormatChanged(mIndex, (ImgBufFormat) format);
//            if (mIndex == mMainSinkPinIndex) {
//                mSrcPin.onFormatChanged(ImgStickerProcessor.this.getSrcPinFormat());
//            }
        }

        @Override
        public void onFrameAvailable(ImgBufFrame frame) {
            if (frame.buf.limit() > 0) {
                synchronized (mBufLock) {
                    if (mInputBufArray == null) {
                        mInputBufArray = new byte[frame.buf.limit()];
                    }
                    frame.buf.get(mInputBufArray);
                }
            }


            if (mIndex == mMainSinkPinIndex) {

            }
        }

        @Override
        public void onDisconnect(boolean recursive) {

        }
    }

    private class ImgStickerTexSinkPin extends SinkPin<ImgTexFrame> {
        private int mIndex;

        public ImgStickerTexSinkPin(int index) {
            mIndex = index;
        }

        @Override
        public void onFormatChanged(Object format) {
            ImgStickerProcessor.this.onFormatChanged(mIndex, (ImgTexFormat) format);
            if (mIndex == mMainSinkPinIndex) {
                mSrcPin.onFormatChanged(ImgStickerProcessor.this.getSrcPinFormat());
            }
        }

        @Override
        public void onFrameAvailable(final ImgTexFrame frame) {
            if (mSrcPin.isConnected()) {

                if (mInputBufArray != null) {
                    int dir = Accelerometer.getDirection();
                    int orientation = dir - 1;
                    if (orientation < 0) {
                        orientation = dir ^ 3;
                    }
                    if (mOutTexture == ImgTexFrame.NO_TEXTURE) {
                        mOutTexture = FboManager.getInstance()
                                .getTextureAndLock(frame.format.width, frame.format.height);
                        mOutFrameBuffer = FboManager.getInstance().getFramebuffer(mOutTexture);
                    }
                    GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, mViewPort, 0);
                    GLES20.glViewport(0, 0, frame.format.width, frame.format.height);
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mOutFrameBuffer);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                    int ret = ST_ERROR;
                    synchronized (mBufLock) {
                        if (mStStickerNative != null) {
                            ret = mStStickerNative.processTexture(frame.textureId,
                                    mInputBufArray,
                                    orientation,
                                    frame.format.width, frame.format.height, false, mOutTexture);
                        }
                    }
                    if (ret < 0) {
                        Log.e(TAG,"Sticker process failed");
                    }
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                    GLES20.glViewport(mViewPort[0], mViewPort[1], mViewPort[2], mViewPort[3]);

                    GLES20.glEnable(GL10.GL_BLEND);
                    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

                    final ImgTexFormat newFormat = new ImgTexFormat(frame.format
                            .colorFormat, frame.format.width, frame.format.height);
                    mGLRender.queueDrawFrameAppends(new Runnable() {
                        @Override
                        public void run() {
                            mSrcPin.onFrameAvailable(
                                        new ImgTexFrame(newFormat, mOutTexture, mTexMatrix,
                                                frame.pts));

                        }
                    });

                }

            }
        }

        @Override
        public void onDisconnect(boolean recursive) {
            if (recursive) {
                release();
            }
        }
    }

    private GLRender.GLRenderListener mGLRenderListener = new GLRender.GLRenderListener() {
        @Override
        public void onReady() {
            mInited = false;
            mOutTexture = ImgTexFrame.NO_TEXTURE;
            ImgStickerProcessor.this.onGLContextReady();
        }

        @Override
        public void onSizeChanged(int width, int height) {
        }

        @Override
        public void onDrawFrame() {
        }
    };

    public void release() {
        mSrcPin.disconnect(true);
        if (mOutTexture != ImgTexFrame.NO_TEXTURE) {
            FboManager.getInstance().unlock(mOutTexture);
            mOutTexture = ImgTexFrame.NO_TEXTURE;
        }

        onRelease();
        mInputBufArray = null;
        mGLRender.removeListener(mGLRenderListener);
    }

    protected void onRelease() {
        releaseSticker();
    }

    public void releaseSticker() {
        if (mStStickerNative != null) {
            mStStickerNative.destoryInstance();
            mStStickerNative = null;
            mStickerInstanceCreated = UNINIT;
    }
    }

    public void showSticker() {
        mGLRender.queueDrawFrameAppends(new Runnable() {
            @Override
            public void run() {
                if (mStickerInstanceCreated == UNINIT) {
                    if (mStStickerNative == null) {
                        mStStickerNative = new STMobileStickerNative();
                    }
                    mStickerInstanceCreated = mStStickerNative.createInstance(mStickerPath, mModulePath,
                            0x0000003F);
                }
            }
        });
    }

    public int  changeSticker(String path) {
        if (mStStickerNative != null) {
            return mStStickerNative.changeSticker(path);
        } else {
            return 0;
        }
    }

}
