package com.hualubeiyou.faceplusapp.utils;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Created by flight on 2016/12/20
 */

public class DeviceUtil {

    /** Check if this device has a camera */
    public static boolean checkCameraHardware(Context context) {
        // this device has a camera
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }
}
