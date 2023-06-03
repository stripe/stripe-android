package com.stripe.android.customersheet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
    application: Application,
    private val configuration: CustomerSheet.Configuration,
    private val customerAdapter: CustomerAdapter,
    private val lpmRepository: LpmRepository,
) : AndroidViewModel(application) {

    private val _viewState = MutableStateFlow<CustomerSheetViewState>(CustomerSheetViewState.Loading)
    val viewState: StateFlow<CustomerSheetViewState> = _viewState

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState

    private val _action = MutableStateFlow<CustomerSheetAction?>(null)
    val action: StateFlow<CustomerSheetAction?> = _action

    init {
        loadPaymentMethods()
    }

    private fun loadPaymentMethods() {
        viewModelScope.launch {
            customerAdapter.retrievePaymentMethods().fold(
                onSuccess = { paymentMethods ->
                    val savedPaymentMethods = paymentMethods.map { paymentMethod ->
                        PaymentOptionsItem.SavedPaymentMethod(
                            displayName = paymentMethod
                                .getLabel(getApplication<Application>().resources)
                                .toString(),
                            paymentMethod = paymentMethod
                        )
                    }
                    val selected = customerAdapter.retrieveSelectedPaymentOption().fold(
                        onSuccess = { selectedPaymentMethod ->
                            _viewState.update {
                                CustomerSheetViewState.SelectPaymentMethod(
                                    title = configuration.headerTextForSelectionScreen,
                                    paymentMethods = savedPaymentMethods,
                                    selectedPaymentMethodId = selectedPaymentMethod?.id,
                                    isLiveMode = false,
                                    isProcessing = false,
                                    isEditing = false,
                                    error = null,
                                )
                            }
                        },
                        onFailure = {
                            onError(it.message)
                        }
                    )

                },
                onFailure = {
                    onError(it.message)
                }
            )
        }
    }

    @Suppress("UNUSED_EXPRESSION")
    fun handleViewAction(viewAction: CustomerSheetViewAction) {
        when (viewAction) {
            is CustomerSheetViewAction.OnAddCardPressed -> onAddCardPressed()
            is CustomerSheetViewAction.OnBackPress -> onBackPressed()
            is CustomerSheetViewAction.OnEdit -> onEdit()
            is CustomerSheetViewAction.OnItemRemoved -> ::onItemRemoved
            is CustomerSheetViewAction.OnItemSelected -> ::onItemSelected
        }
    }

    private fun onAddCardPressed() {
        TODO()
    }

    private fun onBackPressed() {
        _action.update {
            CustomerSheetAction.NavigateUp
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

    private fun onError(message: String?) {
        _errorState.update {
            message
        }
    }

    override fun onCleared() {
        super.onCleared()
        _action.update { null }
    }

    object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            @Suppress("UNCHECKED_CAST")
            return CustomerSessionViewModel.component.customerSheetViewModel as T
        }
    }
}
