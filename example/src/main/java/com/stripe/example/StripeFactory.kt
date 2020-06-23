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
        val paymentConfiguration = PaymentConfiguration.getInstance(context)
        return Stripe(
            context,
            paymentConfiguration.publishableKey,
            stripeAccountId,
            enableLogging,
            betas = paymentConfiguration.betas
        )
    }
}
