package com.stripe.android.checkout.injection

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.PaymentConfiguration
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.common.di.ElementsSessionClientParamsModule
import com.stripe.android.common.nfcscan.NfcScanningAvailabilityModule
import com.stripe.android.common.taptoadd.TapToAddConnectionModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.StripeNetworkClientModule
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.RealUserFacingLogger
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.RealLinkConfigurationCoordinator
import com.stripe.android.link.account.DefaultLinkStore
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.gate.DefaultLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.link.injection.LinkCommonModule
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.injection.PaymentsIntegrityModule
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilterFactory
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.injection.ExtendedPaymentElementConfirmationModule
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.PaymentConfigurationModule
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentOptionCardArtDrawableLoader
import com.stripe.android.paymentsheet.PaymentOptionCardArtModule
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.LoadingEventReporter
import com.stripe.android.paymentsheet.injection.LinkHoldbackExposureModule
import com.stripe.android.paymentsheet.injection.PaymentMethodMessagePromotionsExperimentHandlerModule
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CustomerApiRepository
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.DefaultSavedPaymentMethodRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelperModule
import com.stripe.android.paymentsheet.repositories.RealElementsSessionRepository
import com.stripe.android.paymentsheet.repositories.SavedPaymentMethodRepository
import com.stripe.android.paymentsheet.state.CreateLinkState
import com.stripe.android.paymentsheet.state.DefaultAnalyticsMetadataFactory
import com.stripe.android.paymentsheet.state.DefaultCreateLinkState
import com.stripe.android.paymentsheet.state.DefaultLinkAccountStatusProvider
import com.stripe.android.paymentsheet.state.DefaultPaymentElementLoader
import com.stripe.android.paymentsheet.state.DefaultPaymentMethodFilter
import com.stripe.android.paymentsheet.state.DefaultRetrieveCustomerEmail
import com.stripe.android.paymentsheet.state.DefaultTapToAddAvailabilityFactory
import com.stripe.android.paymentsheet.state.LinkAccountStatusProvider
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentMethodFilter
import com.stripe.android.paymentsheet.state.RetrieveCustomerEmail
import com.stripe.android.paymentsheet.state.TapToAddAvailabilityFactory
import com.stripe.android.paymentsheet.state.TapToAddConnectionStarterModule
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CheckoutControllerModule::class,
        ElementsSessionClientParamsModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
        StripeRepositoryModule::class,
        PaymentElementRequestSurfaceModule::class,
        ExtendedPaymentElementConfirmationModule::class,
        GooglePayLauncherModule::class,
        PaymentOptionCardArtModule::class,
        PaymentsIntegrityModule::class,
        TapToAddConnectionModule::class,
        NfcScanningAvailabilityModule::class,
        LinkHoldbackExposureModule::class,
        CheckoutControllerLinkModule::class,
        PaymentMethodMessagePromotionsHelperModule::class,
        PaymentMethodMessagePromotionsExperimentHandlerModule::class,
        TapToAddConnectionStarterModule::class,
    ],
)
internal interface CheckoutControllerComponent {
    val checkoutSessionRepository: CheckoutSessionRepository
    val analyticsRequestExecutor: AnalyticsRequestExecutor
    val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
    val confirmationHandlerFactory: ConfirmationHandler.Factory
    val paymentElementLoader: PaymentElementLoader
    val iconLoader: PaymentSelection.IconLoader
    val cardArtDrawableLoader: PaymentOptionCardArtDrawableLoader
    val presenterSubcomponentFactory: CheckoutPresenterSubcomponent.Factory

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application,
            @BindsInstance savedStateHandle: SavedStateHandle,
            @BindsInstance @ViewModelScope coroutineScope: CoroutineScope,
        ): CheckoutControllerComponent
    }
}

@Module(
    includes = [
        PaymentConfigurationModule::class,
        StripeNetworkClientModule::class,
    ],
    subcomponents = [
        CheckoutPresenterSubcomponent::class,
    ],
)
@Suppress("TooManyFunctions")
internal interface CheckoutControllerModule {
    @Binds
    fun bindsElementsSessionRepository(impl: RealElementsSessionRepository): ElementsSessionRepository

    @Binds
    fun bindPaymentElementLoader(loader: DefaultPaymentElementLoader): PaymentElementLoader

    @Binds
    fun bindsPaymentMethodFilter(impl: DefaultPaymentMethodFilter): PaymentMethodFilter

    @Binds
    fun bindAnalyticsMetadataFactory(
        implementation: DefaultAnalyticsMetadataFactory
    ): DefaultPaymentElementLoader.AnalyticsMetadataFactory

