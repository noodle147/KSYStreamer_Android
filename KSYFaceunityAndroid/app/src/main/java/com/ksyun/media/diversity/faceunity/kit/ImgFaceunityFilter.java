package com.ksyun.media.diversity.faceunity.kit;

import com.faceunity.wrapper.faceunity;
import com.ksyun.media.streamer.encoder.ImgTexToBuf;
import com.ksyun.media.streamer.filter.imgbuf.ImgBufScaleFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgFilterBase;
import com.ksyun.media.streamer.framework.ImgBufFormat;
import com.ksyun.media.streamer.framework.ImgBufFrame;
import com.ksyun.media.streamer.framework.ImgTexFrame;
import com.ksyun.media.streamer.framework.SinkPin;
import com.ksyun.media.streamer.framework.SrcPin;
import com.ksyun.media.streamer.kit.KSYStreamer;
import com.ksyun.media.streamer.util.gles.FboManager;
import com.ksyun.media.streamer.util.gles.GLRender;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.os.ConditionVariable;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.opengles.GL10;

/**
 * draw face unity prop&beauty
 */

public class ImgFaceunityFilter extends ImgFilterBase {
    private final static String TAG = "ImgFaceunityFilter";

    private final static String VERSION = "1.0.7.0";

    private final static String PROP_TYPE_BEAGLEDOG = "Faceunity/BeagleDog.mp3";
    private final static String PROP_TYPE_COLORGROWN = "Faceunity/ColorCrown.mp3";
    private final static String PROP_TYPE_DEER = "Faceunity/Deer.mp3";

    private final static String PROP_TYPE_HAPPYRABBI = "Faceunity/HappyRabbi.mp3";
    private final static String PROP_TYPE_HARTSHORN = "Faceunity/hartshorn.mp3";
    private final static String PROP_TYPE_ITEM0204 = "Faceunity/item0204.mp3";
    private final static String PROP_TYPE_ITEM0208 = "Faceunity/item0208.mp3";
    private final static String PROP_TYPE_ITEM0210 = "Faceunity/item0210.mp3";
    private final static String PROP_TYPE_ITEM0501 = "Faceunity/item0501.mp3";
    private final static String PROP_TYPE_MOOD = "Faceunity/Mood.mp3";
    private final static String PROP_TYPE_PINCESSCROWN = "Faceunity/PrincessCrown.mp3";
    private final static String PROP_TYPE_TIARA = "Faceunity/tiara.mp3";
    private final static String PROP_TYPE_YELLLOWEAR = "Faceunity/YellowEar.mp3";

    private final static String GESTURE_TYPE_HEART = "Faceunity/heart.mp3";

    private final static String BEAUTY_TYPE_NATURE = "nature";
    private final static String BEAUTY_TYPE_DELTA = "delta";
    private final static String BEAUTY_TYPE_ELECTRIC = "electric";
    private final static String BEAUTY_TYPE_SLOWLIVED = "slowlived";
    private final static String BEAUTY_TYPE_TOKYO = "tokyo";
    private final static String BEAUTY_TYPE_WARM = "warm";

    private final static String[] PROPS = {
            PROP_TYPE_BEAGLEDOG,
            PROP_TYPE_COLORGROWN,
            PROP_TYPE_DEER,
            PROP_TYPE_HAPPYRABBI,
            PROP_TYPE_HARTSHORN,
            PROP_TYPE_ITEM0204,
            PROP_TYPE_ITEM0208,
            PROP_TYPE_ITEM0210,
            PROP_TYPE_ITEM0501,
            PROP_TYPE_MOOD,
            PROP_TYPE_PINCESSCROWN,
            PROP_TYPE_TIARA,
            PROP_TYPE_YELLLOWEAR
    };

    private final static String[] GESTURES = {
            GESTURE_TYPE_HEART
    };

    private final static String[] BEAUTYS = {
            BEAUTY_TYPE_NATURE,
            BEAUTY_TYPE_DELTA,
            BEAUTY_TYPE_ELECTRIC,
            BEAUTY_TYPE_SLOWLIVED,
            BEAUTY_TYPE_TOKYO,
            BEAUTY_TYPE_WARM
    };

