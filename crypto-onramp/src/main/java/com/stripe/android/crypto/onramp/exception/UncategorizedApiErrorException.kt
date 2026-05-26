package com.stripe.android.crypto.onramp.exception

import com.stripe.android.core.exception.StripeException
import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Indicates that a Stripe API request failed without a more specific Crypto Onramp category.
 */
@ExperimentalCryptoOnramp
class UncategorizedApiErrorException internal constructor(
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
    fallbackUserMessage: Lazy<String>,
    cause: Throwable,
) : CryptoOnrampException(
    message = apiUserMessage ?: fallbackUserMessage.value,
    developerMessage = buildGenericDeveloperMessage(
        operation = operation,
        appPackageName = appPackageName,
        mode = mode,
        sdkVersion = sdkVersion,
        reason = reason,
        requestId = (cause as? StripeException)?.requestId,
        apiErrorCode = apiErrorCode,
        apiErrorType = apiErrorType,
        apiErrorMessage = apiErrorMessage,
        apiUserMessage = apiUserMessage,
        docUrl = docUrl,
    ),
    cause = cause,
) {
    override val userMessage: String = apiUserMessage ?: fallbackUserMessage.value
}
