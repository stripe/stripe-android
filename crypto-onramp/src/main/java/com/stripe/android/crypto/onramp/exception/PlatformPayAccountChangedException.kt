package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * The wallet payment method's platform account differs from the customer's current account,
 * or its collection account is unknown. Collect the wallet payment method again, then retry token creation.
 */
@ExperimentalCryptoOnramp
class PlatformPayAccountChangedException internal constructor(
    override val userMessage: String,
) : IllegalStateException(userMessage), StripeCryptoOnrampError {
    override val code: String = "platform_pay_account_changed"
    override val developerMessage: String =
        "The wallet collection account is unknown or differs from the current account. " +
            "Call collectPaymentMethod again for the wallet, then retry createCryptoPaymentToken."
    override val docUrl: String? = null
    override val underlyingError: Throwable? = null
}
