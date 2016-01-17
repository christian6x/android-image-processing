package com.androidimageprocessing;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
//import android.support.v7.app.AppCompatActivity;
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
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;


import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;

import View.MainView;
import renderer.MainRenderer;

//import static android.os.Debug.waitForDebugger;

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

@SuppressWarnings("ALL")
public class MainActivity extends Activity implements SurfaceHolder.Callback {

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
    private Size mPreviewSize = new Size(1920, 1080);
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private Handler mBackgroundHandler;
    private Surface surface;
    private Fragment mCameraFragment;
    private boolean mResizeSwitch;
    private boolean mNormalizeSwitch;
    private boolean mSobelSwitch;
    private boolean mCannySwitch;
    private boolean mDilateSwitch;
    private boolean mGaussianSwitch;
    private boolean mGrayscaleSwitch;
    private double mCannyMin;
    private double mCannyMax;
    private int mCannyOpt;
    private EditText mCannyEditMin;
    private EditText mCannyEditMax;
    private EditText mCannyEditOpt;
    private Button mCameraButton2;
    private EditText mHeight;
    private EditText mWidth;
    private BitmapProcessQueue mProcessList = new BitmapProcessQueue();
    private Button mClearButton;
    private SeekBar mThresholdSeekBar;
    private int mThresholdSeekValue;
    private int mHoughSeekValue;
    private SeekBar mHoughSeekBar;
    private int mLineHeight;
    private int mLineGap;
    private SeekBar mLineHeightBar;
    private SeekBar mLineGapBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //waitForDebugger();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        //getFragmentManager().beginTransaction().add(R.id.Fra, new CameraViewFragment()).commit();
        //  mCameraFragment = getFragmentManager().findFragmentById(R.layout.camera_view_fragment);
        //final CameraViewFragment mCameraFragment = (CameraViewFragment) getFragmentManager().findFragmentById(R.id.fragment);
        final CameraViewFragment3 mCameraFragment = (CameraViewFragment3) getFragmentManager().findFragmentById(R.id.fragment);
        //  fragment.<specific_function_name>();
        //    mSurfaceTexture = ()
        mCameraButton = (Button) findViewById(R.id.button);

        mCameraButton2 = (Button) findViewById(R.id.button2);

        mClearButton = (Button) findViewById(R.id.button7);

        mResizeSwitch = false;

        mHeight = (EditText) findViewById(R.id.editText);
        mWidth = (EditText) findViewById(R.id.editText);

        mCannyEditMin = (EditText) findViewById(R.id.editText3);
        mCannyEditMax = (EditText) findViewById(R.id.editText4);
        mCannyEditOpt = (EditText) findViewById(R.id.editText5);

        mThresholdSeekValue = 0;
        mThresholdSeekBar = (SeekBar) findViewById(R.id.seekBar);

        mHoughSeekValue = 0;
        mHoughSeekBar = (SeekBar) findViewById(R.id.seekBar2);

        mLineHeight = 0;
        mLineGap = 0;

        mLineHeightBar = (SeekBar) findViewById(R.id.seekBar3);
        mLineGapBar = (SeekBar) findViewById(R.id.seekBar4);

        mThresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mThresholdSeekValue = progress;
                drawOnButton((Button) findViewById(R.id.button10),"THRESHOLD" ,String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mHoughSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mHoughSeekValue = progress;
                drawOnButton((Button) findViewById(R.id.button11),"HOUGH" ,String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mLineHeightBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mLineHeight = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mLineGapBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mLineGap = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        findViewById(R.id.switch1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mResizeSwitch = !mResizeSwitch;
                mCameraFragment.runUpdateState("RESIZE", mResizeSwitch);
            }
        });



//        mNormalizeSwitch = false;
//
//        findViewById(R.id.switch2).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mNormalizeSwitch = !mNormalizeSwitch;
//                mCameraFragment.runUpdateState("NORMALIZE", mNormalizeSwitch);
//            }
//        });
//
//        mSobelSwitch = false;
//        findViewById(R.id.switch3).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mSobelSwitch = !mSobelSwitch;
//                mCameraFragment.runUpdateState("SOBEL", mSobelSwitch);
//            }
//        });
//
//        mCannySwitch = false;
//        findViewById(R.id.switch4).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mCannySwitch = !mCannySwitch;
//                mCameraFragment.runUpdateState("CANNY", mCannySwitch);
//            }
//        });
//
//        mDilateSwitch = false;
//        findViewById(R.id.switch5).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mDilateSwitch = !mDilateSwitch;
//                mCameraFragment.runUpdateState("DILATE", mDilateSwitch);
//            }
//        });
//
//        mGaussianSwitch = false;
//        findViewById(R.id.switch6).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mGaussianSwitch = !mGaussianSwitch;
//                mCameraFragment.runUpdateState("GAUSS", mGaussianSwitch);
//            }
//        });
//
//        mGrayscaleSwitch = false;
//        findViewById(R.id.switch7).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mGrayscaleSwitch = !mGrayscaleSwitch;
//                mCameraFragment.runUpdateState("GRAY", mGrayscaleSwitch);
//            }
//        });



//        mCameraButton2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                int val = Integer.parseInt(mCannyEditMin.getText().toString());
//                mCameraFragment.runUpdateState("MCANNYMIN", val);
//
//
//            }
//        });


        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProcessList.add(new BitmapProcessInterface() {
                    @Override
                    public Bitmap process(Bitmap bitmap) {
                        // bitmap = BitmapProcess.OpenCVNormalize(bitmap);
                        // BitmapProcess.normalize(bitmap);
                        return BitmapProcess.OpenCVNormalize(bitmap);
                    }

                    @Override
                    public BitmapMat process(BitmapMat bitmapMat) {
                        return bitmapMat;
                    }
                });
            }
        });

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProcessList.add(new BitmapProcessInterface() {
                    @Override
                    public Bitmap process(Bitmap bitmap) {
                        return BitmapProcess.OpenCVGray(bitmap);
                    }

                    @Override
                    public BitmapMat process(BitmapMat bitmapMat) {
                        return bitmapMat;
                    }
                });
            }
        });

        findViewById(R.id.button5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProcessList.add(new BitmapProcessInterface() {
                    @Override
                    public Bitmap process(Bitmap bitmap) {
                        return BitmapProcess.OpenCVSobel(bitmap);
                    }

                    @Override
                    public BitmapMat process(BitmapMat bitmapMat) {
                        return bitmapMat;
                    }
                });
            }
        });

        findViewById(R.id.button6).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProcessList.add(new BitmapProcessInterface() {
                    @Override
                    public Bitmap process(Bitmap bitmap) {
                        return BitmapProcess.OpenCVCanny(bitmap, (int) mCannyMin, (int) mCannyMax, (int) mCannyOpt);
                    }

                    @Override
                    public BitmapMat process(BitmapMat bitmapMat) {
                        return bitmapMat;
                    }
                });
            }
        });

        findViewById(R.id.button8).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProcessList.add(new BitmapProcessInterface() {
                    @Override
                    public Bitmap process(Bitmap bitmap) {
                        return BitmapProcess.OpenCVDilate(bitmap);
                    }

                    @Override
                    public BitmapMat process(BitmapMat bitmapMat) {
                        return bitmapMat;
                    }
                });
            }
        });

        findViewById(R.id.button9).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProcessList.add(new BitmapProcessInterface() {
                    @Override
                    public Bitmap process(Bitmap bitmap) {
                        return BitmapProcess.OpenCVMedian(bitmap);
                    }

                    @Override
                    public BitmapMat process(BitmapMat bitmapMat) {
                        return bitmapMat;
                    }
                });
            }
        });

        findViewById(R.id.button10).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProcessList.add(new BitmapProcessInterface() {
                    @Override
                    public Bitmap process(Bitmap bitmap) {
                        return BitmapProcess.OpenCVThreshold(bitmap, mThresholdSeekValue);
                    }

                    @Override
                    public BitmapMat process(BitmapMat bitmapMat) {
                        return bitmapMat;
                    }
                });
            }
        });



        findViewById(R.id.button11).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProcessList.add(new BitmapProcessInterface() {
                    @Override
                    public Bitmap process(Bitmap bitmap) {
                        return BitmapProcess.OpenCVHoughLines(bitmap, mHoughSeekValue, mLineHeight, mLineGap);
                    }

                    @Override
                    public BitmapMat process(BitmapMat bitmapMat) {
                        return bitmapMat;
                    }
                });
            }
        });

        findViewById(R.id.button12).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProcessList.add(new BitmapProcessInterface() {
                    @Override
                    public Bitmap process(Bitmap bitmap) {
                        return  bitmap;
                    }

                    @Override
                    public BitmapMat process(BitmapMat bitmapMat) {
                        return BitmapProcess.OpenCVFindCountours(bitmapMat);
                    }
                });
            }
        });


        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int val = 10;
