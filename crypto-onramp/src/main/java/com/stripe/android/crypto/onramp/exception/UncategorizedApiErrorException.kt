package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Indicates that a Stripe API request failed without a more specific Crypto Onramp category.
 */
@ExperimentalCryptoOnramp
class UncategorizedApiErrorException internal constructor(
    val context: APIErrorContext,
    fallbackUserMessage: String,
) : CryptoOnrampException(
    message = context.userMessage(fallbackUserMessage),
    developerMessage = buildGenericDeveloperMessage(context),
    cause = context.underlyingError,
) {
    override val userMessage: String = context.userMessage(fallbackUserMessage)
}
