package com.stripe.android

import androidx.annotation.RestrictTo
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntent

/**
 * Factory class for creating [ConfirmPaymentIntentParams] or [ConfirmSetupIntentParams].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class ConfirmStripeIntentParamsFactory<out T : ConfirmStripeIntentParams> {

    abstract fun create(
        paymentMethodId: String,
        paymentMethodType: PaymentMethod.Type?,
        optionsParams: PaymentMethodOptionsParams?,
    ): T

    abstract fun create(
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams? = null,
    ): T

    fun create(
        paymentMethod: PaymentMethod,
        optionsParams: PaymentMethodOptionsParams?,
    ): T {
        return create(
            paymentMethodId = paymentMethod.id.orEmpty(),
            paymentMethodType = paymentMethod.type,
            optionsParams = optionsParams,
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        fun createFactory(
            clientSecret: String,
            shipping: ConfirmPaymentIntentParams.Shipping?,
        ) = when {
            PaymentIntent.ClientSecret.isMatch(clientSecret) -> {
                ConfirmPaymentIntentParamsFactory(clientSecret, shipping)
            }
            SetupIntent.ClientSecret.isMatch(clientSecret) -> {
                ConfirmSetupIntentParamsFactory(clientSecret)
            }
            else -> {
                error("Encountered an invalid client secret \"$clientSecret\"")
            }
        }
    }
}

internal class ConfirmPaymentIntentParamsFactory(
    private val clientSecret: String,
    private val shipping: ConfirmPaymentIntentParams.Shipping?
) : ConfirmStripeIntentParamsFactory<ConfirmPaymentIntentParams>() {

    override fun create(
        paymentMethodId: String,
        paymentMethodType: PaymentMethod.Type?,
        optionsParams: PaymentMethodOptionsParams?,
    ): ConfirmPaymentIntentParams {
        return ConfirmPaymentIntentParams.createWithPaymentMethodId(
            paymentMethodId = paymentMethodId,
            clientSecret = clientSecret,
            paymentMethodOptions = optionsParams,
            mandateData = MandateDataParams(MandateDataParams.Type.Online.DEFAULT)
                .takeIf { paymentMethodType?.requiresMandate == true },
            shipping = shipping
        )
    }

    override fun create(
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams?,
    ): ConfirmPaymentIntentParams {
        return ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = createParams,
            clientSecret = clientSecret,
            paymentMethodOptions = optionsParams,
            shipping = shipping,
        )
    }
}

internal class ConfirmSetupIntentParamsFactory(
    private val clientSecret: String,
) : ConfirmStripeIntentParamsFactory<ConfirmSetupIntentParams>() {

    override fun create(
        paymentMethodId: String,
        paymentMethodType: PaymentMethod.Type?,
        optionsParams: PaymentMethodOptionsParams?,
    ): ConfirmSetupIntentParams {
        return ConfirmSetupIntentParams.create(
            paymentMethodId = paymentMethodId,
            clientSecret = clientSecret,
            mandateData = paymentMethodType?.requiresMandate?.let {
                MandateDataParams(MandateDataParams.Type.Online.DEFAULT)
            }
        )
    }

    override fun create(
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams?,
    ): ConfirmSetupIntentParams {
        return ConfirmSetupIntentParams.create(
            paymentMethodCreateParams = createParams,
            clientSecret = clientSecret,
        )
    }
}
