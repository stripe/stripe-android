package com.stripe.android.paymentelement.confirmation.injection

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.ConfirmationRegistry
import com.stripe.android.paymentelement.confirmation.ConfirmationSaver
import com.stripe.android.paymentelement.confirmation.DefaultConfirmationHandler
import com.stripe.android.paymentelement.confirmation.DefaultIsCardPaymentMethodForChallenge
import com.stripe.android.paymentelement.confirmation.DefaultIsEligibleForConfirmationChallenge
import com.stripe.android.paymentelement.confirmation.IsCardPaymentMethodForChallenge
import com.stripe.android.paymentelement.confirmation.IsEligibleForConfirmationChallenge
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

    @Binds
    fun bindsIsEligibleForConfirmationChallenge(
        isEligibleForConfirmationChallenge: DefaultIsEligibleForConfirmationChallenge
    ): IsEligibleForConfirmationChallenge

    companion object {
        @JvmSuppressWildcards
        @Provides
        fun providesConfirmationRegistry(
            definitions: Set<ConfirmationDefinition<*, *, *, *>>
        ): ConfirmationRegistry {
            return ConfirmationRegistry(definitions.toList())
        }

        @Provides
        fun providesIsCardPaymentMethod(): IsCardPaymentMethodForChallenge {
            return DefaultIsCardPaymentMethodForChallenge
        }
    }
}
