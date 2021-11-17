package com.stripe.android.cardverificationsheet.cardverifyui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.Keep
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.stripe.android.cardverificationsheet.R
import com.stripe.android.cardverificationsheet.cardverifyui.exception.InvalidCivException
import com.stripe.android.cardverificationsheet.cardverifyui.exception.InvalidStripePublishableKeyException
import com.stripe.android.cardverificationsheet.cardverifyui.exception.StripeNetworkException
import com.stripe.android.cardverificationsheet.cardverifyui.exception.UnknownScanException
import com.stripe.android.cardverificationsheet.cardverifyui.result.MainLoopAggregator
import com.stripe.android.cardverificationsheet.cardverifyui.result.MainLoopState
import com.stripe.android.cardverificationsheet.framework.AggregateResultListener
import com.stripe.android.cardverificationsheet.framework.AnalyzerLoopErrorListener
import com.stripe.android.cardverificationsheet.framework.Config
import com.stripe.android.cardverificationsheet.framework.Stats
import com.stripe.android.cardverificationsheet.framework.api.NetworkResult
import com.stripe.android.cardverificationsheet.framework.api.dto.ScanStatistics
import com.stripe.android.cardverificationsheet.framework.api.getCardImageVerificationIntentDetails
import com.stripe.android.cardverificationsheet.framework.api.uploadSavedFrames
import com.stripe.android.cardverificationsheet.framework.api.uploadScanStats
import com.stripe.android.cardverificationsheet.framework.util.AppDetails
import com.stripe.android.cardverificationsheet.framework.util.Device
import com.stripe.android.cardverificationsheet.payment.card.CardIssuer
import com.stripe.android.cardverificationsheet.payment.card.ScannedCard
import com.stripe.android.cardverificationsheet.payment.card.getCardIssuer
import com.stripe.android.cardverificationsheet.payment.card.getIssuerByDisplayName
import com.stripe.android.cardverificationsheet.payment.card.isValidPanLastFour
import com.stripe.android.cardverificationsheet.payment.card.lastFour
import com.stripe.android.cardverificationsheet.scanui.CardVerificationSheetCancelationReason
import com.stripe.android.cardverificationsheet.scanui.ScanResultListener
import com.stripe.android.cardverificationsheet.scanui.SimpleScanActivity
import com.stripe.android.cardverificationsheet.scanui.util.getColorByRes
import com.stripe.android.cardverificationsheet.scanui.util.getDrawableByRes
import com.stripe.android.cardverificationsheet.scanui.util.hide
import com.stripe.android.cardverificationsheet.scanui.util.setTextSizeByRes
import com.stripe.android.cardverificationsheet.scanui.util.setVisible
import com.stripe.android.cardverificationsheet.scanui.util.show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal const val INTENT_PARAM_REQUEST = "request"
internal const val INTENT_PARAM_RESULT = "result"

@Keep
internal interface CardVerifyResultListener : ScanResultListener {

    /**
     * A payment card was successfully scanned.
     */
    fun cardVerificationComplete(pan: String)

    /**
     * A card was scanned and is ready to be verified.
     */
    fun cardScanned(pan: String, frames: Collection<SavedFrame>)
}

data class RequiredCardDetails(
    val cardIssuer: CardIssuer?,
    val lastFour: String?,
)

private val MINIMUM_RESOLUTION = Size(1067, 600) // minimum size of OCR

@Keep
open class CardVerifyActivity : SimpleScanActivity<RequiredCardDetails?>() {

    /**
     * The text view that lets a user indicate they do not have possession of the required card.
     */
    protected open val cannotScanTextView: TextView by lazy { TextView(this) }

    /**
     * The text view that informs the user which card must be scanned.
     */
    protected open val cardDescriptionTextView: TextView by lazy { TextView(this) }

    /**
     * And overlay to darken the screen during result processing.
     */
    protected open val processingOverlayView by lazy { View(this) }

    /**
     * The spinner indicating that results are processing.
     */
    protected open val processingSpinnerView by lazy { ProgressBar(this) }

    /**
     * The text indicating that results are processing
     */
    protected open val processingTextView by lazy { TextView(this) }

    private val params: CardVerificationSheetParams by lazy {
        intent.getParcelableExtra(INTENT_PARAM_REQUEST) ?: CardVerificationSheetParams("", "", "")
    }

    /**
     * The card issuer that must be scanned
     */
    private var requiredCardIssuer: CardIssuer? = null

    /**
     * The last four digits of the required card
     */
    private var requiredCardLastFour: String? = null

