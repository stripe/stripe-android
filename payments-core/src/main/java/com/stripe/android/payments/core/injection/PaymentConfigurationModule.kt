package com.stripe.android.payments.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.ApiConfiguration
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class PaymentConfigurationModule {
    @Provides
    fun provideApiConfigurationState(
        holder: PaymentConfigurationHolder,
    ): ApiConfiguration.State {
        return holder.get()
    }

    @Provides
    fun providePaymentConfiguration(
        apiConfigurationState: Provider<ApiConfiguration.State>,
    ): PaymentConfiguration {
        return apiConfigurationState.get().toPaymentConfiguration()
    }

    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(
        apiConfigurationState: Provider<ApiConfiguration.State>,
    ): () -> String = { apiConfigurationState.get().publishableKey }

    @Provides
    @Named(STRIPE_ACCOUNT_ID)
    fun provideStripeAccountId(
        apiConfigurationState: Provider<ApiConfiguration.State>,
    ): () -> String? = { apiConfigurationState.get().stripeAccountId }
}
