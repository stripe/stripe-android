package com.stripe.android.link.utils

import com.stripe.android.core.exception.APIConnectionException
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
