package com.stripe.android.payments.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.ApiConfiguration
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import dagger.Module
import dagger.Provides
import javax.inject.Provider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module(includes = [PaymentConfigurationModule::class])
class ApiConfigurationFromPaymentConfigurationModule {
    @Provides
    fun provideApiConfigurationStateProvider(
        paymentConfiguration: Provider<PaymentConfiguration>
    ): () -> ApiConfiguration.State {
        return {
            val config = paymentConfiguration.get()
            ApiConfiguration.State(
                publishableKey = config.publishableKey,
                stripeAccountId = config.stripeAccountId,
            )
        }
    }
}

/**
 * Eagerly evaluates a `() -> ApiConfiguration.State` provider function to produce an
 * [ApiConfiguration.State] instance. Use this in components that only have a
 * `() -> ApiConfiguration.State` binding and need to satisfy dependencies that
 * take [ApiConfiguration.State] directly.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class ApiConfigurationStateFromProviderModule {
    @Provides
    fun provideApiConfigurationState(
        provider: () -> ApiConfiguration.State
    ): ApiConfiguration.State = provider()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class ApiRequestOptionsModule {
    @Provides
    fun provideApiRequestOptionsProvider(
        apiConfigurationProvider: () -> ApiConfiguration.State
    ): () -> ApiRequest.Options {
        return {
            val state = apiConfigurationProvider()
            ApiRequest.Options(
                apiKey = state.publishableKey,
                stripeAccount = state.stripeAccountId,
            )
        }
    }

    @Provides
    fun provideApiRequestOptions(
        apiRequestOptionsProvider: () -> ApiRequest.Options
    ): ApiRequest.Options = apiRequestOptionsProvider()
}
