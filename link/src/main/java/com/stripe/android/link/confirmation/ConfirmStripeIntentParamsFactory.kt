package com.stripe.android.link.model

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent

/**
 * Factory class for creating [ConfirmPaymentIntentParams] or [ConfirmSetupIntentParams] from a
 * [ConsumerPaymentDetails.PaymentDetails].
 */
internal sealed class ConfirmStripeIntentParamsFactory<out T : ConfirmStripeIntentParams> {

    abstract fun create(
        consumerSessionClientSecret: String,
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails
    ): T

    companion object {
        fun createFactory(stripeIntent: StripeIntent) =
            when (stripeIntent) {
                is PaymentIntent -> ConfirmPaymentIntentParamsFactory(stripeIntent)
                is SetupIntent -> ConfirmSetupIntentParamsFactory(stripeIntent)
            }
    }
}

internal class ConfirmPaymentIntentParamsFactory(
    private val paymentIntent: PaymentIntent
) : ConfirmStripeIntentParamsFactory<ConfirmPaymentIntentParams>() {
    override fun create(
        consumerSessionClientSecret: String,
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails
    ) = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
        PaymentMethodCreateParams.createLink(
            selectedPaymentDetails.id,
            consumerSessionClientSecret
        ),
        paymentIntent.clientSecret!!
    )
}

internal class ConfirmSetupIntentParamsFactory(
    private val setupIntent: SetupIntent
) : ConfirmStripeIntentParamsFactory<ConfirmSetupIntentParams>() {
    override fun create(
        consumerSessionClientSecret: String,
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails
    ) = ConfirmSetupIntentParams.Companion.create(
        PaymentMethodCreateParams.createLink(
            selectedPaymentDetails.id,
            consumerSessionClientSecret
        ),
        setupIntent.clientSecret!!
    )
}
