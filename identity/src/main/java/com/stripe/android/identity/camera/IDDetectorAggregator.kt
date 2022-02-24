package com.stripe.android.identity.camera

import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.camera.framework.ResultAggregator
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.states.IdentityScanState

internal class IDDetectorAggregator(
    identityScanType: IdentityScanState.ScanType,
    aggregateResultListener: AggregateResultListener<InterimResult, FinalResult>
) : ResultAggregator<
    AnalyzerInput,
    IdentityScanState,
    AnalyzerOutput,
    IDDetectorAggregator.InterimResult,
    IDDetectorAggregator.FinalResult
    >(
    aggregateResultListener,
    IdentityScanState.Initial(identityScanType),
    statsName = null
) {

    internal data class InterimResult(
        val frame: AnalyzerInput,
        val result: AnalyzerOutput,
        val identityState: IdentityScanState
    )

    internal data class FinalResult(
        val result: AnalyzerOutput,
        val identityState: IdentityScanState
    )

    override suspend fun aggregateResult(
        frame: AnalyzerInput,
        result: AnalyzerOutput
    ): Pair<InterimResult, FinalResult?> {
        val previousState = state
        state = previousState.consumeTransition(result)
        val interimResult = InterimResult(frame, result, state)
        return if (state is IdentityScanState.Finished) {
            interimResult to FinalResult(result, state)
        } else {
            interimResult to null
        }
    }
}
