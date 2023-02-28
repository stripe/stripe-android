package com.stripe.android.paymentsheet.model

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.ConfirmStripeIntentParamsFactory

internal fun <T : ConfirmStripeIntentParams> ConfirmStripeIntentParamsFactory<T>.create(
    paymentSelection: PaymentSelection.Saved,
): T {
    return create(paymentSelection.paymentMethod)
}

internal fun <T : ConfirmStripeIntentParams> ConfirmStripeIntentParamsFactory<T>.create(
    paymentSelection: PaymentSelection.New,
): T {
    val setupFutureUsage = when (paymentSelection.customerRequestedSave) {
        PaymentSelection.CustomerRequestedSave.RequestReuse -> {
            ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
        }
        PaymentSelection.CustomerRequestedSave.RequestNoReuse -> {
            ConfirmPaymentIntentParams.SetupFutureUsage.Blank
        }
        PaymentSelection.CustomerRequestedSave.NoRequest -> {
            null
        }
    }

    return create(
        createParams = paymentSelection.paymentMethodCreateParams,
        setupFutureUsage = setupFutureUsage,
    )
}
