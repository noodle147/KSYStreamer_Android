package com.ksyun.media.diversity.sticker.demo;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.sensetime.sensear.SenseArMaterialRender;

/**
 * Created by sensetime on 16-12-26.
 */

public class SenseARMaterialRenderBuilder {
    private static SenseARMaterialRenderBuilder sInstance;
    private SenseArMaterialRender mSenseArMaterialRender;
    private Context mContext;
    //private ImageLoaderConfig = mConfig;

    // 省略若干
    public static SenseARMaterialRenderBuilder getInstance() {
        if (sInstance == null) {
            synchronized (SenseARMaterialRenderBuilder.class) {
                if (sInstance == null) {
                    sInstance = new SenseARMaterialRenderBuilder();
                }
            }
        }
        return sInstance;
    }
    public void initSenseARMaterialRender(String modelPath, Context context){
        mContext = context;
        new SenseARMaterialRenderBuilder.SenseClientCheckTask().execute(modelPath);
    }

    public SenseArMaterialRender getSenseArMaterialRender(){
        return mSenseArMaterialRender;
    }

    private class SenseClientCheckTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            //Log.d(TAG, "init render start modelpath : " + params[0]);
            long startTime = System.currentTimeMillis();
            mSenseArMaterialRender = SenseArMaterialRender.instanceWithModelPath(mContext,
                    SenseArMaterialRender.ST_SENSEAR_ENABLE_HUMAN_ACTION | SenseArMaterialRender.ST_SENSEAR_ENABLE_BEAUTIFY,
                    params[0]);
            Log.d("init", "SenseArMaterialRender cost: "+(System.currentTimeMillis() - startTime));
            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
            //Log.d(TAG, "init render end");
        }
    }

    public void releaseSenseArMaterialRender(){
        if(mSenseArMaterialRender != null){
            mSenseArMaterialRender.release();
        }
    }
}