    private static final int SINK_NUM = 2;
    public static final int ERROR_UNKNOWN = -1;
    public static final int ERROR_UNSUPPORTED = -2;

    private GLRender mGLRender;
    private SinkPin<ImgTexFrame> mTexSrcSinkPin;
    private SinkPin<ImgTexFrame> mTexSinkPin;
    private SinkPin<ImgBufFrame> mBufSinkPin;
    private SrcPin<ImgTexFrame> mSrcPin;

    public ImgTexToBuf mImgTexToBuf;
    private ImgYFlipFilter mImgYFlipFilter;
    private ImgBufScaleFilter mFaceunityScale;

    private boolean mInited = false;
    private int mFrameID;
    private int[] m_items = new int[3];
    private byte[] mInputBufArray = null;
    private byte[] mFaceuniytBufArray = null;
    private float[] mTexMatrix; // flip vertical matrix
    private Object BUF_LOCK = new Object();
    private ConditionVariable mBufferReady;

    private String mPropPath;
    private String mCurrentPropPath;
    private String mGesturePath;
    private String mCurrentGesturePath;
    private String mBeautyType;
    private String mCurrentBeautyType;
    public double mBeautyColorLevel = 1.0;
    public int mBeautyBlurLevel = 3;
    public double mBeautyCheekLevel = 0;
    public double mBeautyEyeLevel = 0;

    private Context mContext;
    private KSYStreamer mKSYStreamer;
    private int mOutTexture = ImgTexFrame.NO_TEXTURE;
    private int mOutFrameBuffer = -1;
    private int[] mViewPort = new int[4];

