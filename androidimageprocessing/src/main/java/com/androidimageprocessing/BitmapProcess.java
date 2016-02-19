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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

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


    public static BitmapMat OpenCVFindCountours(BitmapMat bitmapMat, boolean debug)
    {
        if(debug)
            Log.d("BitmapProcess","OpenCVFindCountours Begin");
        Bitmap bitmap = bitmapMat.getBitmap();
        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Mat mat2 = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        //   Mat mat2 = mat;
        Utils.bitmapToMat(bitmap, mat);
        // Conver the color


        if(debug)
            Log.d("BitmapProcess","OpenCVFindCountours Initializing countours array");

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Bitmap bmOverlay = bitmap.copy(bitmap.getConfig(),true);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        if(debug)
            Log.d("BitmapProcess","OpenCVFindCountours Creating new canvas");
        Canvas canvas = new Canvas(bmOverlay);
        if(debug)
            Log.d("BitmapProcess","OpenCVFindCountours Drawing on canvas");
        canvas.drawPaint(paint);
        if(debug)
            Log.d("BitmapProcess","OpenCVFindCountours Converting bitmaps");
        Utils.bitmapToMat(bmOverlay, mat2);

        if(debug)
            Log.d("BitmapProcess","OpenCVFindCountours Looking for biggest countours");

        MatOfPoint bCoutnour = getBiggestCountour(mat);
        if(debug)
            Log.d("BitmapProcess","OpenCVFindCountours Got biggest countours");


        if(!bCoutnour.empty()) {
            contours = new ArrayList<MatOfPoint>(1);
            contours.add(0, bCoutnour);
            if(debug)
                Log.d("BitmapProcess","OpenCVFindCountours Drawing countours");
            Imgproc.drawContours(mat2, contours, 0, new Scalar(255, 255, 255));
        }


        //canvas.drawBitmap(rBitmap, new Matrix(), null);
        Utils.matToBitmap(mat2, bmOverlay);
        bitmapMat.setBitmap(bmOverlay);
        bitmapMat.setCountour(bCoutnour);
        if(debug)
            Log.d("BitmapProcess","OpenCVFindCountours Finished");

        return bitmapMat;
    }

    public static BitmapMat OpenCVFindCountours(BitmapMat bitMat) {
        return OpenCVFindCountours(bitMat,false);
    }

    private static MatOfPoint getBiggestCountour(Mat mat)
    {
        try {
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        }
        catch(Exception e)
        {

        }
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(mat, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        /*
            Wybieranie największego kształtu
         */
        HashMap<MatOfPoint, Double> shapeMap = new HashMap<MatOfPoint, Double>();
        MatOfPoint bContour = new MatOfPoint();
        double maxSize = 0;
        int bCountourId = 0;
        for(int i=0; i< contours.size();i++){
            MatOfPoint2f NewMtx = new MatOfPoint2f( contours.get(i).toArray() );
            double peri = Imgproc.arcLength(NewMtx, true);
            MatOfPoint2f DstMtx = NewMtx;
            Imgproc.approxPolyDP(NewMtx, DstMtx, 0.08 * peri, true);
            double area = Imgproc.contourArea(contours.get(i));
            if(DstMtx.rows() == 4) {
                shapeMap.put(contours.get(i), area);
                if (maxSize < area) {
                    maxSize = area;
                    bContour = contours.get(i);
                    bCountourId = i;
                }
            }

        }



        MatOfPoint bCoutnour = new MatOfPoint();
        if(!bContour.empty())
        {
            bCoutnour = contours.get(bCountourId);
        }
        return bCoutnour;
    }

    public static void SaveFile(Bitmap bitmap, File file)
    {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file+".jpg");
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
            output.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }


    }


    public static double CalculateDistance(Point p1, Point p2)
    {
        return Math.sqrt(Math.pow((p2.x - p1.x), 2) + Math.pow((p2.y - p1.y), 2));
    }

    public static Bitmap WriteCountoursOnBitmap(Bitmap bitmap, BitmapMat bitmapMat)
    {
     //   Bitmap rBitmap = bitmap;
        Bitmap bBitmap = bitmapMat.getBitmap();
        MatOfPoint bCoutnour = bitmapMat.getCountour();
        // Pusta bitmapa o wymiarach bitmapy z nałozonymi countarami
        // Nakładam na nią countour
        Bitmap rBitmap = Bitmap.createBitmap(bBitmap.getWidth(), bBitmap.getHeight(), bBitmap.getConfig());

        SaveFile(bBitmap, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "original"));


        // Mapa wejściowych countour. Z niej tworzymy mat z countour w skali
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>(1);
        contours.add(0, bCoutnour);

        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mat);
        // Mat na którym rysujemy countour

        Mat mat2 = new Mat(rBitmap.getHeight(), rBitmap.getWidth(), CvType.CV_8UC1);
        Log.i("IMGHX", "BCOUNTOUR not empty" + bCoutnour.width() + " " + bCoutnour.height());
        Imgproc.drawContours(mat2, contours, 0, new Scalar(255, 255, 255), 4);
        Imgproc.resize(mat2, mat2, new Size(bitmap.getWidth(), bitmap.getHeight()));

        SaveFile(bitmap, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "bitmap_resize"));



        // Imgproc.resize(mat2, mat2, new Size(bitmap.getWidth(), bitmap.getHeight()));
        // mat2 powinno być w skali
        if(!mat2.empty()) {
            MatOfPoint biggestCountour = getBiggestCountour(mat2);

            /*
            Wycinanie fragmentu i obracanie
             */
            double[] centers = {(double)bitmap.getWidth()/2, (double)bitmap.getHeight()/2};
           // Point image_center = new Point(centers);
            contours.clear();
            contours.add(biggestCountour);
            Point[] biggestCountourArray = contours.get(0).toArray();
            MatOfPoint2f NewMtx = new MatOfPoint2f(biggestCountourArray);
            //    Imgproc.
            RotatedRect minRect = Imgproc.minAreaRect(NewMtx);
            Point[] points = new Point[4];
            minRect.points(points);

            Log.d("BitmapProcess","minRect : " + minRect.angle);
            Mat mat3 = mat2.clone();
            for(int i=0; i<4; ++i)
            {
                Log.d("BitmapProcess","Point : " + points[i].toString());
                Imgproc.line(mat3, points[i], points[(i+1)%4], new Scalar(255,255,255), 10);
            }

            Bitmap bitmap4 = Bitmap.createBitmap(mat3.width(), mat3.height(), bBitmap.getConfig());
            Utils.matToBitmap(mat3, bitmap4);
            SaveFile(bitmap4, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "HARRIS_FIRST"));

            // *****************************************************

            biggestCountour = getBiggestCountour(mat3);

            Rect rec = Imgproc.boundingRect(biggestCountour);

            // Mat imCrop =  new Mat(mat,minRect.boundingRect());
            Mat imCrop=  new Mat(mat,rec);

            bitmap4 = Bitmap.createBitmap(imCrop.width(), imCrop.height(), bBitmap.getConfig());

            Utils.matToBitmap(imCrop, bitmap4);
            SaveFile(bitmap4, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "CROPPPED"));

            /*
                Przetwarzanie na wyciętym fragmeńcie
             */
            mat2 = new Mat(bitmap4.getHeight(), bitmap4.getWidth(), CvType.CV_8UC1);

            Utils.bitmapToMat(bitmap4, mat2);
            Imgproc.cvtColor(mat2, mat2, Imgproc.COLOR_RGB2GRAY);
            Imgproc.GaussianBlur(mat2, mat2, new Size(3, 3), 0);
            Imgproc.threshold(mat2, mat2, 0, 255, Imgproc.THRESH_OTSU);
            Imgproc.threshold(mat2, mat2, 0, 255, Imgproc.THRESH_BINARY_INV);
            Utils.matToBitmap(mat2, bitmap4);
            // Utils.bitmapToMat(bitmap4, mat2);

            SaveFile(bitmap4, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "CROPPPED_NORMALIZED"));

            biggestCountour = getBiggestCountour(mat2);
            contours.clear();
            contours.add(biggestCountour);

            Log.d("BitmapProcess", "biggestCountour " + biggestCountour.toString());

            mat3 = new Mat(bitmap4.getHeight(), bitmap4.getWidth(), CvType.CV_8UC1);

            Imgproc.drawContours(mat3, contours, 0, new Scalar(255, 255, 255));
            Utils.matToBitmap(mat3, bitmap4);
            SaveFile(bitmap4, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "CROPPED_BIGGEST_COUNTOUR"));


            Mat mat4 = new Mat(bitmap4.getHeight(), bitmap4.getWidth(), CvType.CV_8UC1);
            Mat mLines = new Mat();

            biggestCountourArray = contours.get(0).toArray();

            NewMtx = new MatOfPoint2f(biggestCountourArray);

            minRect = Imgproc.minAreaRect(NewMtx);

            points = new Point[4];
            minRect.points(points);

            Log.d("BitmapProcess","minRect : " + minRect.angle);

            for(int i=0; i<4; ++i)
            {
                Log.d("BitmapProcess","Point : " + points[i].toString());
                Imgproc.line(mat3, points[i], points[(i+1)%4], new Scalar(255,255,255), 10);
            }



