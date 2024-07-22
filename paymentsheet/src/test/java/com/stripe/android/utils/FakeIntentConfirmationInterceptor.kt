package com.stripe.android.utils

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.channels.Channel

internal class FakeIntentConfirmationInterceptor : IntentConfirmationInterceptor {
    private val _calls = mutableListOf<InterceptCall>()
    val calls: List<InterceptCall>
        get() = _calls.toList()

    private val channel = Channel<IntentConfirmationInterceptor.NextStep>(capacity = 1)

    fun enqueueConfirmStep(
        confirmParams: ConfirmStripeIntentParams,
        isDeferred: Boolean = false,
    ) {
        val nextStep = IntentConfirmationInterceptor.NextStep.Confirm(
            confirmParams = confirmParams,
            isDeferred = isDeferred,
        )
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
            message = message.resolvableString
        )
        channel.trySend(nextStep)
    }

    override suspend fun intercept(
        initializationMode: PaymentSheet.InitializationMode,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        customerRequestedSave: Boolean,
    ): IntentConfirmationInterceptor.NextStep {
        _calls.add(
            InterceptCall.WithNewPaymentMethod(
                initializationMode = initializationMode,
                paymentMethodCreateParams = paymentMethodCreateParams,
                paymentMethodOptionsParams = paymentMethodOptionsParams,
                shippingValues = shippingValues,
                customerRequestedSave = customerRequestedSave,
            )
        )

        return channel.receive()
    }

    override suspend fun intercept(
        initializationMode: PaymentSheet.InitializationMode,
        paymentMethod: PaymentMethod,
        paymentMethodOptionsParams: PaymentMethodOptionsParams?,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
    ): IntentConfirmationInterceptor.NextStep {
        _calls.add(
            InterceptCall.WithExistingPaymentMethod(
                initializationMode = initializationMode,
                paymentMethod = paymentMethod,
                paymentMethodOptionsParams = paymentMethodOptionsParams,
                shippingValues = shippingValues,
            )
        )

        return channel.receive()
    }

    sealed interface InterceptCall {
        data class WithNewPaymentMethod(
            val initializationMode: PaymentSheet.InitializationMode,
            val paymentMethodCreateParams: PaymentMethodCreateParams,
            val paymentMethodOptionsParams: PaymentMethodOptionsParams?,
            val shippingValues: ConfirmPaymentIntentParams.Shipping?,
            val customerRequestedSave: Boolean,
        ) : InterceptCall

        data class WithExistingPaymentMethod(
            val initializationMode: PaymentSheet.InitializationMode,
            val paymentMethod: PaymentMethod,
            val paymentMethodOptionsParams: PaymentMethodOptionsParams?,
            val shippingValues: ConfirmPaymentIntentParams.Shipping?,
        ) : InterceptCall
    }
}
