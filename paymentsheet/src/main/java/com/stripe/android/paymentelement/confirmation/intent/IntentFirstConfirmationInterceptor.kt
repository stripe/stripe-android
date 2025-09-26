package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.ConfirmStripeIntentParamsFactory
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.RadarOptions
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import javax.inject.Named
import com.stripe.android.R as PaymentsCoreR

internal class IntentFirstConfirmationInterceptor(
    val clientSecret: String,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
) : IntentConfirmationInterceptor {

    private val requestOptions: ApiRequest.Options
        get() = ApiRequest.Options(
            apiKey = publishableKeyProvider(),
            stripeAccount = stripeAccountIdProvider(),
        )

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): ConfirmationDefinition.Action<Args> {
        return createConfirmStep(
            clientSecret = clientSecret,
            intent = intent,
            shippingValues = shippingValues,
            paymentMethodCreateParams = confirmationOption.createParams,
            paymentMethodOptionsParams = confirmationOption.optionsParams,
            paymentMethodExtraParams = confirmationOption.extraParams,
        )
    }

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        shippingValues: ConfirmPaymentIntentParams.Shipping?
    ): ConfirmationDefinition.Action<Args> {
        return createConfirmStep(
            clientSecret = clientSecret,
            intent = intent,
            shippingValues = shippingValues,
            paymentMethod = confirmationOption.paymentMethod,
            paymentMethodOptionsParams = confirmationOption.optionsParams,
            paymentMethodExtraParams = null,
            isDeferred = false,
            intentConfigSetupFutureUsage = null,
            hCaptchaToken = confirmationOption.hCaptchaToken,
        )
    }
    private fun createConfirmStep(
        clientSecret: String,
        intent: StripeIntent,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        isDeferred: Boolean,
        intentConfigSetupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
        hCaptchaToken: String?
    ): ConfirmationDefinition.Action<Args> {
        val factory = ConfirmStripeIntentParamsFactory.createFactory(
            clientSecret = clientSecret,
            intent = intent,
            shipping = shippingValues,
        ) ?: run {
            val exception = InvalidClientSecretException(clientSecret, intent)

            return createFailStep(exception, exception.message)
        }

        val confirmParams = factory.create(
            paymentMethod = paymentMethod,
            optionsParams = paymentMethodOptionsParams,
            extraParams = paymentMethodExtraParams,
            intentConfigSetupFutureUsage = intentConfigSetupFutureUsage,
            radarOptions = hCaptchaToken?.let { RadarOptions(it) }
        )
        return ConfirmationDefinition.Action.Launch(
            launcherArguments = Args.Confirm(confirmParams),
            deferredIntentConfirmationType = DeferredIntentConfirmationType.Client.takeIf { isDeferred },
            receivesResultInProcess = false,
        )
    }

    private fun createConfirmStep(
        clientSecret: String,
        intent: StripeIntent,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
        paymentMethodExtraParams: PaymentMethodExtraParams? = null,
    ): ConfirmationDefinition.Action<Args> {
        val paramsFactory = ConfirmStripeIntentParamsFactory.createFactory(
            clientSecret = clientSecret,
            intent = intent,
            shipping = shippingValues,
        ) ?: run {
            val exception = InvalidClientSecretException(clientSecret, intent)

            return createFailStep(exception, exception.message)
        }

        val confirmParams = paramsFactory.create(
            paymentMethodCreateParams,
            paymentMethodOptionsParams,
            paymentMethodExtraParams,
        )

        return ConfirmationDefinition.Action.Launch(
            launcherArguments = Args.Confirm(confirmParams),
            deferredIntentConfirmationType = null,
            receivesResultInProcess = false,
        )
    }

    private fun createFailStep(
        exception: Exception,
        message: String,
    ): ConfirmationDefinition.Action<Args> {
        return ConfirmationDefinition.Action.Fail(
            cause = exception,
            message = if (requestOptions.apiKeyIsLiveMode) {
                PaymentsCoreR.string.stripe_internal_error.resolvableString
            } else {
                message.resolvableString
            },
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
        )
    }
}
