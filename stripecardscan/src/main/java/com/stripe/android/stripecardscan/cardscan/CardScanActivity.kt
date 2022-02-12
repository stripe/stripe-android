package com.stripe.android.stripecardscan.cardscan

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Bundle
import android.util.Size
import android.view.ViewGroup
import androidx.core.view.updateMargins
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.framework.Stats
import com.stripe.android.camera.scanui.util.asRect
import com.stripe.android.camera.scanui.util.setDrawable
import com.stripe.android.camera.scanui.util.startAnimation
import com.stripe.android.stripecardscan.R
import com.stripe.android.stripecardscan.cardscan.exception.InvalidStripePublishableKeyException
import com.stripe.android.stripecardscan.cardscan.exception.UnknownScanException
import com.stripe.android.stripecardscan.cardscan.result.MainLoopAggregator
import com.stripe.android.stripecardscan.cardscan.result.MainLoopState
import com.stripe.android.stripecardscan.databinding.ActivityCardscanBinding
import com.stripe.android.stripecardscan.framework.api.dto.ScanStatistics
import com.stripe.android.stripecardscan.framework.api.uploadScanStatsOCR
import com.stripe.android.stripecardscan.framework.util.AppDetails
import com.stripe.android.stripecardscan.framework.util.Device
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import com.stripe.android.stripecardscan.scanui.CancellationReason
import com.stripe.android.stripecardscan.scanui.ScanActivity
import com.stripe.android.stripecardscan.scanui.ScanErrorListener
import com.stripe.android.stripecardscan.scanui.ScanResultListener
import com.stripe.android.stripecardscan.scanui.ScanState
import com.stripe.android.stripecardscan.scanui.SimpleScanStateful
import com.stripe.android.stripecardscan.scanui.util.getColorByRes
import com.stripe.android.stripecardscan.scanui.util.getFloatResource
import com.stripe.android.stripecardscan.scanui.util.hide
import com.stripe.android.stripecardscan.scanui.util.setVisible
import com.stripe.android.stripecardscan.scanui.util.show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.math.roundToInt

internal const val INTENT_PARAM_REQUEST = "request"
internal const val INTENT_PARAM_RESULT = "result"

private val MINIMUM_RESOLUTION = Size(1067, 600) // minimum size of OCR

internal interface CardScanResultListener : ScanResultListener {

    /**
     * The scan completed.
     */
    fun cardScanComplete(card: ScannedCard)
}

internal sealed class CardScanState(isFinal: Boolean) : ScanState(isFinal) {
    object NotFound : CardScanState(isFinal = false)
    object Found : CardScanState(isFinal = false)
    object Correct : CardScanState(isFinal = true)
}

internal class CardScanActivity : ScanActivity(), SimpleScanStateful<CardScanState> {

    override val minimumAnalysisResolution = MINIMUM_RESOLUTION

    private val viewBinding by lazy {
        ActivityCardscanBinding.inflate(layoutInflater)
    }

    override val previewFrame: ViewGroup by lazy {
        viewBinding.previewFrame
    }

    private val params: CardScanSheetParams by lazy {
        intent.getParcelableExtra(INTENT_PARAM_REQUEST)
            ?: CardScanSheetParams("")
    }

    private val hasPreviousValidResult = AtomicBoolean(false)

    override var scanState: ScanState = CardScanState.NotFound

    override var scanStatePrevious: ScanState? = null

    override val scanErrorListener: ScanErrorListener = ScanErrorListener()

    /**
     * The listener which handles results from the scan.
     */
    override val resultListener: CardScanResultListener =
        object : CardScanResultListener {

