package com.stripe.android

import androidx.annotation.RestrictTo
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent

/**
 * Factory class for creating [ConfirmPaymentIntentParams] or [ConfirmSetupIntentParams].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class ConfirmStripeIntentParamsFactory<out T : ConfirmStripeIntentParams> {

    abstract fun create(
        paymentMethodId: String,
        paymentMethodType: PaymentMethod.Type,
        optionsParams: PaymentMethodOptionsParams?,
        extraParams: PaymentMethodExtraParams?,
    ): T

    abstract fun create(
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams? = null,
        extraParams: PaymentMethodExtraParams? = null,
    ): T

    fun create(
        paymentMethod: PaymentMethod,
        optionsParams: PaymentMethodOptionsParams?,
        extraParams: PaymentMethodExtraParams?,
    ): T {
        return create(
            paymentMethodId = paymentMethod.id.orEmpty(),
            paymentMethodType = requireNotNull(paymentMethod.type),
            optionsParams = optionsParams,
            extraParams = extraParams,
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        fun createFactory(
            clientSecret: String,
            intent: StripeIntent,
            shipping: ConfirmPaymentIntentParams.Shipping?,
        ) = when {
            intent is PaymentIntent && PaymentIntent.ClientSecret.isMatch(clientSecret) -> {
                ConfirmPaymentIntentParamsFactory(clientSecret, intent, shipping)
            }
            intent is SetupIntent && SetupIntent.ClientSecret.isMatch(clientSecret) -> {
                ConfirmSetupIntentParamsFactory(clientSecret, intent)
            }
            else -> null
        }
    }
}

internal class ConfirmPaymentIntentParamsFactory(
    private val clientSecret: String,
    private val intent: PaymentIntent,
    private val shipping: ConfirmPaymentIntentParams.Shipping?
) : ConfirmStripeIntentParamsFactory<ConfirmPaymentIntentParams>() {

    override fun create(
        paymentMethodId: String,
        paymentMethodType: PaymentMethod.Type,
        optionsParams: PaymentMethodOptionsParams?,
        extraParams: PaymentMethodExtraParams?
    ): ConfirmPaymentIntentParams {
        return ConfirmPaymentIntentParams.createWithSetAsDefaultPaymentMethod(
            paymentMethodId = paymentMethodId,
            clientSecret = clientSecret,
            paymentMethodOptions = optionsParams,
            mandateData = mandateData(intent, paymentMethodType),
            shipping = shipping,
            setAsDefaultPaymentMethod = extraParams?.extractSetAsDefaultPaymentMethodFromExtraParams(),
            paymentMethodCode = paymentMethodType.code,
        )
    }

    override fun create(
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams?,
        extraParams: PaymentMethodExtraParams?,
    ): ConfirmPaymentIntentParams {
        return ConfirmPaymentIntentParams.createWithSetAsDefaultPaymentMethod(
            paymentMethodCreateParams = createParams,
            clientSecret = clientSecret,
            paymentMethodOptions = optionsParams,
            shipping = shipping,
            setAsDefaultPaymentMethod = extraParams?.extractSetAsDefaultPaymentMethodFromExtraParams()
        )
    }
}

internal class ConfirmSetupIntentParamsFactory(
    private val clientSecret: String,
    private val intent: SetupIntent,
) : ConfirmStripeIntentParamsFactory<ConfirmSetupIntentParams>() {

    override fun create(
        paymentMethodId: String,
        paymentMethodType: PaymentMethod.Type,
        optionsParams: PaymentMethodOptionsParams?,
        extraParams: PaymentMethodExtraParams?,
    ): ConfirmSetupIntentParams {
        return ConfirmSetupIntentParams.createWithSetAsDefaultPaymentMethod(
            paymentMethodId = paymentMethodId,
            clientSecret = clientSecret,
            mandateData = mandateData(intent, paymentMethodType),
            setAsDefaultPaymentMethod = extraParams?.extractSetAsDefaultPaymentMethodFromExtraParams(),
            paymentMethodCode = paymentMethodType.code,
        )
    }

    override fun create(
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams?,
        extraParams: PaymentMethodExtraParams?,
    ): ConfirmSetupIntentParams {
        return ConfirmSetupIntentParams.createWithSetAsDefaultPaymentMethod(
            paymentMethodCreateParams = createParams,
            clientSecret = clientSecret,
            setAsDefaultPaymentMethod = extraParams?.extractSetAsDefaultPaymentMethodFromExtraParams()
        )
    }
}

private fun mandateData(intent: StripeIntent, paymentMethodType: PaymentMethod.Type?): MandateDataParams? {
    return paymentMethodType?.let { type ->
        val supportsAddingMandateData = when (intent) {
            is PaymentIntent ->
                intent.canSetupFutureUsage(paymentMethodType.code) || type.requiresMandateForPaymentIntent
            is SetupIntent -> true
        }

        return MandateDataParams(MandateDataParams.Type.Online.DEFAULT).takeIf {
            supportsAddingMandateData && type.requiresMandate
        }
    }
}

private fun PaymentIntent.canSetupFutureUsage(paymentMethodCode: PaymentMethodCode): Boolean {
    return isSetupFutureUsageSet(paymentMethodCode)
}

private fun PaymentMethodExtraParams.extractSetAsDefaultPaymentMethodFromExtraParams(): Boolean? {
    return when (this) {
        is PaymentMethodExtraParams.Card -> this.setAsDefault
        is PaymentMethodExtraParams.USBankAccount -> this.setAsDefault
        is PaymentMethodExtraParams.SepaDebit -> this.setAsDefault
        else -> null
    }
}
