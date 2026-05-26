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

internal fun buildGenericDeveloperMessage(
    operation: String,
    appPackageName: String,
    mode: String?,
    sdkVersion: String,
    reason: String?,
    requestId: String?,
    apiErrorCode: String?,
    apiErrorType: String?,
    apiErrorMessage: String?,
    apiUserMessage: String?,
    docUrl: String?,
): String {
    return buildDeveloperMessage(
        summary = apiErrorMessage ?: "Stripe API request failed.",
        operation = operation,
        appPackageName = appPackageName,
        mode = mode,
        reason = reason,
        requestId = requestId,
        apiErrorCode = apiErrorCode,
        apiErrorType = apiErrorType,
        nextStep = apiUserMessage.orFallbackTo(
            "Inspect the preserved Stripe API error for details and retry after correcting the request."
        ),
        docUrl = docUrl,
        sdkVersion = sdkVersion,
    )
}

internal fun buildDeveloperMessage(
    summary: String,
    operation: String,
    appPackageName: String,
    mode: String?,
    reason: String?,
    requestId: String?,
    apiErrorCode: String?,
    apiErrorType: String?,
    nextStep: String,
    docUrl: String?,
    sdkVersion: String,
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

private fun String?.orFallbackTo(fallback: String): String {
    return this?.takeIf { it.isNotBlank() } ?: fallback
}
