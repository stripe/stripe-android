package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
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
        paymentOptionsItems = viewModel.savedPaymentMethodMutator.paymentOptionsItems,
        editing = viewModel.editing,
        isProcessing = viewModel.processing,
        currentSelection = viewModel.selection,
        mostRecentlySelectedSavedPaymentMethod = viewModel.savedPaymentMethodMutator.mostRecentlySelectedSavedPaymentMethod,
        onAddCardPressed = {
            viewModel.navigationHandler.transitionTo(
                AddAnotherPaymentMethod(interactor = DefaultAddPaymentMethodInteractor.create(viewModel))
            )
        },
        onEditPaymentMethod = viewModel.savedPaymentMethodMutator::modifyPaymentMethod,
        onDeletePaymentMethod = viewModel.savedPaymentMethodMutator::removePaymentMethod,
        onPaymentMethodSelected = viewModel::handlePaymentMethodSelected,
    )

    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    private val _paymentOptionsRelevantSelection: MutableStateFlow<PaymentSelection?> = MutableStateFlow(null)

    private val _state: MutableStateFlow<SelectSavedPaymentMethodsInteractor.State> =
        MutableStateFlow(getInitialState())
    override val state: StateFlow<SelectSavedPaymentMethodsInteractor.State> = _state

    private fun getInitialState(): SelectSavedPaymentMethodsInteractor.State {
        val paymentOptionsItems = paymentOptionsItems.value

        return SelectSavedPaymentMethodsInteractor.State(
            paymentOptionsItems = paymentOptionsItems,
            selectedPaymentOptionsItem = getSelectedPaymentOptionsItem(
                currentSelection.value,
                mostRecentlySelectedSavedPaymentMethod.value,
                paymentOptionsItems,
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
            currentSelection.filter { selection ->
                selection is PaymentSelection.Saved ||
                    selection == PaymentSelection.Link ||
                    selection == PaymentSelection.GooglePay
            }.collect {
                _paymentOptionsRelevantSelection.value = it
            }
        }

        coroutineScope.launch {
            combineAsStateFlow(
                _paymentOptionsRelevantSelection,
                mostRecentlySelectedSavedPaymentMethod,
                paymentOptionsItems,
            ) { selection, savedSelection, paymentOptionsItems ->
                getSelectedPaymentOptionsItem(selection, savedSelection, paymentOptionsItems)
            }.collect { selectedPaymentOptionsItem ->
                _state.value = _state.value.copy(
                    selectedPaymentOptionsItem = selectedPaymentOptionsItem
                )
            }
        }
    }

    private fun getSelectedPaymentOptionsItem(
        selection: PaymentSelection?,
        savedSelection: PaymentMethod?,
        paymentOptionsItems: List<PaymentOptionsItem>,
    ): PaymentOptionsItem? {
        val paymentSelection = when (selection) {
            is PaymentSelection.Saved, PaymentSelection.Link, PaymentSelection.GooglePay -> selection

            is PaymentSelection.New, is PaymentSelection.ExternalPaymentMethod, null -> savedSelection?.let {
                PaymentSelection.Saved(it)
            }
        }

        return PaymentOptionsStateFactory.getSelectedItem(
            paymentOptionsItems,
            paymentSelection,
        )
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
