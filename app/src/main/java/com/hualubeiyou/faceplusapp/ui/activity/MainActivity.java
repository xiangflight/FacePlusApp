package com.hualubeiyou.faceplusapp.ui.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.hualubeiyou.faceplusapp.R;
import com.hualubeiyou.faceplusapp.utils.ActivityStackManager;

/**
 * Create by flight on 2016/12/15
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityStackManager.getInstance().pushActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindViewsAndListeners();
    }

    private void bindViewsAndListeners() {
        ActivityStackManager.getInstance().showAllActivities();
        Button mBtnBaseOne = (Button) findViewById(R.id.btn_base_one);
        Button mBtnBaseTwo = (Button) findViewById(R.id.btn_base_two);
        setListeners(mBtnBaseOne, mBtnBaseTwo);
    }

    private void setListeners(Button button1, Button button2) {
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddNewFaceActivity.startAddNewFaceActivity(MainActivity.this);
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DetectFaceActivity.startDetectFaceActivity(MainActivity.this);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityStackManager.getInstance().popAllActivities();
    }
}
