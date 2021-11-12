package com.stripe.android.cardverificationsheet.cardverifyui.result

import androidx.annotation.Keep
import com.stripe.android.cardverificationsheet.cardverifyui.SavedFrame
import com.stripe.android.cardverificationsheet.cardverifyui.SavedFrameType
import com.stripe.android.cardverificationsheet.cardverifyui.VerifyConfig
import com.stripe.android.cardverificationsheet.cardverifyui.analyzer.MainLoopAnalyzer
import com.stripe.android.cardverificationsheet.framework.AggregateResultListener
import com.stripe.android.cardverificationsheet.framework.ResultAggregator
import com.stripe.android.cardverificationsheet.framework.util.FrameSaver
import com.stripe.android.cardverificationsheet.payment.card.CardIssuer
import com.stripe.android.cardverificationsheet.payment.card.CardMatch
import com.stripe.android.cardverificationsheet.payment.card.RequiresMatchingCard
import com.stripe.android.cardverificationsheet.payment.card.isValidPanLastFour
import kotlinx.coroutines.runBlocking

/**
 * Aggregate results from the main loop. Each frame will trigger an [InterimResult] to the
 * [listener]. Once the [MainLoopState.Finished] state is reached, a [FinalResult] will be sent to
 * the [listener].
 *
 * This aggregator is a state machine. The full list of possible states are subclasses of
 * [MainLoopState]. This was written referencing this article:
 * https://thoughtbot.com/blog/finite-state-machines-android-kotlin-good-times
 */
internal class MainLoopAggregator(
    listener: AggregateResultListener<InterimResult, FinalResult>,
    override val requiredCardIssuer: CardIssuer?,
    override val requiredLastFour: String?,
) : RequiresMatchingCard,
    ResultAggregator<
        MainLoopAnalyzer.Input,
        MainLoopState,
        MainLoopAnalyzer.Prediction,
        MainLoopAggregator.InterimResult,
        MainLoopAggregator.FinalResult
        >(
        listener = listener,
        initialState = MainLoopState.Initial(
            requiredCardIssuer = requiredCardIssuer,
            requiredLastFour = requiredLastFour,
        ),
    ) {

    @Keep
    internal data class FinalResult(
        val savedFrames: Map<SavedFrameType, List<SavedFrame>>,
    )

    @Keep
    data class InterimResult(
        val analyzerResult: MainLoopAnalyzer.Prediction,
        val frame: MainLoopAnalyzer.Input,
        val state: MainLoopState,
    )

    init {
        require(requiredLastFour == null || isValidPanLastFour(requiredLastFour)) {
            "Invalid last four"
        }
        require(requiredCardIssuer == null || requiredCardIssuer != CardIssuer.Unknown) {
            "Invalid required iin"
        }
    }

    private val frameSaver = object : FrameSaver<SavedFrameType, SavedFrame, InterimResult>() {
        override fun getMaxSavedFrames(savedFrameIdentifier: SavedFrameType): Int =
            VerifyConfig.MAX_SAVED_FRAMES_PER_TYPE
        override fun getSaveFrameIdentifier(
            frame: SavedFrame,
            metaData: InterimResult,
        ): SavedFrameType? {
            val hasCard = metaData.analyzerResult.isCardVisible == true
            val matchesCard = compareToRequiredCard(metaData.analyzerResult.ocr?.pan)

            return when {
                matchesCard is CardMatch.Match || matchesCard is CardMatch.NoRequiredCard ->
                    SavedFrameType(hasCard = hasCard, hasOcr = true)
                matchesCard is CardMatch.NoPan && hasCard ->
                    SavedFrameType(hasCard = hasCard, hasOcr = false)
                else -> null
            }
        }
    }

    override suspend fun aggregateResult(
        frame: MainLoopAnalyzer.Input,
        result: MainLoopAnalyzer.Prediction,
    ): Pair<InterimResult, FinalResult?> {
        val previousState = state
        val currentState = previousState.consumeTransition(result)

        state = currentState

        val interimResult = InterimResult(
            analyzerResult = result,
            frame = frame,
            state = currentState,
        )

        val savedFrame = SavedFrame(
            hasOcr = result.ocr?.pan?.isNotEmpty() == true,
            frame = frame,
        )

        frameSaver.saveFrame(savedFrame, interimResult)

        return if (currentState is MainLoopState.Finished) {
            val savedFrames = frameSaver.getSavedFrames()
            frameSaver.reset()
            interimResult to FinalResult(savedFrames)
        } else {
            interimResult to null
        }
    }

    override fun reset() {
        super.reset()
        runBlocking { frameSaver.reset() }
    }
}
