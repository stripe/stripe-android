package com.stripe.android.customersheet

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.customersheet.CustomerAdapter.PaymentOption.Companion.toPaymentOption
import com.stripe.android.customersheet.injection.CustomerSessionScope
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.toSavedSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.paymentsheet.ui.getSavedPaymentMethodIcon
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

    // TODO (jameswoo) required for now to clear the result. This allows the view model to not enter
    // a state where the result was already set before. This should be fixed by correctly modeling
    // the lifecycle of this view model.
    fun clear() {
        _result.update {
            null
        }
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
                formViewData = FormViewModel.ViewData(),
                enabled = true,
                isLiveMode = isLiveMode,
                isProcessing = false,
            )
        )

        viewModelScope.launch {
            formViewModel.viewDataFlow.collect { data ->
                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        formViewData = data
                    )
                }
            }
        }
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
        updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
            val isEditing = it.isEditing
            it.copy(isEditing = !isEditing)
        }
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
        when (val currentViewState = viewState.value) {
            is CustomerSheetViewState.AddPaymentMethod -> {
                TODO()
            }
            is CustomerSheetViewState.SelectPaymentMethod -> {
                when (val paymentSelection = currentViewState.paymentSelection) {
                    is PaymentSelection.GooglePay -> selectGooglePay()
                    is PaymentSelection.Saved -> selectSavedPaymentMethod(paymentSelection)
                    else -> error("$paymentSelection is not supported")
                }
            }
            else -> error("${viewState.value} is not supported")
        }
    }

    private fun selectSavedPaymentMethod(savedPaymentSelection: PaymentSelection.Saved) {
        viewModelScope.launch {
            customerAdapter.setSelectedPaymentOption(
                savedPaymentSelection.toSavedSelection()?.toPaymentOption()
            ).onFailure {
                // TODO (jameswoo) Figure out what to do if payment option is unable to be persisted
                updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
                    // TODO (jameswoo) translate string
                    it.copy(errorMessage = "Unable to save the selected payment option")
                }
            }.onSuccess {
                val paymentMethod = savedPaymentSelection.paymentMethod
                val paymentMethodId = paymentMethod.id!!
                val paymentMethodIcon = paymentMethod.getSavedPaymentMethodIcon()
                val paymentMethodLabel = paymentMethod.getLabel(resources)

                // TODO (jameswoo) Figure out what to do if these are null
                if (paymentMethodIcon == null || paymentMethodLabel == null) {
                    _result.tryEmit(
                        InternalCustomerSheetResult.Error(
                            IllegalArgumentException("$paymentMethod is not supported")
                        )
                    )
                } else {
                    _result.tryEmit(
                        InternalCustomerSheetResult.Selected(
                            paymentMethodId = paymentMethodId,
                            drawableResourceId = paymentMethodIcon,
                            label = paymentMethodLabel,
                        )
                    )
                }
            }
        }
    }

    private fun selectGooglePay() {
        viewModelScope.launch {
            customerAdapter.setSelectedPaymentOption(CustomerAdapter.PaymentOption.GooglePay)
                .onFailure {
                    // TODO (jameswoo) Figure out what to do if payment option is unable to be persisted
                    updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
                        // TODO (jameswoo) translate string
                        it.copy(errorMessage = "Unable to save Google Pay")
                    }
                }.onSuccess {
                    _result.tryEmit(
                        InternalCustomerSheetResult.Selected(
                            paymentMethodId = CustomerAdapter.PaymentOption.GooglePay.id,
                            drawableResourceId = R.drawable.stripe_google_pay_mark,
                            label = resources.getString(com.stripe.android.R.string.stripe_google_pay),
                        )
                    )
                }
        }
    }

    private fun transition(to: CustomerSheetViewState) {
        backstack.push(to)
        _viewState.update {
            to
        }
    }

    @Suppress("unused")
    private inline fun <reified T : CustomerSheetViewState> updateViewState(block: (T) -> T) {
        (_viewState.value as? T)?.let {
            _viewState.update {
                block(it as T)
            }
        }
    }
}
