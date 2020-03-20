package com.ksyun.media.diversity.pipstreamer.kit;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.TextureView;

import com.ksyun.media.diversity.pipstreamer.capture.MediaPlayerCapture;
import com.ksyun.media.diversity.pipstreamer.capture.PictureCapture;
import com.ksyun.media.streamer.capture.AudioCapture;
import com.ksyun.media.streamer.capture.AudioPlayerCapture;
import com.ksyun.media.streamer.capture.CameraCapture;
import com.ksyun.media.streamer.capture.WaterMarkCapture;
import com.ksyun.media.streamer.encoder.AVCodecAudioEncoder;
import com.ksyun.media.streamer.encoder.AudioEncodeFormat;
import com.ksyun.media.streamer.encoder.AudioEncoderMgt;
import com.ksyun.media.streamer.encoder.Encoder;
import com.ksyun.media.streamer.encoder.MediaCodecAudioEncoder;
import com.ksyun.media.streamer.encoder.VideoEncodeFormat;
import com.ksyun.media.streamer.encoder.VideoEncoderMgt;
import com.ksyun.media.streamer.filter.audio.AudioFilterMgt;
import com.ksyun.media.streamer.filter.audio.AudioMixer;
import com.ksyun.media.streamer.filter.audio.AudioPreview;
import com.ksyun.media.streamer.filter.audio.AudioResampleFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterMgt;
import com.ksyun.media.streamer.filter.imgtex.ImgTexMixer;
import com.ksyun.media.streamer.filter.imgtex.ImgTexScaleFilter;
import com.ksyun.media.streamer.framework.AVConst;
import com.ksyun.media.streamer.framework.AudioBufFormat;
import com.ksyun.media.streamer.kit.KSYStreamerConfig;
import com.ksyun.media.streamer.kit.OnAudioRawDataListener;
import com.ksyun.media.streamer.kit.OnPreviewFrameListener;
import com.ksyun.media.streamer.kit.RecorderConstants;
import com.ksyun.media.streamer.kit.StreamerConstants;
import com.ksyun.media.streamer.logstats.StatsConstant;
import com.ksyun.media.streamer.logstats.StatsLogReport;
import com.ksyun.media.streamer.publisher.RtmpPublisher;
import com.ksyun.media.streamer.util.gles.GLRender;


/**
 * All in one streamer class.
 */
public class KSYPipStreamer {

    private static final String TAG = "KSYStreamer";
    private static final boolean DEBUG = false;

    private static final int IDX_BG_IMG = 0;
    private static final int IDX_BG_VIDEO = 1;
    private static final int IDX_CAMERA = 2;
    private static final int IDX_WM_LOGO = 3;
    private static final int IDX_WM_TIME = 4;

    private static final int IDX_AUDIO_MIC = 0;
    private static final int IDX_AUDIO_BGM = 1;
    private static final int IDX_AUDIO_PIP = 2;

    private Context mContext;

    private String mUri;
    private int mScreenRenderWidth = 0;
    private int mScreenRenderHeight = 0;
    private int mPreviewResolution = StreamerConstants.VIDEO_RESOLUTION_360P;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private float mPreviewFps = 0;
    private int mTargetResolution = StreamerConstants.VIDEO_RESOLUTION_360P;
    private int mTargetWidth = 0;
    private int mTargetHeight = 0;
    private float mTargetFps = 0;
    private float mIFrameInterval = 3.0f;
    private int mRotateDegrees = 0;
    private int mMaxVideoBitrate = 800 * 1000;
    private int mInitVideoBitrate = 600 * 1000;
    private int mMinVideoBitrate = 200 * 1000;
    private boolean mAutoAdjustVideoBitrate = true;
    private int mAudioBitrate = 48 * 1000;
    private int mAudioSampleRate = 44100;
    private int mAudioChannels = 1;

    private boolean mFrontCameraMirror = false;
    private boolean mEnableStreamStatModule = true;
    private int mCameraFacing = CameraCapture.FACING_FRONT;

    private boolean mIsRecording = false;
    private boolean mIsAudioOnly = false;
    private boolean mIsAudioPreviewing = false;
    private boolean mDelayedStartCameraPreview = false;
    private boolean mEnableDebugLog = false;
    private boolean mEnableAudioMix = false;

    // compat members
    private KSYStreamerConfig mConfig;

    private OnInfoListener mOnInfoListener;
    private OnErrorListener mOnErrorListener;

    private GLRender mGLRender;
    private CameraCapture mCameraCapture;
    private WaterMarkCapture mWaterMarkCapture;
    private MediaPlayerCapture mMediaPlayerCapture;
    private PictureCapture mPictureCapture;
    private ImgTexScaleFilter mImgTexScaleFilter;
    private ImgTexMixer mImgTexMixer;
    private ImgTexFilterMgt mImgTexFilterMgt;
    private AudioCapture mAudioCapture;
    private VideoEncoderMgt mVideoEncoderMgt;
    private AudioEncoderMgt mAudioEncoderMgt;
    private RtmpPublisher mRtmpPublisher;

    private AudioResampleFilter mAudioResampleFilter;
    private AudioFilterMgt mAudioFilterMgt;
    private AudioPlayerCapture mAudioPlayerCapture;
    private AudioMixer mAudioMixer;
    private AudioPreview mAudioPreview;

