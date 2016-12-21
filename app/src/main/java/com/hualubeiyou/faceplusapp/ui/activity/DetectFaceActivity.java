package com.hualubeiyou.faceplusapp.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
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
import com.hualubeiyou.faceplusapp.utils.LogUtil;
import com.hualubeiyou.faceplusapp.utils.PreferenceUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
        initMediaPlayer();
        initView();
    }

    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        File introFile = new File("/sdcard/Download/小幸运.mp3");
        Uri introUri = Uri.fromFile(introFile);
        isFirstPlay = true;
        try {
            mMediaPlayer.setDataSource(this, introUri);
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();
                    isFirstPlay = false;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private File createPhoto() {
        String timeStamp = new SimpleDateFormat(Constants.FILE_NAME_SUFFIX_FORMAT, Locale.CHINA).format(new Date());
        String detectPhotoName = timeStamp + "_" + "_";
        LogUtil.d(Constants.TAG_APPLICATION, "detectPhotoName name is " + detectPhotoName);
        // private storage
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && storageDir.listFiles().length >= Constants.UP_LIMIT_FILES) {
            for (File file : storageDir.listFiles()) {
                file.deleteOnExit();
            }
        }
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
            mCamera.setOneShotPreviewCallback(DetectFaceActivity.this);
        }
    };

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
//            doSomethingNeeded(bmp);
            File file = createPhoto();
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(file);
                fos.write(tmpData);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return file;
        }

        @Override
        protected void onPostExecute(File file) {
            searchFace(file);
        }
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
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null) {
            for (File file: storageDir.listFiles()) {
                if (file.getName().split("_")[1].equals(userId)) {
                    final Bitmap photoBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mIvPersonPhoto.setImageBitmap(photoBitmap);
                        }
                    });
                    break;
                }
            }
        }
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
