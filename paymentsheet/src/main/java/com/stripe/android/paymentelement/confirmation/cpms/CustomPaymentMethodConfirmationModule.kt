package com.stripe.android.paymentelement.confirmation.cpms

import com.stripe.android.paymentelement.CustomPaymentMethodConfirmHandler
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
@Module
internal interface CustomPaymentMethodConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsCvcConfirmationDefinition(
        cvcReConfirmationDefinition: CustomPaymentMethodConfirmationDefinition,
    ): ConfirmationDefinition<*, *, *, *>

    companion object {
        @Provides
        fun provideCustomPaymentMethodConfirmHandler(): CustomPaymentMethodConfirmHandler? {
            return CustomPaymentMethodProxyActivity.customPaymentMethodConfirmHandler
        }
    }
}
