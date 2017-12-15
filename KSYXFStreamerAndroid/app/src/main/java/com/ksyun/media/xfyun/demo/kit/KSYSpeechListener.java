package com.ksyun.media.xfyun.demo.kit;

import android.os.Bundle;

import com.iflytek.cloud.SpeechError;

/**
 * Created by sujia on 2017/12/13.
 */

public interface KSYSpeechListener {
    void onVolumeChanged(int volume, byte[] data);

    void onBeginOfSpeech();

    void onEndOfSpeech();

    void onResult(String text, boolean isLast);

    void onError(SpeechError error);

    void onEvent(int eventType, int arg1, int arg2, Bundle obj);
}
