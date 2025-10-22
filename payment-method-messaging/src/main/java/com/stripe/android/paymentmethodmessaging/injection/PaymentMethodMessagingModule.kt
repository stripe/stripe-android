package com.stripe.android.paymentmethodmessaging.injection

import android.app.Application
import android.content.Context
import com.stripe.android.BuildConfig
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.isSystemDarkTheme
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal object PaymentMethodMessagingModule {
    @Provides
    fun providesAppContext(application: Application): Context = application

    @Provides
    @Named(PRODUCT_USAGE)
    fun providesProductUsage(): Set<String> = emptySet()

    @Provides
    @Named(ENABLE_LOGGING)
    fun providesEnableLogging(): Boolean = BuildConfig.DEBUG

    @Provides
    fun providesStripeImageLoader(
        application: Application
    ): StripeImageLoader = StripeImageLoader(application)

    @Provides
    fun providesIsDarkTheme(
        application: Application
    ): () -> Boolean {
        return application::isSystemDarkTheme
    }
}
