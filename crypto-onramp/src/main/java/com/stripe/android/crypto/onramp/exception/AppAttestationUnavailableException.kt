package com.stripe.android.crypto.onramp.exception

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Indicates that Crypto Onramp could not use native Link because app attestation is unavailable.
 */
@ExperimentalCryptoOnramp
class AppAttestationUnavailableException internal constructor(
    override val underlyingError: Throwable,
    override val sdkVersions: List<SDKVersion>,
    override val userMessage: String,
) : Exception(userMessage, underlyingError),
    StripeCryptoOnrampError {
    override val code: String
        get() = APP_ATTESTATION_UNAVAILABLE_REASON

    override val developerMessage: String
        get() = CryptoOnrampErrorRenderer.renderDeveloperMessage(
            developerBody = APP_ATTESTATION_UNAVAILABLE_DEVELOPER_BODY,
            code = code,
            nextStep = APP_ATTESTATION_UNAVAILABLE_NEXT_STEP,
            docUrl = docUrl,
            sdkVersions = sdkVersions,
        )

    override val docUrl: String?
        get() = null
}

private const val APP_ATTESTATION_UNAVAILABLE_DEVELOPER_BODY =
    "App attestation unavailable: this app isn't configured to use Stripe Crypto Onramp.\n\n" +
        "This usually means app attestation isn't enabled for this Stripe account, or this app " +
        "isn't registered as a trusted application. Use your Android package name and contact " +
        "Stripe to enable app attestation or register the app for this account."
