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
     * @param cardDetails Card summary for the same selected wallet payment method as the token.
     * Available for Google Pay and Samsung Pay when card data is present; null for Link.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val cryptoPaymentToken: String,
        val cardDetails: OnrampCardDetails?,
    ) : OnrampCreateCryptoPaymentTokenResult {
        internal constructor(cryptoPaymentToken: String) : this(cryptoPaymentToken, null)
    }

    /**
     * Creating the token failed.
     * @param error The error that caused the failure
     */
    @ExperimentalCryptoOnramp
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampCreateCryptoPaymentTokenResult
}
