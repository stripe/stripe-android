package com.stripe.android.checkout.analytics

import android.content.Context
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import dagger.Module
import dagger.Provides
import javax.inject.Provider
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class CheckoutAnalytics

@Module
internal object CheckoutAnalyticsModule {
    @Provides
    @CheckoutAnalytics
    fun provideAnalyticsRequestFactory(
        context: Context,
        apiConfigurationProvider: Provider<ApiConfiguration.State>,
    ): AnalyticsRequestFactory {
        return PaymentAnalyticsRequestFactory(
            context = context,
            defaultProductUsageTokens = setOf("checkout_elements"),
            apiConfigurationProvider = apiConfigurationProvider,
        )
    }
}
