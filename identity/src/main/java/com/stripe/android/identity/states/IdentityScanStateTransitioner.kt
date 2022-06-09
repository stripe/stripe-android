package com.stripe.android.identity.states

import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.states.IdentityScanState.Found
import com.stripe.android.identity.states.IdentityScanState.Initial
import com.stripe.android.identity.states.IdentityScanState.Satisfied
import com.stripe.android.identity.states.IdentityScanState.Unsatisfied

/**
 * Interface to determine how to transition between [IdentityScanState]s.
 */
internal interface IdentityScanStateTransitioner {
    suspend fun transitionFromInitial(
        initialState: Initial,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState

    suspend fun transitionFromFound(
        foundState: Found,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState

    suspend fun transitionFromSatisfied(
        satisfiedState: Satisfied,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState

    suspend fun transitionFromUnsatisfied(
        unsatisfiedState: Unsatisfied,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState
}
