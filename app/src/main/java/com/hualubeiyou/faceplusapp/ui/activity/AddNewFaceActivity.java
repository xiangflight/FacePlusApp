package com.hualubeiyou.faceplusapp.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hualubeiyou.faceplusapp.R;
import com.hualubeiyou.faceplusapp.utils.ActivityStackManager;
import com.hualubeiyou.faceplusapp.utils.Constants;
import com.hualubeiyou.faceplusapp.utils.LogUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddNewFaceActivity extends AppCompatActivity {

    private static final int CODE_CAMERA_REQUEST_SRC = 1;

    private Button mBtnUpload;
    private EditText mEtInputName;
    private ImageView mIvPicture;
    private TextView mTvPortraitName;

    private String mCurrentPhotoPath;

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
        mIvPicture = (ImageView) findViewById(R.id.iv_show);
        mTvPortraitName = (TextView) findViewById(R.id.tv_portrait_name);
        mBtnUpload = (Button) findViewById(R.id.btn_upload);
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
                Uri photoURI;
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    photoURI = FileProvider.getUriForFile(this, "com.hualubeiyou.android.fileprovider", photoFile);
                } else {
                    photoURI = Uri.fromFile(photoFile);
                }
                mStartCamera.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(mStartCamera, CODE_CAMERA_REQUEST_SRC);
            }
        }
    }

    private void getPhotoFromFolder() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CODE_CAMERA_REQUEST_SRC) {
                galleryAddPic();
                mBtnUpload.setVisibility(View.VISIBLE);
                mTvPortraitName.setVisibility(View.VISIBLE);
                mTvPortraitName.setText(mEtInputName.getText());
                showOriginalImage();
            }
        }
    }

    private void showOriginalImage() {
        int targetW = mIvPicture.getWidth();
        int targetH = mIvPicture.getHeight();
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        Bitmap portraitBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mIvPicture.setImageBitmap(portraitBitmap);
    }

    public void uploadImage(View view) {
        // TODO: 2016/12/16 upload image
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
        String timeStamp = new SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(new Date());
        String imageFileName = timeStamp + "_" + mEtInputName.getText() + "_";
        LogUtil.d(Constants.TAG_APPLICATION, "imageFile name is " + imageFileName);
        // private storage
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg",        /* suffix */
                storageDir     /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // Add the portrait to the galley
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File portraitPhoto = new File(mCurrentPhotoPath);
        Uri portraitUri = Uri.fromFile(portraitPhoto);
        mediaScanIntent.setData(portraitUri);
        this.sendBroadcast(mediaScanIntent);
    }
}
