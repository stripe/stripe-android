package com.stripe.android.paymentelement.confirmation.cpms

import com.stripe.android.paymentelement.ConfirmCustomPaymentMethodCallback
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
    fun bindsCustomPaymentMethodConfirmationDefinition(
        customPaymentMethodConfirmationDefinition: CustomPaymentMethodConfirmationDefinition,
    ): ConfirmationDefinition<*, *, *, *>

    companion object {
        @Provides
        fun provideConfirmCustomPaymentMethodCallback(): ConfirmCustomPaymentMethodCallback? {
            return CustomPaymentMethodProxyActivity.confirmCustomPaymentMethodCallback
        }
    }
}