//            Imgproc.cornerHarris(mat4, mat4, 4, 7, 0.4);


            // Imgproc.HoughLinesP(mat4, mLines, 1, Math.PI / 180, 20, 100, 20);

            // Imgproc.cornerHarris(mat3, mat4, 4, 7, 0.4);

             Imgproc.cornerHarris(mat3, mat4, 4, 7, 0.4);
            // Imgproc.cornerHarris(mat3, mat4, 14, 7, 0.01);

            //int cornerHarrisCounter = 0;


            Scalar[] colors = new Scalar[4];
            colors[0] = new Scalar(255, 255, 0);        // YELLOW
            colors[1] = new Scalar(166, 216, 74);       // GREEN
            colors[2] = new Scalar(66, 174, 208);       // BLUE
            colors[3] = new Scalar(255, 0, 0);          // RED

//            for(int j = 0; j < mat4.rows(); j++)
//            {
//                for(int i = 0; i < mat4.cols(); i++)
//                {
//                    // Log.d("BitmapProcess","Circle around corner : " + mat4.get(j, i));
//                    Imgproc.circle(mat3, new Point(mat4.get(j, i)), 10, new Scalar(0, 255, 0),4);
//                    cornerHarrisCounter++;
//                }
//
//            }



            Log.d("BitmapProcess","Line : Cols : " + mLines.cols() + " Rows : " + mLines.rows());

//            for(int j = 0; j < mat4.rows(); j++)
//            {
//                for (int i = 0; i < mLines.cols(); i++)
//                {
//                    try {
//                        double[] vec = mLines.get(j, i);
//                        float rho = (float) vec[0];
//                        float theta = (float)  vec[1];
//
//                        double a = cos(theta), b = sin(theta);
//                        double x0 = a*rho, y0 = b*rho;
//
//
//                        Point pt1 = new Point(Math.round((x0 + 1000*(-b))),Math.round((y0 + 1000 * (a))));
//                        Point pt2 = new Point(Math.round((x0 - 1000*(-b))), Math.round((y0 - 1000*(a))));
//
//                        Log.d("BitmapProcess", "Line : " + i + " vector : " + vec.length + " " + vec);
//                        double x1 = vec[0],
//                                y1 = vec[1],
//                                x2 = vec[2],
//                                y2 = vec[3];
//                        Point start = new Point(x1, y1);
//                        Point end = new Point(x2, y2);
//                        //Imgproc.line(mat3, pt1, pt2, new Scalar(255, 0, 0), 10);
//                        cornerHarrisCounter++;
//
//                    }
//                    catch(Exception e)
//                    {
//
//                    }
//                }
//            }

            Log.d("BitmapProcess", "CornerHarris " + mat4.toString() + "\nCOLS : " + mat4.cols() +"\nROWS : " + mat4.rows() );

            Bitmap bb3 = bitmap4.copy(bitmap4.getConfig(),true);

            Utils.matToBitmap(mat3, bb3);
            SaveFile(bb3, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "CORNER_HARRIS"));



            Point lDown = null;
            Point lUp = null;
            Point rDown = null;
            Point rUp = null;

            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double maxY = Double.MIN_VALUE;


            Point[] pArray = biggestCountour.toArray();
            Log.d("BitmapProcess", "countour size" + String.valueOf(pArray.length));
            for(int i = 0; i < pArray.length; i++)
            {
                Point tPoint = pArray[i];
                if(tPoint.x < minX)
                    minX = tPoint.x;

                if(tPoint.x > maxX)
                    maxX = tPoint.x;

                if(tPoint.y < minY)
                    minY = tPoint.y;

                if(tPoint.y > maxY)
                    maxY = tPoint.y;
            }





            Point xlDown = new Point(minX,minY);
            Point xlUp = new Point(minX,maxY);
            Point xrDown = new Point(maxX,minY);
            Point xrUp = new Point(maxX,maxY);

            Point maxxlDown = new Point(minX,minY);
            Point maxxlUp = new Point(minX,maxY);
            Point maxxrDown = new Point(maxX,minY);
            Point maxxrUp = new Point(maxX,maxY);

            double xlDownDistance = Double.MAX_VALUE;
            double xlUpDistance = Double.MAX_VALUE;
            double xrDownDistance = Double.MAX_VALUE;
            double xrUpDistance = Double.MAX_VALUE;

            for(int i = 0; i < pArray.length; i++)
            {
                Log.d("WRITE_CONTOURS","DISTANCES BASE : " + xlDownDistance + " " + xlUpDistance + " " + xrDownDistance + " " + xrUpDistance);

                Point p1 = pArray[i];
                Point p2;
                p2 = maxxlDown;
                double localxlDownDistance = Math.sqrt(Math.pow((p2.x - p1.x), 2) + Math.pow((p2.y - p1.y), 2));
                Log.d("WRITE_CONTOURS","EQUATIONS" + p2.toString() + " " + p1.toString());
                p2 = maxxlUp;
                double localxlUpDistance = Math.sqrt(Math.pow((p2.x - p1.x), 2) + Math.pow((p2.y - p1.y), 2));
                p2 = maxxrDown;
                double localxrDownDistance = Math.sqrt(Math.pow((p2.x - p1.x), 2) + Math.pow((p2.y - p1.y), 2));
                p2 = maxxrUp;
                double localxrUpDistance = Math.sqrt(Math.pow((p2.x - p1.x), 2) + Math.pow((p2.y - p1.y), 2));

                Log.d("WRITE_CONTOURS","DISTANCES PROCESSED : " + localxlDownDistance + " " + localxlUpDistance + " " + localxrDownDistance + " " + localxrUpDistance);



                if(xlDownDistance > localxlDownDistance)
                {
                    xlDownDistance = localxlDownDistance;
                    xlDown = p1;
                }

                if(xlUpDistance > localxlUpDistance)
                {
                    xlUpDistance = localxlUpDistance;
                    xlUp = p1;
                }

                if(xrDownDistance > localxrDownDistance)
                {
                    xrDownDistance = localxrDownDistance;
                    xrDown = p1;
                }

                if(xrUpDistance  > localxrUpDistance)
                {
                    xrUpDistance = localxrUpDistance;
                    xrUp = p1;
                }
                // Compare DOWN LEFT


            }


            Point[] points2 = points.clone();



            Point OO = new Point(0,0);
            Point OM = new Point(0,mat4.height());
            Point MO = new Point(mat4.width(),0);
            Point MM = new Point(mat4.width(),mat4.height());



            Point pOO = new Point();
            Point pOM = new Point();
            Point pMO = new Point();
            Point pMM = new Point();


            double currentCalculation;

            double minOO = Double.MAX_VALUE;
            double minOM = Double.MAX_VALUE;
            double minMO = Double.MAX_VALUE;
            double minMM = Double.MAX_VALUE;


            for(int i = 0; i < points2.length; i++)
            {
                // Wyszukujemy punktów na lewo/dół od środka
                currentCalculation = CalculateDistance(OO,points2[i]);
                if(minOO > currentCalculation )
                {
                    minOO = currentCalculation;
                    pOO = points2[i];
                }

                currentCalculation = CalculateDistance(OM,points2[i]);
                if(minOM > currentCalculation )
                {
                    minOM = currentCalculation;
                    pOM = points2[i];
                }

                currentCalculation = CalculateDistance(MO,points2[i]);
                if(minMO > currentCalculation )
                {
                    minMO  = currentCalculation;
                    pMO = points2[i];
                }

                currentCalculation = CalculateDistance(MM,points2[i]);
                if(minMM > currentCalculation )
                {
                    minMM   = currentCalculation;
                    pMM = points2[i];
                }

            }

