package com.ksyun.media.diversity.eyemorestreamer.capture;

/**
 * Created by ksysun on 16/10/20.
 */
public class EyeWrapper {
    private static final String TAG = "EyeWrapper";
    private OnFrameBuf mOnFrameBuf;

    public EyeWrapper() {}

    public interface OnFrameBuf {
        void onFrameBuf(byte[] data, int len);
    }

    public void setonFrameBufCallback(OnFrameBuf cb) {
        mOnFrameBuf = cb;
    }

    private void onFrameBuf(byte[] data, int len) {
        if (mOnFrameBuf != null) {
            mOnFrameBuf.onFrameBuf(data, len);
        }
    }
    public int initUsb(int fd, String devPath) {
        return _init_Usb(fd, devPath);
    }

    public int destroyUsb() {
        return _destroy_Usb();
    }

    static{
        System.loadLibrary("eyemoresdk");
    }
    public native int _init_Usb(int fd, String devPath);
    public native int _destroy_Usb();
}
