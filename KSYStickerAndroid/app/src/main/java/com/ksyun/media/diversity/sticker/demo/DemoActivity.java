package com.ksyun.media.diversity.sticker.demo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.ksyun.media.streamer.kit.StreamerConstants;
import com.sensetime.sensear.SenseArMaterialService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class DemoActivity extends Activity implements OnClickListener {
    private static final String TAG = DemoActivity.class.getSimpleName();
    private Button mConnectButton;
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
    private final static String LICENSE_NAME = "SenseME.lic";
    private final static String PREF_ACTIVATE_CODE_FILE = "activate_code_file";
    private final static String PREF_ACTIVATE_CODE = "activate_code";
    private Context mContext;

    private SenseArMaterialService mSenseArService;
    private boolean mHasAuthorized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_activity);

        mConnectButton = (Button) findViewById(R.id.connectBT);
        mConnectButton.setOnClickListener(this);

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

        mContext = this;
        mSenseArService = SenseArMaterialService.shareInstance();
        mSenseArService.initialize(mContext);
//        requestPermission();

        mHasAuthorized = authorized(false);

        copyFileIfNeed(SenseArMaterialService.MODEL_FILE_NAME);

        //如果license没检查通过，仍然可以使用demo，但是贴纸功能不能work
        if (!checkLicense()) {
            Toast.makeText(getApplicationContext(), "Check license failed", Toast.LENGTH_LONG).show();
        } else {
        }
        //初始化SenseArMaterialRender服务
        SenseARMaterialRenderBuilder.getInstance().initSenseARMaterialRender(getFilePath(SenseArMaterialService.MODEL_FILE_NAME),mContext);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.connectBT:
                int frameRate = 0;
                int videoBitRate = 0;
                int audioBitRate = 0;
                int videoResolution;
                int encodeMethod;
                boolean landscape;
                boolean startAuto;
                boolean showDebugInfo;

                if (!TextUtils.isEmpty(mUrlEditText.getText())
					&& mUrlEditText.getText().toString().startsWith("rtmp")) {
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

                    StickerCameraActivity.startActivity(getApplicationContext(), 0,
                            mUrlEditText.getText().toString(), frameRate, videoBitRate,
                            audioBitRate, videoResolution, landscape, encodeMethod,
                            startAuto, showDebugInfo);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SenseARMaterialRenderBuilder.getInstance().releaseSenseArMaterialRender();
    }

    /**
     * 检查APP_ID和APP_KEY合法性
     * @return true, 成功 false,失败
     */
    private boolean authorized(final boolean isFinished) {

        if (!isNetworkConnectionAvailable()) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Network unavailabel.", Toast.LENGTH_LONG).show();
                }
            });
            return false;
        }
        boolean authorized = SenseArMaterialService.shareInstance().authorizeWithAppId(Constants.APPID, Constants.KEY);

        if (!authorized) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("", "APPID: " + Constants.APPID + " key: " + Constants.KEY);
                    Toast.makeText(getApplicationContext(), "Application authorized failed", Toast.LENGTH_LONG).show();
                    if (isFinished) {
                        finish();
                    }
                }
            });

        }
        return authorized;
    }

    /**
     * 检查是否是否需要拷贝模型
     * @param fileName 模型名称
     * @return true, 成功 false,失败
     */
    private boolean copyFileIfNeed(String fileName) {
        String path = getFilePath(fileName);
        if (path != null) {
            File file = new File(path);
            if (!file.exists()) {
                //如果模型文件不存在或者当前模型文件的版本跟sdcard中的版本不一样
                try {
                    if (file.exists())
                        file.delete();
                    file.createNewFile();
                    InputStream in = mContext.getApplicationContext().getAssets().open(fileName);
                    if (in == null) {
                        Log.e("copyMode", "the src is not existed");
                        return false;
                    }
                    OutputStream out = new FileOutputStream(file);
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        out.write(buffer, 0, n);
                    }
                    in.close();
                    out.close();
                } catch (IOException e) {
                    file.delete();
                    return false;
                }
            }
        }
        return true;
    }

    protected String getFilePath(String fileName) {
        String path = null;
        File dataDir = mContext.getApplicationContext().getExternalFilesDir(null);
        if (dataDir != null) {
            path = dataDir.getAbsolutePath() + File.separator + fileName;
        }
        return path;
    }

    /**
     * 检查activeCode合法性
     *
     * @return true, 成功 false,失败
     */
    private boolean checkLicense() {
        StringBuilder sb = new StringBuilder();
        InputStreamReader isr = null;
        BufferedReader br = null;
        // 读取license文件内容
        try {
            isr = new InputStreamReader(getResources().getAssets().open(LICENSE_NAME));
            br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }

        // license文件为空,则直接返回
        if (sb.toString().length() == 0) {
            Log.e(TAG, "read license data error");
            return false;
        }

        /**
         * 以下逻辑为：
         * 1. 获取本地保存的激活码
         * 2. 如果没有则生成一个激活码
         * 3. 如果有, 则直接调用checkActiveCode*检查激活码
         * 4. 如果检查失败，则重新生成一个activeCode
         * 5. 如果生成失败，则返回失败，成功则保存新的activeCode，并返回成功
         */
        SharedPreferences sp = getApplicationContext().getSharedPreferences(PREF_ACTIVATE_CODE_FILE, Context.MODE_PRIVATE);
        String activateCode = sp.getString(PREF_ACTIVATE_CODE, null);
        Integer error = new Integer(-1);
        if (activateCode == null || !mSenseArService.checkActiveCodeWithLicenseData(activateCode, sb.toString().getBytes(), error)) {
            activateCode = mSenseArService.generateActiveCodeWithLicenseData(sb.toString().getBytes(), error);
            if (activateCode != null) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(PREF_ACTIVATE_CODE, activateCode);
                editor.commit();
                return true;
            }
            return false;
        }

        Log.d(TAG, "activeCode: " + activateCode);

        return true;
    }

    private boolean isNetworkConnectionAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) return false;
        NetworkInfo.State network = info.getState();
        return (network == NetworkInfo.State.CONNECTED || network == NetworkInfo.State.CONNECTING);
    }
}
