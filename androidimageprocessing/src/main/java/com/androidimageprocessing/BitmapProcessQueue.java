package com.androidimageprocessing;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by ksikora on 12/29/15.
 */
public class BitmapProcessQueue extends ArrayList {

    private ArrayList<BitmapProcessInterface> mFilters;

    public BitmapProcessQueue()
    {
         this.mFilters = new ArrayList<BitmapProcessInterface>();
    }


    public BitmapMat process(BitmapMat bitmapMat)
    {
        Iterator<BitmapProcessInterface> it = this.iterator();
        Bitmap cBitmap;
        while(it.hasNext())
        {
            BitmapProcessInterface next  = it.next();
            cBitmap = next.process(bitmapMat.getBitmap());
            bitmapMat.setBitmap(cBitmap);
            bitmapMat = next.process(bitmapMat);
        }

        return bitmapMat;
    }

}
