package com.stripe.android.paymentsheet.injection

import com.stripe.android.core.ApiConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import dagger.Module
import dagger.Provides
import javax.inject.Provider

@Module
internal object ApiConfigurationModule {
    @Provides
    fun provideApiConfiguration(
        paymentMethodMetadata: Provider<PaymentMethodMetadata?>
    ): ApiConfiguration.State = requireNotNull(paymentMethodMetadata.get()?.apiConfiguration)
}