    public ImgFaceunityFilter(Context context, GLRender glRender, KSYStreamer kit) {
        mContext = context;
        mKSYStreamer = kit;
        mGLRender = glRender;
        mFrameID = 0;
        mBufferReady = new ConditionVariable(true);
        mTexSrcSinkPin = new FaceunitySrcTexSinkPin();
        mTexSinkPin = new FaceunityTexSinkPin();
        mBufSinkPin = new FaceunityBufSinkPin();
        mSrcPin = new SrcPin<>();

        mImgYFlipFilter = new ImgYFlipFilter(mGLRender);
        mImgYFlipFilter.getSrcPin().connect(mTexSinkPin);

        mTexMatrix = new float[16];
        Matrix.setIdentityM(mTexMatrix, 0);
        Matrix.translateM(mTexMatrix, 0, 0, 1, 0);
        Matrix.scaleM(mTexMatrix, 0, 1, -1, 1);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            mFaceunityScale = new ImgBufScaleFilter();
            mFaceunityScale.getSrcPin().connect(mBufSinkPin);
            mKSYStreamer.getCameraCapture().mImgBufSrcPin.connect(mFaceunityScale.getSinkPin());
        } else {
            mImgTexToBuf = new ImgTexToBuf(glRender);
            mImgTexToBuf.setErrorListener(new ImgTexToBuf.ErrorListener() {
                @Override
                public void onError(ImgTexToBuf imgTexToBuf, int err) {
                    if (mErrorListener != null) {
                        int errno = ERROR_UNKNOWN;
                        if (err == ImgTexToBuf.ERROR_UNSUPPORTED) {
                            errno = ERROR_UNSUPPORTED;
                            //should do roll back here
                        }
                    }
                }
            });
            mImgTexToBuf.setOutputColorFormat(ImgBufFormat.FMT_I420);
            mImgTexToBuf.mSrcPin.connect(mBufSinkPin);
        }
    }

    public void onPause() {
        mGLRender.queueDrawFrameAppends(new Runnable() {
            public void run() {
                if (m_items[2] != 0) {
                    faceunity.fuDestroyItem(m_items[2]);
                    m_items[2] = 0;
                }
                faceunity.fuOnDeviceLost();
                mFrameID = 0;
            }
        });
    }

    @Override
    public int getSinkPinNum() {
        return SINK_NUM;
    }

    @Override
    public SinkPin<ImgTexFrame> getSinkPin(int i) {
        return mTexSrcSinkPin;
    }

    public SrcPin<ImgTexFrame> getSrcPin() {
        return mSrcPin;
    }

    public void setMirror(boolean isMirror) {
        if (mFaceunityScale != null) {
            mFaceunityScale.setMirror(isMirror);
        }
    }

    public void setTargetSize(int targetWith, int targetHeight) {
        if (mFaceunityScale != null) {
            mFaceunityScale.setTargetSize(targetWith, targetHeight);
        }
    }

    /**
     * 0~12 使用贴纸，其它不使用贴纸
     *
     * @param index
     */
    public void setPropType(int index) {
        if (index >= PROPS.length || index < 0) {
            mPropPath = null;
            return;
        }

        mPropPath = PROPS[index];
    }

    public void setGestureType(int index) {
        if (index >= GESTURES.length || index < 0) {
            mGesturePath = null;
            return;
        }

        mGesturePath = GESTURES[index];
    }

    /**
     * 0~5 使用美颜，其它不使用美颜
     *
     * @param type
     */
    public void setBeautyType(int type) {
        if (type > 5 || type < 0) {
            mBeautyType = null;
            return;
        }

        mBeautyType = BEAUTYS[type];
    }

    public double getBeautyColorLevel() {
        return mBeautyColorLevel;
    }

    /**
     * 美白级别 1.0 是默认值
     *
     * @param colorLevel
     */
    public void setBeautyColorLevel(double colorLevel) {
        mBeautyColorLevel = colorLevel;
    }

    /**
     * 磨皮级别 3.0 是默认值，取值范围0-5
     * 中等磨皮可以设置为 3.0 ，重度磨皮可以设置为 5.0
     *
     * @param blurLevel
     */
    public void setBeautyBlurLevel(int blurLevel) {
        mBeautyBlurLevel = blurLevel;
    }

    /**
     * 设置瘦脸级别，默认为0
     * 0为关闭效果，1为默认效果，大于1为进一步增强效果
     *
     * @param cheekLevel
     */
    public void setBeautyCheekLevel(double cheekLevel) {
        mBeautyCheekLevel = cheekLevel;
    }

    public double getBeautyCheekLevel() {
        return mBeautyCheekLevel;
    }

    public double getBeautyEyeLevel() {
        return mBeautyEyeLevel;
    }

    /**
     * 设置大眼级别
     * 0为关闭效果，1为默认效果，大于1为进一步增强效果
     *
     * @param eyeLevel
     */
    public void setBeautyEyeLevel(double eyeLevel) {
        mBeautyEyeLevel = eyeLevel;
    }

    public double getBeautyBlurLvevl() {
        return mBeautyBlurLevel;
    }

    public void release() {
        if (mOutTexture != ImgTexFrame.NO_TEXTURE) {
            mGLRender.getFboManager().unlock(mOutTexture);
            mOutTexture = ImgTexFrame.NO_TEXTURE;
        }
        mInputBufArray = null;
    }


    private void onGLContextReady() {
        try {
            InputStream is = mContext.getAssets().open("Faceunity/v3.mp3");
            byte[] v3data = new byte[is.available()];
            is.read(v3data);
            is.close();
            /**
             * fuSetup parameter explanation
             * @param v3data
             * @param null, old parameter, consider removed in the future
             * @param authpack.A(), auth key byte array content
             **/
            faceunity.fuSetup(v3data, null, authpack.A());
            mInited = true;
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e);
        }
    }

    private class FaceunitySrcTexSinkPin extends SinkPin<ImgTexFrame> {
        @Override
        public void onFormatChanged(Object format) {
            if (!mInited) {
                mOutTexture = ImgTexFrame.NO_TEXTURE;
                ImgFaceunityFilter.this.onGLContextReady();
                mInited = true;
            }

            if (mImgTexToBuf != null) {
                mImgTexToBuf.mSinkPin.onFormatChanged(format);
            }

            if (mImgYFlipFilter != null) {
                mImgYFlipFilter.getSinkPin().onFormatChanged(format);
            }
        }

        @Override
        public void onFrameAvailable(ImgTexFrame frame) {
            if (mSrcPin.isConnected()) {
                if (mImgTexToBuf != null) {
                    mBufferReady.close();
                    mImgTexToBuf.mSinkPin.onFrameAvailable(frame);
                    mBufferReady.block();
                }

                mImgYFlipFilter.getSinkPin().onFrameAvailable(frame);
            }
        }

        @Override
        public void onDisconnect(boolean recursive) {
            mImgYFlipFilter.getSinkPin().onDisconnect(recursive);
        }
    }

    /**
     * 接收视频数据并添加faceunity贴纸或者美颜
     */
    private class FaceunityTexSinkPin extends SinkPin<ImgTexFrame> {
        @Override
        public void onFormatChanged(Object format) {
            if (!mInited) {
                mOutTexture = ImgTexFrame.NO_TEXTURE;
                ImgFaceunityFilter.this.onGLContextReady();
                mInited = true;
            }
            mSrcPin.onFormatChanged(format);
        }

        @Override
        public void onFrameAvailable(ImgTexFrame frame) {
            if (mSrcPin.isConnected()) {
                int isTracking = faceunity.fuIsTracking();
                if (isTracking == 0) {
                    //人脸检测状态，为0代表当前画面中未检测到人脸
                }

                //没有原始视频buf数据，或者不需要显示贴纸时，直接返回输入的textureid
                synchronized (BUF_LOCK) {
                    if (mInputBufArray != null) {
                        byte[] array = mInputBufArray;
                        if (mFaceunityScale != null) {
                            if (mFaceuniytBufArray == null) {
                                mFaceuniytBufArray = new byte[mInputBufArray.length];
                            }
                            System.arraycopy(mInputBufArray, 0, mFaceuniytBufArray, 0, mInputBufArray.length);
                            array = mFaceuniytBufArray;
                        }

                        //设置系统时android
                        faceunity.fuItemSetParam(m_items[0], "isAndroid", 1.0);
                        if (!TextUtils.isEmpty(mPropPath)) {
                            createPropItem();
                        } else {
                            if (m_items[0] != 0) {
                                faceunity.fuDestroyItem(m_items[0]);
                                m_items[0] = 0;
                            }
                        }

                        if (!TextUtils.isEmpty(mBeautyType)) {
                            createBeautyItem();
                        } else {
                            if (m_items[1] != 0) {
                                faceunity.fuDestroyItem(m_items[1]);
                                m_items[1] = 0;
                            }
                        }

                        if (!TextUtils.isEmpty(mGesturePath)) {
                            createGestureItem();
                        } else {
                            if (m_items[2] != 0) {
                                faceunity.fuDestroyItem(m_items[2]);
                                m_items[2] = 0;
                            }
                        }

                        if (m_items[0] != 0 || m_items[1] != 0 || m_items[2] != 0) {
                            if (mOutTexture == ImgTexFrame.NO_TEXTURE) {
                                mOutTexture = mGLRender.getFboManager()
                                        .getTextureAndLock(frame.format.width, frame.format.height);
                                mOutFrameBuffer = mGLRender.getFboManager().getFramebuffer(mOutTexture);
                            }
                            GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, mViewPort, 0);
                            GLES20.glViewport(0, 0, frame.format.width, frame.format.height);
                            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mOutFrameBuffer);
                            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);


                            mOutTexture = faceunity.fuDualInputToTexture(array, frame.textureId, 0,
                                    frame.format.width, frame.format.height, mFrameID++, m_items);
                            //for test buf
//                            mOutTexture = faceunity.fuRenderToI420Image(mInputBufArray,
//                                    frame.format.width, frame.format.height, mFrameID++, new int[]{0});

                            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                            GLES20.glViewport(mViewPort[0], mViewPort[1], mViewPort[2], mViewPort[3]);

                            GLES20.glEnable(GL10.GL_BLEND);
                            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                        } else {
                            mOutTexture = frame.textureId;
                        }

                    } else {
                        mOutTexture = frame.textureId;
                    }
                }

                ImgTexFrame outFrame = new ImgTexFrame(frame.format, mOutTexture,
                        mTexMatrix,
                        frame.pts);

                mSrcPin.onFrameAvailable(outFrame);
            }
        }

        @Override
        public void onDisconnect(boolean recursive) {
            if (recursive) {
                faceunity.fuOnDeviceLost();
                mFrameID = 0;
                mInited = false;
                release();
            }
        }
    }

    private class FaceunityBufSinkPin extends SinkPin<ImgBufFrame> {

        @Override
        public void onFormatChanged(Object format) {

        }

        @Override
        public void onFrameAvailable(ImgBufFrame frame) {
            if (mSrcPin.isConnected()) {
                if (frame.buf.limit() > 0) {
                    synchronized (BUF_LOCK) {
                        if (mInputBufArray == null || (mInputBufArray != null && mInputBufArray
                                .length != frame.buf.limit())) {
                            mInputBufArray = new byte[frame.buf.limit()];
                        }
                        frame.buf.get(mInputBufArray);
                    }
                    if(mImgTexToBuf != null) {
                        mBufferReady.open();
                    }
                }
            }
        }

        @Override
        public void onDisconnect(boolean recursive) {

        }

    }

    private void createPropItem() {
        //第一次显示贴纸，给mCurrentPropPath赋值
        if (TextUtils.isEmpty(mCurrentPropPath)) {
            mCurrentPropPath = mPropPath;
        }
        //切换贴纸时，释放上一次的贴纸
        if (!mCurrentPropPath.equals(mPropPath) && m_items[0] != 0) {
            faceunity.fuDestroyItem(m_items[0]);
            m_items[0] = 0;
        }
        mCurrentPropPath = mPropPath;
        //创建贴纸
        if (m_items[0] == 0) {
            try {
                InputStream is = ImgFaceunityFilter.this.mContext.getAssets().open
                        (mCurrentPropPath);
                byte[] item_data = new byte[is.available()];
                is.read(item_data);
                is.close();
                m_items[0] = faceunity.fuCreateItemFromPackage(item_data);
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e);
            }
        }
    }

    private void createGestureItem() {
        //第一次显示手势，给mCurrentGesturePath赋值
        if (TextUtils.isEmpty(mCurrentGesturePath)) {
            mCurrentGesturePath = mGesturePath;
        }
        //切换贴纸时，释放上一次的贴纸
        if (!mCurrentGesturePath.equals(mGesturePath) && m_items[2] != 0) {
            faceunity.fuDestroyItem(m_items[2]);
            m_items[2] = 0;
        }
        mCurrentGesturePath = mGesturePath;
        //创建贴纸
        if (m_items[2] == 0) {
            try {
                InputStream is = ImgFaceunityFilter.this.mContext.getAssets().open
                        (mCurrentGesturePath);
                byte[] item_data = new byte[is.available()];
                is.read(item_data);
                is.close();
                m_items[2] = faceunity.fuCreateItemFromPackage(item_data);
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e);
            }
        }
    }

    private void createBeautyItem() {
        //创建美颜
        if (m_items[1] == 0) {
            try {
                InputStream is = ImgFaceunityFilter.this.mContext.getAssets().open
                        ("Faceunity/face_beautification.mp3");
                byte[] item_data = new byte[is.available()];
                is.read(item_data);
                is.close();
                m_items[1] = faceunity.fuCreateItemFromPackage(item_data);
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e);
            }
        }

        //第一次开启美颜，mCurrentBeautyType
        mCurrentBeautyType = mBeautyType;
        faceunity.fuItemSetParam(m_items[1], "filter_name", mCurrentBeautyType);
        faceunity.fuItemSetParam(m_items[1], "blur_level", mBeautyBlurLevel);
        faceunity.fuItemSetParam(m_items[1], "color_level", mBeautyColorLevel);
        faceunity.fuItemSetParam(m_items[1], "cheek_thinning", mBeautyCheekLevel);
        faceunity.fuItemSetParam(m_items[1], "eye_enlarging", mBeautyEyeLevel);
    }
}
