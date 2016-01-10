package com.androidimageprocessing;

import android.app.Activity;
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
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import org.opencv.core.*;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Krystian on 22.12.2015.
 */
public class CameraViewFragment2 extends Fragment {

    private static final String TAG2 = "DDDD";
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
    private ImageReader mPhotoBuffer;
    private HandlerThread mBackgroundThread;
    private static RenderingThread mThread;
    private PreviewThread mPreviewThread;

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "Camera2BasicFragment";

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

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

    private Handler mBackgroundHandler2;
    private HandlerThread mBackgroundThread2;

    private boolean mTakeShot = false;
    private ImageReader mImageReader;
    private int mState;


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

        updateState.put("MHEIGHT", new CustomRunnable<Integer>() {
            @Override
            public void run(Integer arg1) {
                mThread.downScaleHeight = arg1;
            }
        });

        updateState.put("MWIDTH", new CustomRunnable<Integer>() {
            @Override
            public void run(Integer arg1) {
                mThread.downScaleWidth = arg1;
            }
        });
        updateState.put("PROCESSLIST", new CustomRunnable<BitmapProcessQueue>() {
            @Override
            public void run(BitmapProcessQueue arg1) {
                mThread.mProcessList = arg1;
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

    public void runUpdateState(String mode, BitmapProcessQueue mProcessList) {
        if (mCaptureSession != null)
            updateState.get(mode).run(mProcessList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camera_view_frame_fragment,
                container, false);
        // Pobieram widok textury
        mBackgroundThread = new HandlerThread("background3");
        mBackgroundThread.start();

        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());


        mBackgroundThread2 = new HandlerThread("background2");
        mBackgroundThread2.start();

        mBackgroundHandler2 = new Handler(mBackgroundThread2.getLooper());




        mBitmapTextureView = (TextureView) view.findViewById(R.id.textureView3);
        mBitmapTextureView.setWillNotDraw(false);
        mBitmapTextureView.setOpaque(false);
        mBitmapTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
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


        mBitmapTextureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int xPos = (int) v.getX();
                int yPos = (int) v.getY();
                Log.i("BITMAP", "Click position. " + "X : " + xPos + " Y : " + yPos + " LEFT : " + v.getLeft() + " RIGHT : " + v.getRight() + " RAW X : " + event.getX() + "RAW Y : " + event.getY() + " TIME " );

                takePicture();



                return false;
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

    public void takePicture()
    {
        mTakeShot = true;
        lockFocus();
    }

    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void openCamera() {
        Log.i("RUN", "PREVIEWSIZE " + mSurfaceTextureView.getWidth() + " : " + mSurfaceTextureView.getHeight());
        cacPreviewSize(mSurfaceTextureView.getWidth(), mSurfaceTextureView.getHeight());
        startCameraPreview();
        mThread = new RenderingThread(mBitmapTextureView);
        mThread.start();



        //mPreviewThread = new PreviewThread(surface);
        //mPreviewThread.start();
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
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
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




                mCameraID = cameraID;
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);


                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());

                mPhotoBuffer = ImageReader.newInstance(largest.getWidth(),
                        largest.getHeight(), ImageFormat.JPEG, /*maxImages*/2);

                mPhotoBuffer.setOnImageAvailableListener(mImageCaptureListener2, mBackgroundHandler);

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




            //    mCaptureBuffer = ImageReader.newInstance(640, 480, ImageFormat.JPEG, /*maxImages*/2);
            // mPreviewRequestBuilder.addTarget(surface);
            //   mPreviewRequestBuilder.addTarget(mCaptureBuffer.getSurface());

            List<Surface> surfaces = new ArrayList<Surface>();

            surface = new Surface(mSurfaceTexture);
            // nie rysuje na surface, przetwarzam w wątku preview
            surfaces.add(surface);
            mCaptureBuffer = ImageReader.newInstance(mSurfaceTextureView.getWidth(),
                    mSurfaceTextureView.getHeight(), ImageFormat.JPEG, /*maxImages*/2);



            // mCaptureBuffer przechwytuje bitmapa i wysyła ją do wyświetlenia

            surfaces.add(mCaptureBuffer.getSurface());

        //    surfaces.add(mPhotoBuffer.getSurface());

            //mSurfaceTexture.setOnFrameAvailableListener(mFrameAvailableListener);

            mCaptureBuffer.setOnImageAvailableListener(mImageCaptureListener, mBackgroundHandler2);




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


    private CaptureRequest mPreviewRequest;
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
                    //        requestBuilder.addTarget(mPhotoBuffer.getSurface());
                            requestBuilder.addTarget(surface);

//                            CaptureRequest.Builder requestBuilder2 =
//                                    mCameraDevice.createCaptureRequest(mCameraDevice.TEMPLATE_PREVIEW);
//
//                            requestBuilder2.addTarget(mPhotoBuffer.getSurface());
//                            ArrayList<CaptureRequest> previewRequestList = new ArrayList<CaptureRequest>();
                            mPreviewRequest = requestBuilder.build();
//                            previewRequestList.add(previewRequest);
//                            previewRequestList.add(requestBuilder2.build());


   //                         requestBuilder.addTarget(mPhotoBuffer.getSurface());
                            // Start displaying preview images
                            try {
                                session.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                                        mBackgroundHandler2);

                          //      session.setRepeatingBurst(previewRequestList, null, mBackgroundHandler2);
                                // session.
                                Log.i(TAG, "Session successfully created.");
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
                    Log.i(TAG,"Session closed");
                    mCaptureSession = null;
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.e(TAG, "Configuration error on device '" + mCameraDevice.getId());
                }
            };

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


