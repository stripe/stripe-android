package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Indicates that a Stripe API request failed without a more specific Crypto Onramp category.
 */
@ExperimentalCryptoOnramp
class UncategorizedApiErrorException internal constructor(
    context: APIErrorContext,
    diagnosticContext: DiagnosticContext,
    sdkVersions: List<SDKVersion>,
    userMessage: String,
) : CryptoOnrampApiException(
    context = context,
    sdkVersions = sdkVersions,
    userMessage = userMessage,
    developerMessage = CryptoOnrampErrorRenderer.renderDeveloperMessage(
        summary = context.apiErrorMessage ?: "Stripe API request failed.",
        code = context.code(fallback = UNCATEGORIZED_API_ERROR_CODE),
        nextStep = "Inspect the preserved Stripe API error for details and retry after correcting the request.",
        docUrl = context.docUrl,
        sdkVersions = sdkVersions,
        requestContext = CryptoOnrampErrorRenderer.requestContextLines(
            diagnosticContext = diagnosticContext,
            reason = context.reason,
            requestId = context.requestId,
            apiErrorType = context.apiErrorType,
        ),
    ),
) {
    override val code: String
        get() = context.code(fallback = UNCATEGORIZED_API_ERROR_CODE)
}

private const val UNCATEGORIZED_API_ERROR_CODE = "uncategorized_api_error"
