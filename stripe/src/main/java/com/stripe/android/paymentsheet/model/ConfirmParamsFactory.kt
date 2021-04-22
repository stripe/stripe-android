package com.stripe.android.paymentsheet.model

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams

internal class ConfirmParamsFactory(
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
                    returnUrl = RETURN_URL
                )
            }
            is SetupIntentClientSecret -> {
                ConfirmSetupIntentParams.create(
                    paymentMethodId = paymentSelection.paymentMethod.id.orEmpty(),
                    clientSecret = clientSecret.value,
                    returnUrl = RETURN_URL
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
                    returnUrl = RETURN_URL,
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
                    returnUrl = RETURN_URL
                )
            }
        }
    }

    private companion object {
        // the value of the return URL isn't significant, but a return URL must be specified
        // to properly handle authentication
        private const val RETURN_URL = "stripe://return_url"
    }
}
