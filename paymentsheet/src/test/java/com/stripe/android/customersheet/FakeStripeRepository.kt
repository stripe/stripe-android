package com.stripe.android.customersheet

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.testing.AbsFakeStripeRepository

class FakeStripeRepository(
    private val onCreatePaymentMethod:
        ((paymentMethodCreateParams: PaymentMethodCreateParams) -> PaymentMethod)? = null,
    private val onConfirmSetupIntent:
        (() -> SetupIntent?)? = null
) : AbsFakeStripeRepository() {
    override suspend fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: ApiRequest.Options
    ): PaymentMethod? {
        return onCreatePaymentMethod?.invoke(paymentMethodCreateParams)
    }

    override suspend fun confirmSetupIntent(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): SetupIntent? {
        return onConfirmSetupIntent?.invoke()
    }
}
