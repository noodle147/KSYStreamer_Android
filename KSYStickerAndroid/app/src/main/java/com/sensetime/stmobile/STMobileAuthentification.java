package com.sensetime.stmobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class STMobileAuthentification {
	private String TAG = this.getClass().getSimpleName();
    
	private Context mContext;
    private STMobileStickerNative mSticker;

    private AuthCallback authCallback = null;
    private String licenseStr = "";

    public STMobileAuthentification(Context context, boolean authFromBuffer, AuthCallback callback) {
    	mContext = context;
    	this.authCallback = callback;
    	mSticker = new STMobileStickerNative();

        STUtils.copyModelIfNeed(STUtils.MODEL_NAME, mContext);
        if(!authFromBuffer) {                   //if authentificate by sdCard
            STUtils.copyModelIfNeed(STUtils.LICENSE_NAME, mContext);
        } else {

            try {
                InputStreamReader isr = new InputStreamReader(context.getResources().getAssets().open(STUtils.LICENSE_NAME));
                BufferedReader br = new BufferedReader(isr);
                String line = "";
                while((line=br.readLine()) != null) {
                    licenseStr += line;
                    licenseStr += "\n";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
	

    public boolean hasAuthentificatedByBuffer() {
        String generatedActiveCode = null;
        SharedPreferences sp = mContext.getSharedPreferences("ActiveCodeFile", 0);
        boolean isFirst = sp.getBoolean("isFirst", true);
        int rst = Integer.MIN_VALUE;
        if(isFirst) {
            generatedActiveCode = mSticker.generateActiveCodeFromBuffer(licenseStr, licenseStr.length());
            if(generatedActiveCode == null || generatedActiveCode.length() == 0) {
            	authCallback.authErr("generate active code failed! active code is null");
                return false;
            }

            SharedPreferences.Editor editor = sp.edit();
            editor.putString("activecode", generatedActiveCode);
            editor.putBoolean("isFirst", false);
            editor.commit();
        }

        String activeCode = sp.getString("activecode", "null");
        if(activeCode==null || activeCode.length()==0) {
        	authCallback.authErr("activeCode is null in SharedPreference!");
            return false;
        }

        rst = mSticker.checkActiveCodeFromBuffer( licenseStr, licenseStr.length(), activeCode);
        if(rst != STUtils.ResultCode.ST_OK.getResultCode()) {
        	authCallback.authErr("check activecode failed! errCode="+rst);

            generatedActiveCode = mSticker.generateActiveCodeFromBuffer(licenseStr, licenseStr.length());

            if(generatedActiveCode == null || generatedActiveCode.length() == 0) {
                authCallback.authErr("generate active code failed! active code is null");
                return false;
            }
            rst = mSticker.checkActiveCodeFromBuffer( licenseStr, licenseStr.length(), generatedActiveCode);
            if(rst != STUtils.ResultCode.ST_OK.getResultCode()) {
                authCallback.authErr("again check active code failed, you need a new license! errCode="+rst);
                return false;
            }

            SharedPreferences.Editor editor = sp.edit();
            editor.putString("activecode", generatedActiveCode);
            editor.putBoolean("isFirst", false);
            editor.commit();
        }
        
        authCallback.authErr("you have been authorized!");
        
        return true;
    }
    

    public boolean hasAuthentificatd() {
        String generatedActiveCode = null;
    	String licensePath = STUtils.getModelPath(STUtils.LICENSE_NAME, mContext);
        SharedPreferences sp = mContext.getSharedPreferences("ActiveCodeFile", 0);
        boolean isFirst = sp.getBoolean("isFirst", true);
        int rst = Integer.MIN_VALUE;
        if(isFirst) {
            Log.i(TAG, "-->>hasAuthentificatd: licensePath = "+licensePath);
            generatedActiveCode = mSticker.generateActiveCode( licensePath);
            if(generatedActiveCode == null || generatedActiveCode.length() == 0) {
                authCallback.authErr("generate active code failed! active code is null");
                return false;
            }
            
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("activecode", generatedActiveCode);
            editor.putBoolean("isFirst", false);
            editor.commit();
        }

        String activeCode = sp.getString("activecode", "null");
        if(activeCode==null || activeCode.length()==0) {
        	authCallback.authErr("activeCode is null in SharedPreference!");
            return false;
        }

        rst = mSticker.checkActiveCode( licensePath, activeCode);
        if(rst != STUtils.ResultCode.ST_OK.getResultCode()) {
        	authCallback.authErr("check activecode failed! errCode="+rst);

            generatedActiveCode = mSticker.generateActiveCode(licensePath);
            if(generatedActiveCode == null || generatedActiveCode.length() == 0) {
                authCallback.authErr("generate active code failed! active code is null");
                return false;
            }

            rst = mSticker.checkActiveCodeFromBuffer( licenseStr, licenseStr.length(), generatedActiveCode);
            if(rst != STUtils.ResultCode.ST_OK.getResultCode()) {
                authCallback.authErr("again check active code failed, you need a new license! errCode="+rst);
                return false;
            }

            SharedPreferences.Editor editor = sp.edit();
            editor.putString("activecode", generatedActiveCode);
            editor.putBoolean("isFirst", false);
            editor.commit();
        }

        authCallback.authErr("you have been authorized!");
        return true;
    }
    
}
