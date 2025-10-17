package com.stripe.android.paymentmethodmessaging.view.injection

import android.app.Application
import android.content.Context
import com.stripe.android.BuildConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal object PaymentMethodMessagingModule {
    @Provides
    fun providesAppContext(application: Application): Context = application

    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(
        configuration: PaymentConfiguration
    ): () -> String = { configuration.publishableKey }

    @Provides
    fun paymentConfiguration(application: Application): PaymentConfiguration {
        return PaymentConfiguration.getInstance(application)
    }

    @Provides
    @Named(PRODUCT_USAGE)
    fun providesPaymentConfiguration(): Set<String> = emptySet()

    @Provides
    @Named(ENABLE_LOGGING)
    fun providesEnableLogging(): Boolean = BuildConfig.DEBUG

    @Provides
    fun providesStripeImageLoader(
        application: Application
    ): StripeImageLoader = StripeImageLoader(application)
}
