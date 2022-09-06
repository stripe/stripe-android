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
import com.stripe.android.model.ConfirmStripeIntentParams
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
import kotlinx.coroutines.flow.update
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

    private val _uiState = MutableStateFlow(
        value = WalletUiState(
            supportedTypes = args.stripeIntent.supportedPaymentMethodTypes(
                requireNotNull(linkAccountManager.linkAccount.value)
            )
        )
    )

    val uiState: StateFlow<WalletUiState> = _uiState

    val expiryDateController = SimpleTextFieldController(
        textFieldConfig = DateConfig(),
        initialValue = null
    )

    val cvcController = CvcController(
        cardBrandFlow = uiState.map {
            (it.selectedItem as? ConsumerPaymentDetails.Card)?.brand ?: CardBrand.Unknown
        }
    )

    init {
        loadPaymentDetails(true)

        viewModelScope.launch {
            _uiState.collect {
                navigator.userNavigationEnabled = !it.primaryButtonState.isBlocking
            }
        }

        viewModelScope.launch {
            expiryDateController.formFieldValue.collect { input ->
                _uiState.update {
                    it.copy(expiryDateInput = input)
                }
            }
        }

        viewModelScope.launch {
            cvcController.formFieldValue.collect { input ->
                _uiState.update {
                    it.copy(cvcInput = input)
                }
            }
        }

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
        val selectedPaymentDetails = uiState.value.selectedItem ?: return

        _uiState.update {
            it.setProcessing()
        }

        runCatching { requireNotNull(linkAccountManager.linkAccount.value) }.fold(
            onSuccess = { linkAccount ->
                val params = createConfirmStripeIntentParams(
                    selectedPaymentDetails = selectedPaymentDetails,
                    linkAccount = linkAccount
                )

                confirmationManager.confirmStripeIntent(params) { result ->
                    result.fold(
                        onSuccess = ::handleConfirmPaymentSuccess,
                        onFailure = ::onError
                    )
                }
            },
            onFailure = ::onError
        )
    }

    private fun createConfirmStripeIntentParams(
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount
    ): ConfirmStripeIntentParams {
        val paramsFactory = ConfirmStripeIntentParamsFactory.createFactory(
            stripeIntent = stripeIntent,
            shipping = args.shippingValues?.toConfirmPaymentIntentShipping()
        )

        val cvc = uiState.value.cvcInput.takeIf { it.isComplete }?.value
        val extraParams = if (cvc != null) {
            mapOf("card" to mapOf("cvc" to cvc))
        } else {
            null
        }

        val params = paramsFactory.createPaymentMethodCreateParams(
            consumerSessionClientSecret = linkAccount.clientSecret,
            selectedPaymentDetails = selectedPaymentDetails,
            extraParams = extraParams
        )

        return paramsFactory.createConfirmStripeIntentParams(params)
    }

    private fun handleConfirmPaymentSuccess(paymentResult: PaymentResult) {
        _uiState.update {
            it.updateWithPaymentResult(paymentResult)
        }

        when (paymentResult) {
            is PaymentResult.Canceled -> Unit
            is PaymentResult.Failed -> {
                logger.error("Error: ", paymentResult.throwable)
            }
            is PaymentResult.Completed -> {
                viewModelScope.launch {
                    delay(PrimaryButtonState.COMPLETED_DELAY_MS)
                    navigator.dismiss(LinkActivityResult.Completed)
                }
            }
        }
    }

    fun setExpanded(expanded: Boolean) {
        _uiState.update {
            it.copy(isExpanded = expanded)
        }
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
        _uiState.update {
            it.setProcessing()
        }

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
        _uiState.update {
            it.copy(selectedItem = item)
        }
    }

    private fun loadPaymentDetails(
        initialSetup: Boolean = false,
        selectedItem: String? = null
    ) {
        _uiState.update {
            it.setProcessing()
        }

        viewModelScope.launch {
            linkAccountManager.listPaymentDetails().fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.updateWithResponse(
                            response = response,
                            initialSelectedItemId = selectedItem
                        )
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
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    private fun onError(error: Throwable) {
        logger.error("Error: ", error)
        onError(error.getErrorMessage())
    }

    private fun onError(error: ErrorMessage) {
        _uiState.update {
            it.updateWithError(error)
        }
    }

    private fun onFatal(fatalError: Throwable) {
        logger.error("Fatal error: ", fatalError)
        navigator.dismiss(LinkActivityResult.Failed(fatalError))
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
