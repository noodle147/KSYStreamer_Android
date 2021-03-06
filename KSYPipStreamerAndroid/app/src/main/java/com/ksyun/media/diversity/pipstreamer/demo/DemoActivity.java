package com.ksyun.media.diversity.pipstreamer.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

import com.ksyun.media.diversity.pipstreamer.MD5Helper;
import com.ksyun.media.streamer.demo.R;
import com.ksyun.media.streamer.kit.StreamerConstants;

public class DemoActivity extends Activity implements OnClickListener {
    private static final String TAG = DemoActivity.class.getSimpleName();
    private Button mConnectPipButton;
    private Button mConnectAudioOnlyButton;
    private EditText mUrlEditText;
    private EditText mFrameRateEditText;
    private EditText mVideoBitRateEditText;
    private EditText mAudioBitRateEditText;
    private RadioButton mRes360Button;
    private RadioButton mRes480Button;
    private RadioButton mRes540Button;
    private RadioButton mRes720Button;

    private RadioButton mLandscapeButton;
    private RadioButton mPortraitButton;

    private RadioButton mSWButton;
    private RadioButton mHWButton;
    private RadioButton mSW1Button;
    private CheckBox mAutoStartCheckBox;
    private CheckBox mShowDebugInfoCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_activity);

        mConnectPipButton = (Button) findViewById(R.id.connectPip);
        mConnectPipButton.setOnClickListener(this);
        mConnectAudioOnlyButton = (Button) findViewById(R.id.connectAudioOnly);
        mConnectAudioOnlyButton.setOnClickListener(this);

        mUrlEditText = (EditText) findViewById(R.id.rtmpUrl);
        mFrameRateEditText = (EditText) findViewById(R.id.frameRatePicker);
        mFrameRateEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        mVideoBitRateEditText = (EditText) findViewById(R.id.videoBitratePicker);
        mVideoBitRateEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        mAudioBitRateEditText = (EditText) findViewById(R.id.audioBitratePicker);
        mAudioBitRateEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        mRes360Button = (RadioButton) findViewById(R.id.radiobutton1);
        mRes480Button = (RadioButton) findViewById(R.id.radiobutton2);
        mRes540Button = (RadioButton) findViewById(R.id.radiobutton3);
        mRes720Button = (RadioButton) findViewById(R.id.radiobutton4);
        mLandscapeButton = (RadioButton) findViewById(R.id.orientationbutton1);
        mPortraitButton = (RadioButton) findViewById(R.id.orientationbutton2);
        mSWButton = (RadioButton) findViewById(R.id.encode_sw);
        mHWButton = (RadioButton) findViewById(R.id.encode_hw);
        mSW1Button = (RadioButton) findViewById(R.id.encode_sw1);
        mAutoStartCheckBox = (CheckBox) findViewById(R.id.autoStart);
        mShowDebugInfoCheckBox = (CheckBox) findViewById(R.id.print_debug_info);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.connectAudioOnly:
            case R.id.connectPip:
                int frameRate = 0;
                int videoBitRate = 0;
                int audioBitRate = 0;
                int videoResolution;
                int encodeMethod;
                boolean landscape;
                boolean startAuto;
                boolean showDebugInfo;

                if (!TextUtils.isEmpty(mUrlEditText.getText())) {
                    if (!TextUtils.isEmpty(mFrameRateEditText.getText().toString())) {
                        frameRate = Integer.parseInt(mFrameRateEditText.getText()
                                .toString());
                    }

                    if (!TextUtils.isEmpty(mVideoBitRateEditText.getText().toString())) {
                        videoBitRate = Integer.parseInt(mVideoBitRateEditText.getText()
                                .toString());
                    }

                    if (!TextUtils.isEmpty(mAudioBitRateEditText.getText().toString())) {
                        audioBitRate = Integer.parseInt(mAudioBitRateEditText.getText()
                                .toString());
                    }

                    if (mRes360Button.isChecked()) {
                        videoResolution = StreamerConstants.VIDEO_RESOLUTION_360P;
                    } else if (mRes480Button.isChecked()) {
                        videoResolution = StreamerConstants.VIDEO_RESOLUTION_480P;
                    } else if (mRes540Button.isChecked()) {
                        videoResolution = StreamerConstants.VIDEO_RESOLUTION_540P;
                    } else {
                        videoResolution = StreamerConstants.VIDEO_RESOLUTION_720P;
                    }

                    if (mHWButton.isChecked()) {
                        encodeMethod = StreamerConstants.ENCODE_METHOD_HARDWARE;
                    } else if (mSWButton.isChecked()) {
                        encodeMethod = StreamerConstants.ENCODE_METHOD_SOFTWARE;
                    } else {
                        encodeMethod = StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT;
                    }

                    landscape = mLandscapeButton.isChecked();
                    startAuto = mAutoStartCheckBox.isChecked();
                    showDebugInfo = mShowDebugInfoCheckBox.isChecked();

                    String appNameAndStreamName = mUrlEditText.getText().toString();
                    String[] strArray = appNameAndStreamName.split("/");
                    if(strArray.length != 2) {
                        break;
                    }
                    String appName = strArray[0];
                    String streamName = strArray[1];
                    String key = "";
                    long t = System.currentTimeMillis() / 1000;
                    String input = key + streamName + t;
                    String result = MD5Helper.getMD5(input);
                    result = result.substring(8, 24);
                    String pushString = "rtmp://upstream.omwchat.com/" + appName + "/" + streamName + "?t=" + t + "&k=" + result;

                    if(view.getId() == R.id.connectAudioOnly) {
                        AudioActivity.startActivity(getApplicationContext(), 0, pushString, frameRate, videoBitRate,
                                audioBitRate, videoResolution, landscape, encodeMethod,
                                startAuto, showDebugInfo);
                    } else if(view.getId() == R.id.connectPip) {
                        PipActivity.startActivity(getApplicationContext(), 0,
                                pushString,
                                frameRate, videoBitRate,
                                audioBitRate, videoResolution, landscape, encodeMethod,
                                startAuto, showDebugInfo);
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
