package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.paymentelement.confirmation.DefaultConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SheetActivityViewModel(
    private val args: EmbeddedActivityArgs,
    private val loaderComponent: SheetActivityLoaderComponent,
    private val savedStateHandle: SavedStateHandle,
    @ViewModelScope private val customViewModelScope: CoroutineScope,
) : ViewModel() {
    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        loadPaymentSheet()
    }

    private fun loadPaymentSheet() {
        check(args.launchMode is EmbeddedLaunchMode.Complete)
        check(args.presentationState == EmbeddedActivityArgs.PresentationState.Loading)
        val activityConfiguration =
            args.activityConfiguration as EmbeddedActivityArgs.ActivityConfiguration.PaymentSheet
        val paymentSheetArgs = activityConfiguration.args
        loaderComponent.eventReporter.onInit()
        customViewModelScope.launch {
            loaderComponent.paymentElementLoader.load(
                initializationMode = paymentSheetArgs.initializationMode,
                integrationConfiguration = PaymentElementLoader.Configuration.PaymentSheet(paymentSheetArgs.config),
                metadata = PaymentElementLoader.Metadata(
                    isReloadingAfterProcessDeath = DefaultConfirmationHandler.wasAwaitingResult(savedStateHandle),
                    initializedViaCompose = paymentSheetArgs.initializedViaCompose,
                ),
            ).fold(
                onSuccess = { loadedState ->
                    loadedState.validationError?.let {
                        _state.value = State.Failed(it)
                    } ?: setReady(
                        args.copy(
                            paymentMethodMetadata = loadedState.paymentMethodMetadata,
                            selection = loadedState.paymentSelection,
                            customerState = loadedState.customer,
                            promotions = loaderComponent.promotionsHelper.getPromotions().orEmpty(),
                            presentationState = EmbeddedActivityArgs.PresentationState.Ready,
                        )
                    )
                },
                onFailure = { error -> _state.value = State.Failed(error) },
            )
        }
    }

    private fun setReady(args: EmbeddedActivityArgs) {
        _state.value = State.Ready(args)
    }

    override fun onCleared() {
        customViewModelScope.cancel()
    }

    sealed interface State {
        data object Loading : State
        data class Ready(val args: EmbeddedActivityArgs) : State
        data class Failed(val error: Throwable) : State
    }

    class Factory(
        private val argsSupplier: () -> EmbeddedActivityArgs,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()
            val customViewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            val args = argsSupplier()
            val paymentSheetArgs =
                (args.activityConfiguration as EmbeddedActivityArgs.ActivityConfiguration.PaymentSheet).args
            val loaderComponent = DaggerSheetActivityLoaderComponent.factory().build(
                application = application,
                savedStateHandle = savedStateHandle,
                args = paymentSheetArgs,
                viewModelScope = customViewModelScope,
            )
            return SheetActivityViewModel(
                args = args,
                loaderComponent = loaderComponent,
                savedStateHandle = savedStateHandle,
                customViewModelScope = customViewModelScope,
            ) as T
        }
    }
}
