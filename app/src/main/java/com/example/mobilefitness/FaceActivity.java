package com.example.mobilefitness;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.media.FaceDetector;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.camerakit.CameraKit;
import com.camerakit.CameraKitView;
import com.example.mobilefitness.Helper.GraphicOverlay;
import com.example.mobilefitness.Helper.RectOverlay;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.util.List;

import dmax.dialog.SpotsDialog;


public class FaceActivity extends AppCompatActivity {

    Button faceDetectButton;
    GraphicOverlay graphicOverlay;
    CameraKitView cameraKitView;
    AlertDialog alertDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognition);

        faceDetectButton = (Button)findViewById(R.id.capture_button);
        graphicOverlay = (GraphicOverlay)findViewById(R.id.graphic_overlay);
        cameraKitView = (CameraKitView)findViewById(R.id.camera_frame);

        alertDialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Please wait, Loading...")
                .setCancelable(false).build();
        cameraKitView.setFacing(CameraKit.FACING_FRONT);
        cameraKitView.startVideo();


        faceDetectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("Mobile Fitness", "Capture button clicked!");
                alertDialog.show();
                cameraKitView.captureImage(new CameraKitView.ImageCallback() {
                    @Override
                    public void onImage(CameraKitView cameraKitView, byte[] bytes) {
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        Bitmap mutableBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);
                        mutableBitmap = Bitmap.createScaledBitmap(mutableBitmap,
                                cameraKitView.getWidth(),
                                cameraKitView.getHeight(),
                                true
                        );

                        processFaceDetection(mutableBitmap);
                    }
                });
                cameraKitView.invalidate();
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        cameraKitView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraKitView.onResume();
    }

    @Override
    protected void onPause() {
        cameraKitView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        cameraKitView.onStop();
        super.onStop();
    }

    private void processFaceDetection(Bitmap bitmap) {
        FirebaseVisionImage fbvImage = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVisionFaceDetectorOptions fbvOptions = new FirebaseVisionFaceDetectorOptions
                .Builder().setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .enableTracking().build();

        FirebaseVisionFaceDetector fbvDetector = FirebaseVision.getInstance()
                .getVisionFaceDetector(fbvOptions);

        fbvDetector.detectInImage(fbvImage).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
            @Override
            public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                getFaceResults(firebaseVisionFaces);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(FaceActivity.this, "Error: " + e, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getFaceResults(List<FirebaseVisionFace> fvbFaces) {
        int counter = 0;
        for (FirebaseVisionFace face : fvbFaces) {
            Rect rect = face.getBoundingBox();
            RectOverlay rectOverlay = new RectOverlay(graphicOverlay, rect);
            counter++;
            graphicOverlay.add(rectOverlay);
            alertDialog.dismiss();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
