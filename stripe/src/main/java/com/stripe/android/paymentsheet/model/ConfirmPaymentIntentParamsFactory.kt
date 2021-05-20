package com.stripe.android.paymentsheet.model

import com.stripe.android.model.ConfirmPaymentIntentParams

internal class ConfirmPaymentIntentParamsFactory(
    private val clientSecret: ClientSecret
) : ConfirmStripeIntentParamsFactory<ConfirmPaymentIntentParams> {
    override fun create(paymentSelection: PaymentSelection.Saved) =
        ConfirmPaymentIntentParams.createWithPaymentMethodId(
            paymentMethodId = paymentSelection.paymentMethod.id.orEmpty(),
            clientSecret = clientSecret.value,
        )

    override fun create(paymentSelection: PaymentSelection.New) =
        ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
            clientSecret = clientSecret.value,
            setupFutureUsage = when (paymentSelection.shouldSavePaymentMethod) {
                true -> ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                false -> null
            }
        )
}
