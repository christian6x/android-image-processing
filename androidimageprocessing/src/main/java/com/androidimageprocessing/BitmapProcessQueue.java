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
        Bitmap cBitmap;
        while(it.hasNext())
        {
            BitmapProcessInterface next  = it.next();

            // curr = null;

            cBitmap = next.process(bitmapMat.getBitmap());
           // Log.i("BVC", "FIRST PROCESS BITMAP WIDTH : " + String.valueOf(cBitmap.getWidth()));
            bitmapMat.setBitmap(cBitmap);
        //    cBitmap.recycle();
            bitmapMat = next.process(bitmapMat);

           // Log.i("BVC", "SECOND PROCESS BITMAP WIDTH : " + String.valueOf(bitmapMat.getBitmap().getWidth()));
//
//
//            if(curr == null)
//            {
//                //Log.i("XCV","PROCESSING ONLY BITMAP");
//                cBitmap = next.process(bitmapMat.bitmap);
//                bitmapMat.bitmap = cBitmap;
//            }
//            else
//            {
//                Log.i("XCV","PROCESSING ALL");
//                bitmapMat = curr;
//            }
//           // Log.i("YYY","PROCESSING");
        }

        return bitmapMat;
    }

}
