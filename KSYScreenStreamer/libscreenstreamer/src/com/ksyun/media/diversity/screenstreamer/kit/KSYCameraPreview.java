package com.ksyun.media.diversity.screenstreamer.kit;

import com.ksyun.media.streamer.capture.CameraCapture;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterMgt;
import com.ksyun.media.streamer.filter.imgtex.ImgTexPreview;
import com.ksyun.media.streamer.filter.imgtex.ImgTexScaleFilter;
import com.ksyun.media.streamer.kit.StreamerConstants;
import com.ksyun.media.streamer.util.gles.GLRender;

import android.content.Context;

import javax.microedition.khronos.egl.EGLContext;

import android.util.Log;
import android.view.TextureView;

/**
 * kit for camera preview for screenRecord
 */
public class KSYCameraPreview {
    private static final String TAG = "KSYCameraPreview";
    private static final boolean DEBUG = false;
    private static final int DEFAULT_PREVIEW_WIDTH = 720;
    private static final int DEFAULT_PREVIEW_HEIGHT = 1280;

    private Context mContext;

    private int mScreenRenderWidth = 0;
    private int mScreenRenderHeight = 0;
    private int mCameraFacing = CameraCapture.FACING_FRONT;
    private int mPreviewResolution = StreamerConstants.DEFAULT_PREVIEW_RESOLUTION;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private int mRotateDegrees = 0;
    private float mPreviewFps = StreamerConstants.DEFAULT_PREVIEW_FPS;
    private boolean mDelayedStartCameraPreview = false;

    private EGLContext mEGLContext;
    private GLRender mGLRender;
    private CameraCapture mCameraCapture;
    private ImgTexScaleFilter mImgTexScaleFilter;
    private ImgTexFilterMgt mImgTexFilterMgt;
    private ImgTexPreview mImgTexPreview;

    private KSYCameraPreview.OnInfoListener mOnInfoListener;
    private KSYCameraPreview.OnErrorListener mOnErrorListener;

