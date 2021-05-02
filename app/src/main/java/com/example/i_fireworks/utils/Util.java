package com.example.i_fireworks.utils;

import android.content.res.Resources;
import android.util.TypedValue;

import java.nio.ByteBuffer;

public class Util {
    public static float px2dp(Resources resource, float px)  {
        return (float) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_PX,
                px,
                resource.getDisplayMetrics()
        );
    }
    public static byte[] intToByteArray(int val){
        return ByteBuffer.allocate(4).putInt(val).array();
    }

    public static int byteArrayToInt(byte [] arr){
        return ByteBuffer.wrap(arr).getInt();
    }
    public static byte[] stringToByteArray(String str){
        return str.getBytes();
    }
}
