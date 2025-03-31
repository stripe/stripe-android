package com.stripe.android.stripecardscan.scanui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.stripe.android.camera.CameraAdapter
import com.stripe.android.camera.CameraErrorListener
import com.stripe.android.camera.CameraPermissionCheckingActivity
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.DefaultCameraErrorListener
import com.stripe.android.mlcore.impl.InterpreterInitializerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlin.coroutines.CoroutineContext
import com.stripe.android.camera.R as CameraR

sealed interface CancellationReason : Parcelable {

    @Parcelize
    data object Closed : CancellationReason

    @Parcelize
    data object Back : CancellationReason

    @Parcelize
    data object UserCannotScan : CancellationReason

    @Parcelize
    data object CameraPermissionDenied : CancellationReason
}

internal interface ScanResultListener {

    /**
     * The user canceled the scan.
     */
    fun userCanceled(reason: CancellationReason)

    /**
     * The scan failed because of an error.
     */
    fun failed(cause: Throwable?)
}

internal abstract class ScanActivity : CameraPermissionCheckingActivity(), CoroutineScope {

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
        DefaultCameraErrorListener(this) { t -> scanFailure(t) }
    }

    /**
     * The listener which will handle the results from the scan.
     */
    internal abstract val resultListener: ScanResultListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback {
            resultListener.userCanceled(CancellationReason.Back)
            closeScanner()
        }

        if (!CameraAdapter.isCameraSupported(this)) {
            showCameraNotSupportedDialog()
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
            ensureCameraPermission(
                ::onCameraReady,
                ::onUserDeniedCameraPermission
            )
        }
    }

    protected open fun hideSystemUi() {
        // Prevent screenshots and keep the screen on while scanning.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE +
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_SECURE +
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // Hide both the navigation bar and the status bar. Allow system gestures to show the
        // navigation and status bar, but prevent the UI from resizing when they are shown.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
        } else {
            @Suppress("deprecation")
            window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }
    }

    override fun onPause() {
        super.onPause()
        setFlashlightState(false)
    }

    /**
     * Show a dialog explaining that the camera is not available.
     */
    protected open fun showCameraNotSupportedDialog() {
        AlertDialog.Builder(this)
            .setTitle(CameraR.string.stripe_error_camera_title)
            .setMessage(CameraR.string.stripe_error_camera_unsupported)
            .setPositiveButton(CameraR.string.stripe_error_camera_acknowledge_button) { _, _ ->
                scanFailure()
            }
            .show()
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
     * The camera permission was granted.
     */
    protected abstract fun onCameraReady()

    /**
     * The camera permission was denied.
     */
    protected fun onUserDeniedCameraPermission() {
        resultListener.userCanceled(CancellationReason.CameraPermissionDenied)
        closeScanner()
    }

    /**
     * The user cannot scan the required object.
     */
    protected open fun userCannotScan() {
        resultListener.userCanceled(CancellationReason.UserCannotScan)
        closeScanner()
    }

    /**
     * Close the scanner.
     */
    protected open fun closeScanner() {
        setFlashlightState(false)
        finish()
    }

    protected fun startCameraAdapter() {
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
                context = this@ScanActivity,
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
        cameraProvider(this, previewFrame, minimumAnalysisResolution, cameraErrorListener)

    protected abstract val previewFrame: ViewGroup

    protected abstract val minimumAnalysisResolution: Size

    /**
     * A stream of images from the camera is available to be processed.
     */
    @VisibleForTesting
    internal abstract suspend fun onCameraStreamAvailable(
        cameraStream: Flow<CameraPreviewImage<Bitmap>>
    )

    companion object {
        private val LOG_TAG = ScanActivity::class.java.simpleName
    }
}
