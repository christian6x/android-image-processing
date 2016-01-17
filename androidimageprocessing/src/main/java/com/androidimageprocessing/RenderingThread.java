package com.androidimageprocessing;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Krystian on 09.01.2016.
 */
public class RenderingThread extends Thread {
    private final TextureView mSurface;
    private volatile boolean mRunning = true;
    public volatile int downScaleWidth = 640;
    public volatile int downScaleHeight = 480;
    public volatile boolean mResize = false;
    public volatile boolean mNormalize = false;
    public volatile boolean mDilate = false;
    public volatile boolean mCanny = false;
    public volatile boolean mSobel = false;
    public volatile boolean mGauss = false;
    public volatile boolean mGray = false;
    public volatile double mCannyMin = 0.0;
    public volatile double mCannyMax = 0.0;
    public volatile int mCannyOpt = 0;
    public volatile MatOfPoint bContour;
    public BitmapProcessQueue mProcessList = new BitmapProcessQueue();


    public volatile Image frame;
    public byte[] byteBuffor;
    public IX WorkableImage;
    private Size bitmapSize;


    public RenderingThread(TextureView surface) {
        mSurface = surface;
    }


    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    private double GetColorDistance(int c1, int c2)
    {
        int db= Color.blue(c1)-Color.blue(c2);
        int dg=Color.green(c1)-Color.green(c2);
        int dr=Color.red(c1)-Color.red(c2);


        double d=Math.sqrt(  Math.pow(db, 2) + Math.pow(dg, 2) +Math.pow(dr, 2)  );
        return d;
    }

    private int GetNewColor(int c)
    {
        double dwhite=GetColorDistance(c, Color.WHITE);
        double dblack=GetColorDistance(c,Color.BLACK);

        if(dwhite<=dblack)
        {
            return Color.WHITE;

        }
        else
        {
            return Color.BLACK;
        }


    }

    private Bitmap GetBinaryBitmap(Bitmap bitmap_src)
    {
        Bitmap bitmap_new=bitmap_src.copy(bitmap_src.getConfig(), true);



        for(int x=0; x<bitmap_new.getWidth(); x++)
        {
            for(int y=0; y<bitmap_new.getHeight(); y++)
            {
                int color=bitmap_new.getPixel(x, y);
                color=GetNewColor(color);
                bitmap_new.setPixel(x, y, color);
            }
        }

        return bitmap_new;
    }

    public void normalize(Bitmap bitmap)
    {
        bitmap.setHasAlpha(true);
        bitmap = GetBinaryBitmap(bitmap);
        //  bitmap.eraseColor(Color.BLACK);


    }

    /**
     * Normalizacja bazująca na OpenCV
     * @param bitmap
     * @return
     */
    private Bitmap OpenCVNormalize(Bitmap bitmap)
    {
        Bitmap rBitmap = bitmap;
        Mat tmp = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1);
        // Convert
        Utils.bitmapToMat(bitmap, tmp);
        Mat gray = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1);
        // Conver the color
        Imgproc.cvtColor(tmp, gray, Imgproc.COLOR_RGB2GRAY);
        // Convert back to bitmap
        Mat destination = new Mat(gray.rows(),gray.cols(),gray.type());
