package com.stripe.android.utils

import com.stripe.android.interceptor.IntentConfirmationInterceptor
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.channels.Channel

internal class FakeIntentConfirmationInterceptor : IntentConfirmationInterceptor {
    private val nextStepChannel = Channel<IntentConfirmationInterceptor.NextStep>(1)

    fun emitNextStep(nextStep: IntentConfirmationInterceptor.NextStep) {
        nextStepChannel.trySend(nextStep)
    }

    override suspend fun intercept(
        clientSecret: String?,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?
    ): IntentConfirmationInterceptor.NextStep {
        return nextStepChannel.receive()
    }

    override suspend fun intercept(
        clientSecret: String?,
        paymentMethod: PaymentMethod,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        setupForFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?
    ): IntentConfirmationInterceptor.NextStep {
        return nextStepChannel.receive()
    }
}