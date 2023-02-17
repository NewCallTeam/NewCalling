package com.cmcc.newcalllib.tool;

import android.util.Base64;

import java.nio.charset.StandardCharsets;

public class Base64Util {

    public static String strToBase64(String str) {
        byte[] byteStr = str.getBytes(StandardCharsets.UTF_8);
        return Base64.encodeToString(byteStr, Base64.DEFAULT);
    }

    public static String base64ToStr(String str) {
        byte[] byteStr = Base64.decode(str, Base64.DEFAULT);
        return new String(byteStr);
    }

    public static String encode(byte[] key) {
        return Base64.encodeToString(key, Base64.DEFAULT);
    }

    public static byte[] decode(String key) {
        return Base64.decode(key, Base64.DEFAULT);
    }
}

