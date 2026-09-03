package com.stripe.android.paymentelement.embedded.sheet

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.DefaultConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.asEmbeddedActivityArgs
import com.stripe.android.paymentsheet.asEmbeddedConfiguration
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class EmbeddedSheetViewModel(
    private val args: SheetActivityArgs,
    private val loaderComponent: SheetActivityLoaderComponent?,
    private val savedStateHandle: SavedStateHandle,
    @ViewModelScope private val customViewModelScope: CoroutineScope,
    private val componentFactory: (EmbeddedActivityArgs) -> EmbeddedSheetComponent,
) : ViewModel() {
    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        when (args) {
            is SheetActivityArgs.Embedded -> createEmbeddedSheet(args.args)
            is SheetActivityArgs.PaymentOptions -> createEmbeddedSheet(args.args.asEmbeddedActivityArgs())
            is SheetActivityArgs.PaymentSheet -> loadPaymentSheet(args)
        }
    }

    private fun loadPaymentSheet(args: SheetActivityArgs.PaymentSheet) {
        val component = requireNotNull(loaderComponent)
        component.eventReporter.onInit()
        customViewModelScope.launch {
            component.paymentElementLoader.load(
                initializationMode = args.args.initializationMode,
                integrationConfiguration = PaymentElementLoader.Configuration.PaymentSheet(args.args.config),
                metadata = PaymentElementLoader.Metadata(
                    isReloadingAfterProcessDeath = DefaultConfirmationHandler.wasAwaitingResult(savedStateHandle),
                    initializedViaCompose = args.args.initializedViaCompose,
                ),
            ).fold(
                onSuccess = { loadedState ->
                    val validationError = loadedState.validationError
                    if (validationError != null) {
                        _state.value = State.Failed(validationError)
                    } else {
                        createEmbeddedSheet(
                            EmbeddedActivityArgs(
                                paymentMethodMetadata = loadedState.paymentMethodMetadata,
                                configuration = args.args.config.asEmbeddedConfiguration(
                                    EmbeddedPaymentElement.FormSheetAction.Confirm
                                ),
                                productUsage = EmbeddedActivityArgs.ActivityConfiguration.PaymentSheet.productUsage,
                                paymentElementCallbackIdentifier = args.args.paymentElementCallbackIdentifier,
                                statusBarColor = args.args.statusBarColor,
                                selection = loadedState.paymentSelection,
                                previousNewSelections = Bundle(),
                                customerState = loadedState.customer,
                                promotions = component.promotionsHelper.getPromotions().orEmpty(),
                                launchMode = EmbeddedLaunchMode.Complete,
                                activityConfiguration = EmbeddedActivityArgs.ActivityConfiguration.PaymentSheet,
                            )
                        )
                    }
                },
                onFailure = { error -> _state.value = State.Failed(error) },
            )
        }
    }

    private fun createEmbeddedSheet(args: EmbeddedActivityArgs) {
        val component = componentFactory(args)

        component.customerStateHolder.setCustomerState(args.customerState)
        component.selectionHolder.setPreviousNewSelections(args.previousNewSelections)
        component.selectionHolder.setSelection(args.selection)

        _state.value = State.Ready(args, component)
    }

    override fun onCleared() {
        customViewModelScope.cancel()
    }

    sealed interface State {
        data object Loading : State

        data class Ready(
            val args: EmbeddedActivityArgs,
            val component: EmbeddedSheetComponent,
        ) : State

        data class Failed(val error: Throwable) : State
    }

    class Factory(
        private val argsSupplier: () -> SheetActivityArgs,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()
            val customViewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            val args = argsSupplier()
            val componentFactory = { embeddedArgs: EmbeddedActivityArgs ->
                DaggerEmbeddedSheetComponent.factory().build(
                    paymentMethodMetadata = embeddedArgs.paymentMethodMetadata,
                    statusBarColor = embeddedArgs.statusBarColor,
                    configuration = embeddedArgs.configuration,
                    productUsage = embeddedArgs.productUsage,
                    paymentElementCallbackIdentifier = embeddedArgs.paymentElementCallbackIdentifier,
                    application = application,
                    savedStateHandle = savedStateHandle,
                    promotions = embeddedArgs.promotions,
                    launchMode = embeddedArgs.launchMode,
                    activityConfiguration = embeddedArgs.activityConfiguration,
                    viewModelScope = customViewModelScope,
                )
            }
            val loaderComponent = (args as? SheetActivityArgs.PaymentSheet)?.let { paymentSheetArgs ->
                DaggerSheetActivityLoaderComponent.factory().build(
                    application = application,
                    savedStateHandle = savedStateHandle,
                    args = paymentSheetArgs.args,
                    viewModelScope = customViewModelScope,
                )
            }
            return EmbeddedSheetViewModel(
                args = args,
                loaderComponent = loaderComponent,
                savedStateHandle = savedStateHandle,
                customViewModelScope = customViewModelScope,
                componentFactory = componentFactory,
            )
                as T
        }
    }
}
