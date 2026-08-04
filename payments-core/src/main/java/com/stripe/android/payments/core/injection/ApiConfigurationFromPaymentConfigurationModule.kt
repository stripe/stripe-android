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
    fun provideApiConfigurationState(
        paymentConfiguration: Provider<PaymentConfiguration>
    ): ApiConfiguration.State {
        val config = paymentConfiguration.get()
        return ApiConfiguration.State(
            publishableKey = config.publishableKey,
            stripeAccountId = config.stripeAccountId,
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class ApiRequestOptionsModule {
    @Provides
    fun provideApiRequestOptions(
        apiConfigurationState: Provider<ApiConfiguration.State>
    ): ApiRequest.Options {
        val state = apiConfigurationState.get()
        return ApiRequest.Options(
            apiKey = state.publishableKey,
            stripeAccount = state.stripeAccountId,
        )
    }
}
