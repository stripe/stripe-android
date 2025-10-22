package com.stripe.android.identity.camera

import android.graphics.Bitmap
import android.util.Size
import com.stripe.android.camera.CameraAdapter
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.CameraXAdapter
import com.stripe.android.camera.scanui.CameraView

/**
 * A manager class to hold [CameraView] and [CameraAdapter]. Used to inter-operate with
 * Jetpack Compose views.
 */
internal abstract class IdentityCameraManager {
    private var cameraView: CameraView? = null
    var cameraAdapter: CameraAdapter<CameraPreviewImage<Bitmap>>? = null

    /**
     * Callback from Jetpack Compose when a AndroidView is updated, might be called multiple times
     * with the same [CameraView] instance. Save it at the first occurrence of the call and
     * initialize [cameraAdapter] with the [CameraView].
     */
    fun onCameraViewUpdate(view: CameraView) {
        if (cameraView == null) {
            cameraView = view
            cameraAdapter = createCameraAdapter(view)
            onInitialized()
        }
    }

    fun requireCameraView() = requireNotNull(cameraView)
    fun requireCameraAdapter() = requireNotNull(cameraAdapter)

    /**
     * Get the camera lens model if available.
     */
    fun getCameraLensModel(): String? {
        return (cameraAdapter as? CameraXAdapter)?.getCameraLensModel()
    }

    /**
     * Get current exposure ISO if available.
     */
    fun getExposureIso(): Float? {
        return (cameraAdapter as? CameraXAdapter)?.getExposureIso()
    }

    /**
     * Get current focal length (in mm) if available.
     */
    fun getFocalLength(): Float? {
        return (cameraAdapter as? CameraXAdapter)?.getFocalLength()
    }

    /**
     * Check if the current camera is a virtual/logical camera (combining multiple physical cameras).
     */
    fun isVirtualCamera(): Boolean? {
        return (cameraAdapter as? CameraXAdapter)?.isVirtualCamera()
    }

    /**
     * Get current exposure duration (in milliseconds) if available.
     */
    fun getExposureDuration(): Long? {
        return (cameraAdapter as? CameraXAdapter)?.getExposureDuration()
    }

    /**
     * Initialize [cameraAdapter] when a new [CameraView] is created from compose.
     */
    protected abstract fun createCameraAdapter(cameraView: CameraView): CameraAdapter<CameraPreviewImage<Bitmap>>

    /**
     * Callback when [cameraAdapter] and [CameraView] are initialized.
     */
    protected open fun onInitialized() {}

    open fun toggleInitial() {}
    open fun toggleFound() {}
    open fun toggleSatisfied() {}
    open fun toggleUnsatisfied() {}
    open fun toggleTimeOut() {}
    open fun toggleFinished() {}

    protected companion object {
        val MINIMUM_RESOLUTION = Size(1440, 1080)
    }
}
