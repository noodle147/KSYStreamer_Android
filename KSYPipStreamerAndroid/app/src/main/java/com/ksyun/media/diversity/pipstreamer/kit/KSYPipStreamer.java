package com.ksyun.media.diversity.pipstreamer.kit;

import android.content.Context;
import android.util.Log;

import com.ksyun.media.diversity.pipstreamer.capture.MediaPlayerCapture;
import com.ksyun.media.diversity.pipstreamer.capture.PictureCapture;
import com.ksyun.media.streamer.filter.imgtex.ImgTexMixer;
import com.ksyun.media.streamer.kit.KSYStreamer;

/**
 * All in one streamer class.
 */
public class KSYPipStreamer extends KSYStreamer {

    private static final String TAG = "KSYPipStreamer";
    private static final boolean DEBUG = false;

    private static final int IDX_BG_IMG = 0;
    private static final int IDX_BG_VIDEO = 1;
    private static final int IDX_AUDIO_PIP = 2;

    private MediaPlayerCapture mMediaPlayerCapture;
    private PictureCapture mPictureCapture;

    public KSYPipStreamer(Context context) {
        super(context);
    }

    @Override
    protected void initModules() {
        // override mixer idx
        mIdxCamera = 2;
        mIdxWmLogo = 3;
        mIdxWmTime = 4;

        // super init
        super.initModules();

        // create pip modules
        mPictureCapture = new PictureCapture(getGLRender());
        mMediaPlayerCapture = new MediaPlayerCapture(mContext, getGLRender());

        // pip connection

        mPictureCapture.getSrcPin().connect(getImgTexPreviewMixer().getSinkPin(IDX_BG_IMG));
        mMediaPlayerCapture.mImgTexSrcPin.connect(getImgTexPreviewMixer().getSinkPin(IDX_BG_VIDEO));
        getImgTexPreviewMixer().setScalingMode(IDX_BG_IMG, ImgTexMixer.SCALING_MODE_CENTER_CROP);
        getImgTexPreviewMixer().setScalingMode(IDX_BG_VIDEO, ImgTexMixer.SCALING_MODE_BEST_FIT);
        getImgTexPreviewMixer().setScalingMode(mIdxCamera, ImgTexMixer.SCALING_MODE_CENTER_CROP);
        getImgTexPreviewMixer().setMainSinkPinIndex(mIdxCamera);

        mPictureCapture.getSrcPin().connect(getImgTexMixer().getSinkPin(IDX_BG_IMG));
        mMediaPlayerCapture.mImgTexSrcPin.connect(getImgTexMixer().getSinkPin(IDX_BG_VIDEO));
        mMediaPlayerCapture.mAudioBufSrcPin.connect(getAudioMixer().getSinkPin(IDX_AUDIO_PIP));
        getImgTexMixer().setScalingMode(IDX_BG_IMG, ImgTexMixer.SCALING_MODE_CENTER_CROP);
        getImgTexMixer().setScalingMode(IDX_BG_VIDEO, ImgTexMixer.SCALING_MODE_BEST_FIT);
        getImgTexMixer().setScalingMode(mIdxCamera, ImgTexMixer.SCALING_MODE_CENTER_CROP);
        getImgTexMixer().setMainSinkPinIndex(mIdxCamera);
    }

    @Override
    public void setMuteAudio(boolean b) {
        super.setMuteAudio(b);
        if (!isAudioPreviewing()) {
            mMediaPlayerCapture.getMediaPlayer().setPlayerMute(b ? 1 : 0);
        }
    }

    @Override
    public void setEnableAudioPreview(boolean b) {
        super.setEnableAudioPreview(b);
        if (!isAudioMuted()) {
            mMediaPlayerCapture.getMediaPlayer().setPlayerMute(b ? 1 : 0);
        } else {
            mMediaPlayerCapture.getMediaPlayer().setPlayerMute(1);
        }
    }

    @Override
    public void release() {
        super.release();
        mMediaPlayerCapture.release();
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

    public void showBgPicture(Context context, String uri) {
        mPictureCapture.start(context, uri);
    }

    public void hideBgPicture() {
        mPictureCapture.stop();
    }

    public void showBgVideo(String url) {
        mMediaPlayerCapture.start(url);
        if (isAudioPreviewing()) {
            mMediaPlayerCapture.getMediaPlayer().setPlayerMute(1);
        }
    }

    public void hideBgVideo() {
        mMediaPlayerCapture.stop();
    }

    public void setCameraPreviewRect(float x, float y, float w, float h) {
        getImgTexPreviewMixer().setRenderRect(mIdxCamera, x, y, w, h, 1.0f);
        getImgTexMixer().setRenderRect(mIdxCamera, x, y, w, h, 1.0f);
    }
}
