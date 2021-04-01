package com.stripe.android.paymentsheet.model

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams

internal class ConfirmParamsFactory(
    private val clientSecret: String
) {
    internal fun create(
        paymentSelection: PaymentSelection.Saved
    ): ConfirmPaymentIntentParams {
        return ConfirmPaymentIntentParams.createWithPaymentMethodId(
            paymentMethodId = paymentSelection.paymentMethod.id.orEmpty(),
            clientSecret = clientSecret,
            returnUrl = ConfirmStripeIntentParams.DEFAULT_RETURN_URL
        )
    }

    internal fun create(
        paymentSelection: PaymentSelection.New
    ): ConfirmPaymentIntentParams {
        return ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
            clientSecret = clientSecret,
            returnUrl = ConfirmStripeIntentParams.DEFAULT_RETURN_URL,
            setupFutureUsage = when (paymentSelection.shouldSavePaymentMethod) {
                true -> ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                false -> null
            }
        )
    }
}
