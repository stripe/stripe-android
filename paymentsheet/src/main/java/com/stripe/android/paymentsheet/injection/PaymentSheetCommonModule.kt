package com.stripe.android.paymentsheet.injection

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.common.taptoadd.DefaultTapToAddHelper
import com.stripe.android.common.taptoadd.TapToAddConnectionModule
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.networking.AnalyticsRequestFactory
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
import com.stripe.android.link.injection.LinkCommonModule
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.injection.PaymentsIntegrityModule
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilterFactory
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import com.stripe.android.payments.core.injection.PaymentConfigurationModule
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.LoadingEventReporter
import com.stripe.android.paymentsheet.flowcontroller.DefaultPaymentSelectionUpdater
import com.stripe.android.paymentsheet.flowcontroller.PaymentSelectionUpdater
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionInteractor
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.DefaultCvcRecollectionInteractor
import com.stripe.android.paymentsheet.repositories.CustomerApiRepository
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.DefaultSavedPaymentMethodRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.repositories.RealElementsSessionRepository
import com.stripe.android.paymentsheet.repositories.SavedPaymentMethodRepository
import com.stripe.android.paymentsheet.state.CreateLinkState
import com.stripe.android.paymentsheet.state.DefaultAnalyticsMetadataFactory
import com.stripe.android.paymentsheet.state.DefaultCreateLinkState
import com.stripe.android.paymentsheet.state.DefaultLinkAccountStatusProvider
import com.stripe.android.paymentsheet.state.DefaultLoadSession
import com.stripe.android.paymentsheet.state.DefaultPaymentElementLoader
import com.stripe.android.paymentsheet.state.DefaultPaymentMethodFilter
import com.stripe.android.paymentsheet.state.DefaultRetrieveCustomerEmail
import com.stripe.android.paymentsheet.state.LinkAccountStatusProvider
import com.stripe.android.paymentsheet.state.LoadSession
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentMethodFilter
import com.stripe.android.paymentsheet.state.RetrieveCustomerEmail
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@SuppressWarnings("TooManyFunctions")
@Module(
    subcomponents = [
        LinkAnalyticsComponent::class,
        LinkComponent::class,
    ],
    includes = [
        LinkCommonModule::class,
        TapToAddConnectionModule::class,
        PaymentsIntegrityModule::class,
        PaymentConfigurationModule::class,
    ]
)
internal abstract class PaymentSheetCommonModule {

    @Singleton
    @Binds
    abstract fun bindsEventReporter(eventReporter: DefaultEventReporter): EventReporter

    @Binds
    @Singleton
    abstract fun bindsLoadingReporter(eventReporter: DefaultEventReporter): LoadingEventReporter

    @Binds
    abstract fun bindsCustomerRepository(repository: CustomerApiRepository): CustomerRepository

    @Binds
    abstract fun bindsSavedPaymentMethodRepository(
        repository: DefaultSavedPaymentMethodRepository,
    ): SavedPaymentMethodRepository

    @Binds
    abstract fun bindsErrorReporter(errorReporter: RealErrorReporter): ErrorReporter

    @Binds
    abstract fun bindsStripeIntentRepository(
        impl: RealElementsSessionRepository,
    ): ElementsSessionRepository

    @Binds
    abstract fun bindsPaymentSheetLoader(impl: DefaultPaymentElementLoader): PaymentElementLoader

    @Binds
    abstract fun bindsPaymentMethodFilter(impl: DefaultPaymentMethodFilter): PaymentMethodFilter

    @Binds
    abstract fun bindAnalyticsMetadataFactory(
        implementation: DefaultAnalyticsMetadataFactory
    ): DefaultPaymentElementLoader.AnalyticsMetadataFactory

    @Binds
    abstract fun bindsRetrieveCustomerEmail(
        impl: DefaultRetrieveCustomerEmail,
    ): RetrieveCustomerEmail

    @Binds
    abstract fun bindsUserFacingLogger(impl: RealUserFacingLogger): UserFacingLogger

    @Binds
    abstract fun bindsLinkAccountStatusProvider(
        impl: DefaultLinkAccountStatusProvider,
    ): LinkAccountStatusProvider

    @Binds
    abstract fun bindsCreateLinkState(
        impl: DefaultCreateLinkState,
    ): CreateLinkState

    @Binds
    abstract fun bindsLoadSession(
        impl: DefaultLoadSession,
    ): LoadSession

    @Binds
    abstract fun bindsPaymentSheetUpdater(
        impl: DefaultPaymentSelectionUpdater,
    ): PaymentSelectionUpdater

    @Binds
    abstract fun bindsLinkConfigurationCoordinator(impl: RealLinkConfigurationCoordinator): LinkConfigurationCoordinator

    @Binds
    abstract fun bindsAnalyticsRequestFactory(
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory

    @Binds
    abstract fun bindLinkGateFactory(linkGateFactory: DefaultLinkGate.Factory): LinkGate.Factory

    @Binds
    abstract fun bindCardFundingFilter(
        cardFundingFilterFactory: PaymentSheetCardFundingFilter.Factory
    ): PaymentSheetCardFundingFilterFactory

    @Binds
    abstract fun bindsCardAccountRangeRepositoryFactory(
        defaultCardAccountRangeRepositoryFactory: DefaultCardAccountRangeRepositoryFactory
    ): CardAccountRangeRepository.Factory

    @Binds
    abstract fun bindsPrefsRepositoryFactory(
        factory: DefaultPrefsRepository.Factory
    ): PrefsRepository.Factory

    @Binds
    abstract fun bindsTapToAddHelperFactory(factory: DefaultTapToAddHelper.Factory): TapToAddHelper.Factory

    @Suppress("TooManyFunctions")
    companion object {
        @Provides
        @Singleton
        fun providesLinkAccountHolder(savedStateHandle: SavedStateHandle): LinkAccountHolder {
            return LinkAccountHolder(savedStateHandle)
        }

        @Provides
        fun provideCustomerStateHolderFactory(): CustomerStateHolder.Factory {
            return DefaultCustomerStateHolder.Factory
        }

        @Provides
        @Singleton
        @Named(ENABLE_LOGGING)
        fun provideEnabledLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        @Singleton
        fun provideDurationProvider(): DurationProvider {
            return DefaultDurationProvider.instance
        }

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
