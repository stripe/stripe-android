package com.stripe.android.common.exception

import android.content.Context
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R

@Suppress("ReturnCount")
internal fun Throwable?.stripeErrorMessage(context: Context): String {
    (this as? APIConnectionException)?.let {
        return context.getString(R.string.stripe_network_error_message)
    }
    (this as? LocalStripeException)?.displayMessage?.let {
        return it
    }
    (this as? StripeException)?.stripeError?.message?.let {
        return it
    }
    return context.getString(R.string.stripe_something_went_wrong)
}

@Suppress("ReturnCount")
internal fun Throwable.stripeErrorMessage(): ResolvableString {
    (this as? APIConnectionException)?.let {
        return resolvableString(R.string.stripe_network_error_message)
    }
    (this as? LocalStripeException)?.displayMessage?.let {
        return resolvableString(it)
    }
    (this as? StripeException)?.stripeError?.message?.let {
        return resolvableString(it)
    }
    return resolvableString(R.string.stripe_something_went_wrong)
}
