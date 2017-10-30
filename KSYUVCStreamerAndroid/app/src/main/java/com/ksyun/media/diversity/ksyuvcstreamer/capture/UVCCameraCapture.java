package com.ksyun.media.diversity.ksyuvcstreamer.capture;

import android.graphics.SurfaceTexture;
import android.opengl.Matrix;
import android.util.Log;

import com.ksyun.media.streamer.framework.ImgTexFormat;
import com.ksyun.media.streamer.framework.ImgTexFrame;
import com.ksyun.media.streamer.framework.SrcPin;
import com.ksyun.media.streamer.util.gles.GLRender;
import com.ksyun.media.streamer.util.gles.GlUtil;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import java.util.List;
import java.util.Locale;

/**
 * Capture UVCCamera frame.
 */

public class UVCCameraCapture implements SurfaceTexture.OnFrameAvailableListener {
    private final static String TAG = "UVCCameraCapture";
    private final static boolean VERBOSE = true;
    private final static boolean TRACE_FPS_LIMIT = false;

    /**
     * Source pin transfer ImgTexFrame, used for gpu path and preview
     */
    private SrcPin<ImgTexFrame> mImgTexSrcPin;

    private UVCCamera mUVCCamera;
    private GLRender mGLRender;
    private int mTextureId;
    private SurfaceTexture mSurfaceTexture;
    private boolean mTexInited = false;
    private boolean mFrameAvailable = false;
    private ImgTexFormat mImgTexFormat;
    private final Object mCameraLock = new Object();

    private int mPresetPreviewWidth = 1280;
    private int mPresetPreviewHeight = 720;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private float mPresetPreviewFps = 15.0f;
    private int mOrientationDegrees = 0;

    // Performance trace
    private float mCurrentFps;
    private long mLastTraceTime;
    private long mFrameDrawn;

    public UVCCameraCapture(GLRender glRender) {
        mGLRender = glRender;

        mImgTexSrcPin = new SrcPin<>();
        mGLRender.addListener(mGLRenderListener);
    }

    public SrcPin<ImgTexFrame> getImgTexSrcPin() {
        return mImgTexSrcPin;
    }

    public void setPreviewSize(int width, int height) {
        if (width > height) {
            mPresetPreviewWidth = width;
            mPresetPreviewHeight = height;
        } else {
            //noinspection SuspiciousNameCombination
            mPresetPreviewWidth = height;
            //noinspection SuspiciousNameCombination
            mPresetPreviewHeight = width;
        }
    }

    public void setPreviewFps(float fps) {
        mPresetPreviewFps = fps;
    }

    public void setOrientation(int degrees) {
        if (mOrientationDegrees == degrees) {
            return;
        }

        mOrientationDegrees = degrees;
        // trigger format changed event
        mTexInited = false;
    }

    public float getCurrentPreviewFps() {
        return mCurrentFps;
    }

    public void open(final USBMonitor.UsbControlBlock ctrlBlock) {
        synchronized (mCameraLock) {
            if (mUVCCamera != null) {
                mUVCCamera.destroy();
            }
            mUVCCamera = new UVCCamera();
            mUVCCamera.open(ctrlBlock);
        }
    }

    public void close() {
        synchronized (mCameraLock) {
            if (mUVCCamera != null) {
                mUVCCamera.destroy();
                mUVCCamera = null;
            }
            mTexInited = false;
        }
    }

    public void start() {
        synchronized (mCameraLock) {
            if (mUVCCamera == null) {
                return;
            }
            setCameraParameters();
            if (mSurfaceTexture != null) {
                mUVCCamera.setPreviewTexture(mSurfaceTexture);
                mUVCCamera.startPreview();
            }
        }
    }

    public void stop() {
        synchronized (mCameraLock) {
            if (mUVCCamera == null) {
                return;
            }
            mUVCCamera.stopPreview();
        }
        mTexInited = false;
    }

    public void release() {
        close();

        mImgTexSrcPin.disconnect(true);
        mGLRender.removeListener(mGLRenderListener);
    }

