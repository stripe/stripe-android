package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.model.ClientAttributionMetadata
import dagger.Binds
import dagger.Module
import javax.inject.Inject

internal class UnsupportedCheckoutSessionConfirmationInterceptorFactory @Inject constructor() :
    CheckoutSessionConfirmationInterceptor.Factory {
    override fun create(
        integrationMetadata: IntegrationMetadata.CheckoutSession,
        customerMetadata: CustomerMetadata?,
        clientAttributionMetadata: ClientAttributionMetadata,
    ): CheckoutSessionConfirmationInterceptor {
        error("Checkout Session confirmation is only supported by CheckoutController.")
    }
}

@Module
internal interface UnsupportedCheckoutSessionConfirmationModule {
    @Binds
    fun bindsCheckoutSessionConfirmationInterceptorFactory(
        implementation: UnsupportedCheckoutSessionConfirmationInterceptorFactory,
    ): CheckoutSessionConfirmationInterceptor.Factory
}
