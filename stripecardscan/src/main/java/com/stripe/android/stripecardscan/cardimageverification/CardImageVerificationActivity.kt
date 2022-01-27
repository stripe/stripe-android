package com.stripe.android.stripecardscan.cardimageverification

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.Keep
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.stripe.android.camera.framework.Stats
import com.stripe.android.stripecardscan.R
import com.stripe.android.stripecardscan.cardimageverification.exception.InvalidCivException
import com.stripe.android.stripecardscan.cardimageverification.exception.InvalidStripePublishableKeyException
import com.stripe.android.stripecardscan.cardimageverification.exception.StripeNetworkException
import com.stripe.android.stripecardscan.cardimageverification.exception.UnknownScanException
import com.stripe.android.stripecardscan.cardimageverification.result.MainLoopAggregator
import com.stripe.android.stripecardscan.cardimageverification.result.MainLoopState
import com.stripe.android.stripecardscan.framework.Config
import com.stripe.android.stripecardscan.framework.api.NetworkResult
import com.stripe.android.stripecardscan.framework.api.dto.ScanStatistics
import com.stripe.android.stripecardscan.framework.api.getCardImageVerificationIntentDetails
import com.stripe.android.stripecardscan.framework.api.uploadSavedFrames
import com.stripe.android.stripecardscan.framework.api.uploadScanStats
import com.stripe.android.stripecardscan.framework.util.AppDetails
import com.stripe.android.stripecardscan.framework.util.Device
import com.stripe.android.stripecardscan.payment.card.CardIssuer
import com.stripe.android.stripecardscan.payment.card.ScannedCard
import com.stripe.android.stripecardscan.payment.card.getCardIssuer
import com.stripe.android.stripecardscan.payment.card.getIssuerByDisplayName
import com.stripe.android.stripecardscan.payment.card.isValidPanLastFour
import com.stripe.android.stripecardscan.payment.card.lastFour
import com.stripe.android.stripecardscan.scanui.CancellationReason
import com.stripe.android.stripecardscan.scanui.ScanErrorListener
import com.stripe.android.stripecardscan.scanui.ScanResultListener
import com.stripe.android.stripecardscan.scanui.SimpleScanActivity
import com.stripe.android.stripecardscan.scanui.SimpleScanStateful
import com.stripe.android.stripecardscan.scanui.SimpleScanStateful.ScanState
import com.stripe.android.stripecardscan.scanui.util.getColorByRes
import com.stripe.android.stripecardscan.scanui.util.getDrawableByRes
import com.stripe.android.stripecardscan.scanui.util.hide
import com.stripe.android.stripecardscan.scanui.util.setTextSizeByRes
import com.stripe.android.stripecardscan.scanui.util.setVisible
import com.stripe.android.stripecardscan.scanui.util.show
import com.stripe.android.stripecardscan.scanui.util.startAnimation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal const val INTENT_PARAM_REQUEST = "request"
internal const val INTENT_PARAM_RESULT = "result"

internal interface CardImageVerificationResultListener : ScanResultListener {

    /**
     * A payment card was successfully scanned.
     */
    fun cardImageVerificationComplete(pan: String)

    /**
     * A card was scanned and is ready to be verified.
     */
    fun cardReadyForVerification(pan: String, frames: Collection<SavedFrame>)
}

internal data class RequiredCardDetails(
    val cardIssuer: CardIssuer?,
    val lastFour: String?,
)

private val MINIMUM_RESOLUTION = Size(1067, 600) // minimum size of OCR

