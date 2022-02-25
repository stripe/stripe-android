package com.stripe.android.paymentsheet.model

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams

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
            paymentMethodOptions = PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
            ).takeIf { paymentSelection.paymentMethod.type == PaymentMethod.Type.Card }
        )

    override fun create(paymentSelection: PaymentSelection.New) =
        ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
            clientSecret = clientSecret.value,

            /**
             Sets `payment_method_options[card][setup_future_usage]`
             - Note: PaymentSheet uses this `setup_future_usage` (SFU) value very differently from the top-level one:
             We read the top-level SFU to know the merchant’s desired save behavior
             We write payment method options SFU to set the customer’s desired save behavior
             */
            // At this time, paymentMethodOptions card is the only PM that supports setup future usage
            paymentMethodOptions = PaymentMethodOptionsParams.Card(
                setupFutureUsage = when (paymentSelection.customerRequestedSave) {
                    PaymentSelection.CustomerRequestedSave.RequestReuse ->
                        ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                    PaymentSelection.CustomerRequestedSave.RequestNoReuse ->
                        ConfirmPaymentIntentParams.SetupFutureUsage.Blank
                    PaymentSelection.CustomerRequestedSave.NoRequest ->
                        null
                }.takeIf {
                    paymentSelection.paymentMethodCreateParams.typeCode ==
                        PaymentMethod.Type.Card.code
                }
            )
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
