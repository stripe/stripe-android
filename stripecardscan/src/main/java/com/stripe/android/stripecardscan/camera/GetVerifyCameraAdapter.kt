package com.stripe.android.stripecardscan.camera

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import com.stripe.android.camera.Camera1Adapter
import com.stripe.android.camera.CameraAdapter
import com.stripe.android.camera.CameraErrorListener
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.CameraXAdapter

private const val LOG_TAG = "CameraSelector"

/**
 * Get the appropriate camera adapter. If the customer has provided an additional camera adapter,
 * use that in place of camera 1.
 */
internal fun getVerifyCameraAdapter(
    activity: Activity,
    previewView: ViewGroup,
    minimumResolution: Size,
    cameraErrorListener: CameraErrorListener
): CameraAdapter<CameraPreviewImage<Bitmap>> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        try {
            CameraXAdapter(activity, previewView, minimumResolution, cameraErrorListener)
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "Unable to instantiate CameraX", t)
            Camera1Adapter(activity, previewView, minimumResolution, cameraErrorListener)
        }
    } else {
        // older versions of android (API 20 and older) do not support YUV_420_888
        Camera1Adapter(activity, previewView, minimumResolution, cameraErrorListener)
    }.apply {
        Log.d(LOG_TAG, "Using camera implementation ${this.implementationName}")
    }
