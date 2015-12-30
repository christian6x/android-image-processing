package com.androidimageprocessing;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
        BitmapMat curr;
        while(it.hasNext())
        {
            BitmapProcessInterface next  = it.next();
            Bitmap cBitmap;
            curr = null;
            curr = next.process(bitmapMat);
            if(curr == null)
            {
                Log.i("XCV","PROCESSING ONLY BITMAP");
                cBitmap = next.process(bitmapMat.bitmap);
                bitmapMat.bitmap = cBitmap;
            }
            else
            {
                Log.i("XCV","PROCESSING ALL");
                bitmapMat = curr;
            }
            Log.i("YYY","PROCESSING");
        }

        return bitmapMat;
    }

}
