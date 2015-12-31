package com.androidimageprocessing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ksikora on 12/29/15.
 */
public final class BitmapProcess {


    public static Bitmap toGrayscale(Bitmap bmpOriginal)
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

    private static double GetColorDistance(int c1, int c2)
    {
        int db= Color.blue(c1)-Color.blue(c2);
        int dg=Color.green(c1)-Color.green(c2);
        int dr=Color.red(c1)-Color.red(c2);


        double d=Math.sqrt(  Math.pow(db, 2) + Math.pow(dg, 2) +Math.pow(dr, 2)  );
        return d;
    }

    private static int GetNewColor(int c)
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

    private static Bitmap GetBinaryBitmap(Bitmap bitmap_src)
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


    public static void normalize(Bitmap bitmap)
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
    public static Bitmap OpenCVNormalize(Bitmap bitmap)
    {
        Bitmap rBitmap = bitmap;
        Mat tmp = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1);
        // Convert
        Utils.bitmapToMat(bitmap, tmp);
        Mat gray = new Mat(bitmap.getWidth(), bitmap.getHeight(),     CvType.CV_8UC1);
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
    public static Bitmap rotate(Bitmap in, int angle) {
        Matrix mat = new Matrix();
        mat.postRotate(angle);
        return Bitmap.createBitmap(in, 0, 0, in.getWidth(), in.getHeight(), mat, true);
    }

    /**
     * OpenCV Gauss
     * @param bitmap
     * @return
     */
    public static Bitmap OpenCVGauss(Bitmap bitmap)
    {
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mat);
        Mat mat2 = mat;

        Imgproc.GaussianBlur(mat, mat, new org.opencv.core.Size(5, 5), 0);
        Utils.matToBitmap(mat2, bitmap);
        return bitmap;
    }

    /**
     * OpenCV Gray
     * TO-DO
     * @param bitmap
     * @return
     */
    public static Bitmap OpenCVGray(Bitmap bitmap) {
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mat);
        Mat mat2 = mat;
        Imgproc.cvtColor(mat, mat2, Imgproc.COLOR_RGB2GRAY);
        Utils.matToBitmap(mat2, bitmap);
        return bitmap;
    }

    /*
        OpenCV SOBEL
     */
    public static Bitmap OpenCVSobel(Bitmap bitmap)
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
    public static Bitmap OpenCVFindCountours(Bitmap bitmap)
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

        /*
            Wybieranie największego kształtu
         */
        HashMap<MatOfPoint, Double> shapeMap = new HashMap<MatOfPoint, Double>();
        MatOfPoint bContour = new MatOfPoint();
        double maxSize = 0;
        int bCountourId = 0;
        for(int i=0; i< contours.size();i++){

            double area = Imgproc.contourArea(contours.get(i));

            shapeMap.put(contours.get(i), area);
            if(maxSize < area)
            {
                maxSize = area;
                bContour = contours.get(i);
                bCountourId = i;
            }

        }

        if(!bContour.empty())
        {
//            Imgproc.drawContours(mat2,bContour,1,new Scalar(0,255,255));
            Imgproc.drawContours(mat2,contours,bCountourId,new Scalar(0,255,255));
//            Rect rect = Imgproc.boundingRect(contours.get(i));
        }
        shapeMap.values().toArray();


