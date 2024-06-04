package com.stripe.android.testing

import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent

object SetupIntentFactory {
    fun create(
        paymentMethod: PaymentMethod? = createCardPaymentMethod(),
        paymentMethodTypes: List<String> = listOf("card"),
        status: StripeIntent.Status = StripeIntent.Status.RequiresConfirmation,
        paymentMethodOptionsJsonString: String? = null,
        linkFundingSources: List<String> = emptyList(),
        countryCode: String? = null,
        cancellationReason: SetupIntent.CancellationReason? = null,
        usage: StripeIntent.Usage? = null,
    ): SetupIntent = SetupIntent(
        created = 500L,
        clientSecret = "secret",
        paymentMethod = paymentMethod,
        isLiveMode = false,
        id = "pi_12345",
        countryCode = countryCode,
        paymentMethodTypes = paymentMethodTypes,
        status = status,
        unactivatedPaymentMethods = emptyList(),
        paymentMethodOptionsJsonString = paymentMethodOptionsJsonString,
        linkFundingSources = linkFundingSources,
        cancellationReason = cancellationReason,
        description = null,
        nextActionData = null,
        paymentMethodId = paymentMethod?.id,
        usage = usage,
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
