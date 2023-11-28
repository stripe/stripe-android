package com.stripe.android.identity.camera

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
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
    fun onCameraViewUpdate(view: CameraView, pointF: PointF) {
        if(pointF.x > 0f && cameraView != null) {
            cameraView!!.setOnTouchListener { _, e ->

                val centerPoint = PointF(
                    pointF.x + e.x,
                    pointF.y + e.y
                )
                Log.d("BGLM", "pointF.x ${pointF.x}")
                Log.d("BGLM", "pointF.y ${pointF.y}")


                Log.d("BGLM", "centerPoint.x ${centerPoint.x}")
                Log.d("BGLM", "centerPoint.y ${centerPoint.y}")


                Log.d("BGLM", "e.x ${e.x}")
                Log.d("BGLM", "e.y ${e.y}")


                cameraAdapter?.setFocus(
                    centerPoint
                )
                true
            }
        }
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

    protected companion object {
        val MINIMUM_RESOLUTION = Size(1440, 1080)
    }
}
