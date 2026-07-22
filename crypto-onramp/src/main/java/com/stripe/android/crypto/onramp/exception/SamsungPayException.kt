package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Indicates that Crypto Onramp could not complete a Samsung Pay operation.
 *
 * [reason] and [code] provide a stable SDK-owned category. [samsungPayErrorCode]
 * preserves the raw Samsung Pay SDK error code when one was provided.
 */
@ExperimentalCryptoOnramp
class SamsungPayException internal constructor(
    val reason: Reason,
    val samsungPayErrorCode: Int?,
    override val underlyingError: Throwable?,
    override val userMessage: String,
    diagnosticContext: DiagnosticContext,
    developerSummary: String,
) : IllegalStateException(userMessage, underlyingError),
    StripeCryptoOnrampError {
    override val code: String
        get() = reason.code

    override val developerMessage: String = CryptoOnrampErrorRenderer.renderDeveloperMessage(
        summary = developerSummary,
        code = code,
        nextStep = reason.nextStep,
        docUrl = docUrl,
        sdkVersions = diagnosticContext.sdkVersions,
        requestContext = CryptoOnrampErrorRenderer.requestContextLines(
            diagnosticContext = diagnosticContext,
            reason = samsungPayErrorCode?.toString(),
        ),
    )

    override val docUrl: String?
        get() = null

    @ExperimentalCryptoOnramp
    enum class Reason(
        val code: String,
        internal val nextStep: String,
    ) {
        SdkUnavailable(
            code = "samsung_pay_sdk_unavailable",
            nextStep = "Include the Samsung Pay SDK 2.22.00 JAR in the client app, then retry.",
        ),
        SdkIncompatible(
            code = "samsung_pay_sdk_incompatible",
            nextStep = "Use Samsung Pay SDK 2.22.00, then rebuild and retry.",
        ),
        NotSupported(
            code = "samsung_pay_not_supported",
            nextStep = "Hide Samsung Pay and offer another supported payment method.",
        ),
        TemporarilyUnavailable(
            code = "samsung_pay_temporarily_unavailable",
            nextStep = "Offer another payment method or retry Samsung Pay later.",
        ),
        SetupRequired(
            code = "samsung_pay_setup_required",
            nextStep = "Ask the customer to finish Samsung Pay setup, then check availability again.",
        ),
        AppUpdateRequired(
            code = "samsung_pay_app_update_required",
            nextStep = "Ask the customer to update Samsung Wallet, then check availability again.",
        ),
        NotReady(
            code = "samsung_pay_not_ready",
            nextStep = "Hide Samsung Pay and offer another payment method until it becomes ready.",
        ),
        NotConfigured(
            code = "samsung_pay_not_configured",
            nextStep = "Provide a valid Samsung Pay configuration before presenting Samsung Pay.",
        ),
        InvalidConfiguration(
            code = "samsung_pay_invalid_configuration",
            nextStep = "Correct the Samsung Pay configuration or payment request, then retry.",
        ),
        PlatformKeyUnavailable(
            code = "samsung_pay_platform_key_unavailable",
            nextStep = "Ensure Crypto Onramp is configured for the customer, then retry.",
        ),
        PresentationFailed(
            code = "samsung_pay_presentation_failed",
            nextStep = "Retry presenting Samsung Pay or offer another payment method.",
        ),
        CredentialsFailed(
            code = "samsung_pay_credentials_failed",
            nextStep = "Retry Samsung Pay or ask the customer to choose another payment method.",
        ),
        OperationFailed(
            code = "samsung_pay_operation_failed",
            nextStep = "Inspect the underlying error and retry or offer another payment method.",
        ),
    }
}
