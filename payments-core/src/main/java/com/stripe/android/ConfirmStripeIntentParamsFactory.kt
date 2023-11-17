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

    abstract fun create(paymentMethod: PaymentMethod): T

    abstract fun create(
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams? = null,
    ): T

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        fun createFactory(
            clientSecret: String,
            shipping: ConfirmPaymentIntentParams.Shipping?
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

    override fun create(paymentMethod: PaymentMethod): ConfirmPaymentIntentParams {
        return ConfirmPaymentIntentParams.createWithPaymentMethodId(
            paymentMethodId = paymentMethod.id.orEmpty(),
            clientSecret = clientSecret,
            paymentMethodOptions = when (paymentMethod.type) {
                PaymentMethod.Type.Card -> {
                    PaymentMethodOptionsParams.Card(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
                    )
                }
                PaymentMethod.Type.USBankAccount -> {
                    PaymentMethodOptionsParams.USBankAccount(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                    )
                }
                else -> {
                    null
                }
            },
            mandateData = MandateDataParams(MandateDataParams.Type.Online.DEFAULT)
                .takeIf { paymentMethod.type?.requiresMandate == true },
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
    override fun create(paymentMethod: PaymentMethod): ConfirmSetupIntentParams {
        return ConfirmSetupIntentParams.create(
            paymentMethodId = paymentMethod.id.orEmpty(),
            clientSecret = clientSecret,
            mandateData = paymentMethod.type?.requiresMandate?.let {
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