            override fun cardScanComplete(card: ScannedCard) {
                val intent = Intent()
                    .putExtra(
                        INTENT_PARAM_RESULT,
                        CardScanSheetResult.Completed(
                            ScannedCard(
                                pan = card.pan
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
                    changeScanState(CardScanState.Correct)
                    cameraAdapter.unbindFromLifecycle(this@CardScanActivity)
                    resultListener.cardScanComplete(ScannedCard(result.pan))
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
                    is MainLoopState.Initial -> changeScanState(CardScanState.NotFound)
                    is MainLoopState.OcrFound -> changeScanState(CardScanState.Found)
                    is MainLoopState.Finished -> changeScanState(CardScanState.Correct)
                }
            }.let { }

            override suspend fun onReset() = launch(Dispatchers.Main) {
                changeScanState(CardScanState.NotFound)
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

        setupViewFinderConstraints()

        viewBinding.closeButton.setOnClickListener {
            userClosedScanner()
        }
        viewBinding.torchButton.setOnClickListener {
            toggleFlashlight()
        }
        viewBinding.swapCameraButton.setOnClickListener {
            toggleCamera()
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
    }

    override fun onResume() {
        super.onResume()
        scanState = CardScanState.NotFound
    }

    override fun onDestroy() {
        scanFlow.cancelFlow()
        super.onDestroy()
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

    override fun onFlashSupported(supported: Boolean) {
        viewBinding.torchButton.setVisible(supported)
    }

    override fun onSupportsMultipleCameras(supported: Boolean) {
        viewBinding.swapCameraButton.setVisible(supported)
    }

    override fun onCameraReady() {
        viewBinding.viewFinderBackground
            .setViewFinderRect(viewBinding.viewFinderWindow.asRect())
        startCameraAdapter()
    }

    /**
     * Once the camera stream is available, start processing images.
     */
    override suspend fun onCameraStreamAvailable(cameraStream: Flow<CameraPreviewImage<Bitmap>>) {
        scanFlow.startFlow(
            context = this,
            imageStream = cameraStream,
            viewFinder = viewBinding.viewFinderWindow.asRect(),
            lifecycleOwner = this,
            coroutineScope = this,
            parameters = null
        )
    }

    /**
     * Called when the flashlight state has changed.
     */
    override fun onFlashlightStateChanged(flashlightOn: Boolean) {
        if (flashlightOn) {
            viewBinding.torchButton.setDrawable(R.drawable.stripe_flash_on_dark)
        } else {
            viewBinding.torchButton.setDrawable(R.drawable.stripe_flash_off_dark)
        }
    }

    private fun ensureValidParams() = when {
        params.stripePublishableKey.isEmpty() -> {
            scanFailure(InvalidStripePublishableKeyException("Missing publishable key"))
            false
        }
        else -> true
    }

    override fun displayState(newState: ScanState, previousState: ScanState?) {
        when (newState) {
            is CardScanState.NotFound -> {
                viewBinding.viewFinderBackground
                    .setBackgroundColor(getColorByRes(R.color.stripeNotFoundBackground))
                viewBinding.viewFinderWindow
                    .setBackgroundResource(R.drawable.stripe_card_background_not_found)
                viewBinding.viewFinderBorder.startAnimation(R.drawable.stripe_card_border_not_found)
                viewBinding.instructions.setText(R.string.stripe_card_scan_instructions)
            }
            is CardScanState.Found -> {
                viewBinding.viewFinderBackground
                    .setBackgroundColor(getColorByRes(R.color.stripeFoundBackground))
                viewBinding.viewFinderWindow
                    .setBackgroundResource(R.drawable.stripe_card_background_found)
                viewBinding.viewFinderBorder.startAnimation(R.drawable.stripe_card_border_found)
                viewBinding.instructions.setText(R.string.stripe_card_scan_instructions)
                viewBinding.instructions.show()
            }
            is CardScanState.Correct -> {
                viewBinding.viewFinderBackground
                    .setBackgroundColor(getColorByRes(R.color.stripeCorrectBackground))
                viewBinding.viewFinderWindow
                    .setBackgroundResource(R.drawable.stripe_card_background_correct)
                viewBinding.viewFinderBorder.startAnimation(R.drawable.stripe_card_border_correct)
                viewBinding.instructions.hide()
            }
        }
    }

    override fun closeScanner() {
        uploadScanStatsOCR(
            stripePublishableKey = params.stripePublishableKey,
            instanceId = Stats.instanceId,
            scanId = Stats.scanId,
            device = Device.fromContext(this),
            appDetails = AppDetails.fromContext(this),
            scanStatistics = ScanStatistics.fromStats()
        )
        super.closeScanner()
    }
}
