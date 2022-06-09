package com.stripe.android.stripecardscan.cardscan

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.updateMargins
import androidx.fragment.app.setFragmentResult
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.framework.Stats
import com.stripe.android.camera.scanui.ScanErrorListener
import com.stripe.android.camera.scanui.SimpleScanStateful
import com.stripe.android.camera.scanui.util.asRect
import com.stripe.android.camera.scanui.util.startAnimation
import com.stripe.android.stripecardscan.R
import com.stripe.android.stripecardscan.cardscan.exception.InvalidStripePublishableKeyException
import com.stripe.android.stripecardscan.cardscan.exception.UnknownScanException
import com.stripe.android.stripecardscan.cardscan.result.MainLoopAggregator
import com.stripe.android.stripecardscan.cardscan.result.MainLoopState
import com.stripe.android.stripecardscan.databinding.FragmentCardscanBinding
import com.stripe.android.stripecardscan.framework.api.dto.ScanStatistics
import com.stripe.android.stripecardscan.framework.api.uploadScanStatsOCR
import com.stripe.android.stripecardscan.framework.util.AppDetails
import com.stripe.android.stripecardscan.framework.util.Device
import com.stripe.android.stripecardscan.framework.util.ScanConfig
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import com.stripe.android.stripecardscan.scanui.CancellationReason
import com.stripe.android.stripecardscan.scanui.ScanFragment
import com.stripe.android.stripecardscan.scanui.util.getColorByRes
import com.stripe.android.stripecardscan.scanui.util.getFloatResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.math.roundToInt

private val MINIMUM_RESOLUTION = Size(1067, 600) // minimum size of OCR
const val CARD_SCAN_FRAGMENT_REQUEST_KEY = "CardScanRequestKey"
const val CARD_SCAN_FRAGMENT_BUNDLE_KEY = "CardScanBundleKey"
const val CARD_SCAN_FRAGMENT_PARAMS_KEY = "CardScanParamsKey"

class CardScanFragment : ScanFragment(), SimpleScanStateful<CardScanState> {

    override val minimumAnalysisResolution = MINIMUM_RESOLUTION

    private lateinit var viewBinding: FragmentCardscanBinding

    override val instructionsText: TextView by lazy { viewBinding.instructions }

    override val previewFrame: ViewGroup by lazy { viewBinding.previewFrame }

    private val params: CardScanSheetParams by lazy {
        arguments?.getParcelable(CARD_SCAN_FRAGMENT_PARAMS_KEY) ?: CardScanSheetParams("")
    }

    private val hasPreviousValidResult = AtomicBoolean(false)

    override var scanState: CardScanState? = CardScanState.NotFound

    override var scanStatePrevious: CardScanState? = null

    override val scanErrorListener: ScanErrorListener = ScanErrorListener()

    /**
     * The listener which handles results from the scan.
     */
    override val resultListener: CardScanResultListener =
        object : CardScanResultListener {

            override fun cardScanComplete(card: ScannedCard) {
                setFragmentResult(
                    CARD_SCAN_FRAGMENT_REQUEST_KEY,
                    bundleOf(
                        CARD_SCAN_FRAGMENT_BUNDLE_KEY to CardScanSheetResult.Completed(
                            ScannedCard(
                                pan = card.pan
                            )
                        )
                    )
                )
                closeScanner()
            }

            override fun userCanceled(reason: CancellationReason) {
                setFragmentResult(
                    CARD_SCAN_FRAGMENT_REQUEST_KEY,
                    bundleOf(
                        CARD_SCAN_FRAGMENT_BUNDLE_KEY to CardScanSheetResult.Canceled(reason)
                    )
                )
            }

