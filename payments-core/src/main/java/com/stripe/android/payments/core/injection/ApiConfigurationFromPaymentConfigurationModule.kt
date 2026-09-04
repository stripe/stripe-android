package com.stripe.android.payments.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.ApiRequest
import dagger.Module
import dagger.Provides
import javax.inject.Provider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module(includes = [PaymentConfigurationModule::class])
class ApiConfigurationFromPaymentConfigurationModule {
    @Provides
    fun provideApiConfiguration(
        paymentConfiguration: Provider<PaymentConfiguration>
    ): ApiConfiguration.State {
        return ApiConfiguration.State(
            publishableKey = paymentConfiguration.get().publishableKey,
            stripeAccountId = paymentConfiguration.get().stripeAccountId,
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class ApiRequestOptionsModule {
    @Provides
    fun provideApiRequestOptions(
        apiConfigurationProvider: Provider<ApiConfiguration.State>
    ): ApiRequest.Options {
        return ApiRequest.Options(
            apiKey = apiConfigurationProvider.get().publishableKey,
            stripeAccount = apiConfigurationProvider.get().stripeAccountId,
        )
    }
}
