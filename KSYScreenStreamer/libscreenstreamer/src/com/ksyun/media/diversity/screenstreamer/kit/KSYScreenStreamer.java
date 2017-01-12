package com.ksyun.media.diversity.screenstreamer.kit;

import com.ksyun.media.diversity.screenstreamer.capture.ScreenCapture;
import com.ksyun.media.streamer.capture.AudioCapture;
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
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

/**
 * kit for Screen Record Streamer
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class KSYScreenStreamer {
    private static final String TAG = "KSYScreenStreamer";
    public static final String LIBSCREENSTREAMER_VERSION_VALUE = "1.0.1.1";
    public static final int KSY_STREAMER_SCREEN_RECORD_UNSUPPORTED = -2007;
    public static final int KSY_STREAMER_SCREEN_RECORD_PERMISSION_DENIED = -2008;

    private Context mContext;

    private String mUri;  //push url
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
    private int mAudioBitrate = StreamerConstants.DEFAULT_AUDIO_BITRATE;
    private int mAudioSampleRate = StreamerConstants.DEFAULT_AUDIO_SAMPLE_RATE;
    private int mAudioChannels = StreamerConstants.DEFAULT_AUDIO_CHANNELS;
    private boolean mEnableStreamStatModule = true;

    private boolean mIsRecording = false;
    private boolean mIsFileRecording = false;
    private boolean mIsCaptureStarted = false;
    private boolean mIsLandSpace = false;
    private boolean mEnableDebugLog = false;

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

    public KSYScreenStreamer(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null!");
        }
        mContext = context.getApplicationContext();
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
        mImgTexScaleFilter.getSrcPin().connect(mImgTexMixer.getSinkPin(0));
        mWaterMarkCapture.mLogoTexSrcPin.connect(mImgTexMixer.getSinkPin(1));
        mWaterMarkCapture.mTimeTexSrcPin.connect(mImgTexMixer.getSinkPin(2));

        // Audio
        mAudioCapture = new AudioCapture();
        mAudioResampleFilter = new AudioResampleFilter();
        mAudioFilterMgt = new AudioFilterMgt();
        mAudioMixer = new AudioMixer();
        //connect audio data
        mAudioCapture.mAudioBufSrcPin.connect(mAudioResampleFilter.getSinkPin());
        mAudioResampleFilter.getSrcPin().connect(mAudioFilterMgt.getSinkPin());
        mAudioFilterMgt.getSrcPin().connect(mAudioMixer.getSinkPin(0));

        // encoder
        mVideoEncoderMgt = new VideoEncoderMgt(mScreenGLRender);
        mAudioEncoderMgt = new AudioEncoderMgt();
        mImgTexMixer.getSrcPin().connect(mVideoEncoderMgt.getImgTexSinkPin());
        mAudioMixer.getSrcPin().connect(mAudioEncoderMgt.getSinkPin());

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

        // publisher
        mRtmpPublisher = new RtmpPublisher();
        mFilePublisher = new FilePublisher();

        mPublisherMgt = new PublisherMgt();
        mAudioEncoderMgt.getSrcPin().connect(mPublisherMgt.getAudioSink());
        mVideoEncoderMgt.getSrcPin().connect(mPublisherMgt.getVideoSink());
        mPublisherMgt.addPublisher(mFilePublisher);
        mPublisherMgt.addPublisher(mRtmpPublisher);

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

        mRtmpPublisher.setPubListener(new Publisher.PubListener() {
            @Override
            public void onInfo(int type, long msg) {
                switch (type) {
                    case RtmpPublisher.INFO_CONNECTED:
                        if (!mAudioEncoderMgt.getEncoder().isEncoding()) {
                            mAudioEncoderMgt.getEncoder().start();
                        }
                        mAudioEncoderMgt.getEncoder().sendExtraData();
                        if (mOnInfoListener != null) {
                            mOnInfoListener.onInfo(
                                    StreamerConstants.KSY_STREAMER_OPEN_STREAM_SUCCESS, 0, 0);
                        }
                        break;
                    case RtmpPublisher.INFO_AUDIO_HEADER_GOT:
                        // start video encoder after audio header got
                        if (!mVideoEncoderMgt.getEncoder().isEncoding()) {
                            mVideoEncoderMgt.start();
                        }
                        mVideoEncoderMgt.getEncoder().sendExtraData();
                        mVideoEncoderMgt.getEncoder().forceKeyFrame();
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

        mFilePublisher.setPubListener(new Publisher.PubListener() {

            @Override
            public void onInfo(int type, long msg) {
                switch (type) {
                    case FilePublisher.INFO_OPENED:
                        //start audio encoder first
                        mAudioEncoderMgt.getEncoder().start();
                        mAudioEncoderMgt.getEncoder().sendExtraData();
                        if (mOnInfoListener != null) {
                            mOnInfoListener.onInfo(
                                    StreamerConstants.KSY_STREAMER_OPEN_STREAM_SUCCESS, 0, 0);
                        }
                        break;
                    case FilePublisher.INFO_AUDIO_HEADER_GOT:

                        // start video encoder after audio header got
                        mVideoEncoderMgt.start();
                        mVideoEncoderMgt.getEncoder().sendExtraData();
                        mVideoEncoderMgt.getEncoder().forceKeyFrame();

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
     * @throws IllegalArgumentException
     */
    public void setOffscreenPreview(int width, int height) throws IllegalArgumentException {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid offscreen resolution");
        }
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
     * @throws IllegalArgumentException
     * @see StreamerConstants#VIDEO_RESOLUTION_360P
     * @see StreamerConstants#VIDEO_RESOLUTION_480P
     * @see StreamerConstants#VIDEO_RESOLUTION_540P
     * @see StreamerConstants#VIDEO_RESOLUTION_720P
     */
    public void setTargetResolution(int idx) throws IllegalArgumentException {
        if (idx < StreamerConstants.VIDEO_RESOLUTION_360P ||
                idx > StreamerConstants.VIDEO_RESOLUTION_720P) {
            throw new IllegalArgumentException("Invalid resolution index");
        }
        mTargetResolution = idx;
        mTargetWidth = 0;
        mTargetHeight = 0;
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
     * The set fps would take effect on next
     * {@link #startStream()} call.<br/>
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
        setVideoBitrate(kBitrate * 1024);
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
        if (initVideoBitrate <= 0 || maxVideoBitrate <= 0 || minVideoBitrate <= 0) {
            throw new IllegalArgumentException("the VideoBitrate must > 0");
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
        setVideoBitrate(initVideoKBitrate * 1024,
                maxVideoKBitrate * 1024,
                minVideoKBitrate * 1024);
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
    public void setVideoCodecId(int codecId) {
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
        setAudioBitrate(kBitrate * 1024);
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
        videoEncodeFormat.setFramerate(mTargetFps);
        videoEncodeFormat.setIframeinterval(mIFrameInterval);
        videoEncodeFormat.setScene(mEncodeScene);
        videoEncodeFormat.setProfile(mEncodeProfile);
        mVideoEncoderMgt.setEncodeFormat(videoEncodeFormat);

        AudioEncodeFormat audioEncodeFormat = new AudioEncodeFormat(AudioEncodeFormat.MIME_AAC,
                AVConst.AV_SAMPLE_FMT_S16, mAudioSampleRate, mAudioChannels, mAudioBitrate);
        mAudioEncoderMgt.setEncodeFormat(audioEncodeFormat);

        RtmpPublisher.BwEstConfig bwEstConfig = new RtmpPublisher.BwEstConfig();
        bwEstConfig.initAudioBitrate = mAudioBitrate;
        bwEstConfig.initVideoBitrate = mInitVideoBitrate;
        bwEstConfig.minVideoBitrate = mMinVideoBitrate;
        bwEstConfig.maxVideoBitrate = mMaxVideoBitrate;
        bwEstConfig.isAdjustBitrate = mAutoAdjustVideoBitrate;
        mRtmpPublisher.setBwEstConfig(bwEstConfig);
        mRtmpPublisher.setFramerate(mTargetFps);
        mRtmpPublisher.setVideoBitrate(mMaxVideoBitrate);
        mRtmpPublisher.setAudioBitrate(mAudioBitrate);
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
        if (mIsFileRecording) {
            return false;
        }
        mIsFileRecording = true;
        startCapture();
        mFilePublisher.startRecording(recordUrl);
        return true;
    }

    public void stopRecord() {
        if (!mIsFileRecording) {
            return;
        }
        mIsFileRecording = false;
        mFilePublisher.stop();
        stopCapture();
    }

    private void startCapture() {
        if (mIsCaptureStarted) {
            return;
        }
        mIsCaptureStarted = true;
        setAudioParams();
        setRecordingParams();
        mAudioCapture.start();
        mScreenCapture.start();
    }

    private void stopCapture() {
        if (!mIsCaptureStarted) {
            return;
        }
        if (mIsRecording || mIsFileRecording) {
            return;
        }
        mIsCaptureStarted = false;
        if (mAudioCapture.isRecordingState()) {
            mAudioCapture.stop();
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
        mIsRecording = false;
        stopCapture();
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
     * @param volume volume in 0~1.0f.
     */
    public void setVoiceVolume(float volume) {
        mAudioMixer.setInputVolume(0, volume);
    }

    /**
     * get mic volume
     *
     * @return volume in 0~1.0f.
     */
    public float getVoiceVolume() {
        return mAudioMixer.getInputVolume(0);
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
        if (!mIsRecording) {
            Log.e(TAG, "Should be called after startStream");
            return;
        }
        alpha = Math.max(0.0f, alpha);
        alpha = Math.min(alpha, 1.0f);
        mImgTexMixer.setRenderRect(1, x, y, w, h, alpha);
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
        if (!mIsRecording) {
            Log.e(TAG, "Should be called after startStream");
            return;
        }
        alpha = Math.max(0.0f, alpha);
        alpha = Math.min(alpha, 1.0f);
        mImgTexMixer.setRenderRect(2, x, y, w, 0, alpha);
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
     * Get current KSYStreamer sdk version.
     *
     * @return version number as 1.0.0.0
     */
    public String getKSYStreamerSDKVersion() {
        return StatsConstant.SDK_VERSION_SUB_VALUE;
    }

    /**
     * get libscreenstreamer version
     *
     * @return
     */
    public String getLibScreenStreamerVersion() {
        return LIBSCREENSTREAMER_VERSION_VALUE;
    }

    /**
     * Release all resources used by KSYScreenStreamer.
     */
    public void release() {
        mScreenCapture.release();
        mAudioCapture.release();
        mWaterMarkCapture.release();
        mScreenGLRender.release();
        setOnLogEventListener(null);
    }

    public interface OnInfoListener {
        void onInfo(int what, int msg1, int msg2);
    }

    public interface OnErrorListener {
        void onError(int what, int msg1, int msg2);
    }
}
