package com.stripe.android.paymentelement.confirmation.intent

import com.stripe.android.core.injection.StripeNetworkClientModule
import dagger.Binds
import dagger.Module

@Module(
    includes = [
        IntentConfirmationModule::class,
        StripeNetworkClientModule::class,
    ]
)
internal interface DefaultIntentConfirmationModule {
    @Binds
    fun bindsIntentConfirmationInterceptorFactory(
        defaultConfirmationInterceptorFactory: DefaultIntentConfirmationInterceptorFactory
    ): IntentConfirmationInterceptor.Factory
}
