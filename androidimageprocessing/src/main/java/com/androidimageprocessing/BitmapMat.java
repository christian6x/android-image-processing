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

}
