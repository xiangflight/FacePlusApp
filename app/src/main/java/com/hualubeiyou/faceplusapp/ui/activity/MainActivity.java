package com.hualubeiyou.faceplusapp.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.hualubeiyou.faceplusapp.R;
import com.hualubeiyou.faceplusapp.utils.ActivityStackManager;
import com.hualubeiyou.faceplusapp.utils.Constants;
import com.hualubeiyou.faceplusapp.utils.DeviceUtil;
import com.hualubeiyou.faceplusapp.utils.LogUtil;

/**
 * Create by flight on 2016/12/15
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_FOR_CAPTURE_LOCAL_PHOTO = 1;

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
                if (DeviceUtil.checkCameraHardware(MainActivity.this)) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED) {
                        DetectFaceActivity.startDetectFaceActivity(MainActivity.this);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_FOR_CAPTURE_LOCAL_PHOTO);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "没有检测到相机", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_FOR_CAPTURE_LOCAL_PHOTO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                DetectFaceActivity.startDetectFaceActivity(MainActivity.this);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityStackManager.getInstance().popAllActivities();
    }

}
