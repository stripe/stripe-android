package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Indicates that the submitted signature does not prove control of the wallet.
 */
@ExperimentalCryptoOnramp
class InvalidWalletOwnershipSignatureApiException internal constructor(
    apiErrorContext: APIErrorContext,
    diagnosticContext: DiagnosticContext,
    userMessage: String,
) : CryptoOnrampApiException(
    apiErrorContext = apiErrorContext,
    userMessage = userMessage,
    developerMessage = CryptoOnrampErrorRenderer.renderDeveloperMessage(
        summary = apiErrorContext.apiErrorMessage
            ?: "Wallet ownership verification failed: the submitted signature does not prove control of the wallet.",
        code = apiErrorContext.code(fallback = INVALID_WALLET_OWNERSHIP_SIGNATURE_ERROR_CODE),
        nextStep = "Sign the exact challenge message with the registered wallet address, then submit the " +
            "resulting signature for the same challenge ID.",
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
        get() = apiErrorContext.code(fallback = INVALID_WALLET_OWNERSHIP_SIGNATURE_ERROR_CODE)
}

internal const val INVALID_WALLET_OWNERSHIP_SIGNATURE_ERROR_CODE = "crypto_onramp_invalid_wallet_ownership_signature"
