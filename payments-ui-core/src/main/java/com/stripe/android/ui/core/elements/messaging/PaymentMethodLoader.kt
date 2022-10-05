package com.stripe.android.ui.core.elements.messaging

import android.content.Context
import com.stripe.android.Stripe
import com.stripe.android.getPaymentMethodMessaging
import com.stripe.android.model.PaymentMethodMessage

internal class PaymentMethodLoader(
    context: Context,
    private val configuration: PaymentMethodMessagingView.Configuration
) {
    private val stripe = Stripe(
        context = context,
        publishableKey = configuration.publishableKey,
        stripeAccountId = configuration.stripeAccountId
    )

    suspend fun loadMessage(): Result<PaymentMethodMessage> {
        val message = stripe.getPaymentMethodMessaging(
            paymentMethods = configuration.paymentMethods.map { it.value },
            amount = configuration.amount,
            currency = configuration.currency,
            country = configuration.countryCode,
            locale = configuration.locale.toLanguageTag(),
            logoColor = configuration.imageColor.value
        )
        message?.let {
            return Result.success(
                message
            )
        }
        return Result.failure(Exception("Could not retrieve message"))
    }
}
