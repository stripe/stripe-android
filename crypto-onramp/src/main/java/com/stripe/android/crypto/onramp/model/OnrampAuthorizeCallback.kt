package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

@ExperimentalCryptoOnramp
fun interface OnrampAuthorizeCallback {
    fun onResult(result: OnrampAuthorizeResult)
}

/**
 * Result of an OnRamp authorize operation.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampAuthorizeResult {
    /**
     * The user granted consent to the scopes requested by the LinkAuthIntent.
     * @param customerId The crypto customer id that matches the authenticated account.
     */
    @ExperimentalCryptoOnramp
    class Consented internal constructor(
        val customerId: String
    ) : OnrampAuthorizeResult

    /**
     * The user denied consent to the scopes requested by the LinkAuthIntent.
     */
    @ExperimentalCryptoOnramp
    class Denied internal constructor() : OnrampAuthorizeResult

    /**
     * The user canceled the authorization.
     */
    @ExperimentalCryptoOnramp
    class Canceled internal constructor() : OnrampAuthorizeResult

    /**
     * Authorization failed.
     * @param error The error that caused the failure
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampAuthorizeResult
}
