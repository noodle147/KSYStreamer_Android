package com.ksyun.media.diversity.kiwiandroid;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;

import com.kiwi.tracker.KwFaceTracker;
import com.kiwi.tracker.KwFilterType;
import com.kiwi.tracker.KwTrackerManager;
import com.kiwi.tracker.KwTrackerSettings;
import com.kiwi.tracker.bean.KwFilter;
import com.kiwi.tracker.bean.KwTrackResult;
import com.kiwi.tracker.bean.conf.StickerConfig;
import com.kiwi.tracker.common.Config;
import com.kiwi.tracker.fbo.RgbaToNv21FBO;
import com.kiwi.tracker.utils.Accelerometer;
import com.kiwi.tracker.utils.GlUtil;
import com.kiwi.ui.OnViewEventListener;
import com.kiwi.ui.helper.ResourceHelper;
import com.kiwi.ui.model.SharePreferenceMgr;

import static com.blankj.utilcode.utils.ImageUtils.getBitmap;
import static com.kiwi.ui.KwControlView.BEAUTY_BIG_EYE_TYPE;
import static com.kiwi.ui.KwControlView.BEAUTY_THIN_FACE_TYPE;
import static com.kiwi.ui.KwControlView.REMOVE_BLEMISHES;
import static com.kiwi.ui.KwControlView.SKIN_SHINNING_TENDERNESS;
import static com.kiwi.ui.KwControlView.SKIN_TONE_PERFECTION;
import static com.kiwi.ui.KwControlView.SKIN_TONE_SATURATION;

/**
 * All calls to the Face Effects library are encapsulated in this class
 * Created by shijian on 2016/9/28.
 */

public class KwTrackerWrapper {
    private static final String TAG = KwTrackerWrapper.class.getName();

    public interface UIClickListener {
        void onTakeShutter();

        void onSwitchCamera();

        void onCloseCtrolView();
    }

    private KwTrackerSettings mTrackerSetting;
    private KwTrackerManager mTrackerManager;

    public KwTrackerWrapper(final Context context, int cameraFaceId) {

        SharePreferenceMgr instance = SharePreferenceMgr.getInstance();

        KwTrackerSettings.BeautySettings2 beautySettings2 = new KwTrackerSettings.BeautySettings2();
        beautySettings2.setWhiteProgress(instance.getSkinWhite());
        beautySettings2.setDermabrasionProgress(instance.getSkinRemoveBlemishes());
        beautySettings2.setSaturatedProgress(instance.getSkinSaturation());
        beautySettings2.setPinkProgress(instance.getSkinTenderness());

        KwTrackerSettings.BeautySettings beautySettings = new KwTrackerSettings.BeautySettings();
        beautySettings.setBigEyeScaleProgress(instance.getBigEye());
        beautySettings.setThinFaceScaleProgress(instance.getThinFace());

        mTrackerSetting = new KwTrackerSettings().
                setBeauty2Enabled(instance.isBeautyEnabled()).
                setBeautySettings2(beautySettings2).
                setBeautyFaceEnabled(instance.isLocalBeautyEnabled()).
                setBeautySettings(beautySettings).
                setCameraFaceId(cameraFaceId)
                .setDefaultDirection(KwFaceTracker.CV_CLOCKWISE_ROTATE_180)
        ;

        mTrackerManager = new KwTrackerManager(context).
                setTrackerSetting(mTrackerSetting)
                .build();

        //copy assets config/sticker/filter to sdcard
        ResourceHelper.copyResource2SD(context);

        initKiwiConfig();

        initWaterMark(context);
    }

    private void initWaterMark(Context context) {
//        Bitmap waterMark = getBitmap(context.getResources(), R.drawable.kiwi_logo);
//        mTrackerSetting.setWaterMarkSettings(true, waterMark, new RectF(0.8f, 0.94f, 0.98f, 0.995f));
    }

