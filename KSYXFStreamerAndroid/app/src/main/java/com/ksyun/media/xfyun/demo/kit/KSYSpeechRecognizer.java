package com.ksyun.media.xfyun.demo.kit;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechRecognizer;
import com.ksyun.media.streamer.filter.audio.AudioResampleFilter;
import com.ksyun.media.streamer.framework.AVConst;
import com.ksyun.media.streamer.framework.AudioBufFormat;
import com.ksyun.media.streamer.framework.AudioBufFrame;
import com.ksyun.media.streamer.framework.SinkPin;
import com.ksyun.media.xfyun.demo.util.JsonParser;

import static com.iflytek.cloud.SpeechConstant.KEY_SPEECH_TIMEOUT;
import static com.iflytek.cloud.SpeechConstant.VAD_ENABLE;

/**
 * Created by sujia on 2017/12/13.
 */

public class KSYSpeechRecognizer {
    private final static String TAG = KSYSpeechRecognizer.class.getSimpleName();

    private Context mContext;

    private AudioSinkPin mSinkPin;
    private AudioResampleFilter mAudioResampleFilter;
    private AudioBufFormat mInputFromat;
    private AudioBufFormat mOutputFromat;

    // 语音听写对象
    private SpeechRecognizer mIat;
    private boolean mTranslateEnable = false;
    private Toast mToast;
    private volatile boolean mStarted = false;
    private KSYSpeechListener mSpeechListener;

    public KSYSpeechRecognizer(Context context) {
        mContext = context;

        mSinkPin = new AudioSinkPin();
        mAudioResampleFilter = new AudioResampleFilter();
        //使用writeAudio输入，数据格式必须是采样率为8KHz或16KHz（本地识别只支持16K采样率，云端都支持），
        // 位长16bit，单声道的wav或者pcm
        //注：当音频过长，静音部分时长超过VAD_EOS将导致静音后面部分不能识别
        mOutputFromat = new AudioBufFormat(AVConst.AV_SAMPLE_FMT_S16, 16000, 1);
        mAudioResampleFilter.setOutFormat(mOutputFromat);
        mAudioResampleFilter.getSrcPin().connect(mSinkPin);

        initIat();
        mToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
    }

    private class AudioSinkPin extends SinkPin<AudioBufFrame> {
        @Override
        public void onFormatChanged(Object o) {
            if (o instanceof AudioBufFormat) {
                mInputFromat = (AudioBufFormat) o;
            }
        }

        @Override
        public void onFrameAvailable(AudioBufFrame audioBufFrame) {
            if (mStarted && audioBufFrame != null && audioBufFrame.buf != null) {
                byte[] audioData = new byte[audioBufFrame.buf.limit()];
                audioBufFrame.buf.get(audioData);
                mIat.writeAudio(audioData, 0, audioData.length);
            }
        }
    }

    public SinkPin<AudioBufFrame> getSinkPin() {
        return mAudioResampleFilter.getSinkPin();
    }

    private void initIat() {
        // 初始化识别无UI识别对象
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(mContext, mInitListener);
    }

    /**
     * 参数设置
     * @return
     */
    public void setParam(){
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, "");
        // 设置语言
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置语言区域
        mIat.setParameter(SpeechConstant.ACCENT,"mandarin");

        if( mTranslateEnable ){
            mIat.setParameter( SpeechConstant.ORI_LANG, "cn" );
            mIat.setParameter( SpeechConstant.TRANS_LANG, "en" );
        }
        // 设置引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

        if( mTranslateEnable ){
            Log.i( TAG, "translate enable" );
            mIat.setParameter( SpeechConstant.ASR_SCH, "1" );
            mIat.setParameter( SpeechConstant.ADD_CAP, "translate" );
            mIat.setParameter( SpeechConstant.TRS_SRC, "its" );
        }

        //VAD（Voice Activity Detection,静音抑制）是用于在音频传输时，通过控制音频的静音时长，
        // 减少在网络传输没有意义的数据，以减少网络带宽使用等
        //仅在允许VAD时，VAD_BOS, VAD_EOS才会起作用，且各监听
        // 的音量变化回调（如RecognizerListener.onVolumeChanged(int, byte[])）才会有音量 检测值返回
        mIat.setParameter(VAD_ENABLE, "0");

