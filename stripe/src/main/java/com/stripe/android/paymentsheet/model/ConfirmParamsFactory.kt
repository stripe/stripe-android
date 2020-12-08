package com.stripe.android.paymentsheet.model

import com.stripe.android.model.ConfirmPaymentIntentParams

internal class ConfirmParamsFactory(
    private val clientSecret: String
) {
    internal fun create(
        paymentSelection: PaymentSelection.Saved
    ): ConfirmPaymentIntentParams {
        return ConfirmPaymentIntentParams.createWithPaymentMethodId(
            paymentSelection.paymentMethod.id.orEmpty(),
            clientSecret
        )
    }

    internal fun create(
        paymentSelection: PaymentSelection.New
    ): ConfirmPaymentIntentParams {
        return ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            paymentSelection.paymentMethodCreateParams,
            clientSecret,
            setupFutureUsage = when (paymentSelection.shouldSavePaymentMethod) {
                true -> ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                false -> null
            }
        )
    }
}
