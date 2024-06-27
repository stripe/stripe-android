package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal interface SelectSavedPaymentMethodsInteractor {

    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    fun close()

    data class State(
        val paymentOptionsItems: List<PaymentOptionsItem>,
        val selectedPaymentOptionsItem: PaymentOptionsItem?,
        val isEditing: Boolean,
        val isProcessing: Boolean,
    )

    sealed class ViewAction {
        data object AddCardPressed : ViewAction()

        data class SelectPaymentMethod(val selection: PaymentSelection?) : ViewAction()

        data class DeletePaymentMethod(val paymentMethod: PaymentMethod) : ViewAction()
        data class EditPaymentMethod(val paymentMethod: PaymentMethod) : ViewAction()
    }
}

internal class DefaultSelectSavedPaymentMethodsInteractor(
    private val paymentOptionsItems: StateFlow<List<PaymentOptionsItem>>,
    private val editing: StateFlow<Boolean>,
    private val isProcessing: StateFlow<Boolean>,
    private val currentSelection: StateFlow<PaymentSelection?>,
    private val mostRecentlySelectedSavedPaymentMethod: StateFlow<PaymentMethod?>,
    private val onAddCardPressed: () -> Unit,
    private val onEditPaymentMethod: (PaymentMethod) -> Unit,
    private val onDeletePaymentMethod: (PaymentMethod) -> Unit,
    private val onPaymentMethodSelected: (PaymentSelection?) -> Unit,
    dispatcher: CoroutineContext = Dispatchers.Default,
) : SelectSavedPaymentMethodsInteractor {
    constructor(viewModel: BaseSheetViewModel) : this(
        paymentOptionsItems = viewModel.paymentOptionsItems,
        editing = viewModel.editing,
        isProcessing = viewModel.processing,
        currentSelection = viewModel.selection,
        mostRecentlySelectedSavedPaymentMethod = viewModel.mostRecentlySelectedSavedPaymentMethod,
        onAddCardPressed = viewModel::transitionToAddPaymentScreen,
        onEditPaymentMethod = viewModel::modifyPaymentMethod,
        onDeletePaymentMethod = viewModel::removePaymentMethod,
        onPaymentMethodSelected = viewModel::handlePaymentMethodSelected,
    )

    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    private val _displayedSelection: MutableStateFlow<PaymentSelection?> = MutableStateFlow(currentSelection.value)
    private val displayedSelection: StateFlow<PaymentSelection?> = _displayedSelection

    private val _state: MutableStateFlow<SelectSavedPaymentMethodsInteractor.State> =
        MutableStateFlow(getInitialState())
    override val state: StateFlow<SelectSavedPaymentMethodsInteractor.State> = _state

    private fun getInitialState(): SelectSavedPaymentMethodsInteractor.State {
        val paymentOptionsItems = paymentOptionsItems.value

        return SelectSavedPaymentMethodsInteractor.State(
            paymentOptionsItems = paymentOptionsItems,
            selectedPaymentOptionsItem = PaymentOptionsStateFactory.getSelectedItem(
                paymentOptionsItems,
                currentSelection.value,
            ),
            isEditing = editing.value,
            isProcessing = isProcessing.value,
        )
    }

    init {
        coroutineScope.launch {
            paymentOptionsItems.collect {
                _state.value = _state.value.copy(
                    paymentOptionsItems = it,
                    selectedPaymentOptionsItem = PaymentOptionsStateFactory.getSelectedItem(
                        it,
                        displayedSelection.value,
                    )
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

        coroutineScope.launch {
            val paymentOptionsRelevantSelection = currentSelection.filter { selection ->
                selection is PaymentSelection.Saved ||
                    selection == PaymentSelection.Link ||
                    selection == PaymentSelection.GooglePay
            }.stateIn(this)

            combineAsStateFlow(
                paymentOptionsRelevantSelection,
                mostRecentlySelectedSavedPaymentMethod
            ) { selection, savedSelection ->
                when (selection) {
                    is PaymentSelection.Saved, PaymentSelection.Link, PaymentSelection.GooglePay -> selection

                    is PaymentSelection.New, is PaymentSelection.ExternalPaymentMethod, null -> savedSelection?.let {
                        PaymentSelection.Saved(it)
                    }
                }
            }.collect { newSelection ->
                _displayedSelection.value = newSelection
            }
        }

        coroutineScope.launch {
            displayedSelection.collect {
                _state.value = _state.value.copy(
                    selectedPaymentOptionsItem = PaymentOptionsStateFactory.getSelectedItem(
                        paymentOptionsItems.value,
                        it,
                    )
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

            is SelectSavedPaymentMethodsInteractor.ViewAction.SelectPaymentMethod -> onPaymentMethodSelected(
                viewAction.selection
            )

            SelectSavedPaymentMethodsInteractor.ViewAction.AddCardPressed -> onAddCardPressed()
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }
}
