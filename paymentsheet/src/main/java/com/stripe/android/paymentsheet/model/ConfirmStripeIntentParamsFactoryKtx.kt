package com.stripe.android.paymentsheet.model

import com.stripe.android.ConfirmStripeIntentParamsFactory
import com.stripe.android.model.ConfirmStripeIntentParams

internal fun <T : ConfirmStripeIntentParams> ConfirmStripeIntentParamsFactory<T>.create(
    paymentSelection: PaymentSelection.Saved,
): T {
    return create(paymentSelection.paymentMethod)
}

internal fun <T : ConfirmStripeIntentParams> ConfirmStripeIntentParamsFactory<T>.create(
    paymentSelection: PaymentSelection.New,
): T {
    return create(
        createParams = paymentSelection.paymentMethodCreateParams,
        optionsParams = paymentSelection.paymentMethodOptionsParams,
    )
}
