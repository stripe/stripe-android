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
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.state.PaymentElementLoader
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
        initializationMode: PaymentElementLoader.InitializationMode,
        intent: StripeIntent,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        customerRequestedSave: Boolean,
    ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
        _calls.add(
            InterceptCall.WithNewPaymentMethod(
                initializationMode = initializationMode,
                paymentMethodCreateParams = paymentMethodCreateParams,
                paymentMethodOptionsParams = paymentMethodOptionsParams,
                paymentMethodExtraParams = paymentMethodExtraParams,
                shippingValues = shippingValues,
                customerRequestedSave = customerRequestedSave,
            )
        )

        return channel.receive()
    }

    override suspend fun intercept(
        initializationMode: PaymentElementLoader.InitializationMode,
        intent: StripeIntent,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        paymentMethodExtraParams: PaymentMethodExtraParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        hCaptchaToken: String?,
    ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
        _calls.add(
            InterceptCall.WithExistingPaymentMethod(
                initializationMode = initializationMode,
                paymentMethod = paymentMethod,
                paymentMethodOptionsParams = paymentMethodOptionsParams,
                shippingValues = shippingValues,
                hCaptchaToken = hCaptchaToken,
            )
        )

        return channel.receive()
    }

    sealed interface InterceptCall {
        data class WithNewPaymentMethod(
            val initializationMode: PaymentElementLoader.InitializationMode,
            val paymentMethodCreateParams: PaymentMethodCreateParams,
            val paymentMethodOptionsParams: PaymentMethodOptionsParams?,
            val paymentMethodExtraParams: PaymentMethodExtraParams?,
            val shippingValues: ConfirmPaymentIntentParams.Shipping?,
            val customerRequestedSave: Boolean,
        ) : InterceptCall

        data class WithExistingPaymentMethod(
            val initializationMode: PaymentElementLoader.InitializationMode,
            val paymentMethod: PaymentMethod,
            val paymentMethodOptionsParams: PaymentMethodOptionsParams?,
            val shippingValues: ConfirmPaymentIntentParams.Shipping?,
            val hCaptchaToken: String?,
        ) : InterceptCall
    }
}
