package com.stripe.android.paymentelement.confirmation

import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.DefaultBacsMandateConfirmationLauncherFactory
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
internal interface ConfirmationModule {
    @Binds
    fun bindsConfirmationHandlerFactory(
        defaultConfirmationHandlerFactory: DefaultConfirmationHandler.Factory
    ): ConfirmationHandler.Factory

    @Binds
    fun bindsIntentConfirmationInterceptor(
        defaultConfirmationHandlerFactory: DefaultIntentConfirmationInterceptor
    ): IntentConfirmationInterceptor

    companion object {
        @Provides
        fun providesBacsMandateConfirmationLauncherFactory(): BacsMandateConfirmationLauncherFactory =
            DefaultBacsMandateConfirmationLauncherFactory
    }
}
