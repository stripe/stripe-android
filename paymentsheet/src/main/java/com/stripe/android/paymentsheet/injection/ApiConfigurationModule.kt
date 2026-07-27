package com.stripe.android.paymentsheet.injection

import com.stripe.android.ApiConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import dagger.Module
import dagger.Provides
import javax.inject.Provider

@Module
internal object ApiConfigurationModule {
    @Provides
    fun provideApiConfigurationState(
        paymentMethodMetadata: Provider<PaymentMethodMetadata?>
    ): ApiConfiguration.State {
        return requireNotNull(paymentMethodMetadata.get()).apiConfiguration
    }
}
