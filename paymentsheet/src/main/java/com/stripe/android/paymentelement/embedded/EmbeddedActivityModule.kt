package com.stripe.android.paymentelement.embedded

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.common.spms.DefaultLinkFormElementFactory
import com.stripe.android.common.spms.DefaultLinkInlineSignupAvailability
import com.stripe.android.common.spms.DefaultSavedPaymentMethodLinkFormHelper
import com.stripe.android.common.spms.LinkFormElementFactory
import com.stripe.android.common.spms.LinkInlineSignupAvailability
import com.stripe.android.common.spms.SavedPaymentMethodLinkFormHelper
import com.stripe.android.common.taptoadd.DefaultTapToAddHelper
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.common.taptoadd.TapToAddMode
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.RealUserFacingLogger
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.checkoutSessionResponse
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.form.OnClickDelegateOverrideImpl
import com.stripe.android.paymentelement.embedded.form.OnClickOverrideDelegate
import com.stripe.android.paymentelement.embedded.manage.DefaultEmbeddedManageScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.DefaultEmbeddedUpdateScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.EmbeddedManageScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.EmbeddedSavedPaymentMethodMutatorFactory
import com.stripe.android.paymentelement.embedded.manage.EmbeddedUpdateScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.ManageLaunchSavedPaymentMethodSelector
import com.stripe.android.paymentelement.embedded.manage.ManageScreenSavedPaymentMethodSelector
import com.stripe.android.paymentelement.embedded.sheet.DEFAULT_CONTINUE_COORDINATOR
import com.stripe.android.paymentelement.embedded.sheet.DefaultEmbeddedFormScreenFactory
import com.stripe.android.paymentelement.embedded.sheet.DefaultEmbeddedSheetAnalytics
import com.stripe.android.paymentelement.embedded.sheet.DefaultSheetActivityConfirmationHelper
import com.stripe.android.paymentelement.embedded.sheet.DefaultSheetActivityContinueCoordinator
import com.stripe.android.paymentelement.embedded.sheet.DefaultSheetActivityRegistrar
import com.stripe.android.paymentelement.embedded.sheet.DefaultSheetActivityStateHolder
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedFormScreenFactory
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedInitialScreenFactory
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedNavigator
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetAnalytics
import com.stripe.android.paymentelement.embedded.sheet.FlowControllerEmbeddedSheetAnalytics
import com.stripe.android.paymentelement.embedded.sheet.FlowControllerSheetActivityContinueCoordinator
import com.stripe.android.paymentelement.embedded.sheet.FlowControllerSheetLinkLauncher
import com.stripe.android.paymentelement.embedded.sheet.InitialPaymentOptionsScreenFactory
import com.stripe.android.paymentelement.embedded.sheet.InitialPaymentOptionsScreensProvider
import com.stripe.android.paymentelement.embedded.sheet.SheetActivityConfirmationHelper
import com.stripe.android.paymentelement.embedded.sheet.SheetActivityContinueCoordinator
import com.stripe.android.paymentelement.embedded.sheet.SheetActivityRegistrar
import com.stripe.android.paymentelement.embedded.sheet.SheetActivityStateHolder
import com.stripe.android.paymentelement.embedded.sheet.SheetLinkPaymentMethodLauncher
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.addresselement.AUTOCOMPLETE_DEFAULT_COUNTRIES
import com.stripe.android.paymentsheet.addresselement.AutocompleteActivityLauncher
import com.stripe.android.paymentsheet.addresselement.AutocompleteAppearanceContext
import com.stripe.android.paymentsheet.addresselement.DefaultAutocompleteLauncher
import com.stripe.android.paymentsheet.addresselement.PaymentElementAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.addresselement.StripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.addresselement.analytics.DefaultAddressLauncherEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.createInlineAutocompletePlacesClient
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.NoOpPromotionsHelper
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.repositories.PrefetchedPaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import com.stripe.android.paymentsheet.verticalmode.DefaultSavedPaymentMethodConfirmInteractor
import com.stripe.android.paymentsheet.verticalmode.SavedPaymentMethodConfirmInteractor
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.DefaultIsPlacesAvailable
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Module
internal interface EmbeddedActivityModule {
    @Binds
    fun bindsEmbeddedManageScreenInteractorFactory(
        factory: DefaultEmbeddedManageScreenInteractorFactory
    ): EmbeddedManageScreenInteractorFactory

