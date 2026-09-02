package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Callback invoked when terms of service presentation completes.
 */
@ExperimentalCryptoOnramp
fun interface OnrampTermsOfServiceCallback {
    fun onResult(result: OnrampTermsOfServiceResult)
}

/**
 * Result of presenting and accepting the terms of service, if needed.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampTermsOfServiceResult {
    /**
     * The terms of service were accepted and recorded.
     */
    @ExperimentalCryptoOnramp
    class Accepted internal constructor() : OnrampTermsOfServiceResult

    /**
     * The current terms of service had already been accepted or did not require acceptance,
     * so no UI was presented.
     */
    @ExperimentalCryptoOnramp
    class NotRequired internal constructor() : OnrampTermsOfServiceResult

    /**
     * The terms of service presentation was cancelled.
     */
    @ExperimentalCryptoOnramp
    class Cancelled internal constructor() : OnrampTermsOfServiceResult

    /**
     * Retrieving, presenting, or recording the terms of service failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampTermsOfServiceResult
}
