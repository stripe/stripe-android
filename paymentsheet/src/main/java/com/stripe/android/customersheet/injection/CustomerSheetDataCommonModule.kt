package com.stripe.android.customersheet.injection

import com.stripe.android.BuildConfig
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.PaymentConfigurationModule
import com.stripe.android.paymentsheet.repositories.CustomerApiRepository
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import java.util.Calendar
import javax.inject.Named

@Module(includes = [PaymentConfigurationModule::class])
internal interface CustomerSheetDataCommonModule {
    @Binds
    fun bindsCustomerRepository(repository: CustomerApiRepository): CustomerRepository

    @Binds
    fun bindsAnalyticsRequestFactory(
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory

    @Binds
    fun bindsErrorReporter(errorReporter: RealErrorReporter): ErrorReporter

    companion object {
        @Provides
        @Named(PRODUCT_USAGE)
        fun providesProductUsage(): Set<String> = setOf("CustomerSheet")

        @Provides
        @Named(ENABLE_LOGGING)
        fun providesEnableLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        fun provideTimeProvider(): () -> Long = {
            Calendar.getInstance().timeInMillis
        }
    }
}
