package com.stripe.android.identity.states

import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.states.IdentityScanState.Found
import com.stripe.android.identity.states.IdentityScanState.Initial

/**
 * Interface to determine how to transition between [IdentityScanState]s.
 */
internal interface IdentityScanStateTransitioner {
    fun transitionFromInitial(
        initialState: Initial,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState

    fun transitionFromFound(
        foundState: Found,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState
}
