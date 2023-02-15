package com.stripe.android.link.confirmation

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.DeferredIntent
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import kotlinx.parcelize.RawValue

/**
 * Factory class for creating [PaymentMethodCreateParams] and [ConfirmPaymentIntentParams] or
 * [ConfirmSetupIntentParams] from a [ConsumerPaymentDetails.PaymentDetails].
 */
internal sealed class ConfirmStripeIntentParamsFactory<out T : ConfirmStripeIntentParams> {

    abstract fun createConfirmStripeIntentParams(
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): T

    abstract fun createPaymentMethodCreateParams(
        consumerSessionClientSecret: String,
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
        extraParams: Map<String, @RawValue Any>? = null
    ): PaymentMethodCreateParams

    companion object {
        fun createFactory(
            stripeIntent: StripeIntent,
            shipping: ConfirmPaymentIntentParams.Shipping? = null
        ) =
            when (stripeIntent) {
                is PaymentIntent -> ConfirmPaymentIntentParamsFactory(stripeIntent, shipping)
                is SetupIntent -> ConfirmSetupIntentParamsFactory(stripeIntent)
                is DeferredIntent -> throw IllegalStateException("DeferredIntent cannot be confirmed")
            }
    }
}

internal class ConfirmPaymentIntentParamsFactory(
    private val paymentIntent: PaymentIntent,
    private val shipping: ConfirmPaymentIntentParams.Shipping?
) : ConfirmStripeIntentParamsFactory<ConfirmPaymentIntentParams>() {
    override fun createConfirmStripeIntentParams(
        paymentMethodCreateParams: PaymentMethodCreateParams
    ) = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
        paymentMethodCreateParams,
        paymentIntent.clientSecret!!,
        shipping = shipping
    )

    override fun createPaymentMethodCreateParams(
        consumerSessionClientSecret: String,
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
        extraParams: Map<String, Any>?
    ) = PaymentMethodCreateParams.createLink(
        selectedPaymentDetails.id,
        consumerSessionClientSecret,
        extraParams
    )
}

internal class ConfirmSetupIntentParamsFactory(
    private val setupIntent: SetupIntent
) : ConfirmStripeIntentParamsFactory<ConfirmSetupIntentParams>() {
    override fun createConfirmStripeIntentParams(
        paymentMethodCreateParams: PaymentMethodCreateParams
    ) = ConfirmSetupIntentParams.create(
        paymentMethodCreateParams,
        setupIntent.clientSecret!!
    )

    override fun createPaymentMethodCreateParams(
        consumerSessionClientSecret: String,
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
        extraParams: Map<String, Any>?
    ) = PaymentMethodCreateParams.createLink(
        selectedPaymentDetails.id,
        consumerSessionClientSecret,
        extraParams
    )
}
