@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.injection

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.ece.DefaultExpressCheckoutElementInteractor
import com.stripe.android.checkout.ece.ExpressCheckoutElementInteractor
import com.stripe.android.common.di.ElementsSessionClientParamsModule
import com.stripe.android.common.nfcscan.NfcScanningAvailabilityModule
import com.stripe.android.common.taptoadd.TapToAddConnectionModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.RealUserFacingLogger
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.injection.PaymentsIntegrityModule
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.paymentelement.embedded.EmbeddedLinkExtrasModule
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedSelectionChooser
import com.stripe.android.paymentelement.embedded.content.EmbeddedSelectionChooser
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentOptionCardArtModule
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.LoadingEventReporter
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandler
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandlerImpl
import com.stripe.android.paymentsheet.injection.LinkHoldbackExposureModule
import com.stripe.android.paymentsheet.injection.PaymentMethodMessagePromotionsExperimentHandlerModule
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
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CheckoutControllerModule::class,
        CheckoutModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
        ElementsSessionClientParamsModule::class,
        StripeRepositoryModule::class,
        GooglePayLauncherModule::class,
        TapToAddConnectionStarterModule::class,
        TapToAddConnectionModule::class,
        PaymentsIntegrityModule::class,
        PaymentElementRequestSurfaceModule::class,
        EmbeddedLinkExtrasModule::class,
        LinkHoldbackExposureModule::class,
        PaymentMethodMessagePromotionsHelperModule::class,
        PaymentMethodMessagePromotionsExperimentHandlerModule::class,
        NfcScanningAvailabilityModule::class,
        PaymentOptionCardArtModule::class,
    ],
)
internal interface CheckoutControllerComponent {
    val checkoutController: CheckoutController

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application,
            @BindsInstance savedStateHandle: SavedStateHandle,
            @BindsInstance resultCallback: CheckoutController.ResultCallback,
        ): CheckoutControllerComponent
    }
}

@Suppress("TooManyFunctions")
@Module(subcomponents = [CheckoutPresenterSubcomponent::class])
internal interface CheckoutControllerModule {
    @Binds
    fun bindPaymentElementLoader(loader: DefaultPaymentElementLoader): PaymentElementLoader

    @Binds
    fun bindsElementsSessionRepository(impl: RealElementsSessionRepository): ElementsSessionRepository

    @Binds
    fun bindsTapToAddAvailabilityFactory(impl: DefaultTapToAddAvailabilityFactory): TapToAddAvailabilityFactory

    @Binds
    fun bindsPaymentMethodFilter(impl: DefaultPaymentMethodFilter): PaymentMethodFilter

    @Binds
    fun bindAnalyticsMetadataFactory(
        implementation: DefaultAnalyticsMetadataFactory
    ): DefaultPaymentElementLoader.AnalyticsMetadataFactory

    @Binds
    fun bindsCreateLinkState(impl: DefaultCreateLinkState): CreateLinkState

    @Binds
    fun bindRetrieveCustomerEmail(impl: DefaultRetrieveCustomerEmail): RetrieveCustomerEmail

    @Binds
    fun bindsUserFacingLogger(impl: RealUserFacingLogger): UserFacingLogger

    @Binds
    fun bindsLinkAccountStatusProvider(impl: DefaultLinkAccountStatusProvider): LinkAccountStatusProvider

    @Binds
    fun bindsCardAccountRangeRepositoryFactory(
        factory: DefaultCardAccountRangeRepositoryFactory
    ): CardAccountRangeRepository.Factory

    @Binds
    fun bindsPrefsRepositoryFactory(factory: DefaultPrefsRepository.Factory): PrefsRepository.Factory

    @Binds
    @Singleton
    fun bindsEventReporter(eventReporter: DefaultEventReporter): EventReporter

    @Binds
    @Singleton
    fun bindsLoadingReporter(eventReporter: DefaultEventReporter): LoadingEventReporter

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
        factory: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory

    @Binds
    fun bindsEmbeddedSelectionChooser(impl: DefaultEmbeddedSelectionChooser): EmbeddedSelectionChooser

    @Binds
    fun bindsEmbeddedSelectionHolder(impl: CheckoutControllerStateHolder): EmbeddedSelectionHolder

    @Binds
    fun bindExpressCheckoutElementInteractor(
        impl: DefaultExpressCheckoutElementInteractor
    ): ExpressCheckoutElementInteractor

    companion object {
        private const val CALLBACK_IDENTIFIER_KEY = "CheckoutController_CallbackIdentifier"

        @Provides
        @Singleton
        @PaymentElementCallbackIdentifier
        fun providePaymentElementCallbackIdentifier(savedStateHandle: SavedStateHandle): String {
            return savedStateHandle.get<String>(CALLBACK_IDENTIFIER_KEY)
                ?: UUID.randomUUID().toString().also { savedStateHandle[CALLBACK_IDENTIFIER_KEY] = it }
        }

        @Provides
        @Singleton
        fun providesLinkAccountHolder(savedStateHandle: SavedStateHandle): LinkAccountHolder {
            return LinkAccountHolder(savedStateHandle)
        }

        @Provides
        @Singleton
        @ViewModelScope
        fun provideViewModelScope(): CoroutineScope {
            return CoroutineScope(Dispatchers.Main)
        }

        @Provides
        fun provideDurationProvider(): DurationProvider {
            return DefaultDurationProvider.instance
        }

        @Provides
        @MerchantDisplayName
        fun provideMerchantDisplayName(application: Application): String {
            return application.applicationInfo.loadLabel(application.packageManager).toString()
        }

        @Provides
        fun providesInternalRowSelectionCallback(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
        ): InternalRowSelectionCallback? {
            return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]?.rowSelectionCallback
        }

        @Provides
        @Singleton
        @Named(ALLOWS_MANUAL_CONFIRMATION)
        fun provideAllowsManualConfirmation(): Boolean = true

        @Provides
        fun provideEventReporterMode(): EventReporter.Mode {
            return EventReporter.Mode.Embedded
        }

        @Provides
        @Singleton
        fun provideCvcRecollectionHandler(): CvcRecollectionHandler {
            return CvcRecollectionHandlerImpl()
        }

        @Provides
        fun providePaymentMethodMetadata(
            stateHolder: CheckoutControllerStateHolder,
        ): PaymentMethodMetadata? {
            return stateHolder.state?.paymentMethodMetadata
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
