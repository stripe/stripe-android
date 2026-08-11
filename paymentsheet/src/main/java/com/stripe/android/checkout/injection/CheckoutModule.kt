package com.stripe.android.checkout.injection

import android.app.Application
import android.content.Context
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.common.di.DisplayDensity
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.StripeNetworkClientModule
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.PaymentConfigurationModule
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module(includes = [PaymentConfigurationModule::class, StripeNetworkClientModule::class])
internal object CheckoutModule {
    @Provides
    fun provideContext(application: Application): Context = application.applicationContext

    @Provides
    @Named(ENABLE_LOGGING)
    fun provideEnabledLogging(): Boolean = BuildConfig.DEBUG

    @Provides
    @Named(PRODUCT_USAGE)
    fun provideProductUsageTokens(): Set<String> = setOf("Checkout")

    @Provides
    fun provideApiRequestOptionsProvider(
        apiConfigProvider: () -> ApiConfiguration.State
    ): () -> ApiRequest.Options = {
        ApiRequest.Options(
            apiKey = apiConfigProvider().publishableKey,
            stripeAccount = apiConfigProvider().stripeAccountId,
        )
    }

    @Provides
    fun provideApiRequestOptions(
        apiRequestOptionsProvider: () -> ApiRequest.Options
    ): ApiRequest.Options = apiRequestOptionsProvider()

    @Provides
    fun provideStripeImageLoader(context: Context): StripeImageLoader = DefaultStripeImageLoader(context)

    @Provides
    @DisplayDensity
    fun provideDisplayDensity(context: Context): Float = context.resources.displayMetrics.density
}
