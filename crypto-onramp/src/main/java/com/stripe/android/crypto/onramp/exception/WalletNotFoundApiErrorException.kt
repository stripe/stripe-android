package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Indicates that Crypto Onramp could not find the requested wallet for the authenticated consumer.
 */
@ExperimentalCryptoOnramp
class WalletNotFoundApiErrorException internal constructor(
    apiErrorContext: APIErrorContext,
    diagnosticContext: DiagnosticContext,
    userMessage: String,
) : CryptoOnrampApiException(
    apiErrorContext = apiErrorContext,
    userMessage = userMessage,
    developerMessage = CryptoOnrampErrorRenderer.renderDeveloperMessage(
        summary = apiErrorContext.apiErrorMessage
            ?: "Crypto Onramp couldn't find the wallet for the authenticated consumer.",
        code = apiErrorContext.code(fallback = CRYPTO_ONRAMP_WALLET_NOT_FOUND_ERROR_CODE),
        nextStep = "Use a wallet registered to the authenticated consumer, or register the wallet " +
            "before retrying the request.",
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
        get() = apiErrorContext.code(fallback = CRYPTO_ONRAMP_WALLET_NOT_FOUND_ERROR_CODE)
}

internal const val CRYPTO_ONRAMP_WALLET_NOT_FOUND_ERROR_CODE = "crypto_onramp_wallet_not_found"
