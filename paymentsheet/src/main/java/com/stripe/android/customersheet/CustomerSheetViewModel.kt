package com.stripe.android.customersheet

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.PaymentConfiguration
import com.stripe.android.customersheet.injection.CustomerSessionScope
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.CardBillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Stack
import javax.inject.Inject
import javax.inject.Provider

@OptIn(ExperimentalCustomerSheetApi::class)
@CustomerSessionScope
internal class CustomerSheetViewModel @Inject constructor(
    paymentConfiguration: PaymentConfiguration,
    private val resources: Resources,
    private val configuration: CustomerSheet.Configuration,
    private val customerAdapter: CustomerAdapter,
    private val lpmRepository: LpmRepository,
    private val formViewModelSubcomponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>,
) : ViewModel() {

    private val isLiveMode = paymentConfiguration.publishableKey.contains("live")

    private val backstack = Stack<CustomerSheetViewState>()

    private val _viewState = MutableStateFlow<CustomerSheetViewState>(
        CustomerSheetViewState.Loading(
            isLiveMode = isLiveMode,
        )
    )
    val viewState: StateFlow<CustomerSheetViewState> = _viewState

    private val _result = MutableStateFlow<InternalCustomerSheetResult?>(null)
    val result: StateFlow<InternalCustomerSheetResult?> = _result

    init {
        lpmRepository.initializeWithCardSpec(
            CardBillingDetailsCollectionConfiguration(
                address = CardBillingDetailsCollectionConfiguration.AddressCollectionMode.Never
            )
        )
        loadPaymentMethods()
    }

    fun handleViewAction(viewAction: CustomerSheetViewAction) {
        when (viewAction) {
            is CustomerSheetViewAction.OnDismissed -> onDismissed()
            is CustomerSheetViewAction.OnAddCardPressed -> onAddCardPressed()
            is CustomerSheetViewAction.OnBackPressed -> onBackPressed()
            is CustomerSheetViewAction.OnEditPressed -> onEditPressed()
            is CustomerSheetViewAction.OnItemRemoved -> onItemRemoved(viewAction.paymentMethod)
            is CustomerSheetViewAction.OnItemSelected -> onItemSelected(viewAction.selection)
            is CustomerSheetViewAction.OnPrimaryButtonPressed -> onPrimaryButtonPressed()
            is CustomerSheetViewAction.OnFormValuesChanged ->
                onFormValuesChanged(viewAction.formFieldValues)
        }
    }

    fun providePaymentMethodName(code: PaymentMethodCode?): String {
        val paymentMethod = lpmRepository.fromCode(code)
        return paymentMethod?.displayNameResource?.let {
            resources.getString(it)
        }.orEmpty()
    }

    private fun loadPaymentMethods() {
        viewModelScope.launch {
            var savedPaymentMethods: List<PaymentMethod> = emptyList()
            var paymentSelection: PaymentSelection? = null
            var errorMessage: String? = null
            customerAdapter.retrievePaymentMethods().fold(
                onSuccess = { paymentMethods ->
                    savedPaymentMethods = paymentMethods
                    customerAdapter.retrieveSelectedPaymentOption().onSuccess { paymentOption ->
                        paymentSelection = when (paymentOption) {
                            is CustomerAdapter.PaymentOption.GooglePay -> {
                                PaymentSelection.GooglePay
                            }
                            is CustomerAdapter.PaymentOption.Link -> {
                                PaymentSelection.Link
                            }
                            is CustomerAdapter.PaymentOption.StripeId -> {
                                paymentMethods.find { it.id == paymentOption.id }?.let {
                                    PaymentSelection.Saved(it)
                                }
                            }
                            else -> null
                        }
                    }
                },
                onFailure = { throwable ->
                    errorMessage = throwable.message
                }
            )

            transition(
                to = CustomerSheetViewState.SelectPaymentMethod(
                    title = configuration.headerTextForSelectionScreen,
                    savedPaymentMethods = savedPaymentMethods,
                    paymentSelection = paymentSelection,
                    isLiveMode = isLiveMode,
                    isProcessing = false,
                    isEditing = false,
                    isGooglePayEnabled = configuration.googlePayEnabled,
                    primaryButtonLabel = paymentSelection?.let {
                        resources.getString(
                            com.stripe.android.ui.core.R.string.stripe_continue_button_label
                        )
                    },
                    primaryButtonEnabled = paymentSelection != null,
                    errorMessage = errorMessage,
                )
            )
        }
    }

    private fun onAddCardPressed() {
        val formArguments = FormArguments(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            showCheckbox = false,
            showCheckboxControlledFields = false,
            merchantName = "", // Not showing checkbox, so this is unneeded
        )

        val formViewModel = formViewModelSubcomponentBuilderProvider.get()
            .formArguments(formArguments)
            .showCheckboxFlow(flowOf(false))
            .build()
            .viewModel

        transition(
            to = CustomerSheetViewState.AddPaymentMethod(
                formViewDataFlow = formViewModel.viewDataFlow,
                enabled = true,
                isLiveMode = isLiveMode,
            )
        )
    }

    private fun onDismissed() {
        _result.update {
            InternalCustomerSheetResult.Canceled
        }
    }

    private fun onBackPressed() {
        val shouldExit = backstack.peek() is CustomerSheetViewState.SelectPaymentMethod
        if (backstack.empty() || shouldExit) {
            _result.tryEmit(
                InternalCustomerSheetResult.Canceled
            )
        } else {
            backstack.pop()
            _viewState.update {
                backstack.peek()
            }
        }
    }

    private fun onEditPressed() {
        TODO()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onFormValuesChanged(formFieldValues: FormFieldValues?) {
        // TODO (jameswoo) handle onFormValuesChanged
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onItemRemoved(paymentMethod: PaymentMethod) {
        TODO()
    }

    private fun onItemSelected(paymentSelection: PaymentSelection?) {
        // TODO (jameswoo) consider clearing the error message onItemSelected, currently the only
        // error source is when the payment methods cannot be loaded
        when (paymentSelection) {
            is PaymentSelection.GooglePay, is PaymentSelection.Saved -> {
                updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
                    it.copy(
                        paymentSelection = paymentSelection,
                        primaryButtonLabel = resources.getString(
                            com.stripe.android.ui.core.R.string.stripe_continue_button_label
                        ),
                        primaryButtonEnabled = true,
                    )
                }
            }
            else -> {
                updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
                    it.copy(
                        paymentSelection = null,
                        primaryButtonLabel = null,
                        primaryButtonEnabled = false,
                    )
                }
            }
        }
    }

    private fun onPrimaryButtonPressed() {
        TODO()
    }

    private fun transition(to: CustomerSheetViewState) {
        backstack.push(to)
        _viewState.update {
            to
        }
    }

    override fun onCleared() {
        _result.update {
            null
        }
        super.onCleared()
    }

    @Suppress("unused")
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
