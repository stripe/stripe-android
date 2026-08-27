package com.stripe.android.payments.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import dagger.Module
import dagger.Provides
import javax.inject.Named
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class ApiConfigurationFromNamedModule {
    @Provides
    fun provideApiConfigurationStateProvider(
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?,
    ): () -> ApiConfiguration.State = {
        ApiConfiguration.State(
            publishableKey = publishableKeyProvider(),
            stripeAccountId = stripeAccountIdProvider(),
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class ApiConfigurationToNamedModule {
    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKeyProvider(
        apiConfigurationProvider: () -> ApiConfiguration.State
    ): () -> String = { apiConfigurationProvider().publishableKey }

    @Provides
    @Named(STRIPE_ACCOUNT_ID)
    fun provideStripeAccountIdProvider(
        apiConfigurationProvider: () -> ApiConfiguration.State
    ): () -> String? = { apiConfigurationProvider().stripeAccountId }
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
