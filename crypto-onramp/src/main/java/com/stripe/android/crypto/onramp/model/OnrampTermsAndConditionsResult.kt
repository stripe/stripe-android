package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Callback invoked when terms and conditions presentation completes.
 */
@ExperimentalCryptoOnramp
fun interface OnrampTermsAndConditionsCallback {
    fun onResult(result: OnrampTermsAndConditionsResult)
}

/**
 * Result of presenting and accepting the terms and conditions, if needed.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampTermsAndConditionsResult {
    /**
     * The terms and conditions were accepted and recorded.
     */
    @ExperimentalCryptoOnramp
    class Accepted internal constructor() : OnrampTermsAndConditionsResult

    /**
     * The current terms and conditions had already been accepted, so no UI was presented.
     */
    @ExperimentalCryptoOnramp
    class AlreadyAccepted internal constructor() : OnrampTermsAndConditionsResult

    /**
     * The terms and conditions presentation was cancelled.
     */
    @ExperimentalCryptoOnramp
    class Cancelled internal constructor() : OnrampTermsAndConditionsResult

    /**
     * Retrieving, presenting, or recording the terms and conditions failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampTermsAndConditionsResult
}
