package com.stripe.android.stripecardscan.cardscan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.updateMargins
import com.stripe.android.camera.CameraAdapter
import com.stripe.android.camera.CameraErrorListener
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.framework.AnalyzerLoopErrorListener
import com.stripe.android.camera.framework.Stats
import com.stripe.android.stripecardscan.R
import com.stripe.android.stripecardscan.camera.getCameraAdapter
import com.stripe.android.stripecardscan.cardscan.exception.InvalidStripePublishableKeyException
import com.stripe.android.stripecardscan.cardscan.exception.UnknownScanException
import com.stripe.android.stripecardscan.cardscan.result.MainLoopAggregator
import com.stripe.android.stripecardscan.cardscan.result.MainLoopState
import com.stripe.android.stripecardscan.cardscan.util.asRect
import com.stripe.android.stripecardscan.cardscan.util.getColorByRes
import com.stripe.android.stripecardscan.cardscan.util.getFloatResource
import com.stripe.android.stripecardscan.cardscan.util.hide
import com.stripe.android.stripecardscan.cardscan.util.setDrawable
import com.stripe.android.stripecardscan.cardscan.util.setVisible
import com.stripe.android.stripecardscan.cardscan.util.show
import com.stripe.android.stripecardscan.cardscan.util.startAnimation
import com.stripe.android.stripecardscan.databinding.ActivityCardscanBinding
import com.stripe.android.stripecardscan.framework.Config
import com.stripe.android.stripecardscan.framework.StorageFactory
import com.stripe.android.stripecardscan.framework.util.getAppPackageName
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.math.min
import kotlin.math.roundToInt

internal const val INTENT_PARAM_REQUEST = "request"
internal const val INTENT_PARAM_RESULT = "result"

private val MINIMUM_RESOLUTION = Size(1067, 600) // minimum size of OCR
private const val PERMISSION_RATIONALE_SHOWN = "permission_rationale_shown"

sealed interface CancellationReason : Parcelable {

    @Parcelize
    object Closed : CancellationReason

    @Parcelize
    object Back : CancellationReason

    @Parcelize
    object CameraPermissionDenied : CancellationReason
}

internal interface ScanResultListener {

