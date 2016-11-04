package com.ksyun.media.diversity.screenstreamer.capture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;

/**
 * start media_project activity
 */

public class ScreenBroadcastReceiver extends BroadcastReceiver {
    ScreenCapture mSender;
    public ScreenBroadcastReceiver(ScreenCapture sender) {
        mSender = sender;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equalsIgnoreCase(ScreenCapture.ASSISTANTACITVITYCREATED_INTENT)) {
            ScreenCapture screenCapture = mSender;
            if(ScreenCapture.mScreenCaptureActivity != null) {
                ScreenCapture.ScreenCaptureAssistantActivity screenAcitvity =
                        ScreenCapture.mScreenCaptureActivity;
                ScreenCapture.mScreenCaptureActivity.mScreenCapture = screenCapture;

                if(screenAcitvity.mScreenCapture.mMediaProjectManager == null) {
                    screenAcitvity.mScreenCapture.mMediaProjectManager =
                            (MediaProjectionManager)screenAcitvity.getSystemService("media_projection");
                }

                screenAcitvity.startActivityForResult(
                        screenAcitvity.mScreenCapture.mMediaProjectManager.createScreenCaptureIntent(),
                        ScreenCapture.MEDIAPROJECTION_REQUEST_CODE);
            }
        }

    }
}