    public KSYPipStreamer(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null!");
        }
        mContext = context.getApplicationContext();
        initModules();
    }

    /**
     * Set params to KSYStreamer with {@link KSYStreamerConfig}.
     *
     * @param config streamer params
     * @deprecated
     */
    @Deprecated
    public void setConfig(KSYStreamerConfig config) {
        if (config == null || mContext == null) {
            throw new IllegalStateException("method invoking failed " +
                    "context == null or config == null!");
        }

        mConfig = config;
        setUrl(config.getUrl());
        setPreviewResolution(config.getVideoResolution());
        setTargetResolution(config.getVideoResolution());
        setPreviewFps(config.getFrameRate());
        setTargetFps(config.getFrameRate());
        setIFrameInterval(config.getIFrameIntervalSec());
        setRotateDegrees(config.getDefaultLandscape() ? 90 : 0);
        if (config.isAutoAdjustBitrate()) {
            setVideoBitrate(config.getInitAverageVideoBitrate() * 1000,
                    config.getMaxAverageVideoBitrate() * 1000,
                    config.getMinAverageVideoBitrate() * 1000);
        } else {
            setVideoBitrate(config.getInitAverageVideoBitrate() * 1000);
        }
        setAudioSampleRate(config.getAudioSampleRate());
        setAudioChannels(config.getAudioChannels());
        setAudioBitrate(config.getAudioBitrate() * 1000);
        setEncodeMethod(config.getEncodeMethod());
        setFrontCameraMirror(config.isFrontCameraMirror());
        setEnableStreamStatModule(config.isEnableStreamStatModule());
        setCameraFacing(config.getDefaultFrontCamera() ?
                CameraCapture.FACING_FRONT : CameraCapture.FACING_BACK);
    }

    private void initModules() {
        // Init GLRender for gpu render
        mGLRender = new GLRender();

        // Watermark capture
        mWaterMarkCapture = new WaterMarkCapture(mGLRender);

        // pip capture
        mPictureCapture = new PictureCapture(mGLRender);
        mMediaPlayerCapture = new MediaPlayerCapture(mContext, mGLRender);

        // Camera preview
        mCameraCapture = new CameraCapture(mContext, mGLRender);
        mImgTexScaleFilter = new ImgTexScaleFilter(mGLRender);
        mImgTexFilterMgt = new ImgTexFilterMgt();
        mImgTexMixer = new ImgTexMixer(mGLRender);
        mImgTexMixer.setIsPreviewer(true);
        mCameraCapture.mImgTexSrcPin.connect(mImgTexScaleFilter.getSinkPin());
        mImgTexScaleFilter.getSrcPin().connect(mImgTexFilterMgt.getSinkPin());
        mImgTexFilterMgt.getSrcPin().connect(mImgTexMixer.getSinkPin(IDX_CAMERA));
        mImgTexMixer.setMainSinkPinIndex(IDX_CAMERA);
        mWaterMarkCapture.mLogoTexSrcPin.connect(mImgTexMixer.getSinkPin(IDX_WM_LOGO));
        mWaterMarkCapture.mTimeTexSrcPin.connect(mImgTexMixer.getSinkPin(IDX_WM_TIME));

        // Audio preview
        mAudioPlayerCapture = new AudioPlayerCapture(mContext);
        mAudioCapture = new AudioCapture();
        mAudioResampleFilter = new AudioResampleFilter();
        mAudioFilterMgt = new AudioFilterMgt();
        mAudioMixer = new AudioMixer();
        mAudioPreview = new AudioPreview();
        mAudioCapture.mAudioBufSrcPin.connect(mAudioResampleFilter.getSinkPin());
        mAudioResampleFilter.getSrcPin().connect(mAudioFilterMgt.getSinkPin());
        mAudioFilterMgt.getSrcPin().connect(mAudioMixer.getSinkPin(IDX_AUDIO_MIC));
        if (mEnableAudioMix) {
            mAudioPlayerCapture.mSrcPin.connect(mAudioMixer.getSinkPin(IDX_AUDIO_BGM));
        }
        mAudioMixer.getSrcPin().connect(mAudioPreview.mSinkPin);

        // pip connect
        mPictureCapture.getSrcPin().connect(mImgTexMixer.getSinkPin(IDX_BG_IMG));
        mMediaPlayerCapture.mImgTexSrcPin.connect(mImgTexMixer.getSinkPin(IDX_BG_VIDEO));
        mMediaPlayerCapture.mAudioBufSrcPin.connect(mAudioMixer.getSinkPin(IDX_AUDIO_PIP));
        mImgTexMixer.setScalingMode(IDX_BG_IMG, ImgTexMixer.SCALING_MODE_CENTER_CROP);
        mImgTexMixer.setScalingMode(IDX_BG_VIDEO, ImgTexMixer.SCALING_MODE_BEST_FIT);
        mImgTexMixer.setScalingMode(IDX_CAMERA, ImgTexMixer.SCALING_MODE_CENTER_CROP);

        // encoder
        mVideoEncoderMgt = new VideoEncoderMgt(mGLRender);
        mAudioEncoderMgt = new AudioEncoderMgt();
        mWaterMarkCapture.mLogoBufSrcPin.connect(mVideoEncoderMgt.getImgBufMixer().getSinkPin(1));
        mWaterMarkCapture.mTimeBufSrcPin.connect(mVideoEncoderMgt.getImgBufMixer().getSinkPin(2));
        mImgTexMixer.getSrcPin().connect(mVideoEncoderMgt.getImgTexSinkPin());
        mCameraCapture.mImgBufSrcPin.connect(mVideoEncoderMgt.getImgBufSinkPin());
        mAudioMixer.getSrcPin().connect(mAudioEncoderMgt.getSinkPin());

        // publisher
        mRtmpPublisher = new RtmpPublisher();
        mAudioEncoderMgt.getSrcPin().connect(mRtmpPublisher.mAudioSink);
        mVideoEncoderMgt.getSrcPin().connect(mRtmpPublisher.mVideoSink);

        // stats
        StatsLogReport.getInstance().initLogReport(mContext);

        // set listeners
        mAudioCapture.setAudioCaptureListener(new AudioCapture.OnAudioCaptureListener() {
            @Override
            public void onStatusChanged(int status) {
            }

            @Override
            public void onError(int errorCode) {
                Log.e(TAG, "AudioCapture error: " + errorCode);
                int what;
                switch (errorCode) {
                    case AudioCapture.AUDIO_START_FAILED:
                        what = StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED;
                        break;
                    case AudioCapture.AUDIO_ERROR_UNKNOWN:
                    default:
                        what = StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN;
                        break;
                }
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError(what, 0, 0);
                }
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
                updateFrontMirror();
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

        Encoder.EncoderListener encoderListener = new Encoder.EncoderListener() {
            @Override
            public void onError(Encoder encoder, int err) {
                if (err != 0) {
                    stopStream();
                }

                boolean isVideo = true;
                if (encoder instanceof MediaCodecAudioEncoder ||
                        encoder instanceof AVCodecAudioEncoder) {
                    isVideo = false;
                }

                int what;
                switch (err) {
                    case Encoder.ENCODER_ERROR_UNSUPPORTED:
                        what = isVideo ?
                                StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED :
                                StreamerConstants.KSY_STREAMER_AUDIO_ENCODER_ERROR_UNSUPPORTED;
                        break;
                    case Encoder.ENCODER_ERROR_UNKNOWN:
                    default:
                        what = isVideo ?
                                StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN :
                                StreamerConstants.KSY_STREAMER_AUDIO_ENCODER_ERROR_UNKNOWN;
                        break;
                }
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError(what, 0, 0);
                }
            }
        };
        mVideoEncoderMgt.setEncoderListener(encoderListener);
        mAudioEncoderMgt.setEncoderListener(encoderListener);

        mRtmpPublisher.setRtmpPubListener(new RtmpPublisher.RtmpPubListener() {
            @Override
            public void onInfo(int type, long msg) {
                switch (type) {
                    case RtmpPublisher.INFO_CONNECTED:
                        mAudioEncoderMgt.getEncoder().start();
                        if (mOnInfoListener != null) {
                            mOnInfoListener.onInfo(
                                    StreamerConstants.KSY_STREAMER_OPEN_STREAM_SUCCESS, 0, 0);
                        }
                        break;
                    case RtmpPublisher.INFO_AUDIO_HEADER_GOT:
                        if (!mIsAudioOnly) {
                            // start video encoder after audio header got
                            mVideoEncoderMgt.getEncoder().start();
                        }
                        break;
                    case RtmpPublisher.INFO_PACKET_SEND_SLOW:
                        Log.i(TAG, "packet send slow, delayed " + msg + "ms");
                        if (mOnInfoListener != null) {
                            mOnInfoListener.onInfo(
                                    StreamerConstants.KSY_STREAMER_FRAME_SEND_SLOW,
                                    (int) msg, 0);
                        }
                        break;
                    case RtmpPublisher.INFO_EST_BW_RAISE:
                        if (mIsAudioOnly) {
                            break;
                        }
                        if (mAutoAdjustVideoBitrate) {
                            Log.d(TAG, "Raise video bitrate to " + msg);
                            mVideoEncoderMgt.getEncoder().adjustBitrate((int) msg);
                        }
                        if (mOnInfoListener != null) {
                            mOnInfoListener.onInfo(
                                    StreamerConstants.KSY_STREAMER_EST_BW_RAISE, (int) msg, 0);
                        }
                        break;
                    case RtmpPublisher.INFO_EST_BW_DROP:
                        if (mIsAudioOnly) {
                            break;
                        }
                        if (mAutoAdjustVideoBitrate) {
                            Log.d(TAG, "Drop video bitrate to " + msg);
                            mVideoEncoderMgt.getEncoder().adjustBitrate((int) msg);
                        }
                        if (mOnInfoListener != null) {
                            mOnInfoListener.onInfo(
                                    StreamerConstants.KSY_STREAMER_EST_BW_DROP, (int) msg, 0);
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onError(int err, long msg) {
                Log.e(TAG, "RtmpPub err=" + err);
                if (err != 0) {
                    stopStream();
                }

                if (mOnErrorListener != null) {
                    int status;
                    switch (err) {
                        case RtmpPublisher.ERROR_CONNECT_BREAKED:
                            status = StreamerConstants.KSY_STREAMER_ERROR_CONNECT_BREAKED;
                            break;
                        case RtmpPublisher.ERROR_DNS_PARSE_FAILED:
                            status = StreamerConstants.KSY_STREAMER_ERROR_DNS_PARSE_FAILED;
                            break;
                        case RtmpPublisher.ERROR_CONNECT_FAILED:
                            status = StreamerConstants.KSY_STREAMER_ERROR_CONNECT_FAILED;
                            break;
                        case RtmpPublisher.ERROR_PUBLISH_FAILED:
                            status = StreamerConstants.KSY_STREAMER_ERROR_PUBLISH_FAILED;
                            break;
                        case RtmpPublisher.ERROR_AV_ASYNC_ERROR:
                            status = StreamerConstants.KSY_STREAMER_ERROR_AV_ASYNC;
                            break;
                        default:
                            status = StreamerConstants.KSY_STREAMER_ERROR_PUBLISH_FAILED;
                            break;
                    }
                    mOnErrorListener.onError(status, (int) msg, 0);
                }
            }
        });
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
     * Get {@link AudioCapture} module instance.
     *
     * @return AudioCapture instance.
     */
    public AudioCapture getAudioCapture() {
        return mAudioCapture;
    }

    /**
     * Get {@link PictureCapture} module instance.
     *
     * @return PictureCapture instance.
     */
    public PictureCapture getPictureCapture() {
        return mPictureCapture;
    }

    /**
     * Get {@link MediaPlayerCapture} module instance.
     *
     * @return MediaPlayerCapture instance.
     */
    public MediaPlayerCapture getMediaPlayerCapture() {
        return mMediaPlayerCapture;
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
     * Get {@link AudioFilterMgt} instance to manage audio filters.
     *
     * @return AudioFilterMgt instance
     */
    public AudioFilterMgt getAudioFilterMgt() {
        return mAudioFilterMgt;
    }

    /**
     * Get {@link ImgTexMixer} instance which could handle PIP related operations.
     *
     * @return ImgTexMixer instance.
     */
    public ImgTexMixer getImgTexMixer() {
        return mImgTexMixer;
    }

    /**
     * Get {@link VideoEncoderMgt} instance which control video encoders.
     *
     * @return VideoEncoderMgt instance.
     */
    public VideoEncoderMgt getVideoEncoderMgt() {
        return mVideoEncoderMgt;
    }

    /**
     * Get {@link AudioEncoderMgt} instance which control audio encoders.
     *
     * @return AudioEncoderMgt instance.
     */
    public AudioEncoderMgt getAudioEncoderMgt() {
        return mAudioEncoderMgt;
    }

    /**
     * Get {@link AudioPlayerCapture} instance which could handle BGM related operations.
     *
     * @return AudioPlayerCapture instance
     */
    public AudioPlayerCapture getAudioPlayerCapture() {
        return mAudioPlayerCapture;
    }

    /**
     * Get {@link RtmpPublisher} instance which publish encoded a/v frames throw rtmp protocol.
     *
     * @return RtmpPublisher instance.
     */
    public RtmpPublisher getRtmpPublisher() {
        return mRtmpPublisher;
    }

    /**
     * Set GLSurfaceView as camera previewer.<br/>
     * Must set once before the GLSurfaceView created.
     *
     * @param surfaceView GLSurfaceView to be set.
     */
    public void setDisplayPreview(GLSurfaceView surfaceView) {
        mGLRender.init(surfaceView);
        mGLRender.addListener(mGLRenderListener);
    }

    /**
     * Set TextureView as camera previewer.<br/>
     * Must set once before the TextureView ready.
     *
     * @param textureView TextureView to be set.
     */
    public void setDisplayPreview(TextureView textureView) {
        mGLRender.init(textureView);
        mGLRender.addListener(mGLRenderListener);
    }

    /**
     * Set streaming url.<br/>
     * The set url would take effect on the next {@link #startStream()} call.
     *
     * @param url Streaming url to set.
     */
    public void setUrl(String url) {
        mUri = url;
    }

    /**
     * @param url url streaming to.
     * @deprecated Use {@link #setUrl} instead.
     */
    @Deprecated
    public void updateUrl(String url) {
        setUrl(url);
    }

    /**
     * Set rotate degrees in anti-clockwise of current Activity.
     *
     * @param degrees Degrees in anti-clockwise, only 0, 90, 180, 270 accepted.
     */
    public void setRotateDegrees(int degrees) {
        degrees %= 360;
        if (degrees % 90 != 0) {
            throw new IllegalArgumentException("Invalid rotate degrees");
        }
        mRotateDegrees = degrees;
    }

    /**
     * Set preview resolution.<br/>
     * <p>
     * The set resolution would take effect on next {@link #startCameraPreview()}
     * {@link #startCameraPreview(int)} call.<br/>
     * <p>
     * The set width and height must not be 0 at same time.
     * If one of the params is 0, the other would calculated by the actual preview view size
     * to keep the ratio of the preview view.
     *
     * @param width  preview width.
     * @param height preview height.
     */
    public void setPreviewResolution(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;
    }

    /**
     * Set preview resolution index.<br/>
     * <p>
     * The set resolution would take effect on next {@link #startCameraPreview()}
     * {@link #startCameraPreview(int)} call.
     *
     * @param idx Resolution index.<br/>
     * @see StreamerConstants#VIDEO_RESOLUTION_360P
     * @see StreamerConstants#VIDEO_RESOLUTION_480P
     * @see StreamerConstants#VIDEO_RESOLUTION_540P
     * @see StreamerConstants#VIDEO_RESOLUTION_720P
     */
    public void setPreviewResolution(int idx) {
        if (idx < StreamerConstants.VIDEO_RESOLUTION_360P ||
                idx > StreamerConstants.VIDEO_RESOLUTION_720P) {
            throw new IllegalArgumentException("Invalid resolution index");
        }
        mPreviewResolution = idx;
    }

    /**
     * Set preview fps.<br/>
     * <p>
     * The set fps would take effect on next {@link #startCameraPreview()}
     * {@link #startCameraPreview(int)} call.<br/>
     * <p>
     * The actual preview fps is depend on device, may be different with the set value.
     *
     * @param fps frame rate to be set.
     */
    public void setPreviewFps(float fps) {
        mPreviewFps = fps;
        if (mTargetFps == 0) {
            mTargetFps = mPreviewFps;
        }
    }

    /**
     * Set encode method for both video and audio.<br/>
     * Must not be set while encoding.
     *
     * @param encodeMethod Encode method.<br/>
     * @throws IllegalStateException
     * @see StreamerConstants#ENCODE_METHOD_SOFTWARE
     * @see StreamerConstants#ENCODE_METHOD_SOFTWARE_COMPAT
     * @see StreamerConstants#ENCODE_METHOD_HARDWARE
     */
    public void setEncodeMethod(int encodeMethod) {
        setVideoEncodeMethod(encodeMethod);
        setAudioEncodeMethod(encodeMethod);
    }

    /**
     * Set encode method for video.<br/>
     * Must not be set while encoding.
     *
     * @param encodeMethod Encode method.<br/>
     * @throws IllegalStateException
     * @see StreamerConstants#ENCODE_METHOD_SOFTWARE
     * @see StreamerConstants#ENCODE_METHOD_SOFTWARE_COMPAT
     * @see StreamerConstants#ENCODE_METHOD_HARDWARE
     */
    public void setVideoEncodeMethod(int encodeMethod) {
        if (mIsRecording) {
            throw new IllegalStateException("Cannot set encode method while recording");
        }
        mVideoEncoderMgt.setEncodeMethod(encodeMethod);
    }

    /**
     * Get video encode method.
     *
     * @return video encode method.
     * @see StreamerConstants#ENCODE_METHOD_SOFTWARE
     * @see StreamerConstants#ENCODE_METHOD_SOFTWARE_COMPAT
     * @see StreamerConstants#ENCODE_METHOD_HARDWARE
     */
    public int getVideoEncodeMethod() {
        return mVideoEncoderMgt.getEncodeMethod();
    }

    /**
     * Set encode method for audio.<br/>
     * Must not be set while encoding.
     *
     * @param encodeMethod Encode method.<br/>
     * @throws IllegalStateException
     * @see StreamerConstants#ENCODE_METHOD_SOFTWARE
     * @see StreamerConstants#ENCODE_METHOD_HARDWARE
     */
    public void setAudioEncodeMethod(int encodeMethod) {
        if (mIsRecording) {
            throw new IllegalStateException("Cannot set encode method while recording");
        }
        mAudioEncoderMgt.setEncodeMethod(encodeMethod);
    }

    /**
     * Get audio encode method.
     *
     * @return video encode method.
     * @see StreamerConstants#ENCODE_METHOD_SOFTWARE
     * @see StreamerConstants#ENCODE_METHOD_HARDWARE
     */
    public int getAudioEncodeMethod() {
        return mAudioEncoderMgt.getEncodeMethod();
    }

    /**
     * Set streaming resolution.<br/>
     * <p>
     * The set resolution would take effect on next
     * {@link #startStream()} call.<br/>
     * <p>
     * The set width and height must not be 0 at same time.
     * If one of the params is 0, the other would calculated by the actual preview view size
     * to keep the ratio of the preview view.
     *
     * @param width  streaming width.
     * @param height streaming height.
     */
    public void setTargetResolution(int width, int height) {
        mTargetWidth = width;
        mTargetHeight = height;
    }

    /**
     * Set streaming resolution index.<br/>
     * <p>
     * The set resolution would take effect on next
     * {@link #startStream()} call.
     *
     * @param idx Resolution index.<br/>
     * @see StreamerConstants#VIDEO_RESOLUTION_360P
     * @see StreamerConstants#VIDEO_RESOLUTION_480P
     * @see StreamerConstants#VIDEO_RESOLUTION_540P
     * @see StreamerConstants#VIDEO_RESOLUTION_720P
     */
    public void setTargetResolution(int idx) {
        if (idx < StreamerConstants.VIDEO_RESOLUTION_360P ||
                idx > StreamerConstants.VIDEO_RESOLUTION_720P) {
            throw new IllegalArgumentException("Invalid resolution index");
        }
        mTargetResolution = idx;
    }

    /**
     * Set streaming fps.<br/>
     * <p>
     * The set fps would take effect on next
     * {@link #startStream()} call.<br/>
     * <p>
     * If actual preview fps is larger than set value,
     * the extra frames will be dropped before encoding,
     * and if is smaller than set value, nothing will be done.
     *
     * @param fps frame rate.
     */
    public void setTargetFps(float fps) {
        mTargetFps = fps;
        if (mPreviewFps == 0) {
            mPreviewFps = mTargetFps;
        }
    }

    /**
     * Set key frames interval in seconds.<br/>
     * Would take effect on next {@link #startStream()} call.
     *
     * @param iFrameInterval key frame interval in seconds.
     */
    public void setIFrameInterval(float iFrameInterval) {
        mIFrameInterval = iFrameInterval;
    }

    /**
     * Set video bitrate in bps, and disable video bitrate auto adjustment.<br/>
     * Would take effect on next {@link #startStream()} call.
     *
     * @param bitrate video bitrate in bps
     */
    public void setVideoBitrate(int bitrate) {
        mInitVideoBitrate = bitrate;
        mAutoAdjustVideoBitrate = false;
        StatsLogReport.getInstance().setAutoAdjustVideoBitrate(false);
    }

    /**
     * Set video bitrate in kbps, and disable video bitrate auto adjustment.<br/>
     * Would take effect on next {@link #startStream()} call.
     *
     * @param kBitrate video bitrate in kbps
     */
    public void setVideoKBitrate(int kBitrate) {
        setVideoBitrate(kBitrate * 1024);
    }

    /**
     * Set video init/min/max bitrate in bps, and enable video bitrate auto adjustment.<br/>
     * Would take effect on next {@link #startStream()} call.
     *
     * @param initVideoBitrate init video bitrate in bps.
     * @param maxVideoBitrate  max video bitrate in bps.
     * @param minVideoBitrate  min video bitrate in bps.
     */
    public void setVideoBitrate(int initVideoBitrate, int maxVideoBitrate, int minVideoBitrate) {
        mInitVideoBitrate = initVideoBitrate;
        mMaxVideoBitrate = maxVideoBitrate;
        mMinVideoBitrate = minVideoBitrate;
        mAutoAdjustVideoBitrate = true;
        StatsLogReport.getInstance().setAutoAdjustVideoBitrate(true);
    }

    /**
     * Set video init/min/max bitrate in kbps, and enable video bitrate auto adjustment.<br/>
     * Would take effect on next {@link #startStream()} call.
     *
     * @param initVideoKBitrate init video bitrate in kbps.
     * @param maxVideoKBitrate  max video bitrate in kbps.
     * @param minVideoKBitrate  min video bitrate in kbps.
     */
    public void setVideoKBitrate(int initVideoKBitrate,
                                int maxVideoKBitrate,
                                int minVideoKBitrate) {
        setVideoBitrate(initVideoKBitrate * 1024,
                maxVideoKBitrate * 1024,
                minVideoKBitrate * 1024);
    }

    /**
     * Set audio sample rate while streaming.<br/>
     * Would take effect on next {@link #startStream()} call.
     *
     * @param sampleRate sample rate in Hz.
     */
    public void setAudioSampleRate(int sampleRate) {
        mAudioSampleRate = sampleRate;
    }

    /**
     * Set audio channel number.<br/>
     * Would take effect on next {@link #startStream()} call.
     *
     * @param channels audio channel number, 1 for mono, 2 for stereo.
     */
    public void setAudioChannels(int channels) {
        mAudioChannels = channels;
    }

    /**
     * Set audio bitrate in bps.<br/>
     * Would take effect on next {@link #startStream()} call.
     *
     * @param bitrate audio bitrate in bps.
     */
    public void setAudioBitrate(int bitrate) {
        mAudioBitrate = bitrate;
    }

    /**
     * Set audio bitrate in kbps.<br/>
     * Would take effect on next {@link #startStream()} call.
     *
     * @param kBitrate audio bitrate in kbps.
     */
    public void setAudioKBitrate(int kBitrate) {
        setAudioBitrate(kBitrate * 1024);
    }

    /**
     * Set enable front camera mirror or not while streaming.
     *
     * @param enableMirror true to enable, false to disable.
     * @deprecated use {@link #setFrontCameraMirror(boolean)} instead.
     */
    @Deprecated
    public void setEnableCameraMirror(boolean enableMirror) {
        setFrontCameraMirror(enableMirror);
    }

    /**
     * Set enable front camera mirror or not while streaming.<br/>
     * Would take effect immediately while streaming.
     *
     * @param mirror true to enable, false to disable.
     */
    public void setFrontCameraMirror(boolean mirror) {
        mFrontCameraMirror = mirror;
        updateFrontMirror();
        StatsLogReport.getInstance().setIsFrontCameraMirror(mirror);
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
        if (mScreenRenderWidth == 0 || mScreenRenderHeight == 0) {
            mDelayedStartCameraPreview = true;
        } else {
            setPreviewParams();
            mCameraCapture.start(mCameraFacing);
        }
    }

    /**
     * Stop camera preview.
     */
    public void stopCameraPreview() {
        mCameraCapture.stop();
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
        if (mTargetWidth == 0 && mTargetHeight == 0) {
            int val = getShortEdgeLength(mTargetResolution);
            if (mScreenRenderWidth > mScreenRenderHeight) {
                mTargetHeight = val;
            } else {
                mTargetWidth = val;
            }
        }

        if (mPreviewWidth == 0) {
            mPreviewWidth = mPreviewHeight * mScreenRenderWidth / mScreenRenderHeight;
        } else if (mPreviewHeight == 0) {
            mPreviewHeight = mPreviewWidth * mScreenRenderHeight / mScreenRenderWidth;
        }
        mPreviewWidth = align(mPreviewWidth, 8);
        mPreviewHeight = align(mPreviewHeight, 8);
        if (mTargetWidth == 0) {
            mTargetWidth = mTargetHeight * mScreenRenderWidth / mScreenRenderHeight;
        } else if (mTargetHeight == 0) {
            mTargetHeight = mTargetWidth * mScreenRenderHeight / mScreenRenderWidth;
        }
        mTargetWidth = align(mTargetWidth, 8);
        mTargetHeight = align(mTargetHeight, 8);
        StatsLogReport.getInstance().setTargetResolution(mTargetWidth, mTargetHeight);
    }

    private void updateFrontMirror() {
        if (mCameraFacing == CameraCapture.FACING_FRONT) {
            mImgTexMixer.setMirror(IDX_CAMERA, !mFrontCameraMirror);
            mVideoEncoderMgt.setImgBufMirror(mFrontCameraMirror);
        } else {
            mImgTexMixer.setMirror(IDX_CAMERA, false);
            mVideoEncoderMgt.setImgBufMirror(false);
        }
    }

    private void setAudioParams() {
        mAudioResampleFilter.setOutFormat(new AudioBufFormat(AVConst.AV_SAMPLE_FMT_S16,
                mAudioSampleRate, mAudioChannels));
    }

    private void setPreviewParams() {
        calResolution();
        mWaterMarkCapture.setPreviewSize(mScreenRenderWidth, mScreenRenderHeight);
        mWaterMarkCapture.setTargetSize(mTargetWidth, mTargetHeight);
        mCameraCapture.setOrientation(mRotateDegrees);
        mCameraCapture.setPreviewSize(mPreviewWidth, mPreviewHeight);
        mCameraCapture.setPreviewFps(mPreviewFps);

        mImgTexScaleFilter.setTargetSize(mPreviewWidth, mPreviewHeight);
        mImgTexMixer.setTargetSize(mTargetWidth, mTargetHeight);

        setAudioParams();
    }

    private void setRecordingParams() {
        VideoEncodeFormat videoEncodeFormat = new VideoEncodeFormat(VideoEncodeFormat.MIME_AVC,
                mTargetWidth, mTargetHeight, mInitVideoBitrate);
        videoEncodeFormat.setFramerate(mTargetFps);
        videoEncodeFormat.setIframeinterval(mIFrameInterval);
        mVideoEncoderMgt.setEncodeFormat(videoEncodeFormat);

        AudioEncodeFormat audioEncodeFormat = new AudioEncodeFormat(AudioEncodeFormat.MIME_AAC,
                AVConst.AV_SAMPLE_FMT_S16, mAudioSampleRate, mAudioChannels, mAudioBitrate);
        mAudioEncoderMgt.setEncodeFormat(audioEncodeFormat);

        RtmpPublisher.BwEstConfig bwEstConfig = new RtmpPublisher.BwEstConfig();
        bwEstConfig.initAudioBitrate = mAudioBitrate;
        bwEstConfig.initVideoBitrate = mInitVideoBitrate;
        bwEstConfig.minVideoBitrate = mMinVideoBitrate;
        bwEstConfig.maxVideoBitrate = mMaxVideoBitrate;
        mRtmpPublisher.setBwEstConfig(bwEstConfig);
        mRtmpPublisher.setFramerate(mTargetFps);
        mRtmpPublisher.setVideoBitrate(mMaxVideoBitrate);
        mRtmpPublisher.setAudioBitrate(mAudioBitrate);
    }

    /**
     * Start streaming.<br/>
     * Must be called after {@link #setUrl(String)} and got
     * {@link StreamerConstants#KSY_STREAMER_CAMERA_INIT_DONE} event on
     * {@link OnInfoListener#onInfo(int, int, int)}.
     *
     * @return false if it's already streaming, true otherwise.
     */
    public boolean startStream() {
        if (mIsRecording) {
            return false;
        }
        setAudioParams();
        setRecordingParams();
        mIsRecording = true;
        mAudioCapture.start();
        mCameraCapture.startRecord();
        mRtmpPublisher.connect(mUri);
        return true;
    }

    /**
     * Stop streaming.
     *
     * @return false if it's not streaming, true otherwise.
     */
    public boolean stopStream() {
        if (!mIsRecording) {
            return false;
        }
        mIsRecording = false;
        if (!mIsAudioPreviewing && mAudioCapture.isRecordingState()) {
            mAudioCapture.stop();
        }
        if (mCameraCapture.isRecording()) {
            mCameraCapture.stopRecord();
        }

        mVideoEncoderMgt.getEncoder().stop();
        mAudioEncoderMgt.getEncoder().stop();
        mRtmpPublisher.disconnect();
        return true;
    }

    /**
     * Get is recording started.
     *
     * @return true after start, false otherwise.
     */
    public boolean isRecording() {
        return mIsRecording;
    }

    /**
     * Set if in audio only streaming mode.<br/>
     * If enable audio only before start stream, then disable it while streaming will
     * cause streaming error. Otherwise, start stream with audio only disabled,
     * you can enable or disable it dynamically.
     *
     * @param audioOnly true to enable, false to disable.
     */
    public void setAudioOnly(boolean audioOnly) {
        if (mIsAudioOnly == audioOnly) {
            return;
        }
        if (audioOnly) {
            mVideoEncoderMgt.getSrcPin().disconnect(false);
            if (mIsRecording) {
                mVideoEncoderMgt.getEncoder().stop();
            }
            mRtmpPublisher.setAudioOnly(true);
        } else {
            mVideoEncoderMgt.getSrcPin().connect(mRtmpPublisher.mVideoSink);
            mRtmpPublisher.setAudioOnly(false);
            if (mIsRecording) {
                mVideoEncoderMgt.getEncoder().start();
            }
        }
        mIsAudioOnly = audioOnly;
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
        mMediaPlayerCapture.onPause();
        mCameraCapture.onPause();
    }

    /**
     * Set enable debug log or not.
     *
     * @param enableDebugLog true to enable, false to disable.
     */
    public void enableDebugLog(boolean enableDebugLog) {
        mEnableDebugLog = enableDebugLog;
        StatsLogReport.getInstance().setEnableDebugLog(mEnableDebugLog);
    }

    /**
     * Get encoded frame number.
     *
     * @return Encoded frame number on current streaming session.
     * @see #getVideoEncoderMgt()
     * @see VideoEncoderMgt#getEncoder()
     * @see Encoder#getFrameEncoded()
     */
    public long getEncodedFrames() {
        if (StatsLogReport.getInstance().isPermitLogReport()) {
            return mVideoEncoderMgt.getEncoder().getFrameEncoded();
        } else {
            Log.w(TAG, "you must enableStreamStatModule");
            return 0;
        }
    }

    /**
     * Get dropped frame number.
     *
     * @return Frame dropped number on current streaming session.
     * @see #getVideoEncoderMgt()
     * @see VideoEncoderMgt#getEncoder()
     * @see Encoder#getFrameDropped()
     * @see #getRtmpPublisher()
     * @see RtmpPublisher#getDroppedVideoFrames()
     */
    public int getDroppedFrameCount() {
        if (StatsLogReport.getInstance().isPermitLogReport()) {
            return mVideoEncoderMgt.getEncoder().getFrameDropped() +
                    mRtmpPublisher.getDroppedVideoFrames();
        } else {
            Log.w(TAG, "you must enableStreamStatModule");
            return 0;
        }
    }

    /**
     * Get dns parse time of current or previous streaming session.
     *
     * @return dns parse time in ms.
     * @see #getRtmpPublisher()
     * @see RtmpPublisher#getDnsParseTime()
     */
    public int getDnsParseTime() {
        if (StatsLogReport.getInstance().isPermitLogReport()) {
            return mRtmpPublisher.getDnsParseTime();
        } else {
            Log.w(TAG, "you must enableStreamStatModule");
            return 0;
        }
    }

    /**
     * Get connect time of current or previous streaming session.
     *
     * @return connect time in ms.
     * @see #getRtmpPublisher()
     * @see RtmpPublisher#getConnectTime()
     */
    public int getConnectTime() {
        if (StatsLogReport.getInstance().isPermitLogReport()) {
            return mRtmpPublisher.getConnectTime();
        } else {
            Log.w(TAG, "you must enableStreamStatModule");
            return 0;
        }
    }

    /**
     * Get current upload speed.
     *
     * @return upload speed in kbps.
     * @see #getCurrentUploadKBitrate()
     * @deprecated Use {@link #getCurrentUploadKBitrate()} instead.
     */
    @Deprecated
    public float getCurrentBitrate() {
        if (StatsLogReport.getInstance().isPermitLogReport()) {
            return getCurrentUploadKBitrate();
        } else {
            Log.w(TAG, "you must enableStreamStatModule");
            return 0;
        }
    }

    /**
     * Get current upload speed.
     *
     * @return upload speed in kbps.
     * @see #getRtmpPublisher()
     * @see RtmpPublisher#getCurrentUploadKBitrate()
     */
    public int getCurrentUploadKBitrate() {
        if (StatsLogReport.getInstance().isPermitLogReport()) {
            return mRtmpPublisher.getCurrentUploadKBitrate();
        } else {
            Log.w(TAG, "you must enableStreamStatModule");
            return 0;
        }
    }

    /**
     * Get total uploaded data of current streaming session.
     *
     * @return uploaded data size in kbytes.
     * @see #getRtmpPublisher()
     * @see RtmpPublisher#getUploadedKBytes()
     */
    public int getUploadedKBytes() {
        if (StatsLogReport.getInstance().isPermitLogReport()) {
            return mRtmpPublisher.getUploadedKBytes();
        } else {
            Log.w(TAG, "you must enableStreamStatModule");
            return 0;
        }
    }

    /**
     * Get host ip of current or previous streaming session.
     *
     * @return host ip in format as 120.4.32.122
     * @see #getRtmpPublisher()
     * @see RtmpPublisher#getHostIp()
     */
    public String getRtmpHostIP() {
        if (StatsLogReport.getInstance().isPermitLogReport()) {
            return mRtmpPublisher.getHostIp();
        } else {
            Log.w(TAG, "you must enableStreamStatModule");
            return "";
        }
    }

    /**
     * Set info listener.
     *
     * @param listener info listener
     */
    public void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    /**
     * Set error listener.
     *
     * @param listener error listener
     */
    public void setOnErrorListener(OnErrorListener listener) {
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
     * Get if torch supported on current camera facing.
     *
     * @return true if supported, false if not.
     * @see #getCameraCapture()
     * @see CameraCapture#isTorchSupported()
     */
    public boolean isTorchSupported() {
        return mCameraCapture.isTorchSupported();
    }

    /**
     * Toggle torch of current camera.
     *
     * @param open true to turn on, false to turn off.
     * @return true if success, false if failed or on invalid mState.
     * @see #getCameraCapture()
     * @see CameraCapture#toggleTorch(boolean)
     */
    public boolean toggleTorch(boolean open) {
        return mCameraCapture.toggleTorch(open);
    }

    /**
     * @deprecated Use {@link #startBgm(String, boolean)} instead.
     */
    @Deprecated
    public boolean startMixMusic(String path, boolean loop) {
        startBgm(path, loop);
        return true;
    }

    /**
     * @deprecated Use {@link #stopBgm()} instead.
     */
    @Deprecated
    public boolean stopMixMusic() {
        stopBgm();
        return true;
    }

    /**
     * Start bgm play.
     *
     * @param path bgm path.
     * @param loop true if loop this music, false if not.
     */
    public void startBgm(String path, boolean loop) {
        if (mIsAudioPreviewing) {
            mAudioPlayerCapture.setMute(true);
        }
        mAudioPlayerCapture.start(path, loop);
    }

    /**
     * Stop bgm play.
     */
    public void stopBgm() {
        mAudioPlayerCapture.stop();
    }

    /**
     * Set if headset plugged.
     *
     * @deprecated use {@link #setEnableAudioMix(boolean)} instead.
     * @param isPlugged true if plugged, false if not.
     */
    @Deprecated
    public void setHeadsetPlugged(boolean isPlugged) {
        setEnableAudioMix(isPlugged);
    }

    /**
     * Set if enable audio mix, usually set true when headset plugged.
     *
     * @param enable true to enable, false to disable.
     */
    public void setEnableAudioMix(boolean enable) {
        mEnableAudioMix = enable;
        if (mEnableAudioMix) {
            mAudioPlayerCapture.mSrcPin.connect(mAudioMixer.getSinkPin(1));
        } else {
            mAudioPlayerCapture.mSrcPin.disconnect(mAudioMixer.getSinkPin(1), false);
        }
    }

    /**
     * Set mic volume.
     *
     * @param volume volume in 0~1.0f.
     */
    public void setVoiceVolume(float volume) {
        mAudioMixer.setInputVolume(0, volume);
    }

    /**
     * @deprecated Use {@link #getImgTexFilterMgt()} and
     * {@link ImgTexFilterMgt#setFilter(GLRender, int)} instead.
     */
    @Deprecated
    public void setBeautyFilter(int beautyFilter) {
        mImgTexFilterMgt.setFilter(mGLRender, beautyFilter);
        mVideoEncoderMgt.setEnableImgBufBeauty(
                beautyFilter != RecorderConstants.FILTER_BEAUTY_DISABLE);
    }

    /**
     * Set enable cpu beauty filter.<br/>
     * Only need to set when video encode method is
     * {@link StreamerConstants#ENCODE_METHOD_SOFTWARE_COMPAT}.<br/>
     *
     * @param enable true to enable, false to disable.
     * @see #getVideoEncoderMgt()
     * @see VideoEncoderMgt#getEncodeMethod()
     */
    public void setEnableImgBufBeauty(boolean enable) {
        mVideoEncoderMgt.setEnableImgBufBeauty(enable);
    }

    /**
     * Set if mute audio while streaming.
     *
     * @param isMute true to mute, false to unmute.
     */
    public void setMuteAudio(boolean isMute) {
        if (!mIsAudioPreviewing) {
            mAudioPlayerCapture.setMute(isMute);
            mMediaPlayerCapture.getMediaPlayer().setPlayerMute(isMute ? 1 : 0);
        }
        mAudioMixer.setMute(isMute);
    }

    /**
     * Set if start audio preview.<br/>
     * Should start only when headset plugged.
     *
     * @param enable true to start, false to stop.
     */
    public void setEnableAudioPreview(boolean enable) {
        mIsAudioPreviewing = enable;
        if (enable) {
            mAudioCapture.start();
            mAudioPreview.start();
            mAudioPlayerCapture.setMute(true);
            mMediaPlayerCapture.getMediaPlayer().setPlayerMute(1);
        } else {
            if (!mIsRecording) {
                mAudioCapture.stop();
            }
            mAudioPreview.stop();
            mAudioPlayerCapture.setMute(false);
            mMediaPlayerCapture.getMediaPlayer().setPlayerMute(0);
        }
    }

    /**
     * @deprecated see {@link #setEnableAudioPreview(boolean)}
     */
    @Deprecated
    public void setEnableEarMirror(boolean enableEarMirror) {
        setEnableAudioPreview(enableEarMirror);
    }

    /**
     * Get KSYStreamerConfig instance previous set by setConfig.
     *
     * @return KSYStreamerConfig instance or null.
     * @deprecated
     */
    @Deprecated
    public KSYStreamerConfig getConfig() {
        return mConfig;
    }

    /**
     * @deprecated To implement class extends
     * {@link com.ksyun.media.streamer.filter.imgtex.ImgTexFilter} and set it to
     * {@link ImgTexFilterMgt}.
     */
    @Deprecated
    public void setOnPreviewFrameListener(OnPreviewFrameListener listener) {
        mCameraCapture.setOnPreviewFrameListener(listener);
    }

    /**
     * @deprecated To implement class extends
     * {@link com.ksyun.media.streamer.filter.audio.AudioFilterBase} and set it to
     * {@link AudioFilterMgt}.
     */
    @Deprecated
    public void setOnAudioRawDataListener(OnAudioRawDataListener listener) {
        mAudioCapture.setOnAudioRawDataListener(listener);
    }

    /**
     * Set stat info upstreaming log.
     *
     * @param listener listener
     */
    public void setOnLogEventListener(StatsLogReport.OnLogEventListener listener) {
        StatsLogReport.getInstance().setOnLogEventListener(listener);
    }

    /**
     * Set if enable stat info upstreaming.
     *
     * @param enableStreamStatModule true to enable, false to disable.
     */
    public void setEnableStreamStatModule(boolean enableStreamStatModule) {
        mEnableStreamStatModule = enableStreamStatModule;
        StatsLogReport.getInstance().setIsPermitLogReport(mEnableStreamStatModule);
    }

    public void showBgPicture(Context context, String uri) {
        mPictureCapture.start(context, uri);
    }

    public void hideBgPicture() {
        mPictureCapture.stop();
    }

    public void showBgVideo(String url) {
        if (mIsAudioPreviewing) {
            mMediaPlayerCapture.getMediaPlayer().setPlayerMute(1);
        }
        mMediaPlayerCapture.start(url);
    }

    public void hideBgVideo() {
        mMediaPlayerCapture.stop();
    }

    public void setCameraPreviewRect(float x, float y, float w, float h) {
        mImgTexMixer.setRenderRect(IDX_CAMERA, x, y, w, h, 1.0f);
    }

    /**
     * Set and show watermark logo both on preview and stream. Support jpeg, png.
     *
     * @param path  logo file path.
     *              prefix "file://" for absolute path,
     *              and prefix "assets://" for image resource in assets folder.
     * @param x     x position for left top of logo relative to the video, between 0~1.0.
     * @param y     y position for left top of logo relative to the video, between 0~1.0.
     * @param w     width of logo relative to the video, between 0~1.0, if set to 0,
     *              width would be calculated by h and logo image radio.
     * @param h     height of logo relative to the video, between 0~1.0, if set to 0,
     *              height would be calculated by w and logo image radio.
     * @param alpha alpha value，between 0~1.0
     */
    public void showWaterMarkLogo(String path, float x, float y, float w, float h, float alpha) {
        alpha = Math.max(0.0f, alpha);
        alpha = Math.min(alpha, 1.0f);
        mImgTexMixer.setRenderRect(IDX_WM_LOGO, x, y, w, h, alpha);
        mVideoEncoderMgt.getImgBufMixer().setRenderRect(1, x, y, w, h, alpha);
        mWaterMarkCapture.showLogo(mContext, path, w, h);
    }

    /**
     * Hide watermark logo.
     */
    public void hideWaterMarkLogo() {
        mWaterMarkCapture.hideLogo();
    }

    /**
     * Set and show timestamp both on preview and stream.
     *
     * @param x     x position for left top of timestamp relative to the video, between 0~1.0.
     * @param y     y position for left top of timestamp relative to the video, between 0~1.0.
     * @param w     width of timestamp relative to the video, between 0-1.0,
     *              the height would be calculated automatically.
     * @param color color of timestamp, in ARGB.
     * @param alpha alpha of timestamp，between 0~1.0.
     */
    public void showWaterMarkTime(float x, float y, float w, int color, float alpha) {
        alpha = Math.max(0.0f, alpha);
        alpha = Math.min(alpha, 1.0f);
        mImgTexMixer.setRenderRect(IDX_WM_TIME, x, y, w, 0, alpha);
        mVideoEncoderMgt.getImgBufMixer().setRenderRect(2, x, y, w, 0, alpha);
        mWaterMarkCapture.showTime(color, "yyyy-MM-dd HH:mm:ss", w, 0);
    }

    /**
     * Hide timestamp watermark.
     */
    public void hideWaterMarkTime() {
        mWaterMarkCapture.hideTime();
    }

    /**
     * Get current sdk version.
     *
     * @return version number as 1.0.0.0
     */
    public String getVersion() {
        return StatsConstant.SDK_VERSION_SUB_VALUE;
    }

    /**
     * Release all resources used by KSYStreamer.
     */
    public void release() {
        mCameraCapture.release();
        mAudioCapture.release();
        mWaterMarkCapture.release();
        mAudioPlayerCapture.release();
        mMediaPlayerCapture.release();
    }

    public interface OnInfoListener {
        void onInfo(int what, int msg1, int msg2);
    }

    public interface OnErrorListener {
        void onError(int what, int msg1, int msg2);
    }

    private GLRender.GLRenderListener mGLRenderListener = new GLRender.GLRenderListener() {
        @Override
        public void onReady() {
        }

        @Override
        public void onSizeChanged(int width, int height) {
            mScreenRenderWidth = width;
            mScreenRenderHeight = height;
            if (mDelayedStartCameraPreview) {
                setPreviewParams();
                mCameraCapture.start(mCameraFacing);
                mDelayedStartCameraPreview = false;
            }
        }

        @Override
        public void onDrawFrame() {
        }
    };
}
