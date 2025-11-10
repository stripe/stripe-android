package com.stripe.android.paymentelement.confirmation.injection

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.ConfirmationRegistry
import com.stripe.android.paymentelement.confirmation.ConfirmationSaver
import com.stripe.android.paymentelement.confirmation.DefaultConfirmationHandler
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
internal interface ConfirmationHandlerModule {
    @Binds
    fun bindsConfirmationHandlerFactory(
        defaultConfirmationHandlerFactory: DefaultConfirmationHandler.Factory
    ): ConfirmationHandler.Factory

    @Binds
    fun bindsConfirmationSaver(
        saver: ConfirmationSaver
    ): ConfirmationHandler.Saver

    companion object {
        @JvmSuppressWildcards
        @Provides
        fun providesConfirmationRegistry(
            definitions: Set<ConfirmationDefinition<*, *, *, *>>
        ): ConfirmationRegistry {
            return ConfirmationRegistry(definitions.toList())
        }
    }
}