@Keep
internal open class CardImageVerificationActivity :
    SimpleScanActivity<RequiredCardDetails?>(), SimpleScanStateful {

    override var scanState: ScanState = ScanState.NotFound

    override var scanStatePrevious: ScanState? = null

    override val scanErrorListener: ScanErrorListener = ScanErrorListener()

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

    private val params: CardImageVerificationSheetParams by lazy {
        intent.getParcelableExtra(INTENT_PARAM_REQUEST)
            ?: CardImageVerificationSheetParams("", "", "")
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
    override val resultListener: CardImageVerificationResultListener =
        object : CardImageVerificationResultListener {
            override fun cardImageVerificationComplete(pan: String) {
                val intent = Intent()
                    .putExtra(
                        INTENT_PARAM_RESULT,
                        CardImageVerificationSheetResult.Completed(
                            ScannedCard(
                                pan = pan
                            )
                        )
                    )
                setResult(RESULT_OK, intent)
                closeScanner()
            }

            override fun cardReadyForVerification(pan: String, frames: Collection<SavedFrame>) {
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
                            cardImageVerificationComplete(pan)
                        is NetworkResult.Error ->
                            scanFailure(StripeNetworkException(result.error.error.message))
                        is NetworkResult.Exception ->
                            scanFailure(result.exception)
                    }
                }
            }

            override fun userCanceled(reason: CancellationReason) {
                val intent = Intent()
                    .putExtra(
                        INTENT_PARAM_RESULT,
                        CardImageVerificationSheetResult.Canceled(reason),
                    )
                setResult(RESULT_CANCELED, intent)
            }

            override fun failed(cause: Throwable?) {
                val intent = Intent()
                    .putExtra(
                        INTENT_PARAM_RESULT,
                        CardImageVerificationSheetResult.Failed(cause ?: UnknownScanException()),
                    )
                setResult(RESULT_CANCELED, intent)
            }
        }

    /**
     * The flow used to scan an item.
     */
    override val scanFlow: CardImageVerificationFlow by lazy {
        object : CardImageVerificationFlow(scanErrorListener) {
            /**
             * A final result was received from the aggregator. Set the result from this activity.
             */
            override suspend fun onResult(
                result: MainLoopAggregator.FinalResult,
            ) {
                super.onResult(result)

                launch(Dispatchers.Main) {
                    changeScanState(ScanState.Correct)
                    cameraAdapter.unbindFromLifecycle(this@CardImageVerificationActivity)
                    resultListener.cardReadyForVerification(
                        pan = result.pan,
                        frames = scanFlow.selectCompletionLoopFrames(result.savedFrames),
                    )
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

        displayState(scanState, scanStatePrevious)
    }

    override fun onResume() {
        super.onResume()
        scanState = ScanState.NotFound
    }

    override fun ensureValidParams() = when {
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
                if (expectedCard.lastFour.isNullOrEmpty() ||
                    isValidPanLastFour(expectedCard.lastFour)
                ) {
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
        when (newState) {
            is ScanState.NotFound -> {
                viewFinderBackgroundView
                    .setBackgroundColor(getColorByRes(R.color.stripeNotFoundBackground))
                viewFinderWindowView
                    .setBackgroundResource(R.drawable.stripe_card_background_not_found)
                viewFinderBorderView.startAnimation(R.drawable.stripe_card_border_not_found)
                instructionsTextView.setText(R.string.stripe_card_scan_instructions)
                cardNumberTextView.hide()
                cardNameTextView.hide()
            }
            is ScanState.FoundShort -> {
                viewFinderBackgroundView
                    .setBackgroundColor(getColorByRes(R.color.stripeFoundBackground))
                viewFinderWindowView
                    .setBackgroundResource(R.drawable.stripe_card_background_found)
                viewFinderBorderView.startAnimation(R.drawable.stripe_card_border_found)
                instructionsTextView.setText(R.string.stripe_card_scan_instructions)
                instructionsTextView.show()
            }
            is ScanState.FoundLong -> {
                viewFinderBackgroundView
                    .setBackgroundColor(getColorByRes(R.color.stripeFoundBackground))
                viewFinderWindowView
                    .setBackgroundResource(R.drawable.stripe_card_background_found)
                viewFinderBorderView.startAnimation(R.drawable.stripe_card_border_found_long)
                instructionsTextView.setText(R.string.stripe_card_scan_instructions)
                instructionsTextView.show()
            }
            is ScanState.Correct -> {
                viewFinderBackgroundView
                    .setBackgroundColor(getColorByRes(R.color.stripeCorrectBackground))
                viewFinderWindowView
                    .setBackgroundResource(R.drawable.stripe_card_background_correct)
                viewFinderBorderView.startAnimation(R.drawable.stripe_card_border_correct)
                instructionsTextView.hide()
            }
            is ScanState.Wrong -> {
                viewFinderBackgroundView
                    .setBackgroundColor(getColorByRes(R.color.stripeWrongBackground))
                viewFinderWindowView
                    .setBackgroundResource(R.drawable.stripe_card_background_wrong)
                viewFinderBorderView.startAnimation(R.drawable.stripe_card_border_wrong)
                instructionsTextView.setText(R.string.stripe_scanned_wrong_card)
            }
        }

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
}
