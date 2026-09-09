package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Wraps an unexpected failure without structured API metadata in the common Crypto Onramp error contract.
 */
@ExperimentalCryptoOnramp
internal class UnexpectedException(
    override val underlyingError: Throwable,
    private val diagnosticContext: DiagnosticContext,
    override val userMessage: String,
) : IllegalStateException(userMessage, underlyingError),
    StripeCryptoOnrampError {
    override val code: String
        get() = UNEXPECTED_ERROR_CODE

    override val developerMessage: String
        get() = CryptoOnrampErrorRenderer.renderDeveloperMessage(
            summary = underlyingError.message ?: "An unexpected Crypto Onramp error occurred.",
            code = code,
            nextStep = "Inspect the underlying error and request context, then retry or report the issue to Stripe.",
            docUrl = docUrl,
            sdkVersions = diagnosticContext.sdkVersions,
            requestContext = CryptoOnrampErrorRenderer.requestContextLines(
                diagnosticContext = diagnosticContext,
            ),
        )

    override val docUrl: String?
        get() = null
}

private const val UNEXPECTED_ERROR_CODE = "unexpected_error"
