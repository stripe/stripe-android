package com.stripe.android.utils

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import kotlinx.coroutines.channels.Channel
import org.mockito.Mockito.mock

internal class FakeIntentConfirmationInterceptor : IntentConfirmationInterceptor {
    private val _calls = Turbine<InterceptCall>()
    val calls: ReceiveTurbine<InterceptCall> = _calls

    private val channel = Channel<ConfirmationDefinition.Action<IntentConfirmationDefinition.Args>>(capacity = 1)

    fun enqueueConfirmStep(
        confirmParams: ConfirmStripeIntentParams,
        isDeferred: Boolean = false,
    ) {
        val nextStep: ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> =
            ConfirmationDefinition.Action.Launch(
                launcherArguments = IntentConfirmationDefinition.Args.Confirm(confirmParams),
                deferredIntentConfirmationType = DeferredIntentConfirmationType.Client.takeIf { isDeferred },
                receivesResultInProcess = false,
            )
        channel.trySend(nextStep)
    }

    fun enqueueCompleteStep(
        isForceSuccess: Boolean = false,
        completedFullPaymentFlow: Boolean = true,
        intent: StripeIntent = mock(),
    ) {
        channel.trySend(
            ConfirmationDefinition.Action.Complete(
                intent = intent,
                deferredIntentConfirmationType = if (isForceSuccess) {
                    DeferredIntentConfirmationType.None
                } else {
                    DeferredIntentConfirmationType.Server
                },
                completedFullPaymentFlow = completedFullPaymentFlow,
            )
        )
    }

    fun enqueueNextActionStep(clientSecret: String) {
        channel.trySend(
            ConfirmationDefinition.Action.Launch(
                launcherArguments = IntentConfirmationDefinition.Args.NextAction(clientSecret),
                deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
                receivesResultInProcess = false,
            )
        )
    }

    fun enqueueFailureStep(cause: Exception, message: String) {
        channel.trySend(
            ConfirmationDefinition.Action.Fail(
                cause = cause,
                message = message.resolvableString,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )
    }

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.New,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
        _calls.add(
            InterceptCall.WithNewPaymentMethod(
                paymentMethodCreateParams = confirmationOption.createParams,
                paymentMethodOptionsParams = confirmationOption.optionsParams,
                paymentMethodExtraParams = confirmationOption.extraParams,
                shippingValues = shippingValues,
                customerRequestedSave = confirmationOption.shouldSave,
            )
        )

        return channel.receive()
    }

    override suspend fun intercept(
        intent: StripeIntent,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
        _calls.add(
            InterceptCall.WithExistingPaymentMethod(
                paymentMethod = confirmationOption.paymentMethod,
                paymentMethodOptionsParams = confirmationOption.optionsParams,
                shippingValues = shippingValues,
                hCaptchaToken = confirmationOption.hCaptchaToken,
            )
        )

        return channel.receive()
    }

    sealed interface InterceptCall {
        data class WithNewPaymentMethod(
            val paymentMethodCreateParams: PaymentMethodCreateParams,
            val paymentMethodOptionsParams: PaymentMethodOptionsParams?,
            val paymentMethodExtraParams: PaymentMethodExtraParams?,
            val shippingValues: ConfirmPaymentIntentParams.Shipping?,
            val customerRequestedSave: Boolean,
        ) : InterceptCall

        data class WithExistingPaymentMethod(
            val paymentMethod: PaymentMethod,
            val paymentMethodOptionsParams: PaymentMethodOptionsParams?,
            val shippingValues: ConfirmPaymentIntentParams.Shipping?,
            val hCaptchaToken: String?,
        ) : InterceptCall
    }
}