    @Binds
    fun bindsEmbeddedUpdateScreenInteractorFactory(
        factory: DefaultEmbeddedUpdateScreenInteractorFactory
    ): EmbeddedUpdateScreenInteractorFactory

    @Binds
    fun bindsEmbeddedFormScreenFactory(
        factory: DefaultEmbeddedFormScreenFactory
    ): EmbeddedFormScreenFactory

    @Binds
    fun bindsCardAccountRangeRepositoryFactory(
        defaultCardAccountRangeRepositoryFactory: DefaultCardAccountRangeRepositoryFactory
    ): CardAccountRangeRepository.Factory

    @Binds
    fun bindsUserFacingLogger(impl: RealUserFacingLogger): UserFacingLogger

    @Binds
    fun bindsSheetActivityStateHolder(helper: DefaultSheetActivityStateHolder): SheetActivityStateHolder

    @Binds
    fun bindsPrefsRepositoryFactory(factory: DefaultPrefsRepository.Factory): PrefsRepository.Factory

    @Binds
    fun bindsTapToAddHelperFactory(factory: DefaultTapToAddHelper.Factory): TapToAddHelper.Factory

    @Binds
    fun bindsSavedPaymentMethodLinkFormHelper(
        helper: DefaultSavedPaymentMethodLinkFormHelper
    ): SavedPaymentMethodLinkFormHelper

    @Binds
    fun bindsLinkInlineSignupAvailability(
        impl: DefaultLinkInlineSignupAvailability,
    ): LinkInlineSignupAvailability

    @Binds
    fun providesSheetActivityRegistrar(
        implementation: DefaultSheetActivityRegistrar
    ): SheetActivityRegistrar

    @Binds
    fun bindsConfirmationHelper(
        confirmationHelper: DefaultSheetActivityConfirmationHelper
    ): SheetActivityConfirmationHelper

    @Binds
    @Named(DEFAULT_CONTINUE_COORDINATOR)
    fun bindsDefaultContinueCoordinator(
        continueCoordinator: DefaultSheetActivityContinueCoordinator
    ): SheetActivityContinueCoordinator

    @Binds
    fun bindsSheetLinkPaymentMethodLauncher(
        launcher: FlowControllerSheetLinkLauncher
    ): SheetLinkPaymentMethodLauncher

    @Binds
    fun bindsInitialPaymentOptionsScreensProvider(
        factory: InitialPaymentOptionsScreenFactory
    ): InitialPaymentOptionsScreensProvider

    @Binds
    fun bindsAddressLauncherEventReporter(
        eventReporter: DefaultAddressLauncherEventReporter
    ): AddressLauncherEventReporter

