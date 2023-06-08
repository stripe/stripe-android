package com.stripe.android.customersheet

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.customersheet.injection.CustomerSessionScope
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCustomerSheetApi::class)
@CustomerSessionScope
@Suppress("unused")
internal class CustomerSheetViewModel @Inject constructor(
    private val resources: Resources,
    private val configuration: CustomerSheet.Configuration,
    private val customerAdapter: CustomerAdapter,
    private val lpmRepository: LpmRepository,
) : ViewModel() {

    private val _viewState = MutableStateFlow<CustomerSheetViewState>(CustomerSheetViewState.Loading)
    val viewState: StateFlow<CustomerSheetViewState> = _viewState

    init {
        loadPaymentMethods()
    }

    fun handleViewAction(viewAction: CustomerSheetViewAction) {
        when (viewAction) {
            is CustomerSheetViewAction.OnAddCardPressed -> onAddCardPressed()
            is CustomerSheetViewAction.OnBackPressed -> onBackPressed()
            is CustomerSheetViewAction.OnEditPressed -> onEdit()
            is CustomerSheetViewAction.OnItemRemoved -> onItemRemoved(viewAction.paymentMethod)
            is CustomerSheetViewAction.OnItemSelected -> onItemSelected(viewAction.selection)
        }
    }

    private fun loadPaymentMethods() {
        viewModelScope.launch {
            var savedPaymentMethods: List<PaymentOptionsItem.SavedPaymentMethod> = emptyList()
            var selectedPaymentMethodId: String? = null
            var errorMessage: String? = null
            customerAdapter.retrievePaymentMethods().fold(
                onSuccess = { paymentMethods ->
                    savedPaymentMethods = paymentMethods.map { paymentMethod ->
                        PaymentOptionsItem.SavedPaymentMethod(
                            displayName = paymentMethod
                                .getLabel(resources)
                                .toString(),
                            paymentMethod = paymentMethod
                        )
                    }
                    customerAdapter.retrieveSelectedPaymentOption().fold(
                        onSuccess = { selectedPaymentMethod ->
                            selectedPaymentMethodId = selectedPaymentMethod?.id
                        },
                        onFailure = { throwable ->
                            errorMessage = throwable.message
                        }
                    )
                },
                onFailure = { throwable ->
                    errorMessage = throwable.message
                }
            )
            _viewState.update {
                CustomerSheetViewState.SelectPaymentMethod(
                    title = configuration.headerTextForSelectionScreen,
                    paymentMethods = savedPaymentMethods,
                    selectedPaymentMethodId = selectedPaymentMethodId,
                    isLiveMode = false,
                    isProcessing = false,
                    isEditing = false,
                    errorMessage = errorMessage,
                )
            }
        }
    }

    private fun onAddCardPressed() {
        TODO()
    }

    private fun onBackPressed() {
        updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
            it.copy(result = InternalCustomerSheetResult.Canceled)
        }
    }

    private fun onEdit() {
        TODO()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onItemRemoved(paymentMethod: PaymentMethod) {
        TODO()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onItemSelected(paymentSelection: PaymentSelection?) {
        TODO()
    }

    override fun onCleared() {
        updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
            it.copy(result = null)
        }
        super.onCleared()
    }

    private inline fun <reified T : CustomerSheetViewState> updateViewState(block: (T) -> T) {
        (_viewState.value as? T)?.let {
            _viewState.update {
                block(it as T)
            }
        }
    }

    object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            @Suppress("UNCHECKED_CAST")
            return CustomerSessionViewModel.component.customerSheetViewModel as T
        }
    }
}