    private void initKiwiConfig() {
        //推荐配置，以下情况，选择性能优先模式
        //1.oppo vivo你懂的
        //2.小于5.0的机型可能配置比较差
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        Log.i(TAG, String.format("manufacturer:%s,model:%s,sdk:%s", manufacturer, Build.MODEL, Build.VERSION.SDK_INT));
        boolean isOppoVivo = manufacturer.contains("oppo") || manufacturer.contains("vivo");
        Log.i(TAG, "initKiwiConfig buildVersion" + Build.VERSION.RELEASE);
        if (isOppoVivo || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Config.TRACK_MODE = Config.TRACK_PRIORITY_PERFORMANCE;
        }

        //关闭日志打印,release版本请务必关闭日志打印
        Config.isDebug = true;
    }

    public void onCreate(Activity activity) {
        mTrackerManager.onCreate(activity);

    }

    public void onResume(Activity activity) {
        mTrackerManager.onResume(activity);

        if (rgbaToNv21FBO != null) {
            rgbaToNv21FBO.release();
            rgbaToNv21FBO = null;
        }
    }

    public void onPause(Activity activity) {
        mTrackerManager.onPause(activity);
    }

    public void onDestroy(Activity activity) {
        mTrackerManager.onDestory(activity);
    }

    public void onSurfaceCreated(Context context) {
        mTrackerManager.onSurfaceCreated(context);
    }

    public void onSurfaceChanged(int width, int height, int previewWidth, int previewHeight) {
        mTrackerManager.onSurfaceChanged(width, height, previewWidth, previewHeight);
    }

    public void onSurfaceDestroyed() {
        mTrackerManager.onSurfaceDestroyed();
    }

    public void switchCamera(int ordinal) {
        mTrackerManager.switchCamera(ordinal);
    }

    /**
     * 对纹理进行特效处理（美颜、大眼瘦脸、人脸贴纸、哈哈镜、滤镜）
     * 优点：cpu/内存 占用低，高通/三星 gpu兼容性好
     * 缺点：该方法对gpu有些要求，早期mali gpu的性能可能导致卡顿
     *
     * @param texId     YUV格式纹理
     * @param texWidth  纹理宽度
     * @param texHeight 纹理高度
     * @return 特效处理后的纹理
     */
    public int onDrawOESTexture(int texId, int texWidth, int texHeight) {

        //解开绑定
        int newTexId = texId;
        int maxFaceCount = 1;
        int filterTexId = mTrackerManager.onDrawTexture2D(texId, texWidth, texHeight, maxFaceCount);
        if (filterTexId != -1) {
            newTexId = filterTexId;
        }
        GLES20.glGetError();//请勿删除当前行获取opengl错误代码
        return newTexId;
    }

    /**
     * 对纹理进行特效处理（美颜、大眼瘦脸、人脸贴纸、哈哈镜、滤镜）
     * 优点：对gpu要求低
     * 缺点：cpu/内存 占用高
     *
     * @param nv21        nv21 preview data
     * @param texId       external texture id
     * @param texWidth    texture width
     * @param texHeight   texture height
     * @param orientation texture orientation
     * @return output texture id
     */
    public int onDrawOESTexture(byte[] nv21, int texId, int texWidth, int texHeight, int orientation) {

        long start = System.currentTimeMillis();

        //解开绑定
        int newTexId = texId;
        int maxFaceCount = 1;

        //屏幕方向适配
        int dir = Accelerometer.getDirection();
        if (((orientation == 270 && (dir & 1) == 1) || (orientation == 90 && (dir & 1) == 0)))
            dir = (dir ^ 2);

        KwTrackResult kwTrackResult = KwTrackResult.NO_TRACK_RESULT;
        if (mTrackerSetting.isNeedTrack()) {
            kwTrackResult = mTrackerManager.track(nv21, KwFaceTracker.KW_FORMAT_NV21, texWidth, texHeight, maxFaceCount, dir * 90);
            if (Config.isDebug) Log.i(TAG, "track cost:" + (System.currentTimeMillis() - start));
        }

        int filterTexId = mTrackerManager.onDrawOESTexture(texId, texWidth, texHeight, kwTrackResult);
        if (filterTexId != -1) {
            newTexId = filterTexId;
        }

        GLES20.glGetError();//请勿删除当前行获取opengl错误代码

        if (Config.isDebug)
            Log.i(TAG, "onDrawOESTexture cost:" + (System.currentTimeMillis() - start));

        Log.e("orientation", orientation + "");
        return newTexId;
    }

