package com.example.mobilefitness;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.mobilefitness.Helper.FaceRecognition;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    DisplayMetrics dm;
    WindowManager windowManager;
    String cameraId;
    CameraDevice cameraDevice;

    GameView gameView;

    Handler handler = new Handler();
    Handler mBackgroundHandler;
    HandlerThread mBackgroundThread;

    CameraCaptureSession cameraCaptureSession;
    CaptureRequest.Builder captureRequestBuilder;

    FaceDetectorOptions realTimeOpts;
    InputImage faceImage;

    Boolean frameProcessed = true;
    FaceDetector faceDetector;

    private Size imageDimensions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        Constants.SCREEN_HEIGHT = dm.heightPixels;
        Constants.SCREEN_WIDTH = dm.widthPixels;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(GameActivity.this, new String[]{Manifest.permission.CAMERA}, 101);
            return;
        }

        gameView = new GameView(this);
        setContentView(gameView);

        openCamera();
        handler.post(runnableCode);
    }
    private final Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            if (frameProcessed) {
                frameProcessed = false;
                takePicture();
            }
            handler.postDelayed(this, 200);
        }
    };



    private void takePicture() {
        if (cameraDevice == null) {
            return;
        }
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            ImageReader reader = ImageReader.newInstance(imageDimensions.getWidth(), imageDimensions.getHeight(), ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());

            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getWindowManager().getDefaultDisplay().getRotation());

            ImageReader.OnImageAvailableListener readerListener = imageReader -> {
                Image image = imageReader.acquireLatestImage();

                Log.d("FaceActivity", "Image captured: " + image.toString());
                processFaceDetection(image);
                frameProcessed = true;
            };
            Log.d("Camera", "Capture Initiated");
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);

            final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.capture(captureBuilder.build(), captureCallback, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            },mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        frameProcessed = true;
    }


    private void openCamera() {
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);


         realTimeOpts =
                new FaceDetectorOptions.Builder()
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();

        faceDetector = FaceDetection.getClient(realTimeOpts);

        try {
            // Get the front camera
            cameraId = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            // Map it to a stream
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            assert map != null;
            imageDimensions = map.getOutputSizes(SurfaceTexture.class)[0];

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(GameActivity.this, new String[]{Manifest.permission.CAMERA}, 101);
                return;
            }

            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void createCameraPreview() {
        SurfaceTexture texture = new SurfaceTexture(1);
        texture.setDefaultBufferSize(
                imageDimensions.getWidth(),
                imageDimensions.getHeight()
        );
        Surface surface = new Surface(texture);

        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (cameraDevice == null) {
                                return;
                            }
                            cameraCaptureSession = session;
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(getApplicationContext(), "Configuration failed!", Toast.LENGTH_LONG).show();
                        }
                    }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        openCamera();
        startBackgroundThread();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(runnableCode);
        stopBackgroundThread();
        faceDetector.close();
        super.onPause();
    }

    protected void stopBackgroundThread() {
        try {
            mBackgroundThread.interrupt();
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(runnableCode);
        stopBackgroundThread();
        faceDetector.close();
        super.onStop();
    }

    private void processFaceDetection(Image image) {
        faceImage = InputImage.fromMediaImage(image,270);
        Task<List<Face>> result =
                faceDetector.process(faceImage)
                        .addOnSuccessListener(
                                faces -> {
                                    if (faces.toArray().length > 0) {
                                       if (faces.get(0).getBoundingBox().centerX() > 0 && faces.get(0).getBoundingBox().centerX() < 800)
                                           gameView.playerPoint.x = (5*(Constants.SCREEN_WIDTH/6));
                                       if (faces.get(0).getBoundingBox().centerX() > 800 && faces.get(0).getBoundingBox().centerX() < 1300)
                                           gameView.playerPoint.x = (Constants.SCREEN_WIDTH/2);
                                       if (faces.get(0).getBoundingBox().centerX() > 1300)
                                           gameView.playerPoint.x = (Constants.SCREEN_WIDTH/6);
                                    }
                                })
                        .addOnFailureListener(
                                e -> {
                                    // Task failed with an exception
                                    Toast.makeText(
                                            GameActivity.this,
                                            e.toString(),
                                            Toast.LENGTH_LONG).show();
                                    Log.d("FaceActivity", "Error happened: " + e.getMessage());
                                });
    }
}