//            Imgproc.threshold(gray,destination,255,255,Imgproc.THRESH_BINARY_INV);
        Imgproc.adaptiveThreshold(gray, destination, 255,
                Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 4);
        Utils.matToBitmap(destination, rBitmap);
        return rBitmap;
    }

    /**
     * Obrót
     * @param in
     * @param angle
     * @return
     */
    public Bitmap rotate(Bitmap in, int angle) {
        Matrix mat = new Matrix();
        mat.postRotate(angle);
        return Bitmap.createBitmap(in, 0, 0, in.getWidth(), in.getHeight(), mat, true);
    }

    /**
     * OpenCV Gauss
     * @param bitmap
     * @return
     */
    private Bitmap OpenCVGauss(Bitmap bitmap)
    {
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mat);
        Mat mat2 = mat;

        Imgproc.GaussianBlur(mat, mat,new org.opencv.core.Size(5,5), 0);
        Utils.matToBitmap(mat2, bitmap);
        return bitmap;
    }

    /**
     * OpenCV Gray
     * TO-DO
     * @param bitmap
     * @return
     */
    private Bitmap OpenCVGray(Bitmap bitmap) {
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mat);
        Mat mat2 = mat;

        Utils.matToBitmap(mat2, bitmap);
        return bitmap;
    }

    /*
        OpenCV SOBEL
     */
    private Bitmap OpenCVSobel(Bitmap bitmap)
    {
        Mat mSepiaKernel = new Mat(4, 4, CvType.CV_32F);
        mSepiaKernel.put(0, 0, /* R */0.189f, 0.769f, 0.393f, 0f);
        mSepiaKernel.put(1, 0, /* G */0.168f, 0.686f, 0.349f, 0f);
        mSepiaKernel.put(2, 0, /* B */0.131f, 0.534f, 0.272f, 0f);
        mSepiaKernel.put(3, 0, /* A */0.000f, 0.000f, 0.000f, 1f);
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mat);
        Mat mat2 = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Bitmap rBitmap = Bitmap.createBitmap(mat2.cols(), mat2.rows(), Bitmap.Config.ARGB_8888);
        String s = "";

        //  Core.transform(mat, mat, mSepiaKernel);
        Imgproc.Sobel(mat, mat2, CvType.CV_8UC1, 1, 1);
