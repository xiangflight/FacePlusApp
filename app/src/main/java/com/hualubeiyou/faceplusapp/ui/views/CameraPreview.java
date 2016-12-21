package com.hualubeiyou.faceplusapp.ui.views;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.hualubeiyou.faceplusapp.utils.Constants;
import com.hualubeiyou.faceplusapp.utils.LogUtil;

import java.io.IOException;
import java.util.List;

/**
 * Created by flight on 2016/12/20
 * use to customize camera preview.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback{

    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void setCamera(Camera camera) {
        this.mCamera = camera;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            LogUtil.d(Constants.TAG_APPLICATION, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview and make any resize, rotate or
        // reformatting changes here

        //start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            LogUtil.d(Constants.TAG_APPLICATION, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Empty. Take care of releasing the Camera preview in your activity.
    }

}
