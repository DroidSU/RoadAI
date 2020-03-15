/*
 * Created by Sujoy Datta. Copyright (c) 2020. All rights reserved.
 *
 * To the person who is reading this..
 * When you finally understand how this works, please do explain it to me too at sujoydatta26@gmail.com
 * P.S.: In case you are planning to use this without mentioning me, you will be met with mean judgemental looks and sarcastic comments.
 */

package com.cooperativeai.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cooperativeai.R;
import com.cooperativeai.utils.Constants;
import com.cooperativeai.utils.SharedPreferenceManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CameraActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 11;
    private static final int WRITE_REQUEST_CODE = 21;

    private static final String TAG = "MainActivity";

    @BindView(R.id.textureView_camera_activity)
    TextureView textureView;
    @BindView(R.id.button_take_picture)
    ImageView buttonClick;

    private CameraManager cameraManager;
    private int cameraFacing;
    private Size previewSize;
    private String cameraId;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest captureRequest;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private File galleryFolder;
    private boolean hasWritePermission;
    private boolean wasCreated;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);

        getPermission(Constants.CAMERA_PERMISSION);

        hasWritePermission = false;
        wasCreated = false;

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraFacing = CameraCharacteristics.LENS_FACING_BACK;

        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                setupCamera();
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };

        int autoCaptureStatus = SharedPreferenceManager.getAutoCaptureStatus(CameraActivity.this);

        if (autoCaptureStatus == Constants.AUTO_CAPTURE_DISABLED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Turn on Auto?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(CameraActivity.this, "Auto Click Enabled Every 1 minute", Toast.LENGTH_SHORT).show();
                    dialogInterface.dismiss();
                    SharedPreferenceManager.setAutoCaptureStatus(CameraActivity.this, Constants.AUTO_CAPTURE_ON);
                    startAutoCapture();
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    SharedPreferenceManager.setAutoCaptureStatus(CameraActivity.this, Constants.AUTO_CAPTURE_OFF);
                    Toast.makeText(CameraActivity.this, "Auto capture turned off. Click to capture", Toast.LENGTH_LONG).show();
                }
            });
            builder.show();
        }
    }

    private void startAutoCapture() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                clickPicture();
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 3, Constants.AUTO_CAPTURE_DELAY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                if (cameraId != null) {
                    cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice cameraDevice) {
                            CameraActivity.this.cameraDevice = cameraDevice;
                            createPreviewSession();
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                            cameraDevice.close();
                            CameraActivity.this.cameraDevice = null;
                        }

                        @Override
                        public void onError(@NonNull CameraDevice cameraDevice, int i) {
                            cameraDevice.close();
                            CameraActivity.this.cameraDevice = null;
                        }
                    }, backgroundHandler);
                }
            } else {
                getPermission(Constants.WRITE_PERMISSION);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }

                            try {
                                captureRequest = captureRequestBuilder.build();
                                CameraActivity.this.cameraCaptureSession = cameraCaptureSession;
                                CameraActivity.this.cameraCaptureSession.setRepeatingRequest(captureRequest,
                                        null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void setupCamera() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == cameraFacing) {
                    StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (streamConfigurationMap != null) {
                        previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                        this.cameraId = cameraId;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("TAG", "setupCamera: " + e.getMessage());
            Toast.makeText(this, "Error occurred in setup", Toast.LENGTH_SHORT).show();
        }
    }

    private void getPermission(String writePermission) {
        if (writePermission.equalsIgnoreCase(Constants.CAMERA_PERMISSION)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else if (writePermission.equalsIgnoreCase(Constants.WRITE_PERMISSION)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_REQUEST_CODE);
            else
                hasWritePermission = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                clickPicture();
            }
        } else if (requestCode == WRITE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasWritePermission = true;
                takePicture();
            }
        }
    }

    private void createGalleryFolder() {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        galleryFolder = new File(storageDirectory, getResources().getString(R.string.app_name));
        if (!galleryFolder.exists()) {
            wasCreated = galleryFolder.mkdirs();
            if (!wasCreated) {
                Log.e(TAG, "createGalleryFolder: Folder could not be created");
            }
        } else
            wasCreated = true;
    }

    @OnClick(R.id.button_take_picture)
    public void clickPicture() {
        getPermission(Constants.WRITE_PERMISSION);
        if (hasWritePermission) {
            takePicture();
        } else {
            Toast.makeText(this, "Permissions were not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void takePicture() {
        createGalleryFolder();
        if (wasCreated) {
            lock();
            FileOutputStream fileOutputStream = null;
            try {
                File file = createImageFile();
                if (file != null) {
                    fileOutputStream = new FileOutputStream(file);
                    textureView.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            increaseCoinCount();
                        }
                    });
                    Log.i(TAG, "takePicture: " + file.getAbsolutePath());
                } else {
                    Toast.makeText(this, "File could not be created", Toast.LENGTH_SHORT).show();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                unlock();
                try {
                    if (fileOutputStream != null)
                        fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(this, "Folder could not be created", Toast.LENGTH_SHORT).show();
        }
    }

    private void increaseCoinCount() {
        if (SharedPreferenceManager.changeCoinCount(CameraActivity.this, "add", Constants.BASE_COIN_COUNT)){
            double currentCoinCount = Double.parseDouble(SharedPreferenceManager.getUserCoins(CameraActivity.this));
            int currentLevel = SharedPreferenceManager.getUserLevel(CameraActivity.this);
            if ((currentCoinCount % 100) == 0)
                currentLevel += 1;
            SharedPreferenceManager.setUserLevel(CameraActivity.this, currentLevel);
        }
        else{
            Toast.makeText(this, "Coin addition failed", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("ddmmyyyy", Locale.getDefault()).format(new Date());
        String imageFileName = "CoAi_" + timeStamp;
        try {
            return File.createTempFile(imageFileName, ".jpeg", galleryFolder);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void lock() {
        try {
            cameraCaptureSession.capture(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlock() {
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        if (textureView.isAvailable()) {
            setupCamera();
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
        if (timer != null)
            timer.cancel();
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(CameraActivity.this, MainActivity.class));
        finish();
    }
}
