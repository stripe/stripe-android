package com.stripe.android.link.confirmation

import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.ConsumerPaymentDetails
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
        consumerSessionClientSecret: String,
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
        extraParams: Map<String, @RawValue Any>? = null
    ): T

    abstract fun createPaymentMethodCreateParams(
        consumerSessionClientSecret: String,
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
        extraParams: Map<String, @RawValue Any>? = null
    ): PaymentMethodCreateParams

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
    override fun createConfirmStripeIntentParams(
        consumerSessionClientSecret: String,
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
        extraParams: Map<String, @RawValue Any>?
    ) = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
        createPaymentMethodCreateParams(
            consumerSessionClientSecret,
            selectedPaymentDetails,
            extraParams
        ),
        paymentIntent.clientSecret!!
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
        consumerSessionClientSecret: String,
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
        extraParams: Map<String, @RawValue Any>?
    ) = ConfirmSetupIntentParams.Companion.create(
        createPaymentMethodCreateParams(
            consumerSessionClientSecret,
            selectedPaymentDetails,
            extraParams
        ),
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
