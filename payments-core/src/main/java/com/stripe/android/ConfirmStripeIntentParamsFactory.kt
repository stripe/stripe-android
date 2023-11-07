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
        setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage? = null,
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
        setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
    ): ConfirmPaymentIntentParams {
        return ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = createParams,
            clientSecret = clientSecret,

            /**
             Sets `payment_method_options[card][setup_future_usage]`
             - Note: PaymentSheet uses this `setup_future_usage` (SFU) value very differently from the top-level one:
             We read the top-level SFU to know the merchant’s desired save behavior
             We write payment method options SFU to set the customer’s desired save behavior
             */
            // At this time, paymentMethodOptions card and us_bank_account is the only PM that
            // supports setup future usage
            paymentMethodOptions = when (createParams.typeCode) {
                PaymentMethod.Type.Card.code -> {
                    PaymentMethodOptionsParams.Card(setupFutureUsage = setupFutureUsage)
                }
                PaymentMethod.Type.USBankAccount.code -> {
                    PaymentMethodOptionsParams.USBankAccount(setupFutureUsage = setupFutureUsage)
                }
                PaymentMethod.Type.Blik.code -> {
                    optionsParams
                }
                PaymentMethod.Type.Konbini.code -> {
                    optionsParams
                }
                PaymentMethod.Type.Link.code -> {
                    null
                }
                else -> {
                    PaymentMethodOptionsParams.Card(setupFutureUsage = null)
                }
            },
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
        setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
    ): ConfirmSetupIntentParams {
        return ConfirmSetupIntentParams.create(
            paymentMethodCreateParams = createParams,
            clientSecret = clientSecret,
        )
    }
}
