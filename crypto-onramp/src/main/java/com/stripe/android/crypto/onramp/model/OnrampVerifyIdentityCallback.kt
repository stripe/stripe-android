package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

@ExperimentalCryptoOnramp
fun interface OnrampVerifyIdentityCallback {
    fun onResult(result: OnrampVerifyIdentityResult)
}

/**
 * Result of Stripe Identity verification process in Onramp.
 */
@ExperimentalCryptoOnramp
sealed class OnrampVerifyIdentityResult {
    /**
     * The user has completed uploading their documents.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor() : OnrampVerifyIdentityResult()

    /**
     * The user did not complete uploading their document, and should be allowed to try again.
     */
    @ExperimentalCryptoOnramp
    class Cancelled internal constructor() : OnrampVerifyIdentityResult()

    /**
     * Identity verification failed due to an error.
     * @param error The error that caused the failure.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(val error: Throwable) : OnrampVerifyIdentityResult()
}