    public KSYCameraPreview(Context context, EGLContext eglContext) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null!");
        }
        mContext = context.getApplicationContext();
        mEGLContext = eglContext;
        initModules();
    }

    private void initModules() {
        // Init GLRender for gpu render
        mGLRender = new GLRender(mEGLContext);

        // Camera preview
        mCameraCapture = new CameraCapture(mContext, mGLRender);
        mImgTexScaleFilter = new ImgTexScaleFilter(mGLRender);
        mImgTexFilterMgt = new ImgTexFilterMgt(mContext);
        mImgTexPreview = new ImgTexPreview();
        mCameraCapture.mImgTexSrcPin.connect(mImgTexScaleFilter.getSinkPin());
        mImgTexScaleFilter.getSrcPin().connect(mImgTexFilterMgt.getSinkPin());
        mImgTexFilterMgt.getSrcPin().connect(mImgTexPreview.getSinkPin());

        // set listeners
        mGLRender.addListener(new GLRender.GLRenderListener() {
            @Override
            public void onReady() {
                mImgTexPreview.setEGL10Context(mGLRender.getEGL10Context());
            }

            @Override
            public void onSizeChanged(int width, int height) {
            }

            @Override
            public void onDrawFrame() {
            }

            @Override
            public void onReleased() {
            }
        });

        mCameraCapture.setOnCameraCaptureListener(new CameraCapture.OnCameraCaptureListener() {
            @Override
            public void onStarted() {
                Log.d(TAG, "CameraCapture ready");
                if (mOnInfoListener != null) {
                    mOnInfoListener.onInfo(StreamerConstants.KSY_STREAMER_CAMERA_INIT_DONE, 0, 0);
                }
            }

            @Override
            public void onFacingChanged(int facing) {
                mCameraFacing = facing;
                if (mOnInfoListener != null) {
                    mOnInfoListener.onInfo(StreamerConstants.KSY_STREAMER_CAMERA_FACEING_CHANGED, facing, 0);
                }
            }

            @Override
            public void onError(int err) {
                Log.e(TAG, "CameraCapture error: " + err);
                int what;
                switch (err) {
                    case CameraCapture.CAMERA_ERROR_START_FAILED:
                        what = StreamerConstants.KSY_STREAMER_CAMERA_ERROR_START_FAILED;
                        break;
                    case CameraCapture.CAMERA_ERROR_SERVER_DIED:
                        what = StreamerConstants.KSY_STREAMER_CAMERA_ERROR_SERVER_DIED;
                        break;
                    case CameraCapture.CAMERA_ERROR_EVICTED:
                        what = StreamerConstants.KSY_STREAMER_CAMERA_ERROR_EVICTED;
                        break;
                    case CameraCapture.CAMERA_ERROR_UNKNOWN:
                    default:
                        what = StreamerConstants.KSY_STREAMER_CAMERA_ERROR_UNKNOWN;
                        break;
                }
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError(what, 0, 0);
                }
            }
        });
        // init with offscreen GLRender
        mGLRender.init(1, 1);
    }

    /**
     * Get {@link GLRender} instance.
     *
     * @return GLRender instance.
     */
    public GLRender getGLRender() {
        return mGLRender;
    }

    /**
     * Get {@link CameraCapture} module instance.
     *
     * @return CameraCapture instance.
     */
    public CameraCapture getCameraCapture() {
        return mCameraCapture;
    }

    /**
     * Get {@link ImgTexFilterMgt} instance to manage GPU filters.
     *
     * @return ImgTexFilterMgt instance.
     */
    public ImgTexFilterMgt getImgTexFilterMgt() {
        return mImgTexFilterMgt;
    }

    /**
     * Set TextureView as camera previewer.<br/>
     * Must set once before the TextureView ready.
     *
     * @param textureView TextureView to be set.
     */
    public void setDisplayPreview(TextureView textureView) {
        mImgTexPreview.setDisplayPreview(textureView);
        mImgTexPreview.getGLRender().addListener(mGLRenderListener);
    }

    /**
     * Set rotate degrees in anti-clockwise of current Activity.
     *
     * @param degrees Degrees in anti-clockwise, only 0, 90, 180, 270 accepted.
     * @throws IllegalArgumentException
     */
    public void setRotateDegrees(int degrees) throws IllegalArgumentException {
        degrees %= 360;
        if (degrees % 90 != 0) {
            throw new IllegalArgumentException("Invalid rotate degrees");
        }

        if (mRotateDegrees == degrees) {
            return;
        }

        boolean isLastLandscape = (mRotateDegrees % 180) != 0;
        boolean isLandscape = (degrees % 180) != 0;
        if (isLastLandscape != isLandscape) {
            if (mPreviewWidth > 0 || mPreviewHeight > 0) {
                setPreviewResolution(mPreviewHeight, mPreviewWidth);
            }
        }
        mRotateDegrees = degrees;
        mCameraCapture.setOrientation(degrees);

        mImgTexScaleFilter.setTargetSize(mPreviewWidth, mPreviewHeight);
    }

    /**
     * get rotate degrees
     *
     * @return degrees Degrees in anti-clockwise, only 0, 90, 180, 270 accepted.
     */
    public int getRotateDegrees() {
        return mRotateDegrees;
    }

    /**
     * Set camera capture resolution.<br/>
     * <p>
     * The set resolution would take effect on next {@link #startCameraPreview()}
     * {@link #startCameraPreview(int)} call.<br/>
     * <p>
     * Both of the set width and height must be greater than 0.
     *
     * @param width  capture width
     * @param height capture height
     * @throws IllegalArgumentException
     */
    public void setCameraCaptureResolution(int width, int height) throws IllegalArgumentException {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid resolution");
        }
        mCameraCapture.setPreviewSize(width, height);
    }

    /**
     * Set camera capture resolution.<br/>
     * <p>
     * The set resolution would take effect on next {@link #startCameraPreview()}
     * {@link #startCameraPreview(int)} call.<br/>
     *
     * @param idx Resolution index.<br/>
     * @throws IllegalArgumentException
     * @see StreamerConstants#VIDEO_RESOLUTION_360P
     * @see StreamerConstants#VIDEO_RESOLUTION_480P
     * @see StreamerConstants#VIDEO_RESOLUTION_540P
     * @see StreamerConstants#VIDEO_RESOLUTION_720P
     * @see StreamerConstants#VIDEO_RESOLUTION_1080P
     */
    public void setCameraCaptureResolution(int idx) throws IllegalArgumentException {
        if (idx < StreamerConstants.VIDEO_RESOLUTION_360P ||
                idx > StreamerConstants.VIDEO_RESOLUTION_1080P) {
            throw new IllegalArgumentException("Invalid resolution index");
        }
        int height = getShortEdgeLength(idx);
        int width = height * 16 / 9;
        mCameraCapture.setPreviewSize(width, height);
    }

    /**
     * Set preview resolution.<br/>
     * <p>
     * The set resolution would take effect on next {@link #startCameraPreview()}
     * {@link #startCameraPreview(int)} call, if called not in previewing mode.<br/>
     * If called in previewing mode, it would take effect immediately.<br/>
     * <p>
     * The set width and height must not be 0 at same time.
     * If one of the params is 0, the other would calculated by the actual preview view size
     * to keep the ratio of the preview view.
     *
     * @param width  preview width.
     * @param height preview height.
     * @throws IllegalArgumentException
     */
    public void setPreviewResolution(int width, int height) throws IllegalArgumentException {
        if (width < 0 || height < 0 || (width == 0 && height == 0)) {
            throw new IllegalArgumentException("Invalid resolution");
        }
        mPreviewWidth = width;
        mPreviewHeight = height;

        if (mScreenRenderWidth != 0 && mScreenRenderHeight != 0) {
            calResolution();
            mImgTexScaleFilter.setTargetSize(mPreviewWidth, mPreviewHeight);
        }
    }

    /**
     * Set preview resolution index.<br/>
     * <p>
     * The set resolution would take effect on next {@link #startCameraPreview()}
     * {@link #startCameraPreview(int)} call.
     * If called in previewing mode, it would take effect immediately.<br/>
     *
     * @param idx Resolution index.<br/>
     * @throws IllegalArgumentException
     * @see StreamerConstants#VIDEO_RESOLUTION_360P
     * @see StreamerConstants#VIDEO_RESOLUTION_480P
     * @see StreamerConstants#VIDEO_RESOLUTION_540P
     * @see StreamerConstants#VIDEO_RESOLUTION_720P
     * @see StreamerConstants#VIDEO_RESOLUTION_1080P
     */
    public void setPreviewResolution(int idx) throws IllegalArgumentException {
        if (idx < StreamerConstants.VIDEO_RESOLUTION_360P ||
                idx > StreamerConstants.VIDEO_RESOLUTION_720P) {
            throw new IllegalArgumentException("Invalid resolution index");
        }
        mPreviewResolution = idx;
        mPreviewWidth = 0;
        mPreviewHeight = 0;
        if (mScreenRenderWidth != 0 && mScreenRenderHeight != 0) {
            calResolution();
            mImgTexScaleFilter.setTargetSize(mPreviewWidth, mPreviewHeight);
        }
    }

    /**
     * get preview width
     *
     * @return preview width
     */
    public int getPreviewWidth() {
        return mPreviewWidth;
    }

    /**
     * get preview height
     *
     * @return preview height
     */
    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    /**
     * Set preview fps.<br/>
     * <p>
     * The set fps would take effect on next {@link #startCameraPreview()}
     * {@link #startCameraPreview(int)} call.<br/>
     * <p>
     * The actual preview fps depends on the running device, may be different with the set value.
     *
     * @param fps frame rate to be set.
     * @throws IllegalArgumentException
     */
    public void setPreviewFps(float fps) throws IllegalArgumentException {
        if (fps <= 0) {
            throw new IllegalArgumentException("the fps must > 0");
        }
        mPreviewFps = fps;
    }

    /**
     * get preview frame rate
     *
     * @return preview frame rate
     */
    public float getPreviewFps() {
        return mPreviewFps;
    }

    /**
     * Set initial camera facing.<br/>
     * Set before {@link #startCameraPreview()}, give a chance to set initial camera facing,
     * equals {@link #startCameraPreview(int)}.<br/>
     *
     * @param facing camera facing.
     * @see CameraCapture#FACING_FRONT
     * @see CameraCapture#FACING_BACK
     */
    public void setCameraFacing(int facing) {
        mCameraFacing = facing;
    }

    /**
     * get camera facing.
     *
     * @return camera facing
     */
    public int getCameraFacing() {
        return mCameraFacing;
    }

    /**
     * Start camera preview with default facing, or facing set by
     * {@link #setCameraFacing(int)} before.
     */
    public void startCameraPreview() {
        startCameraPreview(mCameraFacing);
    }

    /**
     * Start camera preview with given facing.
     *
     * @param facing camera facing.
     * @see CameraCapture#FACING_FRONT
     * @see CameraCapture#FACING_BACK
     */
    public void startCameraPreview(int facing) {
        mCameraFacing = facing;
        if ((mPreviewWidth == 0 || mPreviewHeight == 0) &&
                (mScreenRenderWidth == 0 || mScreenRenderHeight == 0)) {
            if (mImgTexPreview.getDisplayPreview() != null) {
                mDelayedStartCameraPreview = true;
                return;
            }
            mScreenRenderWidth = DEFAULT_PREVIEW_WIDTH;
            mScreenRenderHeight = DEFAULT_PREVIEW_HEIGHT;
        }
        setPreviewParams();
        mCameraCapture.start(mCameraFacing);
    }

    /**
     * Stop camera preview.
     * init camera resolution
     */
    public void stopCameraPreview() {
        mCameraCapture.stop();
        mScreenRenderWidth = 0;
        mScreenRenderHeight = 0;
        mPreviewWidth = 0;
        mPreviewHeight = 0;
    }

    private int getShortEdgeLength(int resolution) {
        switch (resolution) {
            case StreamerConstants.VIDEO_RESOLUTION_360P:
                return 360;
            case StreamerConstants.VIDEO_RESOLUTION_480P:
                return 480;
            case StreamerConstants.VIDEO_RESOLUTION_540P:
                return 540;
            case StreamerConstants.VIDEO_RESOLUTION_720P:
                return 720;
            case StreamerConstants.VIDEO_RESOLUTION_1080P:
                return 1080;
            default:
                return 720;
        }
    }

    private int align(int val, int align) {
        return (val + align - 1) / align * align;
    }

    private void calResolution() {
        if (mPreviewWidth == 0 && mPreviewHeight == 0) {
            int val = getShortEdgeLength(mPreviewResolution);
            if (mScreenRenderWidth > mScreenRenderHeight) {
                mPreviewHeight = val;
            } else {
                mPreviewWidth = val;
            }
        }

        if (mScreenRenderWidth != 0 && mScreenRenderHeight != 0) {
            if (mPreviewWidth == 0) {
                mPreviewWidth = mPreviewHeight * mScreenRenderWidth / mScreenRenderHeight;
            } else if (mPreviewHeight == 0) {
                mPreviewHeight = mPreviewWidth * mScreenRenderHeight / mScreenRenderWidth;
            }
        }
        mPreviewWidth = align(mPreviewWidth, 8);
        mPreviewHeight = align(mPreviewHeight, 8);
    }

    private void setPreviewParams() {
        calResolution();
        mCameraCapture.setOrientation(mRotateDegrees);
        mCameraCapture.setPreviewSize(mPreviewWidth, mPreviewHeight);
        if (mPreviewFps == 0) {
            mPreviewFps = CameraCapture.DEFAULT_PREVIEW_FPS;
        }
        mCameraCapture.setPreviewFps(mPreviewFps);

        mImgTexScaleFilter.setTargetSize(mPreviewWidth, mPreviewHeight);
    }

    /**
     * Set info listener.
     *
     * @param listener info listener
     */
    public void setOnInfoListener(KSYCameraPreview.OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    /**
     * Set error listener.
     *
     * @param listener error listener
     */
    public void setOnErrorListener(KSYCameraPreview.OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    /**
     * Switch camera facing between front and back.
     */
    public void switchCamera() {
        mCameraCapture.switchCamera();
    }


    /**
     * Get if current camera in use is front camera.<br/>
     *
     * @return true if front camera in use false otherwise.
     */
    public boolean isFrontCamera() {
        return mCameraFacing == CameraCapture.FACING_FRONT;
    }


    /**
     * Should be called on Activity.onResume or Fragment.onResume.
     */
    public void onResume() {
        mGLRender.onResume();
    }

    /**
     * Should be called on Activity.onPause or Fragment.onPause.
     */
    public void onPause() {
        mGLRender.onPause();
    }

    public void release() {
        mGLRender.release();
        mCameraCapture.release();
    }

    private GLRender.GLRenderListener mGLRenderListener = new GLRender.GLRenderListener() {
        @Override
        public void onReady() {
        }

        @Override
        public void onSizeChanged(int width, int height) {
            mScreenRenderWidth = width;
            mScreenRenderHeight = height;
            setPreviewParams();
            if (mDelayedStartCameraPreview) {
                mCameraCapture.start(mCameraFacing);
                mDelayedStartCameraPreview = false;
            }
        }

        @Override
        public void onDrawFrame() {
        }

        @Override
        public void onReleased() {

        }
    };

    public interface OnInfoListener {
        void onInfo(int what, int msg1, int msg2);
    }

    public interface OnErrorListener {
        void onError(int what, int msg1, int msg2);
    }
}