    /**
     * UI事件处理类
     *
     * @param uiClickListener
     * @return
     */
    public OnViewEventListener initUIEventListener(final UIClickListener uiClickListener) {
        OnViewEventListener eventListener = new OnViewEventListener() {

            @Override
            public void onSwitchBeauty2(boolean enable) {
                mTrackerManager.setBeauty2Enabled(enable);
            }

            @Override
            public void onTakeShutter() {
                uiClickListener.onTakeShutter();
            }

            @Override
            public void onSwitchCamera() {
                uiClickListener.onSwitchCamera();
            }

            @Override
            public void onSwitchFilter(KwFilter filter) {
                mTrackerManager.switchFilter(filter);
            }

            @Override
            public void onStickerChanged(StickerConfig item) {
                mTrackerManager.switchSticker(item);
            }

            @Override
            public void onSwitchBeauty(boolean enable) {
                mTrackerManager.setBeautyEnabled(enable);
            }

            @Override
            public void onSwitchBeautyFace(boolean enable) {
                mTrackerManager.setBeautyFaceEnabled(enable);
            }

            @Override
            public void onDistortionChanged(KwFilterType filterType) {
                mTrackerManager.switchDistortion(filterType);
            }

            @Override
            public void onAdjustFaceBeauty(int type, float param) {
                switch (type) {
                    case BEAUTY_BIG_EYE_TYPE:
                        mTrackerManager.setEyeMagnifying((int) param);
                        break;
                    case BEAUTY_THIN_FACE_TYPE:
                        mTrackerManager.setChinSliming((int) param);
                        break;
                    case SKIN_SHINNING_TENDERNESS:
                        //粉嫩
                        mTrackerManager.setSkinTenderness((int) param);
                        break;
                    case SKIN_TONE_SATURATION:
                        //饱和
                        mTrackerManager.setSkinSaturation((int) param);
                        break;
                    case REMOVE_BLEMISHES:
                        //磨皮
                        mTrackerManager.setSkinBlemishRemoval((int) param);
                        break;
                    case SKIN_TONE_PERFECTION:
                        //美白
                        mTrackerManager.setSkinWhitening((int) param);
                        break;
                }

            }

            @Override
            public void onFaceBeautyLevel(float level) {
                mTrackerManager.adjustBeauty(level);
            }

            @Override
            public void onCloseCtrolView() {
                uiClickListener.onCloseCtrolView();
            }

        };

        return eventListener;
    }

    private RgbaToNv21FBO rgbaToNv21FBO;
    private int mFrameId = 0;

    /**
     * 纹理转换成yuv输出
     *
     * @param context   context
     * @param textureId 输入纹理id
     * @param w         预览宽度
     * @param h         预览高度
     * @param outs      yuv输出
     */
    public void textureToNv21(Context context, int textureId, int w, int h, byte[] outs) {
        if (rgbaToNv21FBO == null) {
            rgbaToNv21FBO = new RgbaToNv21FBO(GLES20.GL_TEXTURE_2D, w, h);
            GlUtil.checkGlError("new RgbaToNv21FBO");
            rgbaToNv21FBO.initialize(context);
            GlUtil.checkGlError("int fbo");
        }

        rgbaToNv21FBO.drawFrame(textureId, w, h);

        //pbo抛弃前两帧
        if (mFrameId++ < 3) {
            return;
        }
        byte[] bytes = rgbaToNv21FBO.getBytes();
        int size = outs.length > bytes.length ? bytes.length : outs.length;
        System.arraycopy(bytes, 0, outs, 0, size);
    }

}
