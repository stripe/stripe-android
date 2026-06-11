package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Indicates that Crypto Onramp could not verify the Android app attestation data.
 */
@ExperimentalCryptoOnramp
class AppAttestationException internal constructor(
    context: APIErrorContext,
    sdkVersions: List<SDKVersion>,
    userMessage: String,
) : CryptoOnrampApiException(
    context = context,
    sdkVersions = sdkVersions,
    userMessage = userMessage,
    developerMessage = buildAppAttestationDeveloperMessage(
        context = context,
        code = context.code(fallback = APP_ATTESTATION_ERROR_CODE),
        sdkVersions = sdkVersions,
    ),
) {
    override val code: String
        get() = context.code(fallback = APP_ATTESTATION_ERROR_CODE)
}

private const val ATTESTATION_NOT_ENABLED_REASON = "attestation_not_enabled"
internal const val APP_ATTESTATION_ERROR_CODE = "link_failed_to_attest_request"
internal const val APP_ATTESTATION_UNAVAILABLE_REASON = "app_attestation_unavailable"
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
private const val APP_ATTESTATION_UNAVAILABLE_NEXT_STEP =
    "Confirm app attestation is enabled for this Stripe account and that this app's package name is registered as trusted, then call configure again."
private const val DEFAULT_ATTESTATION_NEXT_STEP =
    "Inspect the preserved Stripe API error for details and retry after correcting the app attestation configuration."

private fun appAttestationSummary(reason: String?): String? {
    if (reason == APP_ATTESTATION_UNAVAILABLE_REASON) {
        return "App attestation unavailable: this app isn't configured to use Stripe Crypto Onramp."
    }

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

private fun appAttestationNextStep(reason: String?): String {
    return when (reason) {
        ATTESTATION_NOT_ENABLED_REASON -> ATTESTATION_NOT_ENABLED_NEXT_STEP
        APP_ATTESTATION_UNAVAILABLE_REASON -> APP_ATTESTATION_UNAVAILABLE_NEXT_STEP
        APP_NOT_REGISTERED_REASON -> APP_NOT_REGISTERED_NEXT_STEP
        ATTESTATION_DATA_MISSING_REASON -> ATTESTATION_DATA_MISSING_NEXT_STEP
        APP_NOT_PLAY_RECOGNIZED_REASON -> APP_NOT_PLAY_RECOGNIZED_NEXT_STEP
        ANDROID_PACKAGE_NAME_MISMATCH_REASON -> ANDROID_PACKAGE_NAME_MISMATCH_NEXT_STEP
        ANDROID_ENVIRONMENT_MISMATCH_REASON -> ANDROID_ENVIRONMENT_MISMATCH_NEXT_STEP
        ANDROID_VERDICT_VALIDATION_FAILED_REASON -> ANDROID_VERDICT_VALIDATION_FAILED_NEXT_STEP
        else -> DEFAULT_ATTESTATION_NEXT_STEP
    }
}

private fun attestationSummary(description: String): String {
    return "App attestation failed: $description."
}

private fun buildAppAttestationDeveloperMessage(
    context: APIErrorContext,
    code: String,
    sdkVersions: List<SDKVersion>,
): String {
    return CryptoOnrampErrorRenderer.renderApiDeveloperMessage(
        context = context,
        summary = appAttestationSummary(context.reason)
            ?: (context.apiErrorMessage ?: "App attestation failed."),
        code = code,
        nextStep = appAttestationNextStep(context.reason),
        sdkVersions = sdkVersions,
    )
}
