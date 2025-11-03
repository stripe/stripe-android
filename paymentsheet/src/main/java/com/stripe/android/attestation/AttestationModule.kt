package com.stripe.android.attestation

import android.app.Application
import android.content.Context
import com.stripe.android.BuildConfig
import com.stripe.android.attestation.analytics.AttestationAnalyticsModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module(includes = [AttestationAnalyticsModule::class])
internal abstract class AttestationModule {

    @Binds
    abstract fun bindsAnalyticsRequestFactory(
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory

    companion object {
        @Provides
        @Singleton
        fun provideDurationProvider(): DurationProvider {
            return DefaultDurationProvider.instance
        }

        @Provides
        @Named(ENABLE_LOGGING)
        fun providesEnableLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        fun providesContext(application: Application): Context = application
    }
}
