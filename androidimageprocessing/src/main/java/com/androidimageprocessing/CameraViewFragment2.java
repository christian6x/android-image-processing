package com.androidimageprocessing;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
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
import android.text.Editable;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Krystian on 22.12.2015.
 */
public class CameraViewFragment2 extends Fragment {

    private static final String TAG = "DDDD";
    private TextureView mSurfaceTextureView;
    private TextureView mBitmapTextureView;
    // Identyfikator "tej" kamery
    private String mCameraID;
    // Rozmiar podglądu "tej" kamery
    private Size mPreviewSize;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private SurfaceTexture mSurfaceTexture;
    private SurfaceTexture mBitmapSurfaceTexture;
    private Surface surface;
    private Handler mBackgroundHandler;
    private ImageReader mCaptureBuffer;
    private HandlerThread mBackgroundThread;
    private static RenderingThread mThread;
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

    public interface CustomRunnable<T> {
        void run(T arg1);
    }

    private static Map<String, CustomRunnable> updateState = (Map<String, CustomRunnable>) new HashMap<String,CustomRunnable>();
    static {
        updateState.put("DILATE", new CustomRunnable<Boolean>() {
            @Override
            public void run(Boolean arg1) {
                mThread.mDilate = arg1;
            }

        });
        updateState.put("RESIZE", new CustomRunnable<Boolean>() {
            @Override
            public void run(Boolean arg1) {
                mThread.mResize = arg1;
            }
        });
        updateState.put("NORMALIZE", new CustomRunnable<Boolean>() {
            @Override
            public void run(Boolean arg1) {
                mThread.mNormalize = arg1;
            }
        });

        updateState.put("CANNY", new CustomRunnable<Boolean>() {
            @Override
            public void run(Boolean arg1) {
                mThread.mCanny = arg1;
            }
        });

        updateState.put("SOBEL", new CustomRunnable<Boolean>() {
            @Override
            public void run(Boolean arg1) {
                mThread.mSobel = arg1;
            }
        });

        updateState.put("GAUSS", new CustomRunnable<Boolean>() {
            @Override
            public void run(Boolean arg1) {
                mThread.mGauss = arg1;
            }
        });

        updateState.put("GRAY", new CustomRunnable<Boolean>() {
            @Override
            public void run(Boolean arg1) {
                mThread.mGray = arg1;
            }
        });

        updateState.put("MCANNYMIN", new CustomRunnable<Integer>() {
            @Override
            public void run(Integer arg1) {
                mThread.mCannyMin = arg1;
            }
        });

        updateState.put("MCANNYMAX", new CustomRunnable<Integer>() {
            @Override
            public void run(Integer arg1) {
                mThread.mCannyMax = arg1;
            }
        });

        updateState.put("MCANNYOPT", new CustomRunnable<Integer>() {
            @Override
            public void run(Integer arg1) {
                mThread.mCannyOpt = arg1;
            }
        });


    }



    protected volatile boolean mResize = false;
    protected volatile boolean mNormalize = false;
    protected volatile boolean mDilate = false;
    protected volatile boolean mCanny = false;
    protected volatile boolean mSobel = false;



    public void runUpdateState(String mode, boolean state)
    {
        if (mCaptureSession != null)
            updateState.get(mode).run(state);
    }


    public void runUpdateState(String mode, double state)
    {
        if (mCaptureSession != null)
            updateState.get(mode).run(state);
    }

    public void runUpdateState(String mode, int state)
    {
        if (mCaptureSession != null)
            updateState.get(mode).run(state);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camera_view_frame_fragment,
                container, false);
        // Pobieram widok textury
        mBackgroundThread = new HandlerThread("background");
        mBackgroundThread.start();

        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        mBitmapTextureView = (TextureView) view.findViewById(R.id.textureView3);
        mBitmapTextureView.setWillNotDraw(false);
        mBitmapTextureView.setOpaque(false);
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
        mSurfaceTextureView.setOpaque(false);
       // mSurfaceTextureView.setAlpha(0.0f);
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
        Log.i("RUN","PREVIEWSIZE " + mSurfaceTextureView.getWidth() + " : " + mSurfaceTextureView.getHeight());
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

