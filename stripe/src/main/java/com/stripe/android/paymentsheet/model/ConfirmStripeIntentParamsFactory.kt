package com.stripe.android.paymentsheet.model

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams

/**
 * Factory class for creating [ConfirmPaymentIntentParams] or [ConfirmSetupIntentParams].
 */
internal sealed class ConfirmStripeIntentParamsFactory<T : ConfirmStripeIntentParams> {

    abstract fun create(paymentSelection: PaymentSelection.Saved): T

    abstract fun create(paymentSelection: PaymentSelection.New): T
}

internal class ConfirmPaymentIntentParamsFactory(
    private val clientSecret: ClientSecret
) : ConfirmStripeIntentParamsFactory<ConfirmPaymentIntentParams>() {
    override fun create(paymentSelection: PaymentSelection.Saved) =
        ConfirmPaymentIntentParams.createWithPaymentMethodId(
            paymentMethodId = paymentSelection.paymentMethod.id.orEmpty(),
            clientSecret = clientSecret.value,
        )

    override fun create(paymentSelection: PaymentSelection.New) =
        ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
            clientSecret = clientSecret.value,
            setupFutureUsage = when (paymentSelection.shouldSavePaymentMethod) {
                true -> ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                false -> null
            }
        )
}

internal class ConfirmSetupIntentParamsFactory(
    private val clientSecret: ClientSecret
) : ConfirmStripeIntentParamsFactory<ConfirmSetupIntentParams>() {
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
