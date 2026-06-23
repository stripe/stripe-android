package com.stripe.android.crypto.onramp.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException
import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Base exception type for Crypto Onramp failures that originate from a Stripe API error payload.
 *
 * [apiErrorContext] preserves the original API-facing fields.
 *
 * `message` is safe to display directly to app users. Use [developerMessage] for richer
 * diagnostics.
 */
@ExperimentalCryptoOnramp
abstract class CryptoOnrampApiException internal constructor(
    val apiErrorContext: APIErrorContext,
    final override val userMessage: String,
    final override val developerMessage: String,
) : StripeException(
    stripeError = apiErrorContext.underlyingError.stripeError,
    requestId = apiErrorContext.underlyingError.requestId,
    statusCode = apiErrorContext.underlyingError.statusCode,
    cause = apiErrorContext.underlyingError,
    message = userMessage,
),
StripeCryptoOnrampError {
    final override val docUrl: String?
        get() = apiErrorContext.docUrl

    final override val underlyingError: StripeException
        get() = apiErrorContext.underlyingError

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String {
        return apiErrorContext.underlyingError.analyticsValue()
    }
}

data class APIErrorContext(
    /**
     * The raw backend reason, when present.
     */
    val reason: String?,
    /**
     * The raw backend error code, when present.
     */
    val apiErrorCode: String?,
    /**
     * The raw backend error type, when present.
     */
    val apiErrorType: String?,
    /**
     * The raw backend developer-facing message, when present.
     */
    val apiErrorMessage: String?,
    /**
     * The raw backend end-user-facing message, when present.
     */
    val apiUserMessage: String?,
    /**
     * A documentation URL for recovery guidance, when available.
     */
    val docUrl: String?,
    /**
     * The original Stripe exception that was mapped into this context.
     */
    val underlyingError: StripeException,
) {
    /**
     * The Stripe API request ID associated with this error, when available.
     */
    val requestId: String?
        get() = underlyingError.requestId
}

internal fun APIErrorContext.code(fallback: String): String {
    return apiErrorCode ?: fallback
}

internal data class DiagnosticContext(
    /**
     * SDK versions that participated in this operation.
     */
    val sdkVersions: List<SDKVersion>,
    /**
     * The Crypto Onramp operation that failed.
     */
    val operation: String,
    /**
     * The Android application package name used for the request.
     */
    val appPackageName: String,
    /**
     * The Stripe mode inferred from the publishable key, when available.
     */
    val mode: String?,
)
