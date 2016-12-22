package com.hualubeiyou.faceplusapp.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by flight on 2016/12/18
 */

public class FileUtil {

    /**
     * 适配api19以上,根据uri获取图片的绝对路径
     */
    public static String getRealPathFromUri_AboveApi19(Context context, Uri uri) {
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
     * Bitmap存入本地文件
     * @param bitmap bitmap
     * @param file file
     */
    public static void bitmapToJpeg(Bitmap bitmap, File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos); // 压缩20%
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
