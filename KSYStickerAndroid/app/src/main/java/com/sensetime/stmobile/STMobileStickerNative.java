package com.sensetime.stmobile;

import android.R.integer;

public class STMobileStickerNative {
	private final static String TAG = STMobileStickerNative.class.getSimpleName();
	private static ItemCallback mCallback;

	public static void setCallback(ItemCallback callback) {
		mCallback = callback;
	}

	/**
	 * item callback function
	 * */
	public static void item_callback(String materialName, int strStatus) {
		if(mCallback != null) {
			mCallback.processTextureCallback(materialName, strStatus);
		}
	}

	static
	{
		System.loadLibrary("st_mobile");
		System.loadLibrary( "stmobile_jni" );
	}

	private long nativeStickerHandle;

	private long nativeHumanActionHandle;

	public native String generateActiveCode(String licensePath);

	public native int checkActiveCode(String licensePath, String activationCode);

	public native String generateActiveCodeFromBuffer(String licenseBuffer, int licenseSize);

	public native int checkActiveCodeFromBuffer(String licenseBuffer, int licenseSize, String activationCode);

	public native int createInstance(String zippath, String modelpath,int config);

//	public native int processBuffer(byte[] pInputImage, int rotate, int imageWidth, int imageHeight,boolean needsMirroring, int textureOut);

	public native int processTexture(int textureIn, byte[] pInputImage, int rotate, int imageWidth, int imageHeight, boolean needsMirroring, int textureOut);

	public native int changeSticker(String path);

	public native void  destoryInstance();

	public interface ItemCallback {
		void processTextureCallback(String materialName, int strStatus);
	}
}
