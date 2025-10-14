package com.stripe.android.paymentelement.confirmation.interceptor

import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.utils.FakeIntentConfirmationInterceptor

internal open class FakeIntentConfirmationInterceptorFactory(
    val enqueueStep: FakeIntentConfirmationInterceptor.() -> Unit = {}
) : IntentConfirmationInterceptor.Factory {
    lateinit var interceptor: FakeIntentConfirmationInterceptor
    override suspend fun create(
        initializationMode: PaymentElementLoader.InitializationMode,
        ephemeralKeySecret: String?
    ): IntentConfirmationInterceptor {
        interceptor = FakeIntentConfirmationInterceptor().apply(enqueueStep)
        return interceptor
    }
}
