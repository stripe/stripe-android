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

private const val LOG_TAG = "CameraSelector"

/**
 * Get the appropriate camera adapter. If the customer has provided an additional camera adapter,
 * use that in place of camera 1.
 */
internal fun getCameraAdapter(
    activity: Activity,
    previewView: ViewGroup,
    minimumResolution: Size,
    cameraErrorListener: CameraErrorListener
): CameraAdapter<CameraPreviewImage<Bitmap>> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        try {
            getAlternateCamera(activity, previewView, minimumResolution, cameraErrorListener)
        } catch (t: Throwable) {
            Log.d(LOG_TAG, "No alternative camera implementations, falling back to default")
            Camera1Adapter(activity, previewView, minimumResolution, cameraErrorListener)
        }
    } else {
        Log.d(LOG_TAG, "YUV_420_888 is not supported, falling back to default camera")
        Camera1Adapter(activity, previewView, minimumResolution, cameraErrorListener)
    }.apply {
        Log.d(LOG_TAG, "Using camera implementation ${this.implementationName}")
    }

@Suppress("UNCHECKED_CAST")
@Throws(ClassNotFoundException::class, NoSuchMethodException::class, IllegalAccessException::class)
private fun getAlternateCamera(
    activity: Activity,
    previewView: ViewGroup,
    minimumResolution: Size,
    cameraErrorListener: CameraErrorListener
): CameraAdapter<CameraPreviewImage<Bitmap>> =
    Class.forName("com.stripe.android.stripecardscan.camera.extension.CameraAdapterImpl")
        .getConstructor(
            Activity::class.java,
            ViewGroup::class.java,
            Size::class.java,
            CameraErrorListener::class.java
        )
        .newInstance(
            activity,
            previewView,
            minimumResolution,
            cameraErrorListener
        ) as CameraAdapter<CameraPreviewImage<Bitmap>>
