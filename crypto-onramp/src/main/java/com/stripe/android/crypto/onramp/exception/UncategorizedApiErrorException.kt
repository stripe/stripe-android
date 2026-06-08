package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Indicates that a Stripe API request failed without a more specific Crypto Onramp category.
 */
@ExperimentalCryptoOnramp
class UncategorizedApiErrorException internal constructor(
    context: APIErrorContext,
    sdkVersions: List<SDKVersion>,
    userMessage: String,
) : CryptoOnrampApiException(
    context = context,
    sdkVersions = sdkVersions,
    userMessage = userMessage,
    developerMessage = CryptoOnrampErrorRenderer.renderGenericApiDeveloperMessage(
        context = context,
        code = context.code(fallback = UNCATEGORIZED_API_ERROR_CODE),
        sdkVersions = sdkVersions,
    ),
) {
    override val code: String
        get() = context.code(fallback = UNCATEGORIZED_API_ERROR_CODE)
}

private const val UNCATEGORIZED_API_ERROR_CODE = "uncategorized_api_error"