            override fun failed(cause: Throwable?) {
                setFragmentResult(
                    CARD_SCAN_FRAGMENT_REQUEST_KEY,
                    bundleOf(
                        CARD_SCAN_FRAGMENT_BUNDLE_KEY to
                            CardScanSheetResult.Failed(
                                cause ?: UnknownScanException()
                            )
                    )
                )
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
                launch(Dispatchers.Main) {
                    changeScanState(CardScanState.Correct)
                    activity?.let { cameraAdapter.unbindFromLifecycle(it) }
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
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentCardscanBinding.inflate(inflater, container, false)

        setupViewFinderConstraints()

        viewBinding.closeButton.setOnClickListener {
            userClosedScanner()
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

        displayState(requireNotNull(scanState), scanStatePrevious)
        return viewBinding.root
    }

    override fun onStart() {
        super.onStart()
        if (!ensureValidParams()) {
            return
        }
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
     * Set up viewFinderWindowView and viewFinderBorderView centered with predefined margins
     */
    private fun setupViewFinderConstraints() {
        val screenSize = Resources.getSystem().displayMetrics.let {
            Size(it.widthPixels, it.heightPixels)
        }

        val viewFinderMargin = (
            min(screenSize.width, screenSize.height) *
                (context?.getFloatResource(R.dimen.stripeViewFinderMargin) ?: 0F)
            ).roundToInt()

        listOf(viewBinding.viewFinderWindow, viewBinding.viewFinderBorder).forEach { view ->
            (view.layoutParams as ViewGroup.MarginLayoutParams)
                .updateMargins(
                    viewFinderMargin, viewFinderMargin, viewFinderMargin, viewFinderMargin
                )
        }

        viewBinding.viewFinderBackground.setViewFinderRect(viewBinding.viewFinderWindow.asRect())
    }

    override fun onFlashSupported(supported: Boolean) {}

    override fun onSupportsMultipleCameras(supported: Boolean) {}

    /**
     * Prepare to start the camera. Once the camera is ready, [onCameraReady] must be called.
     */
    override fun prepareCamera(onCameraReady: () -> Unit) {
        viewBinding.previewFrame.post {
            viewBinding.viewFinderBackground
                .setViewFinderRect(viewBinding.viewFinderWindow.asRect())
            onCameraReady()
        }
    }

    /**
     * Once the camera stream is available, start processing images.
     */
    override suspend fun onCameraStreamAvailable(cameraStream: Flow<CameraPreviewImage<Bitmap>>) {
        context?.let {
            scanFlow.startFlow(
                context = it,
                imageStream = cameraStream,
                viewFinder = viewBinding.viewFinderWindow.asRect(),
                lifecycleOwner = this,
                coroutineScope = this,
                parameters = null
            )
        }
    }

    /**
     * Called when the flashlight state has changed.
     */
    override fun onFlashlightStateChanged(flashlightOn: Boolean) {}

    private fun ensureValidParams() = when {
        params.stripePublishableKey.isEmpty() -> {
            scanFailure(InvalidStripePublishableKeyException("Missing publishable key"))
            false
        }
        else -> true
    }

    override fun displayState(newState: CardScanState, previousState: CardScanState?) {
        when (newState) {
            is CardScanState.NotFound, CardScanState.Found -> {
                context?.let {
                    viewBinding.viewFinderBackground
                        .setBackgroundColor(
                            it.getColorByRes(R.color.stripeNotFoundBackground)
                        )
                }
                viewBinding.viewFinderWindow
                    .setBackgroundResource(R.drawable.stripe_card_background_not_found)
                viewBinding.viewFinderBorder
                    .startAnimation(R.drawable.stripe_paymentsheet_card_border_not_found)
            }
            is CardScanState.Correct -> {
                context?.let {
                    viewBinding.viewFinderBackground
                        .setBackgroundColor(
                            it.getColorByRes(R.color.stripeCorrectBackground)
                        )
                }
                viewBinding.viewFinderWindow
                    .setBackgroundResource(R.drawable.stripe_card_background_correct)
                viewBinding.viewFinderBorder.startAnimation(R.drawable.stripe_card_border_correct)
            }
        }
    }

    override fun closeScanner() {
        uploadScanStatsOCR(
            stripePublishableKey = params.stripePublishableKey,
            instanceId = Stats.instanceId,
            scanId = Stats.scanId,
            device = Device.fromContext(context),
            appDetails = AppDetails.fromContext(context),
            scanStatistics = ScanStatistics.fromStats(),
            scanConfig = ScanConfig(0),
        )
        super.closeScanner()
    }
}
