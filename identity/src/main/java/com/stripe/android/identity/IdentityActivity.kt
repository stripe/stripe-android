package com.stripe.android.identity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.camera.Camera1Adapter
import com.stripe.android.camera.CameraPermissionCheckingActivity
import com.stripe.android.camera.DefaultCameraErrorListener
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.camera.scanui.util.asRect
import com.stripe.android.identity.IdentityVerificationSheet.VerificationResult
import com.stripe.android.identity.databinding.IdentityActivityBinding

/**
 * Host activity to perform Identity verification.
 *
 * TODO(ccen): Switching between different fragments that has different aspect ratios for different ID types.
 */
internal class IdentityActivity : CameraPermissionCheckingActivity() {
    private val binding by lazy {
        IdentityActivityBinding.inflate(layoutInflater)
    }

    private val cameraView: CameraView by lazy {
        binding.cameraView
    }

    private val cameraAdapter: Camera1Adapter by lazy {
        Camera1Adapter(
            this,
            cameraView.previewFrame,
            MINIMUM_RESOLUTION,
            DefaultCameraErrorListener(this) { cause ->
                Log.d(TAG, "scan fails with exception: $cause")
                // TODO(ccen) determine if further handling is required
            }
        )
    }

    @VisibleForTesting
    internal val viewModelFactory: ViewModelProvider.Factory by lazy {
        IdentityViewModel.IdentityViewModelFactory()
    }

    @VisibleForTesting
    internal val viewModel: IdentityViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ensureCameraPermission()
    }

    override fun onCameraReady() {
        cameraAdapter.bindToLifecycle(this)
        viewModel.identityScanFlow.startFlow(
            context = this,
            imageStream = cameraAdapter.getImageStream(),
            viewFinder = cameraView.viewFinderWindowView.asRect(),
            lifecycleOwner = this,
            coroutineScope = lifecycleScope,
            parameters = 23
        )
    }

    override fun onUserDeniedCameraPermission() {
        Log.d(TAG, "onUserDeniedCameraPermission")
        // TODO(ccen): determine whether to return Fail or Canceled
        finishWithResult(VerificationResult.Canceled)
    }

    override fun onBackPressed() {
        finishWithResult(VerificationResult.Canceled)
    }

    private fun finishWithResult(result: VerificationResult) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(result.toBundle())
        )
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.identityScanFlow.cancelFlow()
    }

    private companion object {
        val TAG: String = IdentityActivity::class.java.simpleName
        val MINIMUM_RESOLUTION = Size(1067, 600) // TODO: decide what to use
    }
}
