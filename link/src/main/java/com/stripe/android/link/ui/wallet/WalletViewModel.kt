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
import com.stripe.android.link.injection.NonFallbackInjectable
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.injection.SignedInViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.ErrorMessage
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
    val linkAccount: LinkAccount,
    private val linkRepository: LinkRepository,
    private val linkAccountManager: LinkAccountManager,
    private val navigator: Navigator,
    private val confirmationManager: ConfirmationManager,
    private val logger: Logger
) : ViewModel() {
    private val stripeIntent = args.stripeIntent

    private val _paymentDetails =
        MutableStateFlow<List<ConsumerPaymentDetails.PaymentDetails>>(emptyList())
    val paymentDetails: StateFlow<List<ConsumerPaymentDetails.PaymentDetails>> = _paymentDetails

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _errorMessage = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: StateFlow<ErrorMessage?> = _errorMessage

    init {
        viewModelScope.launch {
            linkRepository.listPaymentDetails(linkAccount.clientSecret).fold(
                onSuccess = { response ->
                    response.paymentDetails.filterIsInstance<ConsumerPaymentDetails.Card>()
                        .takeIf { it.isNotEmpty() }?.let {
                            _paymentDetails.value = it
                        } ?: addNewPaymentMethod()
                },
                onFailure = ::onError
            )
        }
    }

    fun onSelectedPaymentDetails(selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails) {
        clearError()
        _isProcessing.value = true

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
                .createPaymentMethodCreateParams(linkAccount.clientSecret, selectedPaymentDetails)
            navigator.dismiss(
                LinkActivityResult.Success.Selected(
                    LinkPaymentDetails(selectedPaymentDetails, params)
                )
            )
        }
    }

    fun payAnotherWay() {
        navigator.dismiss()
        linkAccountManager.logout()
    }

    fun addNewPaymentMethod() {
        navigator.navigateTo(LinkScreen.PaymentMethod)
    }

    fun deletePaymentMethod(paymentDetails: ConsumerPaymentDetails.PaymentDetails) {
        _isProcessing.value = true
        clearError()

        viewModelScope.launch {
            linkRepository.deletePaymentDetails(
                linkAccount.clientSecret,
                paymentDetails.id
            ).fold(
                onSuccess = {
                    _paymentDetails.value =
                        _paymentDetails.value.filterNot { it.id == paymentDetails.id }
                    _isProcessing.value = false
                },
                onFailure = {
                    onError(it)
                }
            )
        }
    }

    private fun clearError() {
        _errorMessage.value = null
    }

    private fun onError(error: Throwable) = error.getErrorMessage().let {
        logger.error("Error: ", error)
        _isProcessing.value = false
        _errorMessage.value = it
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
