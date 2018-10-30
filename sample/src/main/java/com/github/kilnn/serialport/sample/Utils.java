package com.github.kilnn.serialport.sample;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

public class Utils {

    /**
     * 获取可用运存大小
     */
    public static long getAvailMemory(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                return mi.availMem / (1024 * 1024);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取总运存大小
     */
    public static long getTotalMemory(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    return mi.totalMem / (1024 * 1024);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }


}
