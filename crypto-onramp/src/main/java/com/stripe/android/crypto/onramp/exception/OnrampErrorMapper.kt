package com.stripe.android.crypto.onramp.exception

import android.content.Context
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.crypto.onramp.R
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsEvent

internal fun Throwable.toCryptoOnrampError(
    context: Context,
    operation: OnrampAnalyticsEvent.ErrorOccurred.Operation,
    publishableKey: String?,
): Throwable {
    if (this is CryptoOnrampException) return this

    val stripeException = this as? StripeException ?: return this
    val stripeError = stripeException.stripeError ?: return this

    val reason = stripeError.extraFields?.get(FIELD_REASON)
    val apiUserMessage = stripeError.extraFields?.get(FIELD_USER_MESSAGE)?.takeIf { it.isNotBlank() }

    return if (stripeError.isAppAttestationError()) {
        AppAttestationException(
            reason = reason,
            operation = operation.value,
            appPackageName = context.packageName,
            mode = publishableKey.toMode(),
            sdkVersion = StripeSdkVersion.VERSION,
            apiErrorCode = stripeError.code,
            apiErrorMessage = stripeError.message,
            apiUserMessage = apiUserMessage,
            docUrl = stripeError.docUrl,
            fallbackUserMessage = apiUserMessage
                ?: reason.appAttestationUserMessageResId?.let(context::getString)
                ?: context.getString(R.string.stripe_onramp_app_attestation_default_user_message),
            cause = stripeException,
        )
    } else {
        UncategorizedApiErrorException(
            rawReason = reason,
            operation = operation.value,
            appPackageName = context.packageName,
            mode = publishableKey.toMode(),
            sdkVersion = StripeSdkVersion.VERSION,
            apiErrorCode = stripeError.code,
            apiErrorMessage = stripeError.message,
            apiUserMessage = apiUserMessage,
            docUrl = stripeError.docUrl,
            fallbackUserMessage = apiUserMessage
                ?: context.getString(R.string.stripe_onramp_default_api_error_user_message),
            cause = stripeException,
        )
    }
}

private fun StripeError.isAppAttestationError(): Boolean {
    return code == ERROR_CODE_APP_ATTESTATION_FAILED
}

private fun String?.toMode(): String? {
    return when {
        this == null -> null
        startsWith("pk_live_") -> "live"
        startsWith("pk_test_") -> "test"
        else -> null
    }
}

private const val ERROR_CODE_APP_ATTESTATION_FAILED = "link_failed_to_attest_request"
private const val FIELD_REASON = "reason"
private const val FIELD_USER_MESSAGE = "user_message"
