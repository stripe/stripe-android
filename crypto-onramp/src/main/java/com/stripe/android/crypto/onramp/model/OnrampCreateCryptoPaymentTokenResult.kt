package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

/**
 * Result of an Onramp create payment token operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampCreateCryptoPaymentTokenResult {

    /**
     * Creating the token was completed.
     * @param cryptoPaymentToken The token that was created.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Completed internal constructor(
        val cryptoPaymentToken: String
    ) : OnrampCreateCryptoPaymentTokenResult

    /**
     * Creating the token failed.
     * @param error The error that caused the failure
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampCreateCryptoPaymentTokenResult
}
