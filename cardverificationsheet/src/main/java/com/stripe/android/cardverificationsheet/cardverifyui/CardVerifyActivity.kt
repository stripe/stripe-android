package com.stripe.android.cardverificationsheet.cardverifyui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.widget.TextView
import androidx.annotation.Keep
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.stripe.android.cardverificationsheet.R
import com.stripe.android.cardverificationsheet.cardverifyui.exception.InvalidCivException
import com.stripe.android.cardverificationsheet.cardverifyui.exception.InvalidRequiredCardException
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
import com.stripe.android.cardverificationsheet.payment.card.getIssuerByDisplayName
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
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal const val INTENT_PARAM_REQUEST = "request"
internal const val INTENT_PARAM_RESULT = "result"

@Keep
internal interface CardVerifyResultListener : ScanResultListener {

    /**
     * A payment card was successfully scanned.
     */
    fun cardVerificationComplete()

    /**
     * A card was scanned and is ready to be verified.
     */
    fun cardScanned(frames: Collection<SavedFrame>)
}

private val MINIMUM_RESOLUTION = Size(1067, 600) // minimum size of OCR

@Keep
open class CardVerifyActivity : SimpleScanActivity() {

    /**
     * The text view that lets a user indicate they do not have possession of the required card.
     */
    protected open val cannotScanTextView: TextView by lazy { TextView(this) }

    /**
     * The text view that informs the user which card must be scanned.
     */
    protected open val cardDescriptionTextView: TextView by lazy { TextView(this) }

    private val params: CardVerificationSheetParams by lazy {
        val params = intent.getParcelableExtra(INTENT_PARAM_REQUEST)
            ?: CardVerificationSheetParams("", "", "")
                .also {
                    scanFailure(InvalidCivException("Missing required parameters"))
                }
        when {
            params.stripePublishableKey.isEmpty() ->
                scanFailure(InvalidStripePublishableKeyException("Missing publishable key"))
            params.cardImageVerificationIntentId.isEmpty() ->
                scanFailure(InvalidCivException("Missing card image verification ID"))
            params.cardImageVerificationIntentSecret.isEmpty() ->
                scanFailure(InvalidCivException("Missing card image verification client secret"))
        }

        params
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
        override fun cardVerificationComplete() {
            val intent = Intent()
                .putExtra(INTENT_PARAM_RESULT, CardVerificationSheetResult.Completed)
            setResult(Activity.RESULT_OK, intent)
        }

        override fun cardScanned(frames: Collection<SavedFrame>) {
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
                        cardVerificationComplete()
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
            requiredCardIssuer,
            requiredCardLastFour ?: "",
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

        launch {
            when (
                val result = getCardImageVerificationIntentDetails(
                    stripePublishableKey = params.stripePublishableKey,
                    civId = params.cardImageVerificationIntentId,
                    civSecret = params.cardImageVerificationIntentSecret,
                )
            ) {
                is NetworkResult.Success ->
                    onScanDetailsAvailable(
                        getIssuerByDisplayName(result.body.expectedCard?.issuer ?: ""),
                        result.body.expectedCard?.lastFour,
                    )
                is NetworkResult.Error ->
                    scanFailure(StripeNetworkException("Unable to get CIV details"))
                is NetworkResult.Exception ->
                    scanFailure(result.exception)
            }
        }

        cannotScanTextView.setOnClickListener { userCannotScan() }
    }

    private fun onScanDetailsAvailable(
        requiredCardIssuer: CardIssuer?,
        requiredCardLastFour: String?,
    ) {
        if (requiredCardLastFour.isNullOrEmpty()) {
            scanFailure(InvalidRequiredCardException("Missing last four"))
            return
        }

        this.requiredCardIssuer = requiredCardIssuer
        this.requiredCardLastFour = requiredCardLastFour

        cardDescriptionTextView.text = getString(
            R.string.stripe_card_description,
            requiredCardIssuer?.displayName ?: "",
            requiredCardLastFour
        )
    }

    override fun addUiComponents() {
        super.addUiComponents()
        appendUiComponents(cannotScanTextView, cardDescriptionTextView)
    }

    override fun setupUiComponents() {
        super.setupUiComponents()

        setupCannotScanUi()
        setupCardDescriptionUi()
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

    override fun setupUiConstraints() {
        super.setupUiConstraints()

        setupCannotScanTextViewConstraints()
        setupCardDescriptionTextViewConstraints()
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
            resultListener.cardScanned(scanFlow.selectCompletionLoopFrames(result.savedFrames))
        }.let { }

        /**
         * An interim result was received from the result aggregator.
         */
        override suspend fun onInterimResult(
            result: MainLoopAggregator.InterimResult,
        ) = launch(Dispatchers.Main) {
            if (
                result.state is MainLoopState.PanFound &&
                !hasPreviousValidResult.getAndSet(true)
            ) {
                scanStat.trackResult("ocr_pan_observed")
            }

            val (cardIssuer, lastFour) = when (result.state) {
                is MainLoopState.Initial -> null to null
                is MainLoopState.PanFound -> requiredCardIssuer to requiredCardLastFour
                is MainLoopState.PanSatisfied -> requiredCardIssuer to requiredCardLastFour
                is MainLoopState.CardSatisfied -> requiredCardIssuer to requiredCardLastFour
                is MainLoopState.WrongPanFound -> null to null
                is MainLoopState.Finished -> requiredCardIssuer to requiredCardLastFour
            }
            if (lastFour != null) {
                cardNumberTextView.text =
                    getString(R.string.stripe_card_description, cardIssuer, lastFour)
                cardNumberTextView.show()
            } else {
                cardNumberTextView.hide()
            }

            when (result.state) {
                is MainLoopState.Initial ->
                    if (scanState !is ScanState.FoundLong) changeScanState(ScanState.NotFound)
                is MainLoopState.PanFound -> changeScanState(ScanState.FoundLong)
                is MainLoopState.PanSatisfied -> changeScanState(ScanState.FoundLong)
                is MainLoopState.CardSatisfied -> changeScanState(ScanState.FoundLong)
                is MainLoopState.WrongPanFound -> changeScanState(ScanState.Wrong)
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
