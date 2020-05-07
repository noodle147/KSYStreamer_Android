package com.ksyun.media.diversity.pipstreamer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Random;

public class MD5Helper {
    private static final Class<?> TAG = MD5Helper.class;

    public static String getMD5(String message) {
        return getMD5(message.getBytes());
    }

    public static String getRandomMD5() {
        Random ran = new Random();
        String mix = "" + ran.nextDouble() + System.currentTimeMillis() + ran.nextDouble();
        return getMD5(mix.getBytes());
    }

    public static String getMD5(byte[] buffer) {
        MessageDigest mdAlgorithm;
        try {
            mdAlgorithm = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            return "";
        }
        mdAlgorithm.update(buffer);
        byte[] digest = mdAlgorithm.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte aDigest : digest) {
            String plainText = Integer.toHexString(0xFF & aDigest);
            if (plainText.length() < 2) {
                plainText = "0" + plainText;
            }
            hexString.append(plainText);
        }
        return hexString.toString();
    }

    public static String getCertificateSHA1Fingerprint(Context context) {
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();
        int flags = PackageManager.GET_SIGNATURES;
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }

        Signature[] signatures = packageInfo.signatures;
        byte[] cert = signatures[0].toByteArray();
        InputStream inputStream = new ByteArrayInputStream(cert);
        CertificateFactory cf;

        try {
            cf = CertificateFactory.getInstance("X509");
        } catch (CertificateException e) {
            return null;
        }
        X509Certificate c;
        try {
            c = (X509Certificate) cf.generateCertificate(inputStream);
        } catch (CertificateException e) {
            return null;
        }

        String hexString = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] publicKey = md.digest(c.getEncoded());
            hexString = byte2HexFormatted(publicKey);
        } catch (NoSuchAlgorithmException | CertificateException e) {
        }
        return hexString;
    }


    private static String byte2HexFormatted(byte[] arr) {
        StringBuilder str = new StringBuilder(arr.length * 2);
        for (int i = 0; i < arr.length; i++) {
            String h = Integer.toHexString(arr[i]);
            int l = h.length();
            if (l == 1)
                h = "0" + h;
            if (l > 2)
                h = h.substring(l - 2, l);
            str.append(h.toUpperCase());
            if (i < (arr.length - 1))
                str.append(':');
        }
        return str.toString();
    }
}
