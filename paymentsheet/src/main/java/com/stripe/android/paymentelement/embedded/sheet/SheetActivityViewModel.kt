package com.stripe.android.paymentelement.embedded.sheet

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.DefaultConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.asEmbeddedActivityArgs
import com.stripe.android.paymentsheet.asEmbeddedConfiguration
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SheetActivityViewModel(
    private val args: SheetActivityArgs,
    private val loaderComponent: SheetActivityLoaderComponent?,
    internal val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        when (args) {
            is SheetActivityArgs.Embedded -> _state.value = State.Ready(args.args)
            is SheetActivityArgs.PaymentOptions -> _state.value = State.Ready(args.args.asEmbeddedActivityArgs())
            is SheetActivityArgs.PaymentSheet -> loadPaymentSheet(args)
        }
    }

    private fun loadPaymentSheet(args: SheetActivityArgs.PaymentSheet) {
        val component = requireNotNull(loaderComponent)
        component.eventReporter.onInit()
        viewModelScope.launch {
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
                        _state.value = State.Ready(
                            EmbeddedActivityArgs(
                                paymentMethodMetadata = loadedState.paymentMethodMetadata,
                                configuration = args.args.config.asEmbeddedConfiguration(
                                    EmbeddedPaymentElement.FormSheetAction.Confirm
                                ),
                                paymentElementCallbackIdentifier = args.args.paymentElementCallbackIdentifier,
                                statusBarColor = args.args.statusBarColor,
                                selection = loadedState.paymentSelection,
                                previousNewSelections = Bundle(),
                                customerState = loadedState.customer,
                                promotions = component.promotionsHelper.getPromotions(),
                                launchMode = EmbeddedLaunchMode.Complete,
                            )
                        )
                    }
                },
                onFailure = { error -> _state.value = State.Failed(error) },
            )
        }
    }

    sealed interface State {
        data object Loading : State

        data class Ready(val args: EmbeddedActivityArgs) : State

        data class Failed(val error: Throwable) : State
    }

    class Factory(
        private val argsSupplier: () -> SheetActivityArgs,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val args = argsSupplier()
            val savedStateHandle = extras.createSavedStateHandle()
            val loaderComponent = (args as? SheetActivityArgs.PaymentSheet)?.let { paymentSheetArgs ->
                DaggerSheetActivityLoaderComponent.factory().build(
                    application = extras.requireApplication(),
                    savedStateHandle = savedStateHandle,
                    args = paymentSheetArgs.args,
                )
            }
            return SheetActivityViewModel(
                args = args,
                loaderComponent = loaderComponent,
                savedStateHandle = savedStateHandle,
            ) as T
        }
    }
}