        //设置录取音频的最长时间。在听写、识别、语音语义和声纹等需要录入音频的业务下，在录音模式时，录取音频的最长时间。
        // 当录音超过这个时间时，SDK会自动结束 录音，并等待结果返回。
        // 当此参数值设为-1时，表示超时时间为无限，仅在评测和转写时生效，在其他业务中，服务器最长仅支持60秒的音频，超过的音频将被忽略
        mIat.setParameter(KEY_SPEECH_TIMEOUT, "-1");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
//        mIat.setParameter(SpeechConstant.VAD_BOS, "4000");

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
//        mIat.setParameter(SpeechConstant.VAD_EOS, "1000");

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "1");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");

        mIat.setParameter(SpeechConstant.SAMPLE_RATE, "16000");
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Log.d(TAG, "初始化失败，错误码：" + code);
            }
        }
    };

    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            Log.d(TAG, "onBeginOfSpeech");
            if (mSpeechListener != null) {
                mSpeechListener.onBeginOfSpeech();
            }
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            if(mTranslateEnable && error.getErrorCode() == 14002) {
                Log.d(TAG,  error.getPlainDescription(true)+"\n请确认是否已开通翻译功能" );
            } else {
                Log.d(TAG, error.getPlainDescription(true));
            }

            if (mSpeechListener != null) {
                mSpeechListener.onError(error);
            }
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            Log.d(TAG, "onEndOfSpeech");
            mStarted = false;
            if (mSpeechListener != null) {
                mSpeechListener.onEndOfSpeech();
            }
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, "onResult : " + JsonParser.parseIatResult(results.getResultString()));
            if( mTranslateEnable ){
                String trans  = JsonParser.parseTransResult(results.getResultString(),"dst");
                String oris = JsonParser.parseTransResult(results.getResultString(),"src");

                if( TextUtils.isEmpty(trans)||TextUtils.isEmpty(oris) ){
                    Log.d(TAG, "解析结果失败，请确认是否已开通翻译功能。" );
                }else{
                    Log.d(TAG, "原始语言:\n"+oris+"\n目标语言:\n"+trans );
                }

                if (mSpeechListener != null) {
                    mSpeechListener.onResult(trans, isLast);
                }
            }else{
                String text = JsonParser.parseIatResult(results.getResultString());
                Log.d(TAG, text);

                if (mSpeechListener != null) {
                    mSpeechListener.onResult(text, isLast);
                }
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
//            Log.d(TAG, "onVolumeChanged，volume：" + volume +
//                    ", data length: " + data.length);
            if (mSpeechListener != null) {
                mSpeechListener.onVolumeChanged(volume, data);
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            Log.d(TAG, "onEvent");
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
                Log.d(TAG, "session id =" + sid);
            }

            if (mSpeechListener != null) {
                mSpeechListener.onEvent(eventType, arg1, arg2, obj);
            }
        }
    };

    public void setSpeechListener(KSYSpeechListener listener) {
        mSpeechListener = listener;
    }

    public void start() {
        Log.d(TAG, "开始音频流识别");

       // 设置参数
        setParam();
        // 设置音频来源为外部文件
        mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
        // 也可以像以下这样直接设置音频文件路径识别（要求设置文件在sdcard上的全路径）：
        // mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-2");
        // mIat.setParameter(SpeechConstant.ASR_SOURCE_PATH, "sdcard/XXX/XXX.pcm");
        int ret = mIat.startListening(mRecognizerListener);

        if (ret != ErrorCode.SUCCESS) {
            Log.e(TAG, "识别失败, 错误码：" + ret);
            mStarted = false;
        } else {
            mStarted = true;
            //AUDIO_SOURCE为-1时，不会有onBeginOfSpeech回调，这里加上
            if (mSpeechListener != null) {
                mSpeechListener.onBeginOfSpeech();
            }
        }
    }

    public void stop() {
        Log.d(TAG, "停止音频流识别");
        mStarted = false;
        mIat.stopListening();
    }

    public void cancel() {
        Log.d(TAG, "cancel音频流识别");
        mIat.cancel();
    }

    public void release() {
        releaseIat();
    }

    private void releaseIat() {
        if( null != mIat ){
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();
        }
    }
}
