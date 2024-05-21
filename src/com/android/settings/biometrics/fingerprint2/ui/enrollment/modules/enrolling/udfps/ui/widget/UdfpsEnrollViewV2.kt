/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.widget

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.DisplayInfo
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.View.OnHoverListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.settings.biometrics.fingerprint2.lib.model.StageViewModel
import com.android.systemui.biometrics.UdfpsUtils
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import com.android.systemui.biometrics.shared.model.UdfpsOverlayParams
import com.android.systemui.biometrics.shared.model.toInt

/**
 * View corresponding with fingerprint_v2_udfps_enroll_view.xml. This view is responsible for
 * drawing the [UdfpsEnrollIconV2] and the [UdfpsEnrollProgressBarDrawableV2].
 */
class UdfpsEnrollViewV2(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
  private lateinit var fingerprintSensorType: FingerprintSensorType
  private var onHoverListener: OnHoverListener = OnHoverListener { _, _ -> false }
  private var isAccessibilityEnabled: Boolean = false
  private lateinit var sensorRect: Rect
  private val fingerprintIcon: UdfpsEnrollIconV2 = UdfpsEnrollIconV2(mContext, attrs)
  private val fingerprintProgressDrawable: UdfpsEnrollProgressBarDrawableV2 =
    UdfpsEnrollProgressBarDrawableV2(mContext, attrs)
  private var remainingSteps = -1
  private val udfpsUtils: UdfpsUtils = UdfpsUtils()
  private lateinit var touchExplorationAnnouncer: TouchExplorationAnnouncer
  private var isRecreating = false

  /**
   * This function computes the center (x,y) location with respect to the parent [FrameLayout] for
   * the [UdfpsEnrollProgressBarDrawableV2]. It also computes the [Rect] with respect to the parent
   * [FrameLayout] for the [UdfpsEnrollIconV2]. This function will also setup the
   * [touchExplorationAnnouncer]
   */
  fun setSensorRect(rect: Rect, sensorType: FingerprintSensorType) {
    this.sensorRect = rect
    this.fingerprintSensorType = sensorType
    findViewById<ImageView?>(R.id.udfps_enroll_animation_fp_progress_view)?.also {
      it.setImageDrawable(fingerprintProgressDrawable)
    }
    findViewById<ImageView>(R.id.udfps_enroll_animation_fp_view)?.also {
      it.setImageDrawable(fingerprintIcon)
    }

    val rotation = display.rotation
    var displayInfo = DisplayInfo()
    context.display.getDisplayInfo(displayInfo)
    val scaleFactor = udfpsUtils.getScaleFactor(displayInfo)
    val overlayParams =
      UdfpsOverlayParams(
        sensorRect,
        fingerprintProgressDrawable.bounds,
        displayInfo.naturalWidth,
        displayInfo.naturalHeight,
        scaleFactor,
        rotation,
        sensorType.toInt(),
      )
    val parentView = parent as ViewGroup
    val coords = parentView.getLocationOnScreen()
    val parentLeft = coords[0]
    val parentTop = coords[1]
    val sensorRectOffset = Rect(sensorRect)
    // If the view has been rotated, we need to translate the sensor coordinates
    // to the new rotated view.
    when (rotation) {
      Surface.ROTATION_90,
      Surface.ROTATION_270 -> {
        sensorRectOffset.set(
          sensorRectOffset.top,
          sensorRectOffset.left,
          sensorRectOffset.bottom,
          sensorRectOffset.right,
        )
      }
      else -> {}
    }
    // Translate the sensor position into UdfpsEnrollView's view space.
    sensorRectOffset.offset(-parentLeft, -parentTop)

    fingerprintIcon.drawSensorRectAt(sensorRectOffset)
    fingerprintProgressDrawable.drawProgressAt(sensorRectOffset)

    touchExplorationAnnouncer = TouchExplorationAnnouncer(context, this, overlayParams, udfpsUtils)
  }

  /** Updates the current enrollment stage. */
  fun updateStage(it: StageViewModel) {
    fingerprintIcon.updateStage(it)
  }

  /** Receive enroll progress event */
  fun onUdfpsEvent(event: FingerEnrollState) {
    when (event) {
      is FingerEnrollState.EnrollProgress ->
        onEnrollmentProgress(event.remainingSteps, event.totalStepsRequired)
      is FingerEnrollState.Acquired -> onAcquired(event.acquiredGood)
      is FingerEnrollState.EnrollHelp -> onEnrollmentHelp()
      is FingerEnrollState.PointerDown -> onPointerDown()
      is FingerEnrollState.PointerUp -> onPointerUp()
      is FingerEnrollState.OverlayShown -> overlayShown()
      is FingerEnrollState.EnrollError ->
        throw IllegalArgumentException("$TAG should not handle udfps error")
    }
  }

  /** Indicates if accessibility is enabled. */
  fun setAccessibilityEnabled(enabled: Boolean) {
    this.isAccessibilityEnabled = enabled
    fingerprintProgressDrawable.setAccessibilityEnabled(enabled)
    if (enabled) {
      addHoverListener()
    } else {
      clearHoverListener()
    }
  }

  private fun udfpsError(errMsgId: Int, errString: String) {}
  /**
   * Sends a touch exploration event to the [onHoverListener] this should only be used for
   * debugging.
   */
  fun sendDebugTouchExplorationEvent(motionEvent: MotionEvent) {
    touchExplorationAnnouncer.onTouch(motionEvent)
  }

  /** Sets the addHoverListener, this should happen when talkback is enabled. */
  private fun addHoverListener() {
    onHoverListener = OnHoverListener { _: View, event: MotionEvent ->
      sendDebugTouchExplorationEvent(event)
      false
    }
    this.setOnHoverListener(onHoverListener)
  }

  /** Clears the hover listener if one was set. */
  private fun clearHoverListener() {
    val listener = OnHoverListener { _, _ -> false }
    this.setOnHoverListener(listener)
    onHoverListener = listener
  }

  private fun overlayShown() {
    Log.e(TAG, "Implement overlayShown")
  }

  /** Receive enroll progress event */
  private fun onEnrollmentProgress(remaining: Int, totalSteps: Int) {
    fingerprintIcon.onEnrollmentProgress(remaining, totalSteps)
    fingerprintProgressDrawable.onEnrollmentProgress(remaining, totalSteps)
  }

  /** Receive enroll help event */
  private fun onEnrollmentHelp() {
    fingerprintProgressDrawable.onEnrollmentHelp()
  }

  /** Receive onAcquired event */
  private fun onAcquired(isAcquiredGood: Boolean) {
    val animateIfLastStepGood = isAcquiredGood && remainingSteps <= 2 && remainingSteps >= 0
    if (animateIfLastStepGood) fingerprintProgressDrawable.onLastStepAcquired()
  }

  /** Receive onPointerDown event */
  private fun onPointerDown() {
    fingerprintIcon.stopDrawing()
  }

  /** Receive onPointerUp event */
  private fun onPointerUp() {
    fingerprintIcon.startDrawing()
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    // Because the layout has changed, we need to recompute all locations.
    if (this::sensorRect.isInitialized && this::fingerprintSensorType.isInitialized) {
      setSensorRect(sensorRect, fingerprintSensorType)
    }
  }

  /**
   * This class is responsible for announcing touch events that are outside of the sensort rect
   * area. Generally, if a touch is to the left of the sensor, the accessibility announcement will
   * be something like "move right"
   */
  private class TouchExplorationAnnouncer(
    val context: Context,
    val view: View,
    val overlayParams: UdfpsOverlayParams,
    val udfpsUtils: UdfpsUtils,
  ) {
    /** Will announce accessibility event for touches outside of the sensor rect. */
    fun onTouch(event: MotionEvent) {
      val scaledTouch: Point =
        udfpsUtils.getTouchInNativeCoordinates(event.getPointerId(0), event, overlayParams)
      if (udfpsUtils.isWithinSensorArea(event.getPointerId(0), event, overlayParams)) {
        return
      }
      val theStr: String =
        udfpsUtils.onTouchOutsideOfSensorArea(
          true /*touchExplorationEnabled*/,
          context,
          scaledTouch.x,
          scaledTouch.y,
          overlayParams,
        )
      if (theStr != null) {
        view.announceForAccessibility(theStr)
      }
    }
  }

  /** Indicates we should should restore the views saved state. */
  fun onEnrollProgressSaved(it: FingerEnrollState.EnrollProgress) {
    fingerprintIcon.onEnrollmentProgress(it.remainingSteps, it.totalStepsRequired, true)
    fingerprintProgressDrawable.onEnrollmentProgress(it.remainingSteps, it.totalStepsRequired, true)
  }

  companion object {
    private const val TAG = "UdfpsEnrollView"
  }
}
