package com.androidimageprocessing;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by ksikora on 12/29/15.
 */
public class BitmapProcessQueue extends ArrayList {

    private boolean mDebug = false;
    private ArrayList<BitmapProcessInterface> mFilters;

    public BitmapProcessQueue()
    {
         this.mFilters = new ArrayList<BitmapProcessInterface>();
    }

    public BitmapProcessQueue(boolean debug)
    {
        this.mDebug = debug;
        this.mFilters = new ArrayList<BitmapProcessInterface>();
    }


    public BitmapMat process(BitmapMat bitmapMat)
    {
        Iterator<BitmapProcessInterface> it = this.iterator();
        Bitmap cBitmap;
        while(it.hasNext())
        {
            if(mDebug)
                Log.d("BitmapProcessQueue","Processing ... ");
            BitmapProcessInterface next  = it.next();
            cBitmap = next.process(bitmapMat.getBitmap());
            bitmapMat.setBitmap(cBitmap);
            bitmapMat = next.process(bitmapMat);
        }

        return bitmapMat;
    }

    public BitmapMat process(BitmapMat bitmapMat, boolean debug)
    {
        this.mDebug = debug;
        return process(bitmapMat);
    }

}
