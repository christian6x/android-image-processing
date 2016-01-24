package com.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;

import com.androidimageprocessing.BitmapMat;
import com.androidimageprocessing.BitmapProcess;

import java.io.ByteArrayOutputStream;
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
    private Matrix mTransformMatrix;

    public ImageCropper(Image image, File file, BitmapMat latestMat) {
        mImage = image;
        mFile = file;
        mBitmapMat = latestMat;
    }

    public ImageCropper(Image image, File file, BitmapMat latestMat, Matrix rotation) {
        mImage = image;
        mFile = file;
        mBitmapMat = latestMat;
        mTransformMatrix = rotation;
    }


    @Override
    public void run() {
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);


        // Szczegóły o przetworzonym zdjęciu
        String description = BitmapMat.describeBitmapMat(mBitmapMat);
        description += "IMAGE   H    : " +  mImage.getHeight() + "\n";
        description += "IMAGE   W    : " +  mImage.getWidth() + "\n";
        mImage.close();
        Log.i("CROPPER_TIME","Decoding bitmap");

        Bitmap pBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        //pBitmap = Bitmap.createBitmap(pBitmap, 0, 0, pBitmap.getWidth(), pBitmap.getHeight());
        /*
            Z uwzględnieniem transformacji
         */
        // pBitmap = Bitmap.createBitmap(pBitmap, 0, 0, pBitmap.getWidth(), pBitmap.getHeight(), mTransformMatrix, true);

        //    pBitmap = rotate(pBitmap, 90);

    //    Bitmap mBitmap = mBitmapMat.getBitmap();
    //    Bitmap bitmap = Bitmap.createScaledBitmap(pBitmap, mImage.getWidth(), mImage.getHeight(), true);
        Log.i("CROPPER_TIME","Processing contours bitmap");
        Bitmap original = mBitmapMat.getBitmap();
        Bitmap bitmap = BitmapProcess.WriteCountoursOnBitmap(pBitmap, mBitmapMat);
        mBitmapMat.setBitmap(bitmap);

        Log.i(TAG,description);

        FileOutputStream output = null;
        FileOutputStream outputBitmap = null;
        try {
            Log.i("CROPPER_TIME","Writing files");
            output = new FileOutputStream(mFile+".jpg");

            outputBitmap = new FileOutputStream(mFile+"org"+".jpg");
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        //    byte[] byteArray = stream.toByteArray();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputBitmap);
           // output.write(bytes);
        //    outputBitmap.write(bytes);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != output) {
                try {
                    output.close();
                    outputBitmap.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.i("CROPPER_TIME","Completed");
    }

}