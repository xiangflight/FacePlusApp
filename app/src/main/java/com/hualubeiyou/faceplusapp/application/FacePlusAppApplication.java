package com.hualubeiyou.faceplusapp.application;

import android.app.Application;

import com.hualubeiyou.faceplusapp.utils.ActivityStackManager;
import com.hualubeiyou.faceplusapp.utils.Constants;
import com.hualubeiyou.faceplusapp.utils.PreferenceUtil;

/**
 * Created by flight on 2016/12/15
 */

public class FacePlusAppApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ActivityStackManager.getInstance().init();
        PreferenceUtil.getInstance().init(this);
        Constants.DETECT_USE_LIMIT = 5;
    }
}
