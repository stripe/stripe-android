package com.stripe.android.paymentelement.confirmation.interceptor

import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.utils.FakeIntentConfirmationInterceptor

internal open class FakeIntentConfirmationInterceptorFactory(
    val enqueueStep: FakeIntentConfirmationInterceptor.() -> Unit = {}
) : IntentConfirmationInterceptor.Factory {
    lateinit var interceptor: FakeIntentConfirmationInterceptor
    override suspend fun create(
        integrationMetadata: IntegrationMetadata,
        customerId: String?,
        ephemeralKeySecret: String?,
        clientAttributionMetadata: ClientAttributionMetadata,
    ): IntentConfirmationInterceptor {
        interceptor = FakeIntentConfirmationInterceptor().apply(enqueueStep)
        return interceptor
    }
}