    @Binds
    fun bindsCreateLinkState(impl: DefaultCreateLinkState): CreateLinkState

    @Binds
    fun bindRetrieveCustomerEmail(
        retrieveCustomerEmail: DefaultRetrieveCustomerEmail
    ): RetrieveCustomerEmail

    @Binds
    fun bindsLinkAccountStatusProvider(
        impl: DefaultLinkAccountStatusProvider,
    ): LinkAccountStatusProvider

    @Binds
    fun bindsTapToAddAvailabilityFactory(
        impl: DefaultTapToAddAvailabilityFactory
    ): TapToAddAvailabilityFactory

    @Binds
    fun bindsErrorReporter(errorReporter: RealErrorReporter): ErrorReporter

    @Binds
    fun bindsCustomerRepository(repository: CustomerApiRepository): CustomerRepository

    @Binds
    fun bindsSavedPaymentMethodRepository(
        repository: DefaultSavedPaymentMethodRepository,
    ): SavedPaymentMethodRepository

    @Binds
    fun bindsPaymentAnalyticsRequestFactory(
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory

    @Binds
    fun bindsCardAccountRangeRepositoryFactory(
        defaultCardAccountRangeRepositoryFactory: DefaultCardAccountRangeRepositoryFactory
    ): CardAccountRangeRepository.Factory

    @Binds
    fun bindsUserFacingLogger(impl: RealUserFacingLogger): UserFacingLogger

    @Binds
    @Singleton
    fun bindsEventReporter(eventReporter: DefaultEventReporter): EventReporter

    @Binds
    @Singleton
    fun bindsLoadingReporter(eventReporter: DefaultEventReporter): LoadingEventReporter

    @Binds
    fun bindsPrefsRepositoryFactory(
        factory: DefaultPrefsRepository.Factory
    ): PrefsRepository.Factory

    companion object {
        @Provides
        fun provideContext(application: Application): Context = application.applicationContext

        @Provides
        fun provideResources(context: Context): Resources = context.resources

        @Provides
        @Named(ENABLE_LOGGING)
        fun provideEnabledLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        @Named(PRODUCT_USAGE)
        fun provideProductUsageTokens(): Set<String> = setOf("CheckoutController")

        @Provides
        fun provideApiRequestOptions(
            paymentConfiguration: Provider<PaymentConfiguration>
        ): ApiRequest.Options = ApiRequest.Options(
            apiKey = paymentConfiguration.get().publishableKey,
            stripeAccount = paymentConfiguration.get().stripeAccountId,
        )

        @Provides
        @Singleton
        fun provideStripeImageLoader(context: Context): StripeImageLoader {
            return DefaultStripeImageLoader(context)
        }

        @Provides
        @Singleton
        @Named(ALLOWS_MANUAL_CONFIRMATION)
        fun provideAllowsManualConfirmation(): Boolean = false

        @Provides
        fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

        @Provides
        fun provideDurationProvider(): DurationProvider = DefaultDurationProvider.instance

        @Provides
        @Singleton
        fun providesLinkAccountHolder(savedStateHandle: SavedStateHandle): LinkAccountHolder {
            return LinkAccountHolder(savedStateHandle)
        }

        @Provides
        @PaymentElementCallbackIdentifier
        fun providePaymentElementCallbackIdentifier(): String = "CheckoutController"

        @Provides
        fun providesSelectedPaymentMethodCode(): String = ""

        @Provides
        fun providePaymentMethodMetadata(): PaymentMethodMetadata? = null

        @OptIn(ExperimentalAnalyticEventCallbackApi::class)
        @Provides
        fun provideAnalyticEventCallback(): AnalyticEventCallback? = null

        @Provides
        @Named(STATUS_BAR_COLOR)
        fun provideStatusBarColor(): Int? = null
    }
}

@Module(
    subcomponents = [
        LinkAnalyticsComponent::class,
        LinkComponent::class,
    ],
    includes = [
        LinkCommonModule::class,
    ]
)
internal interface CheckoutControllerLinkModule {
    @Binds
    fun bindsLinkStore(impl: DefaultLinkStore): LinkStore

    @Binds
    fun bindLinkGateFactory(linkGateFactory: DefaultLinkGate.Factory): LinkGate.Factory

    @Binds
    fun bindCardFundingFilter(
        cardFundingFilterFactory: PaymentSheetCardFundingFilter.Factory
    ): PaymentSheetCardFundingFilterFactory

    @Binds
    fun bindsLinkConfigurationCoordinator(impl: RealLinkConfigurationCoordinator): LinkConfigurationCoordinator
}
