package com.stripe.android.stripecardscan.cardscan.result

import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.camera.framework.ResultAggregator
import com.stripe.android.stripecardscan.cardscan.result.MainLoopAggregator.FinalResult
import com.stripe.android.stripecardscan.cardscan.result.MainLoopAggregator.InterimResult
import com.stripe.android.stripecardscan.payment.ml.SSDOcr
import kotlin.time.TimeSource

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
    InterimResult,
    FinalResult
    >(
    listener = listener,
    initialState = MainLoopState.Initial(TimeSource.Monotonic)
) {

    internal data class FinalResult(
        val pan: String
    )

    internal data class InterimResult(
        val analyzerResult: SSDOcr.Prediction,
        val frame: SSDOcr.Input,
        val state: MainLoopState
    )

    override suspend fun aggregateResult(
        frame: SSDOcr.Input,
        result: SSDOcr.Prediction
    ): Pair<InterimResult, FinalResult?> {
        val previousState = state
        val currentState = previousState.consumeTransition(result)

        state = currentState

        val interimResult = InterimResult(
            analyzerResult = result,
            frame = frame,
            state = currentState
        )

        return if (currentState is MainLoopState.Finished) {
            interimResult to FinalResult(currentState.pan)
        } else {
            interimResult to null
        }
    }
}
