package com.androidimageprocessing;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;



import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;

import View.MainView;
import renderer.MainRenderer;

//public class MainActivity extends Activity{
//
//    private SurfaceView mSurfaceView;
//    private MainView mView;
//    private SurfaceHolder holder;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
//    }
//}

public class MainActivity extends Activity implements SurfaceHolder.Callback{

    private SurfaceView mSurfaceView;
    private SurfaceTexture mSurfaceTexture;     // Texture view dla openGL
    private TextureView mSurfaceTextureView;    // Widok tekstury
    private MainView mView;
    private SurfaceHolder mSurfaceHolder;
    private Button mCameraButton;
    private boolean mCameraState;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private String mCameraID;
    private Size mPreviewSize = new Size ( 1920, 1080 );
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private Handler mBackgroundHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
    //    mSurfaceTexture = ()
        mCameraButton = (Button) findViewById(R.id.button);
        mSurfaceHolder = mSurfaceView .getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mSurfaceTextureView = (TextureView) findViewById(R.id.textureView);
        mSurfaceTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mSurfaceTexture = surface;


                try {
                    CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    String cameraId = cm.getCameraIdList()[0];
                    CameraCharacteristics cc = cm.getCameraCharacteristics(cameraId);
                    StreamConfigurationMap streamConfigs = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    Size[] rawSizes = streamConfigs.getOutputSizes(ImageFormat.RAW_SENSOR);
                    Size[] jpegSizes = streamConfigs.getOutputSizes(ImageFormat.JPEG);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }


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
//        mSurfaceTextureHolder = mSurfaceTexture.getHolder();
//        mSurfaceTextureHolder.addCallback(this);
//        mSurfaceTextureHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Opening/closing camera
                if (!mCameraState)
                {
                    //Point ss = new Point();
                    //mSurfaceView.getDisplay().getRealSize(ss);
                    //mSurfaceView.getHeight();
                    //System.out.println("Displaysize: " + mSurfaceView.getWidth() + " : " + mSurfaceView.getHeight());
                    //cacPreviewSize(ss.x, ss.y);
                    cacPreviewSize(mSurfaceView.getWidth(), mSurfaceView.getHeight());
                    openCamera();
                }
                else
                {
                    closeCamera();
                    mSurfaceView.getHolder().getSurfaceFrame().setEmpty();
                }
                mCameraState = !mCameraState;
            }

        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    void cacPreviewSize( final int width, final int height ) {
        CameraManager manager = (CameraManager)mSurfaceView.getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraID : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
                // Checking if front camera skip then
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                    continue;

                mCameraID = cameraID;
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                for ( Size psize : map.getOutputSizes(SurfaceTexture.class)) {
                    //System.out.println("Rozmiar : " + psize.getWidth() + "\nwidth : " + width + "\nheight : " + height);
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

    void openCamera() {
        CameraManager manager = (CameraManager)mSurfaceView.getContext().getSystemService(Context.CAMERA_SERVICE);
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


    private void closeCamera() {
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
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }


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


    private void createCameraPreviewSession() {
        try {
          //  mSTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

         //   Surface surface = new Surface(mSTexture);

            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(mSurfaceView.getHolder().getSurface());

            mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceView.getHolder().getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        private void process (CaptureResult result)
                        {
                            System.out.println("Processsssss");

                        }
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (null == mCameraDevice)
                                return;

                            mCaptureSession = cameraCaptureSession;
                            try {
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                Log.e("mr", "createCaptureSession");
                            }
                        }
                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                        }

                    }, null
            );
        } catch (CameraAccessException e) {
            Log.e("mr", "createCameraPreviewSession");
        }
    }

}












//public class My extends Activity {
//    private MainView mView;
//
//    @Override
//    public void onCreate ( Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        int ui = getWindow().getDecorView().getSystemUiVisibility();
//        //ui = ui | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
//        getWindow().getDecorView().setSystemUiVisibility(ui);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        mView = new MainView(this);
//        setContentView ( mView );
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        mView.onResume();
//    }
//
//    @Override
//    protected void onPause() {
//        mView.onPause();
//        super.onPause();
//    }
//}