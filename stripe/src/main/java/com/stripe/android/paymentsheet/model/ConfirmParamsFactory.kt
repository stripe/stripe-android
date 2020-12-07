package com.stripe.android.paymentsheet.model

import com.stripe.android.model.ConfirmPaymentIntentParams

internal class ConfirmParamsFactory {

    internal fun create(
        clientSecret: String,
        paymentSelection: PaymentSelection
    ): ConfirmPaymentIntentParams {
        return when (paymentSelection) {
            PaymentSelection.GooglePay -> TODO("smaskell: handle Google Pay confirmation")

            is PaymentSelection.Saved -> {
                // TODO(smaskell): Properly set savePaymentMethod/setupFutureUsage
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    paymentSelection.paymentMethod.id.orEmpty(),
                    clientSecret
                )
            }

            is PaymentSelection.New -> {
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    paymentSelection.paymentMethodCreateParams,
                    clientSecret,
                    setupFutureUsage = when (paymentSelection.shouldSavePaymentMethod) {
                        true -> ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                        false -> null
                    }
                )
            }
        }
    }
}
