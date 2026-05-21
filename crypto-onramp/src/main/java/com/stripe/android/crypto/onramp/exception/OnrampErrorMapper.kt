package com.stripe.android.crypto.onramp.exception

import android.content.Context
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsEvent

internal fun Throwable.toCryptoOnrampError(
    context: Context,
    operation: OnrampAnalyticsEvent.ErrorOccurred.Operation,
    publishableKey: String?,
): Throwable {
    if (this is CryptoOnrampException) return this

    val stripeException = this as? StripeException ?: return this
    val stripeError = stripeException.stripeError ?: return this

    return if (stripeError.isAppAttestationError()) {
        AppAttestationException(
            reason = stripeError.extraFields?.get(FIELD_REASON),
            operation = operation.value,
            appPackageName = context.packageName,
            mode = publishableKey.toMode(),
            sdkVersion = StripeSdkVersion.VERSION,
            apiErrorCode = stripeError.code,
            apiErrorMessage = stripeError.message,
            apiUserMessage = stripeError.extraFields?.get(FIELD_USER_MESSAGE),
            docUrl = stripeError.docUrl ?: APP_ATTESTATION_DOC_URL,
            cause = stripeException,
        )
    } else {
        this
    }
}

private fun StripeError.isAppAttestationError(): Boolean {
    return when {
        type == ERROR_TYPE_CANNOT_PROCEED -> code == ERROR_CODE_APP_ATTESTATION_FAILED
        code == ERROR_CODE_APP_ATTESTATION_FAILED -> true
        else -> false
    }
}

private fun String?.toMode(): String? {
    return when {
        this == null -> null
        startsWith("pk_live_") -> "live"
        startsWith("pk_test_") -> "test"
        else -> null
    }
}

private const val APP_ATTESTATION_DOC_URL = "https://stripe.com/docs/crypto/onramp/app-attestation"
private const val ERROR_CODE_APP_ATTESTATION_FAILED = "link_failed_to_attest_request"
private const val ERROR_TYPE_CANNOT_PROCEED = "cannot_proceed"
private const val FIELD_REASON = "reason"
private const val FIELD_USER_MESSAGE = "user_message"