    /**
     * The listener which handles results from the scan.
     */
    override val resultListener: CardVerifyResultListener = object : CardVerifyResultListener {
        override fun cardVerificationComplete(pan: String) {
            val intent = Intent()
                .putExtra(
                    INTENT_PARAM_RESULT,
                    CardVerificationSheetResult.Completed(
                        ScannedCard(
                            pan = pan
                        )
                    )
                )
            setResult(Activity.RESULT_OK, intent)
        }

        override fun cardScanned(pan: String, frames: Collection<SavedFrame>) {
            launch {
                when (
                    val result = uploadSavedFrames(
                        stripePublishableKey = params.stripePublishableKey,
                        civId = params.cardImageVerificationIntentId,
                        civSecret = params.cardImageVerificationIntentSecret,
                        savedFrames = frames,
                    )
                ) {
                    is NetworkResult.Success ->
                        cardVerificationComplete(pan)
                    is NetworkResult.Error ->
                        scanFailure(StripeNetworkException(result.error.error.message))
                    is NetworkResult.Exception ->
                        scanFailure(result.exception)
                }
            }
        }

        override fun userCanceled(reason: CardVerificationSheetCancelationReason) {
            val intent = Intent()
                .putExtra(INTENT_PARAM_RESULT, CardVerificationSheetResult.Canceled(reason))
            setResult(Activity.RESULT_CANCELED, intent)
        }

        override fun failed(cause: Throwable?) {
            val intent = Intent()
                .putExtra(
                    INTENT_PARAM_RESULT,
                    CardVerificationSheetResult.Failed(cause ?: UnknownScanException()),
                )
            setResult(Activity.RESULT_CANCELED, intent)
        }
    }

    /**
     * The flow used to scan an item.
     */
    override val scanFlow: CardVerifyFlow by lazy {
        CardVerifyFlow(
            scanResultListener,
            scanErrorListener,
        )
    }

    private val hasPreviousValidResult = AtomicBoolean(false)

    override val minimumAnalysisResolution = MINIMUM_RESOLUTION