//                mCannyEditMin.setText(val);
//                val = 100;
//                mCannyEditMax.setText(val);
//                val = 3;
//                mCannyEditOpt.setText(val);
//                val = 640;
//                mHeight.setText(val);
//                val = 480;
//                mWidth.setText(val);
                mProcessList.clear();
            }
        });

        mCannyMin = 0.0;
        mCameraButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int val = Integer.parseInt(mCannyEditMin.getText().toString());
                mCameraFragment.runUpdateState("MCANNYMIN", val);
                val = Integer.parseInt(mCannyEditMax.getText().toString());
                mCameraFragment.runUpdateState("MCANNYMAX", val);
                val = Integer.parseInt(mCannyEditOpt.getText().toString());
                mCameraFragment.runUpdateState("MCANNYOPT", val);
                val = Integer.parseInt(mHeight.getText().toString());
                mCameraFragment.runUpdateState("MHEIGHT", val);
                val = Integer.parseInt(mWidth.getText().toString());
                mCameraFragment.runUpdateState("MWIDTH", val);
                //
                BitmapProcessQueue ddf = new BitmapProcessQueue();
                ddf = (BitmapProcessQueue) mProcessList.clone();
                //
                mCameraFragment.runUpdateState("PROCESSLIST",ddf);
                int mSize = mProcessList.size();
                Log.i("SIZE", String.valueOf(mSize));
            }
        });


//        findViewById(R.id.switch7).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mCameraFragment.runUpdateState("MCANNYMIN", mCannyMin);
//            }
//        });

//        mCannyMax = 0.0;
//
//        mCannyEditMax.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                int val = Integer.parseInt(mCannyEditMax.getText().toString());
//
//                mCameraFragment.runUpdateState("MCANNYMAX", val);
//            }
//        });

//        findViewById(R.id.switch7).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mCameraFragment.runUpdateState("MCANNYMAX", mCannyMax);
//            }
//        });

//
//
//        mCannyOpt = 0;
//        mCannyEditOpt.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                int val = Integer.parseInt(mCannyEditOpt.getText().toString());
//                mCameraFragment.runUpdateState("MCANNYOPT", val);
//            }
//        });
//        findViewById(R.id.switch7).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mCameraFragment.runUpdateState("MCANNYOPT", mCannyOpt);
//            }
//        });


//        mSurfaceHolder = mSurfaceView .getHolder();
//        mSurfaceHolder.addCallback(this);
//        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

