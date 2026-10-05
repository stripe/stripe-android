package com.stripe.android.payments.core.injection

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.ApiRequest
import dagger.Module
import dagger.Provides
import javax.inject.Provider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class ApiConfigurationFromPaymentConfigurationModule {
    @Provides
    fun provideApiConfiguration(
        context: Context
    ): ApiConfiguration.State {
        val paymentConfiguration = PaymentConfiguration.getInstance(context)
        return ApiConfiguration(
            publishableKey = paymentConfiguration.publishableKey,
        ).stripeAccountId(paymentConfiguration.stripeAccountId).build()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class ApiRequestOptionsModule {
    @Provides
    fun provideApiRequestOptions(
        apiConfigurationProvider: Provider<ApiConfiguration.State>
    ): ApiRequest.Options {
        val apiConfiguration = apiConfigurationProvider.get()
        return ApiRequest.Options(
            apiKey = apiConfiguration.publishableKey,
            stripeAccount = apiConfiguration.stripeAccountId,
        )
    }
}
