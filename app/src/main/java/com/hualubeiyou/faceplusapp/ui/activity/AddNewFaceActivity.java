package com.hualubeiyou.faceplusapp.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.hualubeiyou.faceplusapp.R;
import com.hualubeiyou.faceplusapp.utils.ActivityStackManager;

public class AddNewFaceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_face);
        ActivityStackManager.getInstance().pushActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityStackManager.getInstance().showAllActivities();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ActivityStackManager.getInstance().popActivity(this);
    }

    public static void startAddNewFaceActivity(Context context) {
        Intent intent = new Intent(context, AddNewFaceActivity.class);
        context.startActivity(intent);
    }

}