//            points[0] = pOO;
//            points[1] = pOM;
//            points[2] = pMO;
//            points[3] = pMM;

            points[0] = pOO;
            points[1] = pMO;
            points[2] = pMM;
            points[3] = pOM;



            Point pAcurateOO = new Point();
            Point pAcurateOM = new Point();
            Point pAcurateMO = new Point();
            Point pAcurateMM = new Point();

            double minAcurateOO = Double.MAX_VALUE;
            double minAcurateOM = Double.MAX_VALUE;
            double minAcurateMO = Double.MAX_VALUE;
            double minAcurateMM = Double.MAX_VALUE;

            for(int i = 0; i < pArray.length; i++)
            {
                Point tPoint = pArray[i];

                currentCalculation = CalculateDistance(pOO,tPoint );
                if(minAcurateOO > currentCalculation)
                {
                    pAcurateOO = tPoint;
                    minAcurateOO = currentCalculation;
                }

                currentCalculation = CalculateDistance(pOM,tPoint );
                if(minAcurateOM > currentCalculation)
                {
                    pAcurateOM = tPoint;
                    minAcurateOM = currentCalculation;
                }

                currentCalculation = CalculateDistance(pMM,tPoint );
                if(minAcurateMM > currentCalculation)
                {
                    pAcurateMM = tPoint;
                    minAcurateMM = currentCalculation;
                }

                currentCalculation = CalculateDistance(pMO,tPoint );
                if(minAcurateMO > currentCalculation)
                {
                    pAcurateMO = tPoint;
                    minAcurateMO = currentCalculation;
                }
            }



            Log.d("BitmapProcess","NEAREAST OO : " + points[0] + "(" + OO +")"  + "\nNEAREST MO : " + points[1] + "(" + MO +")" + "\nNEAREST MM : " + points[2] + "(" + MM +")" + "\nNEAREST OM : " + points[3] + "(" + OM +")");


            List<Point> dest = new ArrayList<Point>();
            //dest.add(xlDown);
            //dest.add(xlUp);
            //dest.add(xrUp);
            //dest.add(xrDown);

            points2[0] = pAcurateOO;
            points2[1] = pAcurateMO;
            points2[2] = pAcurateMM;
            points2[3] = pAcurateOM;





            points[0] = pOO;
            points[1] = pMO;
            points[2] = pMM;
            points[3] = pOM;

            dest.add(points[0]);
            dest.add(points[1]);
            dest.add(points[2]);
            dest.add(points[3]);





            double p1 = Math.sqrt(Math.pow((points[1].x - points[0].x), 2) + Math.pow((points[1].y - points[0].y), 2));

            double p2 = Math.sqrt(Math.pow((points[2].x - points[1].x), 2) + Math.pow((points[2].y - points[1].y), 2));

            double x;
            double y;
            if(p1 > p2)
            {
                if(mat3.width() < p2) {
                    x = p2;
                    y = p1;
                }
                else
                {
                    x = p1;
                    y = p2;

                }
            }
            else
            {
                if(mat3.width() < p1) {

                    x = p1;
                    y = p2;

                }
                else
                {
                    x = p2;
                    y = p1;
                }
            }


            maxxlDown = new Point(0,0);
            maxxlUp = new Point(0,y);
            maxxrUp = new Point(x,y);
            maxxrDown = new Point(x,0);


            Log.d("BitmapProcess","P1 : " + p1 + "P2 : " + p2);


            Bitmap bb = bitmap4.copy(bitmap4.getConfig(),true);
            Mat matBb = new Mat(bb.getHeight(), bb.getWidth(), CvType.CV_8UC1);
            Utils.bitmapToMat(bb, matBb);
            // Mat na którym rysujemy countour

            // points
            float mX = Float.MAX_VALUE;
            float mY = Float.MAX_VALUE;
            float MX = Float.MIN_VALUE;
            float MY = Float.MIN_VALUE;

            Point minCenter = minRect.center;

            Imgproc.circle(matBb,new Point(10,10),10,new Scalar(200,200,200),8);

            Imgproc.circle(matBb, minCenter, 10, new Scalar(127, 127, 127), 8);



            for (int i = 0; i < points.length; i++)
            {
                Imgproc.circle(matBb,points[i],10,colors[i],4);
                // Szukamy lewego rogu
                if(mX > points[i].x)
                    mX = (float) points[i].x;

                if(MX < points[i].x)
                    MX = (float) points[i].x;

                if(mY > points[i].y)
                    mY = (float) points[i].y;

                if(MY < points[i].y)
                    MY = (float) points[i].y;

            }

            Log.d("BitmapProcess", "Punkty graniczne : " + mX + " " + mY + " " + MX + " " + MY);





//            dest.add(points[0]);
//            dest.add(points[1]);
//            dest.add(points[2]);
//            dest.add(points[3]);






            Imgproc.circle(imCrop, points[0], 10, new Scalar(0,255,0), 4);     // 0,  0
            Imgproc.circle(imCrop, points[1], 10, new Scalar(0,255,0), 4); // MAX 0
            Imgproc.circle(imCrop, points[2], 10, new Scalar(0,255,0), 4);   // MAX MAX
            Imgproc.circle(imCrop, points[3], 10, new Scalar(0,255,0), 4);   // 0   MAX

            Imgproc.circle(imCrop, points2[0], 10, new Scalar(0,255,255), 4);     // 0,  0
            Imgproc.circle(imCrop, points2[1], 10, new Scalar(0,255,255), 4); // MAX 0
            Imgproc.circle(imCrop, points2[2], 10, new Scalar(0,255,255), 4);   // MAX MAX
            Imgproc.circle(imCrop, points2[3], 10, new Scalar(0,255,255), 4);   // 0   MAX




            Bitmap rBitmap5 = Bitmap.createBitmap(imCrop.width(), imCrop.height(), bBitmap.getConfig());
            Utils.matToBitmap(imCrop, rBitmap5);
            SaveFile(rBitmap5, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "PRE_RORATION_POINTS"));

//            Imgproc.circle(matBb,xlDown,10,new Scalar(0, 255, 255),4);
//            Imgproc.circle(matBb,xlUp,10,new Scalar(0, 255, 255),4);
//            Imgproc.circle(matBb,xrUp, 10, new Scalar(0, 255, 255), 4);
//            Imgproc.circle(matBb,xrDown, 10, new Scalar(0, 255, 255), 4);
            Utils.matToBitmap(matBb, bb);
            SaveFile(bb, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "circle_UNO"));




            Bitmap bb2 = bitmap4.copy(bitmap4.getConfig(),true);
            matBb = new Mat(bb2.getHeight(), bb2.getWidth(), CvType.CV_8UC1);
            Utils.bitmapToMat(bb2, matBb);
            // Mat na którym rysujemy countour



            Imgproc.circle(matBb, maxxlDown, 10, colors[0], 4);     // 0,  0
            Imgproc.circle(matBb, maxxrDown, 10, colors[1], 4); // MAX 0
            Imgproc.circle(matBb, maxxrUp, 10, colors[2], 4);   // MAX MAX
            Imgproc.circle(matBb, maxxlUp, 10, colors[3], 4);   // 0   MAX


            Utils.matToBitmap(matBb, bb2);
            SaveFile(bb2, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "circle_DUO"));

            List<Point> src = new ArrayList<Point>();
            src.add(maxxlDown);
            src.add(maxxrDown);
            src.add(maxxrUp);
            src.add(maxxlUp);



            Mat M = Imgproc.getRotationMatrix2D(minRect.center, minRect.angle, 1.0);




            Mat src_mat = Converters.vector_Point2f_to_Mat(src);

            dest.clear();
            dest.add(points2[0]);
            dest.add(points2[1]);
            dest.add(points2[2]);
            dest.add(points2[3]);


            Mat endM = Converters.vector_Point2f_to_Mat(dest);



//            Mat perspectiveTransform = Imgproc.getPerspectiveTransform(src_mat, endM);
            Mat perspectiveTransform = Imgproc.getPerspectiveTransform(endM, src_mat);
            Log.d("WRITE_CONTOUR","PERSPECTIVE_TRANSFORM" + perspectiveTransform.toString());

