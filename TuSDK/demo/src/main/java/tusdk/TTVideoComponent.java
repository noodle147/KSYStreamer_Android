package tusdk;

import android.content.Context;
import android.util.Log;
import android.widget.RelativeLayout;

import com.ksyun.media.streamer.framework.ImgBufFrame;
import com.ksyun.media.streamer.framework.SrcPin;

import org.lasque.tusdk.core.TuSDKStreamingConfiguration;
import org.lasque.tusdk.core.struct.TuSdkSize;
import org.lasque.tusdk.core.utils.hardware.CameraConfigs;
import org.lasque.tusdk.core.utils.hardware.InterfaceOrientation;
import org.lasque.tusdk.core.utils.hardware.TuSdkVideoCamera;

import tusdk.camera.LiveVideoCamera;

/**
 * Created by sujia on 2017/1/2.
 */

public class TTVideoComponent {

    private LiveVideoCamera liveVideoCamera;

    private final static String TAG = "TTVideoComponent";

    // 要支持多款滤镜，直接添加到数组即可
    private String[] mVideoFilters = new String[]{"VideoFair", "VideoWarmSunshine"};
    private int mVideoFilterIndex = 0;
    // 美颜状态
    private Boolean mBeautyEnabled = false;

    private int mTargetWidth = 320;
    private int mTargetHeight = 480;
    private int mVideoBitrate = 600 * 1000;
    private int mFrameRate = 15;

    public TTVideoComponent(Context context,
                            CameraConfigs.CameraFacing facing, RelativeLayout holderView) {
        liveVideoCamera = new LiveVideoCamera(context, facing, holderView);
//        videoCamera.callback = mCameraDelegate;
    }

    public void setTuSdkVideoCameraDelegate(TuSdkVideoCamera.TuSdkVideoCameraDelegate delegate) {
        if (liveVideoCamera == null)
            return;

        liveVideoCamera.setDelegate(delegate);
    }

    public void initCamera() {
        liveVideoCamera.initParams(getVideoStreamingConfiguration());
    }

    private TuSDKStreamingConfiguration getVideoStreamingConfiguration()
    {
        TuSDKStreamingConfiguration config = new TuSDKStreamingConfiguration();
        // 说明: 相机的previewSize 和 最后输出的尺寸可能不一致。
        // 编码器会根据 outputSize 对输出作等比例裁剪，保证输出的视频流尺寸和 outputSize 一致
        config.videoSize = new TuSdkSize(mTargetWidth, mTargetHeight);
        // 码率，该参数目前仅在 _isOuputEncodedStream ＝ true 时有用
        config.videoBitrate = mVideoBitrate;
        // 最大帧率
        config.frameRate = mFrameRate;

        return config;
    }

    /** Start camera capturing */
    public void startCameraCapture() {
        Log.d(TAG, "startCameraCapture");
        liveVideoCamera.startCameraCapture();
    }

    /** Stop camera capturing */
    public void stopCameraCapture() {
        Log.d(TAG, "stopCameraCapture");
        liveVideoCamera.stopCameraCapture();
    }

    public void startRecord() {
        Log.d(TAG, "startRecord");
        liveVideoCamera.startRecording();
    }

    public boolean isRecording() {
        return liveVideoCamera.isRecording();
    }

    public void stopRecord() {
        Log.d(TAG, "stopRecord");
        liveVideoCamera.stopRecording();
    }

    public void release() {
        liveVideoCamera.getSrcPin().disconnect(false);
    }

    public void updateBeautyCode() {
        mVideoFilterIndex ++;

        if (mVideoFilterIndex >= mVideoFilters.length) {
            mVideoFilterIndex = 0;
        }

        String code = mBeautyEnabled ? mVideoFilters[mVideoFilterIndex]: "";
        changeVideoFilterCode(code);
    }

    public void changeVideoFilterCode(String code) {
        liveVideoCamera.switchFilter(code);
    }

    public void toggleTorch(boolean enable) {
        liveVideoCamera.setFlashMode(enable? CameraConfigs.CameraFlash.Torch :
                CameraConfigs.CameraFlash.Off);
    }

    public void switchCamera() {
        liveVideoCamera.rotateCamera();
    }

    public SrcPin<ImgBufFrame> getSrcPin() {
        return liveVideoCamera.getSrcPin();
    }

    public void setEnableBeauty(boolean enable) {
        mBeautyEnabled = enable;

        String code = mBeautyEnabled ? mVideoFilters[mVideoFilterIndex]: "";

        changeVideoFilterCode(code);
    }

    public void setEnableFaceAutoBeauty(boolean enable) {
        liveVideoCamera.setEnableFaceAutoBeauty(true);
    }

    public void setTargetResolution(int width, int height) {
        this.mTargetWidth = width;
        this.mTargetHeight = height;
    }

    public void setVideoBitrate(int videoBitrate) {
        this.mVideoBitrate = videoBitrate;
    }

    public void setFrameRate(int frameRate) {
        this.mFrameRate = frameRate;
    }

    public void setOrientation(int mRotateDegrees) {
        //TODO: check rotation here
        switch (mRotateDegrees) {
            case 0:
                liveVideoCamera.setOutputImageOrientation(InterfaceOrientation.Portrait);
                break;
            case 90:
                liveVideoCamera.setOutputImageOrientation(InterfaceOrientation.PortraitUpsideDown);
                break;
            case 180:
                liveVideoCamera.setOutputImageOrientation(InterfaceOrientation.LandscapeLeft);
                break;
            case 270:
                liveVideoCamera.setOutputImageOrientation(InterfaceOrientation.LandscapeRight);
                break;
        }
    }

    public void setFrontCameraMirror(boolean enable) {
        liveVideoCamera.setHorizontallyMirrorFrontFacingCamera(enable);
    }

    public long getEncodedFrames() {
        return liveVideoCamera.getEncodedFrames();
    }

    public void sendExtraData() {
        liveVideoCamera.sendExtraData();
    }
}
