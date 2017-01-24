package com.ksyun.media.diversity.sticker.demo.utils;


import com.ksyun.media.diversity.sticker.demo.R;

/**
 * Created by sensetime on 16-10-28.
 */

public class TriggerActionUtils {
    enum TriggerActionImage {
        TRIGGER_ACTION_MOUTH(1, R.drawable.ic_trigger_mouth),
        TRIGGER_ACTION_BLINK(2, R.drawable.ic_trigger_blink),
        TRIGGER_ACTION_NOD(3, R.drawable.ic_trigger_nod),
        TRIGGER_ACTION_SHAKE(4, R.drawable.ic_trigger_shake),
        TRIGGER_ACTION_FROWN(5, R.drawable.ic_trigger_frown),
        TRIGGER_ACTION_PALM(6, R.drawable.ic_trigger_palm),
        TRIGGER_ACTION_THUMB(7, R.drawable.ic_trigger_thumb),
        TRIGGER_ACTION_PALM_UP(8, R.drawable.ic_trigger_palm_up),
        TRIGGER_ACTION_HEART_HAND(9, R.drawable.ic_trigger_heart_hand),
        TRIGGER_ACTION_OK(10,R.drawable.ic_trigger_ok),
        TRIGGER_ACTION_PISTOL(11,R.drawable.ic_trigger_pistol),
        TRIGGER_ACTION_CONGRATULATE(12,R.drawable.ic_trigger_congratulate);

        private int mAction;
        private int mResId;
        TriggerActionImage(int action, int resID) {
            mAction = action;
            mResId = resID;
        }

        public int getAction() {
            return mAction;
        }

        public int getResId() {
            return mResId;
        }
    }

    public static int getTriggerImageById(int triggerId) {
        TriggerActionImage[] values = TriggerActionImage.values();
        for (TriggerActionImage image : values) {
            if (image.getAction() == triggerId) {
                return image.getResId();
            }
        }

        return -1;
    }
}