//
//                for (int k = 1; k < mat2.rows(); k++) {
//                    for (int j = 1; j < mat2.cols(); j++) {
//                        double[] res = mat2.get(k, j);
//                        if (res[3] == 255.0) {
//                            for (int i = 0; i < res.length; i++) {
//                                s += i;
//                                s += " : ";
//                                s += res[i];
//                                s += " ";
//
//                            }
//                            Log.d("LLLL", s);
//                            s = "";
//                        }
//                    }
//                }
        // Utils.

        Utils.matToBitmap(mat2, rBitmap);

        Bitmap bmOverlay = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);


        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawPaint(paint);
        canvas.drawBitmap(rBitmap, new Matrix(), null);
        // canvas.drawBitmap(rBitmap, 0, 0, null);
        //return bmOverlay;

        return bmOverlay;
    }


    /**
     *
     * @param bitmap
     * @return
     */
    private Bitmap OpenCVFindCountours(Bitmap bitmap)
    {
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Mat mat2 = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        //   Mat mat2 = mat;
        Utils.bitmapToMat(bitmap, mat);
        // Conver the color
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(mat, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        Bitmap bmOverlay = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawPaint(paint);
        Utils.bitmapToMat(bmOverlay, mat2);


        for(int i=0; i< contours.size();i++){
            //System.out.println(Imgproc.contourArea(contours.get(i)));
            if (Imgproc.contourArea(contours.get(i)) > 50 ){
                Rect rect = Imgproc.boundingRect(contours.get(i));
                //System.out.println(rect.height);
                // if (rect.height > 10){
                //System.out.println(rect.x +","+rect.y+","+rect.height+","+rect.width);
                Log.i("CONT", "Got contour : " + rect.toString());
                //Imgproc.drawContours();
                Imgproc.drawContours(mat2,contours,i,new Scalar(0,255,255));
                //Imgproc.rectangle(mat2, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(255,255,255));
                // }
            }
        }


        Utils.matToBitmap(mat2, bmOverlay);


        //canvas.drawBitmap(rBitmap, new Matrix(), null);

        return bmOverlay;
    }

    /*
        Wykrywanie kształtów
     */
    private Bitmap OpenCVDetectSquares(Bitmap bitmap)
    {
        int erosion_size = 3;
        int dilation_size = 3;
        Bitmap rBitmap = bitmap;

        try {
            Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
            Utils.bitmapToMat(bitmap, mat);
            Mat mat2 = mat;
            if (mCanny) {


                Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new  org.opencv.core.Size(2*erosion_size + 1, 2*erosion_size+1));
                // Imgproc.erode(mat, mat2, element);
                Imgproc.cvtColor(mat,mat,Imgproc.COLOR_BGR2GRAY);

                Imgproc.Canny(mat, mat2, mCannyMin, mCannyMax, mCannyOpt, true);

                Log.i("CANNY", "SUCCESS" + "mCannyMin : " + mCannyMin + " mCannyMax : " + mCannyMax + " mCannyOpt : " + mCannyOpt);
            }
            //Imgproc.Laplacian(mat,mat,1);

            if (mDilate) {
                Mat element1 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new  org.opencv.core.Size(2*dilation_size + 1, 2*dilation_size+1));
                Imgproc.dilate(mat, mat2, element1);

                //Imgproc.dilate(mat, mat, new Mat());
            }

            Utils.matToBitmap(mat2, rBitmap);
        }
        catch (Exception e)
        {
            Log.e("CANNY", e.getLocalizedMessage() + " : " + e.getMessage() + "mCannyMin : " + mCannyMin + " mCannyMax : " + mCannyMax + " mCannyOpt : " + mCannyOpt);
            e.printStackTrace();

        }
        return rBitmap;
    }

    public Bitmap getBitmapImageFromYUV(byte[] data, int width, int height) {
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new android.graphics.Rect(0, 0, width, height), 50, baos);
        byte[] jdata = baos.toByteArray();
        //  BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
        //  bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
        return bmp;
    }

    private byte[] convertYUV420ToN21(Image imgYUV420) {
        byte[] rez = new byte[0];

        ByteBuffer buffer0 = imgYUV420.getPlanes()[0].getBuffer();
        ByteBuffer buffer2 = imgYUV420.getPlanes()[2].getBuffer();
        int buffer0_size = buffer0.remaining();
        int buffer2_size = buffer2.remaining();
        rez = new byte[buffer0_size + buffer2_size];

        buffer0.get(rez, 0, buffer0_size);
        buffer2.get(rez, buffer0_size, buffer2_size);

        return rez;
    }


    private byte[] convertN21ToJpeg(byte[] bytesN21, int w, int h) {
        byte[] rez = new byte[0];

        YuvImage yuv_image = new YuvImage(bytesN21, ImageFormat.NV21, w, h, null);
        android.graphics.Rect rect = new android.graphics.Rect(0, 0, w, h);
        ByteArrayOutputStream output_stream = new ByteArrayOutputStream();
        yuv_image.compressToJpeg(rect, 100, output_stream);
        rez = output_stream.toByteArray();

        return rez;
    }

    private BitmapMat latestMatOfPoint;

    public synchronized BitmapMat getBiggestCountour()
    {
        return this.latestMatOfPoint;
    }

    @Override
    public void run() {
        float x = 0.0f;
        float y = 0.0f;
        float speedX = 5.0f;
        float speedY = 3.0f;

        Paint pText = new Paint();
        pText.setColor(Color.GREEN);
        pText.setTextSize(16);

        Paint paint = new Paint();
        paint.setColor(0xff00ff00);
        //paint.setColor(Color.BLACK);
        long prevTime = 0;
        long currentTime = 0;
        long frameTime = 0;


        while (mRunning && !Thread.interrupted()) {
            try {


                if (WorkableImage != null && WorkableImage.height != 0 && WorkableImage.width != 0 ) {


                    currentTime = System.currentTimeMillis();
                    if(prevTime != 0) {
                        frameTime = currentTime - prevTime;
                    }
                    prevTime = currentTime;

                    Bitmap bitmap = WorkableImage.mBitmap;


                    //bitmap = rotate(bitmap, 90);
                    if (mResize) {
                        bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSize.getWidth(), bitmapSize.getHeight(), false);
                    }


                    //Log.i("QWERTY", String.valueOf(mProcessList.size()));
                    try {
                        BitmapMat bitMat = new BitmapMat(bitmap);
                        bitMat= mProcessList.process(bitMat);
                        this.latestMatOfPoint = bitMat;
                        bitmap = bitMat.getBitmap();
                        //    Log.i("BVC", "BITMAT SIZE" + bitMat.getCountour().cols());
                    }
                    catch (Exception e)
                    {
                        Log.e("QWERTY","ERROR " + e.getLocalizedMessage());
                    }
//                        if (mNormalize)
//                            bitmap = OpenCVNormalize(bitmap);
//
//                        if (mGray)
//                            bitmap = toGrayscale(bitmap);
//
//
//                        //bitmap = GetBinaryBitmap(bitmap);
//
//
//                        bitmap = OpenCVDetectSquares(bitmap);
//
//                        if (mGauss)
//                            bitmap = OpenCVGauss(bitmap);
//
//                        if (mSobel)
//                            bitmap = OpenCVSobel(bitmap);


                    //if (mC)

                    // bitmap = OpenCVFindCountours(bitmap);


                    //          Imgproc.


//
//
//
//
//                        //  Bitmap bbb = BitmapFactory.;
//                        //  Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
//                        Log.i("RUN", "IAM DIFFERENT" + bitmap.getHeight() + "Density : " + bitmap.getDensity());
//// Do note that Highgui has been replaced by Imgcodecs for OpenCV 3.0 and above
//                        //  Mat mat = Imgcodecs.imdecode(buf, Imgcodecs.CV_LOAD_IMAGE_COLOR);
//                        //   mat.release();
//                        //  buf.release();
//                        //   Mat.put(frameArray)
//                        normalize(bitmap);
//                        MatOfByte inputframe = new MatOfByte(WorkableImage.byteBuffer);
//                        // Mat mat = new Mat();
//                        //   Mat.
//                        // mat.put()
//                        // mat.put(inputframe);
//                        Mat mat = Imgcodecs.imdecode(inputframe, Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
//                        Mat mat2 = mat;
//                    //    Imgproc.Sobel(mat, mat2, CvType.CV_8U, 1, 1);
//
//                        //Bitmap bmp = null;
//
//                        try {
//                            //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_RGB2BGRA);
//                            // Imgproc.cv
//                            //  Imgproc.cvtColor(mat2, mat, Imgproc.CO, 4);
//                            //   bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
//                         //   Utils.matToBitmap(mat2, bitmap);
//                        }
//                        catch (CvException e){Log.d("Exception",e.getMessage());}

                    Canvas canvas = mSurface.lockCanvas(null);
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);


                    try {

                        //   Bitmap alphaBitmap = toGrayscale(bitmap);

                        //    bitmap = bmp;
                        if (mResize) {
                            bitmap = Bitmap.createScaledBitmap(bitmap, mSurface.getWidth(), mSurface.getHeight(), false);
                        }
                        bitmap.prepareToDraw();

                        //int allocationMap = bitmap.getAllocationByteCount();
                        //Log.i("RUN", "IAM DIFFERENT ALLOCATION MAP" + allocationMap);
                        //Log.i("RUN", "BITMAP" + bitmap.getHeight() + " : " + bitmap.getWidth());
                        //int canvasDensity = canvas.getDensity();
                        //Log.i("RUN", "IAM DIFFERENT CANVAS DENSITY MAP" + canvasDensity);
                        //  canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint);

//                            Bitmap img = BitmapFactory.decodeResource(this.getResources(), drawable.testimage);
//                            Paint paint2 = new Paint();
//
//                            ColorMatrix cm = new ColorMatrix();
//                            float a = 77f;
//                            float b = 151f;
//                            float c = 28f;
//                            float t = 120 * -256f;
//                            cm.set(new float[] { a, b, c, 0, t, a, b, c, 0, t, a, b, c, 0, t, 0, 0, 0, 1, 0 });
//                            paint.setColorFilter(new ColorMatrixColorFilter(cm));
//                            canvas.drawPaint(paint);



                        canvas.drawBitmap(bitmap, 0, 0, paint);


                        // canvas.drawText("TPS: ", 0, 0, pText);
                        //        Log.i("BITMAP","TPS : " + frameTime);
                        // canvas.drawRect(x, y, x + 20.0f, y + 20.0f, paint);


                        // canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR);
                        // canvas.d
                        //    canvas.drawRect(x, y, x + 220.0f, y + 220.0f, paint);
                    } finally {
                        mSurface.unlockCanvasAndPost(canvas);
                    }




                }
                else
                {
                    sleep(1000);
                }
            }
            catch (Exception e)
            {
                Log.i("RUN",e.toString());
            }


//                if (x + 20.0f + speedX >= mSurface.getWidth() || x + speedX <= 0.0f) {
//                    speedX = -speedX;
//                }
//                if (y + 20.0f + speedY >= mSurface.getHeight() || y + speedY <= 0.0f) {
//                    speedY = -speedY;
//                }
//
//                x += speedX;
//                y += speedY;

            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                // Interrupted
            }

        }
    }




    void stopRendering() {
        interrupt();
        mRunning = false;
    }

    public BitmapMat getLatestMat() {
        return latestMatOfPoint;
    }

    public void setBitmapSize(Size bitmapSize) {
        this.bitmapSize = bitmapSize;
    }
}
