package com.stripe.android.paymentsheet.injection

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.PaymentConfiguration
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.NetworkTypeDetector
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.RealUserFacingLogger
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.RealLinkConfigurationCoordinator
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.gate.DefaultLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.flowcontroller.DefaultPaymentSelectionUpdater
import com.stripe.android.paymentsheet.flowcontroller.PaymentSelectionUpdater
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionInteractor
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.DefaultCvcRecollectionInteractor
import com.stripe.android.paymentsheet.repositories.CustomerApiRepository
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.repositories.RealElementsSessionRepository
import com.stripe.android.paymentsheet.state.DefaultLinkAccountStatusProvider
import com.stripe.android.paymentsheet.state.DefaultPaymentElementLoader
import com.stripe.android.paymentsheet.state.LinkAccountStatusProvider
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@SuppressWarnings("TooManyFunctions")
@Module(
    subcomponents = [
        LinkAnalyticsComponent::class,
        LinkComponent::class,
    ],
)
internal abstract class PaymentSheetCommonModule {

    @Singleton
    @Binds
    abstract fun bindsEventReporter(eventReporter: DefaultEventReporter): EventReporter

    @Binds
    abstract fun bindsCustomerRepository(repository: CustomerApiRepository): CustomerRepository

    @Binds
    abstract fun bindsErrorReporter(errorReporter: RealErrorReporter): ErrorReporter

    @Binds
    abstract fun bindsStripeIntentRepository(
        impl: RealElementsSessionRepository,
    ): ElementsSessionRepository

    @Binds
    abstract fun bindsPaymentSheetLoader(impl: DefaultPaymentElementLoader): PaymentElementLoader

    @Binds
    abstract fun bindsUserFacingLogger(impl: RealUserFacingLogger): UserFacingLogger

    @Binds
    abstract fun bindsLinkAccountStatusProvider(
        impl: DefaultLinkAccountStatusProvider,
    ): LinkAccountStatusProvider

    @Binds
    abstract fun bindsPaymentSheetUpdater(
        impl: DefaultPaymentSelectionUpdater,
    ): PaymentSelectionUpdater

    @Binds
    abstract fun bindsLinkConfigurationCoordinator(impl: RealLinkConfigurationCoordinator): LinkConfigurationCoordinator

    @Binds
    abstract fun bindLinkGateFactory(linkGateFactory: DefaultLinkGate.Factory): LinkGate.Factory

    @Binds
    abstract fun bindsCardAccountRangeRepositoryFactory(
        defaultCardAccountRangeRepositoryFactory: DefaultCardAccountRangeRepositoryFactory
    ): CardAccountRangeRepository.Factory

    @Suppress("TooManyFunctions")
    companion object {
        @Provides
        @Singleton
        fun providesLinkAccountHolder(savedStateHandle: SavedStateHandle): LinkAccountHolder {
            return LinkAccountHolder(savedStateHandle)
        }

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
        fun providesCvcRecollectionInteractorFactory(): CvcRecollectionInteractor.Factory {
            return DefaultCvcRecollectionInteractor.Factory
        }

        @OptIn(ExperimentalAnalyticEventCallbackApi::class)
        @Provides
        fun providesAnalyticEventCallback(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
        ): AnalyticEventCallback? {
            return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]?.analyticEventCallback
        }
    }
}
