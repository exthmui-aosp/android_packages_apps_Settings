/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.settings.wifi.qrcode;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import androidx.annotation.VisibleForTesting;

/**
 * Manage the camera for the QR scanner and help the decoder to get the image inside the scanning
 * frame. Caller prepares a {@link SurfaceHolder} then call {@link #start(SurfaceHolder)} to
 * start QR Code scanning. The scanning result will return by ScannerCallback interface. Caller
 * can also call {@link #stop()} to halt QR Code scanning before the result returned.
 */
public class QrCamera extends Handler {
    private static final String TAG = "QrCamera";

    private static final int MSG_AUTO_FOCUS = 1;

    private static double MIN_RATIO_DIFF_PERCENT = 0.1;
    private static long AUTOFOCUS_INTERVAL_MS = 1500L;

    private static Map<DecodeHintType, List<BarcodeFormat>> HINTS = new ArrayMap<>();
    private static List<BarcodeFormat> FORMATS = new ArrayList<>();

    static {
        FORMATS.add(BarcodeFormat.QR_CODE);
        HINTS.put(DecodeHintType.POSSIBLE_FORMATS, FORMATS);
    }

    private Camera mCamera;
    private Size mPreviewSize;
    private WeakReference<Context> mContext;
    private ScannerCallback mScannerCallback;
    private MultiFormatReader mReader;
    private DecodingTask mDecodeTask;
    private int mCameraOrientation;
    private Camera.Parameters mParameters;

    public QrCamera(Context context, ScannerCallback callback) {
        mContext =  new WeakReference<Context>(context);
        mScannerCallback = callback;
        mReader = new MultiFormatReader();
        mReader.setHints(HINTS);
    }

