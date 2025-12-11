package com.stripe.android.paymentelement.confirmation.intent

import dagger.Binds
import dagger.Module

@Module(includes = [IntentConfirmationModule::class])
internal interface DefaultIntentConfirmationModule {
    @Binds
    fun bindsIntentConfirmationInterceptorFactory(
        defaultConfirmationInterceptorFactory: DefaultIntentConfirmationInterceptorFactory
    ): IntentConfirmationInterceptor.Factory
}
