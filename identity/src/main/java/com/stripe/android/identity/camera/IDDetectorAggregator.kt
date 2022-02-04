package com.stripe.android.identity.camera

import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.camera.framework.ResultAggregator
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.states.ScanState

internal class IDDetectorAggregator(
    scanType: ScanState.ScanType,
    aggregateResultListener: AggregateResultListener<InterimResult, FinalResult>
) : ResultAggregator<
    AnalyzerInput,
    ScanState,
    AnalyzerOutput,
    IDDetectorAggregator.InterimResult,
    IDDetectorAggregator.FinalResult
    >(
    aggregateResultListener,
    ScanState.Initial(scanType),
    statsName = null
) {

    internal data class InterimResult(
        val frame: AnalyzerInput,
        val result: AnalyzerOutput
    )

    internal data class FinalResult(
        val result: AnalyzerOutput
    )

    override suspend fun aggregateResult(
        frame: AnalyzerInput,
        result: AnalyzerOutput
    ): Pair<InterimResult, FinalResult?> {
        val previousState = state
        state = previousState.consumeTransition(result)
        val interimResult = InterimResult(frame, result)
        return if (state is ScanState.Finished) {
            interimResult to FinalResult(result)
        } else {
            interimResult to null
        }
    }
}
