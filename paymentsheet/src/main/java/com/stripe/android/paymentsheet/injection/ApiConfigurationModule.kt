package com.stripe.android.paymentsheet.injection

import com.stripe.android.ApiConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import dagger.Module
import dagger.Provides
import javax.inject.Provider
import kotlin.contracts.contract

@Module
internal object ApiConfigurationModule {
    @Provides
    fun provideApiConfigurationStateProvider(
        paymentMethodMetadata: Provider<PaymentMethodMetadata?>
    ): () -> ApiConfiguration.State {
        return {
            val value = paymentMethodMetadata.get()?.apiConfiguration
            if (value == null) {
                val message = "required value was null"
                throw IllegalArgumentException(message)
            }
            value
        }
    }
}