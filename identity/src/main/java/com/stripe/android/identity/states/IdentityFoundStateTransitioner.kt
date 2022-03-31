package com.stripe.android.identity.states

import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.states.IdentityScanState.Found

/**
 * Interface to determine how to transition from [IdentityScanState.Found] to next state.
 */
internal interface IdentityFoundStateTransitioner {
    fun transition(foundState: Found, analyzerOutput: AnalyzerOutput): IdentityScanState
}