    private void setCameraParameters() {
        List<Size> sizes = mUVCCamera.getSupportedSizeList(UVCCamera.FRAME_FORMAT_MJPEG);
        calSupportedSize(sizes);
        try {
            mUVCCamera.setPreviewSize(mPreviewWidth, mPreviewHeight,
                    UVCCamera.DEFAULT_PREVIEW_MIN_FPS, (int) mPresetPreviewFps,
                    UVCCamera.FRAME_FORMAT_MJPEG, 1.0f);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "MJPEG mode not supported, fallback to YUYV mode");
            sizes = mUVCCamera.getSupportedSizeList(UVCCamera.FRAME_FORMAT_YUYV);
            calSupportedSize(sizes);
            try {
                mUVCCamera.setPreviewSize(mPreviewWidth, mPreviewHeight,
                        UVCCamera.DEFAULT_PREVIEW_MIN_FPS, (int) mPresetPreviewFps,
                        UVCCamera.FRAME_FORMAT_YUYV, 1.0f);
            } catch (IllegalArgumentException e1) {
                e.printStackTrace();
                mUVCCamera.destroy();
                mUVCCamera = null;
            }
        }
    }

    private void calSupportedSize(List<Size> sizes) {
        int targetWidth = 0, targetHeight = 0;
        int secondWidth = 0, secondHeight = 0;
        int offset = Integer.MAX_VALUE;
        int secondOffset = Integer.MAX_VALUE;
        for (Size size : sizes) {
            Log.d(TAG, "==== Camera Support: " + size.width + "x" + size.height);
            int off = (size.width - mPresetPreviewWidth) * (size.width - mPresetPreviewWidth) +
                    (size.height - mPresetPreviewHeight) * (size.height - mPresetPreviewHeight);
            if (off < secondOffset) {
                secondWidth = size.width;
                secondHeight = size.height;
                secondOffset = off;
            }
            if (size.width >= mPresetPreviewWidth && size.height >= mPresetPreviewHeight) {
                if (off < offset) {
                    targetWidth = size.width;
                    targetHeight = size.height;
                    offset = off;
                }
            }
        }
        if (targetWidth == 0 || targetHeight == 0) {
            targetWidth = secondWidth;
            targetHeight = secondHeight;
        }
        mPreviewWidth = targetWidth;
        mPreviewHeight = targetHeight;
    }

    private int getCameraRotate() {
        return (mOrientationDegrees + 90) % 360;
    }

    private void calRotateMatrix(float[] mat, int degrees) {
        degrees %= 360;
        if (degrees % 90 != 0) {
            return;
        }
        switch (degrees) {
            case 0:
                Matrix.translateM(mat, 0, 0, 1, 0);
                break;
            case 90:
                Matrix.translateM(mat, 0, 0, 0, 0);
                break;
            case 180:
                Matrix.translateM(mat, 0, 1, 0, 0);
                break;
            case 270:
                Matrix.translateM(mat, 0, 1, 1, 0);
                break;
        }
        Matrix.rotateM(mat, 0, degrees, 0, 0, 1);
        Matrix.scaleM(mat, 0, 1, -1, 1);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //if (VERBOSE) Log.d(TAG, "onFrameAvailable");
        mFrameAvailable = true;
        mGLRender.requestRender();
    }

    private GLRender.GLRenderListener mGLRenderListener = new GLRender.GLRenderListener() {
        @Override
        public void onReady() {
            if (VERBOSE) Log.d(TAG, "onGLContext ready");
            mTextureId = GlUtil.createOESTextureObject();
            synchronized (mCameraLock) {
                if (mSurfaceTexture != null) {
                    mSurfaceTexture.release();
                }
                mSurfaceTexture = new SurfaceTexture(mTextureId);
                mSurfaceTexture.setOnFrameAvailableListener(UVCCameraCapture.this);
                if (mUVCCamera != null) {
                    mUVCCamera.setPreviewTexture(mSurfaceTexture);
                    mUVCCamera.startPreview();
                }
            }

            mTexInited = false;
            mFrameAvailable = false;
        }

        @Override
        public void onSizeChanged(int width, int height) {
            if (VERBOSE) Log.d(TAG, "onSizeChanged " + width + "x" + height);
        }

        @Override
        public void onDrawFrame() {
            //if (VERBOSE) Log.d(TAG, "onDrawFrame");

            long pts = System.nanoTime() / 1000 / 1000;
            try {
                mSurfaceTexture.updateTexImage();
            } catch (Exception e) {
                Log.e(TAG, "updateTexImage failed, ignore");
                return;
            }
            if (!mFrameAvailable) {
                return;
            }

            if (!mTexInited) {
                mTexInited = true;
                init();
            }

            float[] texMatrix = new float[16];
            mSurfaceTexture.getTransformMatrix(texMatrix);
            calRotateMatrix(texMatrix, getCameraRotate());
            ImgTexFrame frame = new ImgTexFrame(mImgTexFormat, mTextureId, texMatrix, pts);
            try {
                mImgTexSrcPin.onFrameAvailable(frame);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Draw frame failed, ignore");
            }

            // cal preview fps
            mFrameDrawn++;
            long tm = System.currentTimeMillis();
            long tmDiff = tm - mLastTraceTime;
            if (tmDiff >= 1000) {
                mCurrentFps = mFrameDrawn * 1000.f / tmDiff;
                if (TRACE_FPS_LIMIT) {
                    Log.d(TAG, "preview fps: " + String.format(Locale.getDefault(),
                            "%.2f", mCurrentFps));
                }
                mFrameDrawn = 0;
                mLastTraceTime = tm;
            }
        }

        @Override
        public void onReleased() {
            if (VERBOSE) Log.d(TAG, "onGLContext released");

            mFrameAvailable = false;
            synchronized (mCameraLock) {
                if (mSurfaceTexture != null) {
                    mSurfaceTexture.setOnFrameAvailableListener(null);
                    mSurfaceTexture.release();
                    mSurfaceTexture = null;
                }
            }
        }

        private void init() {
            int ori = getCameraRotate();
            int width = mPreviewWidth;
            int height = mPreviewHeight;
            if (ori % 180 != 0) {
                //noinspection SuspiciousNameCombination
                width = mPreviewHeight;
                //noinspection SuspiciousNameCombination
                height = mPreviewWidth;
            }
            mImgTexFormat = new ImgTexFormat(ImgTexFormat.COLOR_EXTERNAL_OES, width, height);
            mImgTexSrcPin.onFormatChanged(mImgTexFormat);

            // cal preview fps
            mLastTraceTime = System.currentTimeMillis();
            mFrameDrawn = 0;
            mCurrentFps = 0;
        }
    };
}
