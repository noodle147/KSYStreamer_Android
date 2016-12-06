package com.sensetime.stmobile;

import java.util.ArrayList;

public class STImageFilterNative {
	static
	{
		System.loadLibrary("st_mobile");
		System.loadLibrary( "stmobile_jni" );
	}
	
	private long nativeHandle;
	
	public native int initBeautify(int width, int height);

	public native int setParam(int type, float value);
	
	public native int processBufferWithCurrentContext(byte[] pInputImage, int inFormat, int outputWidth, int outputHeight, byte[] pOutImage, int outFormat);
	
	public native int processBufferWithNewContext(byte[] pInputImage, int inFormat, int outputWidth, int outputHeight, byte[] pOutImage, int outFormat);
	
	public native int processTexture(int textureIn, int outputWidth, int outputHeight, int textureOut);
	
	public native void destoryBeautify();
	
	public static native int stColorConvert(byte[] imagesrc, byte[] imagedst, int imageWidth, int imageHeight, int type);
	
}
