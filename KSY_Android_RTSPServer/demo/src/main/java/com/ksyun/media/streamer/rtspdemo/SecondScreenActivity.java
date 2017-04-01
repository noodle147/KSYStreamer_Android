package com.ksyun.media.streamer.rtspdemo;

import android.os.Bundle;
import android.view.View;

/**
 * Created by sujia on 2017/3/9.
 */
public class SecondScreenActivity extends DemoActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findViewById(R.id.rtmpTxt).setVisibility(View.GONE);
        mUrlEditText.setVisibility(View.GONE);

        isSecondScreen = true;
    }
}