//            Mat src_mat=new Mat(4,1,CvType.CV_32FC2);
//            Mat dst_mat=new Mat(4,1,CvType.CV_32FC2);
//            src_mat.put(0, 0, 407.0, 74.0, 1606.0, 74.0, 420.0, 2589.0, 1698.0, 2589.0);
//            dst_mat.put(0, 0, 0.0, 0.0, 1600.0, 0.0, 0.0, 2500.0, 1600.0, 2500.0);
//            Mat perspectiveTransform=Imgproc.getPerspectiveTransform(src_mat, dst_mat);
            Log.d("BitmapProcess", "TRANSFORM : " + dest.toString() + " TO : " + src.toString());

            Mat outputMat = imCrop;
            Mat imCropCopy = imCrop.clone();

            Imgproc.warpAffine(imCrop, outputMat, M, new Size(imCrop.width(), imCrop.height()), Imgproc.INTER_CUBIC);

            Bitmap rBitmap2 = Bitmap.createBitmap(rec.width, rec.height, bBitmap.getConfig());

            Utils.matToBitmap(outputMat, rBitmap2);
            SaveFile(rBitmap2, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "POSSIBLE_ROTATED_II"));


            outputMat = imCrop;
            imCrop = imCropCopy.clone();
            Imgproc.warpPerspective(imCrop,
                    outputMat,
                    perspectiveTransform,
                    new Size(imCrop.width(), imCrop.height()),
                    Imgproc.INTER_CUBIC);
            imCrop.release();


          //  Rect rec2 = new Rect(new Point(points[0].x, points[0].y), new Point(points[2].x, points[2].y));

            Rect rec2 = new Rect( (int) maxxlDown.x, (int) maxxlDown.y, (int) (maxxrDown.x - maxxlDown.x), (int) (maxxrUp.y - maxxlDown.y));

//            Imgproc.circle(matBb, maxxlDown, 10, colors[0], 4);     // 0,  0
//            Imgproc.circle(matBb, maxxrDown, 10, colors[1], 4); // MAX 0
//            Imgproc.circle(matBb, maxxrUp, 10, colors[2], 4);   // MAX MAX
//            Imgproc.circle(matBb, maxxlUp, 10, colors[3], 4);   // 0   MAX

            imCrop=  new Mat(outputMat,rec2);
            rBitmap2 = Bitmap.createBitmap(imCrop.width(), imCrop.height(), bBitmap.getConfig());
            Utils.matToBitmap(imCrop, rBitmap2);
            SaveFile(rBitmap2, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "POSSIBLE_ROTATED_I"));
            imCrop.release();

//            // ***********************
//            try {
//            // outputMat = imCrop;
//            // outputMat = new Mat((int) minRect.size.height, (int) minRect.size.width, imCrop.type());
//            imCrop = imCropCopy.clone();
//            outputMat = new Mat(bitmap.getHeight(),bitmap.getWidth(),imCrop.type());
//
//            perspectiveTransform = Imgproc.getPerspectiveTransform(src_mat, endM);
//            Imgproc.warpPerspective(imCrop,
//                    outputMat,
//                    perspectiveTransform,
//                    new Size(outputMat.width(), outputMat.height()),
//                    Imgproc.INTER_CUBIC);
//                imCrop.release();
//
//            Imgproc.circle(outputMat, maxxlDown, 10, new Scalar(255,0,0), 4);     // 0,  0
//            Imgproc.circle(outputMat, maxxrDown, 10, new Scalar(255,0,0), 4); // MAX 0
//            Imgproc.circle(outputMat, maxxrUp, 10, new Scalar(255,0,0), 4);   // MAX MAX
//            Imgproc.circle(outputMat, maxxlUp, 10, new Scalar(255,0,0), 4);   // 0   MAX
//
//
//                Imgproc.circle(outputMat, points[0], 10, new Scalar(255,255,0), 4);     // 0,  0
//                Imgproc.circle(outputMat, points[1], 10, new Scalar(255,255,0), 4); // MAX 0
//                Imgproc.circle(outputMat, points[2], 10, new Scalar(255, 255, 0), 4);   // MAX MAX
//                Imgproc.circle(outputMat, points[3], 10, new Scalar(255, 255, 0), 4);   // 0   MAX
//
//
//
//            rBitmap2 = Bitmap.createBitmap(outputMat.width(), outputMat.height(), bBitmap.getConfig());
//                Utils.matToBitmap(outputMat, rBitmap2);
//            SaveFile(rBitmap2, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "POSSIBLE_ROTATED_III"));
//
//            }
//            catch (Exception e)
//            {
//                Log.d("BitmapProcess", "POSSIBLE_ROTATED_III ERROR : " + e.getLocalizedMessage(),e);
//
//                e.printStackTrace();
//            }

