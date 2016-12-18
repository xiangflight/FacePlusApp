package com.hualubeiyou.faceplusapp.utils;

import android.app.Activity;

import java.util.ArrayList;

/**
 * Create by flight on 2016/12/15
 * use to manage activity
 */

public class ActivityStackManager {

    private ArrayList<Activity> mActivities;

    private static class ActivityStackHolder {
        private static ActivityStackManager mActivityStackManager = new ActivityStackManager();
    }

    private ActivityStackManager(){}

    public static ActivityStackManager getInstance() {
        return ActivityStackHolder.mActivityStackManager;
    }

    /**
     * 初始化ActivityStackManager.
     */
    public void init() {
        mActivities = new ArrayList<>();
    }

    /**
     * 入栈一个Activity
     * @param activity push this activity to the stack.
     */
    public void pushActivity(Activity activity) {
        if (activity != null) {
            mActivities.add(activity);
        }
    }

    /**
     * 出栈一个Activity
     * @param activity pop top activity out of the stack.
     */
    public void popActivity(Activity activity) {
        if (mActivities.contains(activity)) {
            mActivities.remove(activity);
        }
    }

    /**
     * 将ActivityStack清空
     */
    public void popAllActivities() {
        mActivities.clear();
    }

    /**
     * 显示所有栈中的activity
     */
    public void showAllActivities() {
        StringBuilder mStringBuilder = new StringBuilder();
        for (int i = mActivities.size() - 1; i >= 0; i--) {
            mStringBuilder.append(mActivities.get(i).getClass().getSimpleName());
        }
        LogUtil.d(Constants.TAG_APPLICATION, mStringBuilder.toString());
    }

}
