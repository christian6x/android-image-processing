package com.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;

import com.androidimageprocessing.BitmapMat;
import com.androidimageprocessing.BitmapProcess;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import android.util.Log;

import static com.androidimageprocessing.BitmapProcess.rotate;

/**
 * Created by Krystian on 09.01.2016.
 */
public class ImageCropper implements Runnable {

    private final static String TAG = "IMAGE_CROPPER_LOG";

    /**
     * The JPEG image
     */
    private final Image mImage;
    /**
     * The file we save the image into.
     */
    private final File mFile;

    private final BitmapMat mBitmapMat;

    public ImageCropper(Image image, File file, BitmapMat latestMat) {
        mImage = image;
        mFile = file;
        mBitmapMat = latestMat;
    }



    @Override
    public void run() {
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        Bitmap pBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

    //    pBitmap = rotate(pBitmap, 90);

    //    Bitmap mBitmap = mBitmapMat.getBitmap();
    //    Bitmap bitmap = Bitmap.createScaledBitmap(pBitmap, mImage.getWidth(), mImage.getHeight(), true);
        Bitmap bitmap = BitmapProcess.WriteCountoursOnBitmap(pBitmap, mBitmapMat);
        mBitmapMat.setBitmap(bitmap);


        // Szczegóły o przetworzonym zdjęciu
        String description = BitmapMat.describeBitmapMat(mBitmapMat);
        description += "IMAGE   H    : " +  mImage.getHeight() + "\n";
        description += "IMAGE   W    : " +  mImage.getWidth() + "\n";
        Log.i(TAG,description);

        FileOutputStream output = null;
        try {
            output = new FileOutputStream(mFile);
            // pBitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            output.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mImage.close();
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}