package com.ksyun.media.diversity.sticker.demo;

import android.graphics.Bitmap;

import com.sensetime.sensear.SenseArMaterial;

/**
 * Created by sensetime on 16-7-6.
 */
public class MaterialInfoItem {
    public Bitmap thumbnail;
    public SenseArMaterial material;
    public boolean hasDownload = false;
    public int commissionType = 0;
    public int intervalTime = 0;
    public boolean mTriggerCoolDown = false;

    public MaterialInfoItem(SenseArMaterial material, Bitmap thumbnail) {
        this.material = material;
        this.thumbnail = thumbnail;
//        String triggerThreshold = material.triggerThreshold;
//        if(triggerThreshold != null) {
        int intervaltime = material.intervalTime;
            /* disable cool down
            try {
                JSONObject json = new JSONObject(triggerThreshold);
                intervaltime = json.getInt("interval_time");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            this.intervalTime = intervaltime;
            */
            if(intervaltime != 0){
                this.mTriggerCoolDown = true;
            }
//        }
    }

    public void setHasDownload(boolean isDownloaded){
        this.hasDownload = isDownloaded;
    }

    public void setCommissionType(int type){
        this.commissionType = type;
    }

    public void setTriggerCoolDowntag(boolean istrigger){
        this.mTriggerCoolDown = istrigger;
    }
}