    @Suppress("TooManyFunctions")
    companion object {
        @Provides
        fun providesContext(application: Application): Context {
            return application
        }

        @Provides
        fun provideEventReporterMode(launchMode: EmbeddedLaunchMode): EventReporter.Mode {
            return if (launchMode.isFlowController) {
                EventReporter.Mode.Custom
            } else {
                EventReporter.Mode.Embedded
            }
        }

        @Provides
        @Singleton
        fun provideEmbeddedSheetAnalytics(
            launchMode: EmbeddedLaunchMode,
            defaultAnalytics: Provider<DefaultEmbeddedSheetAnalytics>,
            flowControllerAnalytics: Provider<FlowControllerEmbeddedSheetAnalytics>,
        ): EmbeddedSheetAnalytics {
            return if (launchMode.isFlowController) {
                flowControllerAnalytics.get()
            } else {
                defaultAnalytics.get()
            }
        }

        @Provides
        @Singleton
        fun provideEmbeddedNavigator(
            initialScreenFactory: EmbeddedInitialScreenFactory,
            @ViewModelScope viewModelScope: CoroutineScope,
            analytics: EmbeddedSheetAnalytics,
        ): EmbeddedNavigator {
            return EmbeddedNavigator(
                coroutineScope = viewModelScope,
                analytics = analytics,
                initialBackStack = initialScreenFactory.create(),
            )
        }

        @Provides
        fun provideContinueCoordinator(
            launchMode: EmbeddedLaunchMode,
            @Named(DEFAULT_CONTINUE_COORDINATOR) defaultContinueCoordinator: Provider<SheetActivityContinueCoordinator>,
            flowControllerContinueCoordinator: Provider<FlowControllerSheetActivityContinueCoordinator>,
        ): SheetActivityContinueCoordinator {
            return if (launchMode.isFlowController) {
                flowControllerContinueCoordinator.get()
            } else {
                defaultContinueCoordinator.get()
            }
        }

        @Provides
        @Singleton
        fun provideManageScreenSavedPaymentMethodSelector(
            launchMode: EmbeddedLaunchMode,
            selectionHolder: EmbeddedSelectionHolder,
            manageLaunchSelector: Provider<ManageLaunchSavedPaymentMethodSelector>,
        ): ManageScreenSavedPaymentMethodSelector = when (launchMode) {
            is EmbeddedLaunchMode.Manage -> manageLaunchSelector.get()
            is EmbeddedLaunchMode.PaymentOptions,
            is EmbeddedLaunchMode.Form -> ImmediateSavedPaymentMethodSelector(selectionHolder)
        }

        @Provides
        @Singleton
        fun provideSavedPaymentMethodMutator(
            factory: EmbeddedSavedPaymentMethodMutatorFactory
        ): SavedPaymentMethodMutator {
            return factory.createSavedPaymentMethodMutator()
        }

        @Provides
        @Singleton
        fun providesLinkAccountHolder(savedStateHandle: SavedStateHandle): LinkAccountHolder {
            return LinkAccountHolder(savedStateHandle)
        }

        @Provides
        @Singleton
        fun provideConfirmationHandler(
            confirmationHandlerFactory: ConfirmationHandler.Factory,
            @ViewModelScope coroutineScope: CoroutineScope,
        ): ConfirmationHandler {
            return confirmationHandlerFactory.create(coroutineScope)
        }

        @Provides
        @Singleton
        fun providesTapToAddHelper(
            @ViewModelScope coroutineScope: CoroutineScope,
            configuration: EmbeddedPaymentElement.Configuration,
            tapToAddHelperFactory: TapToAddHelper.Factory,
            embeddedSelectionHolder: EmbeddedSelectionHolder,
            customerStateHolder: CustomerStateHolder,
            paymentMethodMetadata: PaymentMethodMetadata,
            @Named(STATUS_BAR_COLOR) statusBarColor: Int?,
        ): TapToAddHelper {
            return tapToAddHelperFactory.create(
                coroutineScope = coroutineScope,
                tapToAddMode = when (configuration.formSheetAction) {
                    EmbeddedPaymentElement.FormSheetAction.Continue -> TapToAddMode.Continue
                    EmbeddedPaymentElement.FormSheetAction.Confirm -> TapToAddMode.Complete
                },
                updateSelection = embeddedSelectionHolder::setSelection,
                customerStateHolder = customerStateHolder,
                linkSignupMode = stateFlowOf(paymentMethodMetadata.linkState?.signupMode),
                statusBarColor = statusBarColor,
            )
        }

        @Provides
        @Singleton
        fun provideOnClickOverrideDelegate(): OnClickOverrideDelegate = OnClickDelegateOverrideImpl()

        @Provides
        @Singleton
        fun provideAutocompleteLauncher(
            configuration: EmbeddedPaymentElement.Configuration,
        ): AutocompleteActivityLauncher {
            return DefaultAutocompleteLauncher(AutocompleteAppearanceContext.PaymentElement(configuration.appearance))
        }

        @Provides
        @Singleton
        fun providePaymentElementAutocompleteAddressInteractorFactory(
            context: Context,
            launchMode: EmbeddedLaunchMode,
            autocompleteLauncher: AutocompleteActivityLauncher,
            stripeAutocompleteRepository: StripeAutocompleteRepository,
            @ViewModelScope coroutineScope: CoroutineScope,
            paymentMethodMetadata: PaymentMethodMetadata,
            eventReporter: AddressLauncherEventReporter,
            errorReporter: ErrorReporter,
        ): PaymentElementAutocompleteAddressInteractor.Factory {
            val flowControllerOptions = launchMode.flowControllerOptions
            return if (flowControllerOptions != null) {
                // FlowController supports Google Places autocomplete in addition to Stripe-hosted inline autocomplete.
                val isPlacesAvailable = DefaultIsPlacesAvailable()()
                PaymentElementAutocompleteAddressInteractor.Factory(
                    launcher = autocompleteLauncher,
                    apiConfigurationProvider = { paymentMethodMetadata.apiConfiguration },
                    autocompleteConfig = AutocompleteAddressInteractor.Config(
                        googlePlacesApiKey = flowControllerOptions.googlePlacesApiKey,
                        autocompleteCountries = AUTOCOMPLETE_DEFAULT_COUNTRIES,
                        isPlacesAvailable = isPlacesAvailable,
                        isInlineAutocompleteEnabled = true,
                    ),
                    placesClient = createInlineAutocompletePlacesClient(
                        context = context,
                        googlePlacesApiKey = flowControllerOptions.googlePlacesApiKey,
                        errorReporter = errorReporter,
                        isPlacesAvailable = isPlacesAvailable,
                    ),
                    stripeAutocompleteRepository = stripeAutocompleteRepository,
                    coroutineScope = coroutineScope,
                    shouldUseAutocompleteProxyEndpointsProvider = {
                        paymentMethodMetadata.shouldUseAutocompleteProxyEndpoints
                    },
                    eventReporter = eventReporter,
                )
            } else {
                PaymentElementAutocompleteAddressInteractor.Factory(
                    // Embedded supports Stripe-hosted inline autocomplete, which does not launch an activity.
                    launcher = null,
                    apiConfigurationProvider = { paymentMethodMetadata.apiConfiguration },
                    autocompleteConfig = AutocompleteAddressInteractor.Config(
                        googlePlacesApiKey = null,
                        autocompleteCountries = AUTOCOMPLETE_DEFAULT_COUNTRIES,
                        isPlacesAvailable = false,
                        isInlineAutocompleteEnabled = true,
                    ),
                    placesClient = null,
                    stripeAutocompleteRepository = stripeAutocompleteRepository,
                    coroutineScope = coroutineScope,
                    shouldUseAutocompleteProxyEndpointsProvider = {
                        paymentMethodMetadata.shouldUseAutocompleteProxyEndpoints
                    },
                    eventReporter = eventReporter,
                )
            }
        }

        @Provides
        fun provideAutocompleteAddressInteractorFactory(
            factory: PaymentElementAutocompleteAddressInteractor.Factory,
        ): AutocompleteAddressInteractor.Factory {
            return factory
        }

        @Provides
        @Singleton
        fun provideStripeImageLoader(context: Context): StripeImageLoader {
            return DefaultStripeImageLoader(context)
        }

        @Provides
        fun providePaymentMethodMetadataFlow(
            paymentMethodMetadata: PaymentMethodMetadata
        ): StateFlow<PaymentMethodMetadata?> {
            return stateFlowOf(paymentMethodMetadata)
        }

        @Provides
        fun providesTapToAddLinkFormElementFactory(): LinkFormElementFactory {
            return DefaultLinkFormElementFactory
        }

        @Provides
        fun provideSavedPaymentMethodConfirmInteractorFactory(
            @ViewModelScope coroutineScope: CoroutineScope,
            paymentMethodMetadata: PaymentMethodMetadata,
            sheetActivityStateHolder: SheetActivityStateHolder,
            linkAccountHolder: LinkAccountHolder,
            savedPaymentMethodLinkFormHelper: SavedPaymentMethodLinkFormHelper,
        ): SavedPaymentMethodConfirmInteractor.Factory {
            return DefaultSavedPaymentMethodConfirmInteractor.Factory(
                paymentMethodMetadata = paymentMethodMetadata,
                savedPaymentMethodLinkFormHelper = savedPaymentMethodLinkFormHelper,
                processing = sheetActivityStateHolder.state.mapAsStateFlow {
                    it.isProcessing
                },
                linkAccountHolder = linkAccountHolder,
                coroutineScope = coroutineScope,
            )
        }

        @Provides
        fun providesPaymentMethodMessagePromotionHelper(
            promotions: List<PaymentMethodMessagePromotion>,
            eventReporter: EventReporter,
            paymentMethodMetadata: PaymentMethodMetadata,
        ): PaymentMethodMessagePromotionsHelper {
            return if (paymentMethodMetadata.checkoutSessionResponse != null) {
                NoOpPromotionsHelper()
            } else {
                PrefetchedPaymentMethodMessagePromotionsHelper(promotions, eventReporter)
            }
        }
    }
}

private class ImmediateSavedPaymentMethodSelector(
    private val selectionHolder: EmbeddedSelectionHolder,
) : ManageScreenSavedPaymentMethodSelector {
    override val selectionState: StateFlow<SavedPaymentMethodSelectionState> =
        stateFlowOf(SavedPaymentMethodSelectionState.Idle)

    override val checkoutSessionResponse: CheckoutSessionResponse? = null

    override suspend fun select(selection: PaymentSelection.Saved): Result<Unit> {
        selectionHolder.setSelection(selection)
        return Result.success(Unit)
    }

    override suspend fun syncBillingAfterEdit(
        original: PaymentMethod,
        updated: PaymentMethod,
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override fun clearError() = Unit
}
