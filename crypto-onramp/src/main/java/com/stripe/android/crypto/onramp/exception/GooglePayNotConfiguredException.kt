package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Google Pay was not configured before the Onramp presenter was created.
 */
@ExperimentalCryptoOnramp
internal class GooglePayNotConfiguredException(
    override val userMessage: String,
) : IllegalStateException(userMessage), StripeCryptoOnrampError {
    override val code: String = "google_pay_not_configured"
    override val developerMessage: String =
        "Google Pay is not configured for this onramp presenter. " +
            "Set OnrampConfiguration.googlePayConfig and call configure successfully before creating the presenter."
    override val docUrl: String? = null
    override val underlyingError: Throwable? = null
}