    /**
     * During on create
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureValidParams()) {
            return
        }

        deferredScanFlowParameters = async { getCivDetails() }

        launch(Dispatchers.Main) {
            onScanDetailsAvailable(deferredScanFlowParameters.await())
        }

        cannotScanTextView.setOnClickListener { userCannotScan() }
    }

    private fun ensureValidParams() = when {
        params.stripePublishableKey.isEmpty() -> {
            scanFailure(InvalidStripePublishableKeyException("Missing publishable key"))
            false
        }
        params.cardImageVerificationIntentId.isEmpty() -> {
            scanFailure(InvalidCivException("Missing card image verification ID"))
            false
        }
        params.cardImageVerificationIntentSecret.isEmpty() -> {
            scanFailure(InvalidCivException("Missing card image verification client secret"))
            false
        }
        else -> true
    }

    private suspend fun getCivDetails(): RequiredCardDetails? = when (
        val result = getCardImageVerificationIntentDetails(
            stripePublishableKey = params.stripePublishableKey,
            civId = params.cardImageVerificationIntentId,
            civSecret = params.cardImageVerificationIntentSecret,
        )
    ) {
        is NetworkResult.Success ->
            result.body.expectedCard?.let { expectedCard ->
                if (expectedCard.lastFour.isNullOrEmpty() || isValidPanLastFour(expectedCard.lastFour)) {
                    RequiredCardDetails(
                        getIssuerByDisplayName(expectedCard.issuer),
                        expectedCard.lastFour,
                    )
                } else {
                    launch(Dispatchers.Main) {
                        scanFailure(InvalidCivException("Invalid required card"))
                    }
                    null
                }
            }
        is NetworkResult.Error -> {
            launch(Dispatchers.Main) {
                scanFailure(StripeNetworkException("Unable to get CIV details"))
            }
            null
        }
        is NetworkResult.Exception -> {
            launch(Dispatchers.Main) {
                scanFailure(result.exception)
            }
            null
        }
    }

    private fun onScanDetailsAvailable(
        requiredCardDetails: RequiredCardDetails?,
    ) {
        if (requiredCardDetails != null) {
            this.requiredCardIssuer = requiredCardDetails.cardIssuer
            this.requiredCardLastFour = requiredCardDetails.lastFour

            cardDescriptionTextView.text = getString(
                R.string.stripe_card_description,
                requiredCardIssuer?.displayName ?: "",
                requiredCardLastFour
            )
        }
    }

    override fun addUiComponents() {
        super.addUiComponents()
        appendUiComponents(
            cannotScanTextView,
            cardDescriptionTextView,
            processingOverlayView,
            processingSpinnerView,
            processingTextView,
        )
    }

    override fun setupUiComponents() {
        super.setupUiComponents()

        setupCannotScanUi()
        setupCardDescriptionUi()
        setupProcessingOverlayViewUi()
        setupProcessingTextViewUi()
    }

    protected open fun setupCannotScanUi() {
        cannotScanTextView.text = getString(R.string.stripe_cannot_scan_card)
        cannotScanTextView.setTextSizeByRes(R.dimen.stripeCannotScanCardTextSize)
        cannotScanTextView.typeface = Typeface.create("sans-serif-thin", Typeface.BOLD)
        cannotScanTextView.gravity = Gravity.CENTER
        cannotScanTextView.setPadding(
            resources.getDimensionPixelSize(R.dimen.stripeButtonPadding),
            resources.getDimensionPixelSize(R.dimen.stripeButtonPadding),
            resources.getDimensionPixelSize(R.dimen.stripeButtonPadding),
            resources.getDimensionPixelSize(R.dimen.stripeButtonPadding),
        )

        cannotScanTextView.setVisible(Config.enableCannotScanButton)

        if (isBackgroundDark()) {
            cannotScanTextView.setTextColor(getColorByRes(R.color.stripeButtonDarkText))
            cannotScanTextView.background =
                getDrawableByRes(R.drawable.stripe_rounded_button_dark)
        } else {
            cannotScanTextView.setTextColor(getColorByRes(R.color.stripeButtonLightText))
            cannotScanTextView.background =
                getDrawableByRes(R.drawable.stripe_rounded_button_light)
        }
    }

    protected open fun setupCardDescriptionUi() {
        cardDescriptionTextView.setTextSizeByRes(R.dimen.stripeCardDescriptionTextSize)
        cardDescriptionTextView.typeface = Typeface.DEFAULT_BOLD
        cardDescriptionTextView.gravity = Gravity.CENTER

        if (isBackgroundDark()) {
            cardDescriptionTextView.setTextColor(
                getColorByRes(R.color.stripeCardDescriptionColorDark)
            )
        } else {
            cardDescriptionTextView.setTextColor(
                getColorByRes(R.color.stripeCardDescriptionColorLight)
            )
        }
    }

    protected open fun setupProcessingOverlayViewUi() {
        processingOverlayView.setBackgroundColor(getColorByRes(R.color.stripeProcessingBackground))
    }

    protected open fun setupProcessingTextViewUi() {
        processingTextView.text = getString(R.string.stripe_processing_card)
        processingTextView.setTextSizeByRes(R.dimen.stripeProcessingTextSize)
        processingTextView.setTextColor(getColorByRes(R.color.stripeProcessingText))
        processingTextView.gravity = Gravity.CENTER
    }

    override fun setupUiConstraints() {
        super.setupUiConstraints()

        setupCannotScanTextViewConstraints()
        setupCardDescriptionTextViewConstraints()
        setupProcessingOverlayViewConstraints()
        setupProcessingSpinnerViewConstraints()
        setupProcessingTextViewConstraints()
    }

    override fun setupInstructionsViewConstraints() {
        super.setupInstructionsViewConstraints()

        instructionsTextView.addConstraints {
            connect(it.id, ConstraintSet.BOTTOM, cardDescriptionTextView.id, ConstraintSet.TOP)
        }
    }

    protected open fun setupCannotScanTextViewConstraints() {
        cannotScanTextView.layoutParams = ConstraintLayout.LayoutParams(
            0, // width
            ConstraintLayout.LayoutParams.WRAP_CONTENT, // height
        ).apply {
            marginStart = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
            marginEnd = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
            bottomMargin = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
            topMargin = resources.getDimensionPixelSize(R.dimen.stripeButtonMargin)
        }

        cannotScanTextView.addConstraints {
            connect(it.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            connect(it.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(it.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        }
    }

    protected open fun setupCardDescriptionTextViewConstraints() {
        cardDescriptionTextView.layoutParams = ConstraintLayout.LayoutParams(
            0, // width
            ConstraintLayout.LayoutParams.WRAP_CONTENT, // height
        ).apply {
            marginStart = resources.getDimensionPixelSize(R.dimen.stripeCardDescriptionMargin)
            marginEnd = resources.getDimensionPixelSize(R.dimen.stripeCardDescriptionMargin)
            bottomMargin = resources.getDimensionPixelSize(R.dimen.stripeCardDescriptionMargin)
            topMargin = resources.getDimensionPixelSize(R.dimen.stripeCardDescriptionMargin)
        }

        cardDescriptionTextView.addConstraints {
            connect(it.id, ConstraintSet.BOTTOM, viewFinderWindowView.id, ConstraintSet.TOP)
            connect(it.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(it.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        }
    }

    protected open fun setupProcessingOverlayViewConstraints() {
        processingOverlayView.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT, // width
            ConstraintLayout.LayoutParams.MATCH_PARENT, // height
        )

        processingOverlayView.constrainToParent()
    }

    protected open fun setupProcessingSpinnerViewConstraints() {
        processingSpinnerView.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT, // width
            ConstraintLayout.LayoutParams.WRAP_CONTENT, // height
        )

        processingSpinnerView.constrainToParent()
    }

    protected open fun setupProcessingTextViewConstraints() {
        processingTextView.layoutParams = ConstraintLayout.LayoutParams(
            0, // width
            ConstraintLayout.LayoutParams.WRAP_CONTENT, // height
        )

        processingTextView.addConstraints {
            connect(it.id, ConstraintSet.TOP, processingSpinnerView.id, ConstraintSet.BOTTOM)
            connect(it.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(it.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        }
    }

    override fun displayState(newState: ScanState, previousState: ScanState?) {
        super.displayState(newState, previousState)

        when (newState) {
            is ScanState.NotFound, ScanState.FoundShort, ScanState.FoundLong, ScanState.Wrong -> {
                processingOverlayView.hide()
                processingSpinnerView.hide()
                processingTextView.hide()
            }
            is ScanState.Correct -> {
                processingOverlayView.show()
                processingSpinnerView.show()
                processingTextView.show()
            }
        }
    }

    override fun closeScanner() {
        uploadScanStats(
            stripePublishableKey = params.stripePublishableKey,
            civId = params.cardImageVerificationIntentId,
            civSecret = params.cardImageVerificationIntentSecret,
            instanceId = Stats.instanceId,
            scanId = Stats.scanId,
            device = Device.fromContext(this),
            appDetails = AppDetails.fromContext(this),
            scanStatistics = ScanStatistics.fromStats()
        )
        super.closeScanner()
    }

    private val scanResultListener = object :
        AggregateResultListener<MainLoopAggregator.InterimResult, MainLoopAggregator.FinalResult> {

        /**
         * A final result was received from the aggregator. Set the result from this activity.
         */
        override suspend fun onResult(
            result: MainLoopAggregator.FinalResult,
        ) = launch(Dispatchers.Main) {
            changeScanState(ScanState.Correct)
            cameraAdapter.unbindFromLifecycle(this@CardVerifyActivity)
            resultListener.cardScanned(
                pan = result.pan,
                frames = scanFlow.selectCompletionLoopFrames(result.savedFrames),
            )
        }.let { }

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

