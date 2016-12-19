package com.hualubeiyou.faceplusapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by window on 2016/12/19
 */

public class PreferenceUtil {

    private SharedPreferences mSharedPreference;

    private PreferenceUtil(){}

    private static class PreferenceUtilsHolder {
        private static PreferenceUtil mInstance = new PreferenceUtil();
    }

    public static PreferenceUtil getInstance() {
        return PreferenceUtilsHolder.mInstance;
    }

    public void init(Context context) {
        mSharedPreference = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean setValue(String key, String value) {
        SharedPreferences.Editor editor = mSharedPreference.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    public String getValue(String key) {
        return mSharedPreference.getString(key, "");
    }

}
