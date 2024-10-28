package com.stripe.android.stripecardscan.cardscan

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Bundle
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.annotation.RestrictTo
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.framework.Stats
import com.stripe.android.camera.scanui.ScanErrorListener
import com.stripe.android.camera.scanui.ScanState
import com.stripe.android.camera.scanui.SimpleScanStateful
import com.stripe.android.camera.scanui.ViewFinderBackground
import com.stripe.android.camera.scanui.util.asRect
import com.stripe.android.camera.scanui.util.setDrawable
import com.stripe.android.camera.scanui.util.startAnimation
import com.stripe.android.stripecardscan.R
import com.stripe.android.stripecardscan.camera.getScanCameraAdapter
import com.stripe.android.stripecardscan.cardscan.exception.UnknownScanException
import com.stripe.android.stripecardscan.cardscan.result.MainLoopAggregator
import com.stripe.android.stripecardscan.cardscan.result.MainLoopState
import com.stripe.android.stripecardscan.databinding.StripeActivityCardscanBinding
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import com.stripe.android.stripecardscan.scanui.CancellationReason
import com.stripe.android.stripecardscan.scanui.ScanActivity
import com.stripe.android.stripecardscan.scanui.ScanResultListener
import com.stripe.android.stripecardscan.scanui.util.getColorByRes
import com.stripe.android.stripecardscan.scanui.util.hide
import com.stripe.android.stripecardscan.scanui.util.setVisible
import com.stripe.android.stripecardscan.scanui.util.show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean

internal const val INTENT_PARAM_REQUEST = "request"
internal const val INTENT_PARAM_RESULT = "result"

private val MINIMUM_RESOLUTION = Size(1067, 600) // minimum size of OCR

internal interface CardScanResultListener : ScanResultListener {

    /**
     * The scan completed.
     */
    fun cardScanComplete(card: ScannedCard)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class CardScanState(isFinal: Boolean) : ScanState(isFinal) {
    data object NotFound : CardScanState(isFinal = false)
    data object Found : CardScanState(isFinal = false)
    data object Correct : CardScanState(isFinal = true)
}

internal class CardScanActivity : ScanActivity(), SimpleScanStateful<CardScanState> {

    override val minimumAnalysisResolution = MINIMUM_RESOLUTION

    private val viewBinding by lazy {
        StripeActivityCardscanBinding.inflate(layoutInflater)
    }

    override val previewFrame: ViewGroup by lazy {
        viewBinding.cameraView.previewFrame
    }

    private val viewFinderWindow: View by lazy {
        viewBinding.cameraView.viewFinderWindowView
    }

    private val viewFinderBorder: ImageView by lazy {
        viewBinding.cameraView.viewFinderBorderView
    }

    private val viewFinderBackground: ViewFinderBackground by lazy {
        viewBinding.cameraView.viewFinderBackgroundView
    }

    private val hasPreviousValidResult = AtomicBoolean(false)

    override var scanState: CardScanState? = CardScanState.NotFound

    override var scanStatePrevious: CardScanState? = null

    override val scanErrorListener: ScanErrorListener = ScanErrorListener()

    override val cameraAdapterBuilder = ::getScanCameraAdapter

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
            }

            override fun userCanceled(reason: CancellationReason) {
                val intent = Intent()
                    .putExtra(
                        INTENT_PARAM_RESULT,
                        CardScanSheetResult.Canceled(reason)
                    )
                setResult(RESULT_CANCELED, intent)
            }

            override fun failed(cause: Throwable?) {
                val intent = Intent()
                    .putExtra(
                        INTENT_PARAM_RESULT,
                        CardScanSheetResult.Failed(cause ?: UnknownScanException())
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
                result: MainLoopAggregator.FinalResult
            ) {
                launch(Dispatchers.Main) {
                    changeScanState(CardScanState.Correct)
                    cameraAdapter.unbindFromLifecycle(this@CardScanActivity)
                    resultListener.cardScanComplete(ScannedCard(result.pan))
                    scanStat.trackResult("card_scanned")
                    closeScanner()
                }.let { }
            }

            /**
             * An interim result was received from the result aggregator.
             */
            override suspend fun onInterimResult(
                result: MainLoopAggregator.InterimResult
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

        onBackPressedDispatcher.addCallback {
            runBlocking { scanStat.trackResult("user_canceled") }
            resultListener.userCanceled(CancellationReason.Back)
            closeScanner()
        }

        viewBinding.closeButton.setOnClickListener {
            userClosedScanner()
        }
        viewBinding.torchButton.setOnClickListener {
            toggleFlashlight()
        }
        viewBinding.swapCameraButton.setOnClickListener {
            toggleCamera()
        }
        viewFinderBorder.setOnTouchListener { _, e ->
            setFocus(
                PointF(
                    e.x + viewFinderBorder.left,
                    e.y + viewFinderBorder.top
                )
            )
            true
        }

        displayState(requireNotNull(scanState), scanStatePrevious)
    }

    override fun onResume() {
        super.onResume()
        scanState = CardScanState.NotFound
    }

    override fun onDestroy() {
        scanFlow.cancelFlow()
        super.onDestroy()
    }

    override fun onFlashSupported(supported: Boolean) {
        viewBinding.torchButton.setVisible(supported)
    }

    override fun onSupportsMultipleCameras(supported: Boolean) {
        viewBinding.swapCameraButton.setVisible(supported)
    }

    override fun onCameraReady() {
        previewFrame.post {
            viewFinderBackground
                .setViewFinderRect(viewFinderWindow.asRect())
            startCameraAdapter()
        }
    }

    /**
     * Once the camera stream is available, start processing images.
     */
    override suspend fun onCameraStreamAvailable(cameraStream: Flow<CameraPreviewImage<Bitmap>>) {
        scanFlow.startFlow(
            context = this,
            imageStream = cameraStream,
            viewFinder = viewBinding.cameraView.viewFinderWindowView.asRect(),
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

    override fun displayState(newState: CardScanState, previousState: CardScanState?) {
        when (newState) {
            is CardScanState.NotFound -> {
                viewFinderBackground
                    .setBackgroundColor(getColorByRes(R.color.stripeNotFoundBackground))
                viewFinderWindow
                    .setBackgroundResource(R.drawable.stripe_card_background_not_found)
                viewFinderBorder.startAnimation(R.drawable.stripe_card_border_not_found)
                viewBinding.instructions.setText(R.string.stripe_card_scan_instructions)
            }
            is CardScanState.Found -> {
                viewFinderBackground
                    .setBackgroundColor(getColorByRes(R.color.stripeFoundBackground))
                viewFinderWindow
                    .setBackgroundResource(R.drawable.stripe_card_background_found)
                viewFinderBorder.startAnimation(R.drawable.stripe_card_border_found)
                viewBinding.instructions.setText(R.string.stripe_card_scan_instructions)
                viewBinding.instructions.show()
            }
            is CardScanState.Correct -> {
                viewFinderBackground
                    .setBackgroundColor(getColorByRes(R.color.stripeCorrectBackground))
                viewFinderWindow
                    .setBackgroundResource(R.drawable.stripe_card_background_correct)
                viewFinderBorder.startAnimation(R.drawable.stripe_card_border_correct)
                viewBinding.instructions.hide()
            }
        }
    }

    override fun closeScanner() {
        super.closeScanner()
    }
}
