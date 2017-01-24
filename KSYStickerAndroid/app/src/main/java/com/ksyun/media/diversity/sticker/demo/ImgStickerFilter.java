package com.ksyun.media.diversity.sticker.demo;


import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.ConditionVariable;
import android.os.Handler;
import android.util.Log;

import com.ksyun.media.streamer.encoder.ImgTexToBuf;
import com.ksyun.media.streamer.filter.imgtex.ImgFilterBase;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterBase;
import com.ksyun.media.streamer.framework.ImgBufFormat;
import com.ksyun.media.streamer.framework.ImgBufFrame;
import com.ksyun.media.streamer.framework.ImgTexFormat;
import com.ksyun.media.streamer.framework.ImgTexFrame;
import com.ksyun.media.streamer.framework.SinkPin;
import com.ksyun.media.streamer.framework.SrcPin;
import com.ksyun.media.streamer.util.gles.FboManager;
import com.ksyun.media.streamer.util.gles.GLRender;
import com.sensetime.sensear.SenseArActionInfo;
import com.sensetime.sensear.SenseArMaterial;
import com.sensetime.sensear.SenseArMaterialPart;
import com.sensetime.sensear.SenseArMaterialRender;

import javax.microedition.khronos.opengles.GL10;


/**
 * Created by qyvideo on 11/5/16.
 */

public class ImgStickerFilter extends ImgFilterBase {
    private static final boolean DEBUG = true;
    private final String TAG = "ImgStickerFilter";
    public static final int ERROR_UNKNOWN = -1;
    public static final int ERROR_UNSUPPORTED = -2;
    protected GLRender mGLRender;
    protected volatile boolean mInitialized;
    private ImgYFlipFilter mImgYFlipFilter;
    protected int mOutTexture = ImgTexFrame.NO_TEXTURE;
    private int mOutFrameBuffer = -1;
    private int[] mViewPort = new int[4];
    private SenseArMaterial mMaterial = null;
    private static final int SINK_NUM = 2;

    private int mTriggerTimes = 0;
    private int mActionInfo = 0;

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
    private ImgTexFormat mOutFormat;
    private SenseArMaterialRender mMaterialRender;

    private ImgTexToBuf mImgTexToBuf;

    protected Handler mMainHandler;
    protected ImgTexFilterBase.OnErrorListener mErrorListener;
    private float[] mTexMatrix; // flip vertical matrix

    private byte[] mBufArrayWithStride = null;
    private byte[] mBufArray = null;

    private ImgBufFrame mBufFrame;
    private ConditionVariable mBufferReady;
    private Object mBufLock = new Object();


    public ImgStickerFilter(GLRender glRender) {
        mSrcPin = new SrcPin<>();
        mMainHandler = new Handler();
        mBufferReady = new ConditionVariable(true);

        mGLRender = glRender;
        mGLRender.addListener(mGLRenderListener);
        mImgYFlipFilter = new ImgYFlipFilter(mGLRender);
        mMaterialRender = SenseARMaterialRenderBuilder.getInstance().getSenseArMaterialRender();
        //特殊素材需要使用,默认为false
        mMaterialRender.enablePartsFeature(true);
        mImgBufSinkPin = new ImgStickerBufSinkPin(0);
        mImgTexSinkPin = new ImgStickerTexSinkPin(0);
        mImgYFlipFilter.getSrcPin().connect(mImgTexSinkPin);
        mTexMatrix = new float[16];
        Matrix.setIdentityM(mTexMatrix, 0);
        Matrix.translateM(mTexMatrix, 0, 0, 1, 0);
        Matrix.scaleM(mTexMatrix, 0, 1, -1, 1);
        mImgTexToBuf = new ImgTexToBuf(glRender);
//        mImgTexToBuf.setErrorListener(new ImgTexToBuf.ErrorListener() {
//            @Override
//            public void onError(ImgTexToBuf imgTexToBuf, int err) {
//                if (mErrorListener != null) {
//                    int errno = ERROR_UNKNOWN;
//                    if (err == ImgTexToBuf.ERROR_UNSUPPORTED) {
//                        errno = ERROR_UNSUPPORTED;
//                        //should do roll back here
//                    }
//                }
//            }
//        });
        mImgTexToBuf.setOutputColorFormat(ImgBufFormat.FMT_RGBA);
        mImgTexToBuf.mSrcPin.connect(mImgBufSinkPin);

    }

