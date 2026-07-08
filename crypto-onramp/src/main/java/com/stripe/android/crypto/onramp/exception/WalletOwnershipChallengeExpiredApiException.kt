package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Indicates that the wallet ownership challenge expired before the signature was submitted.
 */
@ExperimentalCryptoOnramp
class WalletOwnershipChallengeExpiredApiException internal constructor(
    apiErrorContext: APIErrorContext,
    diagnosticContext: DiagnosticContext,
    userMessage: String,
) : CryptoOnrampApiException(
    apiErrorContext = apiErrorContext,
    userMessage = userMessage,
    developerMessage = CryptoOnrampErrorRenderer.renderDeveloperMessage(
        summary = apiErrorContext.apiErrorMessage
            ?: "Wallet ownership verification failed: the challenge has expired.",
        code = apiErrorContext.code(fallback = WALLET_OWNERSHIP_CHALLENGE_EXPIRED_ERROR_CODE),
        nextStep = "Request a new wallet ownership challenge and submit a fresh signature before it expires.",
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
        get() = apiErrorContext.code(fallback = WALLET_OWNERSHIP_CHALLENGE_EXPIRED_ERROR_CODE)
}

internal const val WALLET_OWNERSHIP_CHALLENGE_EXPIRED_ERROR_CODE = "crypto_onramp_wallet_ownership_challenge_expired"
