package com.stripe.android.financialconnections.utils

import android.net.Uri
import android.util.Base64
import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.launcher.InstantDebitsResult

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InstantDebitsResultBuilder {
    fun fromUri(
        uri: Uri,
    ): Result<InstantDebitsResult> = runCatching {
        val paymentMethod = uri.getEncodedPaymentMethodOrThrow()
        InstantDebitsResult(
            encodedPaymentMethod = paymentMethod,
            last4 = uri.getQueryParameter(QUERY_PARAM_LAST4),
            bankName = uri.getQueryParameter(QUERY_BANK_NAME),
            eligibleForIncentive = uri.getQueryParameter(QUERY_INCENTIVE_ELIGIBLE).toBoolean(),
        )
    }

    private fun Uri.getEncodedPaymentMethodOrThrow(): String {
        val encodedPaymentMethod = requireNotNull(getQueryParameter(QUERY_PARAM_PAYMENT_METHOD))
        return String(Base64.decode(encodedPaymentMethod, 0), Charsets.UTF_8)
    }

    internal const val QUERY_PARAM_PAYMENT_METHOD = "payment_method"
    internal const val QUERY_PARAM_LAST4 = "last4"
    internal const val QUERY_BANK_NAME = "bank_name"
    internal const val QUERY_INCENTIVE_ELIGIBLE = "incentive_eligible"
}