//            // ***********************
//            try {
//                perspectiveTransform = Imgproc.getPerspectiveTransform(endM, src_mat);
//                imCrop = imCropCopy.clone();
//                outputMat = imCrop;
//
//                Imgproc.warpPerspective(imCrop,
//                        outputMat,
//                        perspectiveTransform,
//                        new Size(imCrop.width(), imCrop.height()),
//                        Imgproc.INTER_CUBIC);
//                imCrop.release();
//                rBitmap2 = Bitmap.createBitmap(rec.height, rec.width, bBitmap.getConfig());
//                Utils.matToBitmap(outputMat, rBitmap2);
//                SaveFile(rBitmap2, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "POSSIBLE_ROTATED_IIII"));
//
//            }
//            catch (Exception e)
//            {
//                Log.d("BitmapProcess", "POSSIBLE_ROTATED_IIII ERROR");
//                e.printStackTrace();
//            }
//            // ***********************
//
//
//            // ***********************
//            try {
//                imCrop = imCropCopy.clone();
//                perspectiveTransform = Imgproc.getPerspectiveTransform(endM, src_mat);
//                outputMat = imCrop;
//                Imgproc.warpPerspective(imCrop,
//                        outputMat,
//                        perspectiveTransform,
//                        new Size(imCrop.height(),imCrop.width()),
//                        Imgproc.INTER_CUBIC);
//
//                rBitmap2 = Bitmap.createBitmap(rec.width, rec.height, bBitmap.getConfig());
//                imCrop.release();
//                Utils.matToBitmap(outputMat, rBitmap2);
//                SaveFile(rBitmap2, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "POSSIBLE_ROTATED_IIIII"));
//
//            }
//            catch (Exception e)
//            {
//                Log.d("BitmapProcess", "POSSIBLE_ROTATED_IIIII ERROR");
//                e.printStackTrace();
//            }
//            // ***********************
//
//            // ***********************
//            try {
//                perspectiveTransform = Imgproc.getPerspectiveTransform(endM, src_mat);
//                outputMat = imCrop;
//                Imgproc.warpPerspective(imCrop,
//                        outputMat,
//                        perspectiveTransform,
//                        new Size(imCrop.height(),imCrop.width()),
//                        Imgproc.INTER_CUBIC);
//
//                rBitmap2 = Bitmap.createBitmap(rec.height, rec.width, bBitmap.getConfig());
//
//                Utils.matToBitmap(outputMat, rBitmap2);
//                SaveFile(rBitmap2, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "POSSIBLE_ROTATED_IIIIII"));
//
//            }
//            catch (Exception e)
//            {
//                Log.d("BitmapProcess", "POSSIBLE_ROTATED_IIIIII ERROR");
//                e.printStackTrace();
//            }
//            // ***********************

            SaveFile(bitmap, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "POSSIBLE_NON_ROTATED"));




            return rBitmap2;
        }


        MatOfPoint bContour = new MatOfPoint();
        double maxSize = 0;
        int bCountourId = 0;
        for(int i=0; i< contours.size();i++){
//            peri = cv2.arcLength(c, True)
//            approx = cv2.approxPolyDP(c, 0.02
            /// Source variable
            MatOfPoint SrcMtx;
            HashMap<MatOfPoint, Double> shapeMap = new HashMap<MatOfPoint, Double>();
            /// New variable
            MatOfPoint2f NewMtx = new MatOfPoint2f( contours.get(i).toArray() );

            double peri = Imgproc.arcLength(NewMtx, true);
            MatOfPoint2f DstMtx = NewMtx;
            Imgproc.approxPolyDP(NewMtx, DstMtx, 0.02 * peri, true);
            double area = Imgproc.contourArea(contours.get(i));
            // Log.i("CONT",DstMtx.cols() + " rows " + DstMtx.rows());
            if(DstMtx.rows() == 4) {
                shapeMap.put(contours.get(i), area);
                if (maxSize < area) {
                    maxSize = area;
                    bContour = contours.get(i);
                    bCountourId = i;
                }
            }

        }

      //  mat2.mul(bCoutnour);



        if(!bContour.empty())
        {
//            Imgproc.drawContours(mat2,bContour,1,new Scalar(0,255,255));
            Imgproc.drawContours(mat,contours,bCountourId,new Scalar(0,255,255));
            bCoutnour = contours.get(bCountourId);
//            Rect rect = Imgproc.boundingRect(contours.get(i));
        }
        Utils.matToBitmap(mat, rBitmap);
        return rBitmap;
    }




    public static Bitmap WriteCountoursOnBitmap_copy(Bitmap bitmap, BitmapMat bitmapMat)
    {
        //   Bitmap rBitmap = bitmap;
        Bitmap bBitmap = bitmapMat.getBitmap();
        MatOfPoint bCoutnour = bitmapMat.getCountour();
        // Pusta bitmapa o wymiarach bitmapy z nałozonymi countarami
        // Nakładam na nią countour
        Bitmap rBitmap = Bitmap.createBitmap(bBitmap.getWidth(), bBitmap.getHeight(), bBitmap.getConfig());

        SaveFile(bBitmap, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "original"));


        // Mapa wejściowych countour. Z niej tworzymy mat z countour w skali
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>(1);
        contours.add(0, bCoutnour);

        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap, mat);
        // Mat na którym rysujemy countour

        Mat mat2 = new Mat(rBitmap.getHeight(), rBitmap.getWidth(), CvType.CV_8UC1);
        Log.i("IMGHX", "BCOUNTOUR not empty" + bCoutnour.width() + " " + bCoutnour.height());
        Imgproc.drawContours(mat2, contours, 0, new Scalar(255, 255, 255), 4);
        Imgproc.resize(mat2, mat2, new Size(bitmap.getWidth(), bitmap.getHeight()));

        SaveFile(bitmap, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "bitmap_resize"));





        // Imgproc.resize(mat2, mat2, new Size(bitmap.getWidth(), bitmap.getHeight()));
        // mat2 powinno być w skali
        if(!mat2.empty()) {
            MatOfPoint biggestCountour = getBiggestCountour(mat2);

            /*
            Wycinanie fragmentu i obracanie
             */
            double[] centers = {(double)bitmap.getWidth()/2, (double)bitmap.getHeight()/2};
            // Point image_center = new Point(centers);

            Rect rec = Imgproc.boundingRect(biggestCountour);
            Mat imCrop=  new Mat(mat,rec);

            Bitmap bitmap4 = Bitmap.createBitmap(imCrop.width(), imCrop.height(), bBitmap.getConfig());

            Utils.matToBitmap(imCrop, bitmap4);
            SaveFile(bitmap4, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "CROPPPED"));

            /*
                Przetwarzanie na wyciętym fragmeńcie
             */
            mat2 = new Mat(bitmap4.getHeight(), bitmap4.getWidth(), CvType.CV_8UC1);

            Utils.bitmapToMat(bitmap4, mat2);
            Imgproc.cvtColor(mat2, mat2, Imgproc.COLOR_RGB2GRAY);
            Imgproc.GaussianBlur(mat2, mat2, new Size(3, 3), 0);
            Imgproc.threshold(mat2, mat2, 0, 255, Imgproc.THRESH_OTSU);
            Imgproc.threshold(mat2, mat2, 0, 255, Imgproc.THRESH_BINARY_INV);
            Utils.matToBitmap(mat2, bitmap4);
            // Utils.bitmapToMat(bitmap4, mat2);

            SaveFile(bitmap4, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "CROPPPED_NORMALIZED"));

            biggestCountour = getBiggestCountour(mat2);
            contours.clear();
            contours.add(biggestCountour);

            Log.d("BitmapProcess", "biggestCountour " + biggestCountour.toString());

            Mat mat3 = new Mat(bitmap4.getHeight(), bitmap4.getWidth(), CvType.CV_8UC1);

            Imgproc.drawContours(mat3, contours, 0, new Scalar(255, 255, 255));
            Utils.matToBitmap(mat3, bitmap4);
            SaveFile(bitmap4, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "CROPPED_BIGGEST_COUNTOUR"));


            Mat mat4 = new Mat(bitmap4.getHeight(), bitmap4.getWidth(), CvType.CV_8UC1);
            Mat mLines = new Mat();

            Point[] biggestCountourArray = contours.get(0).toArray();

            MatOfPoint2f NewMtx = new MatOfPoint2f(biggestCountourArray);

            //    Imgproc.
            RotatedRect minRect = Imgproc.minAreaRect(NewMtx);


            Point[] points = new Point[4];
            minRect.points(points);

            Log.d("BitmapProcess", "minRect : " + minRect.angle);

            for(int i=0; i<4; ++i)
            {
                Log.d("BitmapProcess","Point : " + points[i].toString());
                Imgproc.line(mat3, points[i], points[(i+1)%4], new Scalar(255,255,255), 10);
            }



//            Imgproc.cornerHarris(mat4, mat4, 4, 7, 0.4);


            // Imgproc.HoughLinesP(mat4, mLines, 1, Math.PI / 180, 20, 100, 20);

            // Imgproc.cornerHarris(mat3, mat4, 4, 7, 0.4);

            Imgproc.cornerHarris(mat3, mat4, 4, 7, 0.4);
            // Imgproc.cornerHarris(mat3, mat4, 14, 7, 0.01);

            //int cornerHarrisCounter = 0;


            Scalar[] colors = new Scalar[4];
            colors[0] = new Scalar(255, 255, 0);        // YELLOW
            colors[1] = new Scalar(166, 216, 74);       // GREEN
            colors[2] = new Scalar(66, 174, 208);       // BLUE
            colors[3] = new Scalar(255, 0, 0);          // RED

//            for(int j = 0; j < mat4.rows(); j++)
//            {
//                for(int i = 0; i < mat4.cols(); i++)
//                {
//                    // Log.d("BitmapProcess","Circle around corner : " + mat4.get(j, i));
//                    Imgproc.circle(mat3, new Point(mat4.get(j, i)), 10, new Scalar(0, 255, 0),4);
//                    cornerHarrisCounter++;
//                }
//
//            }



            Log.d("BitmapProcess","Line : Cols : " + mLines.cols() + " Rows : " + mLines.rows());