            val pan = when (result.state) {
                is MainLoopState.Initial -> null
                is MainLoopState.OcrFound -> result.state.mostLikelyPan
                is MainLoopState.OcrSatisfied -> result.state.pan
                is MainLoopState.CardSatisfied -> result.state.mostLikelyPan
                is MainLoopState.WrongCard -> null
                is MainLoopState.Finished -> result.state.pan
            }

            val lastFour = pan?.lastFour()
            val cardIssuer = if (pan.isNullOrEmpty()) null else getCardIssuer(pan)

            if (lastFour != null) {
                cardNumberTextView.text = getString(
                    R.string.stripe_card_description,
                    cardIssuer?.displayName ?: "",
                    lastFour,
                )
                cardNumberTextView.show()
            } else {
                cardNumberTextView.hide()
            }

            when (result.state) {
                is MainLoopState.Initial -> changeScanState(ScanState.NotFound)
                is MainLoopState.OcrFound -> changeScanState(ScanState.FoundLong)
                is MainLoopState.OcrSatisfied -> changeScanState(ScanState.FoundLong)
                is MainLoopState.CardSatisfied -> changeScanState(ScanState.FoundLong)
                is MainLoopState.WrongCard -> changeScanState(ScanState.Wrong)
                is MainLoopState.Finished -> changeScanState(ScanState.Correct)
            }
        }.let { }

        override suspend fun onReset() = launch(Dispatchers.Main) {
            changeScanState(ScanState.NotFound)
        }.let { }
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
