package com.stripe.android.stripecardscan.scanui

import android.content.Context
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
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.stripe.android.camera.CameraAdapter
import com.stripe.android.camera.CameraErrorListener
import com.stripe.android.camera.CameraPermissionCheckingActivity
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.framework.Stats
import com.stripe.android.stripecardscan.R
import com.stripe.android.stripecardscan.camera.getCameraAdapter
import com.stripe.android.stripecardscan.framework.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import kotlin.coroutines.CoroutineContext

sealed interface CancellationReason : Parcelable {

    @Parcelize
    object Closed : CancellationReason

    @Parcelize
    object Back : CancellationReason

    @Parcelize
    object UserCannotScan : CancellationReason

    @Parcelize
    object CameraPermissionDenied : CancellationReason
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

/**
 * A basic implementation that displays error messages when there is a problem with the camera.
 */
internal class CameraErrorListenerImpl(
    protected val context: Context,
    protected val callback: (Throwable?) -> Unit
) : CameraErrorListener {
    override fun onCameraOpenError(cause: Throwable?) {
        showCameraError(R.string.stripe_error_camera_open, cause)
    }

    override fun onCameraAccessError(cause: Throwable?) {
        showCameraError(R.string.stripe_error_camera_access, cause)
    }

    override fun onCameraUnsupportedError(cause: Throwable?) {
        Log.e(Config.logTag, "Camera not supported", cause)
        showCameraError(R.string.stripe_error_camera_unsupported, cause)
    }

    private fun showCameraError(@StringRes message: Int, cause: Throwable?) {
        AlertDialog.Builder(context)
            .setTitle(R.string.stripe_error_camera_title)
            .setMessage(message)
            .setPositiveButton(R.string.stripe_error_camera_acknowledge_button) { _, _ ->
                callback(cause)
            }
            .show()
    }
}

internal abstract class ScanActivity : CameraPermissionCheckingActivity(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    internal val scanStat = Stats.trackTask("scan_activity")
    private val permissionStat = Stats.trackTask("camera_permission")

    protected var isFlashlightOn: Boolean = false
        private set

    internal val cameraAdapter by lazy { buildCameraAdapter() }
    private val cameraErrorListener by lazy {
        CameraErrorListenerImpl(this) { t -> scanFailure(t) }
    }

    /**
     * The listener which will handle the results from the scan.
     */
    internal abstract val resultListener: ScanResultListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Stats.startScan()

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
            ensureCameraPermission()
        }
    }

    protected open fun hideSystemUi() {
        // Prevent screenshots and keep the screen on while scanning.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE +
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_SECURE +
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
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
            .setTitle(R.string.stripe_error_camera_title)
            .setMessage(R.string.stripe_error_camera_unsupported)
            .setPositiveButton(R.string.stripe_error_camera_acknowledge_button) { _, _ ->
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
        Log.e(Config.logTag, "Canceling scan due to error", cause)
        runBlocking { scanStat.trackResult("scan_failure") }
        resultListener.failed(cause)
        closeScanner()
    }

    /**
     * Cancel the scan when the user presses back.
     */
    override fun onBackPressed() {
        runBlocking { scanStat.trackResult("user_canceled") }
        resultListener.userCanceled(CancellationReason.Back)
        closeScanner()
    }

    /**
     * The scan has been closed by the user.
     */
    protected open fun userClosedScanner() {
        runBlocking { scanStat.trackResult("user_canceled") }
        resultListener.userCanceled(CancellationReason.Closed)
        closeScanner()
    }

    /**
     * The camera permission was denied.
     */
    override fun onUserDeniedCameraPermission() {
        runBlocking { scanStat.trackResult("user_canceled") }
        resultListener.userCanceled(CancellationReason.CameraPermissionDenied)
        closeScanner()
    }

    /**
     * The user cannot scan the required object.
     */
    protected open fun userCannotScan() {
        runBlocking { scanStat.trackResult("user_missing_card") }
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

    override fun onCameraReady() {
        cameraAdapter.bindToLifecycle(this)

        val torchStat = Stats.trackTask("torch_supported")
        cameraAdapter.withFlashSupport {
            launch { torchStat.trackResult(if (it) "supported" else "unsupported") }
            setFlashlightState(cameraAdapter.isTorchOn())
            onFlashSupported(it)
        }

        // TODO: this should probably be reported as part of scanstats, but is not yet supported
        cameraAdapter.withSupportsMultipleCameras {
            onSupportsMultipleCameras(it)
        }

        launch { onCameraStreamAvailable(cameraAdapter.getImageStream()) }
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
    internal open fun buildCameraAdapter(): CameraAdapter<CameraPreviewImage<Bitmap>> =
        getCameraAdapter(
            activity = this,
            previewView = previewFrame,
            minimumResolution = minimumAnalysisResolution,
            cameraErrorListener = cameraErrorListener,
        )

    protected abstract val previewFrame: ViewGroup

    protected abstract val minimumAnalysisResolution: Size

    /**
     * A stream of images from the camera is available to be processed.
     */
    protected abstract suspend fun onCameraStreamAvailable(
        cameraStream: Flow<CameraPreviewImage<Bitmap>>,
    )
}
