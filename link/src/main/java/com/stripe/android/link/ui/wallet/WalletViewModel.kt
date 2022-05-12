package com.stripe.android.link.ui.wallet

import androidx.lifecycle.MutableLiveData
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

    val isProcessing = MutableLiveData(false)

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
        isProcessing.value = true

        if (args.completePayment) {
            val params = ConfirmStripeIntentParamsFactory.createFactory(stripeIntent)
                .createConfirmStripeIntentParams(linkAccount.clientSecret, selectedPaymentDetails)
            confirmationManager.confirmStripeIntent(params) { result ->
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

                isProcessing.value = false
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

    private fun onError(error: Throwable) {
        logger.error(error.localizedMessage ?: "Internal error.")
        // TODO(brnunes-stripe): Add localized error messages, show them in UI.
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
