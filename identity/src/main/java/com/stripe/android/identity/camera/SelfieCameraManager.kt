package com.stripe.android.identity.camera

import android.content.Context
import com.stripe.android.camera.CameraXAdapter
import com.stripe.android.camera.DefaultCameraErrorListener
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.identity.utils.getActivity

internal class SelfieCameraManager(
    private val context: Context,
    private val cameraErrorCallback: (Throwable?) -> Unit
) : IdentityCameraManager() {
    override fun createCameraAdapter(cameraView: CameraView) = CameraXAdapter(
        requireNotNull(context.getActivity()),
        cameraView.previewFrame,
        MINIMUM_RESOLUTION,
        DefaultCameraErrorListener(context, cameraErrorCallback),
        startWithBackCamera = false
    )
}