//            for(int j = 0; j < mat4.rows(); j++)
//            {
//                for (int i = 0; i < mLines.cols(); i++)
//                {
//                    try {
//                        double[] vec = mLines.get(j, i);
//                        float rho = (float) vec[0];
//                        float theta = (float)  vec[1];
//
//                        double a = cos(theta), b = sin(theta);
//                        double x0 = a*rho, y0 = b*rho;
//
//
//                        Point pt1 = new Point(Math.round((x0 + 1000*(-b))),Math.round((y0 + 1000 * (a))));
//                        Point pt2 = new Point(Math.round((x0 - 1000*(-b))), Math.round((y0 - 1000*(a))));
//
//                        Log.d("BitmapProcess", "Line : " + i + " vector : " + vec.length + " " + vec);
//                        double x1 = vec[0],
//                                y1 = vec[1],
//                                x2 = vec[2],
//                                y2 = vec[3];
//                        Point start = new Point(x1, y1);
//                        Point end = new Point(x2, y2);
//                        //Imgproc.line(mat3, pt1, pt2, new Scalar(255, 0, 0), 10);
//                        cornerHarrisCounter++;
//
//                    }
//                    catch(Exception e)
//                    {
//
//                    }
//                }
//            }

            Log.d("BitmapProcess", "CornerHarris " + mat4.toString() + "\nCOLS : " + mat4.cols() +"\nROWS : " + mat4.rows() );

            Bitmap bb3 = bitmap4.copy(bitmap4.getConfig(),true);

            Utils.matToBitmap(mat3, bb3);
            SaveFile(bb3, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "CORNER_HARRIS"));



            Point lDown = null;
            Point lUp = null;
            Point rDown = null;
            Point rUp = null;

            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double maxY = Double.MIN_VALUE;


            Point[] pArray = biggestCountour.toArray();
            Log.d("BitmapProcess", "countour size" + String.valueOf(pArray.length));
            for(int i = 0; i < pArray.length; i++)
            {
                Point tPoint = pArray[i];
                if(tPoint.x < minX)
                    minX = tPoint.x;

                if(tPoint.x > maxX)
                    maxX = tPoint.x;

                if(tPoint.y < minY)
                    minY = tPoint.y;

                if(tPoint.y > maxY)
                    maxY = tPoint.y;
            }




            Point xlDown = new Point(minX,minY);
            Point xlUp = new Point(minX,maxY);
            Point xrDown = new Point(maxX,minY);
            Point xrUp = new Point(maxX,maxY);

            Point maxxlDown = new Point(minX,minY);
            Point maxxlUp = new Point(minX,maxY);
            Point maxxrDown = new Point(maxX,minY);
            Point maxxrUp = new Point(maxX,maxY);

            double xlDownDistance = Double.MAX_VALUE;
            double xlUpDistance = Double.MAX_VALUE;
            double xrDownDistance = Double.MAX_VALUE;
            double xrUpDistance = Double.MAX_VALUE;

            for(int i = 0; i < pArray.length; i++)
            {
                Log.d("WRITE_CONTOURS","DISTANCES BASE : " + xlDownDistance + " " + xlUpDistance + " " + xrDownDistance + " " + xrUpDistance);

                Point p1 = pArray[i];
                Point p2;
                p2 = maxxlDown;
                double localxlDownDistance = Math.sqrt(Math.pow((p2.x - p1.x), 2) + Math.pow((p2.y - p1.y), 2));
                Log.d("WRITE_CONTOURS","EQUATIONS" + p2.toString() + " " + p1.toString());
                p2 = maxxlUp;
                double localxlUpDistance = Math.sqrt(Math.pow((p2.x - p1.x), 2) + Math.pow((p2.y - p1.y), 2));
                p2 = maxxrDown;
                double localxrDownDistance = Math.sqrt(Math.pow((p2.x - p1.x), 2) + Math.pow((p2.y - p1.y), 2));
                p2 = maxxrUp;
                double localxrUpDistance = Math.sqrt(Math.pow((p2.x - p1.x), 2) + Math.pow((p2.y - p1.y), 2));

                Log.d("WRITE_CONTOURS","DISTANCES PROCESSED : " + localxlDownDistance + " " + localxlUpDistance + " " + localxrDownDistance + " " + localxrUpDistance);



                if(xlDownDistance > localxlDownDistance)
                {
                    xlDownDistance = localxlDownDistance;
                    xlDown = p1;
                }

                if(xlUpDistance > localxlUpDistance)
                {
                    xlUpDistance = localxlUpDistance;
                    xlUp = p1;
                }

                if(xrDownDistance > localxrDownDistance)
                {
                    xrDownDistance = localxrDownDistance;
                    xrDown = p1;
                }

                if(xrUpDistance  > localxrUpDistance)
                {
                    xrUpDistance = localxrUpDistance;
                    xrUp = p1;
                }
                // Compare DOWN LEFT


            }


            Point[] points2 = points.clone();



            Point OO = new Point(0,0);
            Point OM = new Point(0,mat4.height());
            Point MO = new Point(mat4.width(),0);
            Point MM = new Point(mat4.width(),mat4.height());



            Point pOO = new Point();
            Point pOM = new Point();
            Point pMO = new Point();
            Point pMM = new Point();


            double currentCalculation;

            double minOO = Double.MAX_VALUE;
            double minOM = Double.MAX_VALUE;
            double minMO = Double.MAX_VALUE;
            double minMM = Double.MAX_VALUE;


            for(int i = 0; i < points2.length; i++)
            {
                // Wyszukujemy punktów na lewo/dół od środka
                currentCalculation = CalculateDistance(OO,points2[i]);
                if(minOO > currentCalculation )
                {
                    minOO = currentCalculation;
                    pOO = points2[i];
                }

                currentCalculation = CalculateDistance(OM,points2[i]);
                if(minOM > currentCalculation )
                {
                    minOM = currentCalculation;
                    pOM = points2[i];
                }

                currentCalculation = CalculateDistance(MO,points2[i]);
                if(minMO > currentCalculation )
                {
                    minMO  = currentCalculation;
                    pMO = points2[i];
                }

                currentCalculation = CalculateDistance(MM,points2[i]);
                if(minMM > currentCalculation )
                {
                    minMM   = currentCalculation;
                    pMM = points2[i];
                }

            }

//            points[0] = pOO;
//            points[1] = pOM;
//            points[2] = pMO;
//            points[3] = pMM;

            points[0] = pOO;
            points[1] = pMO;
            points[2] = pMM;
            points[3] = pOM;

            Log.d("BitmapProcess","NEAREAST OO : " + points[0] + "(" + OO +")"  + "\nNEAREST MO : " + points[1] + "(" + MO +")" + "\nNEAREST MM : " + points[2] + "(" + MM +")" + "\nNEAREST OM : " + points[3] + "(" + OM +")");


            List<Point> dest = new ArrayList<Point>();
            //dest.add(xlDown);
            //dest.add(xlUp);
            //dest.add(xrUp);
            //dest.add(xrDown);



            dest.add(points[0]);
            dest.add(points[1]);
            dest.add(points[2]);
            dest.add(points[3]);


            Mat endM = Converters.vector_Point2f_to_Mat(dest);


            double p1 = Math.sqrt(Math.pow((points[1].x - points[0].x), 2) + Math.pow((points[1].y - points[0].y), 2));

            double p2 = Math.sqrt(Math.pow((points[2].x - points[1].x), 2) + Math.pow((points[2].y - points[1].y), 2));

            double x;
            double y;
            if(p1 > p2)
            {
                if(mat3.width() < p2) {
                    x = p2;
                    y = p1;
                }
                else
                {
                    x = p1;
                    y = p2;

                }
            }
            else
            {
                if(mat3.width() < p1) {

                    x = p1;
                    y = p2;

                }
                else
                {
                    x = p2;
                    y = p1;
                }
            }


            maxxlDown = new Point(0,0);
            maxxlUp = new Point(0,y);
            maxxrUp = new Point(x,y);
            maxxrDown = new Point(x,0);


            Log.d("BitmapProcess","P1 : " + p1 + "P2 : " + p2);


            Bitmap bb = bitmap4.copy(bitmap4.getConfig(),true);
            Mat matBb = new Mat(bb.getHeight(), bb.getWidth(), CvType.CV_8UC1);
            Utils.bitmapToMat(bb, matBb);
            // Mat na którym rysujemy countour

            // points
            float mX = Float.MAX_VALUE;
            float mY = Float.MAX_VALUE;
            float MX = Float.MIN_VALUE;
            float MY = Float.MIN_VALUE;

            Point minCenter = minRect.center;

            Imgproc.circle(matBb,new Point(10,10),10,new Scalar(200,200,200),8);

            Imgproc.circle(matBb, minCenter, 10, new Scalar(127, 127, 127), 8);



            for (int i = 0; i < points.length; i++)
            {
                Imgproc.circle(matBb,points[i],10,colors[i],4);
                // Szukamy lewego rogu
                if(mX > points[i].x)
                    mX = (float) points[i].x;

                if(MX < points[i].x)
                    MX = (float) points[i].x;

                if(mY > points[i].y)
                    mY = (float) points[i].y;

                if(MY < points[i].y)
                    MY = (float) points[i].y;

            }

            Log.d("BitmapProcess", "Punkty graniczne : " + mX + " " + mY + " " + MX + " " + MY);





//            dest.add(points[0]);
//            dest.add(points[1]);
//            dest.add(points[2]);
//            dest.add(points[3]);






            Imgproc.circle(imCrop, points[0], 10, new Scalar(0,255,0), 4);     // 0,  0
            Imgproc.circle(imCrop, points[1], 10, new Scalar(0,255,0), 4); // MAX 0
            Imgproc.circle(imCrop, points[2], 10, new Scalar(0,255,0), 4);   // MAX MAX
            Imgproc.circle(imCrop, points[3], 10, new Scalar(0,255,0), 4);   // 0   MAX


            Bitmap rBitmap5 = Bitmap.createBitmap(imCrop.width(), imCrop.height(), bBitmap.getConfig());
            Utils.matToBitmap(imCrop, rBitmap5);
            SaveFile(rBitmap5, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "PRE_RORATION_POINTS"));

