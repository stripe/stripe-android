package com.stripe.android.paymentsheet.model

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.payments.DefaultReturnUrl

internal class ConfirmParamsFactory(
    private val defaultReturnUrl: DefaultReturnUrl,
    private val clientSecret: ClientSecret
) {
    internal fun create(
        paymentSelection: PaymentSelection.Saved
    ): ConfirmPaymentIntentParams {
        return ConfirmPaymentIntentParams.createWithPaymentMethodId(
            paymentMethodId = paymentSelection.paymentMethod.id.orEmpty(),
            clientSecret = clientSecret.value,
            returnUrl = defaultReturnUrl.value
        )
    }

    internal fun create(
        paymentSelection: PaymentSelection.New
    ): ConfirmPaymentIntentParams {
        return ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
            clientSecret = clientSecret.value,
            returnUrl = defaultReturnUrl.value,
            setupFutureUsage = when (paymentSelection.shouldSavePaymentMethod) {
                true -> ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                false -> null
            }
        )
    }
}
