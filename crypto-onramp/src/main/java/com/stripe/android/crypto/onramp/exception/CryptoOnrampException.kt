package com.stripe.android.crypto.onramp.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.StripeException
import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Base exception type for Crypto Onramp failures that expose SDK-owned recovery guidance.
 *
 * `message` is safe to display directly to app users. Use [developerMessage] for richer diagnostics.
 */
@ExperimentalCryptoOnramp
abstract class CryptoOnrampException internal constructor(
    message: String,
    final override val developerMessage: String,
    final override val sdkVersion: String,
    /**
     * The original Stripe exception that was wrapped by this richer Crypto Onramp error.
     */
    private val stripeException: StripeException,
) : StripeException(
    stripeError = stripeException.stripeError,
    requestId = stripeException.requestId,
    statusCode = stripeException.statusCode,
    cause = stripeException,
    message = message,
),
StripeCryptoOnrampError {
    /**
     * An end-user-facing message, when available.
     */
    abstract override val userMessage: String

    abstract override val code: String?

    abstract override val docUrl: String?

    final override val underlyingError: StripeException
        get() = stripeException

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String {
        return stripeException.analyticsValue()
    }
}

/**
 * Base exception type for Crypto Onramp failures that originate from a Stripe API error payload.
 *
 * [context] preserves the original API-facing fields and SDK-local debugging metadata.
 */
@ExperimentalCryptoOnramp
abstract class CryptoOnrampApiException internal constructor(
    val context: APIErrorContext,
    message: String,
    developerMessage: String,
    sdkVersion: String,
) : CryptoOnrampException(
    message = message,
    developerMessage = developerMessage,
    sdkVersion = sdkVersion,
    stripeException = context.underlyingError,
) {
    final override val code: String?
        get() = context.apiErrorCode

    final override val docUrl: String?
        get() = context.docUrl
}

data class APIErrorContext(
    /**
     * The raw backend reason, when present.
     */
    val reason: String?,
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

internal fun APIErrorContext.userMessage(fallbackUserMessage: String): String {
    return apiUserMessage ?: fallbackUserMessage
}
