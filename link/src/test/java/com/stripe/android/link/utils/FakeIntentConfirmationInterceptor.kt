package com.stripe.android.link.utils

import com.stripe.android.IntentConfirmationInterceptor
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import kotlinx.coroutines.channels.Channel

internal class FakeIntentConfirmationInterceptor : IntentConfirmationInterceptor {

    private val channel = Channel<IntentConfirmationInterceptor.NextStep>(capacity = 1)

    fun enqueueConfirmStep(confirmParams: ConfirmStripeIntentParams) {
        val nextStep = IntentConfirmationInterceptor.NextStep.Confirm(confirmParams)
        channel.trySend(nextStep)
    }

    fun enqueueCompleteStep(stripeIntent: StripeIntent) {
        val nextStep = IntentConfirmationInterceptor.NextStep.Complete(stripeIntent)
        channel.trySend(nextStep)
    }

    fun enqueueNextActionStep(clientSecret: String) {
        val nextStep = IntentConfirmationInterceptor.NextStep.HandleNextAction(clientSecret)
        channel.trySend(nextStep)
    }

    fun enqueueFailureStep(errorMessage: String) {
        val nextStep = IntentConfirmationInterceptor.NextStep.Fail(errorMessage)
        channel.trySend(nextStep)
    }

    override suspend fun intercept(
        clientSecret: String?,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?
    ): IntentConfirmationInterceptor.NextStep {
        return channel.receive()
    }

    override suspend fun intercept(
        clientSecret: String?,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?
    ): IntentConfirmationInterceptor.NextStep {
        return channel.receive()
    }
}
