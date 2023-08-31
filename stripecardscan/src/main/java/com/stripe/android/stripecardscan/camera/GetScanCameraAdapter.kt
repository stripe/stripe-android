package com.stripe.android.stripecardscan.camera

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import com.stripe.android.camera.Camera1Adapter
import com.stripe.android.camera.CameraAdapter
import com.stripe.android.camera.CameraErrorListener
import com.stripe.android.camera.CameraPreviewImage

private const val LOG_TAG = "CameraSelector"

/**
 * Get the appropriate camera adapter. If the customer has provided an additional camera adapter,
 * use that in place of camera 1.
 */
internal fun getScanCameraAdapter(
    activity: Activity,
    previewView: ViewGroup,
    minimumResolution: Size,
    cameraErrorListener: CameraErrorListener
): CameraAdapter<CameraPreviewImage<Bitmap>> =
    Camera1Adapter(activity, previewView, minimumResolution, cameraErrorListener).apply {
        Log.d(LOG_TAG, "Using camera implementation ${this.implementationName}")
    }
