package com.hualubeiyou.faceplusapp.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hualubeiyou.faceplusapp.R;
import com.hualubeiyou.faceplusapp.ui.views.CameraPreview;
import com.hualubeiyou.faceplusapp.utils.ActivityStackManager;
import com.hualubeiyou.faceplusapp.utils.Constants;
import com.hualubeiyou.faceplusapp.utils.FileUtil;
import com.hualubeiyou.faceplusapp.utils.LogUtil;
import com.hualubeiyou.faceplusapp.utils.PreferenceUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DetectFaceActivity extends AppCompatActivity implements Camera.PreviewCallback{

    private Camera mCamera;

    private FaceDetectTask mFaceDetectTask;

    private static final int ID_FRONT_CAMERA = 1;

    private ImageView mIvPersonPhoto;
    private TextView mTvPersonName;
    private TextView mTvPersonInfo;
    private ImageView mIvPlayIntro;

    private MediaPlayer mMediaPlayer;
    private boolean isFirstPlay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_face);
        ActivityStackManager.getInstance().pushActivity(this);

        // Create an instance of Camera in a separate thread not to bog down the UI thread.
        mCamera = getFrontCamera(ID_FRONT_CAMERA);
        initView();
    }

    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        final String introFileName = mTvPersonName.getText().toString();
        // a I/O operation
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                File introFile = getPersonRecord(introFileName);
                if (introFile != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIvPlayIntro.setVisibility(View.VISIBLE);
                        }
                    });
                    Uri introUri = Uri.fromFile(introFile);
                    isFirstPlay = true;
                    try {
                        mMediaPlayer.setDataSource(DetectFaceActivity.this, introUri);
                        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                mMediaPlayer.start();
                                isFirstPlay = false;
                            }
                        });
                        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                mIvPlayIntro.setImageResource(R.drawable.ic_play_intro);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    showUIToast("没有录制介绍");
                }
                return null;
            }
        }.execute();
    }

    /** Create our Preview view and set it as the content of our activity */
    private void initView() {
        FrameLayout previewContainer = (FrameLayout) findViewById(R.id.camera_preview_container);
        previewContainer.setOnClickListener(onClickListener);
        CameraPreview preview = (CameraPreview) findViewById(R.id.camera_preview);
        preview.setCamera(mCamera);
        mIvPersonPhoto = (ImageView) findViewById(R.id.iv_detect_person);
        mTvPersonName = (TextView) findViewById(R.id.tv_detect_name);
        mTvPersonInfo = (TextView) findViewById(R.id.tv_person_info);
        mIvPlayIntro = (ImageView) findViewById(R.id.iv_play_intro);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCameraAndPreview();
        releaseMediaPlayer();
        ActivityStackManager.getInstance().popActivity(this);
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public static void startDetectFaceActivity(Context context) {
        Intent intent = new Intent(context, DetectFaceActivity.class);
        context.startActivity(intent);
    }

    /** A safe way to get an Instance of the Camera object. */
    public static Camera getFrontCamera(int id) {
        Camera c = null;
        try {
            c = Camera.open(id); // attempt to get a front camera instance
        } catch (Exception e) {
            // Front Camera is not available(int use or does not exist)
            LogUtil.e(Constants.TAG_APPLICATION, "front camera in use or not exist");
        }
        return c;
    }

    private void releaseCameraAndPreview() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private File createPhoto() throws IOException {
        String timeStamp = new SimpleDateFormat(Constants.FILE_NAME_SUFFIX_FORMAT, Locale.CHINA).format(new Date());
        String detectPhotoName = timeStamp + "_";
        LogUtil.d(Constants.TAG_APPLICATION, "detectPhotoName name is " + detectPhotoName);
        // private storage
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
//        if (storageDir != null && storageDir.listFiles().length >= Constants.UP_LIMIT_FILES) {
//            for (File file : storageDir.listFiles()) {
//                file.deleteOnExit();
//            }
//        }
        File photo = null;
        try {
            photo = File.createTempFile(
                    detectPhotoName, /* prefix */
                    ".jpg",        /* suffix */
                    storageDir     /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return photo;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (null != mFaceDetectTask) {
            switch (mFaceDetectTask.getStatus()) {
                case RUNNING:
                    return;
                case PENDING:
                    mFaceDetectTask.cancel(false);
                    break;
            }
        }
        mFaceDetectTask = new FaceDetectTask(data);
        mFaceDetectTask.execute();
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            initIntros();
            if (Constants.DETECT_USE_LIMIT >= 0) {
                mCamera.setOneShotPreviewCallback(DetectFaceActivity.this);
                Constants.DETECT_USE_LIMIT--;
            } else {
                Toast.makeText(DetectFaceActivity.this, "使用次数已打上限", Toast.LENGTH_SHORT).show();
            }

        }
    };

    private void initIntros() {
        mIvPersonPhoto.setVisibility(View.GONE);
        mTvPersonName.setVisibility(View.GONE);
        mTvPersonInfo.setVisibility(View.GONE);
        mIvPlayIntro.setVisibility(View.GONE);
    }

    private Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                LogUtil.i(Constants.TAG_APPLICATION, "auto success");
                mCamera.setOneShotPreviewCallback(DetectFaceActivity.this);
            } else {
                LogUtil.i(Constants.TAG_APPLICATION, "auto success");
//                mCamera.autoFocus(autoFocusCallback);
            }
        }
    };

    /** 自定义AsyncTask类转化文件并上传比对 */
    private class FaceDetectTask extends AsyncTask<Void, Void, File> {

        private byte[] mData;

        private FaceDetectTask(byte[] data) {
            this.mData = data;
        }

        @Override
        protected File doInBackground(Void... params) {
            Camera.Size size = mCamera.getParameters().getPreviewSize();
            int width = size.width;
            int height = size.height;
            YuvImage image = new YuvImage(mData, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream os = new ByteArrayOutputStream(mData.length);
            if (!image.compressToJpeg(new Rect(0, 0, width, height), 100, os)) {
                return null;
            }
            byte[] tmpData = os.toByteArray(); // JPEG二进制数据
            Bitmap bmp = BitmapFactory.decodeByteArray(tmpData, 0, tmpData.length);
            return handleBitmap(bmp);
        }

        @Override
        protected void onPostExecute(File file) {
            searchFace(file);
        }
    }

    private File handleBitmap(Bitmap bmp) {
        Bitmap bitmap = bmp;
        if (Build.MANUFACTURER.equals("Huawei")) {
            bitmap = extraHandle(bitmap);
        }
        File file = null;
        try {
            file = createPhoto();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (file != null) {
            FileUtil.bitmapToJpeg(bitmap, file);
        }
        return file;
    }

    private Bitmap extraHandle(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.setRotate(180);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }

    private void searchFace(File file) {
        OkHttpClient client = new OkHttpClient();
        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(Constants.PARAMETER_IMAGE_FILE, file.getName(), RequestBody.create(MediaType.parse("image/jpg"), file))
                    .addFormDataPart(Constants.PARAMETER_API_KEY, Constants.API_KEY_APPLICATION)
                    .addFormDataPart(Constants.PARAMETER_API_SECRET, Constants.API_SECRET_APPLICATION)
                    .addFormDataPart(Constants.PARAMETER_OUTER_ID, Constants.OUTER_ID_TEST)
                    .addFormDataPart(Constants.PARAMTER_RESULT_COUNT, Constants.DEFAULT_RESULT_COUNT)
                    .build();
            Request request = new Request.Builder()
                    .url(Constants.URL_FACE_SEARCH)
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
                            JSONArray facesArray = result.getJSONArray(Constants.VALUE_RETURN_RESULTS);
                            LogUtil.i(Constants.TAG_APPLICATION, "facesArray length is " + facesArray.length());
                            if (facesArray.length() != 0) {
                                final String userId = facesArray.getJSONObject(0)
                                        .getString(Constants.PARAMETER_USER_ID);
                                LogUtil.i(Constants.TAG_APPLICATION, "user id is " + userId);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //照片，姓名及简介
                                        new AsyncTask<Void, Void, Void>() {
                                            @Override
                                            protected Void doInBackground(Void... params) {
                                                showPersonPhoto(userId);
                                                return null;
                                            }
                                        }.execute();
                                        mTvPersonInfo.setText(PreferenceUtil.getInstance().getValue(userId));
                                        mTvPersonName.setText(userId);
                                        mTvPersonName.setVisibility(View.VISIBLE);
                                        mTvPersonInfo.setVisibility(View.VISIBLE);
                                        initMediaPlayer();
                                    }
                                });
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

    public void control_play(View view) {
        if (mMediaPlayer.isPlaying()) {
            mIvPlayIntro.setImageResource(R.drawable.ic_play_intro);
            mMediaPlayer.pause();
        } else {
            mIvPlayIntro.setImageResource(R.drawable.ic_pause_intro);
            if (isFirstPlay) {
                mMediaPlayer.prepareAsync();
            } else {
                mMediaPlayer.start();
            }
        }

    }

    /** an async thread operation. */
    private void showPersonPhoto(String userId) {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (storageDir != null) {
            for (File file: storageDir.listFiles()) {
                if (file.getName().split("_")[1].equals(userId)) {
                    final Bitmap photoBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIvPersonPhoto.setVisibility(View.VISIBLE);
                            mIvPersonPhoto.setImageBitmap(photoBitmap);
                        }
                    });
                    break;
                }
            }
        }
    }

    private File getPersonRecord(String userId) {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (storageDir != null) {
            for (File file: storageDir.listFiles()) {
                if (file.getName().split("_")[1].equals(userId)) {
                    return file;
                }
            }
        }
        return null;
    }

    private void showUIToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DetectFaceActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
