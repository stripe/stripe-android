package com.stripe.android.identity.camera

import com.stripe.android.camera.framework.ResultAggregator
import com.stripe.android.identity.ml.IDDetectorAnalyzer

internal class IDDetectorAggregator :
    ResultAggregator<
        IDDetectorAnalyzer.Input,
        IDDetectorAnalyzer.State,
        IDDetectorAnalyzer.Output,
        IDDetectorAggregator.InterimResult,
        IDDetectorAggregator.FinalResult
        >(
        IDDetectorResultListener(),
        IDDetectorAnalyzer.State(23),
        statsName = null
    ) {

    internal data class InterimResult(
        val value: Int

    )

    internal data class FinalResult(
        val value: Int
    )

    override suspend fun aggregateResult(
        frame: IDDetectorAnalyzer.Input,
        result: IDDetectorAnalyzer.Output
    ): Pair<InterimResult, FinalResult?> {
        // TODO(ccen): Build state machine and return valid result
        return InterimResult(23) to null
    }
}
