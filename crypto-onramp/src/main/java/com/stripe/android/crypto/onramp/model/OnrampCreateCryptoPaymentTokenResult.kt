package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Result of an Onramp create payment token operation.
 */
@ExperimentalCryptoOnramp
sealed interface OnrampCreateCryptoPaymentTokenResult {

    /**
     * Creating the token was completed.
     * @param cryptoPaymentToken The token that was created.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val cryptoPaymentToken: String
    ) : OnrampCreateCryptoPaymentTokenResult

    /**
     * Creating the token failed.
     * @param error The error that caused the failure
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampCreateCryptoPaymentTokenResult
}
