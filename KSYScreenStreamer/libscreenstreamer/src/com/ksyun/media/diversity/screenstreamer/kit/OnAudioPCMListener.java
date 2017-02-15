package com.ksyun.media.diversity.screenstreamer.kit;

import java.nio.ByteBuffer;

/**
 * 音频pcm回调
 */

public interface OnAudioPCMListener {
    void onAudioPCMAvailable(ByteBuffer data, long timestamp, int channels,
                             int samplerate, int samplefmt);
}
