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
    val developerMessage: String,
    cause: Throwable,
) : StripeException(
    stripeError = (cause as? StripeException)?.stripeError,
    requestId = (cause as? StripeException)?.requestId,
    statusCode = (cause as? StripeException)?.statusCode ?: DEFAULT_STATUS_CODE,
    cause = cause,
    message = message,
) {
    /**
     * An end-user-facing message, when available.
     */
    abstract val userMessage: String

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun analyticsValue(): String {
        return (cause as? StripeException)?.analyticsValue() ?: super.analyticsValue()
    }
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
     * The SDK version that produced the request.
     */
    val sdkVersion: String,
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

    fun developerMessage(
        summary: String,
        nextStep: String,
    ): String {
        val context = listOfNotNull(
            "  - operation: $operation",
            "  - app_id: $appPackageName",
            mode?.let { "  - mode: $it" },
            reason?.let { "  - reason: $it" },
            requestId?.let { "  - request_id: $it" },
            apiErrorCode?.let { "  - code: $it" },
            apiErrorType?.let { "  - type: $it" },
        )

        return buildList {
            add("Summary")
            add("  $summary")
            add("")
            add("Context")
            addAll(context)
            add("")
            add("Next step")
            add("  $nextStep")
            docUrl?.let {
                add("")
                add("Docs")
                add("  $it")
            }
            add("")
            add("SDK")
            add("  stripe-android@$sdkVersion")
        }.joinToString(separator = "\n")
    }
}

internal fun buildGenericDeveloperMessage(
    context: APIErrorContext,
): String {
    return context.developerMessage(
        summary = context.apiErrorMessage ?: "Stripe API request failed.",
        nextStep = context.apiUserMessage.orFallbackTo(
            "Inspect the preserved Stripe API error for details and retry after correcting the request."
        ),
    )
}

private fun String?.orFallbackTo(fallback: String): String {
    return this?.takeIf { it.isNotBlank() } ?: fallback
}
