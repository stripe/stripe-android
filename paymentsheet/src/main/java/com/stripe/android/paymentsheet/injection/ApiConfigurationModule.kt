package com.stripe.android.paymentsheet.injection

import com.stripe.android.core.ApiConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.payments.core.injection.ApiRequestOptionsModule
import dagger.Module
import dagger.Provides

@Module(includes = [ApiRequestOptionsModule::class])
internal object ApiConfigurationModule {
    @Provides
    fun provideApiConfiguration(
        paymentMethodMetadata: PaymentMethodMetadata?
    ): ApiConfiguration.State = requireNotNull(paymentMethodMetadata?.apiConfiguration)
}
