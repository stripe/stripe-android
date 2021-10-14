package com.stripe.android.paymentsheet.model

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.MandateDataParams

/**
 * Factory class for creating [ConfirmPaymentIntentParams] or [ConfirmSetupIntentParams].
 */
internal sealed class ConfirmStripeIntentParamsFactory<out T : ConfirmStripeIntentParams> {

    abstract fun create(paymentSelection: PaymentSelection.Saved): T

    abstract fun create(paymentSelection: PaymentSelection.New): T

    companion object {
        fun createFactory(clientSecret: ClientSecret) =
            when (clientSecret) {
                is PaymentIntentClientSecret -> ConfirmPaymentIntentParamsFactory(clientSecret)
                is SetupIntentClientSecret -> ConfirmSetupIntentParamsFactory(clientSecret)
            }
    }
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
            setupFutureUsage = when (paymentSelection.userReuseRequest) {
                PaymentSelection.UserReuseRequest.RequestReuse -> ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                PaymentSelection.UserReuseRequest.RequestNoReuse -> ConfirmPaymentIntentParams.SetupFutureUsage.Blank
                PaymentSelection.UserReuseRequest.NoRequest -> null
            }
        )
}

internal class ConfirmSetupIntentParamsFactory(
    private val clientSecret: ClientSecret
) : ConfirmStripeIntentParamsFactory<ConfirmSetupIntentParams>() {
    override fun create(paymentSelection: PaymentSelection.Saved) =
        ConfirmSetupIntentParams.create(
            paymentMethodId = paymentSelection.paymentMethod.id.orEmpty(),
            clientSecret = clientSecret.value,
            mandateData = paymentSelection.paymentMethod.type?.requiresMandate?.let {
                MandateDataParams(MandateDataParams.Type.Online.DEFAULT)
            }
        )

    override fun create(paymentSelection: PaymentSelection.New) =
        ConfirmSetupIntentParams.create(
            paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
            clientSecret = clientSecret.value
        )
}
