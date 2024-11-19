package com.stripe.android.paymentelement.confirmation

import com.stripe.android.payments.core.analytics.ErrorReporter
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
internal class ExternalPaymentMethodConfirmationModule {
    @JvmSuppressWildcards
    @Provides
    @IntoSet
    fun providesExternalPaymentMethodConfirmationDefinition(
        errorReporter: ErrorReporter
    ): ConfirmationDefinition<*, *, *, *> {
        return ExternalPaymentMethodConfirmationDefinition(errorReporter)
    }
}
