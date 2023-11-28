package com.stripe.android.identity.camera

import android.content.Context
import android.view.View
import com.stripe.android.camera.CameraXAdapter
import com.stripe.android.camera.DefaultCameraErrorListener
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.camera.scanui.util.startAnimation
import com.stripe.android.camera.scanui.util.startAnimationIfNotRunning
import com.stripe.android.identity.R
import com.stripe.android.identity.utils.getActivity

internal class DocumentScanCameraManager(
    private val context: Context,
    private val cameraErrorCallback: (Throwable?) -> Unit
) : IdentityCameraManager() {
    override fun createCameraAdapter(cameraView: CameraView) = CameraXAdapter(
        requireNotNull(context.getActivity()),
        cameraView.previewFrame,
        MINIMUM_RESOLUTION,
        DefaultCameraErrorListener(context, cameraErrorCallback)
    )

    override fun onInitialized() {
        requireCameraView().viewFinderWindowView
            .setBackgroundResource(
                R.drawable.stripe_viewfinder_background
            )
    }

    override fun toggleInitial() {
        requireCameraView().viewFinderBackgroundView.visibility = View.VISIBLE
        requireCameraView().viewFinderWindowView.visibility = View.VISIBLE
        requireCameraView().viewFinderBorderView.visibility = View.VISIBLE
        requireCameraView().viewFinderBorderView.startAnimation(R.drawable.stripe_viewfinder_border_initial)
    }

    override fun toggleFound() {
        requireCameraView().viewFinderBorderView.startAnimationIfNotRunning(R.drawable.stripe_viewfinder_border_found)
    }

    override fun toggleFinished() {
        requireCameraView().viewFinderBackgroundView.visibility = View.INVISIBLE
        requireCameraView().viewFinderWindowView.visibility = View.INVISIBLE
        requireCameraView().viewFinderBorderView.visibility = View.INVISIBLE
        requireCameraView().viewFinderBorderView.startAnimation(R.drawable.stripe_viewfinder_border_initial)
    }
}
