package com.stripe.android.identity

/**
 * An interface to indicate this class is able to finish verification flow with result.
 */
internal fun interface VerificationFlowFinishable {
    fun finishWithResult(result: IdentityVerificationSheet.VerificationFlowResult)
}
