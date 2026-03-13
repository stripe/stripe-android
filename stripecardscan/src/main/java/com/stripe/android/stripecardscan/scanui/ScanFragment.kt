package com.stripe.android.stripecardscan.scanui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stripe.android.camera.CameraAdapter
import com.stripe.android.camera.CameraErrorListener
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.DefaultCameraErrorListener
import com.stripe.android.core.storage.StorageFactory
import com.stripe.android.mlcore.impl.InterpreterInitializerImpl
import com.stripe.android.stripecardscan.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import com.stripe.android.camera.R as CameraR

private const val PERMISSION_RATIONALE_SHOWN = "permission_rationale_shown"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class ScanFragment : Fragment(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    protected var isFlashlightOn: Boolean = false
        private set

    abstract val cameraAdapterBuilder: (
        Activity,
        ViewGroup,
        Size,
        CameraErrorListener
    ) -> CameraAdapter<CameraPreviewImage<Bitmap>>
    internal val cameraAdapter by lazy { buildCameraAdapter(cameraAdapterBuilder) }
    private val cameraErrorListener by lazy {
        DefaultCameraErrorListener(requireActivity()) { t -> scanFailure(t) }
    }

    /**
     * The listener which will handle the results from the scan.
     */
    internal abstract val resultListener: ScanResultListener

    private val storage by lazy {
        StorageFactory.getStorageInstance(requireActivity(), "scan_camera_permissions")
    }

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                prepareCamera { onCameraReady() }
            } else {
                showPermissionDenied()
            }
        }

    override fun onStart() {
        super.onStart()

        context?.let {
            if (!CameraAdapter.isCameraSupported(it)) {
                showCameraNotSupported()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()

        launch {
            delay(1500)
            hideSystemUi()
        }

        if (!cameraAdapter.isBoundToLifecycle()) {
            ensurePermissionAndStartCamera()
        }
    }

    protected open fun hideSystemUi() {
        // Prevent screenshots and keep the screen on while scanning.
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE +
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_SECURE +
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }

    override fun onPause() {
        super.onPause()
        setFlashlightState(false)
    }

    /**
     * Ensure that the camera permission is available. If so, start the camera. If not, request it.
     */
    protected open fun ensurePermissionAndStartCamera() {
        when {
            ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                prepareCamera { onCameraReady() }
            }
            storage.getBoolean(
                PERMISSION_RATIONALE_SHOWN,
                false
            ) -> showPermissionDenied()
            else -> requestCameraPermission()
        }
    }

    /**
     * Show text explaining that the camera is not available.
     */
    protected open fun showCameraNotSupported() {
        instructionsText.visibility = View.VISIBLE
        instructionsText.setText(CameraR.string.stripe_error_camera_unsupported)
        scanFailure()
    }

    /**
     * Show text  for why we are requesting camera permissions when the permission
     * has been denied.
     */
    protected open fun showPermissionDenied() {
        instructionsText.visibility = View.VISIBLE
        instructionsText.setText(R.string.stripe_camera_permission_settings_message)
        storage.storeValue(PERMISSION_RATIONALE_SHOWN, true)
    }

    /**
     * Request permission to use the camera.
     */
    protected open fun requestCameraPermission() {
        cameraPermissionLauncher.launch(
            Manifest.permission.CAMERA
        )
    }

    /**
     * Turn the flashlight on or off.
     */
    protected open fun toggleFlashlight() {
        isFlashlightOn = !isFlashlightOn
        setFlashlightState(isFlashlightOn)
        // TODO: this should be reported as part of scanstats, but is not yet supported
    }

    /**
     * Toggle between available cameras.
     */
    protected open fun toggleCamera() {
        cameraAdapter.changeCamera()
        // TODO: this should probably be reported as part of scanstats, but is not yet supported
    }

    /**
     * Called when the flashlight state has changed.
     */
    protected abstract fun onFlashlightStateChanged(flashlightOn: Boolean)

    /**
     * Turn the flashlight on or off.
     */
    private fun setFlashlightState(on: Boolean) {
        cameraAdapter.setTorchState(on)
        isFlashlightOn = on
        onFlashlightStateChanged(on)
    }

    /**
     * Cancel scanning due to a camera error.
     */
    protected open fun scanFailure(cause: Throwable? = null) {
        Log.e(LOG_TAG, "Canceling scan due to error", cause)
        resultListener.failed(cause)
        closeScanner()
    }

    /**
     * The scan has been closed by the user.
     */
    protected open fun userClosedScanner() {
        resultListener.userCanceled(CancellationReason.Closed)
        closeScanner()
    }

    /**
     * Close the scanner.
     */
    protected open fun closeScanner() {
        setFlashlightState(false)
        cameraAdapter.unbindFromLifecycle(this)
    }

    /**
     * Prepare to start the camera. Once the camera is ready, [onCameraReady] must be called.
     */
    protected abstract fun prepareCamera(onCameraReady: () -> Unit)

    protected open fun onCameraReady() {
        instructionsText.visibility = View.GONE
        cameraAdapter.bindToLifecycle(this)

        cameraAdapter.withFlashSupport {
            setFlashlightState(cameraAdapter.isTorchOn())
            onFlashSupported(it)
        }

        // TODO: this should probably be reported as part of scanstats, but is not yet supported
        cameraAdapter.withSupportsMultipleCameras {
            onSupportsMultipleCameras(it)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            InterpreterInitializerImpl.initialize(
                context = requireContext(),
                onSuccess = {
                    lifecycleScope.launch { onCameraStreamAvailable(cameraAdapter.getImageStream()) }
                },
                onFailure = {
                    scanFailure(it)
                }
            )
        }
    }

    /**
     * Perform an action when the flash is supported
     */
    protected abstract fun onFlashSupported(supported: Boolean)

    /**
     * Perform an action when the camera support is determined
     */
    protected abstract fun onSupportsMultipleCameras(supported: Boolean)

    protected open fun setFocus(point: PointF) {
        cameraAdapter.setFocus(point)
    }

    /**
     * Generate a camera adapter
     */
    internal open fun buildCameraAdapter(
        cameraProvider: (Activity, ViewGroup, Size, CameraErrorListener) -> CameraAdapter<CameraPreviewImage<Bitmap>>
    ): CameraAdapter<CameraPreviewImage<Bitmap>> =
        cameraProvider(requireActivity(), previewFrame, minimumAnalysisResolution, cameraErrorListener)

    protected abstract val previewFrame: ViewGroup

    protected abstract val instructionsText: TextView

    protected abstract val minimumAnalysisResolution: Size

    /**
     * A stream of images from the camera is available to be processed.
     */
    protected abstract suspend fun onCameraStreamAvailable(
        cameraStream: Flow<CameraPreviewImage<Bitmap>>
    )

    internal companion object {
        private val LOG_TAG = ScanFragment::class.java.simpleName
    }
}
