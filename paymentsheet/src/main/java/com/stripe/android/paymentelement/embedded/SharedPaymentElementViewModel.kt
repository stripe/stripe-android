package com.stripe.android.paymentelement.embedded

import android.content.Context
import android.content.res.Resources
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.utils.RealUserFacingLogger
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.RealLinkConfigurationCoordinator
import com.stripe.android.link.gate.DefaultLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.EmbeddedPaymentElement.ConfigureResult
import com.stripe.android.paymentelement.EmbeddedPaymentElement.PaymentOptionDisplayData
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.injection.ExtendedPaymentElementConfirmationModule
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import javax.inject.Inject
import javax.inject.Named
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
    private val customerStateHolder: CustomerStateHolder,
    private val embeddedSheetLauncherFactory: EmbeddedSheetLauncherFactory,
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
        return viewModelScope.async {
            configurationHandler.configure(
                intentConfiguration = intentConfiguration,
                configuration = configuration,
            ).fold(
                onSuccess = { state ->
                    handleLoadedState(
                        state = state,
                        intentConfiguration = intentConfiguration,
                        configuration = configuration,
                    )
                    ConfigureResult.Succeeded()
                },
                onFailure = { error ->
                    ConfigureResult.Failed(error)
                },
            )
        }.await()
    }

    private fun handleLoadedState(
        state: PaymentElementLoader.State,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: EmbeddedPaymentElement.Configuration,
    ) {
        confirmationStateHolder.state = EmbeddedConfirmationStateHolder.State(
            paymentMethodMetadata = state.paymentMethodMetadata,
            selection = state.paymentSelection,
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration
            ),
            configuration = configuration,
        )
        customerStateHolder.setCustomerState(state.customer)
        selectionHolder.set(
            selectionChooser.choose(
                paymentMethodMetadata = state.paymentMethodMetadata,
                paymentMethods = state.customer?.paymentMethods,
                previousSelection = selectionHolder.selection.value,
                newSelection = state.paymentSelection,
                newConfiguration = configuration.asCommonConfiguration(),
            )
        )
        embeddedContentHelper.dataLoaded(
            paymentMethodMetadata = state.paymentMethodMetadata,
            rowStyle = configuration.appearance.embeddedAppearance.style,
            embeddedViewDisplaysMandateText = configuration.embeddedViewDisplaysMandateText,
        )
    }

    fun clearPaymentOption() {
        selectionHolder.set(null)
    }

    fun initEmbeddedSheetLauncher(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        val launcher = embeddedSheetLauncherFactory.create(activityResultCaller, lifecycleOwner)
        embeddedContentHelper.setSheetLauncher(launcher)
    }

    fun clearEmbeddedSheetLauncher() {
        embeddedContentHelper.clearSheetLauncher()
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
        ExtendedPaymentElementConfirmationModule::class,
        EmbeddedCommonModule::class,
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
    fun bindsEmbeddedSheetLauncherFactory(
        factory: DefaultEmbeddedSheetLauncherFactory
    ): EmbeddedSheetLauncherFactory

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

    @Binds
    fun bindLinkGateFactory(linkGateFactory: DefaultLinkGate.Factory): LinkGate.Factory

    companion object {
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
