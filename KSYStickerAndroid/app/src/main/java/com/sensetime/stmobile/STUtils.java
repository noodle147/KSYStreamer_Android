package com.sensetime.stmobile;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Administrator on 2016/9/21.
 */

public class STUtils {
    static final String MODEL_NAME = "face_track_2.0.1.model";
    static final String LICENSE_NAME = "SenseME.lic";

    enum ResultCode {
        ST_OK(0),
        ST_E_INVALIDARG(-1),
        ST_E_HANDLE(-2),
        ST_E_OUTOFMEMORY(-3),
        ST_E_FAIL(-4),
        ST_E_DELNOTFOUND(-5),
        ST_E_INVALID_PIXEL_FORMAT(-6),	///< 不支持的图像格式
        ST_E_FILE_NOT_FOUND(-10),   ///< 模型文件不存在
        ST_E_INVALID_FILE_FORMAT(-11),	///< 模型格式不正确，导致加载失败
        ST_E_INVALID_APPID(-12),		///< 包名错误
        ST_E_INVALID_AUTH(-13),		///< 加密狗功能不支持
        ST_E_AUTH_EXPIRE(-14),		///< SDK过期
        ST_E_FILE_EXPIRE(-15),		///< 模型文件过期
        ST_E_DONGLE_EXPIRE(-16),	///< 加密狗过期
        ST_E_ONLINE_AUTH_FAIL(-17),		///< 在线验证失败
        ST_E_ONLINE_AUTH_TIMEOUT(-18);

        private final int resultCode;

        ResultCode(int resultCode) {
            this.resultCode = resultCode;
        }

        public int getResultCode() {
            return resultCode;
        }
    }

    public static void copyModelIfNeed(String modelName, Context mContext) {
        String path = getModelPath(modelName, mContext);
        if (path != null) {
            File modelFile = new File(path);
            if (!modelFile.exists()) {
                //如果模型文件不存在或者当前模型文件的版本跟sdcard中的版本不一样
                try {
                    if (modelFile.exists())
                        modelFile.delete();
                    modelFile.createNewFile();
                    InputStream in = mContext.getApplicationContext().getAssets().open(modelName);
                    if(in == null)
                    {
                        Log.e("STUtil", "the src module is not existed");
                    }
                    OutputStream out = new FileOutputStream(modelFile);
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        out.write(buffer, 0, n);
                    }
                    in.close();
                    out.close();
                } catch (IOException e) {
                    modelFile.delete();
                }
            }
        }
    }

    public static String getModelPath(String modelName, Context mContext) {
        String path = null;
        File dataDir = mContext.getApplicationContext().getExternalFilesDir(null);
        if (dataDir != null) {
            path = dataDir.getAbsolutePath() + File.separator + modelName;
        }
        return path;
    }

}
