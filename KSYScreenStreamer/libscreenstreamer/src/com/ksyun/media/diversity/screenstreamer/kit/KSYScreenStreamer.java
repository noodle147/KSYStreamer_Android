package com.ksyun.media.diversity.screenstreamer.kit;

import com.ksyun.media.diversity.screenstreamer.capture.ScreenCapture;
import com.ksyun.media.streamer.capture.AudioCapture;
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
import com.ksyun.media.streamer.filter.audio.AudioResampleFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgTexMixer;
import com.ksyun.media.streamer.filter.imgtex.ImgTexScaleFilter;
import com.ksyun.media.streamer.framework.AVConst;
import com.ksyun.media.streamer.framework.AudioBufFormat;
import com.ksyun.media.streamer.kit.StreamerConstants;
import com.ksyun.media.streamer.logstats.StatsConstant;
import com.ksyun.media.streamer.logstats.StatsLogReport;
import com.ksyun.media.streamer.publisher.FilePublisher;
import com.ksyun.media.streamer.publisher.Publisher;
import com.ksyun.media.streamer.publisher.PublisherMgt;
import com.ksyun.media.streamer.publisher.RtmpPublisher;
import com.ksyun.media.streamer.util.gles.GLRender;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.WindowManager;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * kit for Screen Record Streamer
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class KSYScreenStreamer {
    private static final String TAG = "KSYScreenStreamer";
    public static final String LIBSCREENSTREAMER_VERSION_VALUE = "1.0.4.0";
    public static final int KSY_STREAMER_SCREEN_RECORD_UNSUPPORTED = -2007;
    public static final int KSY_STREAMER_SCREEN_RECORD_PERMISSION_DENIED = -2008;

    private Context mContext;
    protected int mAudioCaptureType = AudioCapture.AUDIO_CAPTURE_TYPE_AUDIORECORDER;

    protected int mIdxCamera = 0;
    protected int mIdxWmLogo = 1;
    protected int mIdxWmTime = 2;
    protected int mIdxAudioMic = 0;

    private String mUri;  //push url
    protected String mRecordUri;
    private int mOffScreenWidth;
    private int mOffScreenHeight;
    private int mTargetResolution = StreamerConstants.DEFAULT_TARGET_RESOLUTION;
    private int mTargetWidth = 0;
    private int mTargetHeight = 0;
    private float mTargetFps = StreamerConstants.DEFAULT_TARGET_FPS;
    private float mIFrameInterval = StreamerConstants.DEFAULT_IFRAME_INTERVAL;
    private int mVideoCodecId = AVConst.CODEC_ID_AVC;
    private int mEncodeScene = VideoEncodeFormat.ENCODE_SCENE_SHOWSELF;
    private int mEncodeProfile = VideoEncodeFormat.ENCODE_PROFILE_LOW_POWER;
    private int mMaxVideoBitrate = StreamerConstants.DEFAULT_MAX_VIDEO_BITRATE;
    private int mInitVideoBitrate = StreamerConstants.DEFAULT_INIT_VIDEO_BITRATE;
    private int mMinVideoBitrate = StreamerConstants.DEFAILT_MIN_VIDEO_BITRATE;
    private boolean mAutoAdjustVideoBitrate = true;
    protected int mBwEstStrategy = RtmpPublisher.BW_EST_STRATEGY_NORMAL;
    private int mAudioBitrate = StreamerConstants.DEFAULT_AUDIO_BITRATE;
    private int mAudioSampleRate = StreamerConstants.DEFAULT_AUDIO_SAMPLE_RATE;
    private int mAudioChannels = StreamerConstants.DEFAULT_AUDIO_CHANNELS;
    protected int mAudioProfile = AVConst.PROFILE_AAC_HE;
    private boolean mEnableStreamStatModule = true;

    private boolean mIsRecording = false;
    private boolean mIsFileRecording = false;
    private boolean mIsCaptureStarted = false;
    private boolean mIsLandSpace = false;
    private boolean mEnableDebugLog = false;
    private boolean mAutoRestart = false;
    private int mAutoRestartInterval = 3000;

    protected AtomicInteger mAudioUsingCount;

    private KSYScreenStreamer.OnInfoListener mOnInfoListener;
    private KSYScreenStreamer.OnErrorListener mOnErrorListener;

    private GLRender mScreenGLRender;  //screen的gl上下文
    private WaterMarkCapture mWaterMarkCapture;  //水印
    private ImgTexScaleFilter mImgTexScaleFilter; //screen scale
    private ImgTexMixer mImgTexMixer;            //screen mixer

    private ScreenCapture mScreenCapture;   //output video tex data
    private AudioCapture mAudioCapture;
    private VideoEncoderMgt mVideoEncoderMgt;
    private AudioEncoderMgt mAudioEncoderMgt;
    private RtmpPublisher mRtmpPublisher;

    private AudioResampleFilter mAudioResampleFilter;
    private AudioFilterMgt mAudioFilterMgt;
    private AudioMixer mAudioMixer;
    private FilePublisher mFilePublisher;
    private PublisherMgt mPublisherMgt;

    private Handler mMainHandler;
    private final Object mReleaseObject = new Object();
    private Map<Integer, AudioInputBase> mAudioMixerInputs;  //其它音频输入

    public KSYScreenStreamer(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null!");
        }
        mContext = context.getApplicationContext();
        mMainHandler = new Handler(Looper.getMainLooper());
        StatsLogReport.getInstance().setContext(mContext);
        initModules();
    }

    private void initModules() {
        mScreenGLRender = new GLRender();

        // Watermark capture
        mWaterMarkCapture = new WaterMarkCapture(mScreenGLRender);

        //Screen
        mScreenCapture = new ScreenCapture(mContext, mScreenGLRender);
        mImgTexScaleFilter = new ImgTexScaleFilter(mScreenGLRender);  //screen的分辨率和推流分辨率不同
        mImgTexMixer = new ImgTexMixer(mScreenGLRender);

        //connect video data
        mScreenCapture.mImgTexSrcPin.connect(mImgTexScaleFilter.getSinkPin());
        mImgTexScaleFilter.getSrcPin().connect(mImgTexMixer.getSinkPin(mIdxCamera));
        mWaterMarkCapture.mLogoTexSrcPin.connect(mImgTexMixer.getSinkPin(mIdxWmLogo));
        mWaterMarkCapture.mTimeTexSrcPin.connect(mImgTexMixer.getSinkPin(mIdxWmTime));

        // Audio
        mAudioCapture = new AudioCapture(mContext);
        mAudioCapture.setAudioCaptureType(mAudioCaptureType);
        mAudioFilterMgt = new AudioFilterMgt();
        mAudioMixer = new AudioMixer();
        mAudioResampleFilter = new AudioResampleFilter();
        //connect audio data
        mAudioCapture.getSrcPin().connect(mAudioFilterMgt.getSinkPin());
        mAudioFilterMgt.getSrcPin().connect(mAudioResampleFilter.getSinkPin());
        mAudioResampleFilter.getSrcPin().connect(mAudioMixer.getSinkPin(mIdxAudioMic));

        // encoder
        mVideoEncoderMgt = new VideoEncoderMgt(mScreenGLRender);
        mAudioEncoderMgt = new AudioEncoderMgt();
        mImgTexMixer.getSrcPin().connect(mVideoEncoderMgt.getImgTexSinkPin());
        mAudioMixer.getSrcPin().connect(mAudioEncoderMgt.getSinkPin());

        // publisher
        mRtmpPublisher = new RtmpPublisher();
        mFilePublisher = new FilePublisher();
        mFilePublisher.setForceVideoFrameFirst(true);

        mPublisherMgt = new PublisherMgt();
        mAudioEncoderMgt.getSrcPin().connect(mPublisherMgt.getAudioSink());
        mVideoEncoderMgt.getSrcPin().connect(mPublisherMgt.getVideoSink());
        mPublisherMgt.addPublisher(mRtmpPublisher);

        // set listeners
        mScreenGLRender.addListener(new GLRender.GLRenderListener() {
            @Override
            public void onReady() {
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

        WindowManager wm = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        int screenWidth = wm.getDefaultDisplay().getWidth();
        int screenHeight = wm.getDefaultDisplay().getHeight();
        if ((mIsLandSpace && screenWidth < screenHeight) ||
                (!mIsLandSpace) && screenWidth > screenHeight) {
            screenWidth = wm.getDefaultDisplay().getHeight();
            screenHeight = wm.getDefaultDisplay().getWidth();
        }
        setOffscreenPreview(screenWidth, screenHeight);

        // stat
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
                //do not need to auto restart
            }
        });

        mScreenCapture.setOnScreenCaptureListener(new ScreenCapture.OnScreenCaptureListener() {
            @Override
            public void onStarted() {
                Log.d(TAG, "Screen Record Started");
            }

            @Override
            public void onError(int err) {
                if (err != 0) {
                    stopStream();
                }
                int what = 0;
                switch (err) {
                    case ScreenCapture.SCREEN_ERROR_SYSTEM_UNSUPPORTED:
                        what = KSY_STREAMER_SCREEN_RECORD_UNSUPPORTED;
                        break;
                    case ScreenCapture.SCREEN_ERROR_PERMISSION_DENIED:
                        what = KSY_STREAMER_SCREEN_RECORD_PERMISSION_DENIED;
                        break;
                }

                if (mOnErrorListener != null) {
                    mOnErrorListener.onError(what, 0, 0);
                }
                //do not need to auto restart
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
                //do not need to auto restart
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError(what, 0, 0);
                }
            }
        };
        mVideoEncoderMgt.setEncoderListener(encoderListener);
        mAudioEncoderMgt.setEncoderListener(encoderListener);

        mRtmpPublisher.setPubListener(new Publisher.PubListener() {
            @Override
            public void onInfo(int type, long msg) {
                switch (type) {
                    case RtmpPublisher.INFO_CONNECTED:
                        if (!mAudioEncoderMgt.getEncoder().isEncoding()) {
                            mAudioEncoderMgt.getEncoder().start();
                        } else {
                            mRtmpPublisher.setAudioExtra(mAudioEncoderMgt.getEncoder().getExtra());
                        }
                        if (mOnInfoListener != null) {
                            mOnInfoListener.onInfo(
                                    StreamerConstants.KSY_STREAMER_OPEN_STREAM_SUCCESS, 0, 0);
                        }
                        break;
                    case RtmpPublisher.INFO_AUDIO_HEADER_GOT:
                        // start video encoder after audio header got
                        if (!mVideoEncoderMgt.getEncoder().isEncoding()) {
                            mVideoEncoderMgt.start();
                        } else {
                            mRtmpPublisher.setVideoExtra(mVideoEncoderMgt.
                                    getEncoder().getExtra());
                            mVideoEncoderMgt.getEncoder().forceKeyFrame();
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
                        if (!mAutoAdjustVideoBitrate) {
                            break;
                        }
                        msg = msg - mAudioBitrate;
                        msg = Math.min(msg, mMaxVideoBitrate);
                        Log.d(TAG, "Raise video bitrate to " + msg);
                        mVideoEncoderMgt.getEncoder().adjustBitrate((int) msg);
                        if (mOnInfoListener != null) {
                            mOnInfoListener.onInfo(
                                    StreamerConstants.KSY_STREAMER_EST_BW_RAISE, (int) msg, 0);
                        }
                        break;
                    case RtmpPublisher.INFO_EST_BW_DROP:
                        if (!mAutoAdjustVideoBitrate) {
                            break;
                        }
                        msg -= mAudioBitrate;
                        msg = Math.max(msg, mMinVideoBitrate);
                        Log.d(TAG, "Drop video bitrate to " + msg);
                        mVideoEncoderMgt.getEncoder().adjustBitrate((int) msg);
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
                    //do need to auto restart
                    autoRestart();
                }
            }
        });

        mFilePublisher.setPubListener(new Publisher.PubListener() {

            @Override
            public void onInfo(int type, long msg) {
                Log.d(TAG, "file publisher info:" + type);
                switch (type) {
                    case FilePublisher.INFO_OPENED:
                        //start audio encoder first
                        if (!mAudioEncoderMgt.getEncoder().isEncoding()) {
                            mAudioEncoderMgt.getEncoder().start();
                        } else {
                            mFilePublisher.setAudioExtra(mAudioEncoderMgt.getEncoder().getExtra());
                        }
                        if (mOnInfoListener != null) {
                            mOnInfoListener.onInfo(
                                    StreamerConstants.KSY_STREAMER_OPEN_FILE_SUCCESS, 0, 0);
                        }
                        break;
                    case FilePublisher.INFO_AUDIO_HEADER_GOT:
                        // start video encoder after audio header got
                        if (!mVideoEncoderMgt.getEncoder().isEncoding()) {
                            mVideoEncoderMgt.start();
                        } else {
                            mFilePublisher.setVideoExtra(mVideoEncoderMgt.
                                    getEncoder().getExtra());
                            mVideoEncoderMgt.getEncoder().forceKeyFrame();
                        }
                        break;
                    case FilePublisher.INFO_STOPPED:
                        mPublisherMgt.removePublisher(mFilePublisher);
                        mIsFileRecording = false;
                        if (mOnInfoListener != null) {
                            mOnInfoListener.onInfo(
                                    StreamerConstants.KSY_STREAMER_FILE_RECORD_STOPPED, 0, 0);
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onError(int err, long msg) {
                Log.e(TAG, "FilePublisher err=" + err);
                if (err != 0) {
                    stopRecord();
                }

                if (mOnErrorListener != null) {
                    int status;
                    switch (err) {
                        case FilePublisher.FILE_PUBLISHER_ERROR_OPEN_FAILED:
                            status = StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_OPEN_FAILED;
                            break;
                        case FilePublisher.FILE_PUBLISHER_FORMAT_NOT_SUPPORTED:
                            status = StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_FORMAT_NOT_SUPPORTED;
                            break;
                        case FilePublisher.FILE_PUBLISHER_ERROR_WRITE_FAILED:
                            status = StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_WRITE_FAILED;
                            break;
                        case FilePublisher.FILE_PUBLISHER_ERROR_CLOSE_FAILED:
                            status = StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_CLOSE_FAILED;
                            break;
                        default:
                            status = StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_ERROR_UNKNOWN;
                            break;
                    }
                    mOnErrorListener.onError(status, (int) msg, 0);
                }
                //do not need to restart
            }
        });
    }

    /**
     * Get {@link GLRender} instance.
     *
     * @return GLRender instance.
     */
    public GLRender getGLRender() {
        return mScreenGLRender;
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
     * Get {@link AudioFilterMgt} instance to manage audio filters.
     *
     * @return AudioFilterMgt instance
     */
    public AudioFilterMgt getAudioFilterMgt() {
        return mAudioFilterMgt;
    }

    /**
     * Get {@link AudioMixer} instance.
     *
     * @return AudioMixer instance.
     */
    public AudioMixer getAudioMixer() {
        return mAudioMixer;
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
     * Get {@link RtmpPublisher} instance which publish encoded a/v frames throw rtmp protocol.
     *
     * @return RtmpPublisher instance.
     */
    public RtmpPublisher getRtmpPublisher() {
        return mRtmpPublisher;
    }

    /**
     * Set offscreen preview.
     *
     * @param width  offscreen width
     * @param height offscreen height
     */
    public void setOffscreenPreview(int width, int height) {
        if (width <= 0 || height <= 0) {
            Log.e(TAG, "Invalid offscreen resolution " + width + "x" + height);
            return;
        }
        mOffScreenWidth = width;
        mOffScreenHeight = height;
        mScreenGLRender.init(width, height);
    }

    /**
     * Set streaming url.<br/>
     * must set before startStream, must not be null
     * The set url would take effect on the next {@link #startStream()} call.
     *
     * @param url Streaming url to set.
     * @throws IllegalArgumentException
     */
    public void setUrl(String url) throws IllegalArgumentException {
        if (TextUtils.isEmpty(url)) {
            throw new IllegalArgumentException("url can not be null");
        }
        mUri = url;
    }

    /**
     * get streaming url
     *
     * @return streaming url
     */
    public String getUrl() {
        return mUri;
    }

    /**
     * Set encode method for both video and audio.<br/>
     * Must not be set while encoding.
     * default value:ENCODE_METHOD_SOFTWARE
     *
     * @param encodeMethod Encode method.<br/>
     * @throws IllegalArgumentException must not be ENCODE_METHOD_SOFTWARE_COMPAT
     * @throws IllegalStateException
     * @see StreamerConstants#ENCODE_METHOD_SOFTWARE
     * @see StreamerConstants#ENCODE_METHOD_HARDWARE
     */
    public void setEncodeMethod(int encodeMethod) throws IllegalStateException {
        if (encodeMethod == StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT) {
            throw new IllegalArgumentException("not support ENCODE_METHOD_SOFTWARE_COMPAT for screen");
        }
        setVideoEncodeMethod(encodeMethod);
        setAudioEncodeMethod(encodeMethod);
    }

    /**
     * Set encode method for video.<br/>
     * Must not be set while encoding.
     *
     * @param encodeMethod Encode method.<br/>
     * @throws IllegalStateException
     * @throws IllegalArgumentException must not be ENCODE_METHOD_SOFTWARE_COMPAT
     * @see StreamerConstants#ENCODE_METHOD_SOFTWARE
     * @see StreamerConstants#ENCODE_METHOD_HARDWARE
     */
    public void setVideoEncodeMethod(int encodeMethod) {
        if (mIsRecording) {
            throw new IllegalStateException("Cannot set encode method while recording");
        }

        if (encodeMethod == StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT) {
            throw new IllegalArgumentException("not support ENCODE_METHOD_SOFTWARE_COMPAT for screen");
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
     * @throws IllegalArgumentException must not be ENCODE_METHOD_SOFTWARE_COMPAT
     * @see StreamerConstants#ENCODE_METHOD_SOFTWARE
     * @see StreamerConstants#ENCODE_METHOD_HARDWARE
     */
    public void setAudioEncodeMethod(int encodeMethod) throws IllegalStateException {
        if (mIsRecording) {
            throw new IllegalStateException("Cannot set encode method while recording");
        }

        if (encodeMethod == StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT) {
            throw new IllegalArgumentException("not support ENCODE_METHOD_SOFTWARE_COMPAT for screen");
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
     * set landspace fo target streamer
     * Must not be set while encoding.
     *
     * @param isLandspace false PORTRAIT true LANDSPACE
     */
    public void setIsLandspace(boolean isLandspace) {
        boolean isLastLandscape = mIsLandSpace;
        if (isLastLandscape != isLandspace) {
            setOffscreenPreview(mOffScreenHeight, mOffScreenWidth);
            setTargetResolution(mTargetHeight, mTargetWidth);
            mWaterMarkCapture.setTargetSize(mTargetWidth, mTargetHeight);
        }
        mIsLandSpace = isLandspace;
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
     * @throws IllegalArgumentException
     */
    public void setTargetResolution(int width, int height) throws IllegalArgumentException {
        if (width < 0 || height < 0 || (width == 0 && height == 0)) {
            throw new IllegalArgumentException("Invalid resolution");
        }
        Log.d(TAG, "setTargetResolution: " + width + "*" + height);
        mTargetWidth = width;
        mTargetHeight = height;
        calResolution();
        Log.d(TAG, "setTargetResolution: " + mTargetWidth + "*" + mTargetHeight);
        mImgTexScaleFilter.setTargetSize(mTargetWidth, mTargetHeight);
        mImgTexMixer.setTargetSize(mTargetWidth, mTargetHeight);
        mVideoEncoderMgt.setImgBufTargetSize(mTargetWidth, mTargetHeight);
    }

    /**
     * Set streaming resolution index.<br/>
     * <p>
     * The set resolution would take effect immediately if streaming started.<br/>
     *
     * @param idx Resolution index.<br/>
     * @throws IllegalArgumentException
     * @see StreamerConstants#VIDEO_RESOLUTION_360P
     * @see StreamerConstants#VIDEO_RESOLUTION_480P
     * @see StreamerConstants#VIDEO_RESOLUTION_540P
     * @see StreamerConstants#VIDEO_RESOLUTION_720P
     */
    public void setTargetResolution(int idx) throws IllegalArgumentException {
        if (idx < StreamerConstants.VIDEO_RESOLUTION_360P ||
                idx > StreamerConstants.VIDEO_RESOLUTION_1080P) {
            throw new IllegalArgumentException("Invalid resolution index");
        }
        mTargetResolution = idx;
        mTargetWidth = 0;
        mTargetHeight = 0;
        calResolution();
        mImgTexScaleFilter.setTargetSize(mTargetWidth, mTargetHeight);
        mImgTexMixer.setTargetSize(mTargetWidth, mTargetHeight);
        mVideoEncoderMgt.setImgBufTargetSize(mTargetWidth, mTargetHeight);
    }

    /**
     * get streaming width
     *
     * @return streaming width
     */
    public int getTargetWidth() {
        return mTargetWidth;
    }

    /**
     * get streaming height
     *
     * @return streaming height
     */
    public int getTargetHeight() {
        return mTargetHeight;
    }

    /**
     * Set streaming fps.<br/>
     * <p>
     * The set fps would take effect immediately if streaming started.<br/>
     * <p>
     * If actual preview fps is larger than set value,
     * the extra frames will be dropped before encoding,
     * and if is smaller than set value, nothing will be done.
     * default value : 15
     *
     * @param fps frame rate.
     * @throws IllegalArgumentException
     */
    public void setTargetFps(float fps) throws IllegalArgumentException {
        if (fps <= 0) {
            throw new IllegalArgumentException("the fps must > 0");
        }
        mTargetFps = fps;
    }

    /**
     * get streaming fps
     *
     * @return streaming fps
     */
    public float getTargetFps() {
        return mTargetFps;
    }

    /**
     * Set key frames interval in seconds.<br/>
     * Would take effect on next {@link #startStream()} call.
     * default value 3.0f
     *
     * @param iFrameInterval key frame interval in seconds.
     * @throws IllegalArgumentException
     */
    public void setIFrameInterval(float iFrameInterval) throws IllegalArgumentException {
        if (iFrameInterval <= 0) {
            throw new IllegalArgumentException("the IFrameInterval must > 0");
        }

        mIFrameInterval = iFrameInterval;
    }

    /**
     * get key frames interval in seconds
     *
     * @return key frame interval in seconds.
     */
    public float getIFrameInterval() {
        return mIFrameInterval;
    }

    /**
     * Set video bitrate in bps, and disable video bitrate auto adjustment.<br/>
     * Would take effect on next {@link #startStream()} call.
     * default value : 600 * 1000
     *
     * @param bitrate video bitrate in bps
     * @throws IllegalArgumentException
     */
    public void setVideoBitrate(int bitrate) throws IllegalArgumentException {
        if (bitrate <= 0) {
            throw new IllegalArgumentException("the VideoBitrate must > 0");
        }
        mInitVideoBitrate = bitrate;
        mMaxVideoBitrate = bitrate;
        mMinVideoBitrate = bitrate;
        mAutoAdjustVideoBitrate = false;
    }

    /**
     * Set video bitrate in kbps, and disable video bitrate auto adjustment.<br/>
     * Would take effect on next {@link #startStream()} call.
     *
     * @param kBitrate video bitrate in kbps
     * @throws IllegalArgumentException
     */
    public void setVideoKBitrate(int kBitrate) throws IllegalArgumentException {
        setVideoBitrate(kBitrate * 1000);
    }

    /**
     * Set video init/min/max bitrate in bps, and enable video bitrate auto adjustment.<br/>
     * Would take effect on next {@link #startStream()} call.
     *
     * @param initVideoBitrate init video bitrate in bps. default value 600 * 1000
     * @param maxVideoBitrate  max video bitrate in bps. default value 800 * 1000
     * @param minVideoBitrate  min video bitrate in bps. default value 200 * 1000
     * @throws IllegalArgumentException
     */
    public void setVideoBitrate(int initVideoBitrate, int maxVideoBitrate, int minVideoBitrate)
            throws IllegalArgumentException {
        if (initVideoBitrate <= 0 || maxVideoBitrate <= 0) {
            throw new IllegalArgumentException("the initial and max VideoBitrate must > 0");
        }
        if (minVideoBitrate < 0) {
            throw new IllegalArgumentException("the min VideoBitrate must >= 0");
        }

        mInitVideoBitrate = initVideoBitrate;
        mMaxVideoBitrate = maxVideoBitrate;
        mMinVideoBitrate = minVideoBitrate;
        mAutoAdjustVideoBitrate = true;
    }

    /**
     * Set video init/min/max bitrate in kbps, and enable video bitrate auto adjustment.<br/>
     * Would take effect on next {@link #startStream()} call.
     *
     * @param initVideoKBitrate init video bitrate in kbps.
     * @param maxVideoKBitrate  max video bitrate in kbps.
     * @param minVideoKBitrate  min video bitrate in kbps.
     * @throws IllegalArgumentException
     */
    public void setVideoKBitrate(int initVideoKBitrate,
                                 int maxVideoKBitrate,
                                 int minVideoKBitrate)
            throws IllegalArgumentException {
        setVideoBitrate(initVideoKBitrate * 1000,
                maxVideoKBitrate * 1000,
                minVideoKBitrate * 1000);
    }

    /**
     * Set streaming bandwidth estimate strategy.<br/>
     * Would take effect on next {@link #startStream()} call.
     *
     * @param strategy strategy to set
     * @see RtmpPublisher#BW_EST_STRATEGY_NORMAL
     * @see RtmpPublisher#BW_EST_STRATEGY_NEGATIVE
     */
    public void setBwEstStrategy(int strategy) {
        mBwEstStrategy = strategy;
    }

    /**
     * Get current streaming bandwidth estimate strategy.<br/>
     *
     * @return strategy in use.
     * @see RtmpPublisher#BW_EST_STRATEGY_NORMAL
     * @see RtmpPublisher#BW_EST_STRATEGY_NEGATIVE
     */
    public int getBwEstStrategy() {
        return mBwEstStrategy;
    }

    /**
     * get init video bit rate
     *
     * @return init video bit rate
     */
    public int getInitVideoBitrate() {
        return mInitVideoBitrate;
    }

    /**
     * get min video bit rate
     *
     * @return min video bit rate
     */
    public int getMinVideoBitrate() {
        return mMinVideoBitrate;
    }

    /**
     * get max video bit rate
     *
     * @return max video bit rate
     */
    public int getMaxVideoBitrate() {
        return mMaxVideoBitrate;
    }

    /**
     * check if is auto adjust video bit rate
     *
     * @return true if enabled false if disabled
     */
    public boolean isAutoAdjustVideoBitrate() {
        return mAutoAdjustVideoBitrate;
    }

    /**
     * Set codec id to video encoder.
     *
     * @param codecId video codec id to set.
     * @see AVConst#CODEC_ID_AVC
     * @see AVConst#CODEC_ID_HEVC
     */
    public void setVideoCodecId(int codecId) throws IllegalArgumentException {
        if (codecId != AVConst.CODEC_ID_AVC &&
                codecId != AVConst.CODEC_ID_HEVC) {
            throw new IllegalArgumentException("input video codecid error");
        }
        mVideoCodecId = codecId;
    }

    /**
     * Get video encoder codec id.
     *
     * @return video codec id
     */
    public int getVideoCodecId() {
        return mVideoCodecId;
    }

    /**
     * Set scene mode to video encoder.
     * <p>
     * Only valid in ENCODE_METHOD_SOFTWARE and ENCODE_METHOD_SOFTWARE_COMPAT mode.
     *
     * @param scene scene mode to be set,
     *              default value {@link VideoEncodeFormat#ENCODE_SCENE_SHOWSELF}
     * @see VideoEncodeFormat#ENCODE_SCENE_DEFAULT
     * @see VideoEncodeFormat#ENCODE_SCENE_SHOWSELF
     * @see VideoEncodeFormat#ENCODE_SCENE_GAME
     */
    public void setVideoEncodeScene(int scene) {
        mEncodeScene = scene;
    }

    /**
     * Get scene mode for video encoder.
     *
     * @return scene mode
     */
    public int getVideoEncodeScene() {
        return mEncodeScene;
    }

    /**
     * Set encode profile to video encoder.
     * <p>
     * Only valid in ENCODE_METHOD_SOFTWARE and ENCODE_METHOD_SOFTWARE_COMPAT mode.
     *
     * @param profile encode profile mode to be set,
     *                default value {@link VideoEncodeFormat#ENCODE_PROFILE_LOW_POWER}
     * @see VideoEncodeFormat#ENCODE_PROFILE_LOW_POWER
     * @see VideoEncodeFormat#ENCODE_PROFILE_BALANCE
     * @see VideoEncodeFormat#ENCODE_PROFILE_HIGH_PERFORMANCE
     */
    public void setVideoEncodeProfile(int profile) {
        mEncodeProfile = profile;
    }

    /**
     * Get encode profile for video encoder.
     *
     * @return encode profile mode
     */
    public int getVideoEncodeProfile() {
        return mEncodeProfile;
    }

    /**
     * Set audio sample rate while streaming.<br/>
     * Would take effect on next {@link #startStream()} call.
     * default value 44100
     *
     * @param sampleRate sample rate in Hz.
     * @throws IllegalArgumentException
     */
    public void setAudioSampleRate(int sampleRate) throws IllegalArgumentException {
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("the AudioSampleRate must > 0");
        }

        mAudioSampleRate = sampleRate;
    }

    /**
     * Set audio channel number.<br/>
     * Would take effect on next {@link #startStream()} call.
     * default value : 1
     *
     * @param channels audio channel number, 1 for mono, 2 for stereo.
     * @throws IllegalArgumentException
     */
    public void setAudioChannels(int channels) throws IllegalArgumentException {
        if (channels != 1 && channels != 2) {
            throw new IllegalArgumentException("the AudioChannels must be mono or stereo");
        }

        mAudioChannels = channels;
    }

    /**
     * Set audio bitrate in bps.<br/>
     * Would take effect on next {@link #startStream()} call.
     * default value : 48 * 1000
     *
     * @param bitrate audio bitrate in bps.
     * @throws IllegalArgumentException
     */
    public void setAudioBitrate(int bitrate) throws IllegalArgumentException {
        if (bitrate <= 0) {
            throw new IllegalArgumentException("the AudioBitrate must >0");
        }

        mAudioBitrate = bitrate;
    }

    /**
     * Set audio bitrate in kbps.<br/>
     * Would take effect on next {@link #startStream()} call.
     *
     * @param kBitrate audio bitrate in kbps.
     * @throws IllegalArgumentException
     */
    public void setAudioKBitrate(int kBitrate) throws IllegalArgumentException {
        setAudioBitrate(kBitrate * 1000);
    }

    /**
     * Set audio encode profile.
     *
     * @param profile profile to set.
     * @see AVConst#PROFILE_AAC_LOW
     * @see AVConst#PROFILE_AAC_HE
     * @see AVConst#PROFILE_AAC_HE_V2
     */
    public void setAudioEncodeProfile(int profile) {
        mAudioProfile = profile;
    }

    /**
     * Get audio encode profile.
     *
     * @return current audio encode profile
     * @see AVConst#PROFILE_AAC_LOW
     * @see AVConst#PROFILE_AAC_HE
     * @see AVConst#PROFILE_AAC_HE_V2
     */
    public int getAudioEncodeProfile() {
        return mAudioProfile;
    }

    /**
     * get audio bitrate in bps.
     *
     * @return audio bitrate in bps
     */
    public int getAudioBitrate() {
        return mAudioBitrate;
    }

    /**
     * get audio sample rate.
     *
     * @return audio sample rate in hz
     */
    public int getAudioSampleRate() {
        return mAudioSampleRate;
    }

    /**
     * get audio channel number
     *
     * @return audio channel number
     */
    public int getAudioChannels() {
        return mAudioChannels;
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
        WindowManager wm = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        int screenWidth = wm.getDefaultDisplay().getWidth();
        int screenHeight = wm.getDefaultDisplay().getHeight();

        if ((mIsLandSpace && screenWidth < screenHeight) ||
                (!mIsLandSpace) && screenWidth > screenHeight) {
            screenWidth = wm.getDefaultDisplay().getHeight();
            screenHeight = wm.getDefaultDisplay().getWidth();
        }

        if (mTargetWidth == 0 && mTargetHeight == 0) {
            int val = getShortEdgeLength(mTargetResolution);
            if (screenWidth > screenHeight) {
                mTargetHeight = val;
            } else {
                mTargetWidth = val;
            }
        }

        if (mTargetWidth == 0) {
            mTargetWidth = mTargetHeight * screenWidth / screenHeight;
        } else if (mTargetHeight == 0) {
            mTargetHeight = mTargetWidth * screenHeight / screenWidth;
        }
        mTargetWidth = align(mTargetWidth, 8);
        mTargetHeight = align(mTargetHeight, 8);
    }

    private void setAudioParams() {
        mAudioResampleFilter.setOutFormat(new AudioBufFormat(AVConst.AV_SAMPLE_FMT_S16,
                mAudioSampleRate, mAudioChannels));
    }

    private void setRecordingParams() {
        calResolution();
        mImgTexScaleFilter.setTargetSize(mTargetWidth, mTargetHeight);
        mImgTexMixer.setTargetSize(mTargetWidth, mTargetHeight);
        mWaterMarkCapture.setTargetSize(mTargetWidth, mTargetHeight);
        mWaterMarkCapture.setPreviewSize(mTargetWidth, mTargetHeight);

        VideoEncodeFormat videoEncodeFormat = new VideoEncodeFormat(mVideoCodecId,
                mTargetWidth, mTargetHeight, mInitVideoBitrate);
        if (mTargetFps == 0) {
            mTargetFps = CameraCapture.DEFAULT_PREVIEW_FPS;
        }
        videoEncodeFormat.setFramerate(mTargetFps);
        videoEncodeFormat.setIframeinterval(mIFrameInterval);
        videoEncodeFormat.setScene(mEncodeScene);
        videoEncodeFormat.setProfile(mEncodeProfile);
        mVideoEncoderMgt.setEncodeFormat(videoEncodeFormat);

        // AAC-HE, AAC-HEv2 force use SOFT_ENCODING
        if (mAudioProfile != AVConst.PROFILE_AAC_LOW) {
            mAudioEncoderMgt.setEncodeMethod(AudioEncoderMgt.METHOD_SOFTWARE);
        }
        AudioEncodeFormat audioEncodeFormat = new AudioEncodeFormat(AVConst.CODEC_ID_AAC,
                AVConst.AV_SAMPLE_FMT_S16, mAudioSampleRate, mAudioChannels, mAudioBitrate);
        audioEncodeFormat.setProfile(mAudioProfile);
        mAudioEncoderMgt.setEncodeFormat(audioEncodeFormat);

        RtmpPublisher.BwEstConfig bwEstConfig = new RtmpPublisher.BwEstConfig();
        bwEstConfig.strategy = mBwEstStrategy;
        bwEstConfig.initAudioBitrate = mAudioBitrate;
        bwEstConfig.initVideoBitrate = mInitVideoBitrate;
        bwEstConfig.minVideoBitrate = mMinVideoBitrate;
        bwEstConfig.maxVideoBitrate = mMaxVideoBitrate;
        bwEstConfig.isAdjustBitrate = mAutoAdjustVideoBitrate;
        mRtmpPublisher.setBwEstConfig(bwEstConfig);
        mRtmpPublisher.setFramerate(mTargetFps);
        mRtmpPublisher.setVideoBitrate(mMaxVideoBitrate);
        mRtmpPublisher.setAudioBitrate(mAudioBitrate);

        mFilePublisher.setVideoBitrate(mInitVideoBitrate);
        mFilePublisher.setAudioBitrate(mAudioBitrate);
        mFilePublisher.setFramerate(mTargetFps);
    }

    /**
     * Start streaming.<br/>
     * Must be called after {@link #setUrl(String)}
     *
     * @return false if it's already streaming, true otherwise.
     */
    public boolean startStream() {
        if (mIsRecording) {
            return false;
        }

        mIsRecording = true;
        startCapture();
        mRtmpPublisher.connect(mUri);
        return true;
    }

    public boolean startRecord(String recordUrl) {
        if (mIsFileRecording || TextUtils.isEmpty(recordUrl)) {
            return false;
        }
        mRecordUri = recordUrl;
        mIsFileRecording = true;
        mFilePublisher.startRecording(recordUrl);
        // should connect FilePublisher after startRecord called
        mPublisherMgt.addPublisher(mFilePublisher);
        startCapture();
        return true;
    }

    public void stopRecord() {
        if (!mIsFileRecording) {
            return;
        }
        if (mIsRecording || !mVideoEncoderMgt.getEncoder().isEncoding() ||
                !mAudioEncoderMgt.getEncoder().isEncoding()) {
            mFilePublisher.stop();
        } else {
            stopCapture();
        }
    }

    private void startCapture() {
        if (mIsCaptureStarted) {
            return;
        }
        mIsCaptureStarted = true;
        setAudioParams();
        setRecordingParams();
        startAudioCapture();
        mScreenCapture.start();
    }

    private void stopCapture() {
        if (!mIsCaptureStarted) {
            return;
        }
        mIsCaptureStarted = false;
        stopAudioCapture();

        if (!mIsRecording) {
            mVideoEncoderMgt.getEncoder().flush();
            mAudioEncoderMgt.getEncoder().flush();
        }
        mVideoEncoderMgt.stop();
        mAudioEncoderMgt.getEncoder().stop();
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
        if (!mIsFileRecording) {
            stopCapture();
        }
        mIsRecording = false;
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

    public boolean isFileRecording() {
        return mIsFileRecording;
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
        return mVideoEncoderMgt.getEncoder().getFrameEncoded();
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
        return mVideoEncoderMgt.getEncoder().getFrameDropped() +
                mRtmpPublisher.getDroppedVideoFrames();
    }

    /**
     * Get dns parse time of current or previous streaming session.
     *
     * @return dns parse time in ms.
     * @see #getRtmpPublisher()
     * @see RtmpPublisher#getDnsParseTime()
     */
    public int getDnsParseTime() {
        return mRtmpPublisher.getDnsParseTime();
    }

    /**
     * Get connect time of current or previous streaming session.
     *
     * @return connect time in ms.
     * @see #getRtmpPublisher()
     * @see RtmpPublisher#getConnectTime()
     */
    public int getConnectTime() {
        return mRtmpPublisher.getConnectTime();
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
        return getCurrentUploadKBitrate();
    }

    /**
     * Get current upload speed.
     *
     * @return upload speed in kbps.
     * @see #getRtmpPublisher()
     * @see RtmpPublisher#getCurrentUploadKBitrate()
     */
    public int getCurrentUploadKBitrate() {
        return mRtmpPublisher.getCurrentUploadKBitrate();
    }

    /**
     * Get total uploaded data of current streaming session.
     *
     * @return uploaded data size in kbytes.
     * @see #getRtmpPublisher()
     * @see RtmpPublisher#getUploadedKBytes()
     */
    public int getUploadedKBytes() {
        return mRtmpPublisher.getUploadedKBytes();
    }

    /**
     * Get host ip of current or previous streaming session.
     *
     * @return host ip in format as 120.4.32.122
     * @see #getRtmpPublisher()
     * @see RtmpPublisher#getHostIp()
     */
    public String getRtmpHostIP() {
        return mRtmpPublisher.getHostIp();
    }

    /**
     * Set info listener.
     *
     * @param listener info listener
     */
    public void setOnInfoListener(KSYScreenStreamer.OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    /**
     * Set error listener.
     *
     * @param listener error listener
     */
    public void setOnErrorListener(KSYScreenStreamer.OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    /**
     * Set mic volume.
     *
     * @param volume volume in 0~1.0f, greater than 1.0f also acceptable.
     */
    public void setVoiceVolume(float volume) {
        mAudioCapture.setVolume(volume);
    }

    /**
     * get mic volume
     *
     * @return volume in 0~1.0f, also could be greater than 1.0.
     */
    public float getVoiceVolume() {
        return mAudioCapture.getVolume();
    }

    /**
     * Set if mute audio while streaming.
     *
     * @param isMute true to mute, false to unmute.
     */
    public void setMuteAudio(boolean isMute) {
        mAudioMixer.setMute(isMute);
    }


    /**
     * check if audio is muted or not.
     *
     * @return
     */
    public boolean isAudioMuted() {
        return mAudioMixer.getMute();
    }

    /**
     * auto restart streamer when the following error occurred
     *
     * @param enable   default false
     * @param interval the restart interval(ms) default 3000
     * @see StreamerConstants#KSY_STREAMER_ERROR_CONNECT_BREAKED
     * @see StreamerConstants#KSY_STREAMER_ERROR_DNS_PARSE_FAILED
     * @see StreamerConstants#KSY_STREAMER_ERROR_CONNECT_FAILED
     * @see StreamerConstants#KSY_STREAMER_ERROR_PUBLISH_FAILED
     * @see StreamerConstants#KSY_STREAMER_ERROR_AV_ASYNC
     */
    public void setEnableAutoRestart(boolean enable, int interval) {
        mAutoRestart = enable;
        mAutoRestartInterval = interval;
    }

    public boolean getEnableAutoRestart() {
        return mAutoRestart;
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
     * @param alpha alpha value between 0~1.0
     */
    public void showWaterMarkLogo(String path, float x, float y, float w, float h, float alpha) {
        if (!mIsRecording) {
            Log.e(TAG, "Should be called after startStream");
            return;
        }
        alpha = Math.max(0.0f, alpha);
        alpha = Math.min(alpha, 1.0f);
        mImgTexMixer.setRenderRect(mIdxWmLogo, x, y, w, h, alpha);
        mVideoEncoderMgt.getImgBufMixer().setRenderRect(1, x, y, w, h, alpha);
        mWaterMarkCapture.showLogo(mContext, path, w, h);
    }

    /**
     * Show watermark logo both on preview and stream.
     *
     * @param bitmap logo bitmap, should not be recycled by caller
     * @param x      x position for left top of logo relative to the video, between 0~1.0.
     * @param y      y position for left top of logo relative to the video, between 0~1.0.
     * @param w      width of logo relative to the video, between 0~1.0, if set to 0,
     *               width would be calculated by h and logo image radio.
     * @param h      height of logo relative to the video, between 0~1.0, if set to 0,
     *               height would be calculated by w and logo image radio.
     * @param alpha  alpha value between 0~1.0
     */
    public void showWaterMarkLogo(Bitmap bitmap, float x, float y, float w, float h, float alpha) {
        alpha = Math.max(0.0f, alpha);
        alpha = Math.min(alpha, 1.0f);
        mImgTexMixer.setRenderRect(mIdxWmLogo, x, y, w, h, alpha);
        mVideoEncoderMgt.getImgBufMixer().setRenderRect(1, x, y, w, h, alpha);
        mWaterMarkCapture.showLogo(bitmap, w, h);
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
        if (!mIsRecording) {
            Log.e(TAG, "Should be called after startStream");
            return;
        }
        alpha = Math.max(0.0f, alpha);
        alpha = Math.min(alpha, 1.0f);
        mImgTexMixer.setRenderRect(mIdxWmTime, x, y, w, 0, alpha);
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
     * Release all resources used by KSYScreenStreamer.
     */
    public void release() {
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }
        synchronized (mReleaseObject) {
            mWaterMarkCapture.release();
            mScreenCapture.release();
            mAudioCapture.release();
            mScreenGLRender.release();
            setOnLogEventListener(null);
        }
    }

    /**
     * Get current KSYStreamer sdk version.
     *
     * @return version number as 1.0.0.0
     */
    public String getKSYStreamerSDKVersion() {
        return StatsConstant.SDK_VERSION_VALUE;
    }

    /**
     * get libscreenstreamer version
     *
     * @return
     */
    public String getLibScreenStreamerVersion() {
        return LIBSCREENSTREAMER_VERSION_VALUE;
    }

    private void autoRestart() {
        if (mAutoRestart) {
            if (mMainHandler != null) {
                stopStream();
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (mReleaseObject) {
                            if (mMainHandler != null) {
                                startStream();
                            }
                        }
                    }
                }, mAutoRestartInterval);
            }
        }
    }

    protected void stopAudioCapture() {
        if (mAudioUsingCount == null) {
            mAudioUsingCount = new AtomicInteger(0);
        }
        if (mAudioUsingCount.get() == 0) {
            return;
        }

        if (mAudioUsingCount.decrementAndGet() == 0) {
            mAudioCapture.stop();
        }
    }

    protected void startAudioCapture() {
        // May be used another audio capture
        if (mAudioCapture.getSrcPin().isConnected()) {
            if (mAudioUsingCount == null) {
                mAudioUsingCount = new AtomicInteger(0);
            }
            if (mAudioUsingCount.getAndIncrement() == 0) {
                mAudioCapture.start();
            }
        }
    }

    /**
     * 输入需要mixer的音频
     * 当前支持最多mixer 7个除mic以外的音频
     * 既可以调用该接口7次来输入需要mixer的不同的音频
     *
     * @param input 需要mixer的音频
     * @return true 输入成功  false 输入失败
     */
    public boolean connectAudioInput(AudioInputBase input) {
        if (mAudioMixerInputs == null) {
            mAudioMixerInputs = new ArrayMap<>();
        }

        if (mAudioMixerInputs.size() >= (mAudioMixer.getSinkPinNum() - 1)) {
            return false;
        }

        if (!mAudioMixerInputs.containsValue(input)) {
            if (mAudioMixer != null) {
                int index = mAudioMixer.getEmptySinkPin();
                if (index != -1) {
                    input.getSrcPin().connect(mAudioMixer.getSinkPin(index));
                    mAudioMixerInputs.put(index, input);
                    return true;
                }
            }
        } else {
            if (mAudioMixer != null) {
                int index = findIndex(input);
                if (index != -1) {
                    input.getSrcPin().connect(mAudioMixer.getSinkPin(index));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 停止mixer
     *
     * @param inputBase 需要mixer的音频
     * @param recursive true 释放该音频输入，下次connect无效，需要重新创建
     */
    public void disconnectAudioInput(AudioInputBase inputBase, boolean recursive) {
        if (mAudioMixerInputs != null && mAudioMixerInputs.containsValue(inputBase)) {
            if (mAudioMixer != null) {
                int index = findIndex(inputBase);
                if (index != -1) {
                    inputBase.getSrcPin().disconnect(mAudioMixer.getSinkPin(index),
                            recursive);
                    if (recursive) {
                        mAudioMixerInputs.remove(inputBase);
                    }
                }
            }
        }
    }

    private int findIndex(AudioInputBase inputBase) {
        int index = -1;
        for (int i = 0; i < mAudioMixerInputs.size(); i++) {
            if (mAudioMixerInputs.get(i) == inputBase) {
                index = i;
                break;
            }
        }
        return index;
    }

    public interface OnInfoListener {
        void onInfo(int what, int msg1, int msg2);
    }

    public interface OnErrorListener {
        void onError(int what, int msg1, int msg2);
    }
}