    public int getSinkPinNum() {
        return SINK_NUM;
    }

    @Override
    public SinkPin<ImgTexFrame> getSinkPin(int idx) {
        return mImgYFlipFilter.getSinkPin();
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


    private void onGLContextReady() {
        if (mMaterialRender != null) {
            mGLRender.queueEvent(new Runnable() {
                @Override
                public void run() {
                    mMaterialRender.initGLResource();
                    setEffectParams(4.0f/7.0f);
                }
            });
        }
        mInitialized = true;
    }

    /**
     * Get source pin
     *
     * @return SrcPin object or null
     */
    public SrcPin<ImgTexFrame> getSrcPin() {
        return mSrcPin;
    }

    private class ImgStickerBufSinkPin extends SinkPin<ImgBufFrame> {
        private int mIndex;

        public ImgStickerBufSinkPin(int index) {
            mIndex = index;
        }

        @Override
        public void onFormatChanged(Object format) {
//            mImgTexToBuf.mSinkPin.onFormatChanged(format);
//            ImgStickerFilter.this.onFormatChanged(mIndex, (ImgBufFormat) format);
//            if (mIndex == mMainSinkPinIndex) {
//                mSrcPin.onFormatChanged(ImgStickerFilter.this.getSrcPinFormat());
//            }
        }

        @Override
        public void onFrameAvailable(ImgBufFrame frame) {
            synchronized (mBufLock) {
                mBufFrame = frame;
                if(mBufArrayWithStride == null) {
                    mBufArrayWithStride = new byte[mBufFrame.buf.capacity()];
                }
                if(mBufArray == null) {
                    mBufArray = new byte[4 * mBufFrame.format.width * mBufFrame.format.height];
                }
                mBufFrame.buf.get(mBufArrayWithStride);

                //flip y
                int height = mBufArrayWithStride.length / frame.format.stride[0];
                for(int i = height - 1; i >= 0; i--) {
                    System.arraycopy(mBufArrayWithStride, i * frame
                            .format.stride[0], mBufArray, (height - i - 1) * 4 * mBufFrame.format
                            .width, 4 * mBufFrame.format.width);
                }
                mBufFrame.buf.rewind();
            }
            mBufferReady.open();
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
            if(mImgTexToBuf != null) {
                mImgTexToBuf.mSinkPin.onFormatChanged(format);
            }
            ImgStickerFilter.this.onFormatChanged(mIndex, (ImgTexFormat) format);
        }

        @Override
        public void onFrameAvailable(final ImgTexFrame frame) {
            if (mSrcPin.isConnected()) {
                mBufferReady.close();
                mImgTexToBuf.mSinkPin.onFrameAvailable(frame);
                mBufferReady.block();
                synchronized (mBufLock) {
                    if (mBufArray != null) {

                        if (mOutTexture == ImgTexFrame.NO_TEXTURE) {
                            mOutTexture = FboManager.getInstance()
                                    .getTextureAndLock(frame.format.width, frame.format.height);
                            mOutFrameBuffer = FboManager.getInstance().getFramebuffer(mOutTexture);
                        }
                        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, mViewPort, 0);
                        GLES20.glViewport(0, 0, frame.format.width, frame.format.height);
                        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mOutFrameBuffer);
                        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                        if (mMaterialRender != null) {
                            mMaterialRender.setFrameSize(frame.format.width, frame.format.height);
                            byte[] renderInfo = mMaterialRender.generateBeautyAndRenderInfo(mBufArray,
                                    SenseArMaterialRender.SenseArImageFormat.ST_PIX_FMT_RGBA8888, getCurrentOrientation(), false,
                                    mMaterial, frame.textureId, 0, null,
                                    SenseArMaterialRender.SenseArImageFormat.ST_PIX_FMT_NV21);
                            if (renderInfo != null && renderInfo.length > 0) {
                                long startTime = System.currentTimeMillis();
                                SenseArMaterialRender.RenderStatus status = mMaterialRender.renderMaterial
                                        (frame.textureId, renderInfo,
                                                mOutTexture,
                                                null, SenseArMaterialRender.SenseArImageFormat.ST_PIX_FMT_NV21);
                                if(DEBUG){
                                    Log.d(TAG, "the result11 of renderMaterial is " + status);
                                    Log.d(TAG, "sticker cost: " + (System.currentTimeMillis() - startTime));
                                }

                                //根据特殊素材id获取素材parts,根据parts属性和trigger action设置现显示不同parts组合
                                if (mMaterial != null) {
                                    //爱心素材
                                    if (mMaterial.id.equals("20170109124245233850861")) {
                                        showMaterialParts("20170109124245233850861");
                                        //可乐素材
                                    } else if (mMaterial.id.equals("20170109124355279333705")) {
                                        showMaterialParts("20170109124355279333705");
                                    }
                                }

                                if (status == SenseArMaterialRender.RenderStatus.RENDER_SUCCESS) {

                                }
                            }
                        }
                    }

                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                    GLES20.glViewport(mViewPort[0], mViewPort[1], mViewPort[2], mViewPort[3]);

                    GLES20.glEnable(GL10.GL_BLEND);
                    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

                    mGLRender.queueDrawFrameAppends(new Runnable() {
                        @Override
                        public void run() {
                            mSrcPin.onFrameAvailable(
                                    new ImgTexFrame(frame.format, mOutTexture,
                                            mTexMatrix,
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
            mInitialized = false;
            mOutTexture = ImgTexFrame.NO_TEXTURE;
            ImgStickerFilter.this.onGLContextReady();
        }

        @Override
        public void onSizeChanged(int width, int height) {
        }

        @Override
        public void onDrawFrame() {
        }

        @Override
        public void onReleased() {
            if (mOutTexture != ImgTexFrame.NO_TEXTURE) {
                FboManager.getInstance().unlock(mOutTexture);
                mOutTexture = ImgTexFrame.NO_TEXTURE;
            }
            mMaterialRender.releaseGLResource();
            mBufArrayWithStride = null;
            mBufArray = null;
            mBufFrame = null;
            mInitialized = false;
        }
    };

    private void release() {
        mSrcPin.disconnect(true);
        if (mOutTexture != ImgTexFrame.NO_TEXTURE) {
            FboManager.getInstance().unlock(mOutTexture);
            mOutTexture = ImgTexFrame.NO_TEXTURE;
        }

        onRelease();
        mGLRender.removeListener(mGLRenderListener);
    }

    protected void onRelease() {
        releaseSticker();
    }


    public void startShowSticker(final SenseArMaterial material) {
        mMaterial = material;
        if (!mInitialized && mMaterialRender != null) {
            mGLRender.queueEvent(new Runnable() {
                @Override
                public void run() {
                    mMaterialRender.initGLResource();
                    setEffectParams(4.0f/7.0f);
                    mInitialized = true;
                }
            });
        }
    }

    public void releaseSticker() {
        if (mMaterialRender != null) {
            mGLRender.queueEvent(new Runnable() {
                @Override
                public void run() {
                    mMaterialRender.releaseGLResource();
                }
            });
        }
    }

    /**
     * 设置美颜参数
     * @param paramValue 美颜参数值
     */
    public void setEffectParams(float paramValue)
    {
        if (mMaterialRender != null) {
            mMaterialRender.setParam(SenseArMaterialRender.SenseArParamType.ST_AR_BEAUTIFY_CONTRAST_STRENGTH, 5/7f);
            mMaterialRender.setParam(SenseArMaterialRender.SenseArParamType.ST_AR_BEAUTIFY_SMOOTH_STRENGTH, 5/7f);
            mMaterialRender.setParam(SenseArMaterialRender.SenseArParamType.ST_AR_BEAUTIFY_WHITEN_STRENGTH, 0);
            mMaterialRender.setParam(SenseArMaterialRender.SenseArParamType.ST_AR_MORPH_SHRINK_FACE_RATIO, 0.11f);
            mMaterialRender.setParam(SenseArMaterialRender.SenseArParamType.ST_AR_MORPH_ENLARGE_EYE_RATIO, 0.17f);
            mMaterialRender.setParam(SenseArMaterialRender.SenseArParamType.ST_AR_MORPH_SHRINK_JAW_RATIO, 0.2f);
        }
    }

    /**
     * 获取设备Orientation信息
     */
    private int getCurrentOrientation() {
        int dir = Accelerometer.getDirection();
        int orientation = dir -1;
        if(orientation < 0){
            orientation = dir ^ 3;
        }

        return orientation;
    }

    /**
     * 特殊素材显示不同parts组合
     * @param materialId 素材id
     */
    private void showMaterialParts(String materialId){
        SenseArMaterialPart[] partsInfo;// = new SenseArMaterialPart[10];
        partsInfo = mMaterialRender.getCurrentMaterialPartsInfo();

        if(partsInfo==null || partsInfo.length==0)
        {
            return;
        }

//        爱心素材默认信息, parts num = 5
//        parts info: ear    0  true  1
//        parts info: face   1  true  1
//        parts info: pink   2  true  16384
//        parts info: purple 3  false 16384
//        parts info: yellow 5  false 16384
//
//        饮料素材默认信息, parts num = 5
//        parts info: face      0  true  1
//        parts info: head      1  true  1
//        parts info: cocacolab 2  true  4096
//        parts info: jdba      3  false 4096
//        parts info: milk      4  false 4096

        SenseArActionInfo actionInfo = new SenseArActionInfo();

        //int[] action = new int[5];

        if(partsInfo.length>=5)
        {
            partsInfo[0].partEnable = true;
            partsInfo[1].partEnable = true;
            if(mTriggerTimes == 0){
                partsInfo[2].partEnable = true;
                partsInfo[3].partEnable = false;
                partsInfo[4].partEnable = false;
            }else if(mTriggerTimes == 1){
                partsInfo[2].partEnable = false;
                partsInfo[3].partEnable = true;
                partsInfo[4].partEnable = false;
            }else if(mTriggerTimes == 2){
                partsInfo[2].partEnable = false;
                partsInfo[3].partEnable = false;
                partsInfo[4].partEnable = true;
            }
        }

        //若素材支持为爱心素材，支持爱心手势trigger
        if(materialId.equals("20170109124245233850861")){

            if(mActionInfo != SenseArMaterialRender.ST_MOBILE_HAND_LOVE){
                actionInfo= mMaterialRender.getCurrentFrameActionInfo();
                Log.d(TAG, "action info " + actionInfo.handAction[0] );
                if(actionInfo.handAction[0] == SenseArMaterialRender.ST_MOBILE_HAND_LOVE){

                    mActionInfo = SenseArMaterialRender.ST_MOBILE_HAND_LOVE;
                    mTriggerTimes++;
                    if(mTriggerTimes > 2){
                        mTriggerTimes = 0;
                    }
                }
            }else if(mActionInfo == SenseArMaterialRender.ST_MOBILE_HAND_LOVE){
                actionInfo= mMaterialRender.getCurrentFrameActionInfo();
                if(actionInfo.handAction[0] == 0){
                    int b = mMaterialRender.enableMaterialParts(partsInfo);
                    mActionInfo = 0;
                }
            }

            //若素材为饮料素材,支持手掌trigger
        }else if(materialId.equals("20170109124355279333705")){
            if(mActionInfo != SenseArMaterialRender.ST_MOBILE_HAND_PALM){
                actionInfo= mMaterialRender.getCurrentFrameActionInfo();
                Log.d(TAG, "action info " + actionInfo.handAction[0] );
                if(actionInfo.handAction[0] == SenseArMaterialRender.ST_MOBILE_HAND_PALM){

                    mActionInfo = SenseArMaterialRender.ST_MOBILE_HAND_PALM;
                    mTriggerTimes++;
                    if(mTriggerTimes > 2){
                        mTriggerTimes = 0;
                    }
                }
            }else if(mActionInfo == SenseArMaterialRender.ST_MOBILE_HAND_PALM){
                actionInfo= mMaterialRender.getCurrentFrameActionInfo();
                if(actionInfo.handAction[0] == 0){
                    int b = mMaterialRender.enableMaterialParts(partsInfo);
                    mActionInfo = 0;
                }
            }
        }
    }

}
