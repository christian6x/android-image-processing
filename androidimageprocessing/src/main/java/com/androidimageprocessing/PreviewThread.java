package com.androidimageprocessing;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.Surface;

/**
 * Created by Krystian on 09.01.2016.
 */
public class PreviewThread extends Thread
{
    private final Surface mSurface;
    private boolean mRunning;
    public IX WorkableImage;

    public PreviewThread(Surface surfaceToDraw)
    {

        mSurface = surfaceToDraw;
        //mSurface.setWillNotDraw(false);
        //mSurface.set
    }

    /*
        Wątek rysujący podgląd
     */
    @Override
    public void run() {
        float x = 0.0f;
        float y = 0.0f;
        float speedX = 5.0f;
        float speedY = 3.0f;

        Paint paint = new Paint();
        paint.setColor(0xff00ff00);
        while (mRunning && !Thread.interrupted()) {
            try
            {
                //Bitmap bitmap = BitmapFactory.decodeByteArray(WorkableImage.byteBuffer,0,WorkableImage.byteBuffer.length);
                Canvas canvas = mSurface.lockCanvas(null);

                try {
                    // bitmap.prepareToDraw();
                    //int allocationMap = bitmap.getAllocationByteCount();
                    //Log.i("RUN", "IAM DIFFERENT ALLOCATION MAP" + allocationMap);
                    //int canvasDensity = canvas.getDensity();
                    //Log.i("RUN", "IAM DIFFERENT CANVAS DENSITY MAP" + canvasDensity);
                    // canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint);
                    //  canvas.drawRect(x, y, x + 220.0f, y + 220.0f, paint);
                }
                catch(Exception e)
                {
                    e.printStackTrace();

                } finally {
                    mSurface.unlockCanvasAndPost(canvas);
                }
            }
            catch (Exception e)
            {
                Log.e("RENDERGINTHREAD", e.getLocalizedMessage());
            }
        }
    }

    void stopRendering() {
        interrupt();
        mRunning = false;
    }
}


