package com.androidimageprocessing;

import android.graphics.Bitmap;

import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ksikora on 12/30/15.
 */
public class BitmapMat {

    private Bitmap bitmap;
    private MatOfPoint countour = new MatOfPoint();

    public BitmapMat(Bitmap bitmap)
    {
        this.bitmap = bitmap;
    }

    public void setCountour(MatOfPoint matOfByte)
    {
        this.countour = matOfByte;
    }

    public MatOfPoint getCountour()
    {
        return this.countour;
    }

    public Bitmap getBitmap()
    {
        return this.bitmap;
    }

    public void setBitmap(Bitmap bitmap)
    {
        this.bitmap = bitmap;
    }

    public static String describeBitmapMat(BitmapMat bitmapMat)
    {
        String description = "";
        description += "BITMAP  H    : " + bitmapMat.getBitmap().getHeight() + "\n";
        description += "BITMAP  W    : " + bitmapMat.getBitmap().getWidth() + "\n";
        description += "MATCONT COLS : " + bitmapMat.getCountour().cols() + "\n";
        description += "MATCONT ROWS : " + bitmapMat.getCountour().rows() + "\n";
        description += "MATCONT H    : " + bitmapMat.getCountour().height() + "\n";
        description += "MATCONT W    : " + bitmapMat.getCountour().width() + "\n";
        return description;
    }
}
