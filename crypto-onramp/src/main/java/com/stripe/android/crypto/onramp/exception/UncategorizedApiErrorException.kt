package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Indicates that a Stripe API request failed without a more specific Crypto Onramp category.
 */
@ExperimentalCryptoOnramp
class UncategorizedApiErrorException internal constructor(
    context: APIErrorContext,
    fallbackUserMessage: String,
) : CryptoOnrampApiException(
    context = context,
    message = context.userMessage(fallbackUserMessage),
    developerMessage = CryptoOnrampErrorRenderer.renderGenericApiDeveloperMessage(context),
) {
    override val userMessage: String = context.userMessage(fallbackUserMessage)
}