//            Imgproc.circle(matBb,xlDown,10,new Scalar(0, 255, 255),4);
//            Imgproc.circle(matBb,xlUp,10,new Scalar(0, 255, 255),4);
//            Imgproc.circle(matBb,xrUp, 10, new Scalar(0, 255, 255), 4);
//            Imgproc.circle(matBb,xrDown, 10, new Scalar(0, 255, 255), 4);
            Utils.matToBitmap(matBb, bb);
            SaveFile(bb, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "circle_UNO"));




            Bitmap bb2 = bitmap4.copy(bitmap4.getConfig(),true);
            matBb = new Mat(bb2.getHeight(), bb2.getWidth(), CvType.CV_8UC1);
            Utils.bitmapToMat(bb2, matBb);
            // Mat na którym rysujemy countour



            Imgproc.circle(matBb, maxxlDown, 10, colors[0], 4);     // 0,  0
            Imgproc.circle(matBb, maxxrDown, 10, colors[1], 4); // MAX 0
            Imgproc.circle(matBb, maxxrUp, 10, colors[2], 4);   // MAX MAX
            Imgproc.circle(matBb, maxxlUp, 10, colors[3], 4);   // 0   MAX


            Utils.matToBitmap(matBb, bb2);
            SaveFile(bb2, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "circle_DUO"));

            List<Point> src = new ArrayList<Point>();
            src.add(maxxlDown);
            src.add(maxxrDown);
            src.add(maxxrUp);
            src.add(maxxlUp);



            Mat M = Imgproc.getRotationMatrix2D(minRect.center, minRect.angle, 1.0);




            Mat src_mat = Converters.vector_Point2f_to_Mat(src);

//            Mat perspectiveTransform = Imgproc.getPerspectiveTransform(src_mat, endM);
            Mat perspectiveTransform = Imgproc.getPerspectiveTransform(endM, src_mat);
            Log.d("WRITE_CONTOUR","PERSPECTIVE_TRANSFORM" + perspectiveTransform.toString());

//            Mat src_mat=new Mat(4,1,CvType.CV_32FC2);
//            Mat dst_mat=new Mat(4,1,CvType.CV_32FC2);
//            src_mat.put(0, 0, 407.0, 74.0, 1606.0, 74.0, 420.0, 2589.0, 1698.0, 2589.0);
//            dst_mat.put(0, 0, 0.0, 0.0, 1600.0, 0.0, 0.0, 2500.0, 1600.0, 2500.0);
//            Mat perspectiveTransform=Imgproc.getPerspectiveTransform(src_mat, dst_mat);
            Log.d("BitmapProcess", "TRANSFORM : " + dest.toString() + " TO : " + src.toString());

            Mat outputMat = imCrop;

            Imgproc.warpAffine(imCrop,outputMat,M,new Size(imCrop.width(), imCrop.height()), Imgproc.INTER_CUBIC);



            Bitmap rBitmap2 = Bitmap.createBitmap(rec.width, rec.height, bBitmap.getConfig());

            Bitmap rBitmap3 = rBitmap2.copy(rBitmap2.getConfig(), true);
            Utils.matToBitmap(outputMat, rBitmap3);
            SaveFile(rBitmap3, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "POSSIBLE_ROTATED_II"));



            outputMat = imCrop;

            Imgproc.warpPerspective(imCrop,
                    outputMat,
                    perspectiveTransform,
                    new Size(imCrop.width(), imCrop.height()),
                    Imgproc.INTER_CUBIC);


            //Imgproc.warpAffine(imCrop,outputMat,M,new Size(imCrop.width(), imCrop.height()), Imgproc.INTER_CUBIC);

//            Imgproc.warpPerspective(imCrop,
//                    outputMat,
//                    M,
//                    new Size(imCrop.width(), imCrop.height()),
//                    Imgproc.INTER_CUBIC);

            rBitmap2 = Bitmap.createBitmap(rec.width, rec.height, bBitmap.getConfig());

            rBitmap3 = rBitmap2.copy(rBitmap2.getConfig(), true);
            Utils.matToBitmap(outputMat, rBitmap3);
            SaveFile(rBitmap3, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "POSSIBLE_ROTATED_I"));


            // ***********************
            try {
                // outputMat = imCrop;
                // outputMat = new Mat((int) minRect.size.height, (int) minRect.size.width, imCrop.type());
                outputMat = new Mat(bitmap.getHeight(),bitmap.getWidth(),imCrop.type());

                perspectiveTransform = Imgproc.getPerspectiveTransform(src_mat, endM);
                Imgproc.warpPerspective(imCrop,
                        outputMat,
                        perspectiveTransform,
                        new Size(outputMat.width(), outputMat.height()),
                        Imgproc.INTER_CUBIC);

                Log.d("BitmapProcess", "perspectiveTransform " + perspectiveTransform.toString());


                //Imgproc.warpAffine(imCrop,outputMat,M,new Size(imCrop.width(), imCrop.height()), Imgproc.INTER_CUBIC);

//            Imgproc.warpPerspective(imCrop,
//                    outputMat,
//                    M,
//                    new Size(imCrop.width(), imCrop.height()),
//                    Imgproc.INTER_CUBIC);

                Imgproc.circle(outputMat, maxxlDown, 10, new Scalar(255,0,0), 4);     // 0,  0
                Imgproc.circle(outputMat, maxxrDown, 10, new Scalar(255,0,0), 4); // MAX 0
                Imgproc.circle(outputMat, maxxrUp, 10, new Scalar(255,0,0), 4);   // MAX MAX
                Imgproc.circle(outputMat, maxxlUp, 10, new Scalar(255,0,0), 4);   // 0   MAX


                Imgproc.circle(outputMat, points[0], 10, new Scalar(255,255,0), 4);     // 0,  0
                Imgproc.circle(outputMat, points[1], 10, new Scalar(255,255,0), 4); // MAX 0
                Imgproc.circle(outputMat, points[2], 10, new Scalar(255, 255, 0), 4);   // MAX MAX
                Imgproc.circle(outputMat, points[3], 10, new Scalar(255, 255, 0), 4);   // 0   MAX


//
//                Point[] pointArr = points.clone();
//                MatOfPoint2f pointMtx = new MatOfPoint2f(pointArr);
//                Mat pp3 = new Mat();
//                pp3.convertTo(pp3,CvType.CV_32FC2);
//                Core.transform(pointMtx, pp3, perspectiveTransform);
                //List<Point> pointMtxArr = new ArrayList<>();


                // List<Point> pointMtxArr = new ArrayList<Point>();
                // Converters.Mat_to_vector_Point(pp3, pointMtxArr);
                // Converters.Mat_to_vector_Point

//                for (int i = 0; i < pp3.cols() ; i++)
//                {
//                    for (int j = 0 ; j < pp3.rows(); j++)
//                    {
//
//                        Log.d("BitmapProcess", "PP3 : " + pp3.get(i,j));
//
//                    }
//                }

//
//                for(int i =0; i< pointMtxArr.size(); i++)
//                {
//
//
//                    Imgproc.circle(outputMat, pointMtxArr.get(i), 10, new Scalar(0, 255, 0), 5);   // 0   MAX
//
//
//                }



                rBitmap3 = Bitmap.createBitmap(outputMat.width(), outputMat.height(), bBitmap.getConfig());
//            rBitmap3 = rBitmap2.copy(rBitmap2.getConfig(), true);
                Utils.matToBitmap(outputMat, rBitmap3);
                SaveFile(rBitmap3, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "POSSIBLE_ROTATED_III"));

            }
            catch (Exception e)
            {
                Log.d("BitmapProcess", "POSSIBLE_ROTATED_III ERROR : " + e.getLocalizedMessage(),e);

                e.printStackTrace();
            }

            // ***********************
            try {
                perspectiveTransform = Imgproc.getPerspectiveTransform(endM, src_mat);

                outputMat = imCrop;

                Imgproc.warpPerspective(imCrop,
                        outputMat,
                        perspectiveTransform,
                        new Size(imCrop.width(), imCrop.height()),
                        Imgproc.INTER_CUBIC);

                rBitmap2 = Bitmap.createBitmap(rec.height, rec.width, bBitmap.getConfig());

                rBitmap3 = rBitmap2.copy(rBitmap2.getConfig(), true);
                Utils.matToBitmap(outputMat, rBitmap3);
                SaveFile(rBitmap3, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "POSSIBLE_ROTATED_IIII"));

            }
            catch (Exception e)
            {
                Log.d("BitmapProcess", "POSSIBLE_ROTATED_IIII ERROR");
                e.printStackTrace();
            }
            // ***********************


            // ***********************
            try {
                perspectiveTransform = Imgproc.getPerspectiveTransform(endM, src_mat);
                outputMat = imCrop;
                Imgproc.warpPerspective(imCrop,
                        outputMat,
                        perspectiveTransform,
                        new Size(imCrop.height(),imCrop.width()),
                        Imgproc.INTER_CUBIC);

                rBitmap2 = Bitmap.createBitmap(rec.width, rec.height, bBitmap.getConfig());

                rBitmap3 = rBitmap2.copy(rBitmap2.getConfig(), true);
                Utils.matToBitmap(outputMat, rBitmap3);
                SaveFile(rBitmap3, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "POSSIBLE_ROTATED_IIIII"));

            }
            catch (Exception e)
            {
                Log.d("BitmapProcess", "POSSIBLE_ROTATED_IIIII ERROR");
                e.printStackTrace();
            }
            // ***********************

            // ***********************
            try {
                perspectiveTransform = Imgproc.getPerspectiveTransform(endM, src_mat);
                outputMat = imCrop;
                Imgproc.warpPerspective(imCrop,
                        outputMat,
                        perspectiveTransform,
                        new Size(imCrop.height(),imCrop.width()),
                        Imgproc.INTER_CUBIC);

                rBitmap2 = Bitmap.createBitmap(rec.height, rec.width, bBitmap.getConfig());

                rBitmap3 = rBitmap2.copy(rBitmap2.getConfig(), true);
                Utils.matToBitmap(outputMat, rBitmap3);
                SaveFile(rBitmap3, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "POSSIBLE_ROTATED_IIIIII"));

            }
            catch (Exception e)
            {
                Log.d("BitmapProcess", "POSSIBLE_ROTATED_IIIIII ERROR");
                e.printStackTrace();
            }
            // ***********************

            SaveFile(bitmap, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "POSSIBLE_NON_ROTATED"));



            Mat cropped = new Mat();

