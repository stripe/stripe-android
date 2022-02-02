package com.stripe.android.identity

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.camera.Camera1Adapter
import com.stripe.android.camera.CameraErrorListener
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.identity.databinding.IdentityActivityBinding

/**
 * TODO(ccen): The activity handles camera permission,
 * TODO(ccen): Switching between different fragments that has different aspect ratios for different ID types,
 *
 */
class IdentityActivity : AppCompatActivity(), CameraErrorListener {
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
            this
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
        Log.d(TAG, "started")
        binding.next.setOnClickListener {
            onCameraReady() // TODO: make the call after permission check
        }
    }

    /**
     * Get a rect from a view.
     */
    private fun View.asRect() = Rect(left, top, right, bottom)

    private fun onCameraReady() {
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

    private companion object {
        val TAG: String = IdentityActivity::class.java.simpleName
        val MINIMUM_RESOLUTION = Size(1067, 600) // TODO: decide what to use
    }

    override fun onCameraOpenError(cause: Throwable?) {
        Log.d(TAG, "onCameraOpenError: $cause")
    }

    override fun onCameraAccessError(cause: Throwable?) {
        Log.d(TAG, "onCameraAccessError: $cause")
    }

    override fun onCameraUnsupportedError(cause: Throwable?) {
        Log.d(TAG, "onCameraUnsupportedError: $cause")
    }
}
