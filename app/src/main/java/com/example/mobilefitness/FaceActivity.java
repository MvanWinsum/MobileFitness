package com.example.mobilefitness;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
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
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.example.mobilefitness.Helper.GraphicOverlay;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import android.os.Handler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class FaceActivity extends AppCompatActivity {

    Handler handler = new Handler();
    Handler mBackgroundHandler;
    HandlerThread mBackgroundThread;

    private String cameraId;
    CameraDevice cameraDevice;
    CameraCaptureSession cameraCaptureSession;
    CaptureRequest.Builder captureRequestBuilder;

    private Size imageDimensions;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognition);


        // Start the initial runnable task by posting through the handler
        handler.post(runnableCode);
    }

    private final Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            Log.d("Handlers", "Called on main thread");
            // Repeat this the same runnable code block again another 2 seconds
            // 'this' is referencing the Runnable object
            takePicture();
            handler.postDelayed(this, 2000);
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
//            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));

            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getWindowManager().getDefaultDisplay().getRotation());

            ImageReader.OnImageAvailableListener readerListener = imageReader -> {
                Image image = imageReader.acquireLatestImage();

                Log.d("FaceActivity", "Image captured: " + image.toString());
                processFaceDetection(image);
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
    }


//    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
//        @Override
//        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
//            openCamera();
//        }
//
//        @Override
//        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
//
//        }
//
//        @Override
//        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
//            return false;
//        }
//
//        @Override
//        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
//
//        }
//    };

    private void openCamera() {
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);

        try {
            // Get the front camera
            cameraId = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            // Map it to a stream
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            assert map != null;
            imageDimensions = map.getOutputSizes(SurfaceTexture.class)[0];

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(FaceActivity.this, new String[]{Manifest.permission.CAMERA}, 101);
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
//                            updatePreview();
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

    private void updatePreview() {
        if (cameraDevice == null) {
            return;
        }

        try {
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        openCamera();
        startBackgroundThread();
//        if (textureView.isAvailable()) {
//        } else {
//            textureView.setSurfaceTextureListener(textureListener);
//        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(runnableCode);
        stopBackgroundThread();
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
        super.onStop();
    }

    private void processFaceDetection(Image image) {
        FaceDetectorOptions realTimeOpts =
                new FaceDetectorOptions.Builder()
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();

        InputImage faceImage = InputImage.fromMediaImage(image,270);
        FaceDetector faceDetector = FaceDetection.getClient(realTimeOpts);
        Log.d("FaceActivity", "Face Detector initialized: " + faceDetector.toString());
        Task<List<Face>> result =
                faceDetector.process(faceImage)
                        .addOnSuccessListener(
                                faces -> {
                                    Log.d("FaceActivity", "Faces:" + faces);
                                    faceDetector.close();
                                    Log.d("FaceActivity", "Face processed: " + faces.toArray().length);
                                    Toast.makeText(FaceActivity.this, faces.toArray().length + " Faces found!", Toast.LENGTH_SHORT).show();
                                })
                        .addOnFailureListener(
                                e -> {
                                    // Task failed with an exception
                                    Toast.makeText(
                                            FaceActivity.this,
                                            e.toString(),
                                            Toast.LENGTH_LONG).show();
                                    faceDetector.close();
                                    Log.d("FaceActivity", "Error happened: " + e.toString());
                                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(getApplicationContext(), "Sorry, permissions are required", Toast.LENGTH_LONG).show();
            }
        }
    }
}