//        for(int i=0; i< contours.size();i++){
//            //System.out.println(Imgproc.contourArea(contours.get(i)));
//            if (Imgproc.contourArea(contours.get(i)) > 50 ){
//                Rect rect = Imgproc.boundingRect(contours.get(i));
//                //System.out.println(rect.height);
//                // if (rect.height > 10){
//                //System.out.println(rect.x +","+rect.y+","+rect.height+","+rect.width);
//                Log.i("CONT", "Got contour : " + rect.toString());
//                //Imgproc.drawContours();
//
//                //Imgproc.drawContours(mat2,contours,i,new Scalar(0,255,255));
//                //Imgproc.rectangle(mat2, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(255,255,255));
//                // }
//            }
//        }
//

        Utils.matToBitmap(mat2, bmOverlay);


        //canvas.drawBitmap(rBitmap, new Matrix(), null);

        return bmOverlay;
    }


    public static BitmapMat OpenCVFindCountours(BitmapMat bitmapMat)
    {
        Bitmap bitmap = bitmapMat.getBitmap();
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

        /*
            Wybieranie największego kształtu
         */
        HashMap<MatOfPoint, Double> shapeMap = new HashMap<MatOfPoint, Double>();
        MatOfPoint bContour = new MatOfPoint();
        double maxSize = 0;
        int bCountourId = 0;
        for(int i=0; i< contours.size();i++){

            double area = Imgproc.contourArea(contours.get(i));

            shapeMap.put(contours.get(i), area);
            if(maxSize < area)
            {
                maxSize = area;
                bContour = contours.get(i);
                bCountourId = i;
            }

        }
        MatOfPoint bCoutnour = new MatOfPoint();
        if(!bContour.empty())
        {
//            Imgproc.drawContours(mat2,bContour,1,new Scalar(0,255,255));
            Imgproc.drawContours(mat2,contours,bCountourId,new Scalar(0,255,255));
            bCoutnour = contours.get(bCountourId);
//            Rect rect = Imgproc.boundingRect(contours.get(i));
        }
        shapeMap.values().toArray();


//        for(int i=0; i< contours.size();i++){
//            //System.out.println(Imgproc.contourArea(contours.get(i)));
//            if (Imgproc.contourArea(contours.get(i)) > 50 ){
//                Rect rect = Imgproc.boundingRect(contours.get(i));
//                //System.out.println(rect.height);
//                // if (rect.height > 10){
//                //System.out.println(rect.x +","+rect.y+","+rect.height+","+rect.width);
//                Log.i("CONT", "Got contour : " + rect.toString());
//                //Imgproc.drawContours();
//
//                //Imgproc.drawContours(mat2,contours,i,new Scalar(0,255,255));
//                //Imgproc.rectangle(mat2, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(255,255,255));
//                // }
//            }
//        }
//

        Utils.matToBitmap(mat2, bmOverlay);


        bitmapMat.setBitmap(bmOverlay);
        bitmapMat.setCountour(bCoutnour);
        //canvas.drawBitmap(rBitmap, new Matrix(), null);

        return bitmapMat;
    }

    public static Bitmap OpenCVCanny(Bitmap bitmap, int mCannyMin, int mCannyMax, int mCannyOpt)
    {
        Bitmap rBitmap = bitmap;
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mat);
        Mat mat2 = mat;
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(mat, mat2, mCannyMin, mCannyMax, mCannyOpt, true);

        Utils.matToBitmap(mat2, rBitmap);
        return rBitmap;
    }


    public static Bitmap OpenCVDilate(Bitmap bitmap) {
        int erosion_size = 3;
        int dilation_size = 3;
        Bitmap rBitmap = bitmap;
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mat);
        Mat mat2 = mat;
        Mat element1 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new  org.opencv.core.Size(2*dilation_size + 1, 2*dilation_size+1));
        Imgproc.dilate(mat, mat2, element1);
        Utils.matToBitmap(mat2, rBitmap);
        return rBitmap;
    }



    public static Bitmap OpenCVMedian(Bitmap bitmap) {
        Bitmap rBitmap = bitmap;
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mat);
        Mat mat2 = mat;
        Imgproc.medianBlur(mat, mat2, 3);
        Utils.matToBitmap(mat2, rBitmap);
        return rBitmap;
    }



    public static Bitmap OpenCVThreshold(Bitmap bitmap, int threshold) {
        Bitmap rBitmap = bitmap;
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mat);
        Mat mat2 = mat;
        Imgproc.threshold(mat, mat2, threshold, 255.0, Imgproc.THRESH_BINARY);
        Utils.matToBitmap(mat2, rBitmap);
        return rBitmap;
    }


    public static BitmapMat OpenCVThreshold(BitmapMat bitmapMat, int threshold) {
        Bitmap bitmap = bitmapMat.getBitmap();
        Bitmap rBitmap = bitmap;
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mat);
        Mat mat2 = mat;
        Imgproc.threshold(mat, mat2, threshold, 255.0, Imgproc.THRESH_BINARY);
        Utils.matToBitmap(mat2, rBitmap);
        bitmapMat.setBitmap(rBitmap);
        return bitmapMat;
    }



    public static Bitmap OpenCVHoughLines(Bitmap bitmap, int threshold, int mLineLength, int mLineGap)
    {
        Mat mRgba = new Mat();
        Bitmap rBitmap = bitmap;
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mat);
        // Mat mat2 = mat;
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
//        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        Mat lines = new Mat();

        Imgproc.HoughLinesP(mat, lines, 1.0, Math.PI / 180, threshold, mLineLength, mLineGap);
        //Imgproc.Ho
        // Imgproc.HoughLinesP(mat, lines, 1.0, Math.PI / 180, threshold);

//        Bitmap bmOverlay = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
//        Paint paint = new Paint();
//        paint.setColor(Color.RED);
//        Canvas canvas = new Canvas(bmOverlay);
//        canvas.drawPaint(paint);
//        Utils.bitmapToMat(bmOverlay, mRgba);
        Utils.bitmapToMat(bitmap, mRgba);

        //Log.i("HOUGH", "LINES SIZE : " + lines.rows());
        for (int x = 0; x < lines.rows(); x++)
        {
            double[] vec = lines.get(x, 0);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);
            double[] val = lines.get(x, 0);
            Imgproc.line(mRgba,  new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(0, 255, 0), 2);

        }





        Utils.matToBitmap(mRgba, rBitmap);
        return rBitmap;
    }




}
