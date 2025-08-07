package com.stripe.android.crypto.onramp.model

fun interface OnrampIdentityVerificationCallback {
    fun onResult(result: OnrampIdentityVerificationResult)
}

/**
 * Result of Stripe Identity verification process in Onramp.
 */
sealed class OnrampIdentityVerificationResult {
    /**
     * The user has completed uploading their documents.
     */
    class Completed internal constructor() : OnrampIdentityVerificationResult()

    /**
     * The user did not complete uploading their document, and should be allowed to try again.
     */
    class Canceled internal constructor() : OnrampIdentityVerificationResult()

    /**
     * Identity verification failed due to an error.
     * @param error The error that caused the failure.
     */
    class Failed internal constructor(val error: Throwable) : OnrampIdentityVerificationResult()
}
