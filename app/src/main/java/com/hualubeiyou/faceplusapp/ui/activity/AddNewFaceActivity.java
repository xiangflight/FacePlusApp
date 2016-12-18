package com.hualubeiyou.faceplusapp.ui.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.hualubeiyou.faceplusapp.utils.NetworkUtil;
import com.hualubeiyou.faceplusapp.utils.PreferenceUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AddNewFaceActivity extends AppCompatActivity {

    private static final int CODE_CAMERA_REQUEST_SRC = 1;

    private static final int CODE_PICTURES_REQUEST_SRC = 2;

    private static final int REQUEST_FOR_OPEN_CAMERA_AND_WRITE_EXTERNAL = 3;

    private static final int REQUEST_FOR_COPY_LOCAL_FILE = 4;

    private static final int UP_LIMIT_FILES = 10;

    private static final String authority = "com.hualubeiyou.android.fileprovider";

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
                    openCamera();
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
                    openLocalFolder();
                } else {
                    Toast.makeText(AddNewFaceActivity.this,
                            R.string.please_input_name_first, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            getPhotoByCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_FOR_OPEN_CAMERA_AND_WRITE_EXTERNAL);
        }
    }

    private void openLocalFolder() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            getPhotoFromFolder();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_FOR_COPY_LOCAL_FILE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_FOR_OPEN_CAMERA_AND_WRITE_EXTERNAL) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                getPhotoByCamera();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_FOR_COPY_LOCAL_FILE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getPhotoFromFolder();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
                    photoURI = FileProvider.getUriForFile(this, authority, photoFile);
                } else {
                    photoURI = Uri.fromFile(photoFile);
                }
                mStartCamera.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(mStartCamera, CODE_CAMERA_REQUEST_SRC);
            }
        }
    }

    private void getPhotoFromFolder() {
        Intent getLocalPictures = new Intent();
        getLocalPictures.setType("image/*");
        getLocalPictures.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(getLocalPictures, CODE_PICTURES_REQUEST_SRC);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CODE_CAMERA_REQUEST_SRC) {
                galleryAddPic();
                showOriginalImage();
            } else if (requestCode == CODE_PICTURES_REQUEST_SRC) {
                showLocalImage(data);
            }
            mBtnUpload.setVisibility(View.VISIBLE);
            mTvPortraitName.setVisibility(View.VISIBLE);
            mTvPortraitName.setText(mEtInputName.getText());
        }
    }

    private Bitmap getScaledImage(String filePath, ImageView imageView) {
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        return BitmapFactory.decodeFile(filePath, bmOptions);
    }

    private void showOriginalImage() {
        Bitmap portraitBitmap = getScaledImage(mCurrentPhotoPath, mIvPicture);
        mIvPicture.setImageBitmap(portraitBitmap);
    }

    private void showLocalImage(Intent data) {
        Uri uri = data.getData();
        ContentResolver cr = this.getContentResolver();
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mIvPicture.setImageBitmap(bitmap);
        // 新创建一个文件，写入该uri对应的文件内容，是一个I/O操作，放在AsyncTask里写
        CopiedFileTask mCopyTask = new CopiedFileTask();
        mCopyTask.execute(uri);
    }

    private class CopiedFileTask extends AsyncTask<Uri, Void, Void> {

        @Override
        protected Void doInBackground(Uri... uris) {
            File copiedFile;
            String destPath = null;
            try {
                copiedFile = createImageFile();
                destPath = copiedFile.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String srcPath = getRealPathFromUri_AboveApi19(AddNewFaceActivity.this, uris[0]);
            if(copyFile(srcPath, destPath)) {
                LogUtil.d(Constants.TAG_APPLICATION, "copy successfully");
            }
            return null;
        }
    }

    public void uploadImage(View view) {
        if (NetworkUtil.isConnected(this)) {
            upLoadImage();
        } else {
            Toast.makeText(this, "需要连接网络", Toast.LENGTH_SHORT).show();
        }
    }

    private void upLoadImage() {
        if (!PreferenceUtil.getInstance().getValue(Constants.OUT_ID_KEY)
                .equals(Constants.OUT_ID_TEST)) {
            createFaceSet();
        }
        realUpLoadImage();
    }

    /**
     * should only create once.
     */
    private void createFaceSet() {
        LogUtil.i(Constants.TAG_APPLICATION, "create a new FaceSet id");
        OkHttpClient client = new OkHttpClient();
        FormBody.Builder formBodyBuild = new FormBody.Builder();
        formBodyBuild.add(Constants.PARAMETER_API_KEY, Constants.API_KEY_APPLICATION);
        formBodyBuild.add(Constants.PARAMETER_API_SECRET, Constants.API_SECRET_APPLICATION);
        formBodyBuild.add(Constants.PARAMETER_OUTER_ID, Constants.OUT_ID_TEST);
        Request mRequest = new Request.Builder()
                .url(Constants.URL_FACESET_CREATE)
                .post(formBodyBuild.build())
                .build();
        Call mCall = client.newCall(mRequest);
        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtil.e(Constants.TAG_APPLICATION, e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                LogUtil.i(Constants.TAG_APPLICATION, response.body().string());
            }
        });
    }

    private void realUpLoadImage() {
        // TODO: 2016/12/19
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
        String timeStamp = new SimpleDateFormat(Constants.FILE_NAME_SUFFIX_FORMAT, Locale.CHINA).format(new Date());
        String imageFileName = timeStamp + "_" + mEtInputName.getText() + "_";
        LogUtil.d(Constants.TAG_APPLICATION, "imageFile name is " + imageFileName);
        // private storage
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && storageDir.listFiles().length >= UP_LIMIT_FILES) {
            for (File file : storageDir.listFiles()) {
                file.deleteOnExit();
            }
        }
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

    /**
     * 适配api19以上,根据uri获取图片的绝对路径
     */
    private static String getRealPathFromUri_AboveApi19(Context context, Uri uri) {
        String filePath = null;
        String wholeID = DocumentsContract.getDocumentId(uri);

        // 使用':'分割
        String id = wholeID.split(":")[1];

        String[] projection = {MediaStore.Images.Media.DATA};
        String selection = MediaStore.Images.Media._ID + "=?";
        String[] selectionArgs = {id};

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                selection, selectionArgs, null);
        int columnIndex;
        if (cursor != null) {
            columnIndex = cursor.getColumnIndex(projection[0]);
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
        }
        return filePath;
    }

    /**
     * 复制单个文件
     * @param srcPath String 原文件路径
     * @param destPath String 复制后路径
     * @return boolean
     */
    public boolean copyFile(String srcPath, String destPath) {
        try {
            int byteRead;
            FileInputStream fis = new FileInputStream(srcPath); //读入原文件
            FileOutputStream fos = new FileOutputStream(destPath); //写入新文件
            byte[] buffer = new byte[1444];
            while ( (byteRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, byteRead);
            }
            fis.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.d(Constants.TAG_APPLICATION, "copy error");
            return false;
        }
    }

}
