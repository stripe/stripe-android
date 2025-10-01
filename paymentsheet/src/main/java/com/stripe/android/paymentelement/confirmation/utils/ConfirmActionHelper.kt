package com.stripe.android.paymentelement.confirmation.utils

import com.stripe.android.ConfirmStripeIntentParamsFactory
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition.Args
import com.stripe.android.paymentelement.confirmation.intent.InvalidClientSecretException
import com.stripe.android.paymentelement.confirmation.intent.InvalidDeferredIntentUsageException
import com.stripe.android.paymentsheet.DeferredIntentValidator
import com.stripe.android.paymentsheet.R
import com.stripe.android.R as PaymentsCoreR


internal class ConfirmActionHelper(val isLiveMode: Boolean) {
    fun createNextAction(
        clientSecret: String,
        intent: StripeIntent,
        paymentMethod: PaymentMethod
    ): ConfirmationDefinition.Action<Args> {
        return runCatching<ConfirmationDefinition.Action<Args>> {
            DeferredIntentValidator.validatePaymentMethod(intent, paymentMethod)

            ConfirmationDefinition.Action.Launch(
                launcherArguments = Args.NextAction(clientSecret),
                deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
                receivesResultInProcess = false,
            )
        }.getOrElse {
            ConfirmationDefinition.Action.Fail(
                cause = InvalidDeferredIntentUsageException(),
                message = resolvableString(R.string.stripe_paymentsheet_invalid_deferred_intent_usage),
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        }
    }

    fun createConfirmAction(
        clientSecret: String,
        intent: StripeIntent,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        isDeferred: Boolean,
        confirmParamsCreation:
        ConfirmStripeIntentParamsFactory<ConfirmStripeIntentParams>.() -> ConfirmStripeIntentParams
    ): ConfirmationDefinition.Action<Args> {
        val factory = ConfirmStripeIntentParamsFactory.createFactory(
            clientSecret = clientSecret,
            intent = intent,
            shipping = shippingValues,
        ) ?: run {
            val exception = InvalidClientSecretException(clientSecret, intent)

            return createFailAction(exception, exception.message)
        }

        val confirmParams = factory.confirmParamsCreation()
        return ConfirmationDefinition.Action.Launch(
            launcherArguments = Args.Confirm(confirmParams),
            deferredIntentConfirmationType = DeferredIntentConfirmationType.Client.takeIf { isDeferred },
            receivesResultInProcess = false,
        )
    }

    fun createFailAction(
        exception: Exception,
        message: String,
    ): ConfirmationDefinition.Action<Args> {
        return ConfirmationDefinition.Action.Fail(
            cause = exception,
            message = if (isLiveMode) {
                PaymentsCoreR.string.stripe_internal_error.resolvableString
            } else {
                message.resolvableString
            },
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
        )
    }
}