//            Imgproc.getRectSubPix(outputMat,new Size(10,10),new Point(100,100),cropped);
//
//            Imgproc.getRectSubPix(outputMat,minRect.size,minRect.center,cropped);
//
//
//            rBitmap3 = Bitmap.createBitmap(cropped.width(), cropped.height(), bBitmap.getConfig());
//            Utils.matToBitmap(cropped, rBitmap3);
//            SaveFile(rBitmap3, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "FINALL"));
//            Mat mat5= outputMat.clone();
//
//
//
//            Imgproc.GaussianBlur(mat5, mat5, new Size(3, 3), 0);
//            Imgproc.threshold(mat5, mat5, 0, 255, Imgproc.THRESH_OTSU);
//            Imgproc.threshold(mat5, mat5, 0, 255, Imgproc.THRESH_BINARY_INV);
//            bitmap4 = Bitmap.createBitmap(mat5.width(), mat5.height(), bBitmap.getConfig());
//
//            Utils.matToBitmap(mat5, bitmap4);
//            // Utils.bitmapToMat(bitmap4, mat2);
//
//            SaveFile(bitmap4, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "THRESHOLDED_FINAL_IMAGE"));
//
//            biggestCountour = getBiggestCountour(mat5);
//            contours.clear();
//            contours.add(biggestCountour);
//
//
//            Mat mat6 = new Mat(bitmap4.getHeight(), bitmap4.getWidth(), CvType.CV_8UC1);
//
//            Imgproc.drawContours(mat6, contours, 0, new Scalar(255, 255, 255));
//            Utils.matToBitmap(mat6, bitmap4);
//            SaveFile(bitmap4, new File("/sdcard/debug", String.valueOf(System.currentTimeMillis()) + "FINAL_BIGGEST_CONTOUR"));
//
//
//            Mat mat7 = new Mat(bitmap4.getHeight(), bitmap4.getWidth(), CvType.CV_8UC1);
//            mLines = new Mat();
//
//            biggestCountourArray = contours.get(0).toArray();
//
//            NewMtx = new MatOfPoint2f(biggestCountourArray);
//
//            //    Imgproc.
//            minRect = Imgproc.minAreaRect(NewMtx);
//
//
//            minRect.boundingRect()
//
//            Point[] points = new Point[4];
//            minRect.points(points);



//
//
//            mat = new Mat(rBitmap2.getHeight(), rBitmap2.getWidth(), CvType.CV_8UC1);
//            Utils.bitmapToMat(rBitmap2, mat);
//
//            try {
//
//                MatOfPoint biggestCountourInner = getBiggestCountour(mat);
//
//                rec = Imgproc.boundingRect(biggestCountourInner);
//                Log.d("WRITE_CONTOUR",rec.toString());
//                imCrop = new Mat(imCrop, rec);
//            }
//            catch(Exception e)
//            {
//                Log.e("WRITE_CONTOUR_ERROR",e.getLocalizedMessage());
//            }
            //  contours = new ArrayList<MatOfPoint>(1);
            //   contours.clear();
            //   contours.add(0, biggestCountourInner);
            //   Imgproc.drawContours(mat, contours, 0, new Scalar(255, 0, 0),4);


//
//            rBitmap2 = Bitmap.createBitmap(rec.width, rec.height, bBitmap.getConfig());
//            Utils.matToBitmap(imCrop, rBitmap2);





            return rBitmap3;
        }


        MatOfPoint bContour = new MatOfPoint();
        double maxSize = 0;
        int bCountourId = 0;
        for(int i=0; i< contours.size();i++){
//            peri = cv2.arcLength(c, True)
//            approx = cv2.approxPolyDP(c, 0.02
            /// Source variable
            MatOfPoint SrcMtx;
            HashMap<MatOfPoint, Double> shapeMap = new HashMap<MatOfPoint, Double>();
            /// New variable
            MatOfPoint2f NewMtx = new MatOfPoint2f( contours.get(i).toArray() );

            double peri = Imgproc.arcLength(NewMtx, true);
            MatOfPoint2f DstMtx = NewMtx;
            Imgproc.approxPolyDP(NewMtx, DstMtx, 0.02 * peri, true);
            double area = Imgproc.contourArea(contours.get(i));
            // Log.i("CONT",DstMtx.cols() + " rows " + DstMtx.rows());
            if(DstMtx.rows() == 4) {
                shapeMap.put(contours.get(i), area);
                if (maxSize < area) {
                    maxSize = area;
                    bContour = contours.get(i);
                    bCountourId = i;
                }
            }

        }

        //  mat2.mul(bCoutnour);



        if(!bContour.empty())
        {
//            Imgproc.drawContours(mat2,bContour,1,new Scalar(0,255,255));
            Imgproc.drawContours(mat,contours,bCountourId,new Scalar(0,255,255));
            bCoutnour = contours.get(bCountourId);
//            Rect rect = Imgproc.boundingRect(contours.get(i));
        }
        Utils.matToBitmap(mat, rBitmap);
        return rBitmap;
    }

    public static Mat warp(Mat inputMat, Mat startM) {

        int resultWidth = 1200;
        int resultHeight = 680;

        Point ocvPOut4 = new Point(0, 0);
        Point ocvPOut1 = new Point(0, resultHeight);
        Point ocvPOut2 = new Point(resultWidth, resultHeight);
        Point ocvPOut3 = new Point(resultWidth, 0);

        if (inputMat.height() > inputMat.width()) {
            // int temp = resultWidth;
            // resultWidth = resultHeight;
            // resultHeight = temp;

            ocvPOut3 = new Point(0, 0);
            ocvPOut4 = new Point(0, resultHeight);
            ocvPOut1 = new Point(resultWidth, resultHeight);
            ocvPOut2 = new Point(resultWidth, 0);
        }

        Mat outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC4);

        List<Point> dest = new ArrayList<Point>();
        dest.add(ocvPOut1);
        dest.add(ocvPOut2);
        dest.add(ocvPOut3);
        dest.add(ocvPOut4);

        Mat endM = Converters.vector_Point2f_to_Mat(dest);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);

        Imgproc.warpPerspective(inputMat, outputMat, perspectiveTransform, new Size(resultWidth, resultHeight), Imgproc.INTER_CUBIC);

        return outputMat;
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
