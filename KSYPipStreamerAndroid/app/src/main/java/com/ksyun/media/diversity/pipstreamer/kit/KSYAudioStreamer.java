package com.ksyun.media.diversity.pipstreamer.kit;

import android.content.Context;

import com.ksyun.media.diversity.pipstreamer.capture.MediaPlayerCapture;
import com.ksyun.media.streamer.kit.KSYStreamer;

public class KSYAudioStreamer extends KSYStreamer {
    private static final String TAG = "KSYAudioStreamer";

    private static final int IDX_BG_MUSIC = 1;
    private static final int IDX_SOUND_EFFECT = 2;
    private static final int IDX_USER_VOICE_MESSAGE = 3;
    private static final int IDX_MUSIC = 4;

    private MediaPlayerCapture mBgMusicCapture;
    private MediaPlayerCapture mBgSoundEffectCapture;
    private MediaPlayerCapture mBgUserVoiceCapture;
//    private MediaPlayerCapture mMusicCapture;

    public KSYAudioStreamer(Context context) {
        super(context);
    }

    @Override
    protected void initModules() {
        mIdxCamera = 2;
        mIdxWmLogo = 3;
        mIdxWmTime = 4;
        super.initModules();

        mBgMusicCapture = new MediaPlayerCapture(mContext, getGLRender());
        mBgSoundEffectCapture = new MediaPlayerCapture(mContext, getGLRender());
        mBgUserVoiceCapture = new MediaPlayerCapture(mContext, getGLRender());
//        mMusicCapture = new MediaPlayerCapture(mContext, getGLRender());

        mBgMusicCapture.mAudioBufSrcPin.connect(getAudioMixer().getSinkPin(IDX_BG_MUSIC));
        mBgSoundEffectCapture.mAudioBufSrcPin.connect(getAudioMixer().getSinkPin(IDX_SOUND_EFFECT));
        mBgUserVoiceCapture.mAudioBufSrcPin.connect(getAudioMixer().getSinkPin(IDX_USER_VOICE_MESSAGE));
    }

    @Override
    public void release() {
        super.release();
        mBgMusicCapture.release();
        mBgSoundEffectCapture.release();
        mBgUserVoiceCapture.release();
    }

    @Override
    public void setMuteAudio(boolean enable) {
        super.setMuteAudio(enable);
        if(!isAudioPreviewing()) {
            mBgMusicCapture.getMediaPlayer().setPlayerMute(enable ? 1 : 0);
            mBgSoundEffectCapture.getMediaPlayer().setPlayerMute(enable ? 1 : 0);
            mBgUserVoiceCapture.getMediaPlayer().setPlayerMute(enable ? 1 : 0);
        }
    }

    @Override
    public void setEnableAudioPreview(boolean enable) {
        super.setEnableAudioPreview(enable);
        if(!isAudioMuted()) {
            mBgMusicCapture.getMediaPlayer().setPlayerMute(enable ? 1 : 0);
            mBgSoundEffectCapture.getMediaPlayer().setPlayerMute(enable ? 1 : 0);
            mBgUserVoiceCapture.getMediaPlayer().setPlayerMute(enable ? 1 : 0);
        } else {
            mBgMusicCapture.getMediaPlayer().setPlayerMute(1);
            mBgSoundEffectCapture.getMediaPlayer().setPlayerMute(1);
            mBgUserVoiceCapture.getMediaPlayer().setPlayerMute(1);
        }
    }

    public void startBgMusic(String uri, boolean looping) {
        mBgMusicCapture.start(uri, looping);
    }

    public void stopBgMusic() {
        mBgMusicCapture.stop();
    }

    public void startBgSoundEffect(String uri, boolean looping) {
        mBgSoundEffectCapture.start(uri, looping);
    }

    public void stopBgSoundEffect() {
        mBgSoundEffectCapture.stop();
    }

    public void startBgUserVoice(String uri, boolean looping) {
        mBgUserVoiceCapture.start(uri, looping);
    }

    public void stopBgUserVoice() {
        mBgUserVoiceCapture.stop();
    }

    public MediaPlayerCapture getBgMusicCapture() {
        return mBgMusicCapture;
    }

    public MediaPlayerCapture getBgSoundEffectCapture() {
        return mBgSoundEffectCapture;
    }

    public MediaPlayerCapture getBgUserVoiceCapture() {
        return mBgUserVoiceCapture;
    }
}