//        mSurfaceTextureView = (TextureView) findViewById(R.id.textureView);
//        mSurfaceTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
//            @Override
//            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//                mSurfaceTexture = surface;
//                try {
//                    CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//                    String cameraId = cm.getCameraIdList()[0];
//                    CameraCharacteristics cc = cm.getCameraCharacteristics(cameraId);
//                    StreamConfigurationMap streamConfigs = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//                    Size[] rawSizes = streamConfigs.getOutputSizes(ImageFormat.RAW_SENSOR);
//                    Size[] jpegSizes = streamConfigs.getOutputSizes(ImageFormat.JPEG);
//                }
//                catch(Exception e)
//                {
//                    e.printStackTrace();
//                }
//
//
//            }
//
//            @Override
//            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//
//            }
//
//            @Override
//            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//                return false;
//            }
//
//            @Override
//            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//
//            }
//        });
//        mSurfaceTextureHolder = mSurfaceTexture.getHolder();
//        mSurfaceTextureHolder.addCallback(this);
//        mSurfaceTextureHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


        //  mSurfaceTexture.setOnFrameAvailableListener(mFrameAvailableListener);

        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Opening/closing camera
                if (!mCameraState) {
                    //Point ss = new Point();
                    //mSurfaceView.getDisplay().getRealSize(ss);
                    //mSurfaceView.getHeight();
                    //System.out.println("Displaysize: " + mSurfaceView.getWidth() + " : " + mSurfaceView.getHeight());
                    //cacPreviewSize(ss.x, ss.y);
                    mCameraFragment.openCamera();
                    //cacPreviewSize(mSurfaceTextureView.getWidth(), mSurfaceTextureView.getHeight());
                    //openCamera();
                } else {
                    closeCamera();
                    // mSurfaceView.getHolder().getSurfaceFrame().setEmpty();
                }
                mCameraState = !mCameraState;
            }

        });
    }


    private void drawOnButton(Button button,String original ,String text)
    {
        String currentText = (String) button.getText();
        if(text.isEmpty())
        {
            button.setText(original);
        }
        else {
            button.setText(original + ":" + text);
        }

    }




    private OnFrameAvailableListener mFrameAvailableListener = new OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            Long imageTimestamp = Long.valueOf(surfaceTexture.getTimestamp());

        }
    };

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    void cacPreviewSize(final int width, final int height) {
        CameraManager manager = (CameraManager) mSurfaceTextureView.getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraID : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
                // Checking if front camera skip then
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                    continue;

                mCameraID = cameraID;
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                for (Size psize : map.getOutputSizes(SurfaceTexture.class)) {
                    //System.out.println("Rozmiar : " + psize.getWidth() + "\nwidth : " + width + "\nheight : " + height);
                    if (width == psize.getWidth() && height == psize.getHeight()) {
                        mPreviewSize = psize;
                        break;
                    }
                }
                break;
            }
        } catch (CameraAccessException e) {
            Log.e("mr", "cacPreviewSize - Camera Access Exception");
        } catch (IllegalArgumentException e) {
            Log.e("mr", "cacPreviewSize - Illegal Argument Exception");
        } catch (SecurityException e) {
            Log.e("mr", "cacPreviewSize - Security Exception");
        }
    }

    void openCamera() {
        CameraManager manager = (CameraManager) mSurfaceTextureView.getContext().getSystemService(Context.CAMERA_SERVICE);
        // System.out.println("Acquired manager");
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraID);
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {

                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            // mCameraID.
            manager.openCamera(mCameraID, mStateCallback, mBackgroundHandler);


        } catch (IllegalArgumentException e) {
            Log.e("mr", "OpenCamera - Illegal Argument Exception");
        } catch (SecurityException e) {
            Log.e("mr", "OpenCamera - Security Exception");
        } catch (InterruptedException e) {
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

    private final CaptureCallback mCaptureCallback = new CaptureCallback() {
        private void process(CaptureResult result) {
            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);

//            switch (aeState) {
//                case STATE_PREVIEW: {
//                    // Continuously taking pictures
//                    if(mCaptureSession != null)
//                        lockFocus();
//                    break;
//                }
//                case STATE_WAITING_LOCK: {
//                    int afState = result.get(CaptureResult.CONTROL_AF_STATE);
//                    if(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
//                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
//                        // CONTROL_AE_STATE can be null on some devices
//                        if(aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
//                            mState = STATE_WAITING_NON_PRECAPTURE;
//                            captureStillPicture();
//                        } else {
//                            runPrecaptureSequence();
//                        }
//                    }
//                    break;
//                }
//                case STATE_WAITING_PRECAPTURE: {
//                    // CONTROL_AE_STATE can be null on some devices
//                    if(aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
//                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
//                        mState = STATE_WAITING_NON_PRECAPTURE;
//                    }
//                    break;
//                }
//                case STATE_WAITING_NON_PRECAPTURE: {
//                    // CONTROL_AE_STATE can be null on some devices
//                    if(aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
//                        mState = STATE_PICTURE_TAKEN;
//                        captureStillPicture();
//                    }
//                    break;
//                }
//            }
            System.out.println("XNXX Processing : " + aeState);

        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                        CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            process(result);

            List<CaptureResult.Key<?>> rKeys = result.getKeys();
            Iterator<CaptureResult.Key<?>> it1 = rKeys.iterator();
            while (it1.hasNext()) {
                CaptureResult.Key<?> current = it1.next();
                System.out.println("XNXX TOTAL RESULT" + current.getName());
            }
        }
    };

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
            //mPreviewRequestBuilder.addTarget(mSurfaceTextureView.getSurfaceTexture().get);
            // mPreviewRequestBuilder.addTarget(mSurfaceView.getHolder().getSurface());
            surface = new Surface(mSurfaceTexture);
            mPreviewRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        private void process(CaptureResult result) {
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
                                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
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