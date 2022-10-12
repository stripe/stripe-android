package com.stripe.android.stripecardscan.cardimageverification.result

import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.camera.framework.ResultAggregator
import com.stripe.android.camera.framework.util.FrameSaver
import com.stripe.android.stripecardscan.cardimageverification.SavedFrame
import com.stripe.android.stripecardscan.cardimageverification.SavedFrameType
import com.stripe.android.stripecardscan.cardimageverification.analyzer.MainLoopAnalyzer
import com.stripe.android.stripecardscan.cardimageverification.result.MainLoopAggregator.FinalResult
import com.stripe.android.stripecardscan.cardimageverification.result.MainLoopAggregator.InterimResult
import com.stripe.android.stripecardscan.payment.card.CardIssuer
import com.stripe.android.stripecardscan.payment.card.CardMatchResult
import com.stripe.android.stripecardscan.payment.card.RequiresMatchingCard
import com.stripe.android.stripecardscan.payment.card.isValidPanLastFour
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
    strictModeFrames: Int
) : RequiresMatchingCard,
    ResultAggregator<
        MainLoopAnalyzer.Input,
        MainLoopState,
        MainLoopAnalyzer.Prediction,
        InterimResult,
        FinalResult
        >(
        listener = listener,
        initialState = MainLoopState.Initial(
            requiredCardIssuer = requiredCardIssuer,
            requiredLastFour = requiredLastFour,
            strictModeFrames = strictModeFrames
        ),
        statsName = null // TODO: when we want to collect this in scan stats, give this a name
    ) {

    companion object {

        /**
         * The maximum number of saved frames per type to use.
         */
        const val MAX_SAVED_FRAMES_PER_TYPE = 6
    }

    internal data class FinalResult(
        val pan: String,
        val savedFrames: Map<SavedFrameType, List<SavedFrame>>
    )

    internal data class InterimResult(
        val analyzerResult: MainLoopAnalyzer.Prediction,
        val frame: MainLoopAnalyzer.Input,
        val state: MainLoopState
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
            MAX_SAVED_FRAMES_PER_TYPE
        override fun getSaveFrameIdentifier(
            frame: SavedFrame,
            metaData: InterimResult
        ): SavedFrameType? {
            val hasCard = metaData.analyzerResult.isCardVisible == true
            val matchesCard = compareToRequiredCard(metaData.analyzerResult.ocr?.pan)

            return when {
                matchesCard is CardMatchResult.Match ||
                    matchesCard is CardMatchResult.NoRequiredCard
                ->
                    SavedFrameType(hasCard = hasCard, hasOcr = true)
                matchesCard is CardMatchResult.NoPan && hasCard ->
                    SavedFrameType(hasCard = hasCard, hasOcr = false)
                else -> null
            }
        }
    }

    override suspend fun aggregateResult(
        frame: MainLoopAnalyzer.Input,
        result: MainLoopAnalyzer.Prediction
    ): Pair<InterimResult, FinalResult?> {
        val previousState = state
        val currentState = previousState.consumeTransition(result)

        state = currentState

        val interimResult = InterimResult(
            analyzerResult = result,
            frame = frame,
            state = currentState
        )

        val savedFrame = SavedFrame(
            hasOcr = result.ocr?.pan?.isNotEmpty() == true,
            frame = frame
        )

        frameSaver.saveFrame(savedFrame, interimResult)

        return if (currentState is MainLoopState.Finished) {
            val savedFrames = frameSaver.getSavedFrames()
            frameSaver.reset()
            interimResult to FinalResult(currentState.pan, savedFrames)
        } else {
            interimResult to null
        }
    }

    override fun reset() {
        super.reset()
        runBlocking { frameSaver.reset() }
    }
}
