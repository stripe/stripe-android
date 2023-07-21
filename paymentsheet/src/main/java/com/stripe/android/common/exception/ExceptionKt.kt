package com.stripe.android.common.exception

import android.content.Context
import com.stripe.android.core.exception.StripeException
import com.stripe.android.paymentsheet.R

internal fun Throwable?.stripeErrorMessage(context: Context): String {
    return (this as? StripeException)?.stripeError?.message
        ?: context.resources.getString(R.string.stripe_something_went_wrong)
}
