package com.hualubeiyou.faceplusapp.ui.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
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
import android.widget.Toast;

import com.hualubeiyou.faceplusapp.R;
import com.hualubeiyou.faceplusapp.utils.ActivityStackManager;
import com.hualubeiyou.faceplusapp.utils.Constants;
import com.hualubeiyou.faceplusapp.utils.DeviceUtil;
import com.hualubeiyou.faceplusapp.utils.FileUtil;
import com.hualubeiyou.faceplusapp.utils.LogUtil;
import com.hualubeiyou.faceplusapp.utils.NetworkUtil;
import com.hualubeiyou.faceplusapp.utils.PreferenceUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddNewFaceActivity extends AppCompatActivity {

    private static final int CODE_CAMERA_REQUEST_SRC = 1;

    private static final int CODE_PICTURES_REQUEST_SRC = 2;

    private static final int REQUEST_FOR_OPEN_CAMERA_AND_WRITE_EXTERNAL = 3;

    private static final int REQUEST_FOR_COPY_LOCAL_FILE = 4;

    private static final int REQUEST_FOR_RECORD_AUDIO = 5;

    private Button mBtnUpload;
    private EditText mEtInputName;
    private EditText mEtPersonInfo;
    private ImageView mIvPicture;

    private ImageView mIvRecord;
    private MediaRecorder mRecorder;
    private boolean mStartRecording;

    // 当前需要上传的文件路径
    private String mUploadPhotoName;
    private String mUploadPhotoPath;
    private String mCameraPhotoPath;
    private String mFaceToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_face);
        ActivityStackManager.getInstance().pushActivity(this);
        mStartRecording = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindViewsAndListeners();
    }

    private void bindViewsAndListeners() {
        mEtInputName = (EditText) findViewById(R.id.et_input_name);
        mEtPersonInfo = (EditText) findViewById(R.id.et_person_info);
        ImageView mIvCamera = (ImageView) findViewById(R.id.iv_camera);
        ImageView mIvFolder = (ImageView) findViewById(R.id.iv_folder);
        mIvPicture = (ImageView) findViewById(R.id.iv_show);
        mBtnUpload = (Button) findViewById(R.id.btn_upload);
        mIvRecord = (ImageView) findViewById(R.id.iv_record);
        setListeners(mIvCamera, mIvFolder);
    }

    private void setListeners(ImageView mIvCamera, ImageView mIvFolder) {
        mIvCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (DeviceUtil.checkCameraHardware(AddNewFaceActivity.this)) {
                    if (!TextUtils.isEmpty(mEtInputName.getText())) {
                        openCamera();
                    } else {
                        Toast.makeText(AddNewFaceActivity.this,
                                R.string.please_input_name_first, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AddNewFaceActivity.this, "没有检测到相机", Toast.LENGTH_SHORT).show();
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

    public void recordIntro(View view) {
        if (!TextUtils.isEmpty(mEtInputName.getText())) {
            verifyAudioPermission();
        } else {
            Toast.makeText(AddNewFaceActivity.this,
                    R.string.please_input_name_first, Toast.LENGTH_SHORT).show();
        }
    }

    public void verifyAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            recordAudio();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_FOR_RECORD_AUDIO);
        }
    }

    public void recordAudio() {
        onRecord(mStartRecording);
        if (mStartRecording) {
            mIvRecord.setImageResource(R.drawable.ic_play_record);
        } else {
            mIvRecord.setImageResource(R.drawable.ic_pause_record);
        }
        mStartRecording = ! mStartRecording;
    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        // Create the File where the record file should go
        File recordFile = null;
        try {
            recordFile = createRecordFile();
        } catch (IOException ex) {
            // Error occurred while creating the file...
        }
        // Continue only if the file was successfully created
        if (recordFile != null) {
            mRecorder.setOutputFile(recordFile.getAbsolutePath());
        }
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            LogUtil.e(Constants.TAG_APPLICATION, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
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
        } else if (requestCode == REQUEST_FOR_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recordAudio();
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
                    photoURI = FileProvider.getUriForFile(this, Constants.authority, photoFile);
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
        final Bitmap portraitBitmap = getScaledImage(mCameraPhotoPath, mIvPicture);
        mIvPicture.setImageBitmap(portraitBitmap);
        // 压缩后的Bitmap，存入Documents文件夹
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                File file = null;
                try {
                    file = createCompressedFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (file != null) {
                    FileUtil.bitmapToJpeg(portraitBitmap, file);
                }
                return null;
            }
        }.execute();
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
        CompressedTask mCompressedTask = new CompressedTask();
        mCompressedTask.execute(bitmap);
    }

    private class CompressedTask extends AsyncTask<Bitmap, Void, Void> {

        @Override
        protected Void doInBackground(Bitmap... bitmap) {
            File upLoadFile = null;
            try {
                upLoadFile = createCompressedFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (upLoadFile != null) {
                FileUtil.bitmapToJpeg(bitmap[0], upLoadFile);
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
        LogUtil.d(Constants.TAG_APPLICATION, PreferenceUtil.getInstance().getValue(Constants.OUTER_ID_KEY) +
                                              "-----" + Constants.OUTER_ID_TEST);
        if (!PreferenceUtil.getInstance().getValue(Constants.OUTER_ID_KEY)
                .equals(Constants.OUTER_ID_TEST)) {
            createFaceSet();
        }
        boolean storage = storePersonInfo();
        LogUtil.d(Constants.TAG_APPLICATION, "storage status is " + storage);
        realUpLoadImage();
    }

    private boolean storePersonInfo() {
        return PreferenceUtil.getInstance().setValue(mEtInputName.getText().toString(),
                mEtPersonInfo.getText().toString());
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
        formBodyBuild.add(Constants.PARAMETER_OUTER_ID, Constants.OUTER_ID_TEST);
        Request request = new Request.Builder()
                .url(Constants.URL_FACESET_CREATE)
                .post(formBodyBuild.build())
                .build();
        Call mCall = client.newCall(request);
        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtil.e(Constants.TAG_APPLICATION, e.toString());
                showUIToast("create FaceSet failure");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                LogUtil.i(Constants.TAG_APPLICATION, response.body().string());
                if (!response.isSuccessful()) {
                    showUIToast("create FaceSet failure");
                } else {
                    PreferenceUtil
                            .getInstance().setValue(Constants.OUTER_ID_KEY, Constants.OUTER_ID_TEST);
                }

            }
        });
    }

    private void realUpLoadImage() {
        detectFace();
    }

    /**
     * 检测人脸
     */
    private void detectFace() {
        File detectFile = new File(mUploadPhotoPath);
        OkHttpClient client = new OkHttpClient();
        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(Constants.PARAMETER_IMAGE_FILE, detectFile.getName(), RequestBody.create(MediaType.parse("image/jpg"), detectFile))
                    .addFormDataPart(Constants.PARAMETER_API_KEY, Constants.API_KEY_APPLICATION)
                    .addFormDataPart(Constants.PARAMETER_API_SECRET, Constants.API_SECRET_APPLICATION)
                    .build();
            Request request = new Request.Builder()
                    .url(Constants.URL_FACE_DETECT)
                    .post(requestBody)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    LogUtil.e(Constants.TAG_APPLICATION, e.toString());
                    showUIToast("检测人脸失败");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String localResponse = response.body().string();
                    LogUtil.i(Constants.TAG_APPLICATION, localResponse);
                    if (!response.isSuccessful()) {
                        showUIToast("检测人脸失败");
                    } else {
                        try {
                            JSONObject result = new JSONObject(localResponse);
                            JSONArray facesArray = result.getJSONArray(Constants.VALUE_RETURN_FACES);
                            if (facesArray.length() != 0) {
                                mFaceToken = facesArray.getJSONObject(0)
                                        .getString(Constants.VALUE_RETURN_FACE_TOKEN);
                                LogUtil.i(Constants.TAG_APPLICATION, "face token is " + mFaceToken);
                                setFaceUserId();
                            } else {
                                showUIToast("没有人脸");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setFaceUserId() {
        OkHttpClient client = new OkHttpClient();
        String userId = mUploadPhotoName.split("_")[1];
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        formBodyBuilder.add(Constants.PARAMETER_API_KEY, Constants.API_KEY_APPLICATION);
        formBodyBuilder.add(Constants.PARAMETER_API_SECRET, Constants.API_SECRET_APPLICATION);
        formBodyBuilder.add(Constants.VALUE_RETURN_FACE_TOKEN, mFaceToken);
        formBodyBuilder.add(Constants.PARAMETER_USER_ID, userId);
        final Request request = new Request.Builder()
                .url(Constants.URL_FACE_SETUSERID)
                .post(formBodyBuilder.build())
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtil.e(Constants.TAG_APPLICATION, e.toString());
                showUIToast("设置人脸身份失败");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    showUIToast("设置人脸身份失败");
                } else {
                    LogUtil.i(Constants.TAG_APPLICATION, response.body().string());
                    addFaceToFaceSet();
                }
            }
        });
    }

    /**
     * 添加人脸到faceSet中
     */
    private void addFaceToFaceSet() {
        OkHttpClient client = new OkHttpClient();
        FormBody formBody = new FormBody.Builder()
                .add(Constants.PARAMETER_API_KEY, Constants.API_KEY_APPLICATION)
                .add(Constants.PARAMETER_API_SECRET, Constants.API_SECRET_APPLICATION)
                .add(Constants.PARAMETER_OUTER_ID, Constants.OUTER_ID_TEST)
                .add(Constants.PARAMETER_FACE_TOKENS, mFaceToken)
                .build();
        Request request = new Request.Builder()
                .url(Constants.URL_FACESET_ADDFACE)
                .post(formBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtil.e(Constants.TAG_APPLICATION, e.toString());
                showUIToast("添加人脸失败");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String localResponse = response.body().string();
                LogUtil.i(Constants.TAG_APPLICATION, localResponse);
                if (!response.isSuccessful()) {
                    showUIToast("添加人脸失败");
                } else {
                    showUIToast("添加人脸成功");
                    try {
                        JSONObject values = new JSONObject(localResponse);
                        LogUtil.i(Constants.TAG_APPLICATION,
                                "outer_id is " + values.getString(Constants.PARAMETER_OUTER_ID)
                                + ";face_count is " + values.getInt(Constants.VALUE_RETURN_FACE_COUNT)
                                + ";face_added is " + values.getInt(Constants.VALUE_RETURN_FACE_ADDED)
                        );
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
        ActivityStackManager.getInstance().popActivity(this);
    }

    public static void startAddNewFaceActivity(Context context) {
        Intent intent = new Intent(context, AddNewFaceActivity.class);
        context.startActivity(intent);
    }

    private File createRecordFile() throws IOException {
        String timeStamp = new SimpleDateFormat(Constants.FILE_NAME_SUFFIX_FORMAT, Locale.CHINA).format(new Date());
        String recordFileName = timeStamp + "_" + mEtInputName.getText() + "_";
        LogUtil.d(Constants.TAG_APPLICATION, "recordFile Name is " + recordFileName);
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
//        if (storageDir != null && storageDir.listFiles().length >= Constants.UP_LIMIT_FILES) {
//            for (File file : storageDir.listFiles()) {
//                file.deleteOnExit();
//            }
//        }
        return File.createTempFile(
                recordFileName, /* prefix */
                ".3gp",        /* suffix */
                storageDir     /* directory */
        );
    }
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat(Constants.FILE_NAME_SUFFIX_FORMAT, Locale.CHINA).format(new Date());
        String currentPhotoName = timeStamp + "_" + mEtInputName.getText() + "_";
        LogUtil.d(Constants.TAG_APPLICATION, "imageFile name is " + currentPhotoName);
        // private storage
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        if (storageDir != null && storageDir.listFiles().length >= Constants.UP_LIMIT_FILES) {
//            for (File file : storageDir.listFiles()) {
//                file.deleteOnExit();
//            }
//        }
        File image = File.createTempFile(
                currentPhotoName, /* prefix */
                ".jpg",        /* suffix */
                storageDir     /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCameraPhotoPath = image.getAbsolutePath();
        return image;
    }

    private File createCompressedFile() throws IOException {
        String timeStamp = new SimpleDateFormat(Constants.FILE_NAME_SUFFIX_FORMAT, Locale.CHINA).format(new Date());
        mUploadPhotoName = timeStamp + "_" + mEtInputName.getText() + "_";
        LogUtil.d(Constants.TAG_APPLICATION, "upLoadFile Name is " + mUploadPhotoName);
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
//        if (storageDir != null && storageDir.listFiles().length >= Constants.UP_LIMIT_FILES) {
//            for (File file : storageDir.listFiles()) {
//                file.deleteOnExit();
//            }
//        }
        File upLoadFile =  File.createTempFile(
                mUploadPhotoName, /* prefix */
                ".jpg",        /* suffix */
                storageDir     /* directory */
        );
        mUploadPhotoPath = upLoadFile.getAbsolutePath();
        return upLoadFile;
    }

    // Add the portrait to the galley
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File portraitPhoto = new File(mCameraPhotoPath);
        Uri portraitUri = Uri.fromFile(portraitPhoto);
        mediaScanIntent.setData(portraitUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void showUIToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AddNewFaceActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
