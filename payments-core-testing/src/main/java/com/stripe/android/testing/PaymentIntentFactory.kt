package com.stripe.android.testing

import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent

object PaymentIntentFactory {

    fun create(
        paymentMethod: PaymentMethod? = createCardPaymentMethod(),
        paymentMethodTypes: List<String> = listOf("card"),
        setupFutureUsage: StripeIntent.Usage? = null,
        confirmationMethod: PaymentIntent.ConfirmationMethod = PaymentIntent.ConfirmationMethod.Automatic,
        status: StripeIntent.Status = StripeIntent.Status.RequiresConfirmation,
    ): PaymentIntent = PaymentIntent(
        created = 500L,
        amount = 1000L,
        clientSecret = "secret",
        paymentMethod = paymentMethod,
        isLiveMode = false,
        id = "pi_12345",
        currency = "usd",
        countryCode = null,
        paymentMethodTypes = paymentMethodTypes,
        status = status,
        unactivatedPaymentMethods = emptyList(),
        setupFutureUsage = setupFutureUsage,
        confirmationMethod = confirmationMethod,
    )

    private fun createCardPaymentMethod(): PaymentMethod = PaymentMethod(
        id = "12",
        created = 123456789L,
        liveMode = false,
        type = PaymentMethod.Type.Card,
        card = PaymentMethod.Card(
            brand = CardBrand.Visa,
            last4 = "4242"
        ),
        code = "card"
    )
}