    final ImageReader.OnImageAvailableListener mImageCaptureListener2 =
            new ImageReader.OnImageAvailableListener() {

                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image img = reader.acquireLatestImage();
                  //  Log.i("667", String.valueOf(img.getWidth()));

                //    MatOfPoint biggestCountour = mThread.getBiggestCountour();
                    img.close();
              //      Log.i("987","reader");
                    if (mTakeShot) {

                        mTakeShot = false;
                    }
                }
            };


    final ImageReader.OnImageAvailableListener mImageCaptureListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    // Save the image once we get a chance
                    try {
                        Image image = reader.acquireNextImage();

                        //      ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        //      byte[] data = new byte[buffer.remaining()];
                        //      buffer.get(data);
                        //      image.
                        //      Log.i("SSSS", String.valueOf(image.getWidth()));

                        mThread.WorkableImage = new IX(image);

                        // mPreviewThread.WorkableImage = new IX(image);
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

//    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
//        @Override
//        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
//                                        CaptureResult partialResult) {
//            Log.i(TAG,"ONCAPTUREPROGRESS");
//        }
//
//        @Override
//        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
//                                       TotalCaptureResult result) {
//            Log.i(TAG,"ONCAPTURECOMPLETED");
//        }
//    };
private void captureStillPicture() {
    try {
        final Activity activity = getActivity();
        if (null == activity || null == mCameraDevice) {
            return;
        }
        // This is the CaptureRequest.Builder that we use to take a picture.
        final CaptureRequest.Builder captureBuilder =
                mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(mImageReader.getSurface());

        // Use the same AE and AF modes as the preview.
        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

        // Orientation
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

        CameraCaptureSession.CaptureCallback CaptureCallback
                = new CameraCaptureSession.CaptureCallback() {

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                           @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {
               // showToast("Saved: " + mFile);
              //  Log.d(TAG, mFile.toString());
                unlockFocus();
            }
        };

        mCaptureSession.stopRepeating();
        mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
    } catch (CameraAccessException e) {
        e.printStackTrace();
    }
}

    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private final CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
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
        public volatile MatOfPoint bContour;
        protected BitmapProcessQueue mProcessList = new BitmapProcessQueue();


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

        private MatOfPoint latestMatOfPoint = new MatOfPoint();

        public synchronized MatOfPoint getBiggestCountour()
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
                  //      Log.i("BITMAP","FRAME CT: " + currentTime + " FRAME PT: " + prevTime);
                        prevTime = currentTime;

                        //      Mat buf = new Mat(WorkableImage.height, WorkableImage.width, CvType.CV_8UC1);
                        //      buf.put(0, 0, WorkableImage.byteBuffer);

                         Bitmap bitmap = BitmapFactory.decodeByteArray(WorkableImage.byteBuffer, 0, WorkableImage.byteBuffer.length);
                    //    Bitmap bitmap = getBitmapImageFromYUV(WorkableImage.byteBuffer, WorkableImage.width, WorkableImage.height);

                    //    byte[] byteBuffer = convertN21ToJpeg(convertYUV420ToN21(WorkableImage.imageCopy), WorkableImage.width, WorkableImage.height);

                    //    Bitmap bitmap = BitmapFactory.decodeByteArray(byteBuffer, 0, byteBuffer.length);

                        bitmap = rotate(bitmap, 90);
                        if (mResize) {
                            bitmap = Bitmap.createScaledBitmap(bitmap, downScaleWidth, downScaleHeight, false);
                        }


                        //Log.i("QWERTY", String.valueOf(mProcessList.size()));
                        try {
                            BitmapMat bitMat = new BitmapMat(bitmap);
                            bitMat= mProcessList.process(bitMat);
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
                                bitmap = Bitmap.createScaledBitmap(bitmap, mSurfaceTextureView.getWidth(), mSurfaceTextureView.getHeight(), false);
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
    }
}
