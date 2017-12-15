package com.ksyun.media.xfyun.demo.kit;

import android.content.Context;

import com.ksyun.media.streamer.filter.imgtex.ImgTexPreview;
import com.ksyun.media.streamer.kit.KSYStreamer;

/**
 * Created by sujia on 2017/12/13.
 */

public class KSYXFStreamer extends KSYStreamer {
    private KSYSpeechRecognizer mSpeechRecognizer;

    public KSYXFStreamer(Context context) {
        super(context);
    }

    @Override
    protected void initModules() {
        super.initModules();
        mSpeechRecognizer = new KSYSpeechRecognizer(mContext);
        mAudioCapture.getSrcPin().connect(mSpeechRecognizer.getSinkPin());
    }

    @Override
    public void release() {
        super.release();
        mSpeechRecognizer.release();
    }

    public void setSpeechListener(KSYSpeechListener listener) {
        mSpeechRecognizer.setSpeechListener(listener);
    }

    /**
     * 开始听写
     */
    public void startIat() {
        mSpeechRecognizer.start();
        super.startAudioCapture();
    }

    /**
     * 停止听写
     */
    public void stopIat() {
        mSpeechRecognizer.stop();
        super.stopAudioCapture();
    }

    /**
     * cancel听写
     */
    public void cancelIat() {
        mSpeechRecognizer.cancel();
    }

    public ImgTexPreview getImgTexPreview() {
        return mImgTexPreview;
    }
}
