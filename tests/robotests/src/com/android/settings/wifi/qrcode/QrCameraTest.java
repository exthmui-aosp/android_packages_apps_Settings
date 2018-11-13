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
 * limitations under the License.
 */

package com.android.settings.wifi.qrcode;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Size;
import android.view.SurfaceHolder;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class QrCameraTest {

    @Mock
    private SurfaceHolder mSurfaceHolder;

    private QrCamera mCamera;
    private Context mContext;

    private String mQrCode;
    CountDownLatch mCallbackSignal;
    private boolean mCameraCallbacked;

    private class ScannerTestCallback implements QrCamera.ScannerCallback {
        @Override
        public Size getViewSize() {
            return new Size(0, 0);
        }

        @Override
        public Rect getFramePosition(Size previewSize, int cameraOrientation) {
            return new Rect(0,0,0,0);
        }

        @Override
        public void handleSuccessfulResult(String qrCode) {
            mQrCode = qrCode;
        }

        @Override
        public void handleCameraFailure() {
            mCameraCallbacked = true;
            mCallbackSignal.countDown();
        }
    }

    private ScannerTestCallback mScannerCallback;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mScannerCallback = new ScannerTestCallback();
        mCamera = new QrCamera(mContext, mScannerCallback);
        mSurfaceHolder = mock(SurfaceHolder.class);
        mQrCode = "";
        mCameraCallbacked = false;
        mCallbackSignal = null;
    }

    @Test
    public void testCamera_Init_Callback() throws InterruptedException {
        mCallbackSignal = new CountDownLatch(1);
        mCamera.start(mSurfaceHolder);
        mCallbackSignal.await(5000, TimeUnit.MILLISECONDS);
        assertThat(mCameraCallbacked).isTrue();
    }

    @Test
    public void testDecode_PictureCaptured_QrCodeCorrectValue() {
        final String googleUrl = "http://www.google.com";

        try {
            Bitmap bmp = encodeQrCode(googleUrl, 320);
            int[] intArray = new int[bmp.getWidth() * bmp.getHeight()];
            bmp.getPixels(intArray, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
            LuminanceSource source = new RGBLuminanceSource(bmp.getWidth(), bmp.getHeight(),
                    intArray);

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            mCamera.decodeImage(bitmap);
        } catch (WriterException e) {
        }

        assertThat(mQrCode).isEqualTo(googleUrl);
    }

    private Bitmap encodeQrCode(String qrCode, int size) throws WriterException {
        BitMatrix qrBits = null;
        try {
            qrBits =
                    new MultiFormatWriter().encode(qrCode, BarcodeFormat.QR_CODE, size, size, null);
        } catch (IllegalArgumentException iae) {
            // Should never reach here.
        }
        assertThat(qrBits).isNotNull();

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        for (int x = 0; x < size; ++x) {
            for (int y = 0; y < size; ++y) {
                bitmap.setPixel(x, y, qrBits.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }
}
