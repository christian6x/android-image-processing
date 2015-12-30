package com.androidimageprocessing;

import android.graphics.Bitmap;

import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;

import java.util.List;

/**
 * Created by ksikora on 12/30/15.
 */
public class BitmapMat {

    protected Bitmap bitmap;
    private List<MatOfPoint> countour;

    public BitmapMat(Bitmap bitmap)
    {
        this.bitmap = bitmap;
    }

    public void setCountour(List<MatOfPoint> matOfByte)
    {
        this.countour = matOfByte;
    }

    public List<MatOfPoint> getCountour()
    {
        return this.countour;
    }


}
