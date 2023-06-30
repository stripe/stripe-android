package com.stripe.android.customersheet

import android.app.Application
import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IS_LIVE_MODE
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.customersheet.CustomerAdapter.PaymentOption.Companion.toPaymentOption
import com.stripe.android.customersheet.injection.CustomerSheetViewModelScope
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.toSavedSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.state.toInternal
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.paymentsheet.ui.getSavedPaymentMethodIcon
import com.stripe.android.paymentsheet.ui.transformToPaymentMethodCreateParams
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Stack
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import com.stripe.android.ui.core.R as PaymentsUiCoreR

@OptIn(ExperimentalCustomerSheetApi::class)
@CustomerSheetViewModelScope
internal class CustomerSheetViewModel @Inject constructor(
    private val application: Application,
    // TODO (jameswoo) should the current view state be derived from backstack?
    private val backstack: Stack<CustomerSheetViewState>,
    private val paymentConfiguration: PaymentConfiguration,
    private val resources: Resources,
    private val configuration: CustomerSheet.Configuration,
    private val stripeRepository: StripeRepository,
    private val customerAdapter: CustomerAdapter,
    private val lpmRepository: LpmRepository,
    @Named(IS_LIVE_MODE) private val isLiveMode: Boolean,
    private val formViewModelSubcomponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>,
) : ViewModel() {

    private val _viewState = MutableStateFlow(backstack.peek())
    val viewState: StateFlow<CustomerSheetViewState> = _viewState

    private val _result = MutableStateFlow<InternalCustomerSheetResult?>(null)
    val result: StateFlow<InternalCustomerSheetResult?> = _result

    init {
        lpmRepository.initializeWithCardSpec(
            configuration.billingDetailsCollectionConfiguration.toInternal()
        )
        if (viewState.value is CustomerSheetViewState.Loading) {
            loadPaymentMethods()
        }
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
        val paymentMethodCode = PaymentMethod.Type.Card.code
        val formArguments = FormArguments(
            paymentMethodCode = paymentMethodCode,
            showCheckbox = false,
            showCheckboxControlledFields = false,
            merchantName = configuration.merchantDisplayName
                ?: application.applicationInfo.loadLabel(application.packageManager).toString(),
            billingDetails = configuration.defaultBillingDetails,
            billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration
        )

        val formViewModel = formViewModelSubcomponentBuilderProvider.get()
            .formArguments(formArguments)
            .showCheckboxFlow(flowOf(false))
            .build()
            .viewModel

        transition(
            to = CustomerSheetViewState.AddPaymentMethod(
                paymentMethodCode = paymentMethodCode,
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

    private fun onItemRemoved(paymentMethod: PaymentMethod) {
        viewModelScope.launch {
            customerAdapter.detachPaymentMethod(paymentMethodId = paymentMethod.id!!)
                .onFailure {
                    updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
                        // TODO (jameswoo) translate error message
                        it.copy(
                            errorMessage = "Unable to remove payment method",
                        )
                    }
                }.onSuccess { paymentMethod ->
                    updateViewState<CustomerSheetViewState.SelectPaymentMethod> { viewState ->
                        viewState.copy(
                            savedPaymentMethods = viewState.savedPaymentMethods.filter { pm ->
                                pm.id != paymentMethod.id!!
                            },
                            paymentSelection = viewState.paymentSelection.takeUnless { selection ->
                                selection is PaymentSelection.Saved &&
                                    selection.paymentMethod == paymentMethod
                            },
                        )
                    }
                }
        }
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
                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(isProcessing = true)
                }
                lpmRepository.fromCode(currentViewState.paymentMethodCode)?.let { paymentMethodSpec ->
                    addPaymentMethod(
                        paymentMethodSpec = paymentMethodSpec,
                        formViewData = currentViewState.formViewData,
                    )
                } ?: error("${currentViewState.paymentMethodCode} is not supported")
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

    private fun addPaymentMethod(
        paymentMethodSpec: LpmRepository.SupportedPaymentMethod,
        formViewData: FormViewModel.ViewData,
    ) {
        viewModelScope.launch {
            runCatching {
                val params = formViewData.completeFormValues
                    ?.transformToPaymentMethodCreateParams(paymentMethodSpec)
                val paymentMethod = requireNotNull(createPaymentMethod(params))
                attachPaymentMethodToCustomer(paymentMethod)
            }.onFailure {
                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    // TODO (jameswoo) translate message
                    it.copy(
                        errorMessage = "Could not create payment method",
                        isProcessing = false,
                    )
                }
            }
        }
    }

    private suspend fun createPaymentMethod(
        createParams: PaymentMethodCreateParams?
    ): PaymentMethod? {
        return createParams?.let {
            stripeRepository.createPaymentMethod(
                paymentMethodCreateParams = createParams,
                options = ApiRequest.Options(
                    apiKey = paymentConfiguration.publishableKey,
                    stripeAccount = paymentConfiguration.stripeAccountId,
                ),
            )
        }
    }

    private fun attachPaymentMethodToCustomer(paymentMethod: PaymentMethod) {
        viewModelScope.launch {
            if (customerAdapter.canCreateSetupIntents) {
                attachWithSetupIntent(paymentMethod = paymentMethod)
            } else {
                attachPaymentMethod(paymentMethod = paymentMethod)
            }
        }
    }

    private suspend fun attachWithSetupIntent(paymentMethod: PaymentMethod) {
        customerAdapter.setupIntentClientSecretForCustomerAttach()
            .mapCatching { clientSecret ->
                requireNotNull(
                    stripeRepository.confirmSetupIntent(
                        confirmSetupIntentParams = ConfirmSetupIntentParams.create(
                            paymentMethodId = paymentMethod.id!!,
                            clientSecret = clientSecret,
                        ),
                        options = ApiRequest.Options(
                            apiKey = paymentConfiguration.publishableKey,
                            stripeAccount = paymentConfiguration.stripeAccountId,
                        ),
                    )
                )
            }.onSuccess {
                handlePaymentMethodAttachSuccess(paymentMethod)
            }.onFailure {
                // TODO (jameswoo) translate error message
                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        errorMessage = "Unable to attach this payment method",
                        isProcessing = false,
                    )
                }
            }
    }

    private suspend fun attachPaymentMethod(paymentMethod: PaymentMethod) {
        customerAdapter.attachPaymentMethod(paymentMethod.id!!)
            .onSuccess {
                handlePaymentMethodAttachSuccess(paymentMethod)
            }
            .onFailure {
                // TODO (jameswoo) translate error message
                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        errorMessage = "Unable to attach this payment method",
                        isProcessing = false,
                    )
                }
            }
    }

    private fun handlePaymentMethodAttachSuccess(paymentMethod: PaymentMethod) {
        onBackPressed()
        updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
            it.copy(
                savedPaymentMethods = listOf(paymentMethod) + it.savedPaymentMethods,
                paymentSelection = PaymentSelection.Saved(
                    paymentMethod = paymentMethod
                ),
                primaryButtonLabel = resources.getString(
                    PaymentsUiCoreR.string.stripe_continue_button_label
                ),
                primaryButtonEnabled = true,
            )
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

    private inline fun <reified T : CustomerSheetViewState> updateViewState(block: (T) -> T) {
        (_viewState.value as? T)?.let {
            _viewState.update {
                block(it as T)
            }
        }
    }

    object Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CustomerSessionViewModel.component.customerSheetViewModelComponentBuilder
                .build().viewModel as T
        }
    }
}
