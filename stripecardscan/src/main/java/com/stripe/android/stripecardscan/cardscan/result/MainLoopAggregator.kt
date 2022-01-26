package com.stripe.android.stripecardscan.cardscan.result

import com.stripe.android.stripecardscan.cardscan.SavedFrame
import com.stripe.android.stripecardscan.cardscan.SavedFrameType
import com.stripe.android.stripecardscan.cardscan.CardScanConfig
import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.camera.framework.ResultAggregator
import com.stripe.android.stripecardscan.framework.util.FrameSaver
import com.stripe.android.stripecardscan.payment.ml.SSDOcr
import kotlinx.coroutines.runBlocking

/**
 * Aggregate results from the main loop. Each frame will trigger an [InterimResult] to the
 * [listener]. Once the [MainLoopState.Finished] state is reached, a [FinalResult] will be sent to
 * the [listener].
 *
 * This aggregator is a state machine. The full list of possible states are subclasses of
 * [MainLoopState].
 */
internal class MainLoopAggregator(
    listener: AggregateResultListener<InterimResult, FinalResult>
) : ResultAggregator<
        SSDOcr.Input,
        MainLoopState,
        SSDOcr.Prediction,
        MainLoopAggregator.InterimResult,
        MainLoopAggregator.FinalResult
        >(
        listener = listener,
        initialState = MainLoopState.Initial(),
        statsName = null, // TODO: when we want to collect this in scan stats, give this a name
    ) {

    internal data class FinalResult(
        val pan: String,
        val savedFrames: Map<SavedFrameType, List<SavedFrame>>,
    )

    internal data class InterimResult(
        val analyzerResult: SSDOcr.Prediction,
        val frame: SSDOcr.Input,
        val state: MainLoopState,
    )

    private val frameSaver = object : FrameSaver<SavedFrameType, SavedFrame, InterimResult>() {
        override fun getMaxSavedFrames(savedFrameIdentifier: SavedFrameType): Int =
            CardScanConfig.MAX_SAVED_FRAMES_PER_TYPE
        override fun getSaveFrameIdentifier(
            frame: SavedFrame,
            metaData: InterimResult,
        ): SavedFrameType? {

            return when {
                metaData.analyzerResult.pan.isNullOrEmpty() -> SavedFrameType(hasOcr = false)
                else -> SavedFrameType(hasOcr = true)
            }
        }
    }

    override suspend fun aggregateResult(
        frame: SSDOcr.Input,
        result: SSDOcr.Prediction,
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
            hasOcr = result.pan?.isNotEmpty() == true,
            frame = frame,
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
