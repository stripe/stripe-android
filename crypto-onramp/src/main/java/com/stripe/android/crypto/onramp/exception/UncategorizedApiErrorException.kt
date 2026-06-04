package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Indicates that a Stripe API request failed without a more specific Crypto Onramp category.
 */
@ExperimentalCryptoOnramp
class UncategorizedApiErrorException internal constructor(
    context: APIErrorContext,
    sdkVersions: List<SDKVersion>,
    fallbackUserMessage: String,
) : CryptoOnrampApiException(
    context = context,
    sdkVersions = sdkVersions,
    userMessage = context.userMessage(fallbackUserMessage),
    developerMessage = CryptoOnrampErrorRenderer.renderGenericApiDeveloperMessage(
        context = context,
        sdkVersions = sdkVersions,
    ),
)
