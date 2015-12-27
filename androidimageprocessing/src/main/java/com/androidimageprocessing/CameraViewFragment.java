package com.androidimageprocessing;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import static android.graphics.SurfaceTexture.*;
import static android.hardware.camera2.CaptureRequest.*;

/**
 * Created by Krystian on 19.12.2015.
 */
public class CameraViewFragment extends Fragment {

    private static final String TAG = "DDDD";
    private TextureView mSurfaceTextureView;
    private TextureView mBitmapTextureView;
    // Identyfikator "tej" kamery
    private String mCameraID;
    // Rozmiar podglądu "tej" kamery
    private Size mPreviewSize;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CameraCaptureSession mCaptureSession;
    private Builder mPreviewRequestBuilder;
    private SurfaceTexture mSurfaceTexture;
    private SurfaceTexture mBitmapSurfaceTexture;
    private Surface surface;
    private Handler mBackgroundHandler;
    private ImageReader mCaptureBuffer;
    private HandlerThread mBackgroundThread;
    private RenderingThread mThread;
    private PreviewThread mPreviewThread;

    static {
        if(!OpenCVLoader.initDebug())
        {
            Log.i("OPENCV","Initialization failed");
        }
        else
        {
            Log.i("OPENCV","Initialization succeded");
        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camera_view_fragment,
                container, false);
        // Pobieram widok textury
        mBackgroundThread = new HandlerThread("background");
        mBackgroundThread.start();

        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        mBitmapTextureView = (TextureView) view.findViewById(R.id.textureView3);
        mBitmapTextureView.setWillNotDraw(false);
        mBitmapTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener()
        {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mBitmapSurfaceTexture = surface;
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

        mSurfaceTextureView = (TextureView) view.findViewById(R.id.textureView2);
        mSurfaceTextureView.setWillNotDraw(false);
        mSurfaceTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mSurfaceTexture = surface;

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
        return view;
    }

    public void openCamera()
    {
        cacPreviewSize(mSurfaceTextureView.getWidth(), mSurfaceTextureView.getHeight());
        startCameraPreview();
        mThread = new RenderingThread(mBitmapTextureView);
        mThread.start();

        mPreviewThread = new PreviewThread(surface);
        mPreviewThread.start();
    }

    public void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            mPreviewThread.stopRendering();
            mPreviewThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void startCameraPreview() {
        CameraManager manager = (CameraManager)mSurfaceTextureView.getContext().getSystemService(Context.CAMERA_SERVICE);
        // System.out.println("Acquired manager");
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraID);
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            // mCameraID.
            manager.openCamera(mCameraID,mStateCallback,mBackgroundHandler);
        } catch ( IllegalArgumentException e ) {
            Log.e("mr", "OpenCamera - Illegal Argument Exception");
        } catch ( SecurityException e ) {
            Log.e("mr", "OpenCamera - Security Exception");
        } catch ( InterruptedException e ) {
            Log.e("mr", "OpenCamera - Interrupted Exception");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    void cacPreviewSize( final int width, final int height ) {
        CameraManager manager = (CameraManager)mSurfaceTextureView.getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraID : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
                // Checking if front camera skip then
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                    continue;

                mCaptureBuffer = ImageReader.newInstance(1280,
                        720, ImageFormat.JPEG, /*maxImages*/2);
             //   mCaptureBuffer.setOnImageAvailableListener(
             //           mImageCaptureListener, mBackgroundHandler);
                mCameraID = cameraID;
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                for ( Size psize : map.getOutputSizes(SurfaceTexture.class)) {
                    System.out.println("Rozmiar : " + psize.getWidth() + "\nwidth : " + width + "\nheight : " + height);
                    if ( width == psize.getWidth() && height == psize.getHeight() ) {
                        mPreviewSize = psize;
                        break;
                    }
                }
                break;
            }
        } catch ( CameraAccessException e ) {
            Log.e("mr", "cacPreviewSize - Camera Access Exception");
        } catch ( IllegalArgumentException e ) {
            Log.e("mr", "cacPreviewSize - Illegal Argument Exception");
        } catch ( SecurityException e ) {
            Log.e("mr", "cacPreviewSize - Security Exception");
        }
    }
    private void createCameraPreviewSession() {
        try {
            //  mSTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            //   Surface surface = new Surface(mSTexture);

            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //mPreviewRequestBuilder.addTarget(mSurfaceTextureView.getSurfaceTexture().get);
            // mPreviewRequestBuilder.addTarget(mSurfaceView.getHolder().getSurface());
            surface = new Surface(mSurfaceTexture);

        //    mCaptureBuffer = ImageReader.newInstance(640, 480, ImageFormat.JPEG, /*maxImages*/2);
           // mPreviewRequestBuilder.addTarget(surface);
         //   mPreviewRequestBuilder.addTarget(mCaptureBuffer.getSurface());

            List<Surface> surfaces = new ArrayList<Surface>();
            // nie rysuje na surface, przetwarzam w wątku preview
             surfaces.add(surface);
            // mCaptureBuffer przechwytuje bitmapa i wysyła ją do wyświetlenia
            surfaces.add(mCaptureBuffer.getSurface());
            //mSurfaceTexture.setOnFrameAvailableListener(mFrameAvailableListener);
            mCaptureBuffer.setOnImageAvailableListener(
                    mImageCaptureListener, mBackgroundHandler);
            //mSurfaceTexture.setOnFrameAvailableListener(mImageCaptureListener2);
            //mSurfaceTexture.setOnFrameAvailableListener(mImageCaptureListener2, mBackgroundHandler);
            mCameraDevice.createCaptureSession(surfaces, mCaptureSessionListener, mBackgroundHandler);
            
           // mSurfaceTexture.
//            mCameraDevice.createCaptureSession(Arrays.asList(surface),
//                    new CameraCaptureSession.StateCallback() {
//                        @Override
//                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
//                            if (null == mCameraDevice)
//                                return;
//
//                            mCaptureSession = cameraCaptureSession;
//                            try {
//                                mPreviewRequestBuilder.set(CONTROL_AF_MODE, CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                                mPreviewRequestBuilder.set(CONTROL_AE_MODE, CONTROL_AE_MODE_ON_AUTO_FLASH);
//                                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
//                            } catch (CameraAccessException e) {
//                                Log.e("mr", "createCaptureSession");
//                            }
//                        }
//                        @Override
//                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
//                        }
//
//                    }, null
//            );
        } catch (CameraAccessException e) {
            Log.e("mr", "createCameraPreviewSession");
        }
    }

    private CameraDevice mCameraDevice;
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

    };


    /**
     * Callbacks invoked upon state changes in our {@code CameraCaptureSession}. <p>These are run on
     * {@code mBackgroundThread}.</p>
     */
    final CameraCaptureSession.StateCallback mCaptureSessionListener =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Log.i(TAG, "Finished configuring camera outputs");
                    mCaptureSession = session;

                    if (surface != null) {
                        try {
                            // Build a request for preview footage
                            CaptureRequest.Builder requestBuilder =
                                    mCameraDevice.createCaptureRequest(mCameraDevice.TEMPLATE_PREVIEW);

                            requestBuilder.addTarget(mCaptureBuffer.getSurface());
                            requestBuilder.addTarget(surface);

                            CaptureRequest previewRequest = requestBuilder.build();

                            // Start displaying preview images
                            try {
    //                            mCaptureSession.setRepeatingRequest(previewRequest, mCaptureCallback, mBackgroundHandler);
                                session.setRepeatingRequest(previewRequest, /*listener*/null,
                                /*handler*/null);
                            } catch (CameraAccessException ex) {
                                Log.e(TAG, "Failed to make repeating preview request", ex);
                            }
                        } catch (CameraAccessException ex) {
                            Log.e(TAG, "Failed to build preview request", ex);
                        }
                    }
                }
                @Override
                public void onClosed(CameraCaptureSession session) {
                    mCaptureSession = null;
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.e(TAG, "Configuration error on device '" + mCameraDevice.getId());
                }};

    public class IX
    {
        protected byte[] byteBuffer;
        protected int width = 0;
        protected int height = 0;
        protected Image imageCopy;
        public IX(Image image)
        {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            this.byteBuffer = data;
            this.width = image.getWidth();
            this.height = image.getHeight();
            this.imageCopy = image;
        }
    }

    final ImageReader.OnImageAvailableListener mImageCaptureListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    // Save the image once we get a chance
                    try {
                        Image image = mCaptureBuffer.acquireNextImage();


                  //      ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                  //      byte[] data = new byte[buffer.remaining()];
                  //      buffer.get(data);
                            //image.
                        mThread.WorkableImage = new IX(image);
                        mPreviewThread.WorkableImage = new IX(image);
//                        int width = image.getWidth();
//                        int height = image.getHeight();
//                        Bitmap btmp = mSurfaceTextureView.getBitmap();
//                        Paint p = new Paint(Color.RED);
                       // mSurfaceTexture.


                       // mSurfaceTextureView.setLayerPaint(p);
                       // mSurfaceTextureView.set
                     //   PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, width, height);


                    //    System.out.println("YYYY"   + image.getHeight() );
                        //Rect rect = image.getCropRect();
                        //rect.setEmpty();
                        //image.setCropRect(rect);

                        //.
                       // Image.Plane[] planes = image.getPlanes();
                        //    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        //     byte[] bytes = new byte[buffer.remaining()];
                        //   buffer.get(bytes);
                        image.close();
                        // mBackgroundHandler.post(new CapturedImageSaver(reader.acquireNextImage()));
                        // Control flow continues in CapturedImageSaver#run()
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }

                }};

    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                        CaptureResult partialResult) {
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {

        }
    };

    private final OnFrameAvailableListener mFrameAvailableListener = new OnFrameAvailableListener(){

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {

        }
    };
   // mSurfaceTexture.setOnFrameAvailableListener(mFrameAvailableListener);


    private class PreviewThread extends Thread
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
                        canvas.drawRect(x, y, x + 220.0f, y + 220.0f, paint);
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
                    Log.e("RENDERGINTHREAD",e.getLocalizedMessage());
                }
            }
        }

        void stopRendering() {
            interrupt();
            mRunning = false;
        }
    }

    private class RenderingThread extends Thread {
        private final TextureView mSurface;
        private volatile boolean mRunning = true;
        protected volatile Image frame;
        public byte[] byteBuffor;
        public IX WorkableImage;

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

        @Override
        public void run() {
            float x = 0.0f;
            float y = 0.0f;
            float speedX = 5.0f;
            float speedY = 3.0f;

            Paint paint = new Paint();
            paint.setColor(0xff00ff00);

            while (mRunning && !Thread.interrupted()) {
                try {


                    if (WorkableImage != null && WorkableImage.height != 0 && WorkableImage.width != 0 ) {
                  //      Mat buf = new Mat(WorkableImage.height, WorkableImage.width, CvType.CV_8UC1);
                    //      buf.put(0, 0, WorkableImage.byteBuffer);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(WorkableImage.byteBuffer,0,WorkableImage.byteBuffer.length);
                      //  Bitmap bbb = BitmapFactory.;
                      //  Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
                        Log.i("RUN", "IAM DIFFERENT" + bitmap.getHeight() + "Density : " + bitmap.getDensity());
// Do note that Highgui has been replaced by Imgcodecs for OpenCV 3.0 and above
                        //  Mat mat = Imgcodecs.imdecode(buf, Imgcodecs.CV_LOAD_IMAGE_COLOR);
                       //   mat.release();
                      //  buf.release();
                        //   Mat.put(frameArray)
                          MatOfByte inputframe = new MatOfByte(WorkableImage.byteBuffer);
                            // Mat mat = new Mat();
                        //   Mat.
                       // mat.put()
                          // mat.put(inputframe);
                          Mat mat = Imgcodecs.imdecode(inputframe, Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
                          Mat mat2 = mat;
                          Imgproc.Sobel(mat, mat2, CvType.CV_8U, 1, 1);

                        //Bitmap bmp = null;

                        try {
                            //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_RGB2BGRA);
                           // Imgproc.cv
                          //  Imgproc.cvtColor(mat2, mat, Imgproc.CO, 4);
                         //   bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(mat, bitmap);
                        }
                        catch (CvException e){Log.d("Exception",e.getMessage());}

                        Canvas canvas = mSurface.lockCanvas(null);
                        try {

                         //   Bitmap alphaBitmap = toGrayscale(bitmap);

                        //    bitmap = bmp;

                            bitmap.prepareToDraw();
                            int allocationMap = bitmap.getAllocationByteCount();
                            Log.i("RUN", "IAM DIFFERENT ALLOCATION MAP" + allocationMap);
                            int canvasDensity = canvas.getDensity();
                            Log.i("RUN", "IAM DIFFERENT CANVAS DENSITY MAP" + canvasDensity);
                            canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint);
                            canvas.drawRect(x, y, x + 20.0f, y + 20.0f, paint);

                            // canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR);
                            // canvas.drawRect(x, y, x + 20.0f, y + 20.0f, paint);
                        } finally {
                            mSurface.unlockCanvasAndPost(canvas);
                        }
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

                try
                {

                  //  Image image = mCaptureBuffer.acquireNextImage();
                  //  System.out.println("LLL" + image.getWidth());

                }
                catch (Exception e)
                {

                }
            }
        }

        void stopRendering() {
            interrupt();
            mRunning = false;
        }
    }
}
