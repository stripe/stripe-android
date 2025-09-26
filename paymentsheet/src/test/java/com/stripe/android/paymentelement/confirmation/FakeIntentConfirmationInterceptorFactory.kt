package com.stripe.android.paymentelement.confirmation

import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.utils.FakeIntentConfirmationInterceptor

internal open class FakeIntentConfirmationInterceptorFactory(
    val enqueueStep: FakeIntentConfirmationInterceptor.() -> Unit = {}
) : IntentConfirmationInterceptor.Factory {
    lateinit var interceptor: FakeIntentConfirmationInterceptor
    override fun create(initializationMode: PaymentElementLoader.InitializationMode): IntentConfirmationInterceptor {
        interceptor = FakeIntentConfirmationInterceptor().apply(enqueueStep)
        return interceptor
    }
}
