package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Indicates that the wallet ownership challenge is invalid for this verification attempt.
 */
@ExperimentalCryptoOnramp
class InvalidWalletOwnershipChallengeApiException internal constructor(
    apiErrorContext: APIErrorContext,
    diagnosticContext: DiagnosticContext,
    userMessage: String,
) : CryptoOnrampApiException(
    apiErrorContext = apiErrorContext,
    userMessage = userMessage,
    developerMessage = CryptoOnrampErrorRenderer.renderDeveloperMessage(
        summary = apiErrorContext.apiErrorMessage
            ?: "Wallet ownership verification failed: the challenge is invalid, already consumed, missing, " +
            "or not associated with the authenticated consumer.",
        code = apiErrorContext.code(fallback = INVALID_WALLET_OWNERSHIP_CHALLENGE_ERROR_CODE),
        nextStep = "Request a new challenge for the registered wallet and authenticated consumer, then submit " +
            "that challenge ID with its signature.",
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
        get() = apiErrorContext.code(fallback = INVALID_WALLET_OWNERSHIP_CHALLENGE_ERROR_CODE)
}

internal const val INVALID_WALLET_OWNERSHIP_CHALLENGE_ERROR_CODE = "crypto_onramp_invalid_wallet_ownership_challenge"
