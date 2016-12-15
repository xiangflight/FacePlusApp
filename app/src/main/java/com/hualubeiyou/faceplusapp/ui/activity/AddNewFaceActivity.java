package com.hualubeiyou.faceplusapp.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.hualubeiyou.faceplusapp.R;
import com.hualubeiyou.faceplusapp.utils.ActivityStackManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AddNewFaceActivity extends AppCompatActivity {

    private static final int CODE_CAMERA_REQUEST_THUMB = 1;
    private static final int CODE_CAMERA_REQUEST_SRC = 2;


    private EditText mEtInputName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_face);
        ActivityStackManager.getInstance().pushActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindViewsAndListeners();
    }

    private void bindViewsAndListeners() {
        mEtInputName = (EditText) findViewById(R.id.et_input_name);
        ImageView mIvCamera = (ImageView) findViewById(R.id.iv_camera);
        ImageView mIvFolder = (ImageView) findViewById(R.id.iv_folder);
        setListeners(mIvCamera, mIvFolder);
    }

    private void setListeners(ImageView mIvCamera, ImageView mIvFolder) {
        mIvCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(mEtInputName.getText())) {
                    getPhotoByCamera();
                } else {
                    Toast.makeText(AddNewFaceActivity.this,
                            R.string.please_input_name_first, Toast.LENGTH_SHORT).show();
                }
            }
        });
        mIvFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(mEtInputName.getText())) {
                    getPhotoFromFolder();
                } else {
                    Toast.makeText(AddNewFaceActivity.this,
                            R.string.please_input_name_first, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void getPhotoByCamera() {
        Intent mStartCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (mStartCamera.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the file...
            }
            // Continue only if the file was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.hualubeiyou.android.fileprovider", photoFile);
                mStartCamera.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(mStartCamera, CODE_CAMERA_REQUEST_SRC);
            }
            startActivityForResult(mStartCamera, CODE_CAMERA_REQUEST_SRC);
        }
    }

    private void getPhotoFromFolder() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CODE_CAMERA_REQUEST_THUMB) {
                // data中保存的是缩略图(Thumb)
            } else if (requestCode == CODE_CAMERA_REQUEST_SRC) {
                // TODO: 2016/12/15 做上传操作
            }
        }
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

    private File createImageFile() throws IOException {
        // Create a collision-resistant image file name.
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = timeStamp + "_" + mEtInputName.getText().toString();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return image;
    }
}