    void start(SurfaceHolder surfaceHolder) {
        if (mDecodeTask == null) {
            mDecodeTask = new DecodingTask(surfaceHolder);
            mDecodeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    void stop() {
        removeMessages(MSG_AUTO_FOCUS);
        if (mDecodeTask != null) {
            mDecodeTask.cancel(true);
            mDecodeTask = null;
        }
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    /** The scanner which includes this QrCamera class should implement this */
    interface ScannerCallback {

        /**
         * The function used to handle the decoding result of the QR code.
         *
         * @param result the result QR code after decoding.
         */
        void handleSuccessfulResult(String result);

        /** Request the QR code scanner to handle the failure happened. */
        void handleCameraFailure();

        /**
         * The function used to get the background View size.
         *
         * @return Includes the background view size.
         */
        Size getViewSize();

        /**
         * The function used to get the frame position inside the view
         *
         * @param previewSize Is the preview size set by camera
         * @param cameraOrientation Is the orientation of current Camera
         * @return The rectangle would like to crop from the camera preview shot.
         */
        Rect getFramePosition(Size previewSize, int cameraOrientation);
    }

    private void setCameraParameter() {
        mParameters = mCamera.getParameters();
        mPreviewSize = getBestPreviewSize(mParameters);
        mParameters.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mParameters.setPictureSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        if (mParameters.getSupportedFlashModes().contains(Parameters.FLASH_MODE_OFF)) {
            mParameters.setFlashMode(Parameters.FLASH_MODE_OFF);
        }

        final List<String> supportedFocusModes = mParameters.getSupportedFocusModes();
        if (supportedFocusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            mParameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (supportedFocusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
            mParameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
        }
        mCamera.setParameters(mParameters);
    }

    private boolean startPreview() {
        if (mContext.get() == null) {
            return false;
        }

        final WindowManager winManager =
                (WindowManager) mContext.get().getSystemService(Context.WINDOW_SERVICE);
        final int rotation = winManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        final int rotateDegrees = (mCameraOrientation - degrees + 360) % 360;
        mCamera.setDisplayOrientation(rotateDegrees);
        mCamera.startPreview();
        if (mParameters.getFocusMode() == Parameters.FOCUS_MODE_AUTO) {
            mCamera.autoFocus(/* Camera.AutoFocusCallback */ null);
            sendMessageDelayed(obtainMessage(MSG_AUTO_FOCUS), AUTOFOCUS_INTERVAL_MS);
        }
        return true;
    }

    private class DecodingTask extends AsyncTask<Void, Void, String> {
        private QrYuvLuminanceSource mImage;
        private SurfaceHolder mSurfaceHolder;

        private DecodingTask(SurfaceHolder surfaceHolder) {
            mSurfaceHolder = surfaceHolder;
        }

        @Override
        protected String doInBackground(Void... tmp) {
            if (!initCamera(mSurfaceHolder)) {
                return null;
            }

            final Semaphore imageGot = new Semaphore(0);
            while (true) {
                // This loop will try to capture preview image continuously until a valid QR Code
                // decoded. The caller can also call {@link #stop()} to inturrupts scanning loop.
                mCamera.setOneShotPreviewCallback(
                        (imageData, camera) -> {
                            mImage = getFrameImage(imageData);
                            imageGot.release();
                        });
                try {
                    // Semaphore.acquire() blocking until permit is available, or the thread is
                    // interrupted.
                    imageGot.acquire();
                    Result qrCode = null;
                    try {
                        qrCode =
                                mReader.decodeWithState(
                                        new BinaryBitmap(new HybridBinarizer(mImage)));
                    } catch (ReaderException e) {
                        // No logging since every time the reader cannot decode the
                        // image, this ReaderException will be thrown.
                    } finally {
                        mReader.reset();
                    }
                    if (qrCode != null) {
                        return qrCode.getText();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }

        @Override
        protected void onPostExecute(String qrCode) {
            if (qrCode != null) {
                mScannerCallback.handleSuccessfulResult(qrCode);
            }
        }

        private boolean initCamera(SurfaceHolder surfaceHolder) {
            final int numberOfCameras = Camera.getNumberOfCameras();
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            try {
                for (int i = 0; i < numberOfCameras; ++i) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                        mCamera = Camera.open(i);
                        mCamera.setPreviewDisplay(surfaceHolder);
                        mCameraOrientation = cameraInfo.orientation;
                        break;
                    }
                }
                if (mCamera == null) {
                    Log.e(TAG, "Cannot find available back camera.");
                    mScannerCallback.handleCameraFailure();
                    return false;
                }
                setCameraParameter();
                if (!startPreview()) {
                    Log.e(TAG, "Error to init Camera");
                    mCamera = null;
                    mScannerCallback.handleCameraFailure();
                    return false;
                }
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Error to init Camera");
                mCamera = null;
                mScannerCallback.handleCameraFailure();
                return false;
            }
        }
    }

    private QrYuvLuminanceSource getFrameImage(byte[] imageData) {
        final Rect frame = mScannerCallback.getFramePosition(mPreviewSize, mCameraOrientation);
        final Camera.Size size = mParameters.getPictureSize();
        QrYuvLuminanceSource image = new QrYuvLuminanceSource(imageData, size.width, size.height);
        return (QrYuvLuminanceSource)
                image.crop(frame.left, frame.top, frame.width(), frame.height());
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_AUTO_FOCUS:
                // Calling autoFocus(null) will only trigger the camera to focus once. In order
                // to make the camera continuously auto focus during scanning, need to periodly
                // trigger it.
                mCamera.autoFocus(/* Camera.AutoFocusCallback */ null);
                sendMessageDelayed(obtainMessage(MSG_AUTO_FOCUS), AUTOFOCUS_INTERVAL_MS);
                break;
            default:
                Log.d(TAG, "Unexpected Message: " + msg.what);
        }
    }

    private Size getBestPreviewSize(Camera.Parameters parameters) {
        final Size windowSize = mScannerCallback.getViewSize();
        Size bestChoice = new Size(0, 0);
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= windowSize.getWidth() && size.height <= windowSize.getHeight()) {
                bestChoice = new Size(size.width, size.height);
                break;
            }
        }
        return bestChoice;
    }

    @VisibleForTesting
    protected void decodeImage(BinaryBitmap image) {
        Result qrCode = null;

        try {
            qrCode = mReader.decodeWithState(image);
        } catch (ReaderException e) {
        } finally {
            mReader.reset();
        }

        if (qrCode != null) {
            mScannerCallback.handleSuccessfulResult(qrCode.getText());
        }
    }
}
