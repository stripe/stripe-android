package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Indicates that Crypto Onramp does not support the requested wallet network for this operation.
 */
@ExperimentalCryptoOnramp
class UnsupportedNetworkException internal constructor(
    apiErrorContext: APIErrorContext,
    diagnosticContext: DiagnosticContext,
    userMessage: String,
) : CryptoOnrampApiException(
    apiErrorContext = apiErrorContext,
    userMessage = userMessage,
    developerMessage = CryptoOnrampErrorRenderer.renderDeveloperMessage(
        summary = apiErrorContext.apiErrorMessage
            ?: "Crypto Onramp doesn't support this wallet network for the requested operation.",
        code = apiErrorContext.code(fallback = CRYPTO_ONRAMP_UNSUPPORTED_NETWORK_ERROR_CODE),
        nextStep = "Use a network supported by Crypto Onramp for this operation, then retry the request.",
        docUrl = apiErrorContext.docUrl,
        sdkVersions = diagnosticContext.sdkVersions,
        requestContext = CryptoOnrampErrorRenderer.requestContextLines(
            diagnosticContext = diagnosticContext,
            reason = apiErrorContext.reason,
            requestId = apiErrorContext.requestId,
            apiErrorType = apiErrorContext.apiErrorType,
        ),
    ),
) {
    override val code: String
        get() = apiErrorContext.code(fallback = CRYPTO_ONRAMP_UNSUPPORTED_NETWORK_ERROR_CODE)
}

internal const val CRYPTO_ONRAMP_UNSUPPORTED_NETWORK_ERROR_CODE = "crypto_onramp_unsupported_network"
