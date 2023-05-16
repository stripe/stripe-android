package com.stripe.android.testing

import com.stripe.android.IntentConfirmationInterceptor
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.channels.Channel

class FakeIntentConfirmationInterceptor : IntentConfirmationInterceptor {

    private val channel = Channel<IntentConfirmationInterceptor.NextStep>(capacity = 1)

    fun enqueueConfirmStep(confirmParams: ConfirmStripeIntentParams) {
        val nextStep = IntentConfirmationInterceptor.NextStep.Confirm(confirmParams)
        channel.trySend(nextStep)
    }

    fun enqueueCompleteStep(
        isForceSuccess: Boolean = false,
    ) {
        channel.trySend(IntentConfirmationInterceptor.NextStep.Complete(isForceSuccess))
    }

    fun enqueueNextActionStep(clientSecret: String) {
        val nextStep = IntentConfirmationInterceptor.NextStep.HandleNextAction(clientSecret)
        channel.trySend(nextStep)
    }

    fun enqueueFailureStep(cause: Exception, message: String) {
        val nextStep = IntentConfirmationInterceptor.NextStep.Fail(
            cause = cause,
            message = message
        )
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
