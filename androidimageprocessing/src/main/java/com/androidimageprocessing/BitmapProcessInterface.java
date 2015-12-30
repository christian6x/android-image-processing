package com.androidimageprocessing;

import android.graphics.Bitmap;

/**
 * Created by ksikora on 12/29/15.
 */
interface BitmapProcessInterface {

    Bitmap process(Bitmap bitmap);
    BitmapMat process(BitmapMat bitmapMat);

}
