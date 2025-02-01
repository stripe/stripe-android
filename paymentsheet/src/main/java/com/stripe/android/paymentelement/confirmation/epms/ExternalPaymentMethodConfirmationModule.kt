package com.stripe.android.paymentelement.confirmation.epms

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
internal interface ExternalPaymentMethodConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsExternalPaymentMethodConfirmationDefinition(
        definition: ExternalPaymentMethodConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>

    companion object {
        @Provides
        fun provideExternalPaymentMethodConfirmHandler(): ExternalPaymentMethodConfirmHandler? {
            return ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler
        }
    }
}
