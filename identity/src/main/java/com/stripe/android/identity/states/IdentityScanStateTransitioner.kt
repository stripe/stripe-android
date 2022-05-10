package com.stripe.android.identity.states

import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.states.IdentityScanState.Found
import com.stripe.android.identity.states.IdentityScanState.Initial
import com.stripe.android.identity.states.IdentityScanState.Satisfied
import com.stripe.android.identity.states.IdentityScanState.Unsatisfied

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

    fun transitionFromSatisfied(
        satisfiedState: Satisfied,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState

    fun transitionFromUnsatisfied(
        unsatisfiedState: Unsatisfied,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState
}
