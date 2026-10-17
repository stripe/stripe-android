package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Callback invoked when partner terms presentation completes.
 */
@ExperimentalCryptoOnramp
fun interface OnrampPartnerTermsCallback {
    fun onResult(result: OnrampPartnerTermsResult)
}

/**
 * Result of checking for and presenting partner terms.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampPartnerTermsResult {
    /**
     * The customer accepted the current partner terms.
     */
    @ExperimentalCryptoOnramp
    class Accepted internal constructor() : OnrampPartnerTermsResult

    /**
     * The customer had already accepted the current partner terms or was not required to accept
     * them, so no UI was presented.
     */
    @ExperimentalCryptoOnramp
    class NotRequired internal constructor() : OnrampPartnerTermsResult

    /**
     * The customer dismissed the partner terms without accepting.
     */
    @ExperimentalCryptoOnramp
    class Cancelled internal constructor() : OnrampPartnerTermsResult

    /**
     * Retrieving, presenting, or recording the partner terms failed.
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampPartnerTermsResult
}
