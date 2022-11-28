package com.stripe.android.ui.core.elements.messaging.injection

import android.app.Application
import android.content.Context
import com.stripe.android.BuildConfig
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.ui.core.elements.messaging.PaymentMethodMessageView
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal object PaymentMethodMessageModule {
    @Provides
    fun providesAppContext(application: Application): Context = application

    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(
        configuration: PaymentMethodMessageView.Configuration
    ): () -> String = { configuration.publishableKey }

    @Provides
    @Named(PRODUCT_USAGE)
    fun providesProductUsage(): Set<String> = emptySet()

    @Provides
    @Named(ENABLE_LOGGING)
    fun providesEnableLogging(): Boolean = BuildConfig.DEBUG
}
