package com.stripe.android.paymentelement.embedded

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.NetworkTypeDetector
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.repositories.CustomerApiRepository
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module(
    includes = [
        StripeRepositoryModule::class,
        CoreCommonModule::class,
    ],
)
internal interface EmbeddedCommonModule {
    @Binds
    @Singleton
    fun bindsEventReporter(eventReporter: DefaultEventReporter): EventReporter

    @Binds
    fun bindsErrorReporter(errorReporter: RealErrorReporter): ErrorReporter

    @Binds
    fun bindsCustomerRepository(repository: CustomerApiRepository): CustomerRepository

    @Binds
    fun bindsEmbeddedStateHelper(
        stateHelper: DefaultEmbeddedStateHelper
    ): EmbeddedStateHelper

    companion object {
        @Provides
        @Named(ENABLE_LOGGING)
        fun provideEnabledLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        @IOContext
        fun ioContext(): CoroutineContext {
            return Dispatchers.IO
        }

        @Provides
        fun provideEventReporterMode(): EventReporter.Mode {
            return EventReporter.Mode.Embedded
        }

        @Provides
        @Named(PRODUCT_USAGE)
        fun provideProductUsageTokens() = setOf("EmbeddedPaymentElement")

        @Provides
        fun provideDurationProvider(): DurationProvider {
            return DefaultDurationProvider.instance
        }

        @Provides
        fun provideAnalyticsRequestFactory(
            context: Context,
            paymentConfiguration: Provider<PaymentConfiguration>
        ): AnalyticsRequestFactory = AnalyticsRequestFactory(
            packageManager = context.packageManager,
            packageName = context.packageName.orEmpty(),
            packageInfo = context.packageInfo,
            publishableKeyProvider = { paymentConfiguration.get().publishableKey },
            networkTypeProvider = NetworkTypeDetector(context)::invoke,
        )

        @Provides
        @Singleton
        @Named(ALLOWS_MANUAL_CONFIRMATION)
        fun provideAllowsManualConfirmation() = true

        /**
         * Provides a non-singleton PaymentConfiguration.
         *
         * Should be fetched only when it's needed, to allow client to set the publishableKey and
         * stripeAccountId in PaymentConfiguration any time before configuring the FlowController
         * or presenting Payment Sheet.
         *
         * Should always be injected with [Lazy] or [Provider].
         */
        @Provides
        fun providePaymentConfiguration(appContext: Context): PaymentConfiguration {
            return PaymentConfiguration.getInstance(appContext)
        }

        @Provides
        @Named(PUBLISHABLE_KEY)
        fun providePublishableKey(
            paymentConfiguration: Provider<PaymentConfiguration>
        ): () -> String = { paymentConfiguration.get().publishableKey }

        @Provides
        @Named(STRIPE_ACCOUNT_ID)
        fun provideStripeAccountId(paymentConfiguration: Provider<PaymentConfiguration>):
            () -> String? = { paymentConfiguration.get().stripeAccountId }

        @Provides
        @Singleton
        fun provideCustomerStateHolder(
            savedStateHandle: SavedStateHandle,
            selectionHolder: EmbeddedSelectionHolder,
        ): CustomerStateHolder {
            return CustomerStateHolder(savedStateHandle, selectionHolder.selection)
        }

        @Provides
        @Singleton
        @UIContext
        fun provideUiContext(): CoroutineContext = Dispatchers.Main
    }
}
