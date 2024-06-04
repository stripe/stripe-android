package com.stripe.android.paymentsheet.model

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent

internal val StripeIntent.currency: String?
    get() = when (this) {
        is PaymentIntent -> currency
        else -> null
    }
