package com.stripe.android.stripecardscan.cardscan.result

import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.camera.framework.ResultAggregator
import com.stripe.android.stripecardscan.cardscan.result.MainLoopAggregator.FinalResult
import com.stripe.android.stripecardscan.cardscan.result.MainLoopAggregator.InterimResult
import com.stripe.android.stripecardscan.payment.ml.CardOcr
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
    listener: AggregateResultListener<InterimResult, FinalResult>,
    enableExpiryWait: Boolean = false,
) : ResultAggregator<
    CardOcr.Input,
    MainLoopState,
    CardOcr.Prediction,
    InterimResult,
    FinalResult
    >(
    listener = listener,
    initialState = MainLoopState.Initial(TimeSource.Monotonic, enableExpiryWait)
) {

    internal data class FinalResult(
        val pan: String,
        val expiryMonth: Int? = null,
        val expiryYear: Int? = null,
    )

    internal data class InterimResult(
        val analyzerResult: CardOcr.Prediction,
        val frame: CardOcr.Input,
        val state: MainLoopState
    )

    override suspend fun aggregateResult(
        frame: CardOcr.Input,
        result: CardOcr.Prediction
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
            interimResult to FinalResult(
                pan = currentState.pan,
                expiryMonth = currentState.expiryMonth,
                expiryYear = currentState.expiryYear,
            )
        } else {
            interimResult to null
        }
    }
}
