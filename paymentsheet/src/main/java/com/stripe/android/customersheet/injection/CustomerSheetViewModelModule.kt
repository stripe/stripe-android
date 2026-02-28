package com.stripe.android.customersheet.injection

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.core.os.LocaleListCompat
import com.stripe.android.BuildConfig
import com.stripe.android.core.DefaultIsExampleApp
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.RealUserFacingLogger
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.customersheet.CustomerSheetLoader
import com.stripe.android.customersheet.DefaultCustomerSheetLoader
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.customersheet.analytics.DefaultCustomerSheetEventReporter
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.PaymentConfigurationModule
import com.stripe.android.payments.financialconnections.DefaultIsFinancialConnectionsAvailable
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsSdkAvailable
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.repositories.RealElementsSessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

@Module(includes = [PaymentConfigurationModule::class])
internal interface CustomerSheetViewModelModule {
    @Binds
    fun bindsUserFacingLogger(impl: RealUserFacingLogger): UserFacingLogger

    @Binds
    fun bindsCustomerSheetEventReporter(
        impl: DefaultCustomerSheetEventReporter
    ): CustomerSheetEventReporter

    @Binds
    fun bindsCustomerSheetLoader(
        impl: DefaultCustomerSheetLoader
    ): CustomerSheetLoader

    @Binds
    fun bindsAnalyticsRequestFactory(
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory

    @Binds
    fun bindsStripeIntentRepository(
        impl: RealElementsSessionRepository,
    ): ElementsSessionRepository

    @Binds
    fun bindsPrefsRepositoryFactory(
        factory: DefaultPrefsRepository.Factory
    ): PrefsRepository.Factory

    @Suppress("TooManyFunctions")
    companion object {
        @Provides
        @PaymentElementCallbackIdentifier
        fun providesPaymentElementCallbackIdentifier(): String {
            // We currently do not support multiple instances of Customer Sheet
            return "CustomerSheet"
        }

        @Provides
        fun providesIsFinancialConnectionsAvailable(): IsFinancialConnectionsSdkAvailable {
            return DefaultIsFinancialConnectionsAvailable
        }

        @Provides
        internal fun providesErrorReporter(
            analyticsRequestFactory: AnalyticsRequestFactory,
            analyticsRequestExecutor: AnalyticsRequestExecutor
        ): ErrorReporter = RealErrorReporter(
            analyticsRequestFactory = analyticsRequestFactory,
            analyticsRequestExecutor = analyticsRequestExecutor,
        )

        @Provides
        fun resources(application: Application): Resources {
            return application.resources
        }

        @Provides
        fun context(application: Application): Context {
            return application
        }

        @Provides
        @IOContext
        fun ioContext(): CoroutineContext {
            return Dispatchers.IO
        }

        @Provides
        @UIContext
        fun uiContext(): CoroutineContext {
            return Dispatchers.Main
        }

        @Provides
        @Named(PRODUCT_USAGE)
        fun provideProductUsageTokens() = setOf("CustomerSheet")

        @Provides
        @Named(ENABLE_LOGGING)
        fun providesEnableLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        fun provideLogger(
            @Named(ENABLE_LOGGING) enableLogging: Boolean,
            context: Context,
        ) = Logger.getInstance(enableLogging, DefaultIsExampleApp(context)())

        @Provides
        fun provideLocale() =
            LocaleListCompat.getAdjustedDefault().takeUnless { it.isEmpty }?.get(0)

        @Provides
        @Named(ALLOWS_MANUAL_CONFIRMATION)
        fun provideAllowsManualConfirmation() = false

        @Provides
        fun savedPaymentSelection(): PaymentSelection? = savedPaymentSelection

        @Provides
        fun provideDurationProvider(): DurationProvider {
            return DefaultDurationProvider.instance
        }

        private val savedPaymentSelection: PaymentSelection? = null
    }
}
