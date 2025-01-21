package com.stripe.android.paymentelement.embedded

import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.PaymentConfiguration
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.core.injection.CoreCommonModule
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
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.RealLinkConfigurationCoordinator
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.EmbeddedPaymentElement.ConfigureResult
import com.stripe.android.paymentelement.EmbeddedPaymentElement.PaymentOptionDisplayData
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.injection.ExtendedPaymentElementConfirmationModule
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.repositories.CustomerApiRepository
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.repositories.RealElementsSessionRepository
import com.stripe.android.paymentsheet.state.DefaultLinkAccountStatusProvider
import com.stripe.android.paymentsheet.state.DefaultPaymentElementLoader
import com.stripe.android.paymentsheet.state.LinkAccountStatusProvider
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

@Singleton
@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class SharedPaymentElementViewModel @Inject constructor(
    confirmationStateHolderFactory: EmbeddedConfirmationStateHolderFactory,
    confirmationHandlerFactory: ConfirmationHandler.Factory,
    @IOContext ioContext: CoroutineContext,
    private val configurationHandler: EmbeddedConfigurationHandler,
    private val paymentOptionDisplayDataFactory: PaymentOptionDisplayDataFactory,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val selectionChooser: EmbeddedSelectionChooser,
    embeddedContentHelperFactory: EmbeddedContentHelperFactory,
) : ViewModel() {
    private val _paymentOption: MutableStateFlow<PaymentOptionDisplayData?> = MutableStateFlow(null)
    val paymentOption: StateFlow<PaymentOptionDisplayData?> = _paymentOption.asStateFlow()

    val confirmationStateHolder = confirmationStateHolderFactory.create(viewModelScope)
    val confirmationHandler = confirmationHandlerFactory.create(viewModelScope + ioContext)

    private val embeddedContentHelper = embeddedContentHelperFactory.create(viewModelScope)
    val embeddedContent: StateFlow<EmbeddedContent?> = embeddedContentHelper.embeddedContent

    init {
        viewModelScope.launch {
            selectionHolder.selection.collect { selection ->
                val state = confirmationStateHolder.state
                if (state == null) {
                    _paymentOption.value = null
                } else {
                    _paymentOption.value = paymentOptionDisplayDataFactory.create(
                        selection = selection,
                        paymentMethodMetadata = state.paymentMethodMetadata,
                    )
                }
            }
        }
    }

    suspend fun configure(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: EmbeddedPaymentElement.Configuration,
    ): ConfigureResult {
        return configurationHandler.configure(
            intentConfiguration = intentConfiguration,
            configuration = configuration,
        ).fold(
            onSuccess = { state ->
                val previousConfiguration = confirmationStateHolder.state?.configuration
                confirmationStateHolder.state = EmbeddedConfirmationStateHolder.State(
                    paymentMethodMetadata = state.paymentMethodMetadata,
                    selection = state.paymentSelection,
                    initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(intentConfiguration),
                    configuration = configuration,
                )
                selectionHolder.set(
                    selectionChooser.choose(
                        paymentMethodMetadata = state.paymentMethodMetadata,
                        paymentMethods = state.customer?.paymentMethods,
                        previousSelection = selectionHolder.selection.value,
                        newSelection = state.paymentSelection,
                        previousConfiguration = previousConfiguration,
                        newConfiguration = configuration,
                    )
                )
                embeddedContentHelper.dataLoaded(
                    paymentMethodMetadata = state.paymentMethodMetadata,
                    rowStyle = configuration.appearance.embeddedAppearance.style,
                    embeddedViewDisplaysMandateText = configuration.embeddedViewDisplaysMandateText,
                )
                ConfigureResult.Succeeded()
            },
            onFailure = { error ->
                ConfigureResult.Failed(error)
            },
        )
    }

    fun clearPaymentOption() {
        selectionHolder.set(null)
    }

    class Factory(private val statusBarColor: Int?) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            val component = DaggerSharedPaymentElementViewModelComponent.builder()
                .savedStateHandle(extras.createSavedStateHandle())
                .context(extras.requireApplication())
                .statusBarColor(statusBarColor)
                .build()
            @Suppress("UNCHECKED_CAST")
            return component.viewModel as T
        }
    }
}

