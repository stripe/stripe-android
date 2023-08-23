package com.stripe.android.common.exception

import android.content.Context
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.paymentsheet.R

@Suppress("ReturnCount")
internal fun Throwable?.stripeErrorMessage(context: Context): String {
    (this as? StripeException)?.stripeError?.message?.let {
        return it
    }
    (this as? LocalStripeException)?.message?.let {
        return it
    }
    return context.resources.getString(R.string.stripe_something_went_wrong)
}
