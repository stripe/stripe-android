package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepositoryModule
import dagger.Binds
import dagger.Module

@Module(
    includes = [
        IntentConfirmationModule::class,
        CheckoutSessionRepositoryModule::class,
    ]
)
internal interface DefaultIntentConfirmationModule {
    @Binds
    fun bindsIntentConfirmationInterceptorFactory(
        defaultConfirmationInterceptorFactory: DefaultIntentConfirmationInterceptorFactory
    ): IntentConfirmationInterceptor.Factory
}
