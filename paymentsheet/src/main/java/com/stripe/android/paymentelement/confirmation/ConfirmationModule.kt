package com.stripe.android.paymentelement.confirmation

import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
internal interface ConfirmationModule {
    @Binds
    fun bindsConfirmationHandlerFactory(
        defaultConfirmationHandlerFactory: DefaultConfirmationHandler.Factory
    ): ConfirmationHandler.Factory

    companion object {
        @Provides
        @JvmSuppressWildcards
        fun providesConfirmationRegistry(
            definitions: Set<ConfirmationDefinition<*, *, *, *>>
        ): ConfirmationRegistry {
            return ConfirmationRegistry(definitions.toList())
        }
    }
}
