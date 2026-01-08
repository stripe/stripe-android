package com.stripe.android.identity.camera

import android.content.Context
import android.util.Log
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
        Log.i(TAG, "onInitialized: setting viewfinder background")
        requireCameraView().viewFinderWindowView
            .setBackgroundResource(
                R.drawable.stripe_viewfinder_background
            )
    }

    override fun toggleInitial() {
        Log.i(TAG, "toggleInitial: showing initial border & starting animation")
        requireCameraView().viewFinderBackgroundView.visibility = View.VISIBLE
        requireCameraView().viewFinderWindowView.visibility = View.VISIBLE
        requireCameraView().viewFinderBorderView.visibility = View.VISIBLE
        requireCameraView().viewFinderBorderView.startAnimation(R.drawable.stripe_viewfinder_border_initial)
    }

    override fun toggleFound() {
        Log.i(TAG, "toggleFound: starting 'found' animation")
        requireCameraView().viewFinderBorderView.startAnimationIfNotRunning(R.drawable.stripe_viewfinder_border_found)
    }

    override fun toggleFinished() {
        Log.i(TAG, "toggleFinished: hiding viewfinder UI")
        requireCameraView().viewFinderBackgroundView.visibility = View.INVISIBLE
        requireCameraView().viewFinderWindowView.visibility = View.INVISIBLE
        requireCameraView().viewFinderBorderView.visibility = View.INVISIBLE
        requireCameraView().viewFinderBorderView.startAnimation(R.drawable.stripe_viewfinder_border_initial)
    }

    companion object {
        private const val TAG: String = "DocumentScanCameraManager"
    }
}
