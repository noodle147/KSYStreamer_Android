package com.ksyun.media.diversity.pipstreamer.capture;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;

import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.player.KSYMediaPlayer;
import com.ksyun.media.streamer.framework.AudioBufFormat;
import com.ksyun.media.streamer.framework.AudioBufFrame;
import com.ksyun.media.streamer.framework.ImgTexFormat;
import com.ksyun.media.streamer.framework.ImgTexFrame;
import com.ksyun.media.streamer.framework.SrcPin;
import com.ksyun.media.streamer.util.gles.GLRender;
import com.ksyun.media.streamer.util.gles.GlUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Media player capture.
 */

public class MediaPlayerCapture implements SurfaceTexture.OnFrameAvailableListener {
    private static String TAG = "MediaPlayerCapture";
    private final static boolean VERBOSE = true;

    public SrcPin<AudioBufFrame> mAudioBufSrcPin;
    public SrcPin<ImgTexFrame> mImgTexSrcPin;

    private Context mContext;
    private GLRender mGLRender;
    private KSYMediaPlayer mMediaPlayer;

    private AudioBufFormat mAudioBufFormat;
    private ImgTexFormat mImgTexFormat;
    private ByteBuffer mAudioOutBuffer;

    private int mTextureId;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;

    private volatile boolean mStopped = true;

    public MediaPlayerCapture(Context context, GLRender glRender) {
        mContext = context;
        mGLRender = glRender;
        mAudioBufSrcPin = new SrcPin<>();
        mImgTexSrcPin = new SrcPin<>();
        mGLRender.addListener(mGLRenderListener);
    }

    public KSYMediaPlayer getMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new KSYMediaPlayer.Builder(mContext).build();
        }
        return mMediaPlayer;
    }

    public void start(String url) {
        mAudioBufFormat = null;
        getMediaPlayer();
        mMediaPlayer.reset();
        mMediaPlayer.setOnAudioPCMAvailableListener(mOnAudioPCMListener);
        mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
        try {
            mMediaPlayer.setDataSource(url);
            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mStopped = false;
    }

    public void stop() {
        if (mMediaPlayer != null) {
            mStopped = true;
            if (mSurface != null) {
                mMediaPlayer.setSurface(null);
            }
            if (mSurfaceTexture != null) {
                mSurfaceTexture.setOnFrameAvailableListener(null);
            }
            mMediaPlayer.setOnAudioPCMAvailableListener(null);
            mMediaPlayer.stop();
            mGLRender.queueEvent(new Runnable() {
                @Override
                public void run() {
                    ImgTexFrame frame = new ImgTexFrame(mImgTexFormat,
                            ImgTexFrame.NO_TEXTURE, null, 0);
                    mImgTexSrcPin.onFrameAvailable(frame);
                }
            });
        }
    }

    /**
     * Should be called in Activity.onPause
     */
    public void onPause() {
        if (mMediaPlayer != null) {
            mMediaPlayer.setSurface(null);
        }
    }

    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mGLRender.removeListener(mGLRenderListener);
    }

    private IMediaPlayer.OnPreparedListener mOnPreparedListener =
            new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer iMediaPlayer) {
            if (mSurface != null) {
                mMediaPlayer.setSurface(mSurface);
            }
            mMediaPlayer.start();

            // trig onFormatChanged event
            int w = mMediaPlayer.getVideoWidth();
            int h = mMediaPlayer.getVideoHeight();
            Log.d(TAG, "video prepared, " + w + "x" + h);
            if (mSurfaceTexture != null) {
                mSurfaceTexture.setDefaultBufferSize(w, h);
                mSurfaceTexture.setOnFrameAvailableListener(MediaPlayerCapture.this);
            }
            mImgTexFormat = new ImgTexFormat(ImgTexFormat.COLOR_EXTERNAL_OES, w, h);
            mImgTexSrcPin.onFormatChanged(mImgTexFormat);
        }
    };

    private KSYMediaPlayer.OnAudioPCMListener mOnAudioPCMListener =
            new KSYMediaPlayer.OnAudioPCMListener() {
        @Override
        public void onAudioPCMAvailable(IMediaPlayer iMediaPlayer, ByteBuffer byteBuffer,
                long timestamp, int channels, int samplerate, int samplefmt) {
            if (mAudioBufFormat == null) {
                mAudioBufFormat = new AudioBufFormat(samplefmt, samplerate, channels);
                mAudioBufSrcPin.onFormatChanged(mAudioBufFormat);
            }
            if (byteBuffer == null) {
                return;
            }
            ByteBuffer pcmBuffer = byteBuffer;
            if (!byteBuffer.isDirect()) {
                int len = byteBuffer.limit();
                if (mAudioOutBuffer == null || mAudioOutBuffer.capacity() < len) {
                    mAudioOutBuffer = ByteBuffer.allocateDirect(len);
                    mAudioOutBuffer.order(ByteOrder.nativeOrder());
                }
                mAudioOutBuffer.clear();
                mAudioOutBuffer.put(byteBuffer);
                mAudioOutBuffer.flip();
                pcmBuffer = mAudioOutBuffer;
            }
            AudioBufFrame frame = new AudioBufFrame(mAudioBufFormat, pcmBuffer, timestamp);
            mAudioBufSrcPin.onFrameAvailable(frame);
        }
    };

    private GLRender.GLRenderListener mGLRenderListener = new GLRender.GLRenderListener() {
        @Override
        public void onReady() {
            if (VERBOSE) Log.d(TAG, "onGLContext ready");
            mTextureId = GlUtil.createOESTextureObject();
            if (mSurfaceTexture != null) {
                mSurfaceTexture.release();
            }
            if (mSurface != null) {
                mSurface.release();
            }
            mSurfaceTexture = new SurfaceTexture(mTextureId);
            mSurfaceTexture.setOnFrameAvailableListener(MediaPlayerCapture.this);
            mSurface = new Surface(mSurfaceTexture);
            if (mMediaPlayer != null) {
                mMediaPlayer.setSurface(mSurface);
                if (mMediaPlayer.isPlaying()) {
                    int w = mMediaPlayer.getVideoWidth();
                    int h = mMediaPlayer.getVideoHeight();
                    Log.d(TAG, "onReady " + w + "x" + h);
                    mSurfaceTexture.setDefaultBufferSize(w, h);
                }
            }
        }

        @Override
        public void onSizeChanged(int width, int height) {
            if (VERBOSE) Log.d(TAG, "onSizeChanged " + width + "x" + height);
        }

        @Override
        public void onDrawFrame() {
        }
    };

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        final long pts = System.nanoTime() / 1000 / 1000;
        mGLRender.queueEvent(new Runnable() {
            @Override
            public void run() {
                mSurfaceTexture.updateTexImage();
                if (mStopped) {
                    return;
                }
                float[] texMatrix = new float[16];
                mSurfaceTexture.getTransformMatrix(texMatrix);
                ImgTexFrame frame = new ImgTexFrame(mImgTexFormat, mTextureId, texMatrix, pts);
                try {
                    mImgTexSrcPin.onFrameAvailable(frame);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Draw player frame failed, ignore");
                }
            }
        });
    }
}
