package com.stripe.android.camera

import android.content.Context
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

/**
 * A basic implementation that displays error messages when there is a problem with the camera.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultCameraErrorListener(
    private val context: Context,
    private val callback: (Throwable?) -> Unit
) : CameraErrorListener {
    override fun onCameraOpenError(cause: Throwable?) {
        showCameraError(R.string.stripe_error_camera_open, cause)
    }

    override fun onCameraAccessError(cause: Throwable?) {
        showCameraError(R.string.stripe_error_camera_access, cause)
    }

    override fun onCameraUnsupportedError(cause: Throwable?) {
        Log.e(TAG, "Camera not supported", cause)
        showCameraError(R.string.stripe_error_camera_unsupported, cause)
    }

    private fun showCameraError(@StringRes message: Int, cause: Throwable?) {
        AlertDialog.Builder(context)
            .setTitle(R.string.stripe_error_camera_title)
            .setMessage(message)
            .setPositiveButton(R.string.stripe_error_camera_acknowledge_button) { _, _ ->
                callback(cause)
            }
            .show()
    }

    private companion object {
        val TAG: String = DefaultCameraErrorListener::class.java.simpleName
    }
}
