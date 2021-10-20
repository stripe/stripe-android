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
import com.stripe.android.cardverificationsheet.cardverifyui.analyzer.CompletionLoopAnalyzer
import com.stripe.android.cardverificationsheet.cardverifyui.exception.InvalidRequiredCardException
import com.stripe.android.cardverificationsheet.cardverifyui.result.MainLoopAggregator
import com.stripe.android.cardverificationsheet.cardverifyui.result.MainLoopState
import com.stripe.android.cardverificationsheet.framework.AggregateResultListener
import com.stripe.android.cardverificationsheet.framework.AnalyzerLoopErrorListener
import com.stripe.android.cardverificationsheet.framework.Config
import com.stripe.android.cardverificationsheet.payment.card.CardIssuer
import com.stripe.android.cardverificationsheet.payment.card.getCardIssuer
import com.stripe.android.cardverificationsheet.scanui.CardVerificationSheetCancelationReason
import com.stripe.android.cardverificationsheet.scanui.ScanResultListener
import com.stripe.android.cardverificationsheet.scanui.SimpleScanActivity
import com.stripe.android.cardverificationsheet.scanui.util.getColorByRes
import com.stripe.android.cardverificationsheet.scanui.util.getDrawableByRes
import com.stripe.android.cardverificationsheet.scanui.util.setTextSizeByRes
import com.stripe.android.cardverificationsheet.scanui.util.setVisible
import com.stripe.android.cardverificationsheet.scanui.util.show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

const val PARAM_CARD_ISSUER = "cardIssuer"
const val PARAM_CARD_LAST_FOUR = "lastFour"
const val PARAM_ENABLE_MISSING_CARD = "missingCard"

const val RESULT_CANCELED_REASON = "canceledReason"
const val RESULT_FAILED_CAUSE = "failureCause"

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

    /**
     * If specified, the user will only be able to scan a card that matches this IIN.
     */
    private val cardIssuer: CardIssuer? by lazy {
        intent.getStringExtra(PARAM_CARD_ISSUER)?.let { getCardIssuer(it) }
    }

    /**
     * The user will only be able to scan a card that matches these last four.
     */
    private val cardLastFour: String by lazy {
        intent.getStringExtra(PARAM_CARD_LAST_FOUR)?.also {
            resultListener.failed(InvalidRequiredCardException("Missing last four digits"))
        } ?: ""
    }

    /**
     * If true and a card is required, an "I don't have this card" button will be shown to the user.
     */
    private val enableMissingCard: Boolean by lazy {
        intent.getBooleanExtra(PARAM_ENABLE_MISSING_CARD, true)
    }

    /**
     * The listener which handles results from the scan.
     */
    override val resultListener: CardVerifyResultListener = object : CardVerifyResultListener {
        override fun cardVerificationComplete() {
            setResult(Activity.RESULT_OK, Intent())
        }

        override fun cardScanned(frames: Collection<SavedFrame>) {
            launch(Dispatchers.Default) {
                CompletionLoopAnalyzer.Factory().newInstance().analyze(frames, Unit)
            }
        }

        override fun userCanceled(reason: CardVerificationSheetCancelationReason) {
            val intent = Intent()
                .putExtra(RESULT_CANCELED_REASON, reason)
            setResult(Activity.RESULT_CANCELED, intent)
        }

        override fun failed(cause: Throwable?) {
            val intent = Intent()
                .putExtra(RESULT_FAILED_CAUSE, cause)
            setResult(Activity.RESULT_CANCELED, intent)
        }
    }

    /**
     * The flow used to scan an item.
     */
    override val scanFlow: CardVerifyFlow by lazy {
        CardVerifyFlow(cardIssuer, cardLastFour, scanResultListener, scanErrorListener)
    }

    private val hasPreviousValidResult = AtomicBoolean(false)

    override val minimumAnalysisResolution = MINIMUM_RESOLUTION

    /**
     * During on create
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        cannotScanTextView.setOnClickListener { userCannotScan() }
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

        cannotScanTextView.setVisible(enableMissingCard)

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
        cardDescriptionTextView.text = getString(
            R.string.stripe_card_description,
            cardIssuer ?: "",
            cardLastFour
        )

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

    private val scanResultListener = object :
        AggregateResultListener<MainLoopAggregator.InterimResult, MainLoopAggregator.FinalResult> {

        /**
         * A final result was received from the aggregator. Set the result from this activity.
         */
        override suspend fun onResult(result: MainLoopAggregator.FinalResult) = launch(Dispatchers.Main) {
            changeScanState(ScanState.Correct)
            cameraAdapter.unbindFromLifecycle(this@CardVerifyActivity)
            resultListener.cardScanned(scanFlow.selectCompletionLoopFrames(result.savedFrames))
        }.let { }

        /**
         * An interim result was received from the result aggregator.
         */
        override suspend fun onInterimResult(result: MainLoopAggregator.InterimResult) = launch(Dispatchers.Main) {
            if (result.state is MainLoopState.PanFound && !hasPreviousValidResult.getAndSet(true)) {
                scanStat.trackResult("ocr_pan_observed")
            }

            val (mostLikelyCardIssuer, mostLikelyLastFour) = when (val state = result.state) {
                is MainLoopState.Initial ->
                    null to null
                is MainLoopState.PanFound ->
                    state.getMostLikelyCardIssuer() to state.getMostLikelyLastFour()
                is MainLoopState.PanSatisfied ->
                    state.cardIssuer to state.lastFour
                is MainLoopState.CardSatisfied ->
                    state.getMostLikelyCardIssuer() to state.getMostLikelyLastFour()
                is MainLoopState.WrongPanFound ->
                    null to null
                is MainLoopState.Finished ->
                    state.cardIssuer to state.lastFour
            }
            if (mostLikelyLastFour != null) {
                cardNumberTextView.text =
                    getString(
                        R.string.stripe_card_description,
                        mostLikelyCardIssuer,
                        mostLikelyLastFour,
                    )
                cardNumberTextView.show()
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
