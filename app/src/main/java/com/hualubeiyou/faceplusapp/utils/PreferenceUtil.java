package com.hualubeiyou.faceplusapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by window on 2016/12/19
 */

public class PreferenceUtil {

    private SharedPreferences mSharedPreference;
    private SharedPreferences.Editor mEditor;

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
        mEditor = mSharedPreference.edit();
        mEditor.putString(key, value);
        return mEditor.commit();
    }

    public String getValue(String key) {
        return mSharedPreference.getString(key, "");
    }

}
