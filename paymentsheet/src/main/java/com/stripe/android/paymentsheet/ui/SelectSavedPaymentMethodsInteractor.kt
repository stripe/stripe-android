package com.stripe.android.paymentsheet.ui

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface SelectSavedPaymentMethodsInteractor {
    val isLiveMode: Boolean

    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    fun close()

    data class State(
        val paymentOptionsItems: List<PaymentOptionsItem>,
        val selectedPaymentOptionsItem: PaymentOptionsItem?,
        val isEditing: Boolean,
        val isProcessing: Boolean,
        val canEdit: Boolean,
        val canRemove: Boolean,
    )

    sealed class ViewAction {
        data object AddCardPressed : ViewAction()
        data class SelectPaymentMethod(val selection: PaymentSelection?) : ViewAction()
        data class EditPaymentMethod(val paymentMethod: DisplayableSavedPaymentMethod) : ViewAction()
        data object ToggleEdit : ViewAction()
    }
}

internal class DefaultSelectSavedPaymentMethodsInteractor(
    private val paymentOptionsItems: StateFlow<List<PaymentOptionsItem>>,
    private val editing: StateFlow<Boolean>,
    private val canEdit: StateFlow<Boolean>,
    private val canRemove: StateFlow<Boolean>,
    private val toggleEdit: () -> Unit,
    private val isProcessing: StateFlow<Boolean>,
    private val currentSelection: StateFlow<PaymentSelection?>,
    private val mostRecentlySelectedSavedPaymentMethod: StateFlow<PaymentMethod?>,
    private val onAddCardPressed: () -> Unit,
    private val onUpdatePaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    private val onPaymentMethodSelected: (PaymentSelection?) -> Unit,
    override val isLiveMode: Boolean,
) : SelectSavedPaymentMethodsInteractor {
    private val coroutineScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

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
            canEdit = canEdit.value,
            canRemove = canRemove.value,
        )
    }

    init {
        coroutineScope.launch {
            paymentOptionsItems.collect {
                _state.update { previousState ->
                    previousState.copy(
                        paymentOptionsItems = it,
                    )
                }
            }
        }

        coroutineScope.launch {
            editing.collect {
                _state.update { previousState ->
                    previousState.copy(
                        isEditing = it
                    )
                }
            }
        }

        coroutineScope.launch {
            canEdit.collect {
                _state.update { previousState ->
                    previousState.copy(
                        canEdit = it
                    )
                }
            }
        }

        coroutineScope.launch {
            canRemove.collect {
                _state.update { previousState ->
                    previousState.copy(
                        canRemove = it
                    )
                }
            }
        }

        coroutineScope.launch {
            isProcessing.collect {
                _state.update { previousState ->
                    previousState.copy(
                        isProcessing = it
                    )
                }
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
                _state.update { previousState ->
                    previousState.copy(
                        selectedPaymentOptionsItem = selectedPaymentOptionsItem
                    )
                }
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
            is SelectSavedPaymentMethodsInteractor.ViewAction.EditPaymentMethod -> {
                onUpdatePaymentMethod(viewAction.paymentMethod)
            }

            is SelectSavedPaymentMethodsInteractor.ViewAction.SelectPaymentMethod -> onPaymentMethodSelected(
                viewAction.selection
            )

            SelectSavedPaymentMethodsInteractor.ViewAction.AddCardPressed -> onAddCardPressed()
            SelectSavedPaymentMethodsInteractor.ViewAction.ToggleEdit -> toggleEdit()
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }

    companion object {
        fun create(
            viewModel: BaseSheetViewModel,
            paymentMethodMetadata: PaymentMethodMetadata,
            customerStateHolder: CustomerStateHolder,
            savedPaymentMethodMutator: SavedPaymentMethodMutator,
        ): SelectSavedPaymentMethodsInteractor {
            return DefaultSelectSavedPaymentMethodsInteractor(
                paymentOptionsItems = savedPaymentMethodMutator.paymentOptionsItems,
                editing = savedPaymentMethodMutator.editing,
                canEdit = savedPaymentMethodMutator.canEdit,
                canRemove = savedPaymentMethodMutator.canRemove,
                toggleEdit = savedPaymentMethodMutator::toggleEditing,
                isProcessing = viewModel.processing,
                currentSelection = viewModel.selection,
                mostRecentlySelectedSavedPaymentMethod = customerStateHolder.mostRecentlySelectedSavedPaymentMethod,
                onAddCardPressed = {
                    val interactor = DefaultAddPaymentMethodInteractor.create(
                        viewModel = viewModel,
                        paymentMethodMetadata = paymentMethodMetadata,
                    )
                    viewModel.navigationHandler.transitionTo(
                        AddAnotherPaymentMethod(interactor = interactor)
                    )
                },
                onUpdatePaymentMethod = savedPaymentMethodMutator::updatePaymentMethod,
                onPaymentMethodSelected = viewModel::handlePaymentMethodSelected,
                isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            )
        }
    }
}
