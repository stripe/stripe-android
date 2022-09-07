package com.stripe.android.link.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkActivityResult.Canceled.Reason.PayAnotherWay
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.ConfirmStripeIntentParamsFactory
import com.stripe.android.link.confirmation.ConfirmationManager
import com.stripe.android.link.injection.SignedInViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.model.supportedPaymentMethodTypes
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.getErrorMessage
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.ui.core.address.toConfirmPaymentIntentShipping
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.ui.core.elements.DateConfig
import com.stripe.android.ui.core.elements.SimpleTextFieldController
import com.stripe.android.ui.core.injection.NonFallbackInjectable
import com.stripe.android.ui.core.injection.NonFallbackInjector
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal class WalletViewModel @Inject constructor(
    val args: LinkActivityContract.Args,
    private val linkAccountManager: LinkAccountManager,
    private val navigator: Navigator,
    private val confirmationManager: ConfirmationManager,
    private val logger: Logger
) : ViewModel() {
    private val stripeIntent = args.stripeIntent

    private val _paymentDetailsList =
        MutableStateFlow<List<ConsumerPaymentDetails.PaymentDetails>>(emptyList())
    val paymentDetailsList: StateFlow<List<ConsumerPaymentDetails.PaymentDetails>> =
        _paymentDetailsList

    val supportedTypes = args.stripeIntent.supportedPaymentMethodTypes(
        requireNotNull(linkAccountManager.linkAccount.value)
    )

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded

    private val _selectedItem = MutableStateFlow<ConsumerPaymentDetails.PaymentDetails?>(null)
    val selectedItem: StateFlow<ConsumerPaymentDetails.PaymentDetails?> = _selectedItem

    val expiryDateController = SimpleTextFieldController(
        textFieldConfig = DateConfig(),
        initialValue = null
    )

    val cvcController = CvcController(
        cardBrandFlow = selectedItem.map {
            (it as? ConsumerPaymentDetails.Card)?.brand ?: CardBrand.Unknown
        }
    )

    private val _primaryButtonState = MutableStateFlow(PrimaryButtonState.Disabled)
    val primaryButtonState: StateFlow<PrimaryButtonState> = _primaryButtonState

    private val _errorMessage = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: StateFlow<ErrorMessage?> = _errorMessage

    init {
        loadPaymentDetails(true)

        viewModelScope.launch {
            navigator.getResultFlow<PaymentDetailsResult>(PaymentDetailsResult.KEY)?.collect {
                when (it) {
                    is PaymentDetailsResult.Success ->
                        loadPaymentDetails(selectedItem = it.itemId)
                    PaymentDetailsResult.Cancelled -> {}
                    is PaymentDetailsResult.Failure -> onError(it.error)
                }
            }
        }
    }

    fun onConfirmPayment() {
        val selectedPaymentDetails = selectedItem.value ?: return

        clearError()
        setState(PrimaryButtonState.Processing)

        runCatching { requireNotNull(linkAccountManager.linkAccount.value) }.fold(
            onSuccess = { linkAccount ->
                val paramsFactory = ConfirmStripeIntentParamsFactory.createFactory(
                    stripeIntent,
                    args.shippingValues?.toConfirmPaymentIntentShipping()
                )
                val params = paramsFactory.createPaymentMethodCreateParams(
                    linkAccount.clientSecret,
                    selectedPaymentDetails
                )
                confirmationManager.confirmStripeIntent(
                    paramsFactory.createConfirmStripeIntentParams(params)
                ) { result ->
                    result.fold(
                        onSuccess = { paymentResult ->
                            when (paymentResult) {
                                is PaymentResult.Canceled -> {
                                    // no-op, let the user continue their flow
                                    setState(PrimaryButtonState.Enabled)
                                }
                                is PaymentResult.Failed -> {
                                    onError(paymentResult.throwable)
                                }
                                is PaymentResult.Completed -> {
                                    setState(PrimaryButtonState.Completed)
                                    viewModelScope.launch {
                                        delay(PrimaryButtonState.COMPLETED_DELAY_MS)
                                        navigator.dismiss(LinkActivityResult.Completed)
                                    }
                                }
                            }
                        },
                        onFailure = ::onError
                    )
                }
            },
            onFailure = ::onError
        )
    }

    fun setExpanded(expanded: Boolean) {
        _isExpanded.value = expanded
    }

    fun payAnotherWay() {
        navigator.cancel(reason = PayAnotherWay)
    }

    fun addNewPaymentMethod(clearBackStack: Boolean = false) {
        navigator.navigateTo(LinkScreen.PaymentMethod(), clearBackStack)
    }

    fun editPaymentMethod(paymentDetails: ConsumerPaymentDetails.PaymentDetails) {
        clearError()
        navigator.navigateTo(LinkScreen.CardEdit(paymentDetails.id))
    }

    fun deletePaymentMethod(paymentDetails: ConsumerPaymentDetails.PaymentDetails) {
        setState(PrimaryButtonState.Processing)
        clearError()

        viewModelScope.launch {
            linkAccountManager.deletePaymentDetails(
                paymentDetails.id
            ).fold(
                onSuccess = {
                    loadPaymentDetails()
                },
                onFailure = ::onError
            )
        }
    }

    fun onItemSelected(item: ConsumerPaymentDetails.PaymentDetails) {
        _selectedItem.value = item
    }

    private fun loadPaymentDetails(
        initialSetup: Boolean = false,
        selectedItem: String? = null
    ) {
        setState(PrimaryButtonState.Processing)
        viewModelScope.launch {
            linkAccountManager.listPaymentDetails().fold(
                onSuccess = { response ->
                    setState(PrimaryButtonState.Enabled)
                    _paymentDetailsList.value = response.paymentDetails

                    // Select selectedItem if provided, otherwise the previously selected item
                    _selectedItem.value = (selectedItem ?: _selectedItem.value?.id)?.let { itemId ->
                        response.paymentDetails.firstOrNull { it.id == itemId }
                    } ?: getDefaultItemSelection(response.paymentDetails)

                    if (_selectedItem.value?.id == selectedItem) {
                        _isExpanded.value = false
                    }

                    if (initialSetup && args.prefilledCardParams != null) {
                        // User has already pre-filled the payment details
                        navigator.navigateTo(
                            LinkScreen.PaymentMethod(true),
                            clearBackStack = response.paymentDetails.isEmpty()
                        )
                    } else if (response.paymentDetails.isEmpty()) {
                        addNewPaymentMethod(clearBackStack = true)
                    }
                },
                // If we can't load the payment details there's nothing to see here
                onFailure = ::onFatal
            )
        }
    }

    private fun clearError() {
        _errorMessage.value = null
    }

    private fun onError(error: Throwable) = error.getErrorMessage().let {
        logger.error("Error: ", error)
        onError(it)
    }

    private fun onError(error: ErrorMessage) {
        setState(PrimaryButtonState.Enabled)
        _errorMessage.value = error
    }

    private fun onFatal(fatalError: Throwable) {
        logger.error("Fatal error: ", fatalError)
        navigator.dismiss(LinkActivityResult.Failed(fatalError))
    }

    private fun setState(state: PrimaryButtonState) {
        _primaryButtonState.value = state
        navigator.userNavigationEnabled = !state.isBlocking
    }

    /**
     * The item that should be selected by default from the [paymentDetailsList].
     *
     * @return the default item, if supported. Otherwise the first supported item on the list.
     */
    private fun getDefaultItemSelection(
        paymentDetailsList: List<ConsumerPaymentDetails.PaymentDetails>
    ) = paymentDetailsList.filter { supportedTypes.contains(it.type) }.let { filteredItems ->
        filteredItems.firstOrNull { it.isDefault } ?: filteredItems.firstOrNull()
    }

    internal class Factory(
        private val linkAccount: LinkAccount,
        private val injector: NonFallbackInjector
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<SignedInViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .linkAccount(linkAccount)
                .build().walletViewModel as T
        }
    }
}