@ExperimentalEmbeddedPaymentElementApi
@Singleton
@Component(
    modules = [
        SharedPaymentElementViewModelModule::class,
        GooglePayLauncherModule::class,
        CoreCommonModule::class,
        StripeRepositoryModule::class,
        ExtendedPaymentElementConfirmationModule::class,
    ]
)
internal interface SharedPaymentElementViewModelComponent {
    val viewModel: SharedPaymentElementViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun savedStateHandle(savedStateHandle: SavedStateHandle): Builder

        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun statusBarColor(@Named(STATUS_BAR_COLOR) statusBarColor: Int?): Builder

        fun build(): SharedPaymentElementViewModelComponent
    }
}

@ExperimentalEmbeddedPaymentElementApi
@Module(
    subcomponents = [
        LinkAnalyticsComponent::class,
        LinkComponent::class,
    ],
)
internal interface SharedPaymentElementViewModelModule {
    @Binds
    fun bindsEmbeddedContentHelperFactory(
        factory: DefaultEmbeddedContentHelperFactory
    ): EmbeddedContentHelperFactory

    @Binds
    fun bindsCardAccountRangeRepositoryFactory(
        defaultCardAccountRangeRepositoryFactory: DefaultCardAccountRangeRepositoryFactory
    ): CardAccountRangeRepository.Factory

    @Binds
    fun bindsConfigurationHandler(
        handler: DefaultEmbeddedConfigurationHandler
    ): EmbeddedConfigurationHandler

    @Binds
    fun bindsWalletsHelper(helper: DefaultEmbeddedWalletsHelper): EmbeddedWalletsHelper

    @Singleton
    @Binds
    fun bindsEventReporter(eventReporter: DefaultEventReporter): EventReporter

    @Binds
    fun bindsCustomerRepository(repository: CustomerApiRepository): CustomerRepository

    @Binds
    fun bindsErrorReporter(errorReporter: RealErrorReporter): ErrorReporter

    @Binds
    fun bindsElementsSessionRepository(impl: RealElementsSessionRepository): ElementsSessionRepository

    @Binds
    fun bindPaymentElementLoader(loader: DefaultPaymentElementLoader): PaymentElementLoader

    @Binds
    fun bindSelectionChooser(chooser: DefaultEmbeddedSelectionChooser): EmbeddedSelectionChooser

    @Binds
    fun bindsUserFacingLogger(impl: RealUserFacingLogger): UserFacingLogger

    @Binds
    fun bindsLinkAccountStatusProvider(
        impl: DefaultLinkAccountStatusProvider,
    ): LinkAccountStatusProvider

    @Binds
    fun bindsLinkConfigurationCoordinator(impl: RealLinkConfigurationCoordinator): LinkConfigurationCoordinator

    @Suppress("TooManyFunctions")
    companion object {
        @Provides
        @Singleton
        @Named(ALLOWS_MANUAL_CONFIRMATION)
        fun provideAllowsManualConfirmation() = true

        @Provides
        fun provideEventReporterMode(): EventReporter.Mode {
            return EventReporter.Mode.Embedded
        }

        @Provides
        @Named(PRODUCT_USAGE)
        fun provideProductUsageTokens() = setOf("EmbeddedPaymentElement")

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
        @Named(ENABLE_LOGGING)
        fun provideEnabledLogging(): Boolean = BuildConfig.DEBUG

        @Provides
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
        @IOContext
        fun ioContext(): CoroutineContext {
            return Dispatchers.IO
        }

        @Provides
        fun provideResources(context: Context): Resources {
            return context.resources
        }

        @Provides
        @Singleton
        fun provideStripeImageLoader(context: Context): StripeImageLoader {
            return StripeImageLoader(context)
        }
    }
}
