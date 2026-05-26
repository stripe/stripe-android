package com.stripe.android.crypto.onramp.exception

import androidx.annotation.RestrictTo
import com.stripe.android.core.StripeError
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

/**
 * Indicates that Crypto Onramp could not verify the Android app attestation data.
 */
@ExperimentalCryptoOnramp
class AppAttestationException internal constructor(
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
    developerMessage = buildAppAttestationDeveloperMessage(
        operation = operation,
        appPackageName = appPackageName,
        mode = mode,
        sdkVersion = sdkVersion,
        reason = reason,
        requestId = (cause as? StripeException)?.requestId,
        apiErrorCode = apiErrorCode,
        apiErrorType = apiErrorType,
        apiErrorMessage = apiErrorMessage,
        docUrl = docUrl,
    ),
    cause = cause,
) {
    override val userMessage: String = apiUserMessage ?: fallbackUserMessage.value
}

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

private const val ATTESTATION_NOT_ENABLED_REASON = "attestation_not_enabled"
private const val APP_NOT_REGISTERED_REASON = "app_not_registered"
private const val ATTESTATION_DATA_MISSING_REASON = "attestation_data_missing"
private const val APP_NOT_PLAY_RECOGNIZED_REASON = "app_not_play_recognized"
private const val ANDROID_PACKAGE_NAME_MISMATCH_REASON = "android_package_name_mismatch"
private const val ANDROID_ENVIRONMENT_MISMATCH_REASON = "android_environment_mismatch"
private const val ANDROID_VERDICT_VALIDATION_FAILED_REASON = "android_verdict_validation_failed"

private const val ATTESTATION_NOT_ENABLED_DESCRIPTION =
    "app attestation is not enabled for this Stripe account"
private const val APP_NOT_REGISTERED_DESCRIPTION =
    "this app is not registered as a trusted application"
private const val ATTESTATION_DATA_MISSING_DESCRIPTION =
    "attestation data is missing or incomplete"
private const val APP_NOT_PLAY_RECOGNIZED_DESCRIPTION =
    "this app is not recognized by Google Play"
private const val ANDROID_PACKAGE_NAME_MISMATCH_DESCRIPTION =
    "the app package name does not match the package name registered for this Stripe account"
private const val ANDROID_ENVIRONMENT_MISMATCH_DESCRIPTION =
    "the Play Integrity distribution channel does not match this Stripe mode"
private const val ANDROID_VERDICT_VALIDATION_FAILED_DESCRIPTION =
    "the Play Integrity verdict could not be validated"

private const val ATTESTATION_NOT_ENABLED_NEXT_STEP =
    "Contact Stripe to enable app attestation for this account and mode, then retry the Onramp flow."
private const val APP_NOT_REGISTERED_NEXT_STEP =
    "Register this app's package name as a trusted application with Stripe, then retry the Onramp flow."
private const val ATTESTATION_DATA_MISSING_NEXT_STEP =
    "Make sure all required Play Integrity fields are sent with the request, then retry the Onramp flow."
private const val APP_NOT_PLAY_RECOGNIZED_NEXT_STEP =
    "Install the app from a Google Play testing or production track and retry the Onramp flow. Internal, closed, open testing, and production tracks are supported. Debug builds and sideloaded APKs will not pass this check."
private const val ANDROID_PACKAGE_NAME_MISMATCH_NEXT_STEP =
    "Use the package name registered for this Stripe account, then retry the Onramp flow."
private const val ANDROID_ENVIRONMENT_MISMATCH_NEXT_STEP =
    "Install the app from the Google Play track that matches this Stripe mode, then retry the Onramp flow."
private const val ANDROID_VERDICT_VALIDATION_FAILED_NEXT_STEP =
    "Generate a new Play Integrity verdict and retry the Onramp flow. If the issue persists, check your app attestation configuration."

private fun appAttestationSummary(reason: String?): String? {
    return appAttestationDescription(reason)?.let(::attestationSummary)
}

private fun appAttestationDescription(reason: String?): String? {
    return when (reason) {
        ATTESTATION_NOT_ENABLED_REASON -> ATTESTATION_NOT_ENABLED_DESCRIPTION
        APP_NOT_REGISTERED_REASON -> APP_NOT_REGISTERED_DESCRIPTION
        ATTESTATION_DATA_MISSING_REASON -> ATTESTATION_DATA_MISSING_DESCRIPTION
        APP_NOT_PLAY_RECOGNIZED_REASON -> APP_NOT_PLAY_RECOGNIZED_DESCRIPTION
        ANDROID_PACKAGE_NAME_MISMATCH_REASON -> ANDROID_PACKAGE_NAME_MISMATCH_DESCRIPTION
        ANDROID_ENVIRONMENT_MISMATCH_REASON -> ANDROID_ENVIRONMENT_MISMATCH_DESCRIPTION
        ANDROID_VERDICT_VALIDATION_FAILED_REASON -> ANDROID_VERDICT_VALIDATION_FAILED_DESCRIPTION
        else -> null
    }
}

private fun appAttestationNextStep(reason: String?): String? {
    return when (reason) {
        ATTESTATION_NOT_ENABLED_REASON -> ATTESTATION_NOT_ENABLED_NEXT_STEP
        APP_NOT_REGISTERED_REASON -> APP_NOT_REGISTERED_NEXT_STEP
        ATTESTATION_DATA_MISSING_REASON -> ATTESTATION_DATA_MISSING_NEXT_STEP
        APP_NOT_PLAY_RECOGNIZED_REASON -> APP_NOT_PLAY_RECOGNIZED_NEXT_STEP
        ANDROID_PACKAGE_NAME_MISMATCH_REASON -> ANDROID_PACKAGE_NAME_MISMATCH_NEXT_STEP
        ANDROID_ENVIRONMENT_MISMATCH_REASON -> ANDROID_ENVIRONMENT_MISMATCH_NEXT_STEP
        ANDROID_VERDICT_VALIDATION_FAILED_REASON -> ANDROID_VERDICT_VALIDATION_FAILED_NEXT_STEP
        else -> null
    }
}

private fun attestationSummary(description: String): String {
    return "App attestation failed: $description."
}

private fun buildAppAttestationDeveloperMessage(
    operation: String,
    appPackageName: String,
    mode: String?,
    sdkVersion: String,
    reason: String?,
    requestId: String?,
    apiErrorCode: String?,
    apiErrorType: String?,
    apiErrorMessage: String?,
    docUrl: String?,
): String {
    return buildDeveloperMessage(
        summary = appAttestationSummary(reason) ?: (apiErrorMessage ?: "App attestation failed."),
        operation = operation,
        appPackageName = appPackageName,
        mode = mode,
        reason = reason,
        requestId = requestId,
        apiErrorCode = apiErrorCode,
        apiErrorType = apiErrorType,
        nextStep = appAttestationNextStep(reason)
            ?: (apiErrorMessage
                ?: "Inspect the preserved Stripe API error for details and retry after correcting the app attestation configuration."),
        docUrl = docUrl,
        sdkVersion = sdkVersion,
    )
}

private fun buildGenericDeveloperMessage(
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

private fun String?.orFallbackTo(fallback: String): String {
    return this?.takeIf { it.isNotBlank() } ?: fallback
}

private fun buildDeveloperMessage(
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