                mCaptureBuffer = ImageReader.newInstance(mSurfaceTextureView.getWidth(),
                        mSurfaceTextureView.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
                //   mCaptureBuffer.setOnImageAvailableListener(
                //           mImageCaptureListener, mBackgroundHandler);
                mCameraID = cameraID;
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                for ( Size psize : map.getOutputSizes(SurfaceTexture.class)) {
                    System.out.println("Rozmiar : " + psize.getWidth() + "width : " + width + "height : " + height);
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

    private final SurfaceTexture.OnFrameAvailableListener mFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener(){

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
        protected volatile int downScaleWidth = 480;
        protected volatile int downScaleHeight = 480;
        protected volatile boolean mResize = false;
        protected volatile boolean mNormalize = false;
        protected volatile boolean mDilate = false;
        protected volatile boolean mCanny = false;
        protected volatile boolean mSobel = false;
        protected volatile boolean mGauss = false;
        protected volatile boolean mGray = false;
        protected volatile double mCannyMin = 0.0;
        protected volatile double mCannyMax = 0.0;
        protected volatile int mCannyOpt = 0;


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

        private double GetColorDistance(int c1, int c2)
        {
            int db=Color.blue(c1)-Color.blue(c2);
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
            Imgproc.GaussianBlur(mat, mat,new org.opencv.core.Size(45,45), 0);
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

        @Override
        public void run() {
            float x = 0.0f;
            float y = 0.0f;
            float speedX = 5.0f;
            float speedY = 3.0f;

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
                            prevTime = currentTime;
                        }
                        //      Mat buf = new Mat(WorkableImage.height, WorkableImage.width, CvType.CV_8UC1);
                        //      buf.put(0, 0, WorkableImage.byteBuffer);

                        Bitmap bitmap = BitmapFactory.decodeByteArray(WorkableImage.byteBuffer, 0, WorkableImage.byteBuffer.length);
                        bitmap = rotate(bitmap, 90);
                        if (mResize) {
                            bitmap = Bitmap.createScaledBitmap(bitmap, downScaleWidth, downScaleHeight, false);
                        }



                        if (mNormalize)
                            bitmap = OpenCVNormalize(bitmap);

                        if (mGray)
                            bitmap = toGrayscale(bitmap);


                        //bitmap = GetBinaryBitmap(bitmap);


                        bitmap = OpenCVDetectSquares(bitmap);

                        if (mGauss)
                            bitmap = OpenCVGauss(bitmap);

                        if (mSobel)
                            bitmap = OpenCVSobel(bitmap);


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
                                bitmap = Bitmap.createScaledBitmap(bitmap, mSurfaceTextureView.getWidth(), mSurfaceTextureView.getHeight(), false);
                            }
                            bitmap.prepareToDraw();

                            int allocationMap = bitmap.getAllocationByteCount();
                            Log.i("RUN", "IAM DIFFERENT ALLOCATION MAP" + allocationMap);
                            Log.i("RUN", "BITMAP" + bitmap.getHeight() + " : " + bitmap.getWidth());
                            int canvasDensity = canvas.getDensity();
                            Log.i("RUN", "IAM DIFFERENT CANVAS DENSITY MAP" + canvasDensity);
                          //  canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint);

                      //      bitmap.
                          //  Bitmap img = BitmapFactory.decodeResource(this.getResources(), drawable.testimage);
//                            Paint paint2 = new Paint();
//
//                            ColorMatrix cm = new ColorMatrix();
//                            float a = 77f;
//                            float b = 151f;
//                            float c = 28f;
//                            float t = 120 * -256f;
//                            cm.set(new float[] { a, b, c, 0, t, a, b, c, 0, t, a, b, c, 0, t, 0, 0, 0, 1, 0 });
//                            paint.setColorFilter(new ColorMatrixColorFilter(cm));
                      //      canvas.drawPaint(paint);
                            canvas.drawBitmap(bitmap, 0, 0, paint);



                            canvas.drawText("TPS: ", 0, 0,x + 80.0f, y + 80.0f, paint);

                           // canvas.drawRect(x, y, x + 20.0f, y + 20.0f, paint);



                            // canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR);
                           // canvas.d
                         //    canvas.drawRect(x, y, x + 220.0f, y + 220.0f, paint);
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
