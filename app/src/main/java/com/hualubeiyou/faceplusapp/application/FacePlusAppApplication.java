package com.hualubeiyou.faceplusapp.application;

import android.app.Application;

import com.hualubeiyou.faceplusapp.utils.ActivityStackManager;

/**
 * Created by flight on 2016/12/15
 */

public class FacePlusAppApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ActivityStackManager.getInstance().init();
    }
}
