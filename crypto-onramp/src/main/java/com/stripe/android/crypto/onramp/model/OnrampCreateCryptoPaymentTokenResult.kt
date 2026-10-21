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
     * @param cardDetails Card summary for the instrument used to create the token, when available.
     * Supported for Google Pay, Samsung Pay, and Link cards, including direct card entry.
     * @param bankAccountDetails Bank account summary for a Link bank account/ACH selection, when available.
     * At most one summary is populated. Both are null for unsupported or unavailable instrument details.
     */
    @ExperimentalCryptoOnramp
    class Completed internal constructor(
        val cryptoPaymentToken: String,
        val cardDetails: OnrampCardDetails?,
        val bankAccountDetails: OnrampBankAccountDetails?,
    ) : OnrampCreateCryptoPaymentTokenResult {
        init {
            require(cardDetails == null || bankAccountDetails == null) {
                "A token cannot have both card and bank account details"
            }
        }

        internal constructor(cryptoPaymentToken: String) : this(cryptoPaymentToken, null, null)

        internal constructor(cryptoPaymentToken: String, cardDetails: OnrampCardDetails?) :
            this(cryptoPaymentToken, cardDetails, null)
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
