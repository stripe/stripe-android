package com.stripe.android.identity.camera

import android.graphics.Bitmap
import android.util.Size
import com.stripe.android.camera.CameraAdapter
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.scanui.CameraView

/**
 * A manager class to hold [CameraView] and [CameraAdapter]. Used to inter-operate with
 * Jetpack Compose views.
 */
internal abstract class IdentityCameraManager {
    private var cameraView: CameraView? = null
    private var cameraAdapter: CameraAdapter<CameraPreviewImage<Bitmap>>? = null

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
