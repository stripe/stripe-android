package com.stripe.android.paymentsheet.model

import com.stripe.android.model.ConfirmSetupIntentParams

internal class ConfirmSetupIntentParamsFactory(
    private val clientSecret: ClientSecret
) : ConfirmStripeIntentParamsFactory<ConfirmSetupIntentParams> {
    override fun create(paymentSelection: PaymentSelection.Saved) =
        ConfirmSetupIntentParams.create(
            paymentMethodId = paymentSelection.paymentMethod.id.orEmpty(),
            clientSecret = clientSecret.value
        )

    override fun create(paymentSelection: PaymentSelection.New) =
        ConfirmSetupIntentParams.create(
            paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
            clientSecret = clientSecret.value
        )
}
