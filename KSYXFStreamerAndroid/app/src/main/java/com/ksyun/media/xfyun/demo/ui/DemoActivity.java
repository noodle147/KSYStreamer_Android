package com.ksyun.media.xfyun.demo.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

import com.ksyun.media.xfyun.demo.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DemoActivity extends Activity {
    private static final String TAG = "DemoActivity";

    @BindView(R.id.connectBT)
    protected Button mConnectButton;
    @BindView(R.id.rtmpUrl)
    protected EditText mUrlEditText;

    protected DemoFragment mDemoFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_activity);
        ButterKnife.bind(this);
        mDemoFragment = new BaseDemoFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragment_layout,
                mDemoFragment).commit();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @OnClick(R.id.connectBT)
    public void onClick() {
        doStart();
    }

    protected void doStart() {
        if (!TextUtils.isEmpty(mUrlEditText.getText())) {
            String url = mUrlEditText.getText().toString();
            if (url.startsWith("rtmp")) {
                mDemoFragment.start(url);
            }
        }
    }
}
