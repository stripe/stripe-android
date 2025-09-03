package com.stripe.android.link.utils

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.R as StripeR

internal val Throwable.errorMessage: ResolvableString
    get() = when (this) {
        is APIConnectionException -> resolvableString(StripeR.string.stripe_failure_connection_error)
        else -> localizedMessage?.let {
            resolvableString(it)
        } ?: resolvableString(StripeR.string.stripe_internal_error)
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Throwable.isLinkAuthorizationError(): Boolean {
    return when (this) {
        is APIException -> {
            val code = this.stripeError?.code
            code == "consumer_session_credentials_invalid"
        }
        else -> false
    }
}
