package com.stripe.example

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe

internal class StripeFactory(
    private val context: Context,
    private val stripeAccountId: String? = null,
    private val enableLogging: Boolean = true
) {
    fun create(): Stripe {
        return Stripe(
            context,
            PaymentConfiguration.getInstance(context).publishableKey,
            stripeAccountId,
            enableLogging
        )
    }
}
