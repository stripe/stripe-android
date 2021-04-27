package com.stripe.android.paymentsheet.model

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.payments.DefaultReturnUrl

internal class ConfirmParamsFactory(
    private val defaultReturnUrl: DefaultReturnUrl,
    private val clientSecret: ClientSecret
) {
    internal fun create(
        paymentSelection: PaymentSelection.Saved
    ): ConfirmStripeIntentParams {
        return when (clientSecret) {
            is PaymentIntentClientSecret -> {
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    paymentMethodId = paymentSelection.paymentMethod.id.orEmpty(),
                    clientSecret = clientSecret.value,
                    returnUrl = defaultReturnUrl.value
                )
            }
            is SetupIntentClientSecret -> {
                ConfirmSetupIntentParams.create(
                    paymentMethodId = paymentSelection.paymentMethod.id.orEmpty(),
                    clientSecret = clientSecret.value,
                    returnUrl = defaultReturnUrl.value
                )
            }
        }
    }

    internal fun create(
        paymentSelection: PaymentSelection.New
    ): ConfirmStripeIntentParams {
        return when (clientSecret) {
            is PaymentIntentClientSecret -> {
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
                    clientSecret = clientSecret.value,
                    returnUrl = defaultReturnUrl.value,
                    setupFutureUsage = when (paymentSelection.shouldSavePaymentMethod) {
                        true -> ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                        false -> null
                    }
                )
            }
            is SetupIntentClientSecret -> {
                ConfirmSetupIntentParams.create(
                    paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
                    clientSecret = clientSecret.value,
                    returnUrl = defaultReturnUrl.value
                )
            }
        }
    }
}
