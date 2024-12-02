package com.stripe.android.paymentelement.confirmation.epms

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor
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
        return ExternalPaymentMethodConfirmationDefinition(
            externalPaymentMethodConfirmHandlerProvider = {
                ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler
            },
            errorReporter = errorReporter
        )
    }
}