    /**
     * The scan completed.
     */
    fun cardScanComplete(pan: String)
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
internal open class CameraErrorListenerImpl(
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

@Keep
internal open class CardScanActivity : AppCompatActivity(), CoroutineScope {

    companion object {
        const val PERMISSION_REQUEST_CODE = 1200
    }

    /**
     * The state of the scan flow. This can be expanded if [displayState] is overridden to handle
     * the added states.
     */
    abstract class ScanState(val isFinal: Boolean) {
        object NotFound : ScanState(isFinal = false)
        object FoundShort : ScanState(isFinal = false)
        object FoundLong : ScanState(isFinal = false)
        object Correct : ScanState(isFinal = true)
    }

    private val viewBinding by lazy {
        ActivityCardscanBinding.inflate(layoutInflater)
    }

    private val params: CardScanSheetParams by lazy {
        intent.getParcelableExtra(INTENT_PARAM_REQUEST)
            ?: CardScanSheetParams("")
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main

    private val scanStat = Stats.trackTask("scan_activity")
    private val permissionStat = Stats.trackTask("camera_permission")

    private val storage by lazy {
        StorageFactory.getStorageInstance(this, "scan_camera_permissions")
    }

    private var isFlashlightOn: Boolean = false

    private val cameraAdapter by lazy { buildCameraAdapter() }
    private val cameraErrorListener by lazy {
        CameraErrorListenerImpl(this) { t -> scanFailure(t) }
    }

    private var scanStatePrevious: ScanState? = null
    private var scanState: ScanState = ScanState.NotFound

    private val hasPreviousValidResult = AtomicBoolean(false)

    /**
     * The listener which handles results from the scan.
     */
    private val resultListener: ScanResultListener =
        object : ScanResultListener {

            override fun cardScanComplete(pan: String) {
                val intent = Intent()
                    .putExtra(
                        INTENT_PARAM_RESULT,
                        CardScanSheetResult.Completed(
                            ScannedCard(
                                pan = pan
                            )
                        )
                    )
                setResult(RESULT_OK, intent)
                closeScanner()
            }

            override fun userCanceled(reason: CancellationReason) {
                val intent = Intent()
                    .putExtra(
                        INTENT_PARAM_RESULT,
                        CardScanSheetResult.Canceled(reason),
                    )
                setResult(RESULT_CANCELED, intent)
            }

            override fun failed(cause: Throwable?) {
                val intent = Intent()
                    .putExtra(
                        INTENT_PARAM_RESULT,
                        CardScanSheetResult.Failed(cause ?: UnknownScanException()),
                    )
                setResult(RESULT_CANCELED, intent)
            }
        }

    /**
     * The flow used to scan an item.
     */
    private val scanFlow: CardScanFlow by lazy {
        object : CardScanFlow(scanErrorListener) {
            /**
             * A final result was received from the aggregator. Set the result from this activity.
             */
            override suspend fun onResult(
                result: MainLoopAggregator.FinalResult,
            ) {
                super.onResult(result)

                launch(Dispatchers.Main) {
                    changeScanState(ScanState.Correct)
                    cameraAdapter.unbindFromLifecycle(this@CardScanActivity)
                    resultListener.cardScanComplete(result.pan)
                }.let { }
            }

            /**
             * An interim result was received from the result aggregator.
             */
            override suspend fun onInterimResult(
                result: MainLoopAggregator.InterimResult,
            ) = launch(Dispatchers.Main) {
                if (
                    result.state is MainLoopState.OcrFound &&
                    !hasPreviousValidResult.getAndSet(true)
                ) {
                    scanStat.trackResult("ocr_pan_observed")
                }

                when (result.state) {
                    is MainLoopState.Initial -> changeScanState(ScanState.NotFound)
                    is MainLoopState.OcrFound -> changeScanState(ScanState.FoundLong)
                    is MainLoopState.Finished -> changeScanState(ScanState.Correct)
                }
            }.let { }

            override suspend fun onReset() = launch(Dispatchers.Main) {
                changeScanState(ScanState.NotFound)
            }.let { }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        if (!ensureValidParams()) {
            return
        }

        if (!CameraAdapter.isCameraSupported(this)) {
            showCameraNotSupportedDialog()
        }

        setupViewFinderConstraints()

        viewBinding.closeButton.setOnClickListener {
            userClosedScanner()
        }
        viewBinding.torchButton.setOnClickListener {
            isFlashlightOn = !isFlashlightOn
            setFlashlightState(isFlashlightOn)
        }
        viewBinding.swapCameraButton.setOnClickListener {
            cameraAdapter.changeCamera()
        }
        viewBinding.viewFinderBorder.setOnTouchListener { _, e ->
            setFocus(
                PointF(
                    e.x + viewBinding.viewFinderWindow.left,
                    e.y + viewBinding.viewFinderWindow.top
                )
            )
            true
        }

        displayState(scanState, scanStatePrevious)
        Stats.startScan()
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

    private fun hideSystemUi() {
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
     * Cancel the scan when the user presses back.
     */
    override fun onBackPressed() {
        runBlocking { scanStat.trackResult("user_canceled") }
        resultListener.userCanceled(CancellationReason.Back)
        closeScanner()
    }

    /**
     * Handle permission status changes. If the camera permission has been granted, start it. If
     * not, show a dialog.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            when (grantResults[0]) {
                PackageManager.PERMISSION_GRANTED -> {
                    launch { permissionStat.trackResult("success") }
                    prepareCamera { onCameraReady() }
                }
                else -> {
                    launch { permissionStat.trackResult("failure") }
                    userDeniedCameraPermission()
                }
            }
        }
    }

    /**
     * Set up viewFinderWindowView and viewFinderBorderView centered with predefined margins
     */
    private fun setupViewFinderConstraints() {

        val screenSize = Resources.getSystem().displayMetrics.let {
            Size(it.widthPixels, it.heightPixels)
        }
        val viewFinderMargin = (
            min(screenSize.width, screenSize.height) *
                getFloatResource(R.dimen.stripeViewFinderMargin)
            ).roundToInt()

        listOf(viewBinding.viewFinderWindow, viewBinding.viewFinderBorder).forEach { view ->
            (view.layoutParams as ViewGroup.MarginLayoutParams)
                .updateMargins(
                    viewFinderMargin, viewFinderMargin, viewFinderMargin, viewFinderMargin
                )
        }

        viewBinding.viewFinderBackground.setViewFinderRect(viewBinding.viewFinderWindow.asRect())
    }

    /**
     * Ensure that the camera permission is available. If so, start the camera. If not, request it.
     */
    private fun ensurePermissionAndStartCamera() = when {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED -> {
            launch { permissionStat.trackResult("success") }
            prepareCamera { onCameraReady() }
        }
        ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.CAMERA,
        ) -> showPermissionRationaleDialog()
        storage.getBoolean(
            PERMISSION_RATIONALE_SHOWN,
            false,
        ) -> showPermissionDeniedDialog()
        else -> requestCameraPermission()
    }

    /**
     * Once the camera stream is available, start processing images.
     */
    private fun onCameraStreamAvailable(cameraStream: Flow<CameraPreviewImage<Bitmap>>) {
        scanFlow.startFlow(
            context = this,
            imageStream = cameraStream,
            viewFinder = viewBinding.viewFinderWindow.asRect(),
            lifecycleOwner = this,
            coroutineScope = this
        )
    }

    /**
     * Show a dialog explaining that the camera is not available.
     */
    private fun showCameraNotSupportedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.stripe_error_camera_title)
            .setMessage(R.string.stripe_error_camera_unsupported)
            .setPositiveButton(R.string.stripe_error_camera_acknowledge_button) { _, _ ->
                scanFailure()
            }
            .show()
    }

    /**
     * Show an explanation dialog for why we are requesting camera permissions.
     */
    private fun showPermissionRationaleDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.stripe_camera_permission_denied_message)
            .setPositiveButton(R.string.stripe_camera_permission_denied_ok) { _, _ ->
                requestCameraPermission()
            }
        builder.show()
        storage.storeValue(PERMISSION_RATIONALE_SHOWN, true)
    }

    /**
     * Show an explanation dialog for why we are requesting camera permissions when the permission
     * has been permanently denied.
     */
    private fun showPermissionDeniedDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.stripe_camera_permission_denied_message)
            .setPositiveButton(R.string.stripe_camera_permission_denied_ok) { _, _ ->
                storage.storeValue(PERMISSION_RATIONALE_SHOWN, false)
                openAppSettings()
            }
            .setNegativeButton(R.string.stripe_camera_permission_denied_cancel) { _, _ ->
                userDeniedCameraPermission()
            }
        builder.show()
    }

    /**
     * Request permission to use the camera.
     */
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE,
        )
    }

    /**
     * Open the settings for this app
     */
    private fun openAppSettings() {
        val intent = Intent()
            .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", getAppPackageName(this), null))
        startActivity(intent)
    }

    /**
     * Cancel scanning due to a camera error.
     */
    private fun scanFailure(cause: Throwable? = null) {
        Log.e(Config.logTag, "Canceling scan due to error", cause)
        runBlocking { scanStat.trackResult("scan_failure") }
        resultListener.failed(cause)
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
    protected open fun userDeniedCameraPermission() {
        runBlocking { scanStat.trackResult("user_canceled") }
        resultListener.userCanceled(CancellationReason.CameraPermissionDenied)
        closeScanner()
    }

    /**
     * Prepare to start the camera. Once the camera is ready, [onCameraReady] must be called.
     */
    private fun prepareCamera(onCameraReady: () -> Unit) {
        viewBinding.previewFrame.post {
            viewBinding.viewFinderBackground
                .setViewFinderRect(viewBinding.viewFinderWindow.asRect())
            onCameraReady()
        }
    }

    private fun onCameraReady() {
        cameraAdapter.bindToLifecycle(this)

        val torchStat = Stats.trackTask("torch_supported")
        cameraAdapter.withFlashSupport {
            launch { torchStat.trackResult(if (it) "supported" else "unsupported") }
            setFlashlightState(cameraAdapter.isTorchOn())
            viewBinding.torchButton.setVisible(it)
        }

        cameraAdapter.withSupportsMultipleCameras {
            viewBinding.swapCameraButton.setVisible(it)
        }

        launch { onCameraStreamAvailable(cameraAdapter.getImageStream()) }
    }

    /**
     * Turn the flashlight on or off.
     */
    private fun setFlashlightState(on: Boolean) {
        cameraAdapter.setTorchState(on)
        isFlashlightOn = on
        if (isFlashlightOn) {
            viewBinding.torchButton.setDrawable(R.drawable.stripe_flash_on_dark)
        } else {
            viewBinding.torchButton.setDrawable(R.drawable.stripe_flash_off_dark)
        }
    }

    /**
     * Set the focus of the camera.
     */
    private fun setFocus(point: PointF) {
        cameraAdapter.setFocus(point)
    }

    /**
     * Generate a camera adapter
     */
    private fun buildCameraAdapter(): CameraAdapter<CameraPreviewImage<Bitmap>> =
        getCameraAdapter(
            activity = this,
            previewView = viewBinding.previewFrame,
            minimumResolution = MINIMUM_RESOLUTION,
            cameraErrorListener = cameraErrorListener,
        )

    private fun ensureValidParams() = when {
        params.stripePublishableKey.isEmpty() -> {
            scanFailure(InvalidStripePublishableKeyException("Missing publishable key"))
            false
        }
//        params.cardImageVerificationIntentId.isEmpty() -> {
//            scanFailure(InvalidCivException("Missing card image verification ID"))
//            false
//        }
//        params.cardImageVerificationIntentSecret.isEmpty() -> {
//            scanFailure(InvalidCivException("Missing card image verification client secret"))
//            false
//        }
        else -> true
    }

    private fun closeScanner() {
//        uploadScanStats(
//            stripePublishableKey = params.stripePublishableKey,
//            civId = params.cardImageVerificationIntentId,
//            civSecret = params.cardImageVerificationIntentSecret,
//            instanceId = Stats.instanceId,
//            scanId = Stats.scanId,
//            device = Device.fromContext(this),
//            appDetails = AppDetails.fromContext(this),
//            scanStatistics = ScanStatistics.fromStats()
//        )
        setFlashlightState(false)
        finish()
    }

    private fun displayState(newState: ScanState, previousState: ScanState?) {
        when (newState) {
            is ScanState.NotFound -> {
                viewBinding.viewFinderBackground
                    .setBackgroundColor(getColorByRes(R.color.stripeNotFoundBackground))
                viewBinding.viewFinderWindow
                    .setBackgroundResource(R.drawable.stripe_card_background_not_found)
                viewBinding.viewFinderBorder.startAnimation(R.drawable.stripe_card_border_not_found)
                viewBinding.instructions.setText(R.string.stripe_card_scan_instructions)
            }
            is ScanState.FoundShort -> {
                viewBinding.viewFinderBackground
                    .setBackgroundColor(getColorByRes(R.color.stripeFoundBackground))
                viewBinding.viewFinderWindow
                    .setBackgroundResource(R.drawable.stripe_card_background_found)
                viewBinding.viewFinderBorder.startAnimation(R.drawable.stripe_card_border_found)
                viewBinding.instructions.setText(R.string.stripe_card_scan_instructions)
                viewBinding.instructions.show()
            }
            is ScanState.FoundLong -> {
                viewBinding.viewFinderBackground
                    .setBackgroundColor(getColorByRes(R.color.stripeFoundBackground))
                viewBinding.viewFinderWindow
                    .setBackgroundResource(R.drawable.stripe_card_background_found)
                viewBinding.viewFinderBorder
                    .startAnimation(R.drawable.stripe_card_border_found_long)
                viewBinding.instructions.setText(R.string.stripe_card_scan_instructions)
                viewBinding.instructions.show()
            }
            is ScanState.Correct -> {
                viewBinding.viewFinderBackground
                    .setBackgroundColor(getColorByRes(R.color.stripeCorrectBackground))
                viewBinding.viewFinderWindow
                    .setBackgroundResource(R.drawable.stripe_card_background_correct)
                viewBinding.viewFinderBorder.startAnimation(R.drawable.stripe_card_border_correct)
                viewBinding.instructions.hide()
            }
        }
    }

    private fun changeScanState(newState: ScanState): Boolean {
        if (newState == scanStatePrevious || scanStatePrevious?.isFinal == true) {
            return false
        }

        scanState = newState
        displayState(newState, scanStatePrevious)
        scanStatePrevious = newState
        return true
    }

    private val scanErrorListener = object : AnalyzerLoopErrorListener {
        override fun onAnalyzerFailure(t: Throwable): Boolean {
            Log.e(Config.logTag, "Error executing analyzer", t)
            return false
        }

        override fun onResultFailure(t: Throwable): Boolean {
            Log.e(Config.logTag, "Error executing result", t)
            return true
        }
    }
}
