package com.stripe.android.link.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.ConfirmStripeIntentParamsFactory
import com.stripe.android.link.confirmation.ConfirmationManager
import com.stripe.android.ui.core.injection.NonFallbackInjectable
import com.stripe.android.ui.core.injection.NonFallbackInjector
import com.stripe.android.link.injection.SignedInViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.cardedit.CardEditViewModel
import com.stripe.android.link.ui.getErrorMessage
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.payments.paymentlauncher.PaymentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val initiallySelectedId = args.selectedPaymentDetails?.paymentDetails?.id

    private val _paymentDetails =
        MutableStateFlow<List<ConsumerPaymentDetails.PaymentDetails>>(emptyList())
    val paymentDetails: StateFlow<List<ConsumerPaymentDetails.PaymentDetails>> = _paymentDetails

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _errorMessage = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: StateFlow<ErrorMessage?> = _errorMessage

    init {
        loadPaymentDetails(true)

        viewModelScope.launch {
            navigator.getResultFlow<CardEditViewModel.Result>(CardEditViewModel.Result.KEY)
                ?.collect {
                    when (it) {
                        CardEditViewModel.Result.Success -> loadPaymentDetails()
                        CardEditViewModel.Result.Cancelled -> {}
                        is CardEditViewModel.Result.Failure -> onError(it.error)
                    }
                }
        }
    }

    fun onSelectedPaymentDetails(selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails) {
        clearError()
        _isProcessing.value = true

        runCatching { requireNotNull(linkAccountManager.linkAccount.value) }.fold(
            onSuccess = { linkAccount ->
                if (args.completePayment) {
                    val paramsFactory = ConfirmStripeIntentParamsFactory.createFactory(stripeIntent)
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
                                    }
                                    is PaymentResult.Failed -> {
                                        onError(paymentResult.throwable)
                                    }
                                    is PaymentResult.Completed ->
                                        navigator.dismiss(LinkActivityResult.Success.Completed)
                                }
                            },
                            onFailure = ::onError
                        )

                        _isProcessing.value = false
                    }
                } else {
                    val params = ConfirmStripeIntentParamsFactory.createFactory(stripeIntent)
                        .createPaymentMethodCreateParams(
                            linkAccount.clientSecret,
                            selectedPaymentDetails
                        )
                    navigator.dismiss(
                        LinkActivityResult.Success.Selected(
                            LinkPaymentDetails.Saved(selectedPaymentDetails, params)
                        )
                    )
                }
            },
            onFailure = ::onError
        )
    }

    fun payAnotherWay() {
        navigator.dismiss()
        linkAccountManager.logout()
    }

    fun addNewPaymentMethod(clearBackStack: Boolean = false) {
        navigator.navigateTo(LinkScreen.PaymentMethod(), clearBackStack)
    }

    fun editPaymentMethod(paymentDetails: ConsumerPaymentDetails.PaymentDetails) {
        clearError()
        navigator.navigateTo(LinkScreen.CardEdit(paymentDetails.id))
    }

    fun deletePaymentMethod(paymentDetails: ConsumerPaymentDetails.PaymentDetails) {
        _isProcessing.value = true
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

    private fun loadPaymentDetails(initialSetup: Boolean = false) {
        _isProcessing.value = true
        viewModelScope.launch {
            linkAccountManager.listPaymentDetails().fold(
                onSuccess = { response ->
                    val hasSavedCards =
                        response.paymentDetails.filterIsInstance<ConsumerPaymentDetails.Card>()
                            .takeIf { it.isNotEmpty() }?.let {
                                _paymentDetails.value = it
                                _isProcessing.value = false
                                true
                            } ?: false

                    if (initialSetup && args.selectedPaymentDetails is LinkPaymentDetails.New) {
                        // User is returning and had previously added a new payment method
                        navigator.navigateTo(
                            LinkScreen.PaymentMethod(true),
                            clearBackStack = !hasSavedCards
                        )
                    } else if (!hasSavedCards) {
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
        _isProcessing.value = false
        _errorMessage.value = error
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
