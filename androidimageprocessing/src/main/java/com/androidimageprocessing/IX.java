package com.androidimageprocessing;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.media.Image;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

/**
 * Created by Krystian on 09.01.2016.
 */
public class IX
{
    private Matrix transformMatrix;
    protected byte[] byteBuffer;
    protected int width = 0;
    protected int height = 0;
    protected Image imageCopy;
    protected Bitmap mBitmap;
    public IX(Image image)
    {

        this.mBitmap = createBitmapFromYUV420(image);

//        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//        byte[] data = new byte[buffer.remaining()];
//        buffer.get(data);
//        this.byteBuffer = data;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.imageCopy = image;
    }

    public IX(Image image, Matrix mCurrentTransformMatrix) {
        this.mBitmap = createBitmapFromYUV420(image);
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.imageCopy = image;
        this.transformMatrix = mCurrentTransformMatrix;
    }

    public static Bitmap createBitmapFromYUV420(Image image) {
        Image.Plane[] planes = image.getPlanes();
        byte[] imageData = new byte[image.getWidth() * image.getHeight() * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
        ByteBuffer buffer = planes[0].getBuffer();
        int lastIndex = buffer.remaining();
        buffer.get(imageData, 0, lastIndex);
        int pixelStride = planes[1].getPixelStride();

        for (int i = 1; i < planes.length; i++) {
            buffer = planes[i].getBuffer();
            byte[] planeData = new byte[buffer.remaining()];
            buffer.get(planeData);

            for (int j = 0; j < planeData.length; j += pixelStride) {
                imageData[lastIndex++] = planeData[j];
            }
        }

        Mat yuvMat = new Mat(image.getHeight() + image.getHeight() / 2, image.getWidth(), CvType.CV_8UC1);
        yuvMat.put(0, 0, imageData);

        Mat rgbMat = new Mat();
        Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV420p2RGBA);

        Bitmap bitmap = Bitmap.createBitmap(rgbMat.cols(), rgbMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgbMat, bitmap);

        return bitmap;
    }

    public Matrix getTransformMatrix() {
        return transformMatrix;
    }

    public void setTransformMatrix(Matrix transformMatrix) {
        this.transformMatrix = transformMatrix;
    }
}
