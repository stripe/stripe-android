package com.stripe.android.paymentsheet.injection

import android.app.Application
import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.payments.core.injection.APP_NAME
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.DefaultIntentConfirmationInterceptor
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.flowcontroller.DefaultPaymentSelectionUpdater
import com.stripe.android.paymentsheet.flowcontroller.PaymentSelectionUpdater
import com.stripe.android.paymentsheet.repositories.CustomerApiRepository
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.repositories.RealElementsSessionRepository
import com.stripe.android.paymentsheet.state.CbcEnabledProvider
import com.stripe.android.paymentsheet.state.DefaultLinkAccountStatusProvider
import com.stripe.android.paymentsheet.state.DefaultPaymentSheetLoader
import com.stripe.android.paymentsheet.state.LinkAccountStatusProvider
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.RealCbcEnabledProvider
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module(
    subcomponents = [
        LinkAnalyticsComponent::class,
        LinkComponent::class,
    ],
)
internal abstract class PaymentSheetCommonModule {

    @Binds
    abstract fun bindsDurationProvider(impl: DefaultDurationProvider): DurationProvider

    @Binds
    abstract fun bindsEventReporter(eventReporter: DefaultEventReporter): EventReporter

    @Binds
    abstract fun bindsCustomerRepository(repository: CustomerApiRepository): CustomerRepository

    @Binds
    abstract fun bindsStripeIntentRepository(
        impl: RealElementsSessionRepository,
    ): ElementsSessionRepository

    @Binds
    abstract fun bindsPaymentSheetLoader(impl: DefaultPaymentSheetLoader): PaymentSheetLoader

    @Binds
    abstract fun bindsLinkAccountStatusProvider(
        impl: DefaultLinkAccountStatusProvider,
    ): LinkAccountStatusProvider

    @Binds
    abstract fun bindsCbcEnabledProvider(impl: RealCbcEnabledProvider): CbcEnabledProvider

    @Binds
    abstract fun bindsIntentConfirmationInterceptor(
        impl: DefaultIntentConfirmationInterceptor,
    ): IntentConfirmationInterceptor

    @Binds
    abstract fun bindsPaymentSheetUpdater(
        impl: DefaultPaymentSelectionUpdater,
    ): PaymentSelectionUpdater

    companion object {
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
        @Named(ENABLE_LOGGING)
        fun provideEnabledLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        @Singleton
        fun providePrefsRepositoryFactory(
            appContext: Context,
            @IOContext workContext: CoroutineContext
        ): (PaymentSheet.CustomerConfiguration?) -> PrefsRepository = { customerConfig ->
            DefaultPrefsRepository(
                appContext,
                customerConfig?.id,
                workContext
            )
        }

        @Provides
        @Singleton
        @Named(APP_NAME)
        fun provideAppName(
            appContext: Context,
        ): String {
            val application = appContext as Application
            return application.applicationInfo.loadLabel(application.packageManager).toString()
        }
    }
}
