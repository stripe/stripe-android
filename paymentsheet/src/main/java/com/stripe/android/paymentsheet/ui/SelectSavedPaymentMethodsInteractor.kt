package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal interface SelectSavedPaymentMethodsInteractor {

    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    fun close()

    data class State(
        val paymentOptionsState: PaymentOptionsState,
        val isEditing: Boolean,
        val isProcessing: Boolean,
    )

    sealed class ViewAction {
        data object OnAddCardPressed : ViewAction()

        data class HandlePaymentMethodSelected(val selection: PaymentSelection?) : ViewAction()

        data class DeletePaymentMethod(val paymentMethod: PaymentMethod) : ViewAction()
        data class EditPaymentMethod(val paymentMethod: PaymentMethod) : ViewAction()
    }
}

internal class DefaultSelectSavedPaymentMethodsInteractor(
    private val paymentOptionsState: StateFlow<PaymentOptionsState>,
    private val editing: StateFlow<Boolean>,
    private val isProcessing: StateFlow<Boolean>,
    private val onAddCardPressed: () -> Unit,
    private val onEditPaymentMethod: (PaymentMethod) -> Unit,
    private val onDeletePaymentMethod: (PaymentMethod) -> Unit,
    private val onPaymentMethodSelected: (PaymentSelection?) -> Unit,
    dispatcher: CoroutineContext = Dispatchers.Default,
) : SelectSavedPaymentMethodsInteractor {
        constructor(viewModel: BaseSheetViewModel) : this(
                paymentOptionsState = viewModel.paymentOptionsState,
                editing = viewModel.editing,
                isProcessing = viewModel.processing,
                onAddCardPressed = viewModel::transitionToAddPaymentScreen,
                onEditPaymentMethod = viewModel::modifyPaymentMethod,
                onDeletePaymentMethod = viewModel::removePaymentMethod,
                onPaymentMethodSelected = viewModel::handlePaymentMethodSelected,
        )

    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    private val _state: MutableStateFlow<SelectSavedPaymentMethodsInteractor.State> =
        MutableStateFlow(getInitialState())
    override val state: StateFlow<SelectSavedPaymentMethodsInteractor.State> = _state

    private fun getInitialState(): SelectSavedPaymentMethodsInteractor.State {
        return SelectSavedPaymentMethodsInteractor.State(
            paymentOptionsState = paymentOptionsState.value,
            isEditing = editing.value,
            isProcessing = isProcessing.value,
        )
    }

    init {
        coroutineScope.launch {
            paymentOptionsState.collect {
                _state.value = _state.value.copy(
                    paymentOptionsState = it
                )
            }
        }

        coroutineScope.launch {
            editing.collect {
                _state.value = _state.value.copy(
                    isEditing = it
                )
            }
        }

        coroutineScope.launch {
            isProcessing.collect {
                _state.value = _state.value.copy(
                    isProcessing = it
                )
            }
        }
    }

    override fun handleViewAction(viewAction: SelectSavedPaymentMethodsInteractor.ViewAction) {
        when (viewAction) {
            is SelectSavedPaymentMethodsInteractor.ViewAction.DeletePaymentMethod -> onDeletePaymentMethod(
                viewAction.paymentMethod
            )

            is SelectSavedPaymentMethodsInteractor.ViewAction.EditPaymentMethod -> onEditPaymentMethod(
                viewAction.paymentMethod
            )

            is SelectSavedPaymentMethodsInteractor.ViewAction.HandlePaymentMethodSelected -> onPaymentMethodSelected(
                viewAction.selection
            )

            SelectSavedPaymentMethodsInteractor.ViewAction.OnAddCardPressed -> onAddCardPressed()
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }
}

internal class UnsupportedSelectSavedPaymentMethodsInteractor : SelectSavedPaymentMethodsInteractor {
        override val state: StateFlow<SelectSavedPaymentMethodsInteractor.State>
                get() = TODO("Not yet implemented")

        override fun handleViewAction(viewAction: SelectSavedPaymentMethodsInteractor.ViewAction) {
                TODO("Not yet implemented")
        }

        override fun close() {
                TODO("Not yet implemented")
        }

}